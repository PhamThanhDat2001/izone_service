package com.izone.izone_service.migration3.application.service.quiz;

import com.izone.izone_service.migration3.application.dto.CreateQuizCommand;
import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper chuyển đổi V1ExerciseAggregate → CreateQuizCommand để gọi POST /v1/quizzes.
 *
 * <p>Mapping chính:
 * <ul>
 *   <li>api_exercises.name           → qe_quiz.name
 *   <li>api_exercises.exam_title     → qe_quiz.description
 *   <li>api_exercises.ielt_time      → qe_quiz.duration_in_minutes
 *   <li>api_exercises.max_score      → qe_quiz.max_grade
 *   <li>api_exercises.ielt_type      → qe_quiz.configuration (JSON {clazz: "IELTS_LISTENING"})
 *   <li>api_questionpart             → qe_quiz_page (mỗi part = một page)
 *   <li>questionsByPartId            → qe_quiz_question (câu hỏi đúng của từng page)
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class QuizMapper {

    /**
     * Build CreateQuizCommand từ V1 aggregate.
     *
     * @param v1Dto           aggregate đề thi V1
     * @param questionsByPartId map: partId V1 → danh sách câu hỏi V2 đã tạo tương ứng
     * @return lệnh tạo quiz gửi lên API V2
     */
    public CreateQuizCommand toCreateQuizCommand(
            V1ExerciseAggregate v1Dto,
            Map<Long, List<CreateQuizCommand.CreateQuizQuestionCommand>> questionsByPartId) {

        if (v1Dto == null || v1Dto.getExercise() == null) return null;

        // Thời gian làm bài: ưu tiên ielt_time, fallback theo loại bài
        int duration = resolveDuration(
                v1Dto.getExercise().getIeltTime(),
                v1Dto.getExercise().getIeltType());

        // Điểm tối đa: ưu tiên max_score, fallback = 10
        int maxGrade = (v1Dto.getExercise().getMaxScore() != null
                && v1Dto.getExercise().getMaxScore() > 0)
                ? v1Dto.getExercise().getMaxScore().intValue()
                : 10;

        // Description: ưu tiên exam_title, fallback = name
        String description = (v1Dto.getExercise().getExamTitle() != null
                && !v1Dto.getExercise().getExamTitle().isBlank())
                ? v1Dto.getExercise().getExamTitle()
                : "Migration: " + v1Dto.getExercise().getName();

        return CreateQuizCommand.builder()
                .name(v1Dto.getExercise().getName())
                .description(description)
                .userId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .durationInMinutes(duration)
                .maxGrade(maxGrade)
                .quizPages(buildQuizPages(v1Dto.getParts(), questionsByPartId, v1Dto.getExercise().getIeltType()))
                .tagIds(Collections.emptyList())
                .configuration(buildQuizConfig(v1Dto.getExercise().getIeltType()))
                .conversionSchemeId(null)  // Chưa map, cần xác định sau
                .thumbnail(null)
                .build();
    }

    /**
     * Build danh sách quiz page, mỗi page tương ứng một part V1.
     * Câu hỏi của từng page được lấy đúng từ map theo partId.
     *
     * <p>Nếu không có part, gom tất cả câu hỏi vào một page chung.
     *
     * @param parts             danh sách part V1
     * @param questionsByPartId map partId → danh sách câu hỏi đã tạo trên V2
     * @param ieltType          loại bài thi (dùng để xác định loại metadata)
     * @return danh sách CreateQuizPageCommand
     */
    private List<CreateQuizCommand.CreateQuizPageCommand> buildQuizPages(
            List<V1ExerciseAggregate.V1PartDto> parts,
            Map<Long, List<CreateQuizCommand.CreateQuizQuestionCommand>> questionsByPartId,
            String ieltType) {

        // Trường hợp không có part: gom tất cả câu hỏi vào một page mặc định
        if (parts == null || parts.isEmpty()) {
            List<CreateQuizCommand.CreateQuizQuestionCommand> allQuestions =
                    questionsByPartId.values().stream()
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());

            return List.of(CreateQuizCommand.CreateQuizPageCommand.builder()
                    .name("General Section")
                    .description("Auto-generated section")
                    .sort(0)
                    .quizQuestions(allQuestions)
                    .configMetadata(buildPageConfigMetadata(ieltType, null))
                    .scoringMetadata(buildPageScoringMetadata(ieltType, null))
                    .build());
        }

        // Mỗi part → một page; câu hỏi của page = câu hỏi đúng của part đó
        return parts.stream().map(part -> {

            // Lấy danh sách câu hỏi đã được tạo trên V2 cho part này
            List<CreateQuizCommand.CreateQuizQuestionCommand> pageQuestions =
                    questionsByPartId.getOrDefault(part.getId(), Collections.emptyList());

            // Page rỗng sẽ vi phạm validation @NotEmpty → bỏ qua
            if (pageQuestions.isEmpty()) {
                return null;
            }

            return CreateQuizCommand.CreateQuizPageCommand.builder()
                    .name(part.getTitle() != null && !part.getTitle().isBlank()
                            ? part.getTitle() : "Part " + part.getId())
                    .description(part.getSubTitle() != null
                            ? part.getSubTitle() : "")
                    .sort(part.getSort() != null ? part.getSort() : 0)
                    .quizQuestions(pageQuestions)
                    .configMetadata(buildPageConfigMetadata(ieltType, part))
                    .scoringMetadata(buildPageScoringMetadata(ieltType, part))
                    .build();
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    }

    /**
     * Build configMetadata cho quiz page theo loại IELTS.
     * V2 dùng "clazz" để deserialize đúng subtype của QuizPageConfigMetadata.
     *
     * <p>Mapping:
     * <ul>
     *   <li>listening → IELTS_LISTENING: { clazz, audioFile: null }
     *   <li>reading   → IELTS_READING:   { clazz, passage: subTitle, referenceQuestionId: null }
     *   <li>writing   → IELTS_WRITING:   { clazz }
     *   <li>speaking  → IELTS_SPEAKING:  { clazz }
     * </ul>
     *
     * @param ieltType loại bài thi từ V1
     * @param part     DTO của part (dùng để lấy passage cho reading)
     * @return Map JSON configMetadata
     */
    private Object buildPageConfigMetadata(String ieltType, V1ExerciseAggregate.V1PartDto part) {
        if (ieltType == null || ieltType.isBlank()) return null;

        Map<String, Object> config = new LinkedHashMap<>();
        switch (ieltType.toLowerCase().trim()) {
            case "listening":
                // IeltsListeningPageConfigMetadata: audioFile (chưa có data từ V1, để null)
                config.put("clazz", "IELTS_LISTENING");
                config.put("audioFile", null);
                break;

            case "reading":
                // IeltsReadingPageConfigMetadata: passage lấy từ subTitle của part
                // (subTitle thường chứa nội dung đoạn văn reading)
                config.put("clazz", "IELTS_READING");
                config.put("passage", part != null ? part.getSubTitle() : null);
                config.put("referenceQuestionId", null);
                break;

            case "writing":
                // IeltsWritingPageConfigMetadata: không có field thêm
                config.put("clazz", "IELTS_WRITING");
                break;

            case "speaking":
                // IeltsSpeakingPageConfigMetadata: không có field thêm
                config.put("clazz", "IELTS_SPEAKING");
                break;

            default:
                config.put("clazz", "DEFAULT");
                break;
        }
        return config;
    }

    /**
     * Build scoringMetadata cho quiz page theo loại IELTS.
     * V2 dùng "clazz" để deserialize đúng subtype của QuizPageScoringMetadata.
     *
     * <p>Mapping:
     * <ul>
     *   <li>listening → IELTS_LISTENING: { clazz, reviewPassage: null, gradeItemAudioTimestampInMsMap: null }
     *   <li>reading   → IELTS_READING:   { clazz, reviewPassage: null }
     *   <li>writing   → IELTS_WRITING:   { clazz }
     *   <li>speaking  → IELTS_SPEAKING:  { clazz }
     * </ul>
     *
     * @param ieltType loại bài thi từ V1
     * @param part     DTO của part (tham khảo, hiện chưa dùng)
     * @return Map JSON scoringMetadata
     */
    private Object buildPageScoringMetadata(String ieltType, V1ExerciseAggregate.V1PartDto part) {
        if (ieltType == null || ieltType.isBlank()) return null;

        Map<String, Object> scoring = new LinkedHashMap<>();
        switch (ieltType.toLowerCase().trim()) {
            case "listening":
                // IeltsListeningPageScoringMetadata: reviewPassage và timestamp map (chưa có từ V1)
                scoring.put("clazz", "IELTS_LISTENING");
                scoring.put("reviewPassage", null);
                scoring.put("gradeItemAudioTimestampInMsMap", null);
                break;

            case "reading":
                // IeltsReadingPageScoringMetadata: reviewPassage (chưa có từ V1)
                scoring.put("clazz", "IELTS_READING");
                scoring.put("reviewPassage", null);
                break;

            case "writing":
                // IeltsWritingPageScoringMetadata: không có field thêm
                scoring.put("clazz", "IELTS_WRITING");
                break;

            case "speaking":
                // IeltsSpeakingPageScoringMetadata: không có field thêm
                scoring.put("clazz", "IELTS_SPEAKING");
                break;

            default:
                scoring.put("clazz", "DEFAULT");
                break;
        }
        return scoring;
    }

    /**
     * Build quiz configuration JSON theo loại IELTS.
     * V2 dùng clazz để phân biệt loại đề (listening/reading/writing/speaking).
     *
     * @param ieltType giá trị ielt_type từ V1 (listening/reading/writing/speaking)
     * @return Map JSON chứa clazz
     */
    private Object buildQuizConfig(String ieltType) {
        if (ieltType == null || ieltType.isBlank()) return null;

        String clazz;
        switch (ieltType.toLowerCase().trim()) {
            case "listening": clazz = "IELTS_LISTENING"; break;
            case "reading":   clazz = "IELTS_READING";   break;
            case "writing":   clazz = "IELTS_WRITING";   break;
            case "speaking":  clazz = "IELTS_SPEAKING";  break;
            default:          clazz = "IELTS";           break;
        }

        return Map.of("clazz", clazz);
    }

    /**
     * Xác định thời gian làm bài (phút).
     * Ưu tiên giá trị từ V1 (ielt_time), nếu null thì fallback theo loại bài:
     * - listening: 40 phút (chuẩn IELTS)
     * - reading:   60 phút
     * - writing:   60 phút
     * - speaking:  15 phút
     * - mặc định:  60 phút
     *
     * @param ieltTime thời gian từ V1 (có thể null)
     * @param ieltType loại bài thi
     * @return thời gian làm bài (phút), tối thiểu 1
     */
    private int resolveDuration(Integer ieltTime, String ieltType) {
        if (ieltTime != null && ieltTime > 0) {
            return ieltTime;
        }

        if (ieltType == null) return 60;

        switch (ieltType.toLowerCase().trim()) {
            case "listening": return 40;
            case "speaking":  return 15;
            case "reading":
            case "writing":
            default:          return 60;
        }
    }
}
