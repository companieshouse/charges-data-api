package uk.gov.companieshouse.charges.data.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.charges.data.auth.EricTokenAuthenticationFilter;
import uk.gov.companieshouse.charges.data.converter.ChargeApiReadConverter;
import uk.gov.companieshouse.charges.data.converter.ChargeApiWriteConverter;
import uk.gov.companieshouse.charges.data.converter.EnumConverters;
import uk.gov.companieshouse.charges.data.serialization.LocalDateDeSerializer;
import uk.gov.companieshouse.charges.data.serialization.LocalDateSerializer;
import uk.gov.companieshouse.charges.data.serialization.LocalDateTimeDeSerializer;
import uk.gov.companieshouse.charges.data.serialization.LocalDateTimeSerializer;
import uk.gov.companieshouse.charges.data.serialization.OffsetDateTimeDeSerializer;
import uk.gov.companieshouse.charges.data.serialization.OffsetDateTimeSerializer;

import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

@Configuration
@EnableWebSecurity
public class ChargesApplicationConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private Logger logger;

    @Bean
    EnvironmentReader environmentReader() {
        return new EnvironmentReaderImpl();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public InternalApiClient internalApiClient() {
        return ApiSdkManager.getPrivateSDK();
    }

    /**
     * mongoCustomConversions.
     *
     * @return MongoCustomConversions.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        ObjectMapper objectMapper = mongoDbObjectMapper();
        return new MongoCustomConversions(List.of(new ChargeApiWriteConverter(objectMapper),
                new ChargeApiReadConverter(objectMapper),new EnumConverters.StringToEnum(),
                new EnumConverters.EnumToString()));
    }

    /**
     * Configure Http Security.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
       http.httpBasic().disable()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterAt(new EricTokenAuthenticationFilter(logger), BasicAuthenticationFilter.class)
                .authorizeRequests()
                .anyRequest().permitAll();
    }

    /**
     * Configure Web Security.
     */
    @Override
    public void configure(WebSecurity web) throws  Exception {
        // Excluding healthcheck endpoint from security filter
        web.ignoring().antMatchers("/healthcheck");
    }

    /**
     * Mongo DB Object Mapper.
     *
     * @return ObjectMapper.
     */
    private ObjectMapper mongoDbObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        module.addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer());
        module.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeSerializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeSerializer());

        objectMapper.registerModule(module);
        return objectMapper;
    }
}