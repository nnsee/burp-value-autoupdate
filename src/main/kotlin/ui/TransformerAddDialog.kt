package burp.ui

import burp.TransformerStore
import burp.Transformers
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Font
import java.awt.Window
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import javax.swing.*

class TransformerAddDialog(
    owner: Window,
    private val transformerStore: TransformerStore,
    private val reloadTransformersTable: (Transformers) -> Unit
) : JDialog(owner) {
    private val nameField = JTextField().apply {
        addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                showError(" ")
            }
        })
    }

    private val errorLabel = JLabel(" ").apply {
        foreground = Color.RED
    }

    init {
        contentPane.apply {
            layout = MigLayout("wrap, ins 20 20 20 20", "[][grow]", "[]")
            add(JLabel("Name"))
            add(nameField, "growx")
            add(JLabel("Language"))
            add(JComboBox<String>().apply { addItem("JavaScript") }, "growx")
            add(errorLabel, "push, span, aligny bottom")
            add(JPanel(MigLayout("wrap, ins 0", "[][]", "[]")).apply {
                add(JButton("OK").apply {
                    background = UIManager.getColor("Button.background")
                    font = font.deriveFont(font.style or Font.BOLD)
                    addActionListener { if (apply()) close() }
                }, "pushx")
                add(JButton("Cancel").apply {
                    addActionListener { close() }
                }, "alignx right, growx")
            }, "span, growx")
        }

        pack()
        setLocationRelativeTo(owner)
        defaultCloseOperation = DISPOSE_ON_CLOSE
    }

    private fun close() {
        dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }

    private fun apply(): Boolean {
        val name = nameField.text

        if (name == "") {
            showError("Name field cannot be left blank!")
            return false
        }

        if (transformerStore.transformers[name] != null) {
            showError("Transformer named \"$name\" already exists!")
            return false
        }
        transformerStore.transformers[name] = TRANSFORMER_EDITOR_HINT

        transformerStore.save()
        reloadTransformersTable(transformerStore.transformers)

        return true
    }

    private fun showError(err: String) {
        errorLabel.text = err
    }
}
