package burp.ui

import burp.evalTransformer
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Font
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.*

class TransformerTestDialog(owner: Window, transformer: String, value: String) : JDialog(owner) {
    private val nameLabel = JLabel("Output")
    private val output = JTextArea(10, 80).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
        isEditable = false
        lineWrap = true

        val transformed = evalTransformer(value, transformer)
        if (transformed.err == "") {
            text = transformed.out
        } else {
            nameLabel.text = "Error"
            nameLabel.foreground = Color.RED
            text = transformed.err
        }
    }

    init {
        contentPane.apply {
            layout = MigLayout("wrap, ins 20 20 20 20", "[grow]", "[]")
            add(nameLabel)
            add(JScrollPane(output), "grow, push")
            add(JPanel(MigLayout("wrap, ins 0", "[]", "[]")).apply {
                add(JButton("OK").apply {
                    background = UIManager.getColor("Button.background")
                    font = font.deriveFont(font.style or Font.BOLD)
                    addActionListener {
                        this@TransformerTestDialog.dispatchEvent(
                            WindowEvent(
                                this@TransformerTestDialog, WindowEvent.WINDOW_CLOSING
                            )
                        )
                    }
                })
            }, "span, growx")
        }

        pack()
        setLocationRelativeTo(owner)
        defaultCloseOperation = DISPOSE_ON_CLOSE
    }
}
