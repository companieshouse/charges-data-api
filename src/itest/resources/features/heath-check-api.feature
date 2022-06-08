Feature: Health check API endpoint

  Scenario: Client invokes GET /charges-data-api/healthcheck endpoint
    Given the application running
    When the client invokes '/charges-data-api/healthcheck' endpoint
    Then the client receives status code of 200
    And the client receives response body as '{"status":"UP"}'

  Scenario: Client invokes GET /charges-data-api/healthcheck endpoint
    Given the application running
    But   MongoDB is not reachable
    When the client invokes '/charges-data-api/healthcheck' endpoint
    Then the client receives status code of 503
    And the client receives response body as '{"status":"DOWN"}'