FROM maven:3.8.6-jdk-11 AS builder
WORKDIR /app/
COPY ./ ./
RUN mvn clean install

FROM confluentinc/cp-kafka-connect:6.2.0
RUN confluent-hub install --no-prompt confluentinc/kafka-connect-jdbc:10.5.1
RUN confluent-hub install --no-prompt debezium/debezium-connector-postgresql:1.9.3
COPY --from=builder /app/target/*.jar /usr/share/confluent-hub-components/debezium-debezium-connector-postgresql/lib/
