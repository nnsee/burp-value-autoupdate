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
import java.awt.event.ItemEvent.SELECTED
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
            if (columnIndex == 0) return true  // enabled button
            return false
        }
    }
    columnModel.getColumn(0).resizable = false
    columnModel.getColumn(0).width = 25
}

val TRANSFORMER_TABLE = JTable().apply {
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    model = object : DefaultTableModel(
        arrayOf(), arrayOf(
            "Name"
        )
    ) {}
}

const val TAB_NAME = "Value updater"
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

    private val mainLabel = JLabel("Value Autoupdater").apply {
        font.deriveFont(font.style or Font.BOLD)
    }

    private val enabledToggle = JCheckBox("Enabled").apply {
        addItemListener { e: ItemEvent -> enabledToggle(e) }
    }

    private val headerNestedPanel = JPanel(MigLayout("hidemode 3", "[fill]", "[]")).apply {
        add(enabledToggle, "cell 0 0")
    }

    private val headerPanel = JPanel(MigLayout("hidemode 3", "[fill]", "[][][]")).apply {
        add(mainLabel, "cell 0 0")
        add(headerNestedPanel, "cell 0 1")
    }

    private val separator2 = JSeparator()

    private val valuesLabel = JLabel("Values to watch").apply {
        font.deriveFont(font.style or Font.BOLD)
    }

    private val valueAdd = JButton("Add").apply {
        addActionListener { valueAdd() }
    }

    private val valueEdit = JButton("Edit").apply {
        addActionListener { valueEdit() }
        isEnabled = false
    }

    private val valueRemove = JButton("Remove").apply {
        addActionListener { valueRemove() }
        isEnabled = false
    }

    private val valueButtons = JPanel(MigLayout("hidemode 3", "[fill]", "[][][]")).apply {
        add(valueAdd, "cell 0 0")
        add(valueEdit, "cell 0 1")
        add(valueRemove, "cell 0 2")
    }

    private val valuesTablePanel = JScrollPane(VALUES_TABLE)

    private val valueSelectorPanel = JPanel(MigLayout("hidemode 3", "[fill][fill]", "[]")).apply {
        add(valueButtons, "cell 0 0,aligny top,growy 0")
        add(valuesTablePanel, "cell 1 0,grow,push,span")
    }

    private val valuesPanel = JPanel(MigLayout("hidemode 3", "[fill]", "[][][][]")).apply {
        add(valuesLabel, "cell 0 0")
        add(valueSelectorPanel, "cell 0 2,grow,push,span")
    }

    private val separator1 = JSeparator()

    private val toolsLabel = JLabel("Enabled tools").apply {
        font = font.deriveFont(font.style or Font.BOLD)
    }

    private val proxySel = JCheckBox("Proxy").apply {
        addItemListener { e: ItemEvent -> toolSelected(PROXY, e) }
    }

    private val scannerSel = JCheckBox("Scanner").apply {
        addItemListener { e: ItemEvent -> toolSelected(SCANNER, e) }
    }

    private val intruderSel = JCheckBox("Intruder").apply {
        addItemListener { e: ItemEvent -> toolSelected(INTRUDER, e) }
    }

    private val repeaterSel = JCheckBox("Repeater").apply {
        addItemListener { e: ItemEvent -> toolSelected(REPEATER, e) }
    }

    private val sequencerSel = JCheckBox("Sequencer").apply {
        addItemListener { e: ItemEvent -> toolSelected(SEQUENCER, e) }
    }

    private val extenderSel = JCheckBox("Extensions").apply {
        addItemListener { e: ItemEvent -> toolSelected(EXTENSIONS, e) }
    }

    private val toolSelectionMap = mapOf(
        PROXY to proxySel,
        SCANNER to scannerSel,
        INTRUDER to intruderSel,
        REPEATER to repeaterSel,
        SEQUENCER to sequencerSel,
        EXTENSIONS to extenderSel,
    )

    private val toolSelectionPanel = JPanel(MigLayout("hidemode 3", "[fill][fill][fill]", "[][]")).apply {
        add(proxySel, "cell 0 0")
        add(scannerSel, "cell 1 0")
        add(intruderSel, "cell 2 0")
        add(repeaterSel, "cell 0 1")
        add(sequencerSel, "cell 1 1")
        add(extenderSel, "cell 2 1")
    }

    private val toolsPanel = JPanel(MigLayout("hidemode 3", "[fill]", "[][]")).apply {
        add(toolsLabel, "cell 0 0")
        add(toolSelectionPanel, "cell 0 1")
    }

    private val leftPanel = JPanel(MigLayout("fill,hidemode 3,align left top", "[fill]", "[][][][][]")).apply {
        add(headerPanel, "cell 0 0")
        add(separator2, "cell 0 1")
        add(valuesPanel, "cell 0 2")
        add(separator1, "cell 0 3")
        add(toolsPanel, "cell 0 4")
    }

    private val transformerLabel = JLabel("Value Transformers").apply {
        font = font.deriveFont(font.style or Font.BOLD)
    }

    private val transformerAdd = JButton("Add").apply {
        addActionListener { transformerAdd() }
    }

    private val transformerRemove = JButton("Remove").apply {
        addActionListener { transformerRemove() }
        isEnabled = false
    }

    private val transformerButtons = JPanel(MigLayout("hidemode 3", "[fill]", "[][]")).apply {
        add(transformerAdd, "cell 0 0")
        add(transformerRemove, "cell 0 1")
    }

    private val transformerTablePanel = JScrollPane(TRANSFORMER_TABLE)

    private val transformerSelectorPanel = JPanel(MigLayout("hidemode 3", "[fill][fill]", "[]")).apply {
        add(transformerButtons, "cell 0 0,aligny top,growy")
        add(transformerTablePanel, "cell 1 0,grow,push,span")
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
        setEditable(false)

        (uiComponent() as Container).components
        .filterNotNull()
        .firstOrNull { it.name == "messageEditor" }
        ?.let { it as JScrollPane }
        ?.components
        ?.filterIsInstance<JViewport>()
        ?.flatMap { it.components.asList() }
        ?.firstOrNull { it.name == "syntaxTextArea" }
        ?.let { it as JTextArea }
        ?.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                this@UI.editorTyped()
            }
        })
    }

    private val transformerEditorPanel = JPanel(MigLayout("hidemode 3", "[]", "[][]")).apply {
        add(transformerEditorSave, "cell 0 0")
        add(transformerEditorTest, "cell 1 0")
        add(transformerEditor.uiComponent(), "cell 0 1,grow,push,span")
    }

    private val rightPanel = JPanel(MigLayout("fill,hidemode 3,align left top", "[fill]", "[][][]")).apply {
        add(transformerLabel, "cell 0 0")
        add(transformerSelectorPanel, "cell 0 1")
        add(transformerEditorPanel, "cell 0 2,grow,push,span")
    }

    init {
        layout = MigLayout("fill,hidemode 3,align center top", "fill")
        add(leftPanel, "w 50%,aligny top,growy 0,growx")
        add(rightPanel, "w 50%,aligny top,growy 0,grow,push,span")

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
        extEnabled = e.stateChange == SELECTED
        ctx.setBoolean("extEnabled", extEnabled)
    }

    private fun toolSelected(tool: ToolType, e: ItemEvent) {
        val enabled = e.stateChange == SELECTED
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
                transformerEditor.contents = ByteArray.byteArray("")
                transformerEditor.setEditable(false)
                transformerEditorTest.isEnabled = false
            }

            1 -> {
                transformerRemove.isEnabled = true
                transformerEditor.contents =
                    ByteArray.byteArray(transformerStore.transformers[rowToTName(TRANSFORMER_TABLE.selectedRow)]!!)
                transformerEditor.setEditable(true)
                if (VALUES_TABLE.selectedRowCount == 1) transformerEditorTest.isEnabled = true
            }

            else -> {
                transformerRemove.isEnabled = true
                transformerEditor.contents =
                    ByteArray.byteArray(transformerStore.transformers[rowToTName(TRANSFORMER_TABLE.selectedRow)]!!)
                transformerEditor.setEditable(true)
                transformerEditorTest.isEnabled = false
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

    private val nameLabel = JLabel("Name")
    private val nameField = JTextField().apply {
        addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                this@AddEditDialog.keyTyped()
            }
        })
    }

    private val matchLabel = JLabel("Match")
    private val matchField = JTextField().apply {
        addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                this@AddEditDialog.keyTyped()
            }
        })
    }

    private val typeField = JLabel("Type")

    private val headerButton = JRadioButton("Header").apply {
        isSelected = true
        addItemListener { e: ItemEvent -> headerButtonItemStateChanged(e) }
    }

    private val regexButton = JRadioButton("Regex").apply {
        addItemListener { e: ItemEvent -> regexButtonItemStateChanged(e) }
    }

    private val typeDescription = JLabel(headerTypeHint)

    private val transformerLabel = JLabel("Transformer")
    private val transformerComboBox = JComboBox<String>()

    private val optionsPanel = JPanel(MigLayout("hidemode 3", "[fill][fill]", "[][][][]")).apply {
        add(nameLabel, "cell 0 0")
        add(nameField, "cell 1 0,wmin 270,grow 0")
        add(matchLabel, "cell 0 1")
        add(matchField, "cell 1 1,wmin 270,grow 0")
        add(typeField, "cell 0 2")
        add(headerButton, "cell 1 2")
        add(regexButton, "cell 1 2")
        add(typeDescription, "cell 1 3")
        add(transformerLabel, "cell 0 4")
        add(transformerComboBox, "cell 1 4")
    }

    private val errorLabel = JLabel().apply {
        foreground = Color.RED
        text = " "
    }

    private val okButton = JButton("OK").apply {
        background = UIManager.getColor("Button.background")
        font = font.deriveFont(font.style or Font.BOLD)
        addActionListener { ok() }
    }

    private val applyButton = JButton("Apply").apply {
        isEnabled = false
        addActionListener { apply() }
    }

    private val cancelButton = JButton("Cancel").apply {
        addActionListener { cancel() }
    }

    private val buttonsPanel = JPanel(MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")).apply {
        add(okButton, "west,gapx null 10")
        add(applyButton, "west")
        add(cancelButton, "EAST")
    }

    init {
        ButtonGroup().apply {
            add(headerButton)
            add(regexButton)
        }

        contentPane.apply {
            layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")
            add(optionsPanel, "cell 0 0")
            add(errorLabel, "cell 0 4")
            add(buttonsPanel, "cell 0 5")
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
            log.debug("Failed to get name for row $index")
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
        enableApply()
        errorLabel.text = " "
    }

    private fun headerButtonItemStateChanged(e: ItemEvent) {
        enableApply()
        if (e.stateChange == SELECTED) {
            typeDescription.text = headerTypeHint
        }
    }

    private fun regexButtonItemStateChanged(e: ItemEvent) {
        enableApply()
        if (e.stateChange == SELECTED) {
            typeDescription.text = regexTypeHint
        }
    }

    private fun enableApply() {
        applyButton.isEnabled = true
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
            log.debug("New item at index $index: $item")
        } else {
            val item = itemStore.items[rowToName(index)]
            if (item == null) {
                log.debug("Failed to get name for row $index")
                return false
            }
            item.match = match
            item.type = type
            item.transformer = transformer
            log.debug("Edited item at index $index: $item")
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
    private val nameLabel = JLabel("Name")
    private val nameField = JTextField().apply {
        addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                showError(" ")
            }
        })
    }

    private val namePanel = JPanel(MigLayout("hidemode 3", "[fill][fill]", "[][][][]")).apply {
        add(nameLabel, "cell 0 0")
        add(nameField, "cell 1 0,wmin 270,grow 0")
    }

    private val errorLabel = JLabel(" ").apply {
        foreground = Color.RED
    }

    private val okButton = JButton("OK").apply {
        background = UIManager.getColor("Button.background")
        font = font.deriveFont(font.style or Font.BOLD)
        addActionListener { if (apply()) close() }
    }

    private val cancelButton = JButton("Cancel").apply {
        addActionListener { close() }
    }

    private val buttonPanel = JPanel(MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")).apply {
        add(okButton, "west,gapx null 10")
        add(cancelButton, "EAST")
    }

    init {
        contentPane.apply {
            layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")
            add(namePanel, "cell 0 0")
            add(errorLabel, "cell 0 4")
            add(buttonPanel, "cell 0 5")
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

    private val okButton = JButton("OK").apply {
        background = UIManager.getColor("Button.background")
        font = font.deriveFont(font.style or Font.BOLD)
        addActionListener {
            this@TransformerTestDialog.dispatchEvent(
                WindowEvent(
                    this@TransformerTestDialog, WindowEvent.WINDOW_CLOSING
                )
            )
        }
    }

    private val buttonPanel = JPanel(MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")).apply {
        add(okButton, "west,gapx null 10")
    }

    init {
        contentPane.apply {
            layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")
            add(nameLabel, "cell 0 0, wrap")
            add(JScrollPane(output), "grow, push, span")
            add(buttonPanel, "cell 0 4")
        }

        pack()
        setLocationRelativeTo(owner)
        defaultCloseOperation = DISPOSE_ON_CLOSE
    }
}
