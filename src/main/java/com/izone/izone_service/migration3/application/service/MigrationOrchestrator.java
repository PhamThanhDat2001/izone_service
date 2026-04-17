package com.izone.izone_service.migration3.application.service;

import com.izone.izone_service.migration3.application.dto.CreateQuizCommand;
import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import com.izone.izone_service.migration3.application.service.question.QuestionClient;
import com.izone.izone_service.migration3.application.service.question.QuestionMapper;
import com.izone.izone_service.migration3.application.service.quiz.QuizClient;
import com.izone.izone_service.migration3.application.service.quiz.QuizMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrator điều phối luồng migration dữ liệu đề thi từ V1 → V2.
 *
 * <p>Luồng xử lý cho mỗi đề thi:
 * <ol>
 *   <li>Đọc dữ liệu từ V1 (exercise + parts + questions + metas)
 *   <li>Với mỗi part, tạo từng câu hỏi lên V2 qua POST /v1/question-bank-entries
 *   <li>Gom câu hỏi theo partId → map dùng để gán đúng page
 *   <li>Build CreateQuizCommand với page = part, câu hỏi đúng theo từng page
 *   <li>Tạo quiz trên V2 qua POST /v1/quizzes
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationOrchestrator {

    private final DataReader dataReader;
    private final QuizMapper quizMapper;
    private final QuizClient quizClient;
    private final QuestionMapper questionMapper;
    private final QuestionClient questionClient;

    /**
     * Thực hiện migration một batch đề thi từ V1 sang V2.
     * Gọi method này để bắt đầu quá trình migration.
     */
    public void migrate() {
        // Đọc batch đề thi từ V1 (hiện tại: trang 0, 10 bản ghi/lần)
        // TODO: Thêm pagination loop để migrate toàn bộ dữ liệu
//        List<V1ExerciseAggregate> aggregates =
//                dataReader.readFullAggregates(0, 10);

        List<V1ExerciseAggregate> aggregates = dataReader.readFullAggregates(0, 1);

        if (aggregates.isEmpty()) {
            log.warn("Không có dữ liệu cần migrate.");
            return;
        }

        log.info("Bắt đầu migrate {} đề thi...", aggregates.size());

        int successCount = 0;
        int failCount = 0;

        for (V1ExerciseAggregate agg : aggregates) {
            try {
                migrateOneExercise(agg);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("Lỗi khi migrate exercise id={}: {}",
                        agg.getExercise().getId(), e.getMessage(), e);
                // Tiếp tục với exercise tiếp theo, không dừng toàn bộ
            }
        }

        log.info("Migration hoàn tất: {} thành công, {} thất bại / {} tổng",
                successCount, failCount, aggregates.size());
    }

    /**
     * Migrate một đề thi (exercise) từ V1 sang V2.
     *
     * <p>Quy trình:
     * <ol>
     *   <li>Duyệt từng part, tạo câu hỏi lên V2, gom kết quả theo partId
     *   <li>Build CreateQuizCommand với câu hỏi đúng theo từng page
     *   <li>Gọi API tạo quiz trên V2
     * </ol>
     *
     * @param agg aggregate đề thi V1
     */
    private void migrateOneExercise(V1ExerciseAggregate agg) {
        Long exerciseId = agg.getExercise().getId();
        log.info("▶ Bắt đầu migrate exercise id={}, name='{}'",
                exerciseId, agg.getExercise().getName());

        if (agg.getParts() == null || agg.getParts().isEmpty()) {
            log.warn("Exercise id={} không có part nào, bỏ qua.", exerciseId);
            return;
        }

        // Bước 1: Tạo câu hỏi trên V2, gom theo partId
        // Map: partId (V1) → danh sách CreateQuizQuestionCommand (câu hỏi đã tạo trên V2)
        Map<Long, List<CreateQuizCommand.CreateQuizQuestionCommand>> questionsByPartId =
                new LinkedHashMap<>();

        for (V1ExerciseAggregate.V1PartDto part : agg.getParts()) {
            List<CreateQuizCommand.CreateQuizQuestionCommand> partQuestions =
                    createQuestionsForPart(part, exerciseId);
            questionsByPartId.put(part.getId(), partQuestions);
        }

        // Kiểm tra: phải có ít nhất một câu hỏi được tạo thành công
        boolean hasAnyQuestion = questionsByPartId.values().stream()
                .anyMatch(list -> !list.isEmpty());
        if (!hasAnyQuestion) {
            log.warn("Exercise id={} không tạo được câu hỏi nào, bỏ qua.", exerciseId);
            return;
        }

        // Bước 2: Build lệnh tạo quiz với câu hỏi đúng theo từng page
        CreateQuizCommand command = quizMapper.toCreateQuizCommand(agg, questionsByPartId);
        if (command == null) {
            log.error("Không thể build CreateQuizCommand cho exercise id={}", exerciseId);
            return;
        }

        // Bước 3: Gọi API tạo quiz trên V2
        quizClient.writeQuiz(command);

        log.info("✅ Hoàn tất migrate exercise id={}", exerciseId);
    }

    /**
     * Tạo tất cả câu hỏi của một part lên V2.
     * Trả về danh sách CreateQuizQuestionCommand với questionId V2 tương ứng.
     *
     * @param part       DTO của part V1
     * @param exerciseId ID đề thi (để log)
     * @return danh sách câu hỏi đã tạo thành công trên V2
     */
    private List<CreateQuizCommand.CreateQuizQuestionCommand> createQuestionsForPart(
            V1ExerciseAggregate.V1PartDto part,
            Long exerciseId) {

        if (part.getQuestions() == null || part.getQuestions().isEmpty()) {
            log.warn("Part id={} (exercise id={}) không có câu hỏi.", part.getId(), exerciseId);
            return Collections.emptyList();
        }

        List<CreateQuizCommand.CreateQuizQuestionCommand> result = new ArrayList<>();
        int sortOrder = 0;

        for (V1ExerciseAggregate.V1QuestionDto q : part.getQuestions()) {
            try {
                // Map V1 question → request và gọi API tạo câu hỏi trên V2
                var req = questionMapper.toRequest(q);
                Long newQuestionId = questionClient.create(req);

                // Ghi nhớ ID V2 và vị trí để gán vào quiz page
                result.add(CreateQuizCommand.CreateQuizQuestionCommand.builder()
                        .questionId(newQuestionId)
                        .weight(q.getScore() != null && q.getScore() >= 1
                                ? q.getScore().intValue() : 1)  // Điểm câu hỏi trong quiz
                        .sort(sortOrder++)                        // Thứ tự theo thứ tự insert
                        .build());

            } catch (Exception e) {
                log.error("Lỗi tạo câu hỏi V1 id={} (exercise id={}): {}",
                        q.getId(), exerciseId, e.getMessage(), e);
                // Bỏ qua câu hỏi lỗi, tiếp tục với câu tiếp theo
            }
        }

        log.info("Part id={}: tạo {}/{} câu hỏi thành công",
                part.getId(), result.size(), part.getQuestions().size());

        return result;
    }
}
