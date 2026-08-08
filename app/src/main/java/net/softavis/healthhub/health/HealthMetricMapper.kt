package net.softavis.healthhub.health

import android.util.Log
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import net.softavis.healthhub.api.HealthMetricPayload
import java.time.Instant

object HealthMetricMapper {

    fun map(record: Record): List<HealthMetricPayload> {
        return when (record) {
            is WeightRecord -> listOf(mapWeight(record))
            is StepsRecord -> listOf(mapSteps(record))
            is HeartRateRecord -> mapHeartRate(record)
            is SleepSessionRecord -> listOf(mapSleep(record))

            is ActiveCaloriesBurnedRecord ->
                listOf(mapActiveCalories(record))

            is TotalCaloriesBurnedRecord ->
                listOf(mapTotalCalories(record))

            is DistanceRecord -> listOf(mapDistance(record))
            is HydrationRecord -> listOf(mapHydration(record))

            is OxygenSaturationRecord ->
                listOf(mapOxygenSaturation(record))

            is BloodPressureRecord ->
                listOf(mapBloodPressure(record))

            is BodyFatRecord -> listOf(mapBodyFat(record))

            is RespiratoryRateRecord ->
                listOf(mapRespiratoryRate(record))

            is RestingHeartRateRecord ->
                listOf(mapRestingHeartRate(record))

            is ExerciseSessionRecord ->
                listOf(mapExerciseSession(record))

            is NutritionRecord -> mapNutrition(record)

            else -> emptyList()
        }
    }

