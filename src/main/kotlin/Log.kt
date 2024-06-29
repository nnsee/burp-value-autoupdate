package burp

import burp.api.montoya.MontoyaApi
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused")

object Log {
    private var level = Level.INFO
    private var df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    private lateinit var api: MontoyaApi

    fun init(api: MontoyaApi) {
        this.api = api
    }

    enum class Level {
        DEBUG, INFO, ERROR, FATAL
    }

    private fun log(message: String, isError: Boolean = false) {
        if (isError) {
            api.logging().logToError(formatMessage(message))
        } else {
            api.logging().logToOutput(formatMessage(message))
        }
    }

    private fun log(message: String, throwable: Throwable) {
        api.logging().logToError(formatMessage(message))
        api.logging().logToError(throwable)
    }

    private fun formatMessage(message: String): String {
        val date = df.format(Date())
        // add date to each line in message
        return message.split("\n").joinToString("\n") { "[$date] $it" }
    }

    fun setLevel(newLevel: Level) {
        level = newLevel
    }

    fun debug(message: String) {
        if (level == Level.DEBUG) {
            log(message)
        }
    }

    fun info(message: String) {
        if (level <= Level.INFO) {
            log(message)
        }
    }

    fun error(message: String) {
        if (level <= Level.ERROR) {
            log(message, true)
        }
    }

    fun fatal(message: String) {
        if (level <= Level.FATAL) {
            log(message, true)
        }
    }

    fun error(message: String, throwable: Throwable) {
        if (level <= Level.ERROR) {
            log(message, throwable)
        }
    }

    fun fatal(message: String, throwable: Throwable) {
        if (level <= Level.FATAL) {
            log(message, throwable)
        }
    }

    // handle printstreams here because burp output() and error() are deprecated
    fun output() = object : PrintStream(System.out) {
        override fun println(x: Any?) {
            info(x.toString())
        }
    }

    fun error() = object : PrintStream(System.err) {
        override fun println(x: Any?) {
            if (x is Throwable) {
                error(x.message ?: "Unknown error", x)
            } else
                error(x.toString())
        }
    }
}
