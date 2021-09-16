package com.epam.bd201;

import com.epam.bd201.entity.Category;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class KStreamsApplication {
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        // If needed
        props.put("schema.registry.url", "schemaregistry:8081");


        final String INPUT_TOPIC_NAME = "expedia";
        final String OUTPUT_TOPIC_NAME = "expedia_ext";

        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<String, String> inputStream = builder.stream(INPUT_TOPIC_NAME, Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, String> outputStream = inputStream.mapValues(value -> {
            try {
                Map<String, String> valueMap = objectMapper.readValue(value, HashMap.class);
                String checkIn = valueMap.getOrDefault("srch_ci", "");
                String checkOut = valueMap.getOrDefault("srch_co", "");

                String category = getCategory(checkIn, checkOut).getName();
                valueMap.put("category", category);

                String result = objectMapper.writeValueAsString(valueMap);
                System.out.println(result);

                return result;
            } catch (JsonProcessingException e) {
                System.out.println("Can't parse record: " + value);
                throw new RuntimeException("Can't parse record: " + value);
            }
        });

        outputStream.to(OUTPUT_TOPIC_NAME);

        final Topology topology = builder.build();
        System.out.println(topology.describe());

        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

    private static Category getCategory(String checkIn, String checkOut) {
        long stayPeriod = calculatePeriod(checkIn, checkOut);

        if (stayPeriod >= 1 && stayPeriod <= 4) {
            return Category.SHORT_STAY;
        }

        if (stayPeriod >= 5 && stayPeriod <= 10) {
            return Category.STANDARD_STAY;
        }

        if (stayPeriod >= 11 && stayPeriod <= 14) {
            return Category.STANDARD_EXTENDED_STAY;
        }

        if (stayPeriod > 14) {
            return Category.LONG_STAY;
        }

        return Category.ERRONEOUS_DATE;
    }

    private static long calculatePeriod(String checkIn, String checkOut) {
        try {
            LocalDate checkInDate = LocalDate.parse(checkIn);
            LocalDate checkOutDate = LocalDate.parse(checkOut);
            return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        } catch (Exception e) {
            System.out.println("Can't parse dates. checkIn: " + checkIn + " , checkOut: " + checkOut);
        }
        return 0;
    }
}
