package de.daroge.streams.kafkaqueries.processing.config;

import de.daroge.streams.kafkaqueries.common.model.FlightState;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.admin.AdminClient;
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

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafkaStreams
public
class AppConfiguration {

    @Value("${spring.kafka.bootstrap-server}")
    private String bootstrapServer;

    @Value("${application.name}")
    private String applicationName;

    @Value("${application.kafka.topics.flight-topic-name}")
    private String topicName;

    @Value("${application.kafka.stores.flight-store-name}")
    private String storeName;

    @Value("${application.kafka.streams.port}")
    private int serverPort;

    @Value("${application.kafka.streams.server}")
    private String serverAddress;

    @Value("${spring.kafka.properties.application.server}")
    private String applicationServer;

    @Value("${spring.kafka.streams.replication-factor}")
    private String replicationFactor;

    @Autowired private KafkaProperties kafkaProperties;

    @Autowired
    private KafkaAdmin admin;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration streamsConfig() {

        Map<String, Object> props = new HashMap<>();;
        setReplicationFactor(props);
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG,applicationServer);
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaProperties.getStreams().getApplicationId());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,kafkaProperties.getStreams().getBootstrapServers());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,kafkaProperties.getConsumer().getAutoOffsetReset());
        return new  KafkaStreamsConfiguration(props);
    }

    private void setReplicationFactor(Map<String, Object> props) {
        AdminClient client = AdminClient.create(admin.getConfig());
        DescribeClusterResult clusterResult = client.describeCluster();
        int clusterSize = Try.of(() -> clusterResult.nodes().get().size()).getOrElse(1);
        if (clusterSize > 3) props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG,replicationFactor);
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
        return new HostInfo(serverAddress,serverPort);
    }
}