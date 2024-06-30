package burp.ui

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import java.awt.Color
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.KeyStroke.getKeyStroke
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.max

class Editor(private val editorTyped: () -> Unit, private val transformerSave: () -> Unit) {
    val textArea = RSyntaxTextArea().apply {
        syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
        isCodeFoldingEnabled = true
        tabSize = 4
        isBracketMatchingEnabled = true
        currentLineHighlightColor = Color.LIGHT_GRAY
        caretColor = Color.BLACK
        isAutoIndentEnabled = true
        isEditable = true
        isEnabled = true

        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                editorTyped()
            }

            override fun removeUpdate(e: DocumentEvent) {
                editorTyped()
            }

            override fun changedUpdate(e: DocumentEvent) {
                editorTyped()
            }
        })

        inputMap.apply {
            put(getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "Save")
            put(getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "IncreaseFontSize")
            put(getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "IncreaseFontSize")
            put(getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "IncreaseFontSize")
            put(getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "DecreaseFontSize")
            put(
                getKeyStroke(
                    KeyEvent.VK_MINUS,
                    InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
                ), "DecreaseFontSize"
            )
            put(getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "DecreaseFontSize")
            put(getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "ResetFontSize")
            put(getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK), "ResetFontSize")
        }

        actionMap.apply {
            put("Save", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    transformerSave()
                }
            })
            put("IncreaseFontSize", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    font = Font(font.name, font.style, font.size + 1)
                }
            })
            put("DecreaseFontSize", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    font = Font(font.name, font.style, max(1, font.size - 1))
                }
            })
            put("ResetFontSize", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    font = Font(font.name, font.style, 12)
                }
            })
        }

        addMouseWheelListener { e ->
            if (e.isControlDown) {
                font = Font(font.name, font.style, max(1, font.size - e.wheelRotation))
            }
        }
    }
}
