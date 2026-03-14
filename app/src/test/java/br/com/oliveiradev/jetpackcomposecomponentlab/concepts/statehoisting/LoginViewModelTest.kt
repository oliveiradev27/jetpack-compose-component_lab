package br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Testes síncronos — sem coroutine, leem .value diretamente
    // -------------------------------------------------------------------------

    @Test
    fun `estado inicial deve ser vazio`() {
        val state = viewModel.uiState.value

        assertEquals("", state.email)
        assertEquals("", state.password)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.globalError)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isSubmitEnabled)
    }

    @Test
    fun `onEmailChange deve atualizar email e limpar emailError`() {
        // Arrange: força um emailError no estado via submit sem dados
        viewModel.onSubmit()
        assertNotNull(viewModel.uiState.value.emailError)

        // Act
        viewModel.onEmailChange("novo@email.com")

        // Assert
        val state = viewModel.uiState.value
        assertEquals("novo@email.com", state.email)
        assertNull(state.emailError) // erro deve ter sido limpo
    }

    @Test
    fun `onPasswordChange deve atualizar senha e limpar passwordError`() {
        viewModel.onSubmit()
        assertNotNull(viewModel.uiState.value.passwordError)

        viewModel.onPasswordChange("senha123")

        val state = viewModel.uiState.value
        assertEquals("senha123", state.password)
        assertNull(state.passwordError)
    }

    @Test
    fun `onSubmit com email sem arroba deve setar emailError`() {
        viewModel.onEmailChange("email-invalido")
        viewModel.onPasswordChange("senha123")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertFalse(state.isLoading) // não deve ter iniciado chamada de rede
    }

    @Test
    fun `onSubmit com senha menor que 6 caracteres deve setar passwordError`() {
        viewModel.onEmailChange("valido@email.com")
        viewModel.onPasswordChange("123")

        viewModel.onSubmit()

        assertNotNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `onSubmit com campos vazios deve setar ambos os erros`() {
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
    }

    @Test
    fun `isSubmitEnabled deve ser false quando email esta vazio`() {
        viewModel.onPasswordChange("senha123")
        assertFalse(viewModel.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `isSubmitEnabled deve ser true quando email e senha estao preenchidos`() {
        viewModel.onEmailChange("user@email.com")
        viewModel.onPasswordChange("senha123")
        assertTrue(viewModel.uiState.value.isSubmitEnabled)
    }

    // -------------------------------------------------------------------------
    // Testes assíncronos com Turbine — observam emissões do StateFlow
    //
    // Estratégia: fazemos as ações ANTES de abrir o bloco test { },
    // assim o bloco já encontra o estado final como primeiro item.
    // Alternativamente, podemos usar skipItems() para descartar o
    // estado inicial emitido automaticamente pelo StateFlow.
    // -------------------------------------------------------------------------

    @Test
    fun `onSubmit com credenciais validas deve emitir isSuccess true`() = runTest {
        // Arrange
        coEvery { authRepository.login(any(), any()) } returns
                AuthResult.Success(User(id = "1", email = "user@email.com"))

        viewModel.onEmailChange("user@email.com")
        viewModel.onPasswordChange("senha123")

        // Act + Assert com Turbine
        viewModel.uiState.test {
            // Consome o estado atual antes do submit (emitido pelo StateFlow ao coletar)
            skipItems(1)

            viewModel.onSubmit()

            // Como usamos UnconfinedTestDispatcher, o submit já completou.
            // O próximo item é o estado final com isSuccess = true.
            val finalState = awaitItem()
            assertTrue(finalState.isSuccess)
            assertFalse(finalState.isLoading)
            assertNull(finalState.globalError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSubmit com erro de API deve emitir globalError`() = runTest {
        // Arrange
        coEvery { authRepository.login(any(), any()) } returns
                AuthResult.Error("Credenciais inválidas.")

        viewModel.onEmailChange("erro@erro.com")
        viewModel.onPasswordChange("senha123")

        // Act + Assert com Turbine
        viewModel.uiState.test {
            skipItems(1) // descarta estado atual antes do submit

            viewModel.onSubmit()

            val finalState = awaitItem()
            assertEquals("Credenciais inválidas.", finalState.globalError)
            assertFalse(finalState.isLoading)
            assertFalse(finalState.isSuccess)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSubmit deve emitir isLoading true durante chamada de rede`() = runTest {
        // Arrange: repositório com delay para conseguir capturar o estado intermediário
        coEvery { authRepository.login(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(Long.MAX_VALUE) // nunca completa neste teste
            AuthResult.Success(User(id = "1", email = ""))
        }

        viewModel.onEmailChange("user@email.com")
        viewModel.onPasswordChange("senha123")

        viewModel.uiState.test {
            skipItems(1) // descarta estado antes do submit

            viewModel.onSubmit()

            // Com StandardTestDispatcher, a coroutine fica suspensa no delay.
            // O estado de loading é emitido antes do delay.
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertFalse(loadingState.isSubmitEnabled) // botão desabilitado durante loading

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sequencia completa de estados durante submit com sucesso`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns
                AuthResult.Success(User(id = "1", email = "user@email.com"))

        viewModel.uiState.test {
            // 1. Estado inicial emitido pelo StateFlow ao coletar
            val initial = awaitItem()
            assertFalse(initial.isLoading)
            assertFalse(initial.isSuccess)

            // 2. Preenche os campos — cada update emite um novo estado
            viewModel.onEmailChange("user@email.com")
            skipItems(1) // descarta emissão do onEmailChange

            viewModel.onPasswordChange("senha123")
            skipItems(1) // descarta emissão do onPasswordChange

            // 3. Submit — com UnconfinedTestDispatcher emite loading + success
            viewModel.onSubmit()

            // 4. Estado final: isSuccess = true
            val successState = awaitItem()
            assertTrue(successState.isSuccess)
            assertFalse(successState.isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }
}