package burp

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.handler.*
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.http.message.responses.HttpResponse

const val EXTENSION_NAME = "Value Autoupdater"

class BurpExtender : BurpExtension {
    private lateinit var ui: UI
    private lateinit var items: ItemStore
    private lateinit var transformers: TransformerStore
    private lateinit var replacer: Replacer
    private lateinit var api: MontoyaApi

    override fun initialize(api: MontoyaApi) {
        log.registerStreams(api.logging().output(), api.logging().error())
        log.setLevel(Logger.Level.DEBUG)

        this.api = api

        api.extension().setName(EXTENSION_NAME)
        items = ItemStore(api.persistence().preferences())
        transformers = TransformerStore(api.persistence().preferences())
        ui = UI(api, items, transformers)
        replacer = Replacer(api, items, transformers)

        api.http().registerHttpHandler(ExtHttpHandler())

        log.info("Initialized $EXTENSION_NAME")
    }

    inner class ExtHttpHandler : HttpHandler {
        override fun handleHttpRequestToBeSent(request: HttpRequestToBeSent): RequestToBeSentAction {
            if (!ui.isEnabled(request.toolSource().toolType()))
                return RequestToBeSentAction.continueWith(request)
            val result = replacer.handleRequest(request.toString())
            return RequestToBeSentAction.continueWith(HttpRequest.httpRequest(result.contents))
        }

        override fun handleHttpResponseReceived(response: HttpResponseReceived): ResponseReceivedAction {
            if (!ui.isEnabled(response.toolSource().toolType()))
                return ResponseReceivedAction.continueWith(response)
            val result = replacer.handleRequest(response.toString())
            return ResponseReceivedAction.continueWith(HttpResponse.httpResponse(result.contents))
        }

    }
}