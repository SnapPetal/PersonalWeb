package biz.thonbecker.personal.user.api;

import java.util.Optional;

/** Resolves an authenticated application session to its owning user. */
public interface UserSessionResolver {

    String SESSION_COOKIE_NAME = "PERSONALWEB_AUTH_SESSION";

    Optional<String> resolveUserId(String sessionToken);
}
