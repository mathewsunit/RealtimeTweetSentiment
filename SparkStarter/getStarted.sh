#! /bin/bash
# This script does the following things
# 1. Install Kafka to local folder
# 2. Start Zookeeper and 3*Kafka instances
# 3. Enable collection on topic twitter
# 4. Install Elastisearch and start it
# 5. Install and start Kibana
# 6. Install and start Logstash
# Note: All instances are started in ~ daemon mode and log files for the different processes are also created

script_dir=$(dirname $0)
DAEMON_PATH_KAFKA=$script_dir/kafka/
DAEMON_PATH_ELASTICSEARCH=$script_dir/elasticsearch/
DAEMON_PATH_KIBANA=$script_dir/kibana/
DAEMON_PATH_LOGSTASH=$script_dir/logstash/

#Download Kafka to current folder
wget http://mirrors.ocf.berkeley.edu/apache/kafka/1.1.0/kafka_2.11-1.1.0.tgz &&
tar -xzf kafka_2.11-1.1.0.tgz -C $script_dir/
rm kafka_2.11-1.1.0.tgz
mv $script_dir/kafka_2.11-1.1.0 $script_dir/kafka
mv $script_dir/server1.properties $DAEMON_PATH_KAFKA/config/server1.properties
mv $script_dir/server2.properties $DAEMON_PATH_KAFKA/config/server2.properties

# Create the log files
touch kafka_log_0
touch kafka_log_1
touch kafka_log_2
touch zookeeper_log
touch elasticsearch_log
touch kibana_log
touch logstash_log

#Start Zookeeper
echo "Starting Zookeeper";
sudo nohup $DAEMON_PATH_KAFKA/bin/zookeeper-server-start.sh -daemon $DAEMON_PATH_KAFKA/config/zookeeper.properties 2> zookeeper_log &&
echo "Starting Kafka 0";
sudo nohup $DAEMON_PATH_KAFKA/bin/kafka-server-start.sh -daemon $DAEMON_PATH_KAFKA/config/server.properties 2> kafka_log_0 &&
echo "Starting Kafka 1";
sudo nohup $DAEMON_PATH_KAFKA/bin/kafka-server-start.sh -daemon $DAEMON_PATH_KAFKA/config/server1.properties 2> kafka_log_1 &&
echo "Starting Kafka 2";
sudo nohup $DAEMON_PATH_KAFKA/bin/kafka-server-start.sh -daemon $DAEMON_PATH_KAFKA/config/server2.properties 2> kafka_log_2 &&
echo "Done";

#Enable collection on topic
echo "Enable Collection on Zookeeper";
sudo nohup $DAEMON_PATH_KAFKA/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 3 --partitions 1 --topic $1 &&
echo "Done";

#Download Elasticsearch to current folder
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.6.8.tar.gz &&
tar -xzf elasticsearch-5.6.8.tar.gz -C $script_dir/
rm elasticsearch-5.6.8.tar.gz
mv $script_dir/elasticsearch-5.6.8 $script_dir/elasticsearch

echo "Starting Elasticsearch";
$DAEMON_PATH_ELASTICSEARCH/bin/elasticsearch >> elasticsearch_log &
echo "Done";


#Download Kibana to current folder
wget https://artifacts.elastic.co/downloads/kibana/kibana-5.6.8-linux-x86_64.tar.gz &&
tar -xzf kibana-5.6.8-linux-x86_64.tar.gz -C $script_dir/
rm kibana-5.6.8-linux-x86_64.tar.gz
mv $script_dir/kibana-5.6.8-linux-x86_64 $script_dir/kibana

echo "Starting Kibana";
$DAEMON_PATH_KIBANA/bin/kibana >> kibana_log &
echo "Done";


#Download LogStash to current folder
wget https://artifacts.elastic.co/downloads/logstash/logstash-5.6.8.tar.gz &&
tar -xzf logstash-5.6.8.tar.gz -C $script_dir/
rm logstash-5.6.8.tar.gz
mv $script_dir/logstash-5.6.8 $script_dir/logstash
mv $script_dir/logstash-simple.conf $DAEMON_PATH_LOGSTASH/config/logstash-simple.conf

echo "Starting LogStash";
$DAEMON_PATH_LOGSTASH/bin/logstash -f $DAEMON_PATH_LOGSTASH/config/logstash-simple.conf >> logstash_log &
echo "	Done";