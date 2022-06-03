package uk.gov.companieshouse.charges.data.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.charges.ScottishAlterationsApi;
import uk.gov.companieshouse.charges.data.converter.ChargeApiReadConverter;
import uk.gov.companieshouse.charges.data.converter.ChargeApiWriteConverter;
import uk.gov.companieshouse.charges.data.converter.EnumConverters;
import uk.gov.companieshouse.charges.data.converter.OffsetDateTimeReadConverter;
import uk.gov.companieshouse.charges.data.converter.OffsetDateTimeWriteConverter;
import uk.gov.companieshouse.charges.data.serialization.LocalDateDeSerializer;
import uk.gov.companieshouse.charges.data.serialization.LocalDateSerializer;
import uk.gov.companieshouse.charges.data.serialization.LocalDateTimeDeSerializer;
import uk.gov.companieshouse.charges.data.serialization.LocalDateTimeSerializer;
import uk.gov.companieshouse.charges.data.serialization.NonBlankStringSerializer;
import uk.gov.companieshouse.charges.data.serialization.OffsetDateTimeDeSerializer;
import uk.gov.companieshouse.charges.data.serialization.OffsetDateTimeSerializer;

import uk.gov.companieshouse.charges.data.serialization.ScottishAlterationsApiSerializer;
import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

@Configuration
public class ChargesApplicationConfig implements WebMvcConfigurer {

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
                new EnumConverters.EnumToString(), new OffsetDateTimeReadConverter(),
                new OffsetDateTimeWriteConverter()));
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
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        module.addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer());
        module.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeSerializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeSerializer());
        module.addSerializer(String.class, new NonBlankStringSerializer());
        module.addSerializer(ScottishAlterationsApi.class, new ScottishAlterationsApiSerializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }
}
