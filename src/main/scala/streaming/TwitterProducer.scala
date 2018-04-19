package streaming

import java.util.{HashMap, Properties}

import com.typesafe.config.ConfigFactory
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import streaming.Utils._
import twitter4j.Status
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationBuilder

object TwitterProducer {
  def main(args: Array[String]) {

    if (args.length < 2) {
      System.out.println("Usage: TwitterProducer1 <KafkaTopic> <keyword1>")
      return
    }

    val topic = args(0).toString
    val filters = args.slice(1, args.length)
    val kafkaBrokers = "localhost:9092,localhost:9093,localhost:9094"
    val confFactory = ConfigFactory.load()

    val sparkConfiguration = new SparkConf().
      setAppName("spark-twitter-stream").
      setMaster(sys.env.get("spark.master").getOrElse("local[*]"))
    val sparkContext = new SparkContext(sparkConfiguration)

    val config = new ConfigurationBuilder()
      .setOAuthConsumerKey(confFactory.getString("oath.consumerKey"))
      .setOAuthConsumerSecret(confFactory.getString("oath.consumerSecret"))
      .setOAuthAccessToken(confFactory.getString("oath.accessToken"))
      .setOAuthAccessTokenSecret(confFactory.getString("oath.accessTokenSecret"))
      .build
    val auth = new OAuthAuthorization(config)

    val streamingContext = new StreamingContext(sparkContext, Seconds(5))
    val tweets: DStream[Status] = TwitterUtils.createStream(streamingContext, Some(auth), filters)

    val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, parse, sentiment")
    val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)
    val utils = new SentimentUtils

    val textAndSentences: DStream[String] =
    tweets.filter(x => x.getLang == "en").
      map(_.getText).
      map(tweetText => (utils.computeSentiment(clean(tweetText)).toString))

    textAndSentences.print()
    textAndSentences.foreachRDD( rdd => {
      rdd.foreachPartition( partition => {
        val producerProps = new HashMap[String, Object]()
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")
        val producer = new KafkaProducer[String, String](producerProps)
        partition.foreach( record => {
          val message = new ProducerRecord[String, String](topic, null, record)
          producer.send(message)
        } )
        producer.close()
      })
    })

    streamingContext.start()
    streamingContext.awaitTermination()
  }

}
