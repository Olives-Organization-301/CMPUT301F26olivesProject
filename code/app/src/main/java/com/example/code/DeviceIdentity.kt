package com.example.code

import android.content.Context
import java.util.UUID

/**
 * Provides a stable, app-scoped identity for this installation.
 *
 * The generated identifier is stored locally and is reused on later launches,
 * so entrants do not need to create an account or enter credentials.
 */
object DeviceIdentity {
    private const val PREFERENCES_NAME = "device_identity"
    private const val USER_ID_KEY = "entrant_id"

    fun getOrCreate(context: Context): String {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        return preferences.getString(USER_ID_KEY, null) ?: UUID.randomUUID().toString()
            .also { generatedId ->
                preferences.edit().putString(USER_ID_KEY, generatedId).apply()
            }
    }
    /**
     * wipes the saved ID from the device so when app
     * is restarted, it won't auto-login from the previous account
     * **/
    fun clearEntrantId(context: Context) {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit().remove(USER_ID_KEY).apply()
    }
}
