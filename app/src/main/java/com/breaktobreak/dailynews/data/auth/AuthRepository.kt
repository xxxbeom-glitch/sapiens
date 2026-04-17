package com.breaktobreak.dailynews.data.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUser(
    val displayName: String?,
    val email: String?
)

class AuthRepository {
    private val mockUserState = MutableStateFlow<AuthUser?>(null)

    fun observeAuthState(): Flow<AuthUser?> = mockUserState.asStateFlow()

    fun getCurrentUser(): AuthUser? = mockUserState.value

    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser?> {
        return Result.success(null)
    }

    fun signOut() {
        mockUserState.value = null
    }
}
