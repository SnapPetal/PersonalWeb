package biz.thonbecker.personal.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User profile containing additional information about a user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String userId;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private UserPreferences preferences;
}
