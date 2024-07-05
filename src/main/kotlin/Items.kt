package burp

import kotlinx.serialization.Serializable

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
    var requestFilters: RequestFilterChain? = null,
    var responseFilters: ResponseFilterChain? = null
)
