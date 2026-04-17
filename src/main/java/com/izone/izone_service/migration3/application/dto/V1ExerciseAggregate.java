package com.izone.izone_service.migration3.application.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregate DTO chứa toàn bộ dữ liệu của một đề thi V1 (api_exercises)
 * cùng với các part và câu hỏi liên quan.
 * Đây là đối tượng trung gian trước khi gọi API V2.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V1ExerciseAggregate {

    /** Thông tin đề thi từ bảng api_exercises */
    private V1ExerciseDto exercise;

    /** Danh sách part của đề thi (từ api_questionpart qua api_partexercisesconnection) */
    private List<V1PartDto> parts;

    /**
     * DTO đại diện cho một part (api_questionpart) trong đề thi.
     * Mapping sang V2: qe_quiz_page
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class V1PartDto {
        /** ID của part trong V1 (api_questionpart.id) */
        private Long id;

        /** Tên part → qe_quiz_page.name */
        private String title;

        /** Mô tả part → qe_quiz_page.description */
        private String subTitle;

        /** Thứ tự part trong đề thi → qe_quiz_page.sort */
        private Integer sort;

        /** Loại chấm điểm (tham khảo, không map trực tiếp) */
        private Integer markType;

        /** Loại bài tập (tham khảo) */
        private String exerciseType;

        /** Danh sách câu hỏi thuộc part này */
        private List<V1QuestionDto> questions;
    }

    /**
     * DTO đại diện cho một câu hỏi (api_questions) trong V1.
     * Mapping sang V2: qe_question_bank_entry + qe_question
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class V1QuestionDto {
        /** ID câu hỏi V1 → dùng để tra cứu metadata */
        private Long id;

        /** Nội dung câu hỏi (HTML) → qe_question_bank_entry.latest_name */
        private String questionText;

        /** Mô tả câu hỏi → qe_question.description */
        private String description;

        /** Tiêu đề phụ của câu hỏi (tham khảo) */
        private String subTitle;

        /** Điểm câu hỏi → qe_question.default_weight, qe_quiz_question.weight */
        private Double score;

        /**
         * Loại câu hỏi V1 (string) → V2 QuestionType enum
         * Ví dụ: "multiple_choice" → "MULTIPLE_CHOICE"
         */
        private String questionType;

        /** Tên nhóm câu hỏi (tham khảo) */
        private String groupName;

        /** Loại IELTS (listening/reading/writing/speaking) */
        private String ieltType;

        /** Thời gian làm câu hỏi (giây, tham khảo) */
        private Integer time;

        /** Trạng thái xoá mềm → qe_question_bank_entry.is_deleted */
        private Boolean isDeleted;

        /** File đính kèm (audio/image) → gắn vào metadata */
        private String file;

        /** Thời điểm tạo → qe_question_bank_entry.creation_datetime */
        private LocalDateTime createdAt;

        /** Thời điểm cập nhật → qe_question_bank_entry.update_datetime */
        private LocalDateTime updatedAt;

        /**
         * Danh sách metadata/input của câu hỏi (từ api_questionmeta).
         * Mỗi record → một qe_question_input
         */
        private List<V1QuestionMetaDto> metas;
    }

    /**
     * DTO đại diện cho một bản ghi api_questionmeta trong V1.
     * Mapping sang V2: qe_question_input
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class V1QuestionMetaDto {
        /** ID meta V1 */
        private Long id;

        /**
         * Loại input (string V1) → qe_question_input.type (InputType enum V2)
         * Ví dụ: "text" → "TEXT", "file" → "FILE"
         */
        private String metaType;

        /** Điểm đáp án đúng → qe_question_input.weight */
        private Double answerScore;

        /** Điểm chấm tay → gom vào qe_question_input.scoring_metadata */
        private Double manualScore;

        /** Phản hồi/lỗi của đáp án → qe_question.general_feedback */
        private String error;
    }
}
