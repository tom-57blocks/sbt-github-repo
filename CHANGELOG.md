## [Unreleased]

- Allow handling of HTTP invocable task processing outcomes

## 1.0.0

- Introduce `messaging` module
    - Add a Kafka Avro producer util
    - Add a Kafka Avro consumer util
- Introduce `play-messaging` module
    - Make it possible to inject an `Avro` `Producer` in a Play! application
    - Auto-discover Kafka Avro consumers and initialize streaming
    - Introduce avro codec for `JsValue`s
- Introduce `play-utils` module
    - Introduce a helper to generate UUIDs
    - Introduce a utility to generate JSON formatted responses for errors in controllers
    - Introduce a utility to JSON serialize/deserialize a superclass from/to its subclasses
    - Introduce typical Jia Service HTTP response payload
    - Introduce filter to add commonly required `HTTP` request data
    - Introduce `requestId` and `processingTime` computation helpers
    - Introduce helper for generating standardized `HTTP` responses
    - Introduce helper for creating path and request binders from `Enumeration`s
    - Introduce a helper for parsing `JSON` from `HTTP` requests in controllers
    - Introduce utility for logging masked HTTP request details
    - Introduce utility for logging masked Config details
    - Introduce utility for reading files and resources
    - Introduce utility to convert a `JsError` to `ErrorMessages`
    - Introduce utility to support execution of tasks via HTTP requests
    - Introduce utility to interact with REST APIs
    - Introduce HTTP circuit breaker utility
    - Introduce utils for reading application JSON config from a given directory
- Introduce `play-datasource` module
    - Introduce a profile to ensure that `java.time.LocalDateTime` are stored in columns of
      type `DATETIME` in the MariaDB and MySQL
    - Add utility for reading and setting datetime DB query parameters
- Introduce `play-test` module
    - Introduce utility — `WithDelay` — to help execute a code block after advancing the clock by a
      specified duration
    - Introduce utility — `DbSpec` — to help when writing tests for Database Access Objects
    - Introduce utility for testing recursive methods
- Introduce `utils` module
    - Introduce a helper to generate checksum
    - Introduce a helper to generate UUIDs
    - Introduce a helper for safely working with dates with timezone awareness
    - Introduce utility for reading files and resources
    - Introduce utility for getting the start and end of a given month
