#!/bin/sh

echo "Exporting necessary environment variables"
export KAFKA_BOOTSTRAP_SERVERS="localhost:19092"
export KAFKA_SCHEMA_REGISTRY="http://localhost:8081"

# The topic used to test Kafka plaintext messages
export PLAINTEXT_MESSAGES_TEST_TOPIC="plaintext-tests"

# The topic used to test Avro messages
export AVRO_MESSAGES_TEST_TOPIC="avro-test"

export TEST_DB_DRIVER="com.mysql.cj.jdbc.Driver"
export TEST_DB_URL="jdbc:mysql://localhost:3307/test_db"
export TEST_DB_USER="root"
export TEST_DB_PASSWORD=""

