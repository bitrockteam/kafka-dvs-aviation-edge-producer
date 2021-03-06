# DVS Aviation Edge Producer

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/ea04074b20954f3ca1584c5e5ead4b17)](https://app.codacy.com/gh/bitrockteam/kafka-dvs-aviation-edge-producer?utm_source=github.com&utm_medium=referral&utm_content=bitrockteam/kafka-dvs-aviation-edge-producer&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://iproject-jenkins.reactive-labs.io/buildStatus/icon?job=kafka-dvs-aviation-edge-producer%2Fmaster)](https://iproject-jenkins.reactive-labs.io/job/kafka-dvs-aviation-edge-producer/job/master/)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-v2.0%20adopted-ff69b4.svg)](CODE_OF_CONDUCT.md)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

## Configuration

The application references the following environment variables:

- `HOST`: server host
- `PORT`: server port
- `KAFKA.BOOTSTRAP.SERVERS`: valid `bootstrap.servers` value (see [Confluent docs](https://docs.confluent.io/current/clients/consumer.html#configuration))
- `SCHEMAREGISTRY.URL`: valid `schema.registry.url` value (see [Confluent docs](https://docs.confluent.io/current/schema-registry/docs/schema_registry_tutorial.html#java-consumers))
- `AVIATION_EDGE.BASE_URL`: [Aviation Edge](http://aviation-edge.com/developers/) base URL
- `AVIATION_EDGE.KEY`: Aviation Edge API key
- `AVIATION_EDGE.TIMEOUT`: request timeout in seconds to Aviation Edge API
- `AVIATION_EDGE.FLIGHT_SPEED_LIMIT`: max flight speed to filter on

## How to test

Execute unit tests running the following command:

```sh
sbt test
```

## How to build

Build and publish Docker image running the following command:

```sh
sbt docker:publish
```

## Architectural diagram

Architectural diagram is available [here](docs/diagram.puml). It can be rendered using [PlantText](https://www.planttext.com).

## Contribution

If you'd like to contribute to the project, make sure to review our [recommendations](CONTRIBUTING.md).
