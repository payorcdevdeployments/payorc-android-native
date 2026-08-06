package com.payorc.sdk.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

object ErrorResponseExtractor {
    private val json = Json { ignoreUnknownKeys = true }

    fun extractErrorMessage(exception: HttpException): String {
        return try {
            val errorBody = exception.response()?.errorBody()?.string() ?: return exception.message()
            val jsonElement = json.parseToJsonElement(errorBody)
            val messageField = jsonElement.jsonObject["message"]?.jsonPrimitive?.content
            
            if (!messageField.isNullOrBlank()) {
                messageField
            } else {
                exception.message()
            }
        } catch (e: Exception) {
            exception.message()
        }
    }
}
