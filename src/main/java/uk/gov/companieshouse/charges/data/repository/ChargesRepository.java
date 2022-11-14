package uk.gov.companieshouse.charges.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;

@Repository
public interface ChargesRepository extends MongoRepository<ChargesDocument, String> {

    @Query("{'company_number': ?0, '_id': ?1 }")
    Optional<ChargesDocument> findChargeDetails(final String companyNumber, final String chargeId);

    @Query("{'company_number': ?0, 'data.status': { $nin: ?1 } }, { $set: { exclude: true }")
    Page<ChargesDocument> findCharges(final String companyNumber, final List<ChargeApi.StatusEnum> filter, final Pageable pageable);

}
