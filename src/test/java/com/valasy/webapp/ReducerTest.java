package com.valasy.webapp;

import static com.valasy.webapp.jes.test.Test.run;
import static com.valasy.webapp.jes.util.Configuration.loadDefault;
import static com.valasy.webapp.jes.util.Streams.fromConfig;
import static com.valasy.webapp.Application.createApp;
import static com.valasy.webapp.Application.getMongoClient;
import static com.valasy.webapp.util.Util.tryToDoWithRethrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typesafe.config.Config;
import java.io.File;
import org.junit.jupiter.api.Test;

public class ReducerTest {
  private static final String ENVIRONMENT = "environment";
  private static final String KAFKA = "kafka";

  @Test
  public void test() {
    final Config config = loadDefault();

    tryToDoWithRethrow(
        () -> getMongoClient(config),
        client ->
            assertTrue(
                run(
                    new File("src/test/resources").toPath(),
                    fromConfig(config, KAFKA),
                    config.getString(ENVIRONMENT),
                    com.valasy.webapp.jes.test.Application::report,
                    builder -> createApp(builder, config, client))));
  }
}
