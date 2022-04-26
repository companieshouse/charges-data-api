Feature: Process company charges

  Scenario Outline: Process company charges successfully

    Given Charges data api service is running
    When I send PUT request for company number "08124207" and chargeId "AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng==" with payload "<input>"
    Then I should receive 200 status code
    And verify the data stored in db matches to "<output>" file

    Examples:
      | input                                                             | output                                                      |
     # | Insolvency_cases_Happy_Path_input                                 | Insolvency_cases_Happy_Path_output                          |
      | Additional_notices_Happy_Path_input                               | Additional_notices_Happy_Path_output                        |
     # | Insolvency_cases_Happy_Path_input                                 | Insolvency_cases_Happy_Path_output                          |
     # | Scott_alterations_input                                           | Scott_alterations_output                                    |
     # | alterations_to_order_input                                        | alterations_to_order_output                                 |
     # | assets_ceased_released_Input                                      | assets_ceased_released_output                               |
     # | created_on_input                                                  | created_on_output                                           |
     # | floating_charge_input                                             | floating_charge_output                                      |
     # | more_than_4_persons_input                                         | more_than_4_persons_output                                  |
     # | obligation_secured_&_nature_of_charge_Happy_Path_input            | obligation_secured_&_nature_of_charge_Happy_Path_output     |
     # | satisfied_on_Happy_Path_input                                     | satisfied_on_Happy_Path_output                              |


  Scenario Outline: Retrieve charge details successfully

    Given Charges data api service is running
    And the company charges with "<company_number>" and "<charge_id>" exists with data "<data>"
    When I send GET request with company number "<company_number>" and charge Id "<charge_id>"
    Then I should receive 200 status code
    And the Get call response body should match "<result>" file

    Examples:
      | data                              | company_number  |   charge_id                                                       |  result                                    |
      | Insolvency_cases_Happy_Path_input | 08124207    |   OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng          |  Insolvency_cases_Happy_Path_output        |



  Scenario Outline: Retrieve charges successfully

    Given Charges data api service is running
    And the company charges with "<company_number>" and "<charge_id>" exists with data "<data>"
    When I send GET request with company number "<company_number>"
    Then I should receive 200 status code
    And the Get charges call response body should match "<result>" file

    Examples:
      | data                              | company_number  |   charge_id                                                       |  result                                    |
      | Insolvency_cases_Happy_Path_input | 08124207        |   OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng          |  Insolvency_cases_Happy_Path_output        |
