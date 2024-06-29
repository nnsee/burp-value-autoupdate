package burp

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ByteArray
import burp.api.montoya.core.ToolType
import burp.api.montoya.core.ToolType.*
import burp.api.montoya.persistence.Preferences
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Container
import java.awt.Font
import java.awt.Window
import java.awt.event.*
import javax.swing.*
import javax.swing.JOptionPane.showMessageDialog
import javax.swing.SwingUtilities.getWindowAncestor
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

val VALUES_TABLE = JTable().apply {
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
    model = object : DefaultTableModel(
        arrayOf(), arrayOf(
            "", "Name", "Match", "Last value", "Times updated", "Times replaced", "Transformer"
        )
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> {
            if (columnIndex == 0) return Boolean::class.javaObjectType
            return String::class.javaObjectType
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0  // enabled button
        }
    }

    columnModel.getColumn(0).preferredWidth = 25
}

val TRANSFORMER_TABLE = JTable().apply {
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    model = object : DefaultTableModel(
        arrayOf(), arrayOf(
            "Name"
        )
    ) {
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return false
        }
    }
}

const val TAB_NAME = "Value Tracker"
var TAB_VISIBLE = false
var namedRows = mapOf<String, Int>()
var namedTRows = mapOf<String, Int>()

fun updated(name: String, value: String, count: Int) {
    if (!TAB_VISIBLE) return
    val dtm = VALUES_TABLE.model as DefaultTableModel
    namedRows[name]?.let {
        dtm.setValueAt(value, it, 3)
        dtm.setValueAt(count, it, 4)
    }
}

fun replaced(name: String, count: Int) {
    if (!TAB_VISIBLE) return
    val dtm = VALUES_TABLE.model as DefaultTableModel
    namedRows[name]?.let { dtm.setValueAt(count, it, 5) }
}

private fun reloadValuesTable(items: Items) {
    val dtm = VALUES_TABLE.model as DefaultTableModel
    val tmpMap = mutableMapOf<String, Int>()

    VALUES_TABLE.clearSelection()
    dtm.dataVector.removeAllElements()

    items.forEach {
        dtm.addRow(
            arrayOf(
                it.value.enabled,
                it.key,
                it.value.match,
                it.value.lastMatch,
                it.value.matchCount,
                it.value.replaceCount,
                it.value.transformer
            )
        )
        tmpMap[it.key] = dtm.rowCount - 1
    }

    dtm.fireTableDataChanged()
    namedRows = tmpMap
}

private fun reloadTransformersTable(transformers: Transformers) {
    val dtm = TRANSFORMER_TABLE.model as DefaultTableModel
    val tmpMap = mutableMapOf<String, Int>()

    TRANSFORMER_TABLE.clearSelection()
    dtm.dataVector.removeAllElements()

    transformers.forEach {
        dtm.addRow(
            arrayOf(
                it.key
            )
        )
        tmpMap[it.key] = dtm.rowCount - 1
    }

    dtm.fireTableDataChanged()
    namedTRows = tmpMap
}

private fun rowToName(row: Int): String {
    val dtm = VALUES_TABLE.model

    return dtm.getValueAt(row, 1).toString()
}

private fun rowToTName(row: Int): String {
    val dtm = TRANSFORMER_TABLE.model

    return dtm.getValueAt(row, 0).toString()
}

class UI(api: MontoyaApi, private val itemStore: ItemStore, private val transformerStore: TransformerStore) : JPanel() {
    private val ctx: Preferences = api.persistence().preferences()
    private var extEnabled = true
    private var enabledTools = mutableMapOf(
        PROXY to true,
        REPEATER to true,
        SCANNER to false,
        SEQUENCER to true,
        INTRUDER to true,
        EXTENSIONS to false,
    )

    private val toolSelectionMap = mutableMapOf<ToolType, JCheckBox>()

    private val enabledToggle = JCheckBox("Extension enabled").apply {
        addItemListener { e: ItemEvent -> enabledToggle(e) }
    }

    private val valueEdit = JButton("Edit").apply {
        addActionListener { valueEdit() }
        isEnabled = false
    }

    private val valueRemove = JButton("Remove").apply {
        addActionListener { valueRemove() }
        isEnabled = false
    }

    private val transformerRemove = JButton("Remove").apply {
        addActionListener { transformerRemove() }
        isEnabled = false
    }

