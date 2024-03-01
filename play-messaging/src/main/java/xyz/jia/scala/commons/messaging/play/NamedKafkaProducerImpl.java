package xyz.jia.scala.commons.messaging.play;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * This annotation is used by <a href="https://github.com/google/guice">juice</a> when injecting
 * Kafka producer instances.
 * <p>
 * Annotation classes defined in Scala are not stored in class files in a Java-compatible manner
 * and therefore not visible in Java reflection. This is why we have to write this annotation in Java.
 * Source => https://www.scala-lang.org/api/2.13.8/scala/annotation/StaticAnnotation.html
 */
public class NamedKafkaProducerImpl implements NamedKafkaProducer, Serializable {

  private final String value;

  private static final long serialVersionUID = 1L;

  public NamedKafkaProducerImpl(String value) {
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
    if (!(o instanceof NamedKafkaProducer)) {
      return false;
    }

    NamedKafkaProducer other = (NamedKafkaProducer) o;
    return value.equals(other.value());
  }

  public String toString() {
    return "@" + NamedKafkaProducer.class.getName() + "(value=" + value + ")";
  }

  public Class<? extends Annotation> annotationType() {
    return NamedKafkaProducer.class;
  }
}
