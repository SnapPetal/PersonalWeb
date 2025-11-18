package biz.thonbecker.personal.user.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class UserProfileEntity {

    @Id
    private String userId;

    @Column
    private String displayName;

    @Column
    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @Column
    private boolean emailNotifications;

    @Column
    private boolean pushNotifications;

    @Column
    private String timezone;

    @Column
    private String language;
}
