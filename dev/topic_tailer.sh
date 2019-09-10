#!/usr/bin/env bash

[[ -z "$1" ]] && \
  echo "You've to provide at least the topic name to read from as the first argument." >&2 && \
  exit 1

exec docker-compose exec kafka \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 \
  --topic "$@"
