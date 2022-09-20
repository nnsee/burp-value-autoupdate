package burp

import burp.api.montoya.persistence.PersistenceContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

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
)

typealias Items = MutableMap<String, Item> // name -> item

class ItemStore(ctx: PersistenceContext) {
    private val ctx: PersistenceContext
    var items: Items

    init {
        this.ctx = ctx
        this.items = readFromStore()
    }

    private fun readFromStore() : Items {
        var jsonStr = ctx.getString(ITEM_STORE) ?: "{}"

        return Json.decodeFromString(jsonStr)
    }

    fun syncToStore() {
       ctx.setString(ITEM_STORE, Json.encodeToString(items))
    }

    fun nuke() {
        ctx.delete(ITEM_STORE)
    }
}