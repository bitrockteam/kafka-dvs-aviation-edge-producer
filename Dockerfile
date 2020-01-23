FROM openjdk:8 AS sbt-build

# install sbt
RUN wget -O- 'https://github.com/sbt/sbt/releases/download/v1.3.7/sbt-1.3.7.tgz' | \
    tar xzf - -C /usr/local --strip-components=1 && \
    sbt exit

WORKDIR /app

# fetch dependencies first in order to leverage build cache
COPY .sbtopts .
COPY project project
COPY *.sbt ./
RUN sbt update

COPY src src
COPY .scalafmt.conf .

RUN sbt compile && sbt test && sbt universal:packageBin
RUN mv "target/universal/kafka-dvs-aviation-edge-producer-$(sbt -no-colors version | tail -1 | cut -d ' ' -f 2).zip" app.zip

# end of build stage

FROM openjdk:8-jre-alpine

WORKDIR /app

ENV JAVA_OPTS="-Xmx512m"

COPY --from=sbt-build /app/app.zip .
RUN unzip app.zip

RUN chmod u+x bin/kafka-dvs-aviation-edge-producer

ENTRYPOINT ["./bin/kafka-dvs-aviation-edge-producer"]
