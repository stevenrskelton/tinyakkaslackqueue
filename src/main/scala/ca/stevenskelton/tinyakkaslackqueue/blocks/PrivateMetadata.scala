package ca.stevenskelton.tinyakkaslackqueue.blocks

case class PrivateMetadata private(value: String) extends AnyVal {
  def block: String = s""""private_metadata": "$value""""
}

object PrivateMetadata {
  val Empty = PrivateMetadata("")
}