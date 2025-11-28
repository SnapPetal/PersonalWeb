/**
 * User Management Module
 *
 * <p>This module handles user authentication, authorization, and profile management.
 * It provides a central identity management system for the application.
 *
 * <p>Public API:
 * <ul>
 *   <li>{@link biz.thonbecker.personal.user.api.UserFacade} - Main operations for user management</li>
 *   <li>{@link biz.thonbecker.personal.user.domain.User} - User domain model</li>
 *   <li>{@link biz.thonbecker.personal.user.domain.UserProfile} - User profile information</li>
 *   <li>{@link biz.thonbecker.personal.user.api.UserRegisteredEvent} - Published when user registers</li>
 *   <li>{@link biz.thonbecker.personal.user.api.UserProfileUpdatedEvent} - Published when profile changes</li>
 * </ul>
 *
 * <p>Module Dependencies:
 * <ul>
 *   <li>shared - For security and caching infrastructure</li>
 * </ul>
 *
 * <p>Integration Points:
 * <ul>
 *   <li>Publishes events when users are created or updated</li>
 *   <li>Other modules can query user information through the facade</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "User Management",
        allowedDependencies = {"shared"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.user;
