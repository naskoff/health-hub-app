package net.softavis.healthhub.data

import android.content.Context
import androidx.core.content.edit

data class HealthHubSession(
    val api: String,
    val authorization: String,
)

class SessionStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun getSession(): HealthHubSession? {
        val api = preferences.getString(KEY_API, null)
            ?: return null

        val authorization = preferences.getString(KEY_AUTHORIZATION, null)
            ?: return null

        return HealthHubSession(
            api = api,
            authorization = authorization,
        )
    }

    fun saveSession(session: HealthHubSession) {
        preferences.edit {
            putString(KEY_API, session.api)
            putString(KEY_AUTHORIZATION, session.authorization)
        }
    }

    fun clearSession() {
        preferences.edit {
            clear()
        }
    }

    fun getLastSuccessfulSync(): Long? {
        val value = preferences.getLong(KEY_LAST_SUCCESSFUL_SYNC, -1L)

        return value.takeIf { it >= 0 }
    }

    fun saveLastSuccessfulSync(timestamp: Long) {
        preferences.edit {
            putLong(KEY_LAST_SUCCESSFUL_SYNC, timestamp)
        }
    }

    fun getChangesToken(): String? {
        return preferences.getString(KEY_CHANGES_TOKEN, null)
    }

    fun saveChangesToken(token: String) {
        preferences.edit {
            putString(KEY_CHANGES_TOKEN, token)
        }
    }

    fun clearChangesToken() {
        preferences.edit {
            remove(KEY_CHANGES_TOKEN)
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "health_hub_session"

        private const val KEY_API = "api"
        private const val KEY_AUTHORIZATION = "authorization"
        private const val KEY_LAST_SUCCESSFUL_SYNC = "last_successful_sync"
        private const val KEY_CHANGES_TOKEN = "changes_token"
    }
}