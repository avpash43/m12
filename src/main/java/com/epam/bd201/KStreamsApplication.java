package com.epam.bd201;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class KStreamsApplication {

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "INSERT_YOUR_APP_ID");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "INSERT_YOUR_BOOTSTRAP_IP:PORT");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "INSERT_YOUR_KEY_SERDE_CLASS_HERE");
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, "INSERT_YOUR_VALUE_SERDE_CLASS_HERE");
        //If needed
        props.put("schema.registry.url", "INSERT_YOUR_SCHEMA_REGISTRY_IP:PORT");

        final String INPUT_TOPIC_NAME = "";
        final String OUTPUT_TOPIC_NAME = "";

        final StreamsBuilder builder = new StreamsBuilder();

        final KStream<String, String> input_records = builder.stream(INPUT_TOPIC_NAME, Consumed.with(Serdes.String(), Serdes.String()));

        //Transform your records here
        //input_records.map();

        input_records.to(OUTPUT_TOPIC_NAME);

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
}
