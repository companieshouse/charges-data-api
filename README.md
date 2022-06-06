charges-data-api
==========================

charges-data-api is responsible for 
1. Persisting company charges collection to database
2. Making a call to CHS kafka api to notify the charges change.
3. Delete the charge from company mortgages

## Development

Common commands used for development and running locally can be found in the Makefile, each make target has a
description which can be listed by running `make help`

```text
Target               Description
------               -----------
all                  Calls methods required to build a locally runnable version, typically the build target
build                Pull down any dependencies and compile code into an executable if required
clean                Reset repo to pre-build state (i.e. a clean checkout state)
deps                 Install dependencies
package              Create a single versioned deployable package (i.e. jar, zip, tar, etc.). May be dependent on the build target being run before package
sonar                Run sonar scan
test                 Run all test-* targets (convenience method for developers)
test-integration     Run integration tests
test-unit            Run unit tests

```