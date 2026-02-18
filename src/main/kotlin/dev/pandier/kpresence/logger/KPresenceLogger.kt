@file:Suppress("unused")

package dev.pandier.kpresence.logger

import java.util.Date

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

    public class Println(
        private val debug: Boolean = false,
        private val format: String = $$"%1$tF %1$tT.%1$tL [%2$-5s] %3$s",
    ) : KPresenceLogger {
        override fun error(message: String, throwable: Throwable?) {
            log("ERROR", message, throwable)
        }

        override fun warn(message: String, throwable: Throwable?) {
            log("WARN", message, throwable)
        }

        override fun info(message: String, throwable: Throwable?) {
            log("INFO", message, throwable)
        }

        override fun debug(message: String, throwable: Throwable?) {
            if (debug) {
                log("DEBUG", message, throwable)
            }
        }

        private fun log(level: String, message: String, throwable: Throwable?) {
            println(format.format(Date(), level, message))
            throwable?.printStackTrace(System.out)
        }
    }
}
