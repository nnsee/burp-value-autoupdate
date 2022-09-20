package burp

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.MessageAnnotations
import burp.api.montoya.core.ToolSource
import burp.api.montoya.http.HttpHandler
import burp.api.montoya.http.RequestHandlerResult
import burp.api.montoya.http.ResponseHandlerResult
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.http.message.responses.HttpResponse

const val EXTENSION_NAME = "Value Autoupdater"

class BurpExtender : BurpExtension {
    private lateinit var ui: UI
    private lateinit var items: ItemStore
    private lateinit var replacer: Replacer
    private lateinit var api: MontoyaApi

    // needed for grammar mismatch fix
    fun initialise(api: MontoyaApi) {
        initialize(api)
    }

    override fun initialize(api: MontoyaApi) {
        log.registerStreams(api.logging().output(), api.logging().error())
        log.setLevel(Logger.Level.DEBUG)

        this.api = api

        api.misc().setExtensionName(EXTENSION_NAME)
        items = ItemStore(api.persistence().userContext())
        ui = UI(api, items)
        replacer = Replacer(api, items)

        api.http().registerHttpHandler(ExtHttpHandler())

        log.info("Initialized $EXTENSION_NAME")
    }

    inner class ExtHttpHandler : HttpHandler {
        override fun handleHttpRequest(
            request: HttpRequest, annotations: MessageAnnotations, toolSource: ToolSource
        ): RequestHandlerResult {
            if (!ui.isEnabled(toolSource.toolType())) return RequestHandlerResult.from(request, MessageAnnotations.NONE)
            return RequestHandlerResult.from(
                api.http().createRequest(request.httpService(), replacer.handleRequest(request.toString())),
                MessageAnnotations.NONE
            )
        }

        override fun handleHttpResponse(
            request: HttpRequest, response: HttpResponse, annotations: MessageAnnotations, toolSource: ToolSource
        ): ResponseHandlerResult {
            if (ui.isEnabled(toolSource.toolType())) replacer.handleResponse(response.toString())
            return ResponseHandlerResult.from(response, MessageAnnotations.NONE)
        }

    }

}