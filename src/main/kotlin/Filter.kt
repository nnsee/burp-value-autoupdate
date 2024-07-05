package burp

import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.http.message.responses.HttpResponse
import kotlinx.serialization.Serializable

enum class RequestFilterType {
    METHOD, CONTENT_TYPE, PATH, HEADERS
}

enum class ResponseFilterType {
    STATUS_CODE, CONTENT_TYPE, HEADERS
}

enum class FilterKind {
    IS, CONTAINS, STARTS_WITH, ENDS_WITH
}

enum class FilterChainType {
    AND, OR
}

@Serializable
data class RequestFilter(val type: RequestFilterType, val value: String, val kind: FilterKind, val inverted: Boolean = false)
@Serializable
data class ResponseFilter(val type: ResponseFilterType, val value: String, val kind: FilterKind, val inverted: Boolean = false)

@Serializable
data class RequestFilterChain(val filters: List<RequestFilter>, val chainType: FilterChainType)
@Serializable
data class ResponseFilterChain(val filters: List<ResponseFilter>, val chainType: FilterChainType)

class FilterEvaluator {
    fun matches(request: HttpRequest, filters: RequestFilterChain): Boolean {
        val results = filters.filters.map { filter -> applyRequestFilter(request, filter) }
        return evaluateResults(results, filters.chainType)
    }

    fun matches(response: HttpResponse, filters: ResponseFilterChain): Boolean {
        val results = filters.filters.map { filter -> applyResponseFilter(response, filter) }
        return evaluateResults(results, filters.chainType)
    }

    private fun applyRequestFilter(request: HttpRequest, filter: RequestFilter): Boolean {
        val result = when (filter.type) {
            RequestFilterType.METHOD -> applyFilter(request.method(), filter)
            RequestFilterType.CONTENT_TYPE -> applyFilter(request.header("Content-Type").value() ?: "", filter)
            RequestFilterType.PATH -> applyFilter(request.path(), filter)
            RequestFilterType.HEADERS -> request.headers().any { applyFilter(it.toString(), filter) }
        }
        return if (filter.inverted) !result else result
    }

    private fun applyResponseFilter(response: HttpResponse, filter: ResponseFilter): Boolean {
        val result = when (filter.type) {
            ResponseFilterType.STATUS_CODE -> applyFilter(response.statusCode().toString(), filter)
            ResponseFilterType.CONTENT_TYPE -> applyFilter(response.header("Content-Type").value() ?: "", filter)
            ResponseFilterType.HEADERS -> response.headers().any { applyFilter(it.toString(), filter) }
        }
        return if (filter.inverted) !result else result
    }

    private fun applyFilter(value: String, filter: RequestFilter): Boolean {
        return when (filter.kind) {
            FilterKind.IS -> value == filter.value
            FilterKind.CONTAINS -> value.contains(filter.value)
            FilterKind.STARTS_WITH -> value.startsWith(filter.value)
            FilterKind.ENDS_WITH -> value.endsWith(filter.value)
        }
    }

    private fun applyFilter(value: String, filter: ResponseFilter): Boolean {
        return when (filter.kind) {
            FilterKind.IS -> value == filter.value
            FilterKind.CONTAINS -> value.contains(filter.value)
            FilterKind.STARTS_WITH -> value.startsWith(filter.value)
            FilterKind.ENDS_WITH -> value.endsWith(filter.value)
        }
    }

    private fun evaluateResults(results: List<Boolean>, chainType: FilterChainType): Boolean {
        return when (chainType) {
            FilterChainType.AND -> results.all { it }
            FilterChainType.OR -> results.any { it }
        }
    }
}
