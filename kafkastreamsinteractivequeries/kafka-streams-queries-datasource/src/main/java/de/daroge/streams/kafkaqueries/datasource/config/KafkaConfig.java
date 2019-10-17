package de.daroge.streams.kafkaqueries.datasource.config;

import de.daroge.streams.kafkaqueries.common.model.FlightState;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Properties;

@Configuration
public class KafkaConfig {


    @Autowired
    private KafkaProperties kafkaProperties;

    @Value("${spring.kafka.topics.flights-topic-name}")
    private String flightsTopic;

    private SenderOptions<String, Object> commonSenderOptions(){
        Properties props = new Properties();

        props.put(ProducerConfig.CLIENT_ID_CONFIG,kafkaProperties.getProducer().getClientId());
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducer().getAcks());
        props.put(ProducerConfig.RETRIES_CONFIG,kafkaProperties.getProducer().getRetries());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,1000);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG,3000);
        props.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG,2000);
        SenderOptions senderOptions = SenderOptions.create(props);
        return senderOptions;
    }

    @Bean
    public KafkaSender<String, FlightState> kafkaSender(){
        SenderOptions senderOptions = commonSenderOptions();
        senderOptions.withKeySerializer(new StringSerializer())
                .withValueSerializer(new JsonSerializer());
        KafkaSender kafkaSender = KafkaSender.create(senderOptions);
        return kafkaSender;
    }

    @Bean
    public NewTopic flightTopic(){
        return TopicBuilder
                .name(flightsTopic)
                .partitions(3)
                .replicas(3)
                .build();
    }
}
