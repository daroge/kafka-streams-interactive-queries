package de.daroge.streams.kafkaqueries.datasource.infrastructure;

import de.daroge.streams.kafkaqueries.datasource.de.daroge.streams.kafkaqueries.datasource.model.Flight;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import reactor.retry.Backoff;
import reactor.retry.Retry;

import java.net.URI;
import java.time.Duration;


@Slf4j
@Component
public class DataQuery {

    @Value("${spring.webClient.base-url}")
    private String baseUrl;

    @Value("${spring.webClient.all-uri}")
    private String allUri;

    private WebClient webClient;

    public DataQuery(WebClient.Builder clientBuilder){

        log.info("constructing the webClient");

        this.webClient  = clientBuilder.clientConnector(
                new ReactorClientHttpConnector(HttpClient.create()
                        .tcpConfiguration(tcpClient -> tcpClient.option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,2000)
                                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(3000))))))
                .build();
    }

    public Mono<Flight> getAll(){

        log.info("collecting current flight status");
        URI uri = UriComponentsBuilder.fromUriString(baseUrl+allUri).build().toUri();
        Retry<?> retry = Retry.any() //
                .retryMax(3) //
                .backoff(Backoff.fixed(Duration.ofMillis(2000)));
        Mono<Flight> response = webClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Flight.class)
                .retryWhen(retry);
        return response;
    }

}