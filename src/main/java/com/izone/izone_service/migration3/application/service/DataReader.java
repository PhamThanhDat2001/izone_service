package com.izone.izone_service.migration3.application.service;

import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import com.izone.izone_service.migration3.application.dto.V1ExerciseDto;
import com.izone.izone_service.migration3.domain.modal.*;
import com.izone.izone_service.migration3.domain.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Component chịu trách nhiệm đọc và tổng hợp dữ liệu từ V1 (izone_erp).
 * Thực hiện batch loading để tránh N+1 query problem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataReader {

    private final V1ExerciseRepository exerciseRepo;
    private final V1PartExerciseConnectionRepository partConnectionRepo;
    private final V1QuestionPartRepository questionPartRepo;
    private final V1QuestionRepository questionRepo;
    private final V1QuestionMetaRepository questionMetaRepo;  // Đọc metadata câu hỏi (api_questionmeta)

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Đọc toàn bộ dữ liệu đề thi theo trang (pagination).
     * Trả về danh sách V1ExerciseAggregate chứa đầy đủ thông tin để migration.
     *
     * @param offset vị trí bắt đầu (theo số bản ghi)
     * @param limit  số lượng đề thi mỗi lần đọc
     * @return danh sách aggregate đề thi
     */
    public List<V1ExerciseAggregate> readFullAggregates(int offset, int limit) {
        int page = offset / limit;

        // ===== BƯỚC 1: Lấy danh sách đề thi IELTS (for_ielt = true) =====
        List<V1Exercise> exercises =
                exerciseRepo.findByForIeltTrue(PageRequest.of(page, limit)).getContent();

        if (exercises.isEmpty()) {
            log.warn("Không tìm thấy đề thi nào (trang {}, kích thước {})", page, limit);
            return Collections.emptyList();
        }

        List<Long> exerciseIds = exercises.stream()
                .map(V1Exercise::getId)
                .collect(Collectors.toList());

        // ===== BƯỚC 2: Lấy liên kết exercise ↔ question_part =====
        List<V1PartExerciseConnection> connections =
                partConnectionRepo.findAllByIdExercisesIdIn(exerciseIds);

        // Gom theo exerciseId để tránh O(n²) khi build aggregate
        Map<Long, List<V1PartExerciseConnection>> connectionMap =
                connections.stream()
                        .collect(Collectors.groupingBy(c -> c.getId().getExercisesId()));

        // ===== BƯỚC 3: Lấy danh sách partId liên quan =====
        List<Long> partIds = connections.stream()
                .map(c -> c.getId().getQuestionPartId())
                .distinct()
                .collect(Collectors.toList());

        if (partIds.isEmpty()) {
            log.warn("Không tìm thấy part nào cho các exercise: {}", exerciseIds);
            return Collections.emptyList();
        }

        // ===== BƯỚC 4: Batch load parts và questions =====
        List<V1QuestionPart> parts = questionPartRepo.findAllById(partIds);
        List<V1Question> questions = questionRepo.findAllByPartIdIn(partIds);

        // Index hoá để lookup O(1)
        Map<Long, V1QuestionPart> partMap = parts.stream()
                .collect(Collectors.toMap(V1QuestionPart::getId, p -> p));
        Map<Long, List<V1Question>> questionsByPartMap = questions.stream()
                .collect(Collectors.groupingBy(V1Question::getPartId));

        // ===== BƯỚC 5: Batch load metadata câu hỏi (api_questionmeta) =====
        List<Long> questionIds = questions.stream()
                .map(V1Question::getId)
                .collect(Collectors.toList());

        // Gom metadata theo questionId để lookup O(1)
        Map<Long, List<V1QuestionMeta>> metasByQuestionId = Collections.emptyMap();
        if (!questionIds.isEmpty()) {
            List<V1QuestionMeta> allMetas = questionMetaRepo.findByQuestionsIdIn(questionIds);
            metasByQuestionId = allMetas.stream()
                    .collect(Collectors.groupingBy(V1QuestionMeta::getQuestionsId));
        }

        // Tham chiếu final để dùng trong lambda
        final Map<Long, List<V1QuestionMeta>> finalMetasByQuestionId = metasByQuestionId;

        // ===== BƯỚC 6: Build aggregate cho từng exercise =====
        List<V1ExerciseAggregate> result = exercises.stream().map(ex -> {

            List<V1PartExerciseConnection> exConnections =
                    connectionMap.getOrDefault(ex.getId(), Collections.emptyList());

            List<V1ExerciseAggregate.V1PartDto> partDtos = exConnections.stream()
                    .map(conn -> {
                        Long pId = conn.getId().getQuestionPartId();
                        V1QuestionPart p = partMap.get(pId);
                        List<V1Question> qList =
                                questionsByPartMap.getOrDefault(pId, Collections.emptyList());

                        // Build danh sách câu hỏi kèm metadata
                        List<V1ExerciseAggregate.V1QuestionDto> questionDtos = qList.stream()
                                .map(q -> buildQuestionDto(q, finalMetasByQuestionId))
                                .collect(Collectors.toList());

                        return V1ExerciseAggregate.V1PartDto.builder()
                                .id(pId)
                                .title(p != null ? p.getTitle() : "Untitled Part")
                                .subTitle(p != null ? p.getSubTitle() : "")
                                .sort(conn.getPartNumber() != null ? conn.getPartNumber() : 0)
                                .markType(p != null ? p.getMarkType() : null)
                                .exerciseType(p != null ? p.getExerciseType() : null)
                                .questions(questionDtos)
                                .build();
                    })
                    .collect(Collectors.toList());

            return V1ExerciseAggregate.builder()
                    .exercise(V1ExerciseDto.fromEntity(ex))
                    .parts(partDtos)
                    .build();

        }).collect(Collectors.toList());

        // Log toàn bộ data đã đọc (debug)
        try {
            String json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);
            log.info("\n========== DỮ LIỆU ĐỌC TỪ V1 ==========\n{}\n=========================================", json);
        } catch (Exception e) {
            log.error("Lỗi khi serialize dữ liệu để log", e);
        }

        return result;
    }

    /**
     * Build DTO cho một câu hỏi, kèm theo danh sách metadata (questionInputs).
     *
     * @param q                    entity câu hỏi từ V1
     * @param metasByQuestionId    map questionId → danh sách V1QuestionMeta
     * @return V1QuestionDto đầy đủ thông tin
     */
    private V1ExerciseAggregate.V1QuestionDto buildQuestionDto(
            V1Question q,
            Map<Long, List<V1QuestionMeta>> metasByQuestionId) {

        // Chuyển danh sách V1QuestionMeta sang V1QuestionMetaDto
        List<V1ExerciseAggregate.V1QuestionMetaDto> metaDtos =
                metasByQuestionId.getOrDefault(q.getId(), Collections.emptyList())
                        .stream()
                        .map(meta -> V1ExerciseAggregate.V1QuestionMetaDto.builder()
                                .id(meta.getId())
                                .metaType(meta.getMetaType())      // Loại input (text/file/...)
                                .answerScore(meta.getAnswerScore()) // Điểm đáp án đúng
                                .manualScore(meta.getManualScore()) // Điểm chấm tay
                                .error(meta.getError())             // Phản hồi/feedback
                                .build())
                        .collect(Collectors.toList());

        return V1ExerciseAggregate.V1QuestionDto.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())   // Nội dung câu hỏi (HTML)
                .description(q.getDescription())     // Mô tả câu hỏi
                .subTitle(q.getSubTitle())            // Tiêu đề phụ
                .score(q.getScore())                  // Điểm
                .questionType(q.getQuestionType())    // Loại câu hỏi (string V1)
                .groupName(q.getGroupName())          // Tên nhóm
                .ieltType(q.getIeltType())            // Loại IELTS
                .time(q.getTime())                    // Thời gian (giây)
                .isDeleted(q.getIsQDeleted())         // Trạng thái xoá mềm
                .file(q.getFile())                    // File đính kèm
                .createdAt(q.getCreatedAt())          // Ngày tạo
                .updatedAt(q.getUpdatedAt())          // Ngày cập nhật
                .metas(metaDtos)                      // Danh sách metadata/input
                .build();
    }
}
