package streaming

import java.util.regex.Pattern

import com.vdurmont.emoji.EmojiParser

object Utils {
  def applyNormalizationTemplate(text: String, regex: String, normalizationString: String): String = {
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(text)
    val normalizedText = matcher.replaceAll(normalizationString)

    normalizedText
  }

  def removePunctuationAndSpecialChars(text: String): String = {
    val regex = "[\\.\\,\\:\\-\\!\\?\\n\\t,\\%\\#\\*\\|\\=\\(\\)\\\"\\>\\<\\/]"
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(text)
    val cleanedText = matcher.replaceAll(" ").split("[ ]+").mkString(" ")
    cleanedText
  }

  def normalizeCurrencySymbol(text: String): String = {
    val regex = "[\\$\\€\\£]"
    applyNormalizationTemplate(text, regex, "")
  }

  def normalizeEmonicon(text: String): String = {
    val emonicons = List(":-)", ":)", ":D", ":o)", ":]", ":3", ":c)", ":>", "=]", "8)")
    val regex = "(" + emonicons.map(Pattern.quote).mkString("|") + ")"
    applyNormalizationTemplate(text, regex, "")
  }

  def normalizeNumbers(text: String): String = {
    val regex = "\\d+"
    applyNormalizationTemplate(text, regex, "")
  }

  def normalizeHashTags(text: String): String = {
    val regex = "\\B(\\#[a-zA-Z]+\\b)"
    applyNormalizationTemplate(text, regex, "")
  }

  def normalizeRT(text: String): String = {
    val regex = "\\B(\\@[a-zA-Z]+\\b)"
    applyNormalizationTemplate(text, regex, "")
  }

  def normalizeURL(text: String): String = {
    val regex = "(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?"
    applyNormalizationTemplate(text, regex, "")
  }

  def normalizeEmailAddress(text: String): String = {
    val regex = "\\w+(\\.|-)*\\w+@.*\\.(com|de|uk)"
    applyNormalizationTemplate(text, regex, "")
  }

  def removeHTMLCharacterEntities(text: String): String = {
    val HTMLCharacterEntities = List("&lt;", "&gt;", "&amp;", "&cent;", "&pound;", "&yen;", "&euro;", "&copy;", "&reg;")
    val regex = "(" + HTMLCharacterEntities.map(x => "\\" + x).mkString("|") + ")"
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(text)
    val cleanedText = matcher.replaceAll("")
    cleanedText
  }

  /**
    * First normalizes the `text` and then removes unwanted characters from it.
    */
  def clean(text: String): String = {
    List(text).map(text => text.toLowerCase())
      .map(normalizeEmonicon)
      .map(EmojiParser.parseToAliases)
      .map(normalizeURL)
      .map(normalizeRT)
      .map(normalizeHashTags)
      .map(normalizeEmailAddress)
      .map(normalizeCurrencySymbol)
      .map(removeHTMLCharacterEntities)
      .map(normalizeNumbers)
      .map(removePunctuationAndSpecialChars)
      .map(_.trim)
      .head
  }
}
