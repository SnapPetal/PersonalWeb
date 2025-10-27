/**
 * Shared Module
 *
 * <p>This module provides cross-cutting infrastructure concerns shared across all application
 * modules.
 *
 * <h2>Public API</h2>
 * <p>This module exposes infrastructure services through:
 * <ul>
 *   <li>{@link biz.thonbecker.personal.shared.api.CacheFacade} - Caching operations</li>
 *   <li>{@link biz.thonbecker.personal.shared.api.SecurityFacade} - Security context and authorization</li>
 * </ul>
 *
 * <h2>Internal Implementation</h2>
 * <p>The {@code infrastructure} package contains implementation details including:
 * <ul>
 *   <li>Security configuration - Spring Security setup and CSRF protection</li>
 *   <li>Caching configuration - Caffeine cache manager setup</li>
 *   <li>Web configuration - WebSocket, REST template, and retry logic</li>
 *   <li>Common utilities and helpers</li>
 * </ul>
 *
 * <h2>Module Architecture</h2>
 * <pre>
 * shared/
 * ├── api/                            (public interface)
 * │   ├── CacheFacade                - Cache operations API
 * │   └── SecurityFacade             - Security operations API
 * ├── domain/                         (shared domain types - currently empty)
 * └── infrastructure/                 (internal implementation)
 *     ├── CacheFacadeImpl            - Cache facade implementation
 *     ├── SecurityFacadeImpl         - Security facade implementation
 *     └── configuration/
 *         ├── SecurityConfig          - Application security setup
 *         ├── CacheConfig             - Cache management
 *         ├── WebSocketConfig         - WebSocket messaging
 *         ├── RestTemplateConfig      - REST client
 *         └── RetryConfig             - Retry logic
 * </pre>
 *
 * <h2>Purpose</h2>
 * <p>This module serves as the foundation for:
 * <ul>
 *   <li>Security and authentication across all modules</li>
 *   <li>Caching strategy for application-wide data</li>
 *   <li>WebSocket infrastructure for real-time features</li>
 *   <li>HTTP client configuration for external API calls</li>
 *   <li>Resilience patterns like retry logic</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <p>This module:
 * <ul>
 *   <li>Has NO dependencies on other application modules</li>
 *   <li>Provides foundational services that other modules depend on</li>
 *   <li>Should remain minimal and focused on infrastructure concerns</li>
 * </ul>
 *
 * <h2>Spring Modulith</h2>
 * <p>This module is designed as a shared infrastructure module.
 * All implementation details are package-private and hidden from other modules.
 * Configuration beans are automatically discovered by Spring's component scanning.
 *
 * @since 1.0
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared Infrastructure",
        allowedDependencies = {},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
@org.springframework.lang.NonNullApi
package biz.thonbecker.personal.shared;
