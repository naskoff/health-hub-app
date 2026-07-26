package net.softavis.healthhub.pairing

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

data class PairingQrPayload(
    val api: String,
    val token: String,
) {
    companion object {
        private val gson = Gson()

        fun parse(value: String): PairingQrPayload? {
            return try {
                gson.fromJson(value, PairingQrPayload::class.java)
                    ?.takeIf {
                        it.api.isNotBlank() &&
                                it.token.isNotBlank()
                    }
            } catch (_: JsonSyntaxException) {
                null
            }
        }
    }
}