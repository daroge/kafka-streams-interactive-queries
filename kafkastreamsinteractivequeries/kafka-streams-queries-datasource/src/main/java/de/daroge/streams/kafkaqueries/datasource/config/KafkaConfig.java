package de.daroge.streams.kafkaqueries.datasource.config;

import de.daroge.streams.kafkaqueries.common.model.FlightState;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.StringUtils;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class KafkaConfig {


    @Autowired
    private KafkaProperties kafkaProperties;

    @Value("${spring.kafka.topics.flights-topic-name}")
    private String flightsTopic;

    private SenderOptions<String, Object> commonSenderOptions(){
       Map<String, Object> props = new HashMap<>(
               kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,5000);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG,5000);
        props.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG,5000);
        SenderOptions senderOptions = SenderOptions.create(props);
        return senderOptions;
    }

    @Bean
    public KafkaSender<String, FlightState> kafkaSender(){
        SenderOptions senderOptions = commonSenderOptions();
        senderOptions.withKeySerializer(new StringSerializer())
                .withValueSerializer(new JsonSerializer())
                .stopOnError(false);
        KafkaSender kafkaSender = KafkaSender.create(senderOptions);
        return kafkaSender;
    }

    @Bean
    public KafkaAdmin kafkaAdmin(){
        Map<String,Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                StringUtils.collectionToCommaDelimitedString(kafkaProperties.getBootstrapServers()));
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic flightTopic(){
        return TopicBuilder
                .name(flightsTopic)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG,"600000")
                .build();
    }
}
