Feature: Process company charges (Happy path)

  Scenario Outline: Process company charges successfully

    Given Charges data api service is running
    And i create a company charges record in DB from file "<input>" for company number "<company_number>" and charge id "<charge_id>"
    And the company charges payload created from file "<input>"
    When I send PUT request for company number "08124207" and chargeId "AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng==" with payload
    Then I should receive 200 status code
    And verify the data stored in db matches to "<output>" file

    Examples:
      | input                                                             | output                                                      | company_number | charge_id                                                |
      | Insolvency_cases_Happy_Path_input                                 | Insolvency_cases_Happy_Path_output                          | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
      | Additional_notices_Happy_Path_input                               | Additional_notices_Happy_Path_output                        | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
      | Scott_alterations_input                                           | Scott_alterations_output                                    | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
      | alterations_to_order_input                                        | alterations_to_order_output                                 | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
      | assets_ceased_released_Input                                      | assets_ceased_released_output                               | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
      | created_on_input                                                  | created_on_output                                           | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
      | floating_charge_input                                             | floating_charge_output                                      | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
      | more_than_4_persons_input                                         | more_than_4_persons_output                                  | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
      | obligation_secured_&_nature_of_charge_Happy_Path_input            | obligation_secured_&_nature_of_charge_Happy_Path_output     | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
      | satisfied_on_Happy_Path_input                                     | satisfied_on_Happy_Path_output                              | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |


  Scenario Outline: Retrieve charge details successfully

    Given Charges data api service is running
    And i create a company charges record in DB from file "<data>" for company number "<company_number>" and charge id "<charge_id>"
    And the company charges payload created from file "<data>"
    When I send GET request with company number "<company_number>" and charge Id "<charge_id>"
    Then I should receive 200 status code
    And the Get call response body should match "<result>" file

    Examples:
      | data                              | company_number |   charge_id                                               |  result                             |
      | Insolvency_cases_Happy_Path_input | 08124207       |   OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng  |  Insolvency_cases_Happy_Path_output |



  Scenario Outline: Retrieve charges successfully

    Given Charges data api service is running
    And i create a company charges record in DB from file "<data>" for company number "<company_number>" and charge id "<charge_id>"
    And the company charges payload created from file "<data>"
    When I send GET request with company number "<company_number>"
    Then I should receive 200 status code
    And the Get charges call response body should match "<result>" file for "<company_number>"

    Examples:
      | data                              | company_number  |   charge_id                                               |  result                            |
      | Insolvency_cases_Happy_Path_input | 08124207        |   OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng  |  Insolvency_Company_Metrics_output |


  Scenario Outline: Retrieve charges successfully no metrics

    Given Charges data api service is running
    And i create a company charges record in DB from file "<data>" for company number "<company_number>" and charge id "<charge_id>"
    And the company charges payload created from file "<data>"
    When I send GET request with company number "<company_number>"
    Then I should receive 200 status code
    And the Get charges call response body should match "<result>" file for "<company_number>"

    Examples:
      | data                              | company_number  |   charge_id                                              |  result                                    |
      | Insolvency_cases_Happy_Path_input | 70242180        |   OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng |  Insolvency_Company_Metrics_abent_output   |
