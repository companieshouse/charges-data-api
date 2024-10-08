package uk.gov.companieshouse.charges.data.auth;

import static uk.gov.companieshouse.charges.data.ChargesDataApiApplication.NAMESPACE;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.charges.data.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class EricTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        String ericIdentity = request.getHeader("ERIC-Identity");

        if (StringUtils.isBlank(ericIdentity)) {
            LOGGER.error("Request received without eric identity", DataMapHolder.getLogMap());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String ericIdentityType = request.getHeader("ERIC-Identity-Type");

        if (!"key".equalsIgnoreCase(ericIdentityType)
                && !"oauth2".equalsIgnoreCase(ericIdentityType)) {
            LOGGER.error("Request received without correct eric identity type", DataMapHolder.getLogMap());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!isKeyAuthorised(request, ericIdentityType)) {
            LOGGER.info("Supplied key does not have sufficient privilege for the action", DataMapHolder.getLogMap());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isKeyAuthorised(HttpServletRequest request, String ericIdentityType) {
        String[] privileges = getApiKeyPrivileges(request);

        return request.getMethod().equals("GET")
                || (ericIdentityType.equalsIgnoreCase("Key")
                    && ArrayUtils.contains(privileges, "internal-app"));
    }

    private String[] getApiKeyPrivileges(HttpServletRequest request) {
        String commasSeparatedPrivilegeString = request.getHeader("ERIC-Authorised-Key-Privileges");

        return Optional.ofNullable(commasSeparatedPrivilegeString)
                .map(s -> s.split(","))
                .orElse(new String[]{});
    }
}