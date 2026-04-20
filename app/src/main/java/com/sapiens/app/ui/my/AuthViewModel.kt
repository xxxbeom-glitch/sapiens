package com.sapiens.app.ui.my

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.auth.AuthRepository
import com.sapiens.app.data.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

    val authUser: StateFlow<AuthUser?> = authRepository
        .authStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = authRepository.currentAuthUser()
        )

    fun googleSignInIntent(): Intent = authRepository.googleSignInIntent()

    fun onGoogleSignInActivityResult(data: Intent?) {
        viewModelScope.launch {
            _signInError.value = null
            authRepository.signInWithGoogleActivityResult(data).onFailure { e ->
                _signInError.value = e.message ?: "로그인에 실패했습니다."
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _signInError.value = null
            authRepository.signOut().onFailure { e ->
                _signInError.value = e.message ?: "로그아웃에 실패했습니다."
            }
        }
    }

    fun clearSignInError() {
        _signInError.value = null
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                        "Unknown ViewModel class $modelClass"
                    }
                    return AuthViewModel(AuthRepository(application)) as T
                }
            }
    }
}
