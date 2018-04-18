package streaming

object Utils {

  import java.util.regex.Pattern

  val urlPattern = "(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?"
  val hashTagPattern = "\\B(\\#[a-zA-Z]+\\b)"

  def applyNormalizationTemplate(text: String, regex: String, normalizationString: String): String = {
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(text)
    val normalizedText = matcher.replaceAll(normalizationString)
    normalizedText
  }

  def cleanText(text: String): String = {
    applyNormalizationTemplate(applyNormalizationTemplate(text, urlPattern, ""), hashTagPattern, "")
  }
}
