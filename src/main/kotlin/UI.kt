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

val VALUES_TABLE = JTable()
val TRANSFORMER_TABLE = JTable()
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

class UI(api: MontoyaApi, itemStore: ItemStore, transformerStore: TransformerStore) : JPanel() {
    private val api: MontoyaApi
    private val ctx: Preferences
    private val itemStore: ItemStore
    private val transformerStore: TransformerStore
    private var extEnabled = true
    private var enabledTools = mutableMapOf(
        PROXY to true,
        REPEATER to true,
        SCANNER to false,
        SEQUENCER to true,
        INTRUDER to true,
        EXTENSIONS to false,
    )

    private val headerPanel = JPanel()
    private val leftPanel = JPanel()
    private val rightPanel = JPanel()
    private val mainLabel = JLabel()
    private val headerNestedPanel = JPanel()
    private val enabledToggle = JCheckBox()
    private val separator2 = JSeparator()
    private val valuesPanel = JPanel()
    private val valuesLabel = JLabel()
    private val valueSelectorPanel = JPanel()
    private val valueButtons = JPanel()
    private val valueAdd = JButton()
    private val valueEdit = JButton()
    private val valueRemove = JButton()
    private val valuesTablePanel = JScrollPane()
    private val valuesTable = VALUES_TABLE
    private val separator1 = JSeparator()
    private val toolsPanel = JPanel()
    private val toolsLabel = JLabel()
    private val toolSelectionPanel = JPanel()
    private val proxySel = JCheckBox()
    private val scannerSel = JCheckBox()
    private val intruderSel = JCheckBox()
    private val repeaterSel = JCheckBox()
    private val sequencerSel = JCheckBox()
    private val extenderSel = JCheckBox()
    private val transformerLabel = JLabel()
    private val transformerSelectorPanel = JPanel()
    private val transformerButtons = JPanel()
    private val transformerAdd = JButton()
    private val transformerRemove = JButton()
    private val transformerTablePanel = JScrollPane()
    private val transformerTable = TRANSFORMER_TABLE
    private val transformerEditorPanel = JPanel()
    private val transformerEditor = api.userInterface().createRawEditor()
    private val transformerEditorSave = JButton()
    private val transformerEditorTest = JButton()

    init {
        this.api = api
        this.ctx = api.persistence().preferences()
        this.itemStore = itemStore
        this.transformerStore = transformerStore
        initComponents()
        loadValuesFromStore()
        api.userInterface().registerSuiteTab(TAB_NAME, this)
        this.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                TAB_VISIBLE = true
                reloadValuesTable(itemStore.items)
            }

