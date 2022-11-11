package burp

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Annotations
import burp.api.montoya.core.HighlightColor
import burp.api.montoya.core.ToolSource
import burp.api.montoya.http.HttpHandler
import burp.api.montoya.http.RequestResult
import burp.api.montoya.http.ResponseResult
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.http.message.responses.HttpResponse

const val EXTENSION_NAME = "Value Autoupdater"

class BurpExtender : BurpExtension {
    private lateinit var ui: UI
    private lateinit var items: ItemStore
    private lateinit var transformers: TransformerStore
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

        api.extension().setName(EXTENSION_NAME)
        items = ItemStore(api.persistence().preferences())
        transformers = TransformerStore(api.persistence().preferences())
        ui = UI(api, items, transformers)
        replacer = Replacer(api, items, transformers)

        api.http().registerHttpHandler(ExtHttpHandler())

        log.info("Initialized $EXTENSION_NAME")
    }

    inner class ExtHttpHandler : HttpHandler {
        override fun handleHttpRequest(
            request: HttpRequest, annotations: Annotations, toolSource: ToolSource
        ): RequestResult {
            if (!ui.isEnabled(toolSource.toolType())) return RequestResult.requestResult(
                request, Annotations.annotations()
            )
            val result = replacer.handleRequest(request.toString())
            return RequestResult.requestResult(
                HttpRequest.httpRequest(request.httpService(), result.contents), constructAnnotations(result)
            )
        }

        override fun handleHttpResponse(
            response: HttpResponse, request: HttpRequest, annotations: Annotations, toolSource: ToolSource
        ): ResponseResult {
            if (!ui.isEnabled(toolSource.toolType())) return ResponseResult.responseResult(
                response, Annotations.annotations()
            )
            val result = replacer.handleResponse(response.toString())
            return ResponseResult.responseResult(response, constructAnnotations(result))
        }

    }


    fun constructAnnotations(result: ReplaceResult): Annotations {
        if (!result.matched) return Annotations.annotations()

        // sometimes newlines are removed from comments, so accommodate for that as well
        var comment = when (result.type) {
            ReplaceType.REQUEST -> "Replaced placeholders: \n"
            ReplaceType.RESPONSE -> "Updated values: \n"
        }

        result.values.forEach {
            comment += "(${it.old}: ${it.new}) \n"
        }

        return Annotations.annotations(comment, HighlightColor.NONE)
    }
}