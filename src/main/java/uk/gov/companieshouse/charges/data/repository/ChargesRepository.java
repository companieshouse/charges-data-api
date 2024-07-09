package uk.gov.companieshouse.charges.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesAggregate;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;

@Repository
public interface ChargesRepository extends MongoRepository<ChargesDocument, String> {

    @Query("{'company_number': ?0, '_id': ?1 }")
    Optional<ChargesDocument> findChargeDetails(final String companyNumber, final String chargeId);

    /**
     * Aggregates charges with a specified company number, filters on charge status and sorts by
     * date and charge number. For each document, sorts by created_on date or delivered_on date
     * if created_on does not exist. Sorts by charge number as a second criteria.
     *
     * @param companyNumber The company number to match on.
     * @param filter The list of charge statuses to filter out.
     * @param startIndex The start index.
     * @param pageSize The page size to be returned.
     * @return The list of charges documents to be returned.
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'company_number': ?0, 'data.status': { $nin: ?1 } } }",
            "{ '$addFields': "
                    + "{ 'sort_date': "
                        + "{ $ifNull: [ '$data.created_on', '$data.delivered_on' ] } } }",
            "{ '$sort': { 'sort_date': -1, 'data.charge_number': -1 } }",
            "{ '$facet': { 'total_charges': [{ '$count': 'count' }], "
                    + "'charges_documents': [ { '$skip': ?2 }, { '$limit': ?3 } ] }}",
            })
    ChargesAggregate findCharges(final String companyNumber,
                                   final List<String> filter,
                                   final int startIndex,
                                   final int pageSize);
}
