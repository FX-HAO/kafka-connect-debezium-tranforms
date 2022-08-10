# kafka-connect-debezium-tranforms

https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-temporal-types

This transformation converts a unix epoch microsecond with type `io.debezium.time.MicroTime` into a string. 
For example, `1658912573473765` will be converted to `"2022-07-27T09:02:53.473765"`. And the 
corresponding schema type will be changed from `"type":"int64"` to `"type":"string"`.

## Installation

```
mvn package
```

Then you can put the jar file `target/kafka-connect-debezium-transforms-1.0-SNAPSHOT.jar` 
under the plugin directory on your Kafka Connect worker.

