#!/usr/bin/env bash

echo -e "Building and starting the services...\n" >&2
sleep 1

docker-compose build && \
  docker-compose up -d kafka-flightstream-producer kafka-flightstream-streams

docker_cmd_status=$?

if [[ $docker_cmd_status == 0 ]]; then
  # the command has succeeded
  echo -e "\nContainers had been successfully started. " \
    'Use `docker-compose logs -f <service-name>` to check the logs.' >&2
fi

exit $docker_cmd_status
