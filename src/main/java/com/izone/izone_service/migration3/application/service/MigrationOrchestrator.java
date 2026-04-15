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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationOrchestrator {

    private final DataReader dataReader;
    private final QuizMapper quizMapper;
    private final QuizClient quizClient;
    private final QuestionMapper questionMapper;
    private final QuestionClient questionClient;

    public void migrate() {

        List<V1ExerciseAggregate> aggregates =
                dataReader.readFullAggregates(0, 1);

        for (V1ExerciseAggregate agg : aggregates) {

            // 🔥 1. create ALL questions trước
            var quizQuestions = agg.getParts().stream()
                    .flatMap(p -> p.getQuestions().stream())
                    .map(q -> {
                        var req = questionMapper.toRequest(q);
                        Long newId = questionClient.create(req);

                        return CreateQuizCommand.CreateQuizQuestionCommand.builder()
                                .questionId(newId)
                                .weight(q.getScore() != null ? q.getScore().intValue() : 1)
                                .sort(0)
                                .build();
                    })
                    .collect(Collectors.toList());

            // 🔥 2. build quiz
            CreateQuizCommand command =
                    quizMapper.toCreateQuizCommand(agg, quizQuestions);

            // 🔥 3. call API
            quizClient.writeQuiz(command);

            log.info("✅ DONE Exercise {}", agg.getExercise().getId());
        }
    }
}