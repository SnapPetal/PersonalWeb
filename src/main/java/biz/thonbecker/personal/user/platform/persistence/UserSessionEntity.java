package biz.thonbecker.personal.user.platform.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_sessions", schema = "identity")
@Getter
@Setter
@NoArgsConstructor
public class UserSessionEntity {

    @Id
    private String sessionHash;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;
}
