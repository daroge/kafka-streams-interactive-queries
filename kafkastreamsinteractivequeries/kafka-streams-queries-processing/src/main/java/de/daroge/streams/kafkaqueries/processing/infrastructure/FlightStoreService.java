package de.daroge.streams.kafkaqueries.processing.infrastructure;

import de.daroge.streams.kafkaqueries.processing.model.HostInformation;
import io.github.resilience4j.retry.Retry;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.retry.Backoff;

import java.time.Duration;
import java.util.Objects;

@Component
public class FlightStoreService {

    private HostInfo hostInfo;
    private StreamsBuilderFactoryBean streamsBuilderFactoryBean;
    private KafkaStreams kafkaStreams;
    private WebClient webClient;
    private Retry retry;
    private String countryCountUri;
    private String flightStoreName;
    private KafkaStoreMetaDataService kafkaStoreMetaDataService;

    @Autowired
    public FlightStoreService(HostInfo hostInfo,
                              StreamsBuilderFactoryBean streamsBuilderFactoryBean,
                              WebClient.Builder builder, Retry retry,
                              @Value("${application.web.country-count-uri}")String countryCountUri,
                              @Value("${application.kafka.stores.flight-store-name}")String flightStoreName,
                              KafkaStoreMetaDataService kafkaStoreMetaDataService){

        this.hostInfo = hostInfo;
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
        webClient = builder.build();
        this.retry = retry;
        this.countryCountUri = countryCountUri;
        this.flightStoreName = flightStoreName;
        this.kafkaStoreMetaDataService = kafkaStoreMetaDataService;
    }

    public Mono<Long> getFlightsCountForKey(String key){
        HostInformation hostStoreInfo = kafkaStoreMetaDataService.hostInfoForStoreAndKey(flightStoreName,key, new StringSerializer());
        if ( Objects.isNull(hostStoreInfo) ) return Mono.empty();
        if(!hostStoreInfo.isEquivalentTo(hostInfo)){
            return getFlightsCountFromRemoteStore(hostStoreInfo,key);
        }

        ReadOnlyKeyValueStore<String,Long> keyValueStore = kafkaStoreMetaDataService
                .getKeyValueStoreFromLocalKeyValueStore(flightStoreName, QueryableStoreTypes.<String,Long>keyValueStore());
        if(Objects.isNull(keyValueStore)) return Mono.empty();
        return Mono.just(keyValueStore.get(key));
    }

    @EventListener(ContextRefreshedEvent.class)
    public void kafkaStreams(){
        this.kafkaStreams = this.streamsBuilderFactoryBean.getKafkaStreams();
    }


    private Mono<Long> getFlightsCountFromRemoteStore(final HostInformation host, final String countryName){
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host(host.getHost())
                        .port(host.getPort())
                        .path(countryCountUri)
                        .build(countryName))
                .retrieve()
                .bodyToMono(Long.class)
                .retryWhen(reactor.retry.Retry.any().retryMax(2).backoff(Backoff.fixed(Duration.ofMillis(2000))));
    }

}
