package xyz.jia.scala.commons.messaging.play;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface NamedKafkaProducer {
  String value();
}
