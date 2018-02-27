package com.gok.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaMessager {

	private static String KAFKA_BOOTSTRAP_SERVERS_ENVNAME = "KAFKA_BOOTSTRAP_SERVERS";
	private static String KAFKA_BOOTSTRAP_SERVERS_DEFAULT = "kafka-service:9092";

	private static String kafkaBootstrapServers = null;
	private static Properties kafkaConnectionProperties = null;

	public static void send(String message) {
		Producer<String, String> producer = new KafkaProducer<>(getKafkaConnectionProperties());
		producer.send(new ProducerRecord<String, String>("test", "testkey", message));
		producer.close();
	}

	private static Properties getKafkaConnectionProperties() {
		if (kafkaConnectionProperties == null) {
			System.out.println("props init");
			Properties props = new Properties();
			props.put("bootstrap.servers", getBootstrapServers());
			props.put("acks", "all");
			props.put("retries", 0);
			props.put("batch.size", 16384);
			props.put("linger.ms", 1);
			props.put("buffer.memory", 33554432);
			props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
			props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
			kafkaConnectionProperties = props;
		}
		return kafkaConnectionProperties;
	}

	private static String getBootstrapServers() {
		if (kafkaBootstrapServers == null) {
			try {
				kafkaBootstrapServers = System.getenv(KAFKA_BOOTSTRAP_SERVERS_ENVNAME);
			} catch (Exception e) {
				// ignore
			}
		}

		return kafkaBootstrapServers == null ? KAFKA_BOOTSTRAP_SERVERS_DEFAULT : kafkaBootstrapServers;

	}

	public static void main(String[] args) {
		System.out.println("My env: " + System.getenv(KAFKA_BOOTSTRAP_SERVERS_ENVNAME));
		KafkaMessager.send("test message-1!");
		KafkaMessager.send("test message-2!");
	}
}
