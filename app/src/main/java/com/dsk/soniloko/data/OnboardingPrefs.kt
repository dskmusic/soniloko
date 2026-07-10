package com.dsk.soniloko.data

import android.content.Context

/**
 * Tracks whether the first-run permission prompts have already been shown. Deliberately plain
 * SharedPreferences (not DataStore, not part of AppSettings) so factory reset doesn't re-trigger
 * the onboarding flow.
 */
object OnboardingPrefs {
    private const val PREFS_NAME = "soniloko_onboarding"
    private const val KEY_PERMISSIONS_REQUESTED = "permissions_requested"

    fun hasRequestedPermissions(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_PERMISSIONS_REQUESTED, false)

    fun markPermissionsRequested(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_PERMISSIONS_REQUESTED, true).apply()
    }
}
