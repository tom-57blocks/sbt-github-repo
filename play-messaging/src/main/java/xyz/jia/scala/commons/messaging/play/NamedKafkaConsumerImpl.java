package xyz.jia.scala.commons.messaging.play;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * This annotation is used by <a href="https://github.com/google/guice">juice</a> when injecting
 * Kafka consumer instances.
 * <p>
 * Annotation classes defined in Scala are not stored in class files in a Java-compatible manner
 * and therefore not visible in Java reflection. This is why we have to write this annotation in Java.
 * Source => https://www.scala-lang.org/api/2.13.8/scala/annotation/StaticAnnotation.html
 */
public class NamedKafkaConsumerImpl implements NamedKafkaConsumer, Serializable {

  private final String value;

  private static final long serialVersionUID = 1L;

  public NamedKafkaConsumerImpl(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }

  public int hashCode() {
    // This is specified in java.lang.Annotation.
    return (127 * "value".hashCode()) ^ value.hashCode();
  }

  public boolean equals(Object o) {
    if (!(o instanceof NamedKafkaConsumer)) {
      return false;
    }

    NamedKafkaConsumer other = (NamedKafkaConsumer) o;
    return value.equals(other.value());
  }

  public String toString() {
    return "@" + NamedKafkaConsumer.class.getName() + "(value=" + value + ")";
  }

  public Class<? extends Annotation> annotationType() {
    return NamedKafkaConsumer.class;
  }
}
