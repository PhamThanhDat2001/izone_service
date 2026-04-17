package com.izone.izone_service.migration3.application.service.question;

import com.izone.izone_service.migration3.application.dto.QuestionBankEntryCreateRequest;
import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper chuyển đổi dữ liệu câu hỏi từ V1 (api_questions + api_questionmeta)
 * sang V2 (QuestionBankEntryCreateRequest) để gọi API lms-service.
 *
 * <p>Mapping chính:
 * <ul>
 *   <li>question_text  → name (nội dung câu hỏi)
 *   <li>description    → description
 *   <li>score          → defaultWeight
 *   <li>question_type  → type (string V1 → enum name V2)
 *   <li>error (meta)   → generalFeedback
 *   <li>api_questionmeta → questionInputs
 * </ul>
 */
@Component
public class QuestionMapper {

    /**
     * Chuyển V1QuestionDto thành QuestionBankEntryCreateRequest để gọi POST /v1/question-bank-entries.
     *
     * @param q DTO câu hỏi từ V1 (đã bao gồm metas)
     * @return request object gửi lên API V2
     */
    public QuestionBankEntryCreateRequest toRequest(V1ExerciseAggregate.V1QuestionDto q) {
        if (q == null) return null;

        // Map loại câu hỏi từ V1 string → V2 enum name
        String v2Type = mapQuestionType(q.getQuestionType());

        // Lấy general_feedback từ error của meta đầu tiên có giá trị
        String generalFeedback = extractGeneralFeedback(q.getMetas());

        // Build danh sách questionInputs từ api_questionmeta
        List<QuestionBankEntryCreateRequest.CreateQuestionInputRequest> questionInputs =
                buildQuestionInputs(q.getMetas(), v2Type);

        // Name: ưu tiên questionText, fallback về "Question {id}"
        String name = (q.getQuestionText() != null && !q.getQuestionText().isBlank())
                ? q.getQuestionText()
                : "Question " + q.getId();

        // Description: ưu tiên description, fallback về name
        String description = (q.getDescription() != null && !q.getDescription().isBlank())
                ? q.getDescription()
                : name;

        // defaultWeight phải >= 1
        int defaultWeight = (q.getScore() != null && q.getScore() >= 1)
                ? q.getScore().intValue()
                : 1;

        // metadata chứa clazz để lms-service deserialize đúng subtype
        Map<String, Object> metadata = Map.of("clazz", v2Type);

        return QuestionBankEntryCreateRequest.builder()
                .name(name)                          // Nội dung câu hỏi
                .type(v2Type)                        // Loại câu hỏi (V2 enum name)
                .description(description)            // Mô tả câu hỏi
                .defaultWeight(defaultWeight)        // Điểm mặc định
                .generalFeedback(generalFeedback)    // Phản hồi chung (từ api_questionmeta.error)
                .ownerId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .questionInputs(questionInputs)      // Danh sách input (từ api_questionmeta)
                .tagIds(Collections.emptyList())
                .metadata(metadata)                  // Chứa clazz để V2 biết deserialize
                .build();
    }

    /**
     * Map loại câu hỏi từ V1 string → tên enum QuestionType của V2.
     * V1 lưu dạng lowercase, V2 dùng UPPER_CASE enum name.
     *
     * <p>Mapping:
     * <ul>
     *   <li>multiple_choice, multiple-choice → MULTIPLE_CHOICE
     *   <li>short_answer, fill_blank         → SHORT_ANSWER
     *   <li>essay                             → ESSAY
     *   <li>cloze, gap_fill                  → CLOZE
     *   <li>group                             → GROUP
     *   <li>matching, match                  → MATCHING
     *   <li>record, recording                → RECORD
     *   <li>writing, ielts_writing           → IELTS_WRITING
     *   <li>speaking, ielts_speaking         → IELTS_SPEAKING_OFFLINE
     *   <li>grid_choice                      → GRID_CHOICE
     *   <li>default (không khớp)             → MULTIPLE_CHOICE
     * </ul>
     *
     * @param v1Type giá trị question_type từ V1
     * @return tên enum V2 QuestionType
     */
    private String mapQuestionType(String v1Type) {
        if (v1Type == null || v1Type.isBlank()) {
            return "MULTIPLE_CHOICE"; // Mặc định
        }

        switch (v1Type.toLowerCase().trim()) {
            case "multiple_choice":
            case "multiple-choice":
            case "mc":
                return "MULTIPLE_CHOICE";

            case "short_answer":
            case "short-answer":
            case "fill_blank":
            case "fill-blank":
            case "fill_in_the_blank":
                return "SHORT_ANSWER";

            case "essay":
            case "open_ended":
                return "ESSAY";

            case "cloze":
            case "gap_fill":
            case "gap-fill":
            case "completion":
                return "CLOZE";

            case "group":
            case "group_question":
                return "GROUP";

            case "matching":
            case "match":
            case "map_matching":
                return "MATCHING";

            case "record":
            case "recording":
                return "RECORD";

            case "writing":
            case "ielts_writing":
                return "IELTS_WRITING";

            case "speaking":
            case "ielts_speaking":
            case "ielts_speaking_offline":
                return "IELTS_SPEAKING_OFFLINE";

            case "grid_choice":
            case "grid":
                return "GRID_CHOICE";

            default:
                // Ghi log để biết có loại mới chưa được map
                return "MULTIPLE_CHOICE";
        }
    }

