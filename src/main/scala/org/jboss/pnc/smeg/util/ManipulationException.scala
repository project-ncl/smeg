package org.jboss.pnc.smeg.util

class ManipulationException(message: String, cause: Throwable) extends Exception(message, cause) {

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this(Option(cause).map(_.toString).orNull, cause)

  def this() = this(null: String, null)
}
