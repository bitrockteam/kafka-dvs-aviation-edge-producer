#!/usr/bin/env bash

exec docker-compose exec kafka \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:9092 \
  --list
