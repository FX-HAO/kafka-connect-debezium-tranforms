# kafka-connect-debezium-tranforms

https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-temporal-types

This transformation converts the following debezium time types to a string.

* io.debezium.time.Date
* io.debezium.time.Time
* io.debezium.time.MicroTime
* io.debezium.time.Timestamp
* io.debezium.time.MicroTimestamp

For example, `1658912573473765` will be converted to `"2022-07-27T09:02:53.473765Z"`. And the 
corresponding schema type will be changed from 

```
{
    "type":"io.debezium.time.MicroTimestamp",
    "optional":true,
    "field":"created_at"
}
```

to

```
{
    "type":"string",
    "optional":true,
    "field":"created_at"
}
```

## Installation

```
mvn package
```

Then you can put the jar file `target/kafka-connect-debezium-transforms-1.0-SNAPSHOT.jar` 
under the plugin directory on your Kafka Connect worker.

## Configuration file

```
{
    "name": "test",
    "config": {
        ...
        "transforms": "convertTimeStamp",
        "transforms.convertTimeStamp.type": "com.github.haofuxin.kafka.connect.DebeziumTimestampConverter"
    }
}
```
