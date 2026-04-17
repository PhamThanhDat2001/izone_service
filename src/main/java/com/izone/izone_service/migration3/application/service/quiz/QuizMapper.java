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
                .quizPages(buildQuizPages(v1Dto.getParts(), questionsByPartId))
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
     * @param parts            danh sách part V1
     * @param questionsByPartId map partId → danh sách câu hỏi đã tạo trên V2
     * @return danh sách CreateQuizPageCommand
     */
    private List<CreateQuizCommand.CreateQuizPageCommand> buildQuizPages(
            List<V1ExerciseAggregate.V1PartDto> parts,
            Map<Long, List<CreateQuizCommand.CreateQuizQuestionCommand>> questionsByPartId) {

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
                    .configMetadata(null)
                    .scoringMetadata(null)
                    .build());
        }

        // Mỗi part → một page; câu hỏi của page = câu hỏi đúng của part đó
        return parts.stream().map(part -> {

            // Lấy danh sách câu hỏi đã được tạo trên V2 cho part này
            List<CreateQuizCommand.CreateQuizQuestionCommand> pageQuestions =
                    questionsByPartId.getOrDefault(part.getId(), Collections.emptyList());

            // Cảnh báo nếu page không có câu hỏi (có thể do lỗi tạo question)
            if (pageQuestions.isEmpty()) {
                // Tạo page với danh sách rỗng sẽ vi phạm validation @NotEmpty
                // → log cảnh báo, không thêm page này vào danh sách
                return null;
            }

            return CreateQuizCommand.CreateQuizPageCommand.builder()
                    .name(part.getTitle() != null && !part.getTitle().isBlank()
                            ? part.getTitle() : "Part " + part.getId())
                    .description(part.getSubTitle() != null
                            ? part.getSubTitle() : "")
                    .sort(part.getSort() != null ? part.getSort() : 0)
                    .quizQuestions(pageQuestions)  // Chỉ câu hỏi thuộc ĐÚNG part này
                    .configMetadata(null)
                    .scoringMetadata(null)
                    .build();
        })
        .filter(Objects::nonNull)  // Loại bỏ page rỗng
        .collect(Collectors.toList());
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
