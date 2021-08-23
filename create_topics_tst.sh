kafka-topics --command-config $HOME/.ccloud/config_tst --bootstrap-server pkc-l6y8e.eu-central-1.aws.confluent.cloud:9092 --create --topic plusminus-counter-command-tst --partitions 1 --replication-factor 3
kafka-topics --command-config $HOME/.ccloud/config_tst --bootstrap-server pkc-l6y8e.eu-central-1.aws.confluent.cloud:9092 --create --topic plusminus-counter-reply-tst --partitions 1 --replication-factor 3
kafka-topics --command-config $HOME/.ccloud/config_tst --bootstrap-server pkc-l6y8e.eu-central-1.aws.confluent.cloud:9092 --create --topic plusminus-counter-event-tst --partitions 1 --replication-factor 3
kafka-topics --command-config $HOME/.ccloud/config_tst --bootstrap-server pkc-l6y8e.eu-central-1.aws.confluent.cloud:9092 --create --topic plusminus-counter-event-full-tst --partitions 1 --replication-factor 3
kafka-topics --command-config $HOME/.ccloud/config_tst --bootstrap-server pkc-l6y8e.eu-central-1.aws.confluent.cloud:9092 --create --topic plusminus-counter-aggregate-tst --partitions 1 --replication-factor 3
kafka-topics --command-config $HOME/.ccloud/config_tst --bootstrap-server pkc-l6y8e.eu-central-1.aws.confluent.cloud:9092 --create --topic plusminus-counter-monitor-tst --partitions 1 --replication-factor 3
kafka-topics --command-config $HOME/.ccloud/config_tst --bootstrap-server pkc-l6y8e.eu-central-1.aws.confluent.cloud:9092 --create --topic audit-tst --partitions 1 --replication-factor 3
