package biz.thonbecker.personal.trivia.infrastructure;

import biz.thonbecker.personal.trivia.domain.Question;
import biz.thonbecker.personal.trivia.domain.QuizDifficulty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Generates Financial Peace trivia questions using AI.
 * Package-private to enforce module boundaries.
 */
@Component
@Slf4j
class FinancialPeaceQuestionGenerator implements QuestionGenerator {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final AtomicLong questionIdGenerator = new AtomicLong(1);

    @Autowired(required = false)
    public FinancialPeaceQuestionGenerator(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    public List<Question> generateQuestions(int count, QuizDifficulty difficulty) {
        if (chatModel == null) {
            log.warn("ChatModel not configured, using fallback questions");
            return generateFallbackQuestions(count);
        }

        try {
            log.info("Generating {} {} questions about Financial Peace by Dave Ramsey", count, difficulty);

            String promptText = """
                You are a financial literacy expert specializing in Dave Ramsey's Financial Peace principles.
                Generate {count} multiple-choice trivia questions about Dave Ramsey's Financial Peace teachings.

                Difficulty level: {difficulty}

                Topics to cover:
                - The 7 Baby Steps
                - Emergency Fund principles
                - Debt Snowball method
                - Budgeting strategies
                - Saving and investing principles
                - Insurance recommendations
                - Wealth building

                For each question, provide:
                1. A clear question text
                2. Exactly 4 answer options
                3. The index (0-3) of the correct answer

                Return the response as a JSON array with this exact structure:
                [
                  {
                    "questionText": "What is Baby Step 1 in Dave Ramsey's Financial Peace plan?",
                    "options": ["Pay off all debt", "Save $1,000 for emergencies", "Invest 15% in retirement", "Pay off the mortgage"],
                    "correctAnswerIndex": 1
                  }
                ]

                Make sure the questions are accurate to Dave Ramsey's actual teachings.
                Difficulty guidelines:
                - EASY: Basic concepts and definitions
                - MEDIUM: Application of principles and multi-step thinking
                - HARD: Detailed scenarios and edge cases
                """;

            PromptTemplate promptTemplate = new PromptTemplate(promptText);
            Prompt prompt = promptTemplate.create(Map.of(
                    "count", String.valueOf(count),
                    "difficulty", difficulty.name()));

            String response = chatModel.call(prompt).getResult().getOutput().getText();
            log.debug("AI Response: {}", response);

            // Parse JSON response
            List<Map<String, Object>> questionMaps = objectMapper.readValue(
                    extractJsonFromResponse(response), new TypeReference<List<Map<String, Object>>>() {});

            List<Question> questions = new ArrayList<>();
            for (Map<String, Object> qMap : questionMaps) {
                questions.add(new Question(
                        questionIdGenerator.incrementAndGet(),
                        (String) qMap.get("questionText"),
                        (List<String>) qMap.get("options"),
                        (Integer) qMap.get("correctAnswerIndex")));
            }

            log.info("Successfully generated {} AI questions", questions.size());
            return questions;

        } catch (Exception e) {
            log.error("Error generating AI questions, falling back to hardcoded questions", e);
            return generateFallbackQuestions(count);
        }
    }

    private String extractJsonFromResponse(String response) {
        // Remove markdown code blocks if present
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private List<Question> generateFallbackQuestions(int count) {
        log.info("Using fallback Financial Peace questions");

        String[] questions = {
            "What is Baby Step 1 in Dave Ramsey's Financial Peace plan?",
            "What is the recommended amount for a fully-funded emergency fund?",
            "What is the Debt Snowball method?",
            "According to Dave Ramsey, what percentage of income should you invest for retirement in Baby Step 4?",
            "What is Baby Step 2?",
            "What type of life insurance does Dave Ramsey recommend?",
            "In what order should you pay off debts using the Debt Snowball?",
            "What is Baby Step 3?",
            "According to Financial Peace, what is a 'sinking fund'?",
            "What does Dave Ramsey say about credit cards?",
            "What is Baby Step 5?",
            "What percentage of your budget should go to housing according to Dave Ramsey?",
            "What is Baby Step 6?",
            "What is the 'Four Walls' concept in Financial Peace?",
            "What is Baby Step 7?",
            "According to Dave Ramsey, when should you invest in the stock market?",
            "What does FPU stand for?",
            "What is Dave Ramsey's recommendation for car buying?",
            "According to Financial Peace, what should you do before investing?",
            "What is the 'envelope system' in budgeting?"
        };

        String[][] options = {
            {"Pay off all debt", "Save $1,000 for emergencies", "Invest 15% in retirement", "Pay off the mortgage"},
            {"$1,000", "3-6 months of expenses", "$5,000", "1 year of expenses"},
            {
                "Pay off debts largest to smallest",
                "Pay off debts smallest to largest",
                "Pay minimum on all debts",
                "Pay highest interest first"
            },
            {"5%", "10%", "15%", "20%"},
            {"Build emergency fund", "Pay off all debt except mortgage", "Save for college", "Pay off mortgage"},
            {"Whole life", "Term life", "Universal life", "Variable life"},
            {"Highest interest rate first", "Largest balance first", "Smallest balance first", "Random order"},
            {"Save 3-6 months of expenses", "Pay off mortgage", "Save for college", "Invest 15%"},
            {
                "A fund for emergencies",
                "A fund for specific future expenses",
                "A retirement account",
                "A debt payment plan"
            },
            {"Use them responsibly", "Only for emergencies", "Cut them up", "Get rewards cards"},
            {"Save for children's college", "Pay off mortgage", "Invest 15%", "Buy a car"},
            {"50%", "35%", "25%", "15%"},
            {"Pay off the mortgage early", "Invest in real estate", "Save for retirement", "Build wealth"},
            {
                "The four Baby Steps",
                "Four types of insurance",
                "Four essential budget categories",
                "Four investment types"
            },
            {"Build wealth and give", "Retire early", "Buy vacation homes", "Start a business"},
            {"Immediately", "After Baby Step 1", "After paying off all debt except mortgage", "After retirement"},
            {"Financial Peace University", "Free Public Utility", "Federal Planning Unit", "Financial Protection Union"
            },
            {"Lease new cars", "Buy used with cash", "Finance at 0%", "Buy new with low interest"},
            {"Have life insurance", "Be debt-free except mortgage", "Have $10,000 saved", "Have a college degree"},
            {
                "Using cash for all purchases",
                "Using cash in envelopes for budget categories",
                "Hiding money in envelopes",
                "Mailing bills in envelopes"
            }
        };

        int[] correctAnswers = {1, 1, 1, 2, 1, 1, 2, 0, 1, 2, 0, 2, 0, 2, 0, 2, 0, 1, 1, 1};

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < questions.length; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices);

        List<Question> result = new ArrayList<>();
        int questionsToGenerate = Math.min(count, questions.length);
        for (int i = 0; i < questionsToGenerate; i++) {
            int idx = indices.get(i);
            result.add(new Question(
                    questionIdGenerator.incrementAndGet(), questions[idx], List.of(options[idx]), correctAnswers[idx]));
        }

        return result;
    }
}
