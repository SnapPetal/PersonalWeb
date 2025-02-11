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

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!request.isSecure() && !"localhost".equals(request.getServerName())) {
            String redirectUrl = "https://" + request.getServerName()
                    + (request.getServerPort() != 80 ? ":" + request.getServerPort() : "")
                    + request.getRequestURI()
                    + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

            response.sendRedirect(redirectUrl);
            return;
        }

        chain.doFilter(req, res);
    }
}