    private val transformerEditorSave = JButton("Save").apply {
        addActionListener { transformerSave() }
        isEnabled = false
    }

    private val transformerEditorTest = JButton("Test").apply {
        addActionListener { transformerTest() }
        isEnabled = false
    }

    private val transformerEditor = api.userInterface().createRawEditor().apply {
        (uiComponent() as Container).components.filterNotNull().firstOrNull { it.name == "messageEditor" }
            ?.let { it as JScrollPane }?.components?.filterIsInstance<JViewport>()?.flatMap { it.components.asList() }
            ?.firstOrNull { it.name == "syntaxTextArea" }?.let { it as JTextArea }
            ?.addKeyListener(object : KeyAdapter() {
                override fun keyTyped(e: KeyEvent) {
                    this@UI.editorTyped()
                }
            })
    }

    private val transformerEditorPanel = JPanel(MigLayout("hidemode 3", "[]", "[][]")).apply {
        isVisible = false
        add(transformerEditorSave, "cell 0 0")
        add(transformerEditorTest, "cell 1 0")
        add(transformerEditor.uiComponent(), "cell 0 1,grow,push,span")
    }

    init {
        entries.filter { it in enabledTools }.forEach { tool ->
            toolSelectionMap[tool] = JCheckBox(tool.toolName()).apply {
                isSelected = enabledTools[tool] ?: false
                addItemListener { e: ItemEvent -> toolSelected(tool, e) }
            }
        }

        layout = MigLayout("fill,hidemode 3,align center top", "fill")
        add(JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            JPanel(MigLayout("fillx,hidemode 3,align left top", "[fill]", "[][][][][][]")).apply {
                add(JLabel("Values to Track").apply {
                    font = font.deriveFont(font.style or Font.BOLD)
                }, "cell 0 0")
                add(JPanel(MigLayout("hidemode 3", "[fill][fill]", "[]")).apply {
                    add(JPanel(MigLayout("hidemode 3", "[fill]", "[][][]")).apply {
                        add(JButton("Add").apply {
                            addActionListener { valueAdd() }
                        }, "cell 0 0")
                        add(valueEdit, "cell 0 1")
                        add(valueRemove, "cell 0 2")
                    }, "cell 0 0,aligny top,growy 0")
                    add(JScrollPane(VALUES_TABLE), "cell 1 0,grow,push,span")
                }, "cell 0 1")
                add(JSeparator(), "cell 0 2")
                add(JPanel(MigLayout("hidemode 3", "[fill]", "[][]")).apply {
                    add(JLabel("Enabled tools").apply {
                        font = font.deriveFont(font.style or Font.BOLD)
                    }, "cell 0 0")
                    add(JPanel(MigLayout("hidemode 3", "[fill][fill][fill]", "[][]")).apply {
                        toolSelectionMap.values.forEachIndexed { index, checkBox ->
                            add(checkBox, "cell ${index % 3} ${index / 3}")
                        }
                    }, "cell 0 1")
                }, "cell 0 3")
                add(JSeparator(), "cell 0 4")
                add(JPanel(MigLayout("hidemode 3", "[fill]", "[][][]")).apply {
                    add(JLabel("Settings").apply {
                        font = font.deriveFont(font.style or Font.BOLD)
                    }, "cell 0 0")
                    add(JPanel(MigLayout("hidemode 3", "[fill]", "[]")).apply {
                        add(enabledToggle, "cell 0 0")
                    }, "cell 0 1")
                }, "cell 0 5")
            }, JPanel(MigLayout("fill,hidemode 1,align left top", "[fill]", "[][][]")).apply {
                add(JLabel("Value Transformers").apply {
                    font = font.deriveFont(font.style or Font.BOLD)
                }, "cell 0 0")
                add(JPanel(MigLayout("hidemode 3", "[fill][fill]", "[]")).apply {
                    add(JPanel(MigLayout("hidemode 3", "[fill]", "[][]")).apply {
                        add(JButton("Add").apply {
                            addActionListener { transformerAdd() }
                        }, "cell 0 0")
                        add(transformerRemove, "cell 0 1")
                    }, "cell 0 0,aligny top,growy")
                    add(JScrollPane(TRANSFORMER_TABLE), "cell 1 0,grow,push,span")
                }, "cell 0 1")
                add(transformerEditorPanel, "cell 0 2,grow,push,span")
            }
        ).apply {
            resizeWeight = 0.5
        }, "w 100%,aligny top,grow,span"
        )

