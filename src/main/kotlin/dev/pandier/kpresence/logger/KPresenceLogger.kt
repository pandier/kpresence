@file:Suppress("unused")

package dev.pandier.kpresence.logger

import java.util.logging.Level
import java.util.logging.Logger

public interface KPresenceLogger {
    public fun error(message: String, throwable: Throwable? = null)
    public fun warn(message: String, throwable: Throwable? = null)
    public fun info(message: String, throwable: Throwable? = null)
    public fun debug(message: String, throwable: Throwable? = null)

    public object Dummy : KPresenceLogger {
        override fun error(message: String, throwable: Throwable?) {}
        override fun warn(message: String, throwable: Throwable?) {}
        override fun info(message: String, throwable: Throwable?) {}
        override fun debug(message: String, throwable: Throwable?) {}
    }

    public class Java(private val logger: Logger = Logger.getLogger("KPresence")) : KPresenceLogger {
        override fun error(message: String, throwable: Throwable?) {
            logger.log(Level.SEVERE, message, throwable)
        }

        override fun warn(message: String, throwable: Throwable?) {
            logger.log(Level.WARNING, message, throwable)
        }

        override fun info(message: String, throwable: Throwable?) {
            logger.log(Level.INFO, message, throwable)
        }

        override fun debug(message: String, throwable: Throwable?) {
            logger.log(Level.FINE, message, throwable)
        }
    }
}
