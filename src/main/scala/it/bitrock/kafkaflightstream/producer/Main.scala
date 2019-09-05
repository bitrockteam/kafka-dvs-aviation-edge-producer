package it.bitrock.kafkaflightstream.producer

import akka.actor.{ActorSystem, Cancellable}
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import it.bitrock.kafkaflightstream.producer.config.{AirlineStream, AirportStream, AppConfig, AviationStream, CityStream, FlightStream}
import it.bitrock.kafkaflightstream.producer.kafka.KafkaSinkFactory
import it.bitrock.kafkaflightstream.producer.kafka.KafkaTypes.{Airline, Airport, City, Flight}
import it.bitrock.kafkaflightstream.producer.model.MessageJson
import it.bitrock.kafkaflightstream.producer.services.{AviationFlow, TickSource}
import it.bitrock.kafkageostream.kafkacommons.serialization.AvroSerdes
import org.apache.kafka.common.serialization.Serdes

object Main extends App with LazyLogging {
  logger.info("Starting up")

  val config = AppConfig.load
  logger.debug(s"Loaded configuration: $config")

  implicit val system: ActorSystem    = ActorSystem("KafkaFlightstreamProducer")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val keySerde = Serdes.String()

  val cancellableFlight = runStream(
    config.aviation.flightStream.pollingInterval,
    config.aviation.getAviationUri(FlightStream),
    config.kafka.flightRawTopic,
    FlightStream
  )

  val cancellableAirport = runStream(
    config.aviation.airportStream.pollingInterval,
    config.aviation.getAviationUri(AirportStream),
    config.kafka.airportRawTopic,
    AirportStream
  )

  val cancellableAirline = runStream(
    config.aviation.airlineStream.pollingInterval,
    config.aviation.getAviationUri(AirlineStream),
    config.kafka.airlineRawTopic,
    AirlineStream
  )

  val cancellableCity = runStream(
    config.aviation.cityStream.pollingInterval,
    config.aviation.getAviationUri(CityStream),
    config.kafka.cityRawTopic,
    CityStream
  )

  sys.addShutdownHook {
    logger.info("Shutting down")
    cancellableFlight.cancel()
    cancellableAirport.cancel()
    cancellableAirline.cancel()
    cancellableCity.cancel()
  }

  private def runStream(
      pollingInterval: Int,
      aviationUri: String,
      topic: String,
      obj: AviationStream
  ): Cancellable = {

    val source = new TickSource(pollingInterval).source

    val flow = new AviationFlow().flow(aviationUri)

    val sink = obj match {
      case FlightStream =>
        val flightRawSerde         = AvroSerdes.serdeFrom[Flight.Value](config.kafka.schemaRegistryUrl)
        val flightProducerSettings = ProducerSettings(system, keySerde.serializer, flightRawSerde.serializer)
        new KafkaSinkFactory[MessageJson, Flight.Key, Flight.Value](topic, flightProducerSettings).sink
      case AirportStream =>
        val airportRawSerde         = AvroSerdes.serdeFrom[Airport.Value](config.kafka.schemaRegistryUrl)
        val airportProducerSettings = ProducerSettings(system, keySerde.serializer, airportRawSerde.serializer)
        new KafkaSinkFactory[MessageJson, Airport.Key, Airport.Value](topic, airportProducerSettings).sink
      case AirlineStream =>
        val airlineRawSerde         = AvroSerdes.serdeFrom[Airline.Value](config.kafka.schemaRegistryUrl)
        val airlineProducerSettings = ProducerSettings(system, keySerde.serializer, airlineRawSerde.serializer)
        new KafkaSinkFactory[MessageJson, Airline.Key, Airline.Value](topic, airlineProducerSettings).sink
      case CityStream =>
        val cityRawSerde         = AvroSerdes.serdeFrom[City.Value](config.kafka.schemaRegistryUrl)
        val cityProducerSettings = ProducerSettings(system, keySerde.serializer, cityRawSerde.serializer)
        new KafkaSinkFactory[MessageJson, City.Key, City.Value](topic, cityProducerSettings).sink
    }

    source.via(flow).mapConcat(identity).to(sink).run()

  }

  //TODO filtrare iatacode di partenza e arrivo per i flight
  //TODO filtrare la compagnia aerea sulla chiave icao e sul campo founding più recente

}
