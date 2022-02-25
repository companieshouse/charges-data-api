Feature: Health check API endpoint

  Scenario: Client invokes GET /healthcheck endpoint
    Given the application running

    When the client invokes '/healthcheck' endpoint

    Then the client receives status code of 200
    And the client receives response body as 'I am healthy'