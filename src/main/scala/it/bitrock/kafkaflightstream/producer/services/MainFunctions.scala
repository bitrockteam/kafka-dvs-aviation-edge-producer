package it.bitrock.kafkaflightstream.producer.services

import java.net.URI

import akka.actor.{ActorSystem, Cancellable}
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import it.bitrock.kafkaflightstream.producer.kafka.KafkaSinkFactory
import it.bitrock.kafkaflightstream.producer.kafka.KafkaTypes.{Airline, Airplane, Airport, City, Flight}
import it.bitrock.kafkaflightstream.producer.model.{
  AirlineMessageJson,
  AirlineStream,
  AirplaneStream,
  AirportStream,
  AviationStream,
  CityStream,
  FlightMessageJson,
  FlightStream,
  MessageJson
}
import it.bitrock.kafkageostream.kafkacommons.serialization.AvroSerdes
import org.apache.kafka.common.serialization.Serdes

import scala.concurrent.ExecutionContext

object MainFunctions {

  def runStream(
      schemaRegistryUrl: URI,
      pollingStart: Int,
      pollingInterval: Int,
      aviationUri: String,
      apiTimeout: Int,
      topic: String,
      obj: AviationStream,
      filterFunction: MessageJson => Boolean = _ => true
  )(implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Cancellable = {

    val source = new TickSource(pollingStart, pollingInterval).source

    val flow = new AviationFlow().flow(aviationUri, apiTimeout)

    val keySerde = Serdes.String()
    val sink = obj match {
      case FlightStream =>
        val flightRawSerde         = AvroSerdes.serdeFrom[Flight.Value](schemaRegistryUrl)
        val flightProducerSettings = ProducerSettings(system, keySerde.serializer, flightRawSerde.serializer)
        new KafkaSinkFactory[MessageJson, Flight.Key, Flight.Value](topic, flightProducerSettings).sink
      case AirplaneStream =>
        val airplaneRawSerde         = AvroSerdes.serdeFrom[Airplane.Value](schemaRegistryUrl)
        val airplaneProducerSettings = ProducerSettings(system, keySerde.serializer, airplaneRawSerde.serializer)
        new KafkaSinkFactory[MessageJson, Airplane.Key, Airplane.Value](topic, airplaneProducerSettings).sink
      case AirportStream =>
        val airportRawSerde         = AvroSerdes.serdeFrom[Airport.Value](schemaRegistryUrl)
        val airportProducerSettings = ProducerSettings(system, keySerde.serializer, airportRawSerde.serializer)
        new KafkaSinkFactory[MessageJson, Airport.Key, Airport.Value](topic, airportProducerSettings).sink
      case AirlineStream =>
        val airlineRawSerde         = AvroSerdes.serdeFrom[Airline.Value](schemaRegistryUrl)
        val airlineProducerSettings = ProducerSettings(system, keySerde.serializer, airlineRawSerde.serializer)
        new KafkaSinkFactory[MessageJson, Airline.Key, Airline.Value](topic, airlineProducerSettings).sink
      case CityStream =>
        val cityRawSerde         = AvroSerdes.serdeFrom[City.Value](schemaRegistryUrl)
        val cityProducerSettings = ProducerSettings(system, keySerde.serializer, cityRawSerde.serializer)
        new KafkaSinkFactory[MessageJson, City.Key, City.Value](topic, cityProducerSettings).sink
    }

    source.via(flow).mapConcat(identity).filter(filterFunction).to(sink).run()

  }

  def filterFlight: MessageJson => Boolean = { msg =>
    val departureIataCode = msg.asInstanceOf[FlightMessageJson].departure.iataCode
    val arrivalIataCode   = msg.asInstanceOf[FlightMessageJson].arrival.iataCode
    !departureIataCode.isEmpty && !arrivalIataCode.isEmpty
  }

  def filterAirline: MessageJson => Boolean = { msg =>
    val statusAirline = msg.asInstanceOf[AirlineMessageJson].statusAirline
    statusAirline == "active"
  }

}