        (VALUES_TABLE.model as DefaultTableModel).addTableModelListener { e: TableModelEvent -> tableEdit(e) }
        VALUES_TABLE.selectionModel.addListSelectionListener { tableSelected() }
        TRANSFORMER_TABLE.selectionModel.addListSelectionListener { transformerSelected() }
        addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                TAB_VISIBLE = true
                reloadValuesTable(itemStore.items)
            }

            override fun componentHidden(e: ComponentEvent?) {
                TAB_VISIBLE = false
            }
        })

        loadValuesFromStore()
        api.userInterface().registerSuiteTab(TAB_NAME, this)
    }

    private fun loadValuesFromStore() {
        extEnabled = ctx.getBoolean("extEnabled") ?: true
        enabledToggle.isSelected = extEnabled

        toolSelectionMap.forEach {
            enabledTools[it.key] = ctx.getBoolean("${it.key.name}-enabled") ?: enabledTools[it.key]!!
            it.value.isSelected = enabledTools[it.key]!!
        }

        reloadValuesTable(itemStore.items)
        reloadTransformersTable(transformerStore.transformers)
    }


    fun isEnabled(tool: ToolType): Boolean {
        return extEnabled and enabledTools[tool]!!
    }

    private fun valueAdd() {
        val window = AddEditDialog(getWindowAncestor(this), -1, itemStore, transformerStore)
        window.title = "Add value"
        window.isVisible = true
    }

    private fun transformerAdd() {
        val window = TransformerAddDialog(getWindowAncestor(this), transformerStore)
        window.title = "Add transformer"
        window.isVisible = true
    }

    private fun valueEdit() {
        val selected = VALUES_TABLE.selectedRowCount
        if (selected < 1) {
            return
        }
        if (selected != 1) {
            showMessageDialog(null, "Can only edit 1 entry at a time!")
            return
        }

        val index = VALUES_TABLE.selectedRows[0]

        val window = AddEditDialog(getWindowAncestor(this), index, itemStore, transformerStore)
        window.title = "Edit value"
        window.isVisible = true
    }

    private fun valueRemove() {
        val selectedRows = VALUES_TABLE.selectedRows.reversed()
        VALUES_TABLE.clearSelection() // visual bug workaround

        for (row in selectedRows) {
            itemStore.items.remove(rowToName(row))
        }

        itemStore.save()

        reloadValuesTable(itemStore.items)
    }

    private fun transformerRemove() {
        val selectedRows = TRANSFORMER_TABLE.selectedRows
        TRANSFORMER_TABLE.clearSelection() // visual bug workaround

        if (selectedRows.isEmpty()) return

        transformerStore.transformers.remove(rowToTName(selectedRows[0]))
        transformerStore.save()
        transformerEditor.contents = ByteArray.byteArray("")

        reloadTransformersTable(transformerStore.transformers)
    }

    private fun transformerSave() {
        transformerStore.transformers[rowToTName(TRANSFORMER_TABLE.selectedRow)] = transformerEditor.contents.toString()
        transformerStore.save()

        transformerEditorSave.isEnabled = false
    }

    private fun transformerTest() {
        val window = TransformerTestDialog(
            getWindowAncestor(this),
            transformerEditor.contents.toString(),
            itemStore.items[rowToName(VALUES_TABLE.selectedRow)]?.lastMatch!!
        )
        window.title = "Transformer output"
        window.isVisible = true
    }

    private fun enabledToggle(e: ItemEvent) {
        extEnabled = e.stateChange == ItemEvent.SELECTED
        ctx.setBoolean("extEnabled", extEnabled)
    }

    private fun toolSelected(tool: ToolType, e: ItemEvent) {
        val enabled = e.stateChange == ItemEvent.SELECTED
        enabledTools[tool] = enabled
        ctx.setBoolean("${tool.name}-enabled", enabled)
    }

    private fun tableEdit(e: TableModelEvent) {
        val dtm = e.source as TableModel
        if (dtm.rowCount == 0) return

        val index = e.firstRow
        val enabled = dtm.getValueAt(index, 0) as Boolean

        itemStore.items[rowToName(index)]!!.enabled = enabled
        itemStore.save()
    }

    private fun tableSelected() {
        when (VALUES_TABLE.selectedRowCount) {
            0 -> {
                valueRemove.isEnabled = false
                valueEdit.isEnabled = false
                transformerEditorTest.isEnabled = false
            }

            1 -> {
                valueRemove.isEnabled = true
                valueEdit.isEnabled = true
                if (TRANSFORMER_TABLE.selectedRowCount == 1) transformerEditorTest.isEnabled = true
            }

            else -> {
                valueRemove.isEnabled = true
                valueEdit.isEnabled = false
                transformerEditorTest.isEnabled = false
            }
        }
    }

    private fun transformerSelected() {
        transformerEditorSave.isEnabled = false

        when (TRANSFORMER_TABLE.selectedRowCount) {
            0 -> {
                transformerRemove.isEnabled = false
                transformerEditorPanel.isVisible = false
            }

            else -> {
                transformerRemove.isEnabled = true
                transformerEditor.contents =
                    ByteArray.byteArray(transformerStore.transformers[rowToTName(TRANSFORMER_TABLE.selectedRow)]!!)
                if (VALUES_TABLE.selectedRowCount == 1) transformerEditorTest.isEnabled = true
                transformerEditorPanel.isVisible = true
            }
        }
    }

    private fun editorTyped() {
        if (TRANSFORMER_TABLE.selectedRowCount != 0) {
            transformerEditorSave.isEnabled = true
        }
    }
}

