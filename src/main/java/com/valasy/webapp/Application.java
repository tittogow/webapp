package com.valasy.webapp;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.reactivestreams.client.MongoClients.create;
import static java.lang.System.exit;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.logging.Level.parse;
import static java.util.logging.Logger.getLogger;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createValue;
import static com.valasy.webapp.jes.elastic.Logging.logKafka;
import static com.valasy.webapp.jes.util.Configuration.loadDefault;
import static com.valasy.webapp.jes.util.Event.changed;
import static com.valasy.webapp.jes.util.JsonFields.COMMAND;
import static com.valasy.webapp.jes.util.JsonFields.ID;
import static com.valasy.webapp.jes.util.Mongo.addNotDeleted;
import static com.valasy.webapp.jes.util.Streams.start;
import static com.valasy.webapp.mongo.JsonClient.aggregationPublisher;
import static com.valasy.webapp.util.Collections.list;
import static com.valasy.webapp.util.Util.tryToDoWithRethrow;
import static com.valasy.webapp.util.Util.tryToGetSilent;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.typesafe.config.Config;
import java.util.concurrent.CompletionStage;
import java.util.function.IntUnaryOperator;
import java.util.logging.Level;
import javax.json.JsonObject;
import com.valasy.webapp.jes.Aggregate;
import com.valasy.webapp.jes.Reactor;
import com.valasy.webapp.jes.util.Streams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.reactivestreams.Publisher;

/**
 * A demo application for pincette-jes.
 *
 * @author Werner Donn\u00e9
 */
public class Application {
  private static final String AGGREGATE_TYPE = "counter";
  private static final String APP = "plusminus";
  private static final String DEV = "dev";
  private static final String ENVIRONMENT = "environment";
  private static final String INFO = "INFO";
  private static final String KAFKA = "kafka";
  private static final String LOG_LEVEL = "logLevel";
  private static final String LOG_TOPIC = "logTopic";
  private static final String MINUS = "minus";
  private static final String MONGODB_DATABASE = "mongodb.database";
  private static final String MONGODB_URI = "mongodb.uri";
  private static final String PLUS = "plus";
  private static final String VALUE = "value";
  private static final String VERSION = "1.0-SNAPSHOT";

  static StreamsBuilder createApp(
      final StreamsBuilder builder, final Config config, final MongoClient mongoClient) {
    final String environment = getEnvironment(config);
    final Level logLevel = getLogLevel(config);
    final Aggregate aggregate =
        new Aggregate()
            .withApp(APP)
            .withType(AGGREGATE_TYPE)
            .withEnvironment(environment)
            .withBuilder(builder)
            .withMongoDatabase(mongoClient.getDatabase(config.getString(MONGODB_DATABASE)))
            .withReducer(PLUS, (command, currentState) -> reduce(currentState, v -> v + 1))
            .withReducer(MINUS, (command, currentState) -> reduce(currentState, v -> v - 1));

    aggregate.build();
    tryToGetSilent(() -> config.getString(LOG_TOPIC))
        .ifPresent(topic -> logKafka(aggregate, logLevel, VERSION, topic));

    return builder;
  }

  private static CompletionStage<JsonObject> createCommand(final JsonObject event) {
    return completedFuture(createObjectBuilder().add(COMMAND, "plus").build());
  }

  @SuppressWarnings("squid:UnusedPrivateMethod") // Reactor example.
  private static StreamsBuilder createReactor(
      final StreamsBuilder builder, final Config config, final MongoClient mongoClient) {
    final String environment = getEnvironment(config);

    return new Reactor()
        .withBuilder(builder)
        .withEnvironment(environment)
        .withSourceType("plusminus-counter")
        .withDestinationType("plusminus-counter")
        .withDestinations(
            event ->
                getOthers(
                    event.getString(ID),
                    mongoClient.getDatabase(config.getString(MONGODB_DATABASE)),
                    environment))
        .withEventToCommand(Application::createCommand)
        .withFilter(event -> changed(event, "/value", createValue(9), createValue(10)))
        .build();
  }

  static String getEnvironment(final Config config) {
    return tryToGetSilent(() -> config.getString(ENVIRONMENT)).orElse(DEV);
  }

  private static Level getLogLevel(final Config config) {
    return parse(tryToGetSilent(() -> config.getString(LOG_LEVEL)).orElse(INFO));
  }

  static MongoClient getMongoClient(final Config config) {
    return create(config.getString(MONGODB_URI));
  }

  private static Publisher<JsonObject> getOthers(
      final String id, final MongoDatabase database, final String environment) {
    return aggregationPublisher(
        database.getCollection("plusminus-counter-" + environment),
        list(match(addNotDeleted(ne(ID, id))), project(include(ID))));
  }

  private static CompletionStage<JsonObject> reduce(
      final JsonObject currentState, final IntUnaryOperator op) {
    return completedFuture(
        createObjectBuilder(currentState)
            .add(VALUE, op.applyAsInt(currentState.getInt(VALUE, 0)))
            .build());
  }

  public static void main(final String[] args) {
    final Config config = loadDefault();

    tryToDoWithRethrow(
        () -> getMongoClient(config),
        client -> {
          final Topology topology = createApp(new StreamsBuilder(), config, client).build();

          getLogger(APP).log(Level.INFO, "Topology:\n\n {0}", topology.describe());

          if (!start(topology, Streams.fromConfig(config, KAFKA))) {
            exit(1);
          }
        });
  }
}
