package com.example.network

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

object FirebaseManager {
    val auth: FirebaseAuth?
        get() = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        
    val firestore: FirebaseFirestore?
        get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }

    val currentUser get() = auth?.currentUser

    suspend fun signInWithGoogle(context: Context): Boolean {
        val credentialManager = CredentialManager.create(context)
        
        val googleIdOption = GetSignInWithGoogleOption.Builder("YOUR_SERVER_CLIENT_ID") // This would normally come from BuildConfig or strings.xml
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = GoogleAuthProvider.getCredential(
                result.credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN"),
                null
            )
            auth?.signInWithCredential(credential)?.await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Sign in failed", e)
            false
        }
    }

    fun signOut() {
        auth?.signOut()
    }

    suspend fun saveUserCommand(command: String, response: String) {
        val user = currentUser ?: return
        val data = hashMapOf(
            "command" to command,
            "response" to response,
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        try {
            firestore?.collection("users")?.document(user.uid)
                ?.collection("history")?.add(data)?.await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to save command", e)
        }
    }
}
