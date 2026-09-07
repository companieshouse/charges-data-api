package uk.gov.companieshouse.charges.data.logging;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Installs the OpenTelemetry logback appender so that log records are correlated with traces.
 * Spring Boot 4's native OpenTelemetry starter does not wire this up automatically.
 */
@Component
@ConditionalOnProperty( prefix = "management.opentelemetry", name = "enabled", havingValue = "true" )
class OpenTelemetryAppenderInitializer implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    OpenTelemetryAppenderInitializer( final OpenTelemetry openTelemetry ) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        installAppender( this.openTelemetry );
    }

    /**
     * Delegates to the static {@link OpenTelemetryAppender#install(OpenTelemetry)} call.
     * Package-private and non-static so it can be stubbed out in a test spy, avoiding the
     * need to mock a static method (and its global JVM logging side effect) directly.
     *
     * @param openTelemetry the configured OpenTelemetry instance to install.
     */
    void installAppender( final OpenTelemetry openTelemetry ) {
        OpenTelemetryAppender.install( openTelemetry );
    }
}
