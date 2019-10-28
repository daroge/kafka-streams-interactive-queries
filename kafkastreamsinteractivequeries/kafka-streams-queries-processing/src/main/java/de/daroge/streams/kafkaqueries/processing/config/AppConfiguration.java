package de.daroge.streams.kafkaqueries.processing.config;

import de.daroge.streams.kafkaqueries.common.model.FlightState;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.util.StringUtils;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafkaStreams
public
class AppConfiguration {

    @Value("${application.kafka.topics.flight-topic-name}")
    private String topicName;

    @Value("${application.kafka.stores.flight-store-name}")
    private String storeName;

    @Value("${spring.kafka.properties.application.server}")
    private String applicationServer;

    @Value("${spring.kafka.streams.replication-factor}")
    private String replicationFactor;

    @Autowired private KafkaProperties kafkaProperties;

    @Bean
    public KafkaAdmin admin(){
        Map<String,Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                StringUtils.collectionToCommaDelimitedString(kafkaProperties.getBootstrapServers()));
        return new KafkaAdmin(configs);
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration streamsConfig() {

        Map<String, Object> props = kafkaProperties.buildStreamsProperties();
        setReplicationFactor(props);
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG,applicationServer);
        return new  KafkaStreamsConfiguration(props);
    }

    private void setReplicationFactor(Map<String, Object> props) {
        AdminClient client = AdminClient.create(admin().getConfig());
        DescribeClusterResult clusterResult = client.describeCluster();
        int clusterSize = Try.of(() -> clusterResult.nodes().get().size()).getOrElse(1);
        if (clusterSize >= 3) props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG,replicationFactor);
        else  props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG,clusterSize) ; // for internal topic

    }

    @Bean
    public KTable<String, Long> streamOfFlightInfo(StreamsBuilder builder){

        KStream<String, FlightState> stream = builder.stream(topicName, Consumed.with(Serdes.String(),
                new JsonSerde<>(FlightState.class)));
        KeyValueBytesStoreSupplier stateStore = Stores.inMemoryKeyValueStore(storeName);
        Materialized<String,Long,KeyValueStore<Bytes,byte[]>> materialized = Materialized.as(stateStore);
        materialized.withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Long());
        KGroupedStream<String, FlightState> flightCount = stream.groupByKey();
        return flightCount.count(materialized);
    }

    @Bean
    HostInfo hostInfo() {
        String[] parts = applicationServer.split(":");
        return new HostInfo(parts[0], Integer.parseInt(parts[1]));
    }
}