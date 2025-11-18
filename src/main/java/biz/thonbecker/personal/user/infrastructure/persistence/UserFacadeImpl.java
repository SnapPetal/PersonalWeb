package biz.thonbecker.personal.user.infrastructure.persistence;

import biz.thonbecker.personal.user.api.UserFacade;
import biz.thonbecker.personal.user.api.UserLoginEvent;
import biz.thonbecker.personal.user.api.UserProfileUpdatedEvent;
import biz.thonbecker.personal.user.api.UserRegisteredEvent;
import biz.thonbecker.personal.user.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
class UserFacadeImpl implements UserFacade {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserFacadeImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public User registerUser(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setEmail(email);
        entity.setRole(UserRole.USER);
        entity.setEnabled(true);
        entity.setCreatedAt(Instant.now());

        UserEntity saved = userRepository.save(entity);
        log.info("User registered: {} ({})", username, saved.getId());

        // Create default profile
        UserProfileEntity profileEntity = new UserProfileEntity();
        profileEntity.setUserId(saved.getId());
        profileEntity.setDisplayName(username);
        profileEntity.setEmailNotifications(true);
        profileEntity.setPushNotifications(true);
        profileEntity.setTimezone("UTC");
        profileEntity.setLanguage("en");
        userProfileRepository.save(profileEntity);

        // Publish event
        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId(), username, email, saved.getCreatedAt()));

        return toUser(saved);
    }

    @Override
    public Optional<User> findUserById(String userId) {
        return userRepository.findById(userId).map(this::toUser);
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username).map(this::toUser);
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toUser);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUser).toList();
    }

    @Override
    @Transactional
    public void recordLogin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            log.info("User login recorded: {}", user.getUsername());

            // Publish event
            eventPublisher.publishEvent(new UserLoginEvent(userId, user.getUsername(), user.getLastLoginAt()));
        });
    }

    @Override
    public Optional<UserProfile> getUserProfile(String userId) {
        return userProfileRepository.findById(userId).map(this::toUserProfile);
    }

    @Override
    @Transactional
    public UserProfile updateUserProfile(UserProfile profile) {
        if (profile.getUserId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        UserProfileEntity entity = userProfileRepository
                .findById(profile.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + profile.getUserId()));

        entity.setDisplayName(profile.getDisplayName());
        entity.setAvatarUrl(profile.getAvatarUrl());
        entity.setBio(profile.getBio());

        if (profile.getPreferences() != null) {
            entity.setEmailNotifications(profile.getPreferences().isEmailNotifications());
            entity.setPushNotifications(profile.getPreferences().isPushNotifications());
            entity.setTimezone(profile.getPreferences().getTimezone());
            entity.setLanguage(profile.getPreferences().getLanguage());
        }

        UserProfileEntity saved = userProfileRepository.save(entity);
        log.info("User profile updated: {}", profile.getUserId());

        // Publish event
        eventPublisher.publishEvent(
                new UserProfileUpdatedEvent(profile.getUserId(), profile.getDisplayName(), Instant.now()));

        return toUserProfile(saved);
    }

    @Override
    @Transactional
    public void setUserEnabled(String userId, boolean enabled) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setEnabled(enabled);
            userRepository.save(user);
            log.info("User {} enabled status set to: {}", user.getUsername(), enabled);
        });
    }

    private User toUser(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getRole(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getLastLoginAt());
    }

    private UserProfile toUserProfile(UserProfileEntity entity) {
        UserPreferences preferences = new UserPreferences(
                entity.isEmailNotifications(),
                entity.isPushNotifications(),
                entity.getTimezone(),
                entity.getLanguage());

        return new UserProfile(
                entity.getUserId(), entity.getDisplayName(), entity.getAvatarUrl(), entity.getBio(), preferences);
    }
}
