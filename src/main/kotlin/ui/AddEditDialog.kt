package burp.ui

import burp.*
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Font
import java.awt.Window
import java.awt.event.ItemEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import javax.swing.*

class AddEditDialog(
    owner: Window,
    private var index: Int,
    private val itemStore: ItemStore,
    private val transformerStore: TransformerStore,
    private val reloadValuesTable: (Items) -> Unit,
    private val rowToName: (Int) -> String,
    private val tableLen: () -> Int,
) : JDialog(owner) {

    private val headerTypeHint = "Matches header names and replaces values "
    private val regexTypeHint = "Uses regex for matches (named group: val)"

    private val nameField = JTextField().apply {
        addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                this@AddEditDialog.keyTyped()
            }
        })
    }

    private val matchField = JTextField().apply {
        addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                this@AddEditDialog.keyTyped()
            }
        })
    }

    private val headerButton = JRadioButton("Header").apply {
        isSelected = true
        addItemListener { e: ItemEvent ->
            applyButton.isEnabled = true
            if (e.stateChange == ItemEvent.SELECTED) {
                typeDescription.text = headerTypeHint
            }
        }
    }

    private val regexButton = JRadioButton("Regex").apply {
        addItemListener { e: ItemEvent ->
            applyButton.isEnabled = true
            if (e.stateChange == ItemEvent.SELECTED) {
                typeDescription.text = regexTypeHint
            }
        }
    }

    private val typeDescription = JLabel(headerTypeHint)

    private val transformerComboBox = JComboBox<String>()

    private val errorLabel = JLabel().apply {
        foreground = Color.RED
        text = " "
    }

    private val applyButton = JButton("Apply").apply {
        isEnabled = false
        addActionListener { apply() }
    }

    init {
        ButtonGroup().apply {
            add(headerButton)
            add(regexButton)
        }

        contentPane.apply {
            layout = MigLayout("wrap, ins 20 20 20 20", "[][grow]", "[]")
            add(JLabel("Name"))
            add(nameField, "growx")
            add(JLabel("Match"))
            add(matchField, "growx")
            add(JLabel("Type"))
            add(headerButton, "split 2")
            add(regexButton)
            add(typeDescription, "skip 1, growx")
            add(JLabel("Transformer"))
            add(transformerComboBox, "growx")
            add(errorLabel, "push, span, aligny bottom")
            add(JPanel(MigLayout("wrap, ins 0", "[][][]", "[]")).apply {
                add(JButton("OK").apply {
                    background = UIManager.getColor("Button.background")
                    font = font.deriveFont(font.style or Font.BOLD)
                    addActionListener { ok() }
                })
                add(applyButton, "pushx")
                add(JButton("Cancel").apply {
                    addActionListener { cancel() }
                }, "alignx right, growx")
            }, "span, growx")
        }

        pack()
        setLocationRelativeTo(owner)
        loadTransformers()
        if (index != -1) {
            loadStoredValues()
        }

        defaultCloseOperation = DISPOSE_ON_CLOSE
    }

    private fun loadTransformers() {
        transformerComboBox.addItem("")
        transformerStore.items.forEach {
            transformerComboBox.addItem(it.key)
        }
        transformerComboBox.selectedIndex = 0
    }

    private fun loadStoredValues() {
        val name = rowToName(index)
        val item = itemStore.items[name]
        if (item == null) {
            Log.debug("Failed to get name for row $index")
            return
        }
        nameField.text = name
        matchField.text = item.match
        when (item.type) {
            ItemType.HEADER -> headerButton.isSelected = true
            ItemType.REGEX -> regexButton.isSelected = true
        }

        transformerComboBox.selectedItem = item.transformer

        nameField.isEnabled = false
    }

    private fun keyTyped() {
        applyButton.isEnabled = true
        errorLabel.text = " "
    }

    private fun ok() {
        if (apply()) this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }

    private fun apply(): Boolean {
        applyButton.isEnabled = false
        val name = nameField.text
        val match = matchField.text
        val type = if (regexButton.isSelected) ItemType.REGEX else ItemType.HEADER
        val transformer = transformerComboBox.selectedItem as String

        if (name.isEmpty()) {
            errorLabel.text = "Name field cannot be left blank!"
            return false
        }

        if (match.isEmpty()) {
            errorLabel.text = "Match field cannot be left blank!"
            return false
        }

        if (type == ItemType.REGEX) {
            val err = checkRegexSyntax(match)
            if (err.isNotEmpty()) {
                errorLabel.text = err
                return false
            }
        }

        if (index == -1) {
            val item = Item(match, type, "", true, 0, 0, transformer)
            if (itemStore.items[name] != null) {
                errorLabel.text = "Entry named \"$name\" already exists!"
                return false
            }
            itemStore.items[name] = item
            index = tableLen()
            Log.debug("New item at index $index: $item")
        } else {
            val item = itemStore.items[rowToName(index)]
            if (item == null) {
                Log.debug("Failed to get name for row $index")
                return false
            }
            item.match = match
            item.type = type
            item.transformer = transformer
            Log.debug("Edited item at index $index: $item")
        }

        nameField.isEnabled = false

        itemStore.save()
        reloadValuesTable(itemStore.items)

        return true
    }

    private fun cancel() {
        this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }
}
