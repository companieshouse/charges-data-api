package uk.gov.companieshouse.charges.data.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;

@Repository
public interface ChargesRepository extends MongoRepository<ChargesDocument, String> {

    @Query("{'company_number': ?0, '_id': ?1, 'updated.at':{$gte : { \"$date\" : ?2 } }}")
    List findChargesDelta(String companyNumber, String chargeId, String at);

}
