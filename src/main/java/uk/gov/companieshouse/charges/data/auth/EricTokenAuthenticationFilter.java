package uk.gov.companieshouse.charges.data.auth;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.logging.Logger;

public class EricTokenAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger;

    public EricTokenAuthenticationFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ericIdentity = request.getHeader("ERIC-Identity");

        if (StringUtils.isBlank(ericIdentity)) {
            logger.error("Unauthorised request received without eric identity");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String ericIdentityType = request.getHeader("ERIC-Identity-Type");
        String contextPath = request.getContextPath();

        if (!"key".equalsIgnoreCase(ericIdentityType)
                && !"oauth2".equalsIgnoreCase(ericIdentityType)) {
            logger.error("Unauthorised request received without eric identity type");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

}