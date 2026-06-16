package uk.gov.companieshouse.charges.data.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Mongodb configuration.
 */
public class AbstractMongoConfig {

    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:8.2.5"));

    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        mongoDBContainer.start();

        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
}
