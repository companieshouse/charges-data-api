Feature: Process company charges gone scenarios

  Scenario Outline: Process company charges GET without security headers

    Given Charges Data API component is successfully running
    And the company charges payload created from file "<data>"
    When I send GET request with company number "<company_number>" and charge Id "<charge_id>" without security headers
    Then an exception is thrown with message text containing "<exceptionText>"


    Examples:
      | data                              | company_number | exceptionText                    |  charge_id                                               |
      | Insolvency_cases_Happy_Path_input | 08124207       | Error while extracting response | OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng  |

  Scenario Outline: Process company charges GET without charge ID without security headers

    Given Charges Data API component is successfully running
    And the company charges payload created from file "<data>"
    When I send GET request with company number "<company_number>" without security headers
    Then an exception is thrown with message text containing "<exceptionText>"


    Examples:
      | data                              | company_number | exceptionText                    |  charge_id                                               |
      | Insolvency_cases_Happy_Path_input | 08124207       | Error while extracting response | OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng  |


  Scenario Outline: Process company charges PUT without security headers

    Given Charges Data API component is successfully running
    And the company charges payload created from file "<data>"
    When I send PUT request with company number "<company_number>" and charge Id "<charge_id>" without security headers
    Then Rest endpoint returns http response code 401 to the client

    Examples:
      | data                              | company_number | charge_id                                               |
      | Insolvency_cases_Happy_Path_input | 08124207       | OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng  |


  Scenario Outline: Process company charges DELETE without security headers

    Given Charges Data API component is successfully running
    And the company charges payload created from file "<data>"
    When I send DELETE request with company number "<company_number>" and charge Id "<charge_id>" without security headers
    Then Rest endpoint returns http response code 401 to the client

    Examples:
      | data                              | company_number | charge_id                                               |
      | Insolvency_cases_Happy_Path_input | 08124207       | OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng  |
