package br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * LoginForm é 100% stateless.
 *
 * Não sabe que existe um ViewModel, não usa remember,
 *  - Exibe o estado recebido via [uiState]
 *  - Delega interações via lambdas ([onEmailChange], [onPasswordChange], [onSubmit])
 *
 * Isso significa que pode ser testado e visualizado no Preview
 * sem nenhuma dependência de infraestrutura.
 */
@Composable
fun LoginForm(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = "Entrar",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 24.dp)
        )

        // Campo de e-mail
        OutlinedTextField(
            value         = uiState.email,
            onValueChange = onEmailChange,
            label         = { Text("E-mail") },
            singleLine    = true,
            isError       = uiState.emailError != null,
            supportingText = uiState.emailError?.let { error ->
                { Text(text = error, color = MaterialTheme.colorScheme.error) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo de senha
        OutlinedTextField(
            value                = uiState.password,
            onValueChange        = onPasswordChange,
            label                = { Text("Senha") },
            singleLine           = true,
            visualTransformation = PasswordVisualTransformation(),
            isError              = uiState.passwordError != null,
            supportingText       = uiState.passwordError?.let { error ->
                { Text(text = error, color = MaterialTheme.colorScheme.error) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Erro global (ex: credenciais inválidas retornadas pela API)
        if (uiState.globalError != null) {
            Text(
                text     = uiState.globalError,
                color    = MaterialTheme.colorScheme.error,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )
        }

        // Botão de submit
        Button(
            onClick  = onSubmit,
            enabled  = uiState.isSubmitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(20.dp),
                    color     = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Entrar")
            }
        }
    }
}

// -------------------------------------------------------------------------
// Previews — funcionam sem ViewModel porque LoginForm é stateless
// -------------------------------------------------------------------------

@Preview(showBackground = true, name = "Estado inicial")
@Composable
private fun LoginFormPreview() {
    LoginForm(
        uiState          = LoginUiState(),
        onEmailChange    = {},
        onPasswordChange = {},
        onSubmit         = {}
    )
}

@Preview(showBackground = true, name = "Com erros de validação")
@Composable
private fun LoginFormErrorPreview() {
    LoginForm(
        uiState = LoginUiState(
            email         = "email-invalido",
            password      = "123",
            emailError    = "E-mail inválido",
            passwordError = "Mínimo 6 caracteres"
        ),
        onEmailChange    = {},
        onPasswordChange = {},
        onSubmit         = {}
    )
}

@Preview(showBackground = true, name = "Carregando")
@Composable
private fun LoginFormLoadingPreview() {
    LoginForm(
        uiState = LoginUiState(
            email     = "usuario@email.com",
            password  = "senha123",
            isLoading = true
        ),
        onEmailChange    = {},
        onPasswordChange = {},
        onSubmit         = {}
    )
}