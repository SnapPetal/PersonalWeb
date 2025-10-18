package solutions.thonbecker.personal.trivia.infrastructure;

import org.springframework.stereotype.Component;

import solutions.thonbecker.personal.service.FinancialPeaceQuestionGenerator;
import solutions.thonbecker.personal.trivia.domain.Question;
import solutions.thonbecker.personal.trivia.domain.QuizDifficulty;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter that bridges the old FinancialPeaceQuestionGenerator with the new modular structure.
 * This is temporary until we fully migrate the question generator into the module.
 */
@Component
class FinancialPeaceQuestionGeneratorAdapter implements QuestionGenerator {

    private final FinancialPeaceQuestionGenerator delegate;

    public FinancialPeaceQuestionGeneratorAdapter(FinancialPeaceQuestionGenerator delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Question> generateQuestions(int count, QuizDifficulty difficulty) {
        // Convert new domain difficulty to old types difficulty
        solutions.thonbecker.personal.types.quiz.QuizDifficulty oldDifficulty =
                solutions.thonbecker.personal.types.quiz.QuizDifficulty.valueOf(difficulty.name());

        // Call the old generator
        List<solutions.thonbecker.personal.types.quiz.Question> oldQuestions =
                delegate.generateQuestions(count, oldDifficulty);

        // Convert old questions to new domain questions
        return oldQuestions.stream()
                .map(old -> new Question(
                        old.getId(),
                        old.getQuestionText(),
                        old.getOptions(),
                        old.getCorrectAnswerIndex()))
                .collect(Collectors.toList());
    }
}
