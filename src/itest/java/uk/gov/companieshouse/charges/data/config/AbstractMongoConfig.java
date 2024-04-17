package uk.gov.companieshouse.charges.data.config;

import java.time.Duration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Mongodb configuration.
 */
public class AbstractMongoConfig {

    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:5"));

    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        mongoDBContainer.start();
    }
}