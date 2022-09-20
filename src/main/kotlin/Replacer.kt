package burp

import burp.api.montoya.MontoyaApi
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

data class Response (var matched: Boolean, var contents: String)

interface ReplaceStrategy {
    fun updateValue(request: String, match: String): Response
    fun matchAndReplace(request: String, key: String, value: String): Response {
        val res = Response(false, "")

        if (request.contains("\$$key\$")) {
            res.matched = true
            res.contents = request.replace("\$$key\$", value)
        }

        return res
    }
}

class RegexStrategy : ReplaceStrategy {
    override fun updateValue(request: String, match: String): Response {
        val res = Response(false, "")
        val pattern: Pattern
        try {
            pattern = Pattern.compile(match, Pattern.MULTILINE)
        } catch (e: PatternSyntaxException) {
            log.error("Invalid regex pattern: $match")
            return res
        }

        val matcher = pattern.matcher(request)

        while (matcher.find()) {
            res.matched = true
            res.contents = matcher.group("val")
        }

        return res
    }
}

class HeaderStrategy : ReplaceStrategy {
    override fun updateValue(request: String, match: String): Response {
        val res = Response(false, "")
        val headers = request.split("\r\n\r\n")[0]
        val lookup = "$match: "

        if (headers.contains(lookup)) {
            res.matched = true
            res.contents = headers.substringAfter(lookup).substringBefore("\r\n")
        }

        return res
    }
}

class Replacer (api: MontoyaApi, itemStore: ItemStore) {
    private val itemStore: ItemStore
    private val api: MontoyaApi

    private val strategies: Map<ItemType, ReplaceStrategy> = mapOf(
        ItemType.REGEX to RegexStrategy(),
        ItemType.HEADER to HeaderStrategy(),
    )

    init {
        this.itemStore = itemStore
        this.api = api
    }

    fun handleRequest(request: String) : String {
        // replaces values if necessary
        var needsSync = false
        var updatedRequest = request

        itemStore.items.forEach {
            if (!it.value.enabled) return@forEach
            val resp = strategies[it.value.type]?.matchAndReplace(updatedRequest, it.key, it.value.lastMatch)!!

            if (resp.matched) {
                it.value.replaceCount += 1
                updatedRequest = resp.contents
                needsSync = true
                log.debug("Found placeholder for item: ${it.key}")
            }
        }

        if (needsSync) itemStore.syncToStore()
        return updatedRequest
    }

    fun handleResponse(response: String) {
        // updates last values in store
        var needsSync = false

        itemStore.items.forEach {
            if (!it.value.enabled) return@forEach
            val resp = strategies[it.value.type]?.updateValue(response, it.value.match)!!

            if (resp.matched) {
                if (resp.contents == it.value.lastMatch) return@forEach
                it.value.matchCount += 1
                it.value.lastMatch = resp.contents
                needsSync = true
                log.debug("Replaced value for: ${it.key}, new value ${resp.contents}")
            }
        }

        if (needsSync) itemStore.syncToStore()
    }
}