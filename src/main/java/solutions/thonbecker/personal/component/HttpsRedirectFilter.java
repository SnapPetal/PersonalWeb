package solutions.thonbecker.personal.component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class HttpsRedirectFilter implements Filter {
    private static final String HEALTH_CHECK_PATH = "/actuator/health";
    private static final String DOMAIN = "thonbecker.solutions";
    private static final String WWW_PREFIX = "www.";
    private static final String LOCALHOST = "localhost";
    private static final String HTTPS_SCHEME = "https://";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (shouldSkipRedirect(request)) {
            chain.doFilter(req, res);
            return;
        }

        if (requiresRedirect(request)) {
            String redirectUrl = buildRedirectUrl(request);
            response.sendRedirect(redirectUrl);
            return;
        }

        chain.doFilter(req, res);
    }

    private boolean shouldSkipRedirect(HttpServletRequest request) {
        return request.getRequestURI().equals(HEALTH_CHECK_PATH) || LOCALHOST.equals(request.getServerName());
    }

    private boolean requiresRedirect(HttpServletRequest request) {
        String serverName = request.getServerName();
        return !request.isSecure() || (serverName.endsWith(DOMAIN) && !serverName.startsWith(WWW_PREFIX));
    }

    private String buildRedirectUrl(HttpServletRequest request) {
        String serverName = request.getServerName();
        String redirectDomain =
                serverName.endsWith(DOMAIN) ? WWW_PREFIX + serverName.replaceFirst("^www\\.", "") : serverName;

        StringBuilder urlBuilder =
                new StringBuilder(HTTPS_SCHEME).append(redirectDomain).append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null) {
            urlBuilder.append('?').append(queryString);
        }

        return urlBuilder.toString();
    }
}
