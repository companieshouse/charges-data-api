package uk.gov.companieshouse.charges.data.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import uk.gov.companieshouse.charges.data.converter.ChargeApiReadConverter;
import uk.gov.companieshouse.charges.data.converter.ChargeApiWriteConverter;
import uk.gov.companieshouse.charges.data.serialization.LocalDateDeSerializer;
import uk.gov.companieshouse.charges.data.serialization.LocalDateSerializer;

@Configuration
@EnableTransactionManagement
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.name}")
    private String databaseName;

    @Value("${spring.data.mongodb.uri}")
    private String databaseUri;

    @Bean
    PlatformTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
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
                new ChargeApiReadConverter(objectMapper)));
    }

    /**
     * mongoDbObjectMapper.
     *
     * @return ObjectMapper.
     */
    private ObjectMapper mongoDbObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }

    @Override
    protected String getDatabaseName() {
        return this.databaseName == null ? "company-mortgages" : this.databaseName;
    }

    protected  String getDatabaseUri() {
        return this.databaseUri == null ? "" : this.databaseUri;
    }

    @Override
    public MongoClient mongoClient() {
        final ConnectionString connectionString =
                new ConnectionString(getDatabaseUri());
        final MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        return MongoClients.create(mongoClientSettings);
    }
}
