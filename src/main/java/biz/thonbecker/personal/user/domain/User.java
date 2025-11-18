package biz.thonbecker.personal.user.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User domain model representing a registered user in the system.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String username;
    private String email;
    private UserRole role;
    private boolean enabled;
    private Instant createdAt;
    private Instant lastLoginAt;
}
