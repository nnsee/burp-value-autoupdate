package burp

import burp.api.montoya.MontoyaApi
import com.google.re2j.Pattern
import com.google.re2j.PatternSyntaxException

enum class ReplaceType {
    REQUEST, RESPONSE
}

data class Response(var matched: Boolean, var contents: String)
data class ReplacedValue(val old: String, val new: String)
data class ReplaceResult(
    var matched: Boolean, var contents: String, val type: ReplaceType, var values: MutableList<ReplacedValue>
)

val regexCache = mutableMapOf<String, Pattern>()

interface ReplaceStrategy {
    fun updateValue(request: String, match: String): Response
    fun matchAndReplace(request: String, key: String, item: Item, transformerStore: TransformerStore): Response {
        val res = Response(false, "")

        if (request.contains("\$$key\$")) {
            res.matched = true
            var value = item.lastMatch
            if (item.transformer != "") {
                transformerStore.transformers[item.transformer]?.let {
                    val transformed = evalTransformer(value, it)
                    if (transformed.err == "")
                        value = transformed.out
                }
            }
            res.contents = fixContentLength(request.replace("\$$key\$", value))
        }

        return res
    }
}

class RegexStrategy : ReplaceStrategy {
    override fun updateValue(request: String, match: String): Response {
        val res = Response(false, "")

        if (match !in regexCache) {
            log.debug("First time seeing pattern, compiling: $match")
            regexCache[match] = Pattern.compile(match, Pattern.MULTILINE)
        }

        val matcher = regexCache[match]!!.matcher(request)

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

class Replacer(api: MontoyaApi, itemStore: ItemStore, transformerStore: TransformerStore) {
    private val itemStore: ItemStore
    private val transformerStore: TransformerStore
    private val api: MontoyaApi

    private val strategies: Map<ItemType, ReplaceStrategy> = mapOf(
        ItemType.REGEX to RegexStrategy(),
        ItemType.HEADER to HeaderStrategy(),
    )

    init {
        this.itemStore = itemStore
        this.transformerStore = transformerStore
        this.api = api
    }

    fun handleRequest(request: String): ReplaceResult {
        // replaces values if necessary
        val result = ReplaceResult(false, request, ReplaceType.REQUEST, mutableListOf())

        itemStore.items.forEach {
            if (!it.value.enabled) return@forEach
            val resp = strategies[it.value.type]?.matchAndReplace(result.contents, it.key, it.value, transformerStore)!!

            if (resp.matched) {
                it.value.replaceCount += 1
                result.contents = resp.contents
                result.matched = true
                result.values.add(ReplacedValue(it.key, it.value.lastMatch))
                replaced(it.key, it.value.replaceCount)
                log.debug("Found placeholder for item: ${it.key}")
            }
        }

        if (result.matched) itemStore.save()
        return result
    }

    fun handleResponse(response: String): ReplaceResult {
        // updates last values in store
        val result = ReplaceResult(false, "", ReplaceType.RESPONSE, mutableListOf())

        itemStore.items.forEach {
            if (!it.value.enabled) return@forEach
            val resp = strategies[it.value.type]?.updateValue(response, it.value.match)!!

            if (resp.matched) {
                if (resp.contents == it.value.lastMatch) return@forEach
                it.value.matchCount += 1
                it.value.lastMatch = resp.contents
                result.matched = true
                result.values.add(ReplacedValue(it.key, resp.contents))
                updated(it.key, resp.contents, it.value.matchCount)
                log.debug("Replaced value for: ${it.key}, new value ${resp.contents}")
            }
        }

        if (result.matched) itemStore.save()
        return result
    }
}

fun checkRegexSyntax(re: String): String {
    try {
        val pattern = Pattern.compile(re, Pattern.MULTILINE)
        if ("val" !in pattern.namedGroups()) {
            return "Named group `val` not found!"
        }
    } catch (e: PatternSyntaxException) {
        return e.message ?: "Regex pattern has syntax errors!"
    }

    return ""
}

fun fixContentLength(content: String): String {
    val contentLengthHeader = "Content-Length"
    val parts = content.split("\r\n\r\n", limit = 2)
    if (parts.size == 1) return content // no body
    val headerLoc = parts[0].indexOf(contentLengthHeader, ignoreCase = true)
    if (headerLoc < 0) return content // no Content-Length header
    val valLoc = headerLoc + contentLengthHeader.length + 2 // ": "
    val valLen = content.substring(valLoc).indexOf("\r\n")

    return "${content.take(valLoc)}${parts[1].length}${content.substring(valLoc + valLen)}"
}