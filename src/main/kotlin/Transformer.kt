package burp

import burp.api.montoya.persistence.Preferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.InputStreamReader

var LIB: Value? = null
var DONE_LIB_INIT = false
const val TRANSFORMER_STORE = "transformerStore"

data class TransformerResult(var out: String, var err: String)

fun initTransformerBundle(): Value? {
    DONE_LIB_INIT = true
    val pluginClassLoader = object {}.javaClass.classLoader
    val context = Context.newBuilder("js")
        .allowExperimentalOptions(true)
        .option("js.esm-eval-returns-exports", "true")
        .build()
    val resource = pluginClassLoader.getResourceAsStream("bundle.mjs")
    if (resource == null) {
        log.error("Failed to load bundled JavaScript libraries!")
        return null
    }
    val src = InputStreamReader(resource)
    val source = Source.newBuilder("js", src, "lib.mjs").build()
    return context.eval(source)
}

fun evalTransformer(value: String, transformer: String): TransformerResult {
    var res = TransformerResult("", "")

    if (!DONE_LIB_INIT) {
        LIB = initTransformerBundle()
    }
    val context = Context.create()
    val bindings = context.getBindings("js")
    bindings.putMember("value", value)
    bindings.putMember("Lib", LIB)
    try {
        res.out = context.eval("js", transformer).asString()
    } catch (e: Exception) {
        var error = "Transformer exception!"
        e.message?.let {
            error += "\n${it}"
            res.err = it
        }
        log.error(error)
    }
    context.close()

    return res
}

typealias Transformers = MutableMap<String, String> // name -> transformer

class TransformerStore(ctx: Preferences) {
    // todo: unify stores somehow
    private val ctx: Preferences
    var transformers: Transformers

    init {
        this.ctx = ctx
        this.transformers = load()
    }

    private fun load(): Transformers {
        // loads and returns transformers from persistent storage
        var jsonStr = ctx.getString(TRANSFORMER_STORE) ?: "{}"

        return Json.decodeFromString(jsonStr)
    }

    fun save() {
        // saves items in memory to disk
        ctx.setString(TRANSFORMER_STORE, Json.encodeToString(transformers))
    }

    fun nuke() {
        ctx.deleteString(TRANSFORMER_STORE)
    }
}
