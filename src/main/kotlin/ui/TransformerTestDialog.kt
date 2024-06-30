package burp.ui

import burp.evalTransformer
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Font
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.*

class TransformerTestDialog(owner: Window?, transformer: String, value: String) : JDialog(owner) {
    private val nameLabel = JLabel("Output")
    private val output = JTextArea(10, 80).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
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
            layout = MigLayout("", "[fill][fill][fill][fill][fill]", "[][][][][][][]")
            add(nameLabel, "cell 0 0, wrap")
            add(JScrollPane(output), "grow, push, span")
            add(JPanel(MigLayout("fillx", "[fill][fill][fill][fill][fill]", "[fill]")).apply {
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
                }, "west,gapx null 10")
            }, "cell 0 4")
        }

        pack()
        setLocationRelativeTo(owner)
        defaultCloseOperation = DISPOSE_ON_CLOSE
    }
}
