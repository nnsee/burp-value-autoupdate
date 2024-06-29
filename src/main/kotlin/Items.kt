package burp

import burp.api.montoya.persistence.Preferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val ITEM_STORE = "itemStore"

enum class ItemType {
    HEADER, REGEX
}

@Serializable
data class Item(
    var match: String,
    var type: ItemType,
    var lastMatch: String,
    var enabled: Boolean,
    var matchCount: Int,
    var replaceCount: Int,
    var transformer: String,
)

typealias Items = MutableMap<String, Item> // name -> item

class ItemStore(private val ctx: Preferences) {
    var items: Items

    init {
        this.items = load()
    }

    private fun load(): Items {
        // loads and returns items from persistent storage
        val jsonStr = ctx.getString(ITEM_STORE) ?: "{}"

        return Json.decodeFromString(jsonStr)
    }

    fun save() {
        // saves items in memory to disk
        ctx.setString(ITEM_STORE, Json.encodeToString(items))
    }

    @Suppress("unused")
    fun nuke() {
        ctx.deleteString(ITEM_STORE)
    }
}
