package uk.gov.companieshouse.charges.data;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ActiveProfiles({"test"})
@RunWith(Cucumber.class)
@CucumberOptions(features = "src/itest/resources/features/charges-api.feature",
        plugin = {"pretty", "json:target/cucumber-report.json"})
@CucumberContextConfiguration
public class CucumberFeaturesRunnerITest {

    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.10"));

    @Autowired
    private ChargesRepository chargesRepository;

    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        mongoDBContainer.start();
        System.out.println(mongoDBContainer.getReplicaSetUrl());
        System.out.println(mongoDBContainer.getHost());
    }

    @AfterEach
    void cleanUp() {
        this.chargesRepository.deleteAll();
    }

}
