package biz.thonbecker.personal.shared.api;

import java.util.Optional;
import org.springframework.modulith.NamedInterface;

/**
 * Public API for security and authentication operations across modules.
 * This facade provides security context and authorization checks.
 */
@NamedInterface("SecurityOperations")
public interface SecurityFacade {

    /**
     * Get the current authenticated user's ID.
     *
     * @return the user ID if authenticated
     */
    Optional<String> getCurrentUserId();

    /**
     * Get the current authenticated user's username.
     *
     * @return the username if authenticated
     */
    Optional<String> getCurrentUsername();

    /**
     * Check if the current user has a specific role.
     *
     * @param role the role to check
     * @return true if the user has the role
     */
    boolean hasRole(String role);

    /**
     * Check if the current user is authenticated.
     *
     * @return true if authenticated
     */
    boolean isAuthenticated();

    /**
     * Check if the current user is an administrator.
     *
     * @return true if the user has admin role
     */
    boolean isAdmin();
}
