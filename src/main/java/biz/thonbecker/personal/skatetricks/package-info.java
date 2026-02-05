/**
 * Skateboard Trick Analyzer Module
 *
 * This module provides real-time skateboard trick identification and form evaluation
 * using browser-captured video frames and AWS Bedrock (Claude) multimodal vision.
 *
 * <h2>Public API</h2>
 * Other modules should interact with this module ONLY through:
 * <ul>
 *   <li>{@link biz.thonbecker.personal.skatetricks.api.SkateTricksFacade} - Main service interface</li>
 *   <li>{@link biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult} - Analysis result DTO</li>
 *   <li>{@link biz.thonbecker.personal.skatetricks.api.TrickAnalysisEvent} - Domain event</li>
 *   <li>{@link biz.thonbecker.personal.skatetricks.api.SupportedTrick} - Known trick definitions</li>
 * </ul>
 *
 * <h2>Internal Implementation</h2>
 * The {@code infrastructure} package contains implementation details and should NOT be accessed
 * directly by other modules.
 *
 * @since 1.0
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Skateboard Trick Analyzer",
        allowedDependencies = {"shared"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.skatetricks;
