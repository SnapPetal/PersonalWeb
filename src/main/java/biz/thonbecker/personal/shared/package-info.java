/**
 * Shared Module
 *
 * <p>This module provides cross-cutting infrastructure concerns shared across all application
 * modules.
 *
 * <h2>Internal Implementation</h2>
 * <p>The {@code infrastructure} package contains implementation details including:
 * <ul>
 *   <li>Security configuration - Spring Security setup and CSRF protection</li>
 *   <li>Caching configuration - Caffeine cache manager setup</li>
 *   <li>Web configuration - WebSocket, HTTP Interface, and retry logic</li>
 *   <li>Common utilities and helpers</li>
 * </ul>
 *
 * <h2>Module Architecture</h2>
 * <pre>
 * shared/
 * └── infrastructure/configuration/
 *     ├── SecurityConfig          - Application security setup
 *     ├── CacheConfig             - Cache management
 *     ├── WebSocketConfig         - WebSocket messaging
 *     ├── HttpInterfaceConfig     - HTTP client interfaces
 *     └── RetryConfig             - Retry logic
 * </pre>
 *
 * <h2>Purpose</h2>
 * <p>This module serves as the foundation for:
 * <ul>
 *   <li>Security and authentication across all modules</li>
 *   <li>WebSocket infrastructure for real-time features</li>
 *   <li>Declarative HTTP client configuration for external API calls</li>
 *   <li>Resilience patterns like retry logic</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <p>This module:
 * <ul>
 *   <li>Has NO dependencies on other application modules</li>
 *   <li>Provides foundational configuration beans that other modules depend on</li>
 *   <li>Should remain minimal and focused on infrastructure concerns</li>
 * </ul>
 *
 * <h2>Spring Modulith</h2>
 * <p>This module is designed as a shared infrastructure configuration module.
 * Configuration beans are automatically discovered by Spring's component scanning.
 *
 * @since 1.0
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared Infrastructure",
        allowedDependencies = {},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package biz.thonbecker.personal.shared;
