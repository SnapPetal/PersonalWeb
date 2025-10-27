package biz.thonbecker.personal.user.infrastructure.web;

import biz.thonbecker.personal.user.api.UserFacade;
import biz.thonbecker.personal.user.domain.User;
import biz.thonbecker.personal.user.domain.UserProfile;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Slf4j
class UserController {

    private final UserFacade userFacade;

    public UserController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody UserRegistrationRequest request) {
        User user = userFacade.registerUser(request.username(), request.email());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        return userFacade
                .findUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userFacade
                .findUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userFacade.getAllUsers());
    }

    @PostMapping("/{userId}/login")
    public ResponseEntity<Void> recordLogin(@PathVariable String userId) {
        userFacade.recordLogin(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable String userId) {
        return userFacade
                .getUserProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<UserProfile> updateUserProfile(
            @PathVariable String userId, @RequestBody UserProfile profile) {
        profile.setUserId(userId);
        UserProfile updated = userFacade.updateUserProfile(profile);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/enabled")
    public ResponseEntity<Void> setUserEnabled(
            @PathVariable String userId, @RequestParam boolean enabled) {
        userFacade.setUserEnabled(userId, enabled);
        return ResponseEntity.ok().build();
    }

    record UserRegistrationRequest(String username, String email) {}
}
