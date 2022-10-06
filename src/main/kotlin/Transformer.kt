package burp

import burp.api.montoya.persistence.PersistenceContext
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

fun initTransformerBundle(): Value? {
    DONE_LIB_INIT = true
    val context = Context.newBuilder("js")
        .allowIO(true)
        .allowExperimentalOptions(true)
        .option("js.esm-eval-returns-exports", "true")
        .build()
//    val src = "import * as Lib from \"/home/xx/Projects/transformerHook/bundle.mjs\";" + "Lib;"
//    InputStreamReader(it)
    val resource = object {}.javaClass.classLoader.getResourceAsStream("bundle.mjs")
    if (resource == null) {
        log.error("Failed to load bundled JavaScript libraries!")
        return null
    }
    val src = InputStreamReader(resource)
    val source = Source.newBuilder("js", src, "lib.mjs").build()
    return context.eval(source)
}

fun evalTransformer(value: String, transformer: Transformer): String {
    if (!DONE_LIB_INIT) {
        LIB = initTransformerBundle()
    }
    val context = Context.create()
    val bindings = context.getBindings("js")
    var res = value
    bindings.putMember("value", value)
    bindings.putMember("Lib", LIB)
    try {
        res = context.eval("js", transformer.code).asString()
    } catch (e: Exception) {
        var error = "Transformer exception!"
        e.message?.let {
            error += "\n${it}"
            transformer.error = it
        }
        log.error(error)
    }
    context.close()

    return res
}

@Serializable
data class Transformer(
    var code: String,
    var error: String,
)

typealias Transformers = MutableMap<String, Transformer> // name -> transformer

class TransformerStore(ctx: PersistenceContext) {
    // todo: unify stores somehow
    private val ctx: PersistenceContext
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
        ctx.delete(TRANSFORMER_STORE)
    }
}
