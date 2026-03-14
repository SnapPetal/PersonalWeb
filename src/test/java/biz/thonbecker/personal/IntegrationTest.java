package biz.thonbecker.personal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Meta-annotation for module integration tests.
 *
 * <p>Combines {@link ApplicationModuleTest} with Testcontainers PostgreSQL,
 * stub OAuth2, and the test profile. Usage:
 *
 * <pre>{@code
 * @IntegrationTest
 * class BookingModuleTest {
 *     @Test
 *     void myTest(Scenario scenario) { ... }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ApplicationModuleTest(extraIncludes = "shared")
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
public @interface IntegrationTest {}
