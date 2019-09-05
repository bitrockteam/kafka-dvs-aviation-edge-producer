# TODO: lock to a specific tag (which one?)
FROM bigtruedata/sbt AS sbt-build

# TODO: take RELEASE_VERSION from version.sbt
ARG RELEASE_VERSION="0.0.1-SNAPSHOT"

WORKDIR /app

# fetch dependencies first in order to leverage build cache
COPY .sbt .sbt
COPY project project
COPY *.sbt ./
RUN sbt update

COPY src src
COPY .scalafmt.conf .

RUN sbt compile && \
  sbt universal:packageBin && \
  mv "target/universal/kafka-flightstream-producer-${RELEASE_VERSION}.zip" /app.zip

# end of build stage

FROM openjdk:8-jre-alpine

WORKDIR /app

ENV JAVA_OPTS="-Xmx512m"

COPY --from=sbt-build /app.zip .
RUN unzip app.zip

RUN chmod u+x bin/kafka-flightstream-producer

ENTRYPOINT ["./bin/kafka-flightstream-producer"]
