package de.daroge.streams.kafkaqueries.processing.config;

import de.daroge.streams.kafkaqueries.processing.infrastructure.InteractiveQueryHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import org.springframework.web.reactive.function.server.RouterFunction;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class InteractiveQueryRoute {

    private InteractiveQueryHandler interactiveQueryHandler;
    private String countCountUri;

    public InteractiveQueryRoute(InteractiveQueryHandler interactiveQueryHandler,
                                 @Value("${application.web.country-count-uri}") String countCountUri){
        this.interactiveQueryHandler = interactiveQueryHandler;
        this.countCountUri = countCountUri;
    }

    @Bean
    public RouterFunction<ServerResponse> route(){
        return nest(path("flights"),
                RouterFunctions.route()
                        .GET(countCountUri,accept(MediaType.APPLICATION_JSON),interactiveQueryHandler::handleFlightSumFrom)
                        .build());
    }
}
