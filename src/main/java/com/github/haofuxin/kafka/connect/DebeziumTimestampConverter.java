package com.github.haofuxin.kafka.connect;

import org.apache.kafka.common.cache.Cache;
import org.apache.kafka.common.cache.LRUCache;
import org.apache.kafka.common.cache.SynchronizedCache;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SchemaUtil;

import io.debezium.time.MicroTimestamp;
import io.debezium.time.Date;
import io.debezium.time.Time;
import io.debezium.time.MicroTime;
import io.debezium.time.Timestamp;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

public class DebeziumTimestampConverter<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOG = LoggerFactory.getLogger(DebeziumTimestampConverter.class);

    private Cache<Schema, Schema> schemaUpdateCache;

    private static final String PURPOSE = "convert io.debezium.time.MicroTimestamp into String";

    @Override
    public void configure(Map<String, ?> props) {
        schemaUpdateCache = new SynchronizedCache<>(new LRUCache<Schema, Schema>(16));
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef();
    }

    @Override
    public void close() {

    }

    protected Schema operatingSchema(R record) {
        return record.valueSchema();
    }

    protected Object operatingValue(R record) {
        return record.value();
    }

    private String formatDate(Integer epoch) {
        if (epoch == null) {
            return "";
        }

        LocalDate d = LocalDate.ofEpochDay(epoch);
        return d.toString();
    }

    private String formatTime(Integer epoch) {
        if (epoch == null) {
            return "";
        }

        java.util.Date date = new java.util.Date(epoch);
        return new SimpleDateFormat("HH:mm:ss.SSS").format(date);
    }

    private String formatMicroTime(Long epochMicroSeconds) {
        if (epochMicroSeconds == null) {
            return "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS").withZone(ZoneId.from(ZoneOffset.UTC));
        long epochSeconds = epochMicroSeconds / 1000000L;
        long nanoOffset = ( epochMicroSeconds % 1000000L ) * 1000L ;
        Instant instant = Instant.ofEpochSecond( epochSeconds, nanoOffset );
        return formatter.format(instant);
    }

    private String formatTimestamp(Long epochMilliSeconds) {
        if (epochMilliSeconds == null) {
            return "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.from(ZoneOffset.UTC));
        Instant instant = Instant.ofEpochMilli( epochMilliSeconds );
        return formatter.format(instant);
    }

    private String formatMicroTimestamp(Long epochMicroSeconds) {
        if (epochMicroSeconds == null) {
            return "";
        }

        long epochSeconds = epochMicroSeconds / 1000000L;
        long nanoOffset = ( epochMicroSeconds % 1000000L ) * 1000L ;
        Instant instant = Instant.ofEpochSecond( epochSeconds, nanoOffset );
        return instant.toString();
    }

    private Schema makeUpdatedSchema(Schema schema) {
        final SchemaBuilder builder = SchemaUtil.copySchemaBasics(schema, SchemaBuilder.struct());

        for (Field field: schema.fields()) {
            if (field.schema().type() != Schema.Type.STRING && (
                    MicroTimestamp.SCHEMA_NAME.equals(field.schema().name()) ||
                    Date.SCHEMA_NAME.equals(field.schema().name()) ||
                    Time.SCHEMA_NAME.equals(field.schema().name()) ||
                    MicroTime.SCHEMA_NAME.equals(field.schema().name()) ||
                    Timestamp.SCHEMA_NAME.equals(field.schema().name()))) {
                builder.field(field.name(), Schema.OPTIONAL_STRING_SCHEMA);
            } else {
                builder.field(field.name(), field.schema());
            }
        }

        return builder.build();
    }

    protected R newRecord(R record, Schema updatedSchema, Object updatedValue) {
        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                updatedSchema,
                updatedValue,
                record.timestamp()
        );
    }

    private R applyWithSchema(R r) {
        final Struct struct = requireStruct(operatingValue(r), PURPOSE);

        Schema updatedSchema = schemaUpdateCache.get(struct.schema());
        if(updatedSchema == null) {
            updatedSchema = makeUpdatedSchema(struct.schema());
            schemaUpdateCache.put(struct.schema(), updatedSchema);
        }

        final Struct updatedValue = new Struct(updatedSchema);

        for (Field field : struct.schema().fields()) {
            if (field.schema().type() != Schema.Type.STRING && field.schema().name() != null) {
                switch (field.schema().name()) {
                    case Date.SCHEMA_NAME:
                        Object value = struct.get(field);
                        if (value == null) {
                            updatedValue.put(field.name(), null);
                            continue;
                        }

                        if (value instanceof Integer) {
                            updatedValue.put(field.name(), formatDate((Integer)value));
                        } else {
                            updatedValue.put(field.name(), value);
                        }
                        break;
                    case Time.SCHEMA_NAME:
                        value = struct.get(field);
                        if (value == null) {
                            updatedValue.put(field.name(), null);
                            continue;
                        }

                        if (value instanceof Integer) {
                            updatedValue.put(field.name(), formatTime((Integer)value));
                        } else {
                            updatedValue.put(field.name(), value);
                        }
                        break;
                    case MicroTime.SCHEMA_NAME:
                        value = struct.get(field);
                        if (value == null) {
                            updatedValue.put(field.name(), null);
                            continue;
                        }

                        if (value instanceof Long) {
                            updatedValue.put(field.name(), formatMicroTime((Long)value));
                        } else {
                            updatedValue.put(field.name(), value);
                        }
                        break;
                    case Timestamp.SCHEMA_NAME:
                        value = struct.get(field);
                        if (value == null) {
                            updatedValue.put(field.name(), null);
                            continue;
                        }

                        if (value instanceof Long) {
                            updatedValue.put(field.name(), formatTimestamp((Long)value));
                        } else {
                            updatedValue.put(field.name(), value);
                        }
                        break;
                    case MicroTimestamp.SCHEMA_NAME:
                        value = struct.get(field);
                        if (value == null) {
                            updatedValue.put(field.name(), null);
                            continue;
                        }

                        if (value instanceof Long) {
                            updatedValue.put(field.name(), formatMicroTimestamp((Long)value));
                        } else {
                            updatedValue.put(field.name(), value);
                        }
                        break;
                }
            } else {
                updatedValue.put(field.name(), struct.get(field));
            }
        }

        return newRecord(r, updatedSchema, updatedValue);
    }

    @Override
    public R apply(R record) {
        if (operatingSchema(record) == null) {
            return record;
        } else {
            return applyWithSchema(record);
        }
    }
}