    /**
     * Map loại input từ V1 meta_type string → tên enum InputType của V2.
     *
     * <p>Mapping:
     * <ul>
     *   <li>text, answer, option → TEXT
     *   <li>file, audio, image   → FILE
     *   <li>text_array           → TEXT_ARRAY
     *   <li>file_array           → FILE_ARRAY
     *   <li>default              → TEXT
     * </ul>
     *
     * @param metaType giá trị meta_type từ V1
     * @return tên enum V2 InputType
     */
    private String mapInputType(String metaType) {
        if (metaType == null || metaType.isBlank()) {
            return "TEXT"; // Mặc định
        }

        switch (metaType.toLowerCase().trim()) {
            case "text":
            case "answer":
            case "option":
            case "input":
                return "TEXT";

            case "file":
            case "audio":
            case "image":
                return "FILE";

            case "text_array":
            case "text-array":
            case "answers":
                return "TEXT_ARRAY";

            case "file_array":
            case "file-array":
            case "files":
                return "FILE_ARRAY";

            default:
                return "TEXT";
        }
    }

    /**
     * Lấy general_feedback từ danh sách meta.
     * Lấy giá trị error đầu tiên không null/blank.
     *
     * @param metas danh sách V1QuestionMetaDto
     * @return feedback string hoặc null nếu không có
     */
    private String extractGeneralFeedback(List<V1ExerciseAggregate.V1QuestionMetaDto> metas) {
        if (metas == null || metas.isEmpty()) return null;
        return metas.stream()
                .map(V1ExerciseAggregate.V1QuestionMetaDto::getError)
                .filter(e -> e != null && !e.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * Build danh sách questionInputs từ api_questionmeta.
     * Mỗi meta record tương ứng một input của câu hỏi trong V2.
     *
     * <p>Nếu không có meta, trả về một input TEXT mặc định
     * để đảm bảo câu hỏi hợp lệ trong V2.
     *
     * @param metas  danh sách V1QuestionMetaDto
     * @param v2Type loại câu hỏi V2 (dùng để build scoringMetadata đúng clazz)
     * @return danh sách CreateQuestionInputRequest
     */
    private List<QuestionBankEntryCreateRequest.CreateQuestionInputRequest> buildQuestionInputs(
            List<V1ExerciseAggregate.V1QuestionMetaDto> metas,
            String v2Type) {

        // Nếu không có meta, tạo một input TEXT mặc định
        if (metas == null || metas.isEmpty()) {
            return List.of(buildDefaultInput(v2Type));
        }

        List<QuestionBankEntryCreateRequest.CreateQuestionInputRequest> inputs =
                metas.stream().map(meta -> {
                    String inputType = mapInputType(meta.getMetaType());

                    // Điểm input: lấy answerScore, tối thiểu 1
                    int weight = (meta.getAnswerScore() != null && meta.getAnswerScore() >= 1)
                            ? meta.getAnswerScore().intValue()
                            : 1;

                    // scoringMetadata: cần có "clazz" để V2 deserialize đúng subtype
                    // Gom answerScore và manualScore vào JSON
                    Map<String, Object> scoringMetadata = new LinkedHashMap<>();
                    scoringMetadata.put("clazz", v2Type);
                    if (meta.getAnswerScore() != null) {
                        scoringMetadata.put("answerScore", meta.getAnswerScore());
                    }
                    if (meta.getManualScore() != null) {
                        scoringMetadata.put("manualScore", meta.getManualScore());
                    }

                    return QuestionBankEntryCreateRequest.CreateQuestionInputRequest.builder()
                            .type(inputType)               // Loại input V2 (TEXT/FILE/...)
                            .weight(weight)                // Trọng số input
                            .scoringMetadata(scoringMetadata) // JSON chứa điểm và clazz
                            .questionMetadata(null)        // Metadata câu hỏi (null vì chưa có data)
                            .build();
                })
                .collect(Collectors.toList());

        return inputs;
    }

    /**
     * Tạo một input mặc định (TEXT, weight=1) khi câu hỏi không có meta.
     * Đảm bảo câu hỏi luôn có ít nhất một input trong V2.
     *
     * @param v2Type loại câu hỏi V2
     * @return input mặc định
     */
    private QuestionBankEntryCreateRequest.CreateQuestionInputRequest buildDefaultInput(String v2Type) {
        Map<String, Object> scoringMetadata = Map.of("clazz", v2Type);
        return QuestionBankEntryCreateRequest.CreateQuestionInputRequest.builder()
                .type("TEXT")
                .weight(1)
                .scoringMetadata(scoringMetadata)
                .questionMetadata(null)
                .build();
    }
}