            override fun componentHidden(e: ComponentEvent?) {
                TAB_VISIBLE = false
            }
        })

    }

    private fun loadValuesFromStore() {
        extEnabled = ctx.getBoolean("extEnabled") ?: true
        enabledToggle.isSelected = extEnabled

        enabledTools[PROXY] = ctx.getBoolean("proxyE") ?: enabledTools[PROXY]!!
        proxySel.isSelected = enabledTools[PROXY]!!
        enabledTools[SCANNER] = ctx.getBoolean("scannerE") ?: enabledTools[SCANNER]!!
        scannerSel.isSelected = enabledTools[SCANNER]!!
        enabledTools[INTRUDER] = ctx.getBoolean("intruderE") ?: enabledTools[INTRUDER]!!
        intruderSel.isSelected = enabledTools[INTRUDER]!!
        enabledTools[REPEATER] = ctx.getBoolean("repeaterE") ?: enabledTools[REPEATER]!!
        repeaterSel.isSelected = enabledTools[REPEATER]!!
        enabledTools[SEQUENCER] = ctx.getBoolean("sequencerE") ?: enabledTools[SEQUENCER]!!
        sequencerSel.isSelected = enabledTools[SEQUENCER]!!
        enabledTools[EXTENSIONS] = ctx.getBoolean("extenderE") ?: enabledTools[EXTENSIONS]!!
        extenderSel.isSelected = enabledTools[EXTENSIONS]!!

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
        val selected = valuesTable.selectedRowCount
        if (selected < 1) {
            return
        }
        if (selected != 1) {
            showMessageDialog(null, "Can only edit 1 entry at a time!")
            return
        }

        val index = valuesTable.selectedRows[0]

        val window = AddEditDialog(getWindowAncestor(this), index, itemStore, transformerStore)
        window.title = "Edit value"
        window.isVisible = true
    }

    private fun valueRemove() {
        val selectedRows = valuesTable.selectedRows.reversed()
        VALUES_TABLE.clearSelection() // visual bug work around

        for (row in selectedRows) {
            itemStore.items.remove(rowToName(row))
        }

        itemStore.save()

        reloadValuesTable(itemStore.items)
    }

    private fun transformerRemove() {
        val selectedRows = transformerTable.selectedRows
        TRANSFORMER_TABLE.clearSelection() // visual bug work around

        if (selectedRows.isEmpty()) return

        transformerStore.transformers.remove(rowToTName(selectedRows[0]))
        transformerStore.save()
        transformerEditor.contents = ByteArray.byteArray("")

        reloadTransformersTable(transformerStore.transformers)
    }

    private fun transformerSave() {
        transformerStore.transformers[rowToTName(transformerTable.selectedRow)] =
            transformerEditor.contents.toString()
        transformerStore.save()

        transformerEditorSave.isEnabled = false
    }

    private fun transformerTest() {
        val window = TransformerTestDialog(getWindowAncestor(this), transformerStore.transformers[rowToTName(transformerTable.selectedRow)]!!, itemStore.items[rowToName(valuesTable.selectedRow)]?.lastMatch!!)
        window.title = "Transformer output"
        window.isVisible = true
    }

    private fun enabledToggle(e: ItemEvent) {
        extEnabled = e.stateChange == SELECTED
        ctx.setBoolean("extEnabled", extEnabled)
    }

    private fun proxySel(e: ItemEvent) {
        enabledTools[PROXY] = e.stateChange == SELECTED
        ctx.setBoolean("proxyE", enabledTools[PROXY]!!)
    }

    private fun scannerSel(e: ItemEvent) {
        enabledTools[SCANNER] = e.stateChange == SELECTED
        ctx.setBoolean("scannerE", enabledTools[SCANNER]!!)
    }

    private fun intruderSel(e: ItemEvent) {
        enabledTools[INTRUDER] = e.stateChange == SELECTED
        ctx.setBoolean("intruderE", enabledTools[INTRUDER]!!)
    }

    private fun repeaterSel(e: ItemEvent) {
        enabledTools[REPEATER] = e.stateChange == SELECTED
        ctx.setBoolean("repeaterE", enabledTools[REPEATER]!!)
    }

    private fun sequencerSel(e: ItemEvent) {
        enabledTools[SEQUENCER] = e.stateChange == SELECTED
        ctx.setBoolean("sequencerE", enabledTools[SEQUENCER]!!)
    }

    private fun extenderSel(e: ItemEvent) {
        enabledTools[EXTENSIONS] = e.stateChange == SELECTED
        ctx.setBoolean("extenderE", enabledTools[EXTENSIONS]!!)
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
        when (valuesTable.selectedRowCount) {
            0 -> {
                valueRemove.isEnabled = false
                valueEdit.isEnabled = false
                transformerEditorTest.isEnabled = false
            }

            1 -> {
                valueRemove.isEnabled = true
                valueEdit.isEnabled = true
                if (transformerTable.selectedRowCount == 1)
                    transformerEditorTest.isEnabled = true
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

        when (transformerTable.selectedRowCount) {
            0 -> {
                transformerRemove.isEnabled = false
                transformerEditor.contents = ByteArray.byteArray("")
                transformerEditor.setEditable(false)
                transformerEditorTest.isEnabled = false
            }

            1 -> {
                transformerRemove.isEnabled = true
                transformerEditor.contents =
                   ByteArray.byteArray(transformerStore.transformers[rowToTName(transformerTable.selectedRow)]!!)
                transformerEditor.setEditable(true)
                if (valuesTable.selectedRowCount == 1)
                    transformerEditorTest.isEnabled = true
            }

            else -> {
                transformerRemove.isEnabled = true
                transformerEditor.contents =
                    ByteArray.byteArray(transformerStore.transformers[rowToTName(transformerTable.selectedRow)]!!)
                transformerEditor.setEditable(true)
                transformerEditorTest.isEnabled = false
            }
        }
    }

    private fun editorTyped() {
        if (transformerTable.selectedRowCount != 0) {
            transformerEditorSave.isEnabled = true
        }
    }

    //<editor-fold desc="UI layout cruft">
    private fun initComponents() {
        layout = MigLayout("fill,hidemode 3,align center top", "fill")
        leftPanel.layout = MigLayout("fill,hidemode 3,align left top", "[fill]", "[][][][][]")
        rightPanel.layout = MigLayout("fill,hidemode 3,align left top", "[fill]", "[][][]")

        headerPanel.layout = MigLayout("hidemode 3", "[fill]", "[][][]")

        mainLabel.text = "Value Autoupdater"
        mainLabel.font = mainLabel.font.deriveFont(mainLabel.font.style or Font.BOLD)
        headerPanel.add(mainLabel, "cell 0 0")

        headerNestedPanel.layout = MigLayout("hidemode 3", "[fill]", "[]")

        enabledToggle.text = "Enabled"
        enabledToggle.addItemListener { e: ItemEvent -> enabledToggle(e) }
        headerNestedPanel.add(enabledToggle, "cell 0 0")
        headerPanel.add(headerNestedPanel, "cell 0 1")
        leftPanel.add(headerPanel, "cell 0 0")
        leftPanel.add(separator2, "cell 0 1")

        valuesPanel.layout = MigLayout("hidemode 3", "[fill]", "[][][][]")

        valuesLabel.text = "Values to watch"
        valuesLabel.font = valuesLabel.font.deriveFont(valuesLabel.font.style or Font.BOLD)
        valuesPanel.add(valuesLabel, "cell 0 0")

        valueSelectorPanel.layout = MigLayout("hidemode 3", "[fill][fill]", "[]")

        valueButtons.layout = MigLayout("hidemode 3", "[fill]", "[][][]")

        valueAdd.text = "Add"
        valueAdd.addActionListener { valueAdd() }
        valueButtons.add(valueAdd, "cell 0 0")

        valueEdit.text = "Edit"
        valueEdit.addActionListener { valueEdit() }
        valueEdit.isEnabled = false
        valueButtons.add(valueEdit, "cell 0 1")

        valueRemove.text = "Remove"
        valueRemove.addActionListener { valueRemove() }
        valueRemove.isEnabled = false
        valueButtons.add(valueRemove, "cell 0 2")

        valueSelectorPanel.add(valueButtons, "cell 0 0,aligny top,growy 0")

        valuesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        valuesTable.model = object : DefaultTableModel(
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
        val cm = valuesTable.columnModel
        cm.getColumn(0).resizable = false
        cm.getColumn(0).width = 25
        (valuesTable.model as DefaultTableModel).addTableModelListener { e: TableModelEvent -> tableEdit(e) }
        valuesTable.selectionModel.addListSelectionListener { tableSelected() }

        valuesTablePanel.setViewportView(valuesTable)
        valueSelectorPanel.add(valuesTablePanel, "cell 1 0,grow,push,span")
        valuesPanel.add(valueSelectorPanel, "cell 0 2,grow,push,span")
        leftPanel.add(valuesPanel, "cell 0 2")
        leftPanel.add(separator1, "cell 0 3")

        toolsPanel.layout = MigLayout("hidemode 3", "[fill]", "[][]")

        toolsLabel.text = "Enabled tools"
        toolsLabel.font = toolsLabel.font.deriveFont(toolsLabel.font.style or Font.BOLD)
        toolsPanel.add(toolsLabel, "cell 0 0")

        toolSelectionPanel.layout = MigLayout("hidemode 3", "[fill][fill][fill]", "[][]")

        proxySel.text = "Proxy"
        proxySel.addItemListener { e: ItemEvent -> proxySel(e) }
        toolSelectionPanel.add(proxySel, "cell 0 0")

        scannerSel.text = "Scanner"
        scannerSel.addItemListener { e: ItemEvent -> scannerSel(e) }
        toolSelectionPanel.add(scannerSel, "cell 1 0")

        intruderSel.text = "Intruder"
        intruderSel.addItemListener { e: ItemEvent -> intruderSel(e) }
        toolSelectionPanel.add(intruderSel, "cell 2 0")

        repeaterSel.text = "Repeater"
        repeaterSel.addItemListener { e: ItemEvent -> repeaterSel(e) }
        toolSelectionPanel.add(repeaterSel, "cell 0 1")

        sequencerSel.text = "Sequencer"
        sequencerSel.addItemListener { e: ItemEvent -> sequencerSel(e) }
        toolSelectionPanel.add(sequencerSel, "cell 1 1")

        extenderSel.text = "Extender"
        extenderSel.addItemListener { e: ItemEvent -> extenderSel(e) }
        toolSelectionPanel.add(extenderSel, "cell 2 1")

        toolsPanel.add(toolSelectionPanel, "cell 0 1")
        leftPanel.add(toolsPanel, "cell 0 4")

        add(leftPanel, "w 50%,aligny top,growy 0,growx")
        transformerLabel.text = "Value Transformers"
        transformerLabel.font = transformerLabel.font.deriveFont(transformerLabel.font.style or Font.BOLD)
        rightPanel.add(transformerLabel, "cell 0 0")

        transformerSelectorPanel.layout = MigLayout("hidemode 3", "[fill][fill]", "[]")

        transformerButtons.layout = MigLayout("hidemode 3", "[fill]", "[][]")

        transformerAdd.text = "Add"
        transformerAdd.addActionListener { transformerAdd() }
        transformerButtons.add(transformerAdd, "cell 0 0")

        transformerRemove.text = "Remove"
        transformerRemove.addActionListener { transformerRemove() }
        transformerRemove.isEnabled = false
        transformerButtons.add(transformerRemove, "cell 0 1")

        transformerSelectorPanel.add(transformerButtons, "cell 0 0,aligny top,growy")

        transformerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        transformerTable.model = object : DefaultTableModel(
            arrayOf(), arrayOf(
                "Name"
            )
        ) {}
        transformerTable.selectionModel.addListSelectionListener { transformerSelected() }

        transformerTablePanel.setViewportView(transformerTable)
        transformerSelectorPanel.add(transformerTablePanel, "cell 1 0,grow,push,span")
        rightPanel.add(transformerSelectorPanel, "cell 0 1")

        transformerEditorPanel.layout = MigLayout("hidemode 3", "[]", "[][]")
        transformerEditor.setEditable(false)

        transformerEditorSave.text = "Save"
        transformerEditorSave.addActionListener { transformerSave() }
        transformerEditorSave.isEnabled = false
        transformerEditorPanel.add(transformerEditorSave, "cell 0 0")

        transformerEditorTest.text = "Test"
        transformerEditorTest.addActionListener { transformerTest() }
        transformerEditorTest.isEnabled = false
        transformerEditorPanel.add(transformerEditorTest, "cell 1 0")

        (transformerEditor.uiComponent() as Container).components.forEach top@{
            if (it != null && it.name == "messageEditor") {
                (it as JScrollPane).components.filterIsInstance<JViewport>().forEach { child ->
                    child.components.forEach { candidate ->
                        if (candidate.name == "syntaxTextArea") {
                            (candidate as JTextArea).addKeyListener(object : KeyAdapter() {
                                override fun keyTyped(e: KeyEvent) {
                                    this@UI.editorTyped()
                                }
                            })
                            return@top
                        }
                    }
                }
            }
        }

        transformerEditorPanel.add(transformerEditor.uiComponent(), "cell 0 1,grow,push,span")
        rightPanel.add(transformerEditorPanel, "cell 0 2,grow,push,span")
        add(rightPanel, "w 50%,aligny top,growy 0,grow,push,span")
    }

    //</editor-fold>
}

class AddEditDialog(owner: Window?, index: Int, itemStore: ItemStore, transformerStore: TransformerStore) :
    JDialog(owner) {
    private val headerTypeHint = "Matches header names and replaces values "
    private val regexTypeHint = "Uses regex for matches (named group: val)"
    private var index: Int
    private val itemStore: ItemStore
    private val transformerStore: TransformerStore

    private val panel1 = JPanel()
    private val nameLabel = JLabel()
    private val nameField = JTextField()
    private val matchLabel = JLabel()
    private val matchField = JTextField()
    private val typeField = JLabel()
    private val headerButton = JRadioButton()
    private val regexButton = JRadioButton()
    private val typeDescription = JLabel()
    private val transformerLabel = JLabel()
    private val transformerComboBox = JComboBox<String>()
    private val errorLabel = JLabel()
    private val panel3 = JPanel()
    private val okButton = JButton()
    private val applyButton = JButton()
    private val cancelButton = JButton()

    init {
        this.index = index
        this.itemStore = itemStore
        this.transformerStore = transformerStore
        initComponents()
        loadTransformers()
        if (index != -1) {
            loadStoredValues()
        }
        this.defaultCloseOperation = DISPOSE_ON_CLOSE
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
        showError(" ")
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
            showError("Name field cannot be left blank!")
            return false
        }

        if (match == "") {
            showError("Match field cannot be left blank!")
            return false
        }

        if (type == ItemType.REGEX) {
            val err = checkRegexSyntax(match)
            if (err != "") {
                showError(err)
                return false
            }
        }

        if (index == -1) {
            val item = Item(match, type, "", true, 0, 0, transformer)
            if (itemStore.items[name] != null) {
                showError("Entry named \"$name\" already exists!")
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

    private fun showError(err: String) {
        errorLabel.text = err
    }

    //<editor-fold desc="UI layout cruft">
    private fun initComponents() {
        contentPane.layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")

        panel1.layout = MigLayout("hidemode 3", "[fill][fill]", "[][][][]")

        nameLabel.text = "Name"
        panel1.add(nameLabel, "cell 0 0")

        nameField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                this@AddEditDialog.keyTyped()
            }
        })
        panel1.add(nameField, "cell 1 0,wmin 270,grow 0")

        matchLabel.text = "Match"
        panel1.add(matchLabel, "cell 0 1")

        matchField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                this@AddEditDialog.keyTyped()
            }
        })
        panel1.add(matchField, "cell 1 1,wmin 270,grow 0")

        typeField.text = "Type"
        panel1.add(typeField, "cell 0 2")

        headerButton.text = "Header"
        headerButton.isSelected = true
        headerButton.addItemListener { e: ItemEvent -> headerButtonItemStateChanged(e) }
        panel1.add(headerButton, "cell 1 2")

        regexButton.text = "Regex"
        regexButton.addItemListener { e: ItemEvent -> regexButtonItemStateChanged(e) }
        panel1.add(regexButton, "cell 1 2")

        typeDescription.text = headerTypeHint
        panel1.add(typeDescription, "cell 1 3")

        transformerLabel.text = "Transformer"
        panel1.add(transformerLabel, "cell 0 4")

        panel1.add(transformerComboBox, "cell 1 4")

        contentPane.add(panel1, "cell 0 0")

        errorLabel.foreground = Color.RED
        showError(" ")
        contentPane.add(errorLabel, "cell 0 4")

        panel3.layout = MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")

        okButton.text = "OK"
        okButton.background = UIManager.getColor("Button.background")
        okButton.font = okButton.font.deriveFont(okButton.font.style or Font.BOLD)
        okButton.addActionListener { ok() }
        panel3.add(okButton, "west,gapx null 10")

        applyButton.text = "Apply"
        applyButton.isEnabled = false
        applyButton.addActionListener { apply() }
        panel3.add(applyButton, "west")

        cancelButton.text = "Cancel"
        cancelButton.addActionListener { cancel() }
        panel3.add(cancelButton, "EAST")
        contentPane.add(panel3, "cell 0 5")

        val buttonGroup1 = ButtonGroup()
        buttonGroup1.add(headerButton)
        buttonGroup1.add(regexButton)

        setSize(250, 100)
        isResizable = false
        pack()
        setLocationRelativeTo(owner)
    }
    //</editor-fold>
}

