package net.softavis.healthhub.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.softavis.healthhub.api.HealthHubApiFactory
import net.softavis.healthhub.api.PairDeviceRequest
import net.softavis.healthhub.data.HealthHubSession
import net.softavis.healthhub.data.SessionStore
import net.softavis.healthhub.health.HealthConnectManager
import net.softavis.healthhub.pairing.PairingQrPayload
import net.softavis.healthhub.sync.HealthSyncScheduler
import net.softavis.healthhub.sync.HealthSyncWorker
import java.time.Instant

data class HealthHubUiState(
    val session: HealthHubSession? = null,
    val healthConnectAvailable: Boolean = false,
    val permissionsGranted: Boolean = false,
    val scanning: Boolean = false,
    val pairing: Boolean = false,
    val syncing: Boolean = false,
    val lastSync: Instant? = null,
    val message: String? = null,
    val error: String? = null,
)

class HealthHubViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val sessionStore =
        SessionStore(application)

    val healthConnectManager =
        HealthConnectManager(application)

    private val syncScheduler =
        HealthSyncScheduler(application)

    private val workManager =
        WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(
        HealthHubUiState(
            session = sessionStore.getSession(),
            healthConnectAvailable =
                healthConnectManager.isAvailable,
            lastSync = sessionStore
                .getLastSuccessfulSync()
                ?.let(Instant::ofEpochMilli),
        ),
    )

    val uiState: StateFlow<HealthHubUiState> =
        _uiState.asStateFlow()

    init {
        refreshPermissions()
        observeImmediateSync()

        if (sessionStore.getSession() != null) {
            syncScheduler.schedulePeriodicSync()
        }
    }

    fun startScanner() {
        _uiState.value = _uiState.value.copy(
            scanning = true,
            message = null,
            error = null,
        )
    }

    fun stopScanner() {
        _uiState.value = _uiState.value.copy(
            scanning = false,
        )
    }

    fun handleQrCode(rawValue: String) {
        val payload = PairingQrPayload.parse(rawValue)

        if (payload == null) {
            _uiState.value = _uiState.value.copy(
                scanning = false,
                error = "The QR code is not a valid Health Hub pairing code.",
            )

            return
        }

        pair(payload)
    }

    private fun pair(payload: PairingQrPayload) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                scanning = false,
                pairing = true,
                message = null,
                error = null,
            )

            runCatching {
                val api =
                    HealthHubApiFactory.create(payload.api)

                val response = api.pairDevice(
                    PairDeviceRequest(
                        token = payload.token,
                        name = android.os.Build.MODEL
                            .takeIf { it.isNotBlank() }
                            ?: "Android Connector",
                    ),
                )

                val session = HealthHubSession(
                    api = payload.api,
                    authorization = listOf(
                        response.data.tokenType,
                        response.data.accessToken,
                    ).joinToString(" "),
                )

                sessionStore.saveSession(session)

                syncScheduler.schedulePeriodicSync()
                syncScheduler.syncNow()

                session
            }.onSuccess { session ->
                _uiState.value = _uiState.value.copy(
                    session = session,
                    pairing = false,
                    message = "Device paired successfully. Synchronization started.",
                    error = null,
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    pairing = false,
                    error = exception.message
                        ?: "Pairing failed.",
                )
            }
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            val granted = runCatching {
                healthConnectManager.hasAllPermissions()
            }.getOrDefault(false)

            _uiState.value = _uiState.value.copy(
                healthConnectAvailable =
                    healthConnectManager.isAvailable,
                permissionsGranted = granted,
            )

            if (granted && sessionStore.getSession() != null) {
                syncScheduler.schedulePeriodicSync()
            }
        }
    }

    fun sync() {
        _uiState.value = _uiState.value.copy(
            syncing = true,
            message = "Synchronization started.",
            error = null,
        )

        syncScheduler.syncNow()
    }

    fun disconnect() {
        syncScheduler.cancelAll()
        sessionStore.clearSession()

        _uiState.value = HealthHubUiState(
            healthConnectAvailable =
                healthConnectManager.isAvailable,
        )

        refreshPermissions()
    }

    private fun observeImmediateSync() {
        viewModelScope.launch {
            workManager
                .getWorkInfosForUniqueWorkFlow(
                    HealthSyncScheduler.UNIQUE_IMMEDIATE_SYNC,
                )
                .collect { workInfos ->
                    val workInfo =
                        findRelevantWorkInfo(workInfos)
                            ?: return@collect

                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.BLOCKED,
                        WorkInfo.State.RUNNING,
                            -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    syncing = true,
                                    error = null,
                                )
                        }

                        WorkInfo.State.SUCCEEDED -> {
                            handleSuccessfulSync(workInfo)
                        }

                        WorkInfo.State.FAILED -> {
                            val error =
                                workInfo.outputData.getString(
                                    HealthSyncWorker.KEY_ERROR,
                                )

                            _uiState.value =
                                _uiState.value.copy(
                                    syncing = false,
                                    error = error
                                        ?: "Synchronization failed.",
                                )
                        }

                        WorkInfo.State.CANCELLED -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    syncing = false,
                                )
                        }
                    }
                }
        }
    }

    private fun findRelevantWorkInfo(
        workInfos: List<WorkInfo>,
    ): WorkInfo? {
        return workInfos.firstOrNull { workInfo ->
            !workInfo.state.isFinished
        } ?: workInfos.lastOrNull()
    }

    private fun handleSuccessfulSync(
        workInfo: WorkInfo,
    ) {
        val metricsSent =
            workInfo.outputData.getInt(
                HealthSyncWorker.KEY_METRICS_SENT,
                0,
            )

        val lastSync = sessionStore
            .getLastSuccessfulSync()
            ?.let(Instant::ofEpochMilli)

        _uiState.value = _uiState.value.copy(
            syncing = false,
            lastSync = lastSync,
            message = "$metricsSent metrics sent successfully.",
            error = null,
        )
    }
}
