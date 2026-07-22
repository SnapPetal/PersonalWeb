package biz.thonbecker.personal.user.platform.web;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class LoginDestinationRegistry {

    private static final List<LoginDestination> DESTINATIONS =
            List.of(new LoginDestination("/trivia", "Trivia"), new LoginDestination("/landscape", "Landscape"));

    private LoginDestinationRegistry() {}

    static List<LoginDestination> destinations() {
        return DESTINATIONS;
    }

    static Optional<String> findPath(final String path) {
        return DESTINATIONS.stream()
                .filter(destination -> Objects.equals(destination.path(), path))
                .map(LoginDestination::path)
                .findFirst();
    }

    record LoginDestination(String path, String label) {}
}
