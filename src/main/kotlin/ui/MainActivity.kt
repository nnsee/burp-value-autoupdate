package burp.ui

import burp.ItemStore
import burp.Items
import burp.TransformerStore
import burp.Transformers
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.persistence.Preferences
import net.miginfocom.swing.MigLayout
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.JOptionPane.showMessageDialog
import javax.swing.SwingUtilities.getWindowAncestor
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableModel

class MainActivity(
    api: MontoyaApi,
    private val itemStore: ItemStore,
    private val transformerStore: TransformerStore
) : JPanel() {

    private val ctx: Preferences = api.persistence().preferences()
    private var extEnabled = true
    private var enabledTools = mutableMapOf(
        ToolType.PROXY to true,
        ToolType.REPEATER to true,
        ToolType.SCANNER to false,
        ToolType.SEQUENCER to true,
        ToolType.INTRUDER to true,
        ToolType.EXTENSIONS to false,
    )
    private val toolSelectionMap = mutableMapOf<ToolType, JCheckBox>()

    private var namedRows = mapOf<String, Int>()
    private var namedTRows = mapOf<String, Int>()
    private var tabVisible = false

    private val valuesTable = createValuesTable()
    private val transformerTable = createTransformersTable()

    private val enabledToggle = JCheckBox("Extension enabled").apply {
        addItemListener { e: ItemEvent -> toggleExtensionEnabled(e) }
    }

    private val valueEdit = createButton("Edit", ::editValue)
    private val valueRemove = createButton("Remove", ::removeValue)
    private val transformerRemove = createButton("Remove", ::removeTransformer)
    private val transformerEditorSave = createButton("Save", ::saveTransformer)
    private val transformerEditorTest = createButton("Test", ::testTransformer)

    private val transformerEditor = Editor(::onEditorTyped, ::saveTransformer).textArea

    private val transformerEditorPanel = JPanel(MigLayout("", "[]", "[][]")).apply {
        isVisible = false
        add(transformerEditorSave)
        add(transformerEditorTest, "wrap")
        add(RTextScrollPane(transformerEditor).apply {
            lineNumbersEnabled = true
            isEnabled = true
        }, "span, grow, push")
    }

    init {
        setupUI()
        loadValuesFromStore()
        api.userInterface().registerSuiteTab("Value tracker", this)
    }

    private fun setupUI() {
        layout = MigLayout("fill, align center top", "[fill]")

        enabledTools.keys.forEach { tool ->
            toolSelectionMap[tool] = JCheckBox(tool.toolName()).apply {
                isSelected = enabledTools[tool] ?: false
                addItemListener { e: ItemEvent -> toggleToolSelected(tool, e) }
            }
        }

        add(
            JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createLeftPanel(),
                createRightPanel()
            ).apply {
                resizeWeight = 0.5
            },
            "grow, push, span"
        )

        (valuesTable.model as DefaultTableModel).addTableModelListener { e: TableModelEvent -> onTableEdit(e) }
        valuesTable.selectionModel.addListSelectionListener { onTableSelected() }
        transformerTable.selectionModel.addListSelectionListener { onTransformerSelected() }

        addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                tabVisible = true
                reloadValuesTable(itemStore.items)
            }

            override fun componentHidden(e: ComponentEvent?) {
                tabVisible = false
            }
        })
    }

    private fun createLeftPanel() = JPanel(MigLayout("fillx, align left top", "[fill]")).apply {
        add(JLabel("Values to Track").apply { font = font.deriveFont(Font.BOLD) }, "wrap")
        add(createValuesPanel(), "wrap")
        add(JSeparator(), "growx, wrap")
        add(JLabel("Enabled tools").apply { font = font.deriveFont(Font.BOLD) }, "wrap")
        add(createToolsPanel(), "wrap")
        add(JSeparator(), "growx, wrap")
        add(JLabel("Settings").apply { font = font.deriveFont(Font.BOLD) }, "wrap")
        add(JPanel(MigLayout()).apply { add(enabledToggle) }, "wrap")
    }

    private fun createValuesPanel() = JPanel(MigLayout("", "[fill][fill]")).apply {
        add(JPanel(MigLayout("ins 0", "[fill]")).apply {
            add(createButton("Add", ::addValue).apply { isEnabled = true }, "wrap")
            add(valueEdit, "wrap")
            add(valueRemove, "wrap")
        }, "aligny top, growy 0")
        add(JScrollPane(valuesTable), "grow, push")
    }

    private fun createToolsPanel() = JPanel(MigLayout("", "[fill][fill][fill]")).apply {
        toolSelectionMap.values.forEachIndexed { index, checkBox ->
            add(checkBox, "cell ${index % 3} ${index / 3}")
        }
    }

    private fun createRightPanel() = JPanel(MigLayout("fill, align left top", "[fill]")).apply {
        add(JLabel("Value Transformers").apply { font = font.deriveFont(Font.BOLD) }, "wrap")
        add(createTransformersPanel(), "wrap")
        add(transformerEditorPanel, "grow, push")
    }

    private fun createTransformersPanel() = JPanel(MigLayout("", "[fill][fill]")).apply {
        add(JPanel(MigLayout("ins 0", "[fill]")).apply {
            add(createButton("Add", ::addTransformer).apply { isEnabled = true }, "wrap")
            add(transformerRemove, "wrap")
        }, "aligny top, growy")
        add(JScrollPane(transformerTable), "grow, push")
    }

    private fun createButton(text: String, action: () -> Unit) = JButton(text).apply {
        addActionListener { action() }
        isEnabled = false
    }

    private fun createValuesTable() = JTable().apply {
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        model = object : DefaultTableModel(
            arrayOf(), arrayOf(
                "", "Name", "Match", "Last value", "Times updated", "Times replaced", "Transformer"
            )
        ) {
            override fun getColumnClass(columnIndex: Int): Class<*> {
                return if (columnIndex == 0) Boolean::class.javaObjectType else String::class.javaObjectType
            }

            override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
                return columnIndex == 0  // enabled button
            }
        }

        columnModel.getColumn(0).preferredWidth = 25
    }

    private fun createTransformersTable() = JTable().apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        model = object : DefaultTableModel(
            arrayOf(), arrayOf("Name")
        ) {
            override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
                return false
            }
        }
    }

    private fun reloadValuesTable(items: Items) {
        val dtm = valuesTable.model as DefaultTableModel
        val tmpMap = mutableMapOf<String, Int>()

        valuesTable.clearSelection()
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
        val dtm = transformerTable.model as DefaultTableModel
        val tmpMap = mutableMapOf<String, Int>()

        transformerTable.clearSelection()
        dtm.dataVector.removeAllElements()

        transformers.forEach {
            dtm.addRow(arrayOf(it.key))
            tmpMap[it.key] = dtm.rowCount - 1
        }

        dtm.fireTableDataChanged()
        namedTRows = tmpMap
    }

    private fun loadValuesFromStore() {
        extEnabled = ctx.getBoolean("extEnabled") ?: true
        enabledToggle.isSelected = extEnabled

        toolSelectionMap.forEach {
            if (!enabledTools.containsKey(it.key))
                return@forEach
            enabledTools[it.key] = ctx.getBoolean("${it.key.name}-enabled") ?: enabledTools[it.key]!!
            it.value.isSelected = enabledTools[it.key]!!
        }

        reloadValuesTable(itemStore.items)
        reloadTransformersTable(transformerStore.transformers)
    }

    private fun addValue() {
        val window = AddEditDialog(
            getWindowAncestor(this),
            -1,
            itemStore,
            transformerStore,
            ::reloadValuesTable,
            ::getRowName
        ) { tableLen(valuesTable) }
        window.title = "Add value"
        window.isVisible = true
    }

    private fun editValue() {
        if (valuesTable.selectedRowCount != 1) {
            showMessageDialog(null, "Can only edit 1 entry at a time!")
            return
        }

        val index = valuesTable.selectedRows[0]
        val window = AddEditDialog(
            getWindowAncestor(this),
            index,
            itemStore,
            transformerStore,
            ::reloadValuesTable,
            ::getRowName
        ) { tableLen(valuesTable) }
        window.title = "Edit value"
        window.isVisible = true
    }

    private fun removeValue() {
        val selectedRows = valuesTable.selectedRows.reversed()
        valuesTable.clearSelection() // visual bug workaround

        selectedRows.forEach { row ->
            itemStore.items.remove(getRowName(row))
        }

        itemStore.save()
        reloadValuesTable(itemStore.items)
    }

    private fun addTransformer() {
        val window = TransformerAddDialog(getWindowAncestor(this), transformerStore) {
            reloadTransformersTable(transformerStore.transformers)
        }
        window.title = "Add transformer"
        window.isVisible = true
    }

    private fun removeTransformer() {
        val selectedRows = transformerTable.selectedRows
        transformerTable.clearSelection() // visual bug workaround

        if (selectedRows.isEmpty()) return

        transformerStore.transformers.remove(getTransformerRowName(selectedRows[0]))
        transformerStore.save()
        transformerEditor.text = ""

        reloadTransformersTable(transformerStore.transformers)
    }

    private fun saveTransformer() {
        transformerStore.transformers[getTransformerRowName(transformerTable.selectedRow)] = transformerEditor.text
        transformerStore.save()
        transformerEditorSave.isEnabled = false
    }

    private fun testTransformer() {
        val window = TransformerTestDialog(
            getWindowAncestor(this),
            transformerEditor.text,
            itemStore.items[getRowName(valuesTable.selectedRow)]?.lastMatch ?: ""
        )
        window.title = "Transformer output"
        window.isVisible = true
    }

    private fun toggleExtensionEnabled(e: ItemEvent) {
        extEnabled = e.stateChange == ItemEvent.SELECTED
        ctx.setBoolean("extEnabled", extEnabled)
    }

    private fun toggleToolSelected(tool: ToolType, e: ItemEvent) {
        val enabled = e.stateChange == ItemEvent.SELECTED
        enabledTools[tool] = enabled
        ctx.setBoolean("${tool.name}-enabled", enabled)
    }

    private fun onEditorTyped() {
        if (transformerTable.selectedRowCount != 0) {
            transformerEditorSave.isEnabled = true
        }
    }

    private fun getRowName(row: Int): String {
        return valuesTable.model.getValueAt(row, 1).toString()
    }

    private fun getTransformerRowName(row: Int): String {
        return transformerTable.model.getValueAt(row, 0).toString()
    }

    private fun onTableEdit(e: TableModelEvent) {
        val dtm = e.source as DefaultTableModel
        if (dtm.rowCount == 0) return

        val index = e.firstRow
        val enabled = dtm.getValueAt(index, 0) as Boolean

        itemStore.items[getRowName(index)]!!.enabled = enabled
        itemStore.save()
    }

    private fun onTableSelected() {
        val selected = valuesTable.selectedRowCount
        valueRemove.isEnabled = selected > 0
        valueEdit.isEnabled = selected == 1
        transformerEditorTest.isEnabled = selected == 1 && transformerTable.selectedRowCount == 1
    }

    private fun onTransformerSelected() {
        transformerEditorSave.isEnabled = false
        val selected = transformerTable.selectedRowCount

        transformerRemove.isEnabled = selected > 0
        transformerEditorPanel.isVisible = selected > 0

        if (selected > 0) {
            transformerEditor.text = transformerStore.transformers[getTransformerRowName(transformerTable.selectedRow)]
            transformerEditorTest.isEnabled = valuesTable.selectedRowCount == 1
            transformerEditor.requestFocusInWindow()
        } else {
            transformerEditor.text = ""
        }
    }

    fun isEnabled(tool: ToolType): Boolean {
        return enabledTools[tool] ?: false
    }

    private fun tableLen(table: JTable): Int {
        return table.model.rowCount
    }

    fun updated(name: String, value: String, count: Int) {
        val item = itemStore.items[name] ?: return
        item.lastMatch = value
        item.matchCount = count
        itemStore.save()
    }

    fun replaced(name: String, count: Int) {
        val item = itemStore.items[name] ?: return
        item.replaceCount = count
        itemStore.save()
    }
}
