package burp.ui

import burp.ItemStore
import burp.Items
import burp.TransformerStore
import burp.Transformers
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.core.ToolType.*
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
import javax.swing.table.TableModel

const val TRANSFORMER_EDITOR_HINT = """// Transformer function JavaScript code

// value: string - the value to transform
// Lib: object - the library object (contains lodash, etc.)
// returns: string - the transformed value (last line is returned implicitly)

// Example 1:
// value.toUpperCase()

// Example 2:
// Lib._.camelCase(value)

"""

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

class MainActivity(api: MontoyaApi, private val itemStore: ItemStore, private val transformerStore: TransformerStore) :
    JPanel() {
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

    private val transformerEditor = Editor({ editorTyped() }, { transformerSave() }).textArea

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
        entries.filter { it in enabledTools }.forEach { tool ->
            toolSelectionMap[tool] = JCheckBox(tool.toolName()).apply {
                isSelected = enabledTools[tool] ?: false
                addItemListener { e: ItemEvent -> toolSelected(tool, e) }
            }
        }

        layout = MigLayout("fill, align center top", "[fill]")

        add(JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            JPanel(MigLayout("fillx, align left top", "[fill]")).apply {
                add(JLabel("Values to Track").apply { font = font.deriveFont(Font.BOLD) }, "wrap")
                add(JPanel(MigLayout("", "[fill][fill]")).apply {
                    add(JPanel(MigLayout("", "[fill]")).apply {
                        add(JButton("Add").apply { addActionListener { valueAdd() } }, "wrap")
                        add(valueEdit, "wrap")
                        add(valueRemove, "wrap")
                    }, "aligny top, growy 0")
                    add(JScrollPane(VALUES_TABLE), "grow, push")
                }, "wrap")
                add(JSeparator(), "growx, wrap")
                add(JLabel("Enabled tools").apply { font = font.deriveFont(Font.BOLD) }, "wrap")
                add(JPanel(MigLayout("", "[fill][fill][fill]")).apply {
                    toolSelectionMap.values.forEachIndexed { index, checkBox ->
                        add(checkBox, "cell ${index % 3} ${index / 3}")
                    }
                }, "wrap")
                add(JSeparator(), "growx, wrap")
                add(JLabel("Settings").apply { font = font.deriveFont(Font.BOLD) }, "wrap")
                add(JPanel(MigLayout()).apply { add(enabledToggle) }, "wrap")
            },
            JPanel(MigLayout("fill, align left top", "[fill]")).apply {
                add(JLabel("Value Transformers").apply { font = font.deriveFont(Font.BOLD) }, "wrap")
                add(JPanel(MigLayout("", "[fill][fill]")).apply {
                    add(JPanel(MigLayout("", "[fill]")).apply {
                        add(JButton("Add").apply { addActionListener { transformerAdd() } }, "wrap")
                        add(transformerRemove, "wrap")
                    }, "aligny top, growy")
                    add(JScrollPane(TRANSFORMER_TABLE), "grow, push")
                }, "wrap")
                add(transformerEditorPanel, "grow, push")
            }
        ).apply {
            resizeWeight = 0.5
        }, "grow, push, span"
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
        val window = AddEditDialog(
            getWindowAncestor(this),
            -1,
            itemStore,
            transformerStore,
            { reloadValuesTable(itemStore.items) },
            { rowToName(-1) })
        window.title = "Add value"
        window.isVisible = true
    }

    private fun transformerAdd() {
        val window = TransformerAddDialog(getWindowAncestor(this), transformerStore) {
            reloadTransformersTable(
                transformerStore.transformers
            )
        }
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

        val window = AddEditDialog(
            getWindowAncestor(this),
            index,
            itemStore,
            transformerStore,
            { reloadValuesTable(itemStore.items) },
            { rowToName(index) })
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
        transformerEditor.text = ""

        reloadTransformersTable(transformerStore.transformers)
    }

    private fun transformerSave() {
        transformerStore.transformers[rowToTName(TRANSFORMER_TABLE.selectedRow)] = transformerEditor.text
        transformerStore.save()

        transformerEditorSave.isEnabled = false
    }

    private fun transformerTest() {
        val window = TransformerTestDialog(
            getWindowAncestor(this),
            transformerEditor.text,
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
                transformerEditor.text =
                    transformerStore.transformers[rowToTName(TRANSFORMER_TABLE.selectedRow)]
                if (VALUES_TABLE.selectedRowCount == 1) transformerEditorTest.isEnabled = true
                transformerEditorPanel.isVisible = true
                transformerEditor.requestFocusInWindow()
            }
        }
    }

    private fun editorTyped() {
        if (TRANSFORMER_TABLE.selectedRowCount != 0) {
            transformerEditorSave.isEnabled = true
        }
    }
}
