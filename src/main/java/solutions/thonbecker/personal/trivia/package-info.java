/**
 * Trivia Quiz Module
 *
 * This module provides Dave Ramsey Financial Peace University trivia quiz functionality.
 *
 * <h2>Public API</h2>
 * Other modules should interact with this module ONLY through:
 * <ul>
 *   <li>{@link solutions.thonbecker.personal.trivia.api.TriviaFacade} - Main service interface</li>
 *   <li>{@link solutions.thonbecker.personal.trivia.domain} - Domain model types (Quiz, Question, Player, etc.)</li>
 *   <li>{@link solutions.thonbecker.personal.trivia.api} - API types (QuizState, QuizResult, etc.)</li>
 * </ul>
 *
 * <h2>Internal Implementation</h2>
 * The {@code infrastructure} package contains implementation details and should NOT be accessed
 * directly by other modules. It includes:
 * <ul>
 *   <li>Persistence layer (entities, repositories)</li>
 *   <li>Service implementations</li>
 *   <li>Question generation logic</li>
 * </ul>
 *
 * <h2>Module Dependencies</h2>
 * This module depends on:
 * <ul>
 *   <li>shared - Common utilities and configuration</li>
 * </ul>
 *
 * @since 1.0
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Trivia Quiz",
        allowedDependencies = {},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
@org.springframework.lang.NonNullApi
package solutions.thonbecker.personal.trivia;
