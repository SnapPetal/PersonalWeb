package biz.thonbecker.personal.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User preferences for notifications and UI settings.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {
    private boolean emailNotifications;
    private boolean pushNotifications;
    private String timezone;
    private String language;
}
