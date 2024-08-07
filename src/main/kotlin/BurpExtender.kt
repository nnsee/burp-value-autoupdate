package burp

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.handler.*
import burp.api.montoya.http.message.requests.HttpRequest
import burp.ui.MainActivity
import java.util.*

const val EXTENSION_NAME = "Value Autoupdater"

@Suppress("unused")
class BurpExtender : BurpExtension {
    private lateinit var mainActivity: MainActivity
    private lateinit var items: ItemStore
    private lateinit var transformers: TransformerStore
    private lateinit var replacer: Replacer
    private lateinit var api: MontoyaApi

    override fun initialize(api: MontoyaApi) {
        Log.init(api)
        Log.setLevel(Log.Level.DEBUG)
        System.setOut(Log.output())
        System.setErr(Log.error())

        val properties = Properties().apply {
            object {}.javaClass.classLoader.getResourceAsStream("version.properties").use { load(it) }
        }

        this.api = api

        api.extension().setName(EXTENSION_NAME)
        items = ItemStore(api.persistence().preferences())
        transformers = TransformerStore(api.persistence().preferences())
        mainActivity = MainActivity(api, items, transformers)
        replacer = Replacer(
            items,
            transformers,
            { name, count -> mainActivity.replaced(name, count) },
            { name, value, index -> mainActivity.updated(name, value, index) })

        api.http().registerHttpHandler(ExtHttpHandler())

        Log.info(
            "Initialized $EXTENSION_NAME ${properties.getProperty("version")} (${
                properties.getProperty("commitHash").take(8)
            })"
        )
    }

    inner class ExtHttpHandler : HttpHandler {
        override fun handleHttpRequestToBeSent(request: HttpRequestToBeSent): RequestToBeSentAction {
            if (!mainActivity.isEnabled(request.toolSource().toolType()))
                return RequestToBeSentAction.continueWith(request)
            val result = replacer.handleRequest(request.toString())
            return RequestToBeSentAction.continueWith(HttpRequest.httpRequest(request.httpService(), result.contents))
        }

        override fun handleHttpResponseReceived(response: HttpResponseReceived): ResponseReceivedAction {
            if (mainActivity.isEnabled(response.toolSource().toolType()))
                replacer.handleResponse(response)
            return ResponseReceivedAction.continueWith(response)
        }

    }
}
