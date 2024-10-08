package uk.gov.companieshouse.charges.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class ChargesDataApiApplication {

    public static final String NAMESPACE = "charges-data-api";

    public static void main(String[] args) {
        SpringApplication.run(ChargesDataApiApplication.class, args);
    }
}