class AddEditDialog(
    owner: Window?,
    private var index: Int,
    private val itemStore: ItemStore,
    private val transformerStore: TransformerStore
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
            layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")
            add(JPanel(MigLayout("hidemode 3", "[fill][fill]", "[][][][]")).apply {
                add(JLabel("Name"), "cell 0 0")
                add(nameField, "cell 1 0,wmin 270,grow 0")
                add(JLabel("Match"), "cell 0 1")
                add(matchField, "cell 1 1,wmin 270,grow 0")
                add(JLabel("Type"), "cell 0 2")
                add(headerButton, "cell 1 2")
                add(regexButton, "cell 1 2")
                add(typeDescription, "cell 1 3")
                add(JLabel("Transformer"), "cell 0 4")
                add(transformerComboBox, "cell 1 4")
            }, "cell 0 0")
            add(errorLabel, "cell 0 4")
            add(JPanel(MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")).apply {
                add(JButton("OK").apply {
                    background = UIManager.getColor("Button.background")
                    font = font.deriveFont(font.style or Font.BOLD)
                    addActionListener { ok() }
                }, "west,gapx null 10")
                add(applyButton, "west")
                add(JButton("Cancel").apply {
                    addActionListener { cancel() }
                }, "EAST")
            }, "cell 0 5")
        }

        setSize(250, 100)
        isResizable = false
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
        transformerStore.transformers.forEach {
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

        if (name == "") {
            errorLabel.text = "Name field cannot be left blank!"
            return false
        }

        if (match == "") {
            errorLabel.text = "Match field cannot be left blank!"
            return false
        }

        if (type == ItemType.REGEX) {
            val err = checkRegexSyntax(match)
            if (err != "") {
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
            index = VALUES_TABLE.rowCount
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

class TransformerAddDialog(owner: Window?, private val transformerStore: TransformerStore) : JDialog(owner) {
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
            layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")
            add(JPanel(MigLayout("hidemode 3", "[fill][fill]", "[][][][]")).apply {
                add(JLabel("Name"), "cell 0 0")
                add(nameField, "cell 1 0,wmin 270,grow 0")
            }, "cell 0 0")
            add(errorLabel, "cell 0 4")
            add(JPanel(MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")).apply {
                add(JButton("OK").apply {
                    background = UIManager.getColor("Button.background")
                    font = font.deriveFont(font.style or Font.BOLD)
                    addActionListener { if (apply()) close() }
                }, "west,gapx null 10")
                add(JButton("Cancel").apply {
                    addActionListener { close() }
                }, "EAST")
            }, "cell 0 5")
        }

        setSize(250, 100)
        isResizable = false
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
        transformerStore.transformers[name] = ""

        transformerStore.save()
        reloadTransformersTable(transformerStore.transformers)

        return true
    }

    private fun showError(err: String) {
        errorLabel.text = err
    }
}

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
            layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")
            add(nameLabel, "cell 0 0, wrap")
            add(JScrollPane(output), "grow, push, span")
            add(JPanel(MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")).apply {
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
