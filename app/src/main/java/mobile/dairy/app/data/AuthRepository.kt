package mobile.dairy.app.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val paths: FirestorePaths,
    private val functions: FirebaseFunctions,
    private val database: AppDatabase,
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(name: String, email: String, password: String) {
        val cred = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        cred.user?.updateProfile(userProfileChangeRequest { displayName = name.trim() })?.await()
        ensureUserDoc(cred.user)
    }

    suspend fun signIn(email: String, password: String) {
        val cred = auth.signInWithEmailAndPassword(email.trim(), password).await()
        ensureUserDoc(cred.user)
    }

    suspend fun sendReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun signInWithGoogle(activityContext: Context, webClientId: String) {
        try {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val result =
                CredentialManager.create(activityContext).getCredential(activityContext, request)

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCred = GoogleAuthProvider.getCredential(idToken, null)
                val user = auth.signInWithCredential(authCred).await()
                ensureUserDoc(user.user)
            }
        } catch (e: NoCredentialException) {
            // Silently handle - user likely has no Google accounts signed in on device
            Log.i("AuthRepo", "No Google credentials found on device.")
        } catch (e: GetCredentialException) {
            // Log other credential errors (like cancellation or config issues) silently for a smooth UI
            Log.w("AuthRepo", "Google Sign-In failed or cancelled: ${e.type}", e)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Unexpected Google Sign-In error", e)
            throw e
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    private suspend fun ensureUserDoc(user: FirebaseUser?) {
        val u = user ?: return
        paths.userDoc(u.uid).set(
            mapOf(
                "displayName" to (u.displayName ?: "Friend"),
                "email" to u.email,
                "createdAt" to System.currentTimeMillis(),
                "timezone" to java.util.TimeZone.getDefault().id,
            ),
            com.google.firebase.firestore.SetOptions.merge(),
        ).await()
    }

    /* --------------- server-side callables (shared backend) --------- */

    suspend fun requestDataExport(): String {
        val result = functions.getHttpsCallable("exportUserData").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?> ?: error("Export failed")
        return data["url"] as? String ?: error("Export failed")
    }

    suspend fun deleteAccount() {
        functions.getHttpsCallable("deleteAccount").call().await()
        auth.signOut()
    }

    companion object {
        /** Friendly copy for common auth errors — direct, never blaming. */
        fun friendlyError(e: Throwable): String = when ((e as? FirebaseAuthException)?.errorCode) {
            "ERROR_INVALID_EMAIL" -> "That email address doesn't look right. Check it and try again."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Try signing in instead."
            "ERROR_WEAK_PASSWORD" -> "Passwords need at least 6 characters."
            "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                "Email or password doesn't match. Try again or reset your password."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a minute, then try again."
            "ERROR_NETWORK_REQUEST_FAILED" -> "No connection. Check your network and try again."
            else -> e.message ?: "Something went wrong while signing in. Try again."
        }
    }
}