class TransformerAddDialog(owner: Window?, transformerStore: TransformerStore) : JDialog(owner) {
    private val transformerStore: TransformerStore

    private val panel1 = JPanel()
    private val nameLabel = JLabel()
    private val nameField = JTextField()
    private val errorLabel = JLabel()
    private val panel3 = JPanel()
    private val okButton = JButton()
    private val cancelButton = JButton()

    init {
        this.transformerStore = transformerStore
        initComponents()
        this.defaultCloseOperation = DISPOSE_ON_CLOSE
    }

    private fun keyTyped() {
        showError(" ")
    }

    private fun ok() {
        if (apply()) this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
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

    private fun cancel() {
        this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }

    private fun showError(err: String) {
        errorLabel.text = err
    }

    //<editor-fold desc="UI layout cruft">
    private fun initComponents() {
        contentPane.layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")

        panel1.layout = MigLayout("hidemode 3", "[fill][fill]", "[][][][]")

        nameLabel.text = "Name"
        panel1.add(nameLabel, "cell 0 0")

        nameField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                this@TransformerAddDialog.keyTyped()
            }
        })
        panel1.add(nameField, "cell 1 0,wmin 270,grow 0")

        contentPane.add(panel1, "cell 0 0")

        errorLabel.foreground = Color.RED
        showError(" ")
        contentPane.add(errorLabel, "cell 0 4")

        panel3.layout = MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")

        okButton.text = "OK"
        okButton.background = UIManager.getColor("Button.background")
        okButton.font = okButton.font.deriveFont(okButton.font.style or Font.BOLD)
        okButton.addActionListener { ok() }
        panel3.add(okButton, "west,gapx null 10")

        cancelButton.text = "Cancel"
        cancelButton.addActionListener { cancel() }
        panel3.add(cancelButton, "EAST")
        contentPane.add(panel3, "cell 0 5")

        setSize(250, 100)
        isResizable = false
        pack()
        setLocationRelativeTo(owner)
    }
    //</editor-fold>
}

