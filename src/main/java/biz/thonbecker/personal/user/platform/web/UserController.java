package biz.thonbecker.personal.user.platform.web;

import biz.thonbecker.personal.user.domain.User;
import biz.thonbecker.personal.user.domain.UserProfile;
import biz.thonbecker.personal.user.platform.persistence.UserService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Slf4j
class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody UserRegistrationRequest request) {
        User user = userService.registerUser(request.username(), request.email());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        return userService
                .findUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService
                .findUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/{userId}/login")
    public ResponseEntity<Void> recordLogin(@PathVariable String userId) {
        userService.recordLogin(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable String userId) {
        return userService
                .getUserProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<UserProfile> updateUserProfile(
            @PathVariable String userId, @RequestBody UserProfile profile) {
        profile.setUserId(userId);
        UserProfile updated = userService.updateUserProfile(profile);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/enabled")
    public ResponseEntity<Void> setUserEnabled(@PathVariable String userId, @RequestParam boolean enabled) {
        userService.setUserEnabled(userId, enabled);
        return ResponseEntity.ok().build();
    }

    record UserRegistrationRequest(String username, String email) {}
}
