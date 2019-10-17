package de.daroge.streams.kafkaqueries.processing.infrastructure;

import de.daroge.streams.kafkaqueries.processing.model.HostInformation;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

@Component
public class KafkaStoreMetaDataService {

    private KafkaStreams kafkaStreams;
    private StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public KafkaStoreMetaDataService(StreamsBuilderFactoryBean streamsBuilderFactoryBean){
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void kafkaStreams(){
        this.kafkaStreams = this.streamsBuilderFactoryBean.getKafkaStreams();
    }

    public <K,V> ReadOnlyKeyValueStore getKeyValueStoreFromLocalKeyValueStore(
            String storeName,
            QueryableStoreType<ReadOnlyKeyValueStore<K,V>> queryableStoreType){

        Supplier<ReadOnlyKeyValueStore<K,V>>  supplierKeyValueStore = () -> kafkaStreams.store(storeName,queryableStoreType);
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(30)
                .intervalFunction(IntervalFunction.ofExponentialBackoff()).build();
        Retry retry = Retry.of("storeQueryConfig",retryConfig);
        supplierKeyValueStore = Retry.decorateSupplier(retry,supplierKeyValueStore);
        ReadOnlyKeyValueStore<K,V> keyValueStore = Try.ofSupplier(supplierKeyValueStore).get();
        return keyValueStore;
    }

    public HostInformation hostInfoForStoreAndKey(final String store, final String key,
                                                  final Serializer<String> serializer) {
        final StreamsMetadata metadata = kafkaStreams.metadataForKey(store, key, serializer);
        if (Objects.isNull(metadata)) {
            return null;
        }
        return new HostInformation(metadata.host(), metadata.port(), metadata.stateStoreNames());
    }
}