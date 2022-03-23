package uk.gov.companieshouse.charges.data.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesService {

    private final Logger logger;
    private ChargesTransformer chargesTransformer;
    private ChargesRepository chargesRepository;


    /**
     * ChargesService constructor.
     *
     * @param logger            Logger.
     * @param chargesRepository chargesRepository.
     */
    public ChargesService(final Logger logger, final ChargesRepository chargesRepository,
            final ChargesTransformer chargesTransformer) {
        this.logger = logger;
        this.chargesRepository = chargesRepository;
        this.chargesTransformer = chargesTransformer;
    }

    /**
     * Save or Update charges.
     *
     * @param companyNumber company number for charge.
     * @param chargeId      charges Id.
     * @param requestBody   request body.
     */
    @Transactional
    public void upsertCharges(String companyNumber, String chargeId,
            InternalChargeApi requestBody) {
        logger.debug(String.format("Started : Save or Update charge %s with company number %s ",
                chargeId,
                companyNumber));

        ChargesDocument charges =
                this.chargesTransformer.transform(companyNumber, chargeId, requestBody);
        logger.debug(String.format("Started : Saving charges in DB "));
        this.chargesRepository.save(charges);
        logger.debug(String.format("Finished : Save or Update charge %s with company number %s",
                chargeId,
                companyNumber));
    }

    public Propagation isTransactionEnabled() {
        return Propagation.NEVER;
    }


}
