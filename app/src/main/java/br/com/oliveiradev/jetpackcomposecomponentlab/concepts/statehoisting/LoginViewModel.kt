package br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Estado privado e mutável — somente o ViewModel pode alterar
    private val _uiState = MutableStateFlow(LoginUiState())

    // Estado público e imutável — a UI apenas lê
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Eventos vindos da UI (state hoisting: eventos sobem, estado desce)
    // -------------------------------------------------------------------------

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = null,   // limpa o erro ao digitar
                globalError = null
            )
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null, // limpa o erro ao digitar
                globalError = null
            )
        }
    }

    fun onSubmit() {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, globalError = null) }

            val result = authRepository.login(
                email    = _uiState.value.email,
                password = _uiState.value.password
            )

            _uiState.update {
                when (result) {
                    is AuthResult.Success -> it.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                    is AuthResult.Error -> it.copy(
                        isLoading = false,
                        globalError = result.message
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Validação local (antes de chamar a rede)
    // -------------------------------------------------------------------------

    private fun validate(): Boolean {
        var isValid = true

        _uiState.update { state ->
            state.copy(
                emailError = when {
                    state.email.isBlank() -> {
                        isValid = false; "Preencha o e-mail"
                    }
                    !state.email.contains("@") -> {
                        isValid = false; "E-mail inválido"
                    }
                    else -> null
                },
                passwordError = when {
                    state.password.isBlank() -> {
                        isValid = false; "Preencha a senha"
                    }
                    state.password.length < 6 -> {
                        isValid = false; "Mínimo 6 caracteres"
                    }
                    else -> null
                }
            )
        }

        return isValid
    }
}