package burp

import burp.api.montoya.persistence.Preferences
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

enum class StoreType(val storeKey: String) {
    ITEM_STORE("itemStore"),
    TRANSFORMER_STORE("transformerStore");
}

typealias Transformers = MutableMap<String, String>
typealias Items = MutableMap<String, Item>

class TransformerStore(ctx: Preferences) : Store<String>(ctx, StoreType.TRANSFORMER_STORE, String.serializer())
class ItemStore(ctx: Preferences) : Store<Item>(ctx, StoreType.ITEM_STORE, Item.serializer())

abstract class Store<T>(private val ctx: Preferences, private val type: StoreType, private val serializer: KSerializer<T>) {
    var items: MutableMap<String, T>

    init {
        this.items = load()
    }

    private fun load(): MutableMap<String, T> {
        // loads and returns items from persistent storage
        val jsonStr = ctx.getString(type.storeKey) ?: "{}"
        return Json.decodeFromString(MapSerializer(String.serializer(), serializer), jsonStr).toMutableMap()
    }

    fun save() {
        // saves items in memory to disk
        ctx.setString(type.storeKey, Json.encodeToString(MapSerializer(String.serializer(), serializer), items))
    }

    @Suppress("unused")
    fun nuke() {
        ctx.deleteString(type.storeKey)
    }
}
