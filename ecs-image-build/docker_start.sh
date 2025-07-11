#!/bin/bash
#
# Start script for charges-data-api

PORT=8080

exec java -jar -Dserver.port="${PORT}" "charges-data-api.jar"
