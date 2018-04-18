package streaming

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class SentimentUtils extends Serializable {
  @transient private var pipeline: StanfordCoreNLP = _

  private def getOrCreatePipeline(): StanfordCoreNLP = {
    if (pipeline == null) {
      val props = new Properties()
      props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment")
      pipeline = new StanfordCoreNLP(props)
    }
    pipeline
  }

  def computeSentiment(text: String): Int = {
    val (_, sentiment) = extractSentiments(text)
      .maxBy { case (sentence, _) => sentence.length }
    sentiment
  }

  def normalizeCoreNLPSentiment(sentiment: Double): Int = {
    sentiment match {
      case s if s <= 0.0 => 0
      case s if s < 2.0 => -1
      case s if s < 3.0 => 0
      case s if s < 5.0 => 1
      case _ => 0
    }
  }

  def extractSentiments(text: String): List[(String, Int)] = {
    getOrCreatePipeline
    val annotation: Annotation = pipeline.process(text)
    val sentences = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
    sentences
      .map(sentence => (sentence, sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])))
      .map { case (sentence, tree) => (sentence.toString, normalizeCoreNLPSentiment(RNNCoreAnnotations.getPredictedClass(tree))) }
      .toList
  }

  def computeWeightedSentiment(tweet: String): Int = {
    val annotation = pipeline.process(tweet)
    val sentiments: ListBuffer[Double] = ListBuffer()
    val sizes: ListBuffer[Int] = ListBuffer()
    for (sentence <- annotation.get(classOf[CoreAnnotations.SentencesAnnotation])) {
      val tree = sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])
      val sentiment = RNNCoreAnnotations.getPredictedClass(tree)
      sentiments += sentiment.toDouble
      sizes += sentence.toString.length
    }
    val weightedSentiment = if (sentiments.isEmpty) {-1} else {
      val weightedSentiments = (sentiments, sizes).zipped.map((sentiment, size) => sentiment * size)
      weightedSentiments.sum / sizes.sum
    }
    normalizeCoreNLPSentiment(weightedSentiment)
  }
}
