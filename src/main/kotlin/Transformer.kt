package burp

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.InputStreamReader

var LIB: Value? = null
var DONE_LIB_INIT = false

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
        Log.error("Failed to load bundled JavaScript libraries!")
        return null
    }
    val src = InputStreamReader(resource)
    val source = Source.newBuilder("js", src, "lib.mjs").build()
    return context.eval(source)
}

fun evalTransformer(value: String, values: Items, transformer: String): TransformerResult {
    val res = TransformerResult("", "")

    if (!DONE_LIB_INIT) {
        LIB = initTransformerBundle()
    }

    val context = Context.create()
    val bindings = context.getBindings("js")
    bindings.putMember("value", value)

    // convert the HashMap to a JavaScript object
    val jsValues = context.eval("js", "({})")
    values.forEach { (k, v) -> jsValues.putMember(k, v.lastMatch) }
    bindings.putMember("values", jsValues)
    bindings.putMember("Lib", LIB)

    try {
        res.out = context.eval("js", transformer).asString()
    } catch (e: Exception) {
        Log.error("Transformer exception!", e)
        res.err = e.message ?: "Unknown error"
    }

    context.close()

    return res
}
