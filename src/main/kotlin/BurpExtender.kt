package burp

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.handler.*
import burp.api.montoya.http.message.requests.HttpRequest
import java.util.*

const val EXTENSION_NAME = "Value Autoupdater"

@Suppress("unused")
class BurpExtender : BurpExtension {
    private lateinit var ui: UI
    private lateinit var items: ItemStore
    private lateinit var transformers: TransformerStore
    private lateinit var replacer: Replacer
    private lateinit var api: MontoyaApi

    override fun initialize(api: MontoyaApi) {
        log.registerStreams(api.logging().output(), api.logging().error())
        log.setLevel(Logger.Level.DEBUG)

        System.setOut(api.logging().output())
        System.setErr(api.logging().error())

        val properties = Properties().apply {
            object {}.javaClass.classLoader.getResourceAsStream("version.properties").use { load(it) }
        }

        this.api = api

        api.extension().setName(EXTENSION_NAME)
        items = ItemStore(api.persistence().preferences())
        transformers = TransformerStore(api.persistence().preferences())
        ui = UI(api, items, transformers)
        replacer = Replacer(api, items, transformers)

        api.http().registerHttpHandler(ExtHttpHandler())

        log.info("Initialized $EXTENSION_NAME ${properties.getProperty("version")} (${properties.getProperty("commitHash").take(8)})")
    }

    inner class ExtHttpHandler : HttpHandler {
        override fun handleHttpRequestToBeSent(request: HttpRequestToBeSent): RequestToBeSentAction {
            if (!ui.isEnabled(request.toolSource().toolType()))
                return RequestToBeSentAction.continueWith(request)
            val result = replacer.handleRequest(request.toString())
            return RequestToBeSentAction.continueWith(HttpRequest.httpRequest(request.httpService(), result.contents))
        }

        override fun handleHttpResponseReceived(response: HttpResponseReceived): ResponseReceivedAction {
            if (ui.isEnabled(response.toolSource().toolType()))
                replacer.handleResponse(response.toString())
            return ResponseReceivedAction.continueWith(response)
        }

    }
}
