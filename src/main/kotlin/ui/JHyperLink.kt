package ui

import burp.Log
import java.awt.Cursor
import java.awt.Desktop
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.TextAttribute
import java.io.IOException
import java.net.URI
import javax.swing.JLabel
import javax.swing.SwingUtilities
import javax.swing.UIManager

class JHyperLink(
    linkText: String,
    uri: String
) : JLabel(linkText) {

    private var linkUri: URI = URI.create(uri)

    init {
        toolTipText = linkUri.toString()
        foreground = UIManager.getColor("Component.linkColor")

        UIManager.addPropertyChangeListener { e ->
            if ("lookAndFeel" == e.propertyName) {
                SwingUtilities.invokeLater {
                    foreground = UIManager.getColor("Component.linkColor")
                }
            }
        }

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                open(linkUri)
            }

            override fun mouseEntered(e: MouseEvent) {
                cursor = Cursor(Cursor.HAND_CURSOR)
                setUnderline(true)
            }

            override fun mouseExited(e: MouseEvent) {
                cursor = Cursor(Cursor.DEFAULT_CURSOR)
                setUnderline(false)
            }
        })
    }

    private fun setUnderline(to: Boolean) {
        val curFont = font
        val attributes = curFont.attributes.toMutableMap()
        attributes[TextAttribute.UNDERLINE] = if (to) TextAttribute.UNDERLINE_ON else -1
        font = curFont.deriveFont(attributes)
    }

    companion object {
        fun open(uri: URI) {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(uri)
                } catch (e: IOException) {
                    Log.error("Error when trying to open link: $uri", e)
                }
            } else {
                Log.error("Attempted to open URI $uri, but Desktop is not supported")
            }
        }
    }
}
