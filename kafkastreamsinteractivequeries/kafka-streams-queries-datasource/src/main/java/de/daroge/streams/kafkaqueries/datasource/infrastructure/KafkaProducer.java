package de.daroge.streams.kafkaqueries.datasource.infrastructure;

import de.daroge.streams.kafkaqueries.common.model.FlightState;
import de.daroge.streams.kafkaqueries.datasource.de.daroge.streams.kafkaqueries.datasource.model.Flight;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;

import javax.annotation.PreDestroy;
import java.util.Objects;

@Component
@Slf4j
public class KafkaProducer {

    private DataQuery dataQuery;
    private KafkaSender sender;

    @Value("${spring.kafka.topics.flights-topic-name}")
    private String topic;

    public KafkaProducer(DataQuery dataQuery, KafkaSender sender){
        this.dataQuery = dataQuery;
        this.sender = sender;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start(){
        log.debug("application ready");
        dataQuery.getAll()
                .switchIfEmpty(Mono.just(new Flight()))
                .flatMapMany(flight -> Objects.nonNull(flight.getFlightStates()) ?
                        Flux.fromIterable(flight.getFlightStates()): Flux.empty())
                .delayUntil(this::send)
                .subscribe();

    }

    @PreDestroy
    public void destroy(){
        sender.close();
    }

    private Mono<SenderResult> send (FlightState stateVector){

        log.info("new flight's information arrived: "+ stateVector.getIcao24());
        if (Objects.isNull(stateVector)) {
            log.info("no flight");
            return Mono.empty();
        }
        SenderRecord<String,FlightState,Object> senderRecord = SenderRecord.create(new
                ProducerRecord<String, FlightState>(topic,stateVector.getOriginCountry(),stateVector),null);
        Flux<SenderResult> result = sender.send(Mono.just(senderRecord));
        log.info("flight's information successfully send to kafka: "+ stateVector.getIcao24());
        return result.next();
    }
}
