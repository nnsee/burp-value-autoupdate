package burp

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.core.ToolType.*
import burp.api.montoya.persistence.PersistenceContext
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Window
import java.awt.event.ItemEvent
import java.awt.event.ItemEvent.SELECTED
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.JOptionPane.showMessageDialog
import javax.swing.SwingUtilities.getWindowAncestor
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

val VALUES_TABLE = JTable()

private fun reloadValuesTable(items: Items) {
    val dtm = VALUES_TABLE.model as DefaultTableModel
    dtm.dataVector.removeAllElements()

    items.forEach {
        dtm.addRow(
            arrayOf(
                it.value.enabled, it.key, it.value.match, it.value.lastMatch, it.value.matchCount, it.value.replaceCount
            )
        )
    }

    dtm.fireTableDataChanged()
}

private fun rowToName(row: Int): String {
    val dtm = VALUES_TABLE.model

    return dtm.getValueAt(row, 1).toString()
}

class UI(api: MontoyaApi, itemStore: ItemStore) : JPanel() {
    private val api: MontoyaApi
    private val ctx: PersistenceContext
    private val itemStore: ItemStore
    private var extEnabled = true
    private var enabledTools = mutableMapOf(
        PROXY to true,
        REPEATER to true,
        SCANNER to false,
        SEQUENCER to true,
        INTRUDER to true,
        EXTENDER to false,
    )

    private val headerPanel = JPanel()
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
    private val valueRefresh = JButton()
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

    init {
        this.api = api
        this.ctx = api.persistence().userContext()
        this.itemStore = itemStore
        initComponents()
        loadValuesFromStore()
        api.userInterface().registerSuiteTab("Value updater", this)
//        Thread.sleep(1_000)
//        log.debug(this.parent.toString())
//        this.parent.components.forEach {
//            if (it::class.simpleName == "TabContainer") {
//                log.debug(it.toString())
//                (it as JTabbedPane).components.reversed().forEach { comp ->
//                    log.debug(comp.toString())
//                }
//                return@forEach
//            }
//        }
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
        enabledTools[EXTENDER] = ctx.getBoolean("extenderE") ?: enabledTools[EXTENDER]!!
        extenderSel.isSelected = enabledTools[EXTENDER]!!

        reloadValuesTable(itemStore.items)
    }


    fun isEnabled(tool: ToolType): Boolean {
        return extEnabled and enabledTools[tool]!!
    }

    private fun valueAdd() {
        val window = AddEditDialog(getWindowAncestor(this), -1, itemStore)
        window.title = "Add value"
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

        val window = AddEditDialog(getWindowAncestor(this), index, itemStore)
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

    private fun valueRefresh() {
        reloadValuesTable(itemStore.items)
    }

    private fun enabledToggle(e: ItemEvent) {
        extEnabled = e.stateChange == SELECTED
        ctx.setBoolean("extEnabled", extEnabled)
    }

    private fun proxySel(e: ItemEvent) {
        enabledTools[PROXY] = e.stateChange == SELECTED
        ctx.setBoolean("proxyE", enabledTools[PROXY])
    }

    private fun scannerSel(e: ItemEvent) {
        enabledTools[SCANNER] = e.stateChange == SELECTED
        ctx.setBoolean("scannerE", enabledTools[SCANNER])
    }

    private fun intruderSel(e: ItemEvent) {
        enabledTools[INTRUDER] = e.stateChange == SELECTED
        ctx.setBoolean("intruderE", enabledTools[INTRUDER])
    }

    private fun repeaterSel(e: ItemEvent) {
        enabledTools[REPEATER] = e.stateChange == SELECTED
        ctx.setBoolean("repeaterE", enabledTools[REPEATER])
    }

    private fun sequencerSel(e: ItemEvent) {
        enabledTools[SEQUENCER] = e.stateChange == SELECTED
        ctx.setBoolean("sequencerE", enabledTools[SEQUENCER])
    }

    private fun extenderSel(e: ItemEvent) {
        enabledTools[EXTENDER] = e.stateChange == SELECTED
        ctx.setBoolean("extenderE", enabledTools[EXTENDER])
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
            }

            1 -> {
                valueRemove.isEnabled = true
                valueEdit.isEnabled = true
            }

            else -> {
                valueRemove.isEnabled = true
                valueEdit.isEnabled = false
            }
        }
    }

    //<editor-fold desc="UI layout cruft">
    private fun initComponents() {
        layout = MigLayout("hidemode 3", "[fill][fill]", "[][][][][]")

        headerPanel.layout = MigLayout("hidemode 3", "[fill]", "[][][]")

        mainLabel.text = "Value Autoupdater"
        mainLabel.font = mainLabel.font.deriveFont(mainLabel.font.style or Font.BOLD)
        headerPanel.add(mainLabel, "cell 0 0")

        headerNestedPanel.layout = MigLayout("hidemode 3", "[fill]", "[]")

        enabledToggle.text = "Enabled"
        enabledToggle.addItemListener { e: ItemEvent -> enabledToggle(e) }
        headerNestedPanel.add(enabledToggle, "cell 0 0")
        headerPanel.add(headerNestedPanel, "cell 0 1")
        add(headerPanel, "cell 0 0")
        add(separator2, "cell 0 1")

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

        valueRefresh.text = "Refresh"
        valueRefresh.addActionListener { valueRefresh() }
        valueButtons.add(valueRefresh, "cell 0 3")
        valueSelectorPanel.add(valueButtons, "cell 0 0,aligny top,growy 0")

        valuesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        valuesTable.model = object : DefaultTableModel(
            arrayOf(), arrayOf(
                "", "Name", "Match", "Last value", "Times updated", "Times replaced"
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
        cm.getColumn(0).preferredWidth = 25
        valuesTable.preferredScrollableViewportSize = Dimension(600, 300)
        (valuesTable.model as DefaultTableModel).addTableModelListener { e: TableModelEvent -> tableEdit(e) }
        valuesTable.selectionModel.addListSelectionListener { tableSelected() }

        valuesTablePanel.setViewportView(valuesTable)
        valueSelectorPanel.add(valuesTablePanel, "cell 1 0")
        valuesPanel.add(valueSelectorPanel, "cell 0 2")
        add(valuesPanel, "cell 0 2")
        add(separator1, "cell 0 3")

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
        add(toolsPanel, "cell 0 4")
    }

    //</editor-fold>
}

class AddEditDialog(owner: Window?, index: Int, itemStore: ItemStore) : JDialog(owner) {
    private val headerTypeHint = "Matches header names and replaces values "
    private val regexTypeHint = "Uses regex for matches (named group: val)"
    private var index: Int
    private val itemStore: ItemStore

    private val panel1 = JPanel()
    private val nameLabel = JLabel()
    private val nameField = JTextField()
    private val matchLabel = JLabel()
    private val matchField = JTextField()
    private val typeField = JLabel()
    private val headerButton = JRadioButton()
    private val regexButton = JRadioButton()
    private val typeDescription = JLabel()
    private val errorLabel = JLabel()
    private val panel3 = JPanel()
    private val okButton = JButton()
    private val applyButton = JButton()
    private val cancelButton = JButton()

    init {
        this.index = index
        this.itemStore = itemStore
        initComponents()
        if (index != -1) {
            loadStoredValues()
        }
        this.defaultCloseOperation = DISPOSE_ON_CLOSE
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
            val item = Item(match, type, "", true, 0, 0)
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
        val contentPane = contentPane
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
