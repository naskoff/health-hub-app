package net.softavis.healthhub.health

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale

object IdempotencyKeyFactory {

    fun create(
        source: String,
        recordType: String,
        sourceRecordId: String?,
        startTime: Instant?,
        endTime: Instant?,
        measuredAt: Instant?,
        discriminator: String?,
        value: Double?,
        unit: String?,
    ): String {
        val canonicalValue = value
            ?.let {
                String.format(
                    Locale.ROOT,
                    "%.8f",
                    it,
                ).trimEnd('0').trimEnd('.')
            }
            ?: ""

        val canonical = listOf(
            "health-connect",
            source.trim().lowercase(Locale.ROOT),
            recordType.trim().lowercase(Locale.ROOT),

            /*
             * Keep the source ID as useful entropy, but do not make it the
             * only identity component.
             */
//            sourceRecordId.orEmpty(),

            startTime?.toString().orEmpty(),
            endTime?.toString().orEmpty(),
            measuredAt?.toString().orEmpty(),

            discriminator.orEmpty(),
            canonicalValue,
            unit.orEmpty().lowercase(Locale.ROOT),
        ).joinToString("|")

        return sha256(canonical)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }
}