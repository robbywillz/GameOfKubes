package com.gok.flink;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.util.Collector;

/**
 * Consumes message from Kafka and send the message to Flink for processing
 */
public class KafkaConsumer {
	private static String KAFKA_BOOTSTRAP_SERVERS_ENVNAME = "KAFKA_BOOTSTRAP_SERVERS";
	private static String KAFKA_BOOTSTRAP_SERVERS_DEFAULT = "kafka-service:9092";

	private static String kafkaBootstrapServers = null;

	public static void main(String[] args) throws Exception {

		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		Properties properties = new Properties();
		properties.setProperty("bootstrap.servers", getBootstrapServers());
		properties.setProperty("group.id", "test");
		FlinkKafkaConsumer010<String> consumer = new FlinkKafkaConsumer010<>("test", new SimpleStringSchema(),
				properties);
		consumer.setStartFromEarliest(); // start from the earliest record possible
		consumer.setStartFromLatest(); // start from the latest record
		consumer.setStartFromGroupOffsets(); // the default behaviour

		DataStream<String> stream = env.addSource(consumer);

		// parse the data, group it, window it, and aggregate the counts
		DataStream<MessageWithCount> alarmCounts = stream

				.flatMap(new FlatMapFunction<String, MessageWithCount>() {
					@Override
					public void flatMap(String value, Collector<MessageWithCount> out) {

						ObjectMapper mapper = new ObjectMapper();
						try {
							JsonNode jsonNode = mapper.readValue(value, JsonNode.class);
							String severity = jsonNode.get("severity").textValue();
							String alarmText = jsonNode.get("alarmText").textValue();
							out.collect(new MessageWithCount(value, severity, alarmText, 1L));
						} catch (IOException e) {
							System.out.println("Invalid JSON format :  " + value);
						}
					}
				})

				.keyBy("severity", "alarmText").timeWindow(Time.seconds(5))

				.reduce(new ReduceFunction<MessageWithCount>() {
					@Override
					public MessageWithCount reduce(MessageWithCount a, MessageWithCount b) {
						return new MessageWithCount(a.message, a.severity, a.alarmText, a.count + b.count);
					}
				});

		// print the results with a single thread, rather than in parallel
		alarmCounts.print().setParallelism(1);
		// windowCounts.writeAsText("/tmp/gokjobs.out",
		// WriteMode.NO_OVERWRITE).setParallelism(1);
		env.execute("GameOfKubes Kafka Consumer");
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

	public static class MessageWithCount {

		public String message;
		public String severity;
		public long count;
		public String alarmText;

		public MessageWithCount() {
		}

		public MessageWithCount(String message, String severity, String alarmText, long count) {
			this.message = message;
			this.severity = severity;
			this.alarmText = alarmText;
			this.count = count;
		}

		@Override
		public String toString() {
			return String.format("[message=%s, count=%s]", message, count);
		}
	}
}
