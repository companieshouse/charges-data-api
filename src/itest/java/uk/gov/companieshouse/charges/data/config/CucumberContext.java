package uk.gov.companieshouse.charges.data.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

/**
 * Context to store the state.
 */
public enum CucumberContext {

    CONTEXT;

    private static final String RESPONSE = "RESPONSE";

    private final ThreadLocal<Map<String, Object>> testContexts = ThreadLocal.withInitial(HashMap::new);

    public ResponseEntity<?> getResponse() {
        return get(RESPONSE);
    }

    public ResponseEntity<?> setResponse(ResponseEntity<?> response) {
        return set(RESPONSE, response);
    }

    public <T> T get(String name) {
        return (T) testContexts.get()
                .get(name);
    }

    public <T> T set(String name, T object) {
        testContexts.get()
                .put(name, object);
        return object;
    }
}
