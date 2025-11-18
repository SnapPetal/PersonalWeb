package biz.thonbecker.personal.user.api;

import biz.thonbecker.personal.user.domain.User;
import biz.thonbecker.personal.user.domain.UserProfile;
import java.util.List;
import java.util.Optional;

/**
 * Public API for User Management module.
 * This is the only entry point other modules should use to interact with user functionality.
 */
public interface UserFacade {

    /**
     * Register a new user in the system.
     *
     * @param username the username
     * @param email the user's email
     * @return the created user
     */
    User registerUser(String username, String email);

    /**
     * Find a user by their ID.
     *
     * @param userId the user ID
     * @return the user if found
     */
    Optional<User> findUserById(String userId);

    /**
     * Find a user by their username.
     *
     * @param username the username
     * @return the user if found
     */
    Optional<User> findUserByUsername(String username);

    /**
     * Find a user by their email.
     *
     * @param email the email address
     * @return the user if found
     */
    Optional<User> findUserByEmail(String email);

    /**
     * Get all registered users.
     *
     * @return list of all users
     */
    List<User> getAllUsers();

    /**
     * Update user's last login timestamp.
     *
     * @param userId the user ID
     */
    void recordLogin(String userId);

    /**
     * Get user profile information.
     *
     * @param userId the user ID
     * @return the user profile if found
     */
    Optional<UserProfile> getUserProfile(String userId);

    /**
     * Update user profile information.
     *
     * @param profile the updated profile
     * @return the updated profile
     */
    UserProfile updateUserProfile(UserProfile profile);

    /**
     * Enable or disable a user account.
     *
     * @param userId the user ID
     * @param enabled true to enable, false to disable
     */
    void setUserEnabled(String userId, boolean enabled);
}
