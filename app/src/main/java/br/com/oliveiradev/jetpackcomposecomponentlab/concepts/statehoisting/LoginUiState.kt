package br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting


data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val globalError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

val LoginUiState.isSubmitEnabled: Boolean
    get() = email.isNotBlank()
            && password.isNotBlank()
            && !isLoading
