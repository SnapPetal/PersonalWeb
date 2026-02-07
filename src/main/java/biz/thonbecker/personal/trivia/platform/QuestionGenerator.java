package biz.thonbecker.personal.trivia.platform;

import biz.thonbecker.personal.trivia.domain.Question;
import biz.thonbecker.personal.trivia.domain.QuizDifficulty;
import java.util.List;

/**
 * Internal interface for generating quiz questions.
 * This is NOT part of the public API.
 */
interface QuestionGenerator {
    List<Question> generateQuestions(int count, QuizDifficulty difficulty);
}
