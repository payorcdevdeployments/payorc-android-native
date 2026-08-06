package com.payorc.sdk.data.mapper

import com.payorc.sdk.data.remote.dto.TabbyConfirmResponseDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

object TabbyMerchantResponseMapper {
    fun toMerchantResponse(dto: TabbyConfirmResponseDto): Map<String, Any?> {
        val dataValue = dto.data?.let { jsonElementToAny(it) }

        return mapOf(
            "data" to (dataValue ?: emptyMap<String, Any?>()),
            "message" to dto.message,
            "status" to dto.status,
            "code" to dto.code
        )
    }

    private fun jsonElementToAny(element: JsonElement): Any? {
        return when (element) {
            is JsonNull -> null
            is JsonPrimitive -> jsonPrimitiveToAny(element)
            is JsonObject -> element.mapValues { jsonElementToAny(it.value) }
            is JsonArray -> element.map { jsonElementToAny(it) }
            else -> null
        }
    }

    private fun jsonPrimitiveToAny(primitive: JsonPrimitive): Any? {
        return when {
            primitive.isString -> primitive.content
            primitive.booleanOrNull != null -> primitive.boolean
            primitive.longOrNull != null -> primitive.long
            primitive.doubleOrNull != null -> primitive.double
            else -> primitive.content
        }
    }
}