    private fun mapWeight(
        record: WeightRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "weight",
            value = record.weight.inKilograms,
            unit = "kg",
            measuredAt = record.time,
        )
    }

    private fun mapSteps(
        record: StepsRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "steps",
            value = record.count.toDouble(),
            unit = "count",
            measuredAt = record.endTime,
            startTime = record.startTime,
            endTime = record.endTime,
        )
    }

    private fun mapHeartRate(
        record: HeartRateRecord,
    ): List<HealthMetricPayload> {
        return record.samples.mapIndexed { index, sample ->
            createPayload(
                record = record,
                type = "heart_rate",
                value = sample.beatsPerMinute.toDouble(),
                unit = "bpm",
                measuredAt = sample.time,
                startTime = record.startTime,
                endTime = record.endTime,
                discriminator = index.toString(),
            )
        }
    }

    private fun mapSleep(
        record: SleepSessionRecord,
    ): HealthMetricPayload {
        val durationMinutes = java.time.Duration
            .between(record.startTime, record.endTime)
            .toMinutes()
            .toDouble()

        return createPayload(
            record = record,
            type = "sleep",
            value = durationMinutes,
            unit = "min",
            measuredAt = record.endTime,
            startTime = record.startTime,
            endTime = record.endTime,
            extraData = mapOf(
                "title" to record.title,
                "notes" to record.notes,
                "stages" to record.stages.map { stage ->
                    mapOf(
                        "stage" to stage.stage,
                        "start_time" to stage.startTime.toString(),
                        "end_time" to stage.endTime.toString(),
                    )
                },
            ),
        )
    }

    private fun mapActiveCalories(
        record: ActiveCaloriesBurnedRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "active_calories",
            value = record.energy.inKilocalories,
            unit = "kcal",
            measuredAt = record.endTime,
            startTime = record.startTime,
            endTime = record.endTime,
        )
    }

    private fun mapTotalCalories(
        record: TotalCaloriesBurnedRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "total_calories",
            value = record.energy.inKilocalories,
            unit = "kcal",
            measuredAt = record.endTime,
            startTime = record.startTime,
            endTime = record.endTime,
        )
    }

    private fun mapDistance(
        record: DistanceRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "distance",
            value = record.distance.inMeters,
            unit = "m",
            measuredAt = record.endTime,
            startTime = record.startTime,
            endTime = record.endTime,
        )
    }

    private fun mapHydration(
        record: HydrationRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "hydration",
            value = record.volume.inLiters,
            unit = "l",
            measuredAt = record.endTime,
            startTime = record.startTime,
            endTime = record.endTime,
        )
    }

    private fun mapOxygenSaturation(
        record: OxygenSaturationRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "oxygen_saturation",
            value = record.percentage.value,
            unit = "%",
            measuredAt = record.time,
        )
    }

    private fun mapBloodPressure(
        record: BloodPressureRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "blood_pressure",
            value = record.systolic.inMillimetersOfMercury,
            unit = "mmHg",
            measuredAt = record.time,
            extraData = mapOf(
                "systolic" to
                        record.systolic.inMillimetersOfMercury,

                "diastolic" to
                        record.diastolic.inMillimetersOfMercury,

                "body_position" to record.bodyPosition,
                "measurement_location" to
                        record.measurementLocation,
            ),
        )
    }

    private fun mapBodyFat(
        record: BodyFatRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "body_fat",
            value = record.percentage.value,
            unit = "%",
            measuredAt = record.time,
        )
    }

    private fun mapRespiratoryRate(
        record: RespiratoryRateRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "respiratory_rate",
            value = record.rate,
            unit = "breaths/min",
            measuredAt = record.time,
        )
    }

    private fun mapRestingHeartRate(
        record: RestingHeartRateRecord,
    ): HealthMetricPayload {
        return createPayload(
            record = record,
            type = "resting_heart_rate",
            value = record.beatsPerMinute.toDouble(),
            unit = "bpm",
            measuredAt = record.time,
        )
    }

    private fun mapExerciseSession(
        record: ExerciseSessionRecord,
    ): HealthMetricPayload {
        val durationMinutes = java.time.Duration
            .between(record.startTime, record.endTime)
            .toMinutes()
            .toDouble()

        return createPayload(
            record = record,
            type = "exercise_session",
            value = durationMinutes,
            unit = "min",
            measuredAt = record.endTime,
            startTime = record.startTime,
            endTime = record.endTime,
            extraData = mapOf(
                "exercise_type" to record.exerciseType,
                "title" to record.title,
                "notes" to record.notes,
            ),
        )
    }

    private fun mapNutrition(
        record: NutritionRecord,
    ): List<HealthMetricPayload> {

        Log.i(
            "HealthHub",
            """
        NutritionRecord
        id=${record.metadata.id}
        lastModified=${record.metadata.lastModifiedTime}
        package=${record.metadata.dataOrigin.packageName}

        mealType=${record.mealType}
        mealName=${record.name}

        start=${record.startTime}
        end=${record.endTime}

        calories=${record.energy?.inKilocalories}
        protein=${record.protein?.inGrams}
        fat=${record.totalFat?.inGrams}
        carbs=${record.totalCarbohydrate?.inGrams}
        fiber=${record.dietaryFiber?.inGrams}
        sugar=${record.sugar?.inGrams}
        sodium=${record.sodium?.inMilligrams}
        """.trimIndent(),
        )

        val metrics = mutableListOf<HealthMetricPayload>()

        fun addMetric(
            type: String,
            value: Double?,
            unit: String,
        ) {
            if (value == null) {
                return
            }

            metrics += createPayload(
                record = record,
                type = type,
                value = value,
                unit = unit,
                measuredAt = record.endTime,
                startTime = record.startTime,
                endTime = record.endTime,
                discriminator = type,
                extraData = mapOf(
                    "meal_type" to record.mealType,
                    "meal_name" to record.name,
                ),
            )
        }

        addMetric(
            type = "calories",
            value = record.energy?.inKilocalories,
            unit = "kcal",
        )

        addMetric(
            type = "protein",
            value = record.protein?.inGrams,
            unit = "g",
        )

        addMetric(
            type = "fat",
            value = record.totalFat?.inGrams,
            unit = "g",
        )

        addMetric(
            type = "carbohydrates",
            value = record.totalCarbohydrate?.inGrams,
            unit = "g",
        )

        addMetric(
            type = "fiber",
            value = record.dietaryFiber?.inGrams,
            unit = "g",
        )

        addMetric(
            type = "sugar",
            value = record.sugar?.inGrams,
            unit = "g",
        )

        addMetric(
            type = "sodium",
            value = record.sodium?.inMilligrams,
            unit = "mg",
        )

        return metrics
    }

    private fun createPayload(
        record: Record,
        type: String,
        value: Double?,
        unit: String?,
        measuredAt: Instant?,
        startTime: Instant? = null,
        endTime: Instant? = null,
        discriminator: String? = null,
        extraData: Map<String, Any?> = emptyMap(),
    ): HealthMetricPayload {
        val metadata = record.metadata
        val source = metadata.dataOrigin.packageName

        val idempotencyKey = IdempotencyKeyFactory.create(
            source = source,
            recordType = type,
            sourceRecordId = metadata.id,
            startTime = startTime,
            endTime = endTime,
            measuredAt = measuredAt,
            discriminator = discriminator,
            value = value,
            unit = unit,
        )

        val data = mutableMapOf<String, Any?>(
            "source" to "health_connect",
            "provider" to source,
            "source_record_id" to metadata.id,
            "client_record_id" to metadata.clientRecordId,
            "last_modified_time" to
                    metadata.lastModifiedTime.toString(),
            "recording_method" to metadata.recordingMethod,
        )

        metadata.device?.let { device ->
            data["device"] = mapOf(
                "manufacturer" to device.manufacturer,
                "model" to device.model,
                "type" to device.type,
            )
        }

        startTime?.let {
            data["start_time"] = it.toString()
        }

        endTime?.let {
            data["end_time"] = it.toString()
        }

        data.putAll(extraData)

        return HealthMetricPayload(
            idempotencyKey = idempotencyKey,
            type = type,
            value = value,
            unit = unit,
            measuredAt = measuredAt?.toString(),
            data = data,
        )
    }
}