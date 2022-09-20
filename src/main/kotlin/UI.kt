package burp

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.core.ToolType.*
import burp.api.montoya.persistence.PersistenceContext
import net.miginfocom.swing.MigLayout
import java.awt.Dimension
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

private fun reloadValuesTable(items: Items) {
    val dtm = VALUES_TABLE.model as DefaultTableModel
    dtm.dataVector.removeAllElements()

    items.forEach {
        dtm.addRow(arrayOf(it.value.enabled, it.key, it.value.match, it.value.lastMatch, it.value.matchCount, it.value.replaceCount))
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

    init {
        this.api = api
        this.ctx = api.persistence().userContext()
        this.itemStore = itemStore
        initComponents()
        loadValuesFromStore()
        api.userInterface().registerSuiteTab("Value updater", this)
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

    private fun valueAdd(e: ActionEvent) {
        val window = AddEditDialog(getWindowAncestor(this), api, -1, itemStore)
        window.title = "Add value"
        window.isVisible = true
    }

    private fun valueEdit(e: ActionEvent) {
        val selected = valuesTable.selectedRowCount
        if (selected < 1) {
            return
        }
        if (selected != 1) {
            showMessageDialog(null, "Can only edit 1 entry at a time!")
            return
        }

        val index = valuesTable.selectedRows[0]

        val window = AddEditDialog(getWindowAncestor(this), api, index, itemStore)
        window.title = "Edit value"
        window.isVisible = true
    }

    private fun valueRemove(e: ActionEvent) {
        val selectedRows = valuesTable.selectedRows.reversed()
        VALUES_TABLE.clearSelection() // visual bug work around

        for (row in selectedRows) {
            itemStore.items.remove(rowToName(row))
        }

        itemStore.syncToStore()

        reloadValuesTable(itemStore.items)
    }

    private fun valueRefresh(e: ActionEvent) {
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
        itemStore.syncToStore()
    }

    //<editor-fold desc="UI layout cruft">
    private fun initComponents() {
        headerPanel = JPanel()
        mainLabel = JLabel()
        headerNestedPanel = JPanel()
        enabledToggle = JCheckBox()
        separator2 = JSeparator()
        valuesPanel = JPanel()
        valuesLabel = JLabel()
        valueSelectorPanel = JPanel()
        valueButtons = JPanel()
        valueAdd = JButton()
        valueEdit = JButton()
        valueRemove = JButton()
        valueRefresh = JButton()
        valuesTablePanel = JScrollPane()
        valuesTable = VALUES_TABLE
        separator1 = JSeparator()
        toolsPanel = JPanel()
        toolsLabel = JLabel()
        toolSelectionPanel = JPanel()
        proxySel = JCheckBox()
        scannerSel = JCheckBox()
        intruderSel = JCheckBox()
        repeaterSel = JCheckBox()
        sequencerSel = JCheckBox()
        extenderSel = JCheckBox()
        tableSel = JCheckBox()

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
        valueAdd.addActionListener { e: ActionEvent -> valueAdd(e) }
        valueButtons.add(valueAdd, "cell 0 0")

        valueEdit.text = "Edit"
        valueEdit.addActionListener { e: ActionEvent -> valueEdit(e) }
        valueButtons.add(valueEdit, "cell 0 1")

        valueRemove.text = "Remove"
        valueRemove.addActionListener { e: ActionEvent -> valueRemove(e) }
        valueButtons.add(valueRemove, "cell 0 2")

        valueRefresh.text = "Refresh"
        valueRefresh.addActionListener { e: ActionEvent -> valueRefresh(e) }
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

    private lateinit var headerPanel: JPanel
    private lateinit var mainLabel: JLabel
    private lateinit var headerNestedPanel: JPanel
    private lateinit var enabledToggle: JCheckBox
    private lateinit var separator2: JSeparator
    private lateinit var valuesPanel: JPanel
    private lateinit var valuesLabel: JLabel
    private lateinit var valueSelectorPanel: JPanel
    private lateinit var valueButtons: JPanel
    private lateinit var valueAdd: JButton
    private lateinit var valueEdit: JButton
    private lateinit var valueRemove: JButton
    private lateinit var valueRefresh: JButton
    private lateinit var valuesTablePanel: JScrollPane
    private lateinit var valuesTable: JTable
    private lateinit var separator1: JSeparator
    private lateinit var toolsPanel: JPanel
    private lateinit var toolsLabel: JLabel
    private lateinit var toolSelectionPanel: JPanel
    private lateinit var proxySel: JCheckBox
    private lateinit var scannerSel: JCheckBox
    private lateinit var intruderSel: JCheckBox
    private lateinit var repeaterSel: JCheckBox
    private lateinit var sequencerSel: JCheckBox
    private lateinit var extenderSel: JCheckBox
    private lateinit var tableSel: JCheckBox
    //</editor-fold>
}

class AddEditDialog(owner: Window?, api: MontoyaApi, index: Int, itemStore: ItemStore) : JDialog(owner) {
    private val headerTypeHint = "Matches header names and replaces values "
    private val regexTypeHint  = "Uses regex for matches (named group: val)"
    private var index: Int
    private val itemStore: ItemStore

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

    private fun nameFieldKeyTyped(e: KeyEvent) {
        enableApply()
    }

    private fun matchFieldKeyTyped(e: KeyEvent) {
        enableApply()
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

    private fun ok(e: ActionEvent) {
        if (apply(e)) this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }

    private fun apply(e: ActionEvent): Boolean {
        applyButton.isEnabled = false
        val name = nameField.text
        val match = matchField.text
        val type = if (regexButton.isSelected) ItemType.REGEX else ItemType.HEADER

        if (index == -1) {
            val item = Item(match, type, "", true, 0, 0)
            if (itemStore.items[name] != null) {
                // todo add error
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

        itemStore.syncToStore()
        reloadValuesTable(itemStore.items)

        return true
    }

    private fun cancel(e: ActionEvent) {
        this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }

    //<editor-fold desc="UI layout cruft">
    private fun initComponents() {
        panel1 = JPanel()
        nameLabel = JLabel()
        nameField = JTextField()
        matchLabel = JLabel()
        matchField = JTextField()
        typeField = JLabel()
        headerButton = JRadioButton()
        regexButton = JRadioButton()
        typeDescription = JLabel()
        panel3 = JPanel()
        okButton = JButton()
        applyButton = JButton()
        cancelButton = JButton()

        val contentPane = contentPane
        contentPane.layout = MigLayout("hidemode 3", "[fill][fill][fill][fill][fill]", "[][][][][][][]")

        panel1.layout = MigLayout("hidemode 3", "[fill][fill]", "[][][][]")

        nameLabel.text = "Name"
        panel1.add(nameLabel, "cell 0 0")

        nameField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                nameFieldKeyTyped(e)
            }
        })
        panel1.add(nameField, "cell 1 0,wmin 270,grow 0")

        matchLabel.text = "Match"
        panel1.add(matchLabel, "cell 0 1")

        matchField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                matchFieldKeyTyped(e)
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

        panel3.layout = MigLayout("fillx,hidemode 3", "[fill][fill][fill][fill][fill]", "[fill]")

        okButton.text = "OK"
        okButton.background = UIManager.getColor("Button.background")
        okButton.font = okButton.font.deriveFont(okButton.font.style or Font.BOLD)
        okButton.addActionListener { e: ActionEvent -> ok(e) }
        panel3.add(okButton, "west,gapx null 10")

        applyButton.text = "Apply"
        applyButton.isEnabled = false
        applyButton.addActionListener { e: ActionEvent -> apply(e) }
        panel3.add(applyButton, "west")

        cancelButton.text = "Cancel"
        cancelButton.addActionListener { e: ActionEvent -> cancel(e) }
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

    private lateinit var panel1: JPanel
    private lateinit var nameLabel: JLabel
    private lateinit var nameField: JTextField
    private lateinit var matchLabel: JLabel
    private lateinit var matchField: JTextField
    private lateinit var typeField: JLabel
    private lateinit var headerButton: JRadioButton
    private lateinit var regexButton: JRadioButton
    private lateinit var typeDescription: JLabel
    private lateinit var panel3: JPanel
    private lateinit var okButton: JButton
    private lateinit var applyButton: JButton
    private lateinit var cancelButton: JButton
    //</editor-fold>
}
