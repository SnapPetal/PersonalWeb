package biz.thonbecker.personal.shared.infrastructure;

import biz.thonbecker.personal.shared.api.SecurityFacade;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
class SecurityFacadeImpl implements SecurityFacade {

    @Override
    public Optional<String> getCurrentUserId() {
        return getAuthentication().map(Authentication::getName);
    }

    @Override
    public Optional<String> getCurrentUsername() {
        return getAuthentication().map(Authentication::getName);
    }

    @Override
    public boolean hasRole(String role) {
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority ->
                                authority.equals("ROLE_" + role) || authority.equals(role)))
                .orElse(false);
    }

    @Override
    public boolean isAuthenticated() {
        return getAuthentication().map(Authentication::isAuthenticated).orElse(false);
    }

    @Override
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    private Optional<Authentication> getAuthentication() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of(authentication);
            }
        } catch (Exception e) {
            log.debug("Error getting authentication context", e);
        }
        return Optional.empty();
    }
}
