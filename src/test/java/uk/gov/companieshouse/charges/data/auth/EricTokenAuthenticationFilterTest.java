package uk.gov.companieshouse.charges.data.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class EricTokenAuthenticationFilterTest {

    @Mock
    Logger logger;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain filterChain;

    @Test
    @DisplayName("EricTokenAuthenticationFilter doFilterInternal Test successfully calling filterchain method")
    void doFilterInternal() throws ServletException, IOException {
        // given
        EricTokenAuthenticationFilter ericTokenAuthenticationFilter = new EricTokenAuthenticationFilter(logger);

        when(request.getHeader("ERIC-Identity")).thenReturn("SOME-IDENTITY");
        when(request.getHeader("ERIC-Identity-Type")).thenReturn("OAUTH2");
        when(request.getMethod()).thenReturn("GET");

        // when
        ericTokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("EricTokenAuthenticationFilter doFilterInternal Test not calling filterchain method when not passing proper Eric headers")
    void doFilterInternalNoCallToFilterChain() throws ServletException, IOException {
        // given
        EricTokenAuthenticationFilter ericTokenAuthenticationFilter = new EricTokenAuthenticationFilter(logger);

        when(request.getHeader("ERIC-Identity")).thenReturn("SOME-IDENTITY");

        // when
        ericTokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(0)).doFilter(request, response);
    }
}
