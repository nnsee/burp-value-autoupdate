package burp

import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.Charsets.UTF_8

val log = Logger()

class Logger {
    private var level = Level.INFO
    private var stdOut: OutputStream? = null
    private var stdErr: OutputStream? = null
    private var df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

    enum class Level {
        DEBUG, INFO, ERROR, FATAL
    }

    private fun print(message: String) {
        writeMessage(message, stdOut)
    }

    private fun err(message: String) {
        writeMessage(message, stdErr)
    }

    private fun writeMessage(message: String, where: OutputStream?) {
        if (where == null) {
            return
        }
        val date = df.format(Date())
        try {
            where.write(String.format("[%s] %s\n", date, message).toByteArray(UTF_8))
        } catch (ignored: IOException) {
        }
    }

    fun registerStreams(outStream: OutputStream?, errStream: OutputStream?) {
        stdOut = outStream
        stdErr = errStream
    }

    fun setLevel(newLevel: Level) {
        level = newLevel
    }

    fun debug(message: String) {
        if (level == Level.DEBUG) {
            print(message)
        }
    }

    fun info(message: String) {
        if (level <= Level.INFO) {
            print(message)
        }
    }

    fun error(message: String) {
        if (level <= Level.ERROR) {
            err(message)
        }
    }

    fun fatal(message: String) {
        if (level <= Level.FATAL) {
            err(message)
        }
    }
}