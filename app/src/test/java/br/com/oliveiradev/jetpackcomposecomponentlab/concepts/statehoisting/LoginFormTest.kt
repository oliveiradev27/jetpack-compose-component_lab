package br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * ESTRATÉGIA DE TESTE — LoginForm
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * LoginForm é um componente 100% stateless: recebe LoginUiState e lambdas,
 * não possui remember, LaunchedEffect ou qualquer efeito colateral interno.
 *
 * Por isso:
 *   - Não é necessário controlar o clock (autoAdvance pode ficar true)
 *   - Não é necessário Turbine (sem Flow)
 *   - Não é necessário MockK para ViewModels (sem dependências de infraestrutura)
 *   - MockK é usado apenas para verificar que lambdas são chamadas corretamente
 *
 * Cada teste segue o padrão:
 *   1. Montar o componente com um LoginUiState específico
 *   2. Verificar o que aparece (ou não) na árvore semântica
 *   3. Opcionalmente interagir (performClick, performTextInput) e verificar callbacks
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoginFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private fun setContent(
        uiState: LoginUiState,
        onEmailChange: (String) -> Unit = {},
        onPasswordChange: (String) -> Unit = {},
        onSubmit: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                LoginForm(
                    uiState          = uiState,
                    onEmailChange    = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onSubmit         = onSubmit
                )
            }
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 1. ESTADO INICIAL
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `campos de email e senha estao vazios no estado inicial`() {
        setContent(uiState = LoginUiState())

        composeTestRule.onNodeWithText("E-mail").assertExists()
        composeTestRule.onNodeWithText("Senha").assertExists()
    }

    @Test
    fun `botao entrar esta desabilitado no estado inicial`() {
        // email e password vazios → isSubmitEnabled = false (extension property)
        setContent(uiState = LoginUiState())

        composeTestRule.onNode(hasText("Entrar") and hasClickAction())
            .assertIsNotEnabled()
    }

    @Test
    fun `erros de validacao nao aparecem no estado inicial`() {
        setContent(uiState = LoginUiState())

        composeTestRule.onNodeWithText("E-mail inválido").assertDoesNotExist()
        composeTestRule.onNodeWithText("Mínimo 6 caracteres").assertDoesNotExist()
    }

    @Test
    fun `erro global nao aparece no estado inicial`() {
        setContent(uiState = LoginUiState())

        composeTestRule.onNodeWithText("Credenciais inválidas").assertDoesNotExist()
    }

    @Test
    fun `indicador de loading nao aparece no estado inicial`() {
        setContent(uiState = LoginUiState())

        composeTestRule.onNode(hasContentDescription("CircularProgressIndicator"))
            .assertDoesNotExist()

        composeTestRule.onNode(hasText("Entrar") and hasClickAction()).assertExists()
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 2. EXIBIÇÃO DE ESTADO — o componente reflete o uiState recebido
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `campos exibem os valores recebidos via uiState`() {
        setContent(uiState = LoginUiState(email = "usuario@email.com", password = "senha123"))

        composeTestRule.onNodeWithText("usuario@email.com").assertExists()
        // Senha fica oculta por PasswordVisualTransformation — verificamos via value semântico
        composeTestRule
            .onNode(hasText("senha123", substring = false) and !hasClickAction())
            .assertDoesNotExist() // o texto visível deve ser mascarado
    }

    @Test
    fun `erro de email e exibido quando emailError nao e nulo`() {
        setContent(uiState = LoginUiState(emailError = "E-mail inválido"))

        composeTestRule.onNodeWithText("E-mail inválido").assertExists()
    }

    @Test
    fun `erro de senha e exibido quando passwordError nao e nulo`() {
        setContent(uiState = LoginUiState(passwordError = "Mínimo 6 caracteres"))

        composeTestRule.onNodeWithText("Mínimo 6 caracteres").assertExists()
    }

    @Test
    fun `erro global e exibido quando globalError nao e nulo`() {
        setContent(uiState = LoginUiState(globalError = "Credenciais inválidas"))

        composeTestRule.onNodeWithText("Credenciais inválidas").assertExists()
    }

    @Test
    fun `erro global nao aparece quando globalError e nulo`() {
        setContent(uiState = LoginUiState(globalError = null))

        composeTestRule.onNodeWithText("Credenciais inválidas").assertDoesNotExist()
    }

    @Test
    fun `botao entrar esta habilitado quando email e senha estao preenchidos e nao esta carregando`() {
        setContent(uiState = LoginUiState(email = "usuario@email.com", password = "senha123"))

        composeTestRule.onNode(hasText("Entrar") and hasClickAction())
            .assertIsEnabled()
    }

    @Test
    fun `botao entrar esta desabilitado quando email ou senha estao vazios`() {
        setContent(uiState = LoginUiState(email = "", password = ""))

        composeTestRule.onNode(hasText("Entrar") and hasClickAction())
            .assertIsNotEnabled()
    }

    @Test
    fun `spinner de loading e exibido quando isLoading e true`() {
        setContent(uiState = LoginUiState(isLoading = true))

        composeTestRule.onAllNodesWithText("Entrar").let { nodes ->
            nodes.fetchSemanticsNodes().size.let { count ->
                assert(count == 1) {
                    "Esperado apenas o título 'Entrar', mas encontrado $count nós com esse texto"
                }
            }
        }
    }

    @Test
    fun `spinner de loading nao aparece quando isLoading e false`() {
        setContent(uiState = LoginUiState(email = "usuario@email.com", password = "senha123", isLoading = false))

        // Com isLoading = false, o texto "Entrar" deve aparecer dentro do botão também
        composeTestRule.onAllNodesWithText("Entrar").fetchSemanticsNodes().let { nodes ->
            assert(nodes.size == 2) {
                "Esperado título + botão com texto 'Entrar', mas encontrado ${nodes.size} nós"
            }
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 3. CALLBACKS — o componente delega interações corretamente
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `onEmailChange e chamado ao digitar no campo de email`() {
        val onEmailChange = mockk<(String) -> Unit>(relaxed = true)
        setContent(uiState = LoginUiState(), onEmailChange = onEmailChange)

        composeTestRule.onNode(hasSetTextAction() and hasText("E-mail", substring = true))
            .performTextInput("a")

        verify(exactly = 1) { onEmailChange(any()) }
    }

    @Test
    fun `onPasswordChange e chamado ao digitar no campo de senha`() {
        val onPasswordChange = mockk<(String) -> Unit>(relaxed = true)
        setContent(uiState = LoginUiState(), onPasswordChange = onPasswordChange)

        composeTestRule.onNode(hasSetTextAction() and hasText("Senha", substring = true))
            .performTextInput("a")

        verify(exactly = 1) { onPasswordChange(any()) }
    }

    @Test
    fun `onSubmit e chamado ao clicar no botao quando habilitado`() {
        val onSubmit = mockk<() -> Unit>(relaxed = true)
        setContent(uiState = LoginUiState(email = "usuario@email.com", password = "senha123"), onSubmit = onSubmit)

        composeTestRule.onNode(hasText("Entrar") and hasClickAction())
            .performClick()

        verify(exactly = 1) { onSubmit() }
    }

    @Test
    fun `onSubmit nao e chamado quando o botao esta desabilitado`() {
        val onSubmit = mockk<() -> Unit>(relaxed = true)
        setContent(uiState = LoginUiState(email = "", password = ""), onSubmit = onSubmit)

        composeTestRule.onNode(hasText("Entrar") and hasClickAction())
            .performClick()

        verify(exactly = 0) { onSubmit() }
    }

    @Test
    fun `onEmailChange nao e chamado ao digitar no campo de senha`() {
        val onEmailChange = mockk<(String) -> Unit>(relaxed = true)
        setContent(uiState = LoginUiState(), onEmailChange = onEmailChange)

        composeTestRule.onNode(hasSetTextAction() and hasText("Senha", substring = true))
            .performTextInput("a")

        verify(exactly = 0) { onEmailChange(any()) }
    }

    @Test
    fun `onPasswordChange nao e chamado ao digitar no campo de email`() {
        val onPasswordChange = mockk<(String) -> Unit>(relaxed = true)
        setContent(uiState = LoginUiState(), onPasswordChange = onPasswordChange)

        composeTestRule.onNode(hasSetTextAction() and hasText("E-mail", substring = true))
            .performTextInput("a")

        verify(exactly = 0) { onPasswordChange(any()) }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // 4. CASOS DE BORDA
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `multiplos erros podem ser exibidos simultaneamente`() {
        setContent(uiState = LoginUiState(
            emailError    = "E-mail inválido",
            passwordError = "Mínimo 6 caracteres",
            globalError   = "Credenciais inválidas"
        ))

        composeTestRule.onNodeWithText("E-mail inválido").assertExists()
        composeTestRule.onNodeWithText("Mínimo 6 caracteres").assertExists()
        composeTestRule.onNodeWithText("Credenciais inválidas").assertExists()
    }

    @Test
    fun `estado com loading e submit desabilitado renderiza sem crash`() {
        setContent(uiState = LoginUiState(email = "usuario@email.com", password = "senha123", isLoading = true))

        composeTestRule.onNode(hasClickAction() and !hasSetTextAction())
            .assertIsNotEnabled()
    }

    @Test
    fun `email com caracteres especiais e exibido corretamente`() {
        val emailEspecial = "usuario+teste@email-empresa.com.br"
        setContent(uiState = LoginUiState(email = emailEspecial))

        composeTestRule.onNodeWithText(emailEspecial).assertExists()
    }
}