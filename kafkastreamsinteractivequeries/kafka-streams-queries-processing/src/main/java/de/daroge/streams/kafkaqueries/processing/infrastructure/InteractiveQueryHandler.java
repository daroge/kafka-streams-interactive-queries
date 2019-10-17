package de.daroge.streams.kafkaqueries.processing.infrastructure;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
import static org.springframework.web.reactive.function.server.ServerResponse.*;
import reactor.core.publisher.Mono;


@Component
public class InteractiveQueryHandler{

    private FlightStoreService flightStoreService;

    public InteractiveQueryHandler(FlightStoreService flightStoreService){
        this.flightStoreService = flightStoreService;
    }

    public Mono<ServerResponse> handleFlightSumFrom(ServerRequest request) {
        String country = request.pathVariable("country");
        Mono<Long> count = flightStoreService.getFlightsCountForKey(country);
        return count.flatMap(c -> ok().contentType(MediaType.APPLICATION_JSON).body(fromPublisher(count,Long.class)))
                .switchIfEmpty(notFound().build());
    }
}