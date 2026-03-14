package br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * LoginScreen é o único composable que "conhece" o ViewModel.
 *
 * Responsabilidades:
 *  - Coletar o StateFlow do ViewModel
 *  - Repassar o estado e as lambdas para LoginForm
 *  - Reagir a eventos de navegação (isSuccess)
 *
 * LoginScreen NÃO tem layout próprio — toda a UI fica em LoginForm.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Reage ao evento de sucesso para navegar.
    // LaunchedEffect garante que a navegação ocorre fora da fase de composição.
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess()
    }

    // State hoisting em ação:
    //  - uiState desce como parâmetro (dado imutável)
    //  - lambdas sobem os eventos de volta para o ViewModel
    LoginForm(
        uiState          = uiState,
        onEmailChange    = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit         = viewModel::onSubmit
    )
}