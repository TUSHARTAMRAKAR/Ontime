package com.tushartamrakar.ontime.calendar.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes

object GoogleSignInHelper {

    private fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(CalendarScopes.CALENDAR),
                Scope(CalendarScopes.CALENDAR_EVENTS),
                Scope(CalendarScopes.CALENDAR_READONLY),  // ✅ needed for holiday calendars
            )
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(context: Context): Intent =
        getClient(context).signInIntent

    fun getSignedInAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun isSignedIn(context: Context): Boolean =
        getSignedInAccount(context) != null

    fun signOut(context: Context, onComplete: () -> Unit) {
        getClient(context).signOut().addOnCompleteListener { onComplete() }
    }

    fun revokeAccess(context: Context, onComplete: () -> Unit) {
        getClient(context).revokeAccess().addOnCompleteListener { onComplete() }
    }
}
