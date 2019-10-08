FROM bigtruedata/scala:2.12 AS sbt-build

# install sbt
RUN wget -O- 'https://github.com/sbt/sbt/releases/download/v1.2.8/sbt-1.2.8.tgz' \
    |  tar xzf - -C /usr/local --strip-components=1 \
    && sbt exit

WORKDIR /app

# fetch dependencies first in order to leverage build cache
COPY .sbt .sbt
COPY project project
COPY *.sbt ./
RUN sbt update

COPY src src
COPY .scalafmt.conf .

RUN sbt compile && \
  sbt test && \
  sbt universal:packageBin && \
  mv "target/universal/kafka-flightstream-producer-$(sbt -no-colors version | tail -1 | cut -d ' ' -f 2).zip" /app.zip

# end of build stage

FROM openjdk:8-jre-alpine

WORKDIR /app

ENV JAVA_OPTS="-Xmx512m"

COPY --from=sbt-build /app.zip .
RUN unzip app.zip

RUN chmod u+x bin/kafka-flightstream-producer

ENTRYPOINT ["./bin/kafka-flightstream-producer"]
