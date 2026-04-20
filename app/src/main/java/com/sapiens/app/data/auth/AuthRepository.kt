package com.sapiens.app.data.auth

import android.app.Application
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.sapiens.app.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?
)

class AuthRepository(
    private val app: Application
) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val webClientId: String
        get() = app.getString(R.string.google_oauth_web_client_id)

    private val googleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(app, gso)
    }

    fun googleSignInIntent(): Intent = googleSignInClient.signInIntent

    /**
     * [androidx.activity.result.ActivityResultContracts.StartActivityForResult] 결과 처리.
     */
    suspend fun signInWithGoogleActivityResult(data: Intent?): Result<AuthUser> {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
                ?: return Result.failure(IllegalStateException("Google ID 토큰이 없습니다."))
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            val user = firebaseAuth.currentUser?.toAuthUser()
                ?: return Result.failure(IllegalStateException("Firebase 사용자를 가져오지 못했습니다."))
            Result.success(user)
        } catch (e: ApiException) {
            Result.failure(Exception("Google 로그인 오류 (${e.statusCode}): ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            googleSignInClient.signOut().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun currentAuthUser(): AuthUser? = firebaseAuth.currentUser.toAuthUser()

    fun authStateFlow(): Flow<AuthUser?> = callbackFlow {
        trySend(firebaseAuth.currentUser.toAuthUser())
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
}

private fun FirebaseUser?.toAuthUser(): AuthUser? =
    this?.let {
        val uid = it.uid
        if (uid.isBlank()) null
        else AuthUser(uid = uid, displayName = it.displayName, email = it.email)
    }
