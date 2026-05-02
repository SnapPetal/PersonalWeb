/**
 * Skateboard Trick Analyzer Module
 *
 * This module provides real-time skateboard trick identification and form evaluation
 * using browser-captured video frames and OpenAI multimodal vision.
 *
 * <h2>Public API</h2>
 * The exported cross-module contract is the {@code api} named interface. Other modules should
 * depend only on these types:
 * <ul>
 *   <li>{@link biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult} - Analysis result DTO</li>
 *   <li>{@link biz.thonbecker.personal.skatetricks.api.TrickAnalysisEvent} - Domain event</li>
 *   <li>{@link biz.thonbecker.personal.skatetricks.api.SupportedTrick} - Known trick definitions</li>
 * </ul>
 *
 * <h2>Internal Implementation</h2>
 * The {@code platform} package contains implementation details, including
 * {@link biz.thonbecker.personal.skatetricks.platform.SkateTricksService}, and should NOT be
 * accessed directly by other modules.
 *
 * @since 1.0
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Skateboard Trick Analyzer",
        allowedDependencies = {"shared"})
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.skatetricks;
