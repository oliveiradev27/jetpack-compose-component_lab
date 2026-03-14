package br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * ESTRATÉGIA DE TESTE — LoginScreen
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * LoginScreen tem três responsabilidades testáveis:
 *
 *   1. PONTE DE ESTADO — coleta o uiState do ViewModel e repassa para LoginForm.
 *      Verificamos que a UI reflete corretamente o estado emitido pelo StateFlow.
 *
 *   2. PONTE DE EVENTOS — repassa as lambdas do ViewModel para LoginForm.
 *      Verificamos que interações na UI chamam os métodos corretos do ViewModel.
 *
 *   3. NAVEGAÇÃO — reage ao isSuccess = true chamando onLoginSuccess().
 *      Verificamos que o LaunchedEffect dispara a navegação no momento certo
 *      e não dispara quando isSuccess = false.
 *
 * Por que MutableStateFlow em vez de mockk<StateFlow>?
 *   Mockar StateFlow diretamente exige configurar value, collect, e coroutines
 *   internamente — frágil e verboso. MutableStateFlow é a implementação real,
 *   permite update() entre asserts, e se comporta exatamente como em produção.
 *
 * Por que NÃO precisamos de Turbine aqui?
 *   Turbine testa a emissão de valores de um Flow. Aqui não testamos o Flow
 *   em si — testamos o efeito que o estado tem na UI e na navegação.
 *   O Compose Test já garante que o estado foi coletado e renderizado.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─────────────────────────────────────────────────────────────────────────
    // SETUP — ViewModel mockado com StateFlow controlável
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cria um mock do ViewModel com um StateFlow real que podemos controlar.
     *
     * O ViewModel é mockado (relaxed = true) para que seus métodos
     * (onEmailChange, onPasswordChange, onSubmit) não precisem de implementação —
     * apenas verificamos se foram chamados.
     *
     * O uiState é um MutableStateFlow real, não mockado, porque o Compose
     * precisa de um Flow funcional para collectAsStateWithLifecycle().
     */
    private fun createViewModel(
        initialState: LoginUiState = LoginUiState()
    ): Pair<LoginViewModel, MutableStateFlow<LoginUiState>> {
        val stateFlow = MutableStateFlow(initialState)
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        every { viewModel.uiState } returns stateFlow
        return viewModel to stateFlow
    }

    private fun setContent(
        viewModel: LoginViewModel,
        onLoginSuccess: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginSuccess = onLoginSuccess,
                    viewModel      = viewModel
                )
            }
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 1. PONTE DE ESTADO — UI reflete o uiState do ViewModel
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `estado inicial e renderizado corretamente`() {
        val (viewModel, _) = createViewModel(LoginUiState())
        setContent(viewModel)

        composeTestRule.onNodeWithText("E-mail").assertExists()
        composeTestRule.onNodeWithText("Senha").assertExists()
        composeTestRule.onNode(hasText("Entrar") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun `email do uiState e exibido no campo de email`() {
        val (viewModel, _) = createViewModel(LoginUiState(email = "usuario@email.com"))
        setContent(viewModel)

        composeTestRule.onNodeWithText("usuario@email.com").assertExists()
    }

    @Test
    fun `erro de email do uiState e exibido na tela`() {
        val (viewModel, _) = createViewModel(LoginUiState(emailError = "E-mail inválido"))
        setContent(viewModel)

        composeTestRule.onNodeWithText("E-mail inválido").assertExists()
    }

    @Test
    fun `erro de senha do uiState e exibido na tela`() {
        val (viewModel, _) = createViewModel(LoginUiState(passwordError = "Mínimo 6 caracteres"))
        setContent(viewModel)

        composeTestRule.onNodeWithText("Mínimo 6 caracteres").assertExists()
    }

    @Test
    fun `erro global do uiState e exibido na tela`() {
        val (viewModel, _) = createViewModel(LoginUiState(globalError = "Credenciais inválidas"))
        setContent(viewModel)

        composeTestRule.onNodeWithText("Credenciais inválidas").assertExists()
    }

    @Test
    fun `botao fica habilitado quando email e senha estao preenchidos`() {
        val (viewModel, _) = createViewModel(
            LoginUiState(email = "usuario@email.com", password = "senha123")
        )
        setContent(viewModel)

        composeTestRule.onNode(hasText("Entrar") and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun `ui e atualizada quando o stateflow emite novo estado`() {
        val (viewModel, stateFlow) = createViewModel(LoginUiState())
        setContent(viewModel)

        // Estado inicial — sem erro
        composeTestRule.onNodeWithText("E-mail inválido").assertDoesNotExist()

        // ViewModel emite novo estado com erro
        stateFlow.update { it.copy(emailError = "E-mail inválido") }
        composeTestRule.waitForIdle()

        // UI deve refletir o novo estado
        composeTestRule.onNodeWithText("E-mail inválido").assertExists()
    }

    @Test
    fun `botao fica habilitado apos stateflow emitir email e senha preenchidos`() {
        val (viewModel, stateFlow) = createViewModel(LoginUiState())
        setContent(viewModel)

        composeTestRule.onNode(hasText("Entrar") and hasClickAction()).assertIsNotEnabled()

        stateFlow.update { it.copy(email = "usuario@email.com", password = "senha123") }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasText("Entrar") and hasClickAction()).assertIsEnabled()
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 2. PONTE DE EVENTOS — interações chamam os métodos do ViewModel
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `digitar no campo de email chama viewModel onEmailChange`() {
        val (viewModel, _) = createViewModel()
        setContent(viewModel)

        composeTestRule.onNode(hasSetTextAction() and hasText("E-mail", substring = true))
            .performTextInput("a")

        verify(exactly = 1) { viewModel.onEmailChange(any()) }
    }

    @Test
    fun `digitar no campo de senha chama viewModel onPasswordChange`() {
        val (viewModel, _) = createViewModel()
        setContent(viewModel)

        composeTestRule.onNode(hasSetTextAction() and hasText("Senha", substring = true))
            .performTextInput("a")

        verify(exactly = 1) { viewModel.onPasswordChange(any()) }
    }

    @Test
    fun `clicar no botao chama viewModel onSubmit`() {
        val (viewModel, _) = createViewModel(
            LoginUiState(email = "usuario@email.com", password = "senha123")
        )
        setContent(viewModel)

        composeTestRule.onNode(hasText("Entrar") and hasClickAction()).performClick()

        verify(exactly = 1) { viewModel.onSubmit() }
    }

    @Test
    fun `clicar no botao desabilitado nao chama viewModel onSubmit`() {
        val (viewModel, _) = createViewModel(LoginUiState())
        setContent(viewModel)

        composeTestRule.onNode(hasText("Entrar") and hasClickAction()).performClick()

        verify(exactly = 0) { viewModel.onSubmit() }
    }

    @Test
    fun `digitar no campo de senha nao chama viewModel onEmailChange`() {
        val (viewModel, _) = createViewModel()
        setContent(viewModel)

        composeTestRule.onNode(hasSetTextAction() and hasText("Senha", substring = true))
            .performTextInput("a")

        verify(exactly = 0) { viewModel.onEmailChange(any()) }
    }

    @Test
    fun `digitar no campo de email nao chama viewModel onPasswordChange`() {
        val (viewModel, _) = createViewModel()
        setContent(viewModel)

        composeTestRule.onNode(hasSetTextAction() and hasText("E-mail", substring = true))
            .performTextInput("a")

        verify(exactly = 0) { viewModel.onPasswordChange(any()) }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 3. NAVEGAÇÃO — LaunchedEffect reage ao isSuccess
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `onLoginSuccess nao e chamado quando isSuccess e false`() {
        val onLoginSuccess = mockk<() -> Unit>(relaxed = true)
        val (viewModel, _) = createViewModel(LoginUiState(isSuccess = false))
        setContent(viewModel, onLoginSuccess)

        composeTestRule.waitForIdle()

        verify(exactly = 0) { onLoginSuccess() }
    }

    @Test
    fun `onLoginSuccess e chamado quando isSuccess e true no estado inicial`() {
        val onLoginSuccess = mockk<() -> Unit>(relaxed = true)
        val (viewModel, _) = createViewModel(LoginUiState(isSuccess = true))
        setContent(viewModel, onLoginSuccess)

        composeTestRule.waitForIdle()

        verify(exactly = 1) { onLoginSuccess() }
    }

    @Test
    fun `onLoginSuccess e chamado quando stateflow emite isSuccess true`() {
        val onLoginSuccess = mockk<() -> Unit>(relaxed = true)
        val (viewModel, stateFlow) = createViewModel(LoginUiState(isSuccess = false))
        setContent(viewModel, onLoginSuccess)

        // Antes da transição — não deve ter sido chamado
        verify(exactly = 0) { onLoginSuccess() }

        // ViewModel emite sucesso (ex: após retorno da API)
        stateFlow.update { it.copy(isSuccess = true) }
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onLoginSuccess() }
    }

    @Test
    fun `onLoginSuccess e chamado apenas uma vez mesmo com recomposicoes`() {
        val onLoginSuccess = mockk<() -> Unit>(relaxed = true)
        val (viewModel, stateFlow) = createViewModel(LoginUiState(isSuccess = false))
        setContent(viewModel, onLoginSuccess)

        // Emite sucesso
        stateFlow.update { it.copy(isSuccess = true) }
        composeTestRule.waitForIdle()

        // Emite outro estado (sem mudar isSuccess) — não deve chamar novamente
        // LaunchedEffect(isSuccess) só roda quando a KEY muda, não em toda recomposição
        stateFlow.update { it.copy(email = "qualquer@email.com") }
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onLoginSuccess() }
    }
}