class TransformerTestDialog(owner: Window?, transformer: String, value: String) : JDialog(owner) {
    private val transformer: String
    private val value: String

    private val nameLabel = JLabel()
    private val output = JTextArea()
    private val panel3 = JPanel()
    private val okButton = JButton()

    init {
        this.transformer = transformer
        this.value = value
        initComponents()
        this.defaultCloseOperation = DISPOSE_ON_CLOSE
    }

    private fun ok() {
        this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }

    //<editor-fold desc="UI layout cruft">
    private fun initComponents() {
        contentPane.layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")

        nameLabel.text = "Output"
        contentPane.add(nameLabel, "cell 0 0")

        output.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        output.isEditable = false
        output.lineWrap = true
        output.columns = 80
        output.rows = 10
        val transformed = evalTransformer(value, transformer)
        if (transformed.err == "") {
            output.text = transformed.out
        } else {
            nameLabel.text = "Error"
            output.text = transformed.err
        }
        contentPane.add(JScrollPane(output), "cell 0 1")

        panel3.layout = MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")

        okButton.text = "OK"
        okButton.background = UIManager.getColor("Button.background")
        okButton.font = okButton.font.deriveFont(okButton.font.style or Font.BOLD)
        okButton.addActionListener { ok() }

        panel3.add(okButton, "west,gapx null 10")
        contentPane.add(panel3, "cell 0 4")

        setSize(250, 100)
        isResizable = false
        pack()
        setLocationRelativeTo(owner)
    }
    //</editor-fold>
}
