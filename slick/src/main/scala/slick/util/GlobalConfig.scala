package slick.util

import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import com.typesafe.config._
import java.util.concurrent.TimeUnit
import java.util.Properties
import scala.collection.mutable

/** Singleton object with Slick's configuration, loaded from the application config.
  * This includes configuration for the global driver objects and settings for debug logging. */
object GlobalConfig {
  private[this] val config = ConfigFactory.load()

  /** Dump individual `Select` and `Ref` nodes instead of a single `Path` */
  val dumpPaths = config.getBoolean("slick.dumpPaths")

  /** Use ANSI color sequences in tree dumps */
  val ansiDump = config.getBoolean("slick.ansiDump")

  /** Use Unixode box characters in table dumps */
  val unicodeDump = config.getBoolean("slick.unicodeDump")

  /** Use multi-line, indented formatting for SQL statements */
  val sqlIndent = config.getBoolean("slick.sqlIndent")

  /** Get a `Config` object for a Slick driver */
  def driverConfig(name: String): Config = {
    val path = "slick.driver." + name
    if(config.hasPath(path)) config.getConfig(path) else ConfigFactory.empty()
  }
}

/** Extension methods to make Typesafe Config easier to use */
class ConfigExtensionMethods(val c: Config) extends AnyVal {
  import scala.collection.JavaConverters._

  def getBooleanOr(path: String, default: => Boolean = false) = if(c.hasPath(path)) c.getBoolean(path) else default
  def getIntOr(path: String, default: => Int = 0) = if(c.hasPath(path)) c.getInt(path) else default
  def getStringOr(path: String, default: => String = null) = if(c.hasPath(path)) c.getString(path) else default
  def getConfigOr(path: String, default: => Config = ConfigFactory.empty()) = if(c.hasPath(path)) c.getConfig(path) else default

  def getMillisecondsOr(path: String, default: => Long = 0L) = if(c.hasPath(path)) c.getDuration(path, TimeUnit.MILLISECONDS) else default
  def getDurationOr(path: String, default: => Duration = Duration.Zero) =
    if(c.hasPath(path)) Duration(c.getDuration(path, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS) else default

  def getPropertiesOr(path: String, default: => Properties = null): Properties =
    if(c.hasPath(path)) new ConfigExtensionMethods(c.getConfig(path)).toProperties else default

  def toProperties: Properties = {
    def toProps(m: mutable.Map[String, ConfigValue]): Properties = {
      val props = new Properties(null)
      m.foreach { case (k, cv) =>
        val v =
          if(cv.valueType() == ConfigValueType.OBJECT) toProps(cv.asInstanceOf[ConfigObject].asScala)
          else if(cv.unwrapped eq null) null
          else cv.unwrapped.toString
        if(v ne null) props.put(k, v)
      }
      props
    }
    toProps(c.root.asScala)
  }

  def getBooleanOpt(path: String): Option[Boolean] = if(c.hasPath(path)) Some(c.getBoolean(path)) else None
  def getIntOpt(path: String): Option[Int] = if(c.hasPath(path)) Some(c.getInt(path)) else None
  def getStringOpt(path: String) = Option(getStringOr(path))
  def getPropertiesOpt(path: String) = Option(getPropertiesOr(path))
}

object ConfigExtensionMethods {
  @inline implicit def configExtensionMethods(c: Config): ConfigExtensionMethods = new ConfigExtensionMethods(c)
}
