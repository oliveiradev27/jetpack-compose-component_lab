package br.com.oliveiradev.jetpackcomposecomponentlab.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.com.oliveiradev.jetpackcomposecomponentlab.ui.components.TypingText
import io.mockk.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TypingTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        // Tempo suficiente para qualquer animação dos testes terminar
        private const val SAFE_ANIMATION_TIME = 5_000L

        // Tempo alto o suficiente para "congelar" a animação durante asserts
        private const val FROZEN_INTERVAL = 10_000L
    }

    @Before
    fun setup() {
        composeTestRule.mainClock.autoAdvance = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private fun setContent(
        text: String,
        intervalMs: Long = 100L,
        showCursor: Boolean = true,
        onFinished: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                TypingText(
                    text = text,
                    intervalMs = intervalMs,
                    showCursor = showCursor,
                    onFinished = onFinished
                )
            }
        }
    }

    /**
     * Avança N intervalos + 10ms de margem e aguarda o Compose recompor.
     * Usado para verificar estados INTERMEDIÁRIOS da animação.
     *
     * Por que waitForIdle() é necessário aqui também?
     *   advanceTimeBy() avança o relógio virtual e desbloqueia as coroutines,
     *   mas o Compose ainda não processou as mudanças de estado resultantes.
     *   Sem waitForIdle(), a árvore semântica não foi atualizada e os asserts
     *   falham mesmo que o estado interno já tenha mudado.
     */
    private fun advanceIntervals(count: Int, intervalMs: Long) {
        // Garante que o LaunchedEffect teve ao menos 1 frame para iniciar.
        // Sem isso, o delay() interno ainda não começou a contar quando
        // advanceTimeBy() é chamado, e o estado não avança.
        composeTestRule.mainClock.advanceTimeBy(1L)
        composeTestRule.waitForIdle()

        composeTestRule.mainClock.advanceTimeBy(intervalMs * count + 10L)
        composeTestRule.waitForIdle()
    }

    /**
     * Drena completamente os delays das coroutines e aguarda o Compose estabilizar.
     *
     * Por que advanceTimeBy() + waitForIdle() e não só waitForIdle()?
     *   waitForIdle() aguarda recomposições, mas não avança o relógio virtual.
     *   Coroutines em delay() ficam suspensas indefinidamente sem advanceTimeBy().
     *   A combinação garante que: (1) os delays passam, (2) o Compose recompõe.
     */
    private fun drainAnimation() {
        composeTestRule.mainClock.advanceTimeBy(SAFE_ANIMATION_TIME)
        composeTestRule.waitForIdle()
    }


    // =========================================================================
    // 1. ESTADO INICIAL
    // =========================================================================

    @Test
    fun `ao iniciar nenhuma palavra esta visivel`() {
        setContent("Olá mundo Compose", intervalMs = FROZEN_INTERVAL)

        composeTestRule.onNodeWithText("Olá", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("mundo", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Compose", substring = true).assertDoesNotExist()
    }

    @Test
    fun `cursor esta visivel no estado inicial quando showCursor e true`() {
        setContent("Olá mundo", intervalMs = FROZEN_INTERVAL, showCursor = true)

        composeTestRule.onNodeWithText("|", substring = true).assertExists()
    }

    @Test
    fun `cursor nao aparece no estado inicial quando showCursor e false`() {
        setContent("Olá mundo", intervalMs = FROZEN_INTERVAL, showCursor = false)

        composeTestRule.onNodeWithText("|", substring = true).assertDoesNotExist()
    }


    // =========================================================================
    // 2. PROGRESSÃO — palavra por palavra
    // =========================================================================

    @Test
    fun `primeira palavra aparece apos um intervalo`() {
        setContent("Olá mundo Compose", intervalMs = 100L)

        advanceIntervals(count = 1, intervalMs = 100L)

        composeTestRule.onNodeWithText("Olá", substring = true).assertExists()
        composeTestRule.onNodeWithText("mundo", substring = true).assertDoesNotExist()
    }

    @Test
    fun `segunda palavra aparece apos dois intervalos`() {
        setContent("Olá mundo Compose", intervalMs = 100L)

        advanceIntervals(count = 2, intervalMs = 100L)

        composeTestRule.onNodeWithText("Olá mundo", substring = true).assertExists()
        composeTestRule.onNodeWithText("Compose", substring = true).assertDoesNotExist()
    }

    @Test
    fun `todas as palavras aparecem apos todos os intervalos`() {
        setContent("Olá mundo Compose", intervalMs = 100L)

        advanceIntervals(count = 3, intervalMs = 100L)

        composeTestRule.onNodeWithText("Olá mundo Compose", substring = true).assertExists()
    }

    @Test
    fun `palavras aparecem estritamente uma a uma sem pular`() {
        val words = listOf("Um", "dois", "três", "quatro")
        val intervalMs = 200L
        setContent(words.joinToString(" "), intervalMs = intervalMs)

        words.forEachIndexed { index, word ->
            advanceIntervals(count = 1, intervalMs = intervalMs)

            composeTestRule.onNodeWithText(word, substring = true).assertExists()

            words.getOrNull(index + 1)?.let { next ->
                composeTestRule.onNodeWithText(next, substring = true).assertDoesNotExist()
            }
        }
    }

    @Test
    fun `nenhuma palavra aparece antes do primeiro intervalo`() {
        setContent("Olá mundo", intervalMs = 100L)

        composeTestRule.mainClock.advanceTimeBy(50L)

        composeTestRule.onNodeWithText("Olá", substring = true).assertDoesNotExist()
    }

    @Test
    fun `texto de uma unica palavra e exibido apos um intervalo`() {
        setContent("Compose", intervalMs = 100L)

        advanceIntervals(count = 1, intervalMs = 100L)

        composeTestRule.onNodeWithText("Compose", substring = true).assertExists()
    }


    // =========================================================================
    // 3. ESTADO FINAL
    // =========================================================================

    @Test
    fun `texto completo e exibido ao final da animacao`() {
        setContent("Olá mundo Compose", intervalMs = 100L)

        // drainAnimation() avança o relógio virtual e drena os delays das coroutines.
        // Sem isso, waitForIdle() sozinho não faz a animação progredir.
        drainAnimation()

        composeTestRule.onNodeWithText("Olá mundo Compose", substring = true).assertExists()
    }

    @Test
    fun `cursor some ao terminar a animacao com showCursor true`() {
        setContent("Olá mundo", intervalMs = 100L, showCursor = true)

        // Avança além do tempo da animação (2 palavras × 100ms) + 500ms do cursor
        composeTestRule.mainClock.advanceTimeBy(800L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("|", substring = true).assertDoesNotExist()
    }

    @Test
    fun `cursor nunca aparece ao terminar quando showCursor e false`() {
        setContent("Olá mundo", intervalMs = 100L, showCursor = false)

        drainAnimation()

        composeTestRule.onNodeWithText("|", substring = true).assertDoesNotExist()
    }

    @Test
    fun `texto permanece estavel apos animacao terminar`() {
        setContent("Olá mundo Compose", intervalMs = 100L)

        drainAnimation()
        // Avança mais tempo para garantir que o texto não muda
        composeTestRule.mainClock.advanceTimeBy(2_000L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Olá mundo Compose", substring = true).assertExists()
    }


    // =========================================================================
    // 4. CALLBACK onFinished
    // =========================================================================

    @Test
    fun `onFinished e chamado exatamente uma vez ao terminar`() {
        val onFinished = mockk<() -> Unit>(relaxed = true)
        setContent("Olá mundo", intervalMs = 100L, onFinished = onFinished)

        drainAnimation()

        verify(exactly = 1) { onFinished() }
    }

    @Test
    fun `onFinished nao e chamado antes da animacao terminar`() {
        val onFinished = mockk<() -> Unit>(relaxed = true)
        setContent("Olá mundo Compose", intervalMs = FROZEN_INTERVAL, onFinished = onFinished)

        advanceIntervals(count = 1, intervalMs = FROZEN_INTERVAL)

        verify(exactly = 0) { onFinished() }
    }

    @Test
    fun `onFinished e chamado para texto de uma unica palavra`() {
        val onFinished = mockk<() -> Unit>(relaxed = true)
        setContent("Compose", intervalMs = 100L, onFinished = onFinished)

        drainAnimation()

        verify(exactly = 1) { onFinished() }
    }

    @Test
    fun `onFinished e chamado mesmo para texto vazio pois esta fora do loop`() {
        // COMPORTAMENTO REAL DO COMPONENTE:
        // O loop `for (i in 1..words.size)` não executa quando words está vazio,
        // mas onFinished() está APÓS o loop — é chamado de qualquer forma.
        //
        // Se isso for indesejado, a correção é no componente:
        //   if (words.isNotEmpty()) onFinished()
        //
        // Este teste documenta o comportamento atual, não um bug dos testes.
        val onFinished = mockk<() -> Unit>(relaxed = true)
        setContent("", intervalMs = 100L, onFinished = onFinished)

        drainAnimation()

        verify(exactly = 1) { onFinished() }
    }


    // =========================================================================
    // 5. MUDANÇA DE TEXTO
    // =========================================================================

    @Test
    fun `animacao reinicia do zero quando o texto muda`() {
        var currentText by mutableStateOf("Primeiro texto")

        composeTestRule.setContent {
            MaterialTheme {
                TypingText(text = currentText, intervalMs = 100L)
            }
        }

        drainAnimation()
        composeTestRule.onNodeWithText("Primeiro texto", substring = true).assertExists()

        // Troca o texto
        currentText = "Segundo texto"
        composeTestRule.waitForIdle() // processa a recomposição

        // Sem avançar o tempo — nenhuma palavra do novo texto visível ainda
        composeTestRule.onNodeWithText("Segundo", substring = true).assertDoesNotExist()
    }

    @Test
    fun `novo texto aparece progressivamente apos mudanca`() {
        // Usa textos completamente distintos para evitar falso positivo no assertExists.
        val intervalMs = 100L
        var currentText by mutableStateOf("Primeira mensagem")

        composeTestRule.setContent {
            MaterialTheme {
                TypingText(text = currentText, intervalMs = intervalMs)
            }
        }

        drainAnimation()
        composeTestRule.onNodeWithText("Primeira mensagem", substring = true).assertExists()

        // Troca o texto. O LaunchedEffect(text) é cancelado e relançado do zero.
        currentText = "Segundo bloco agora"
        composeTestRule.waitForIdle()

        // Avança tempo suficiente para exatamente 1 palavra aparecer (intervalo + margem),
        // mas NÃO suficiente para a segunda (menos que 2 * intervalMs).
        // Usamos um valor entre intervalMs e 2*intervalMs para garantir só a primeira.
        composeTestRule.mainClock.advanceTimeBy(intervalMs + intervalMs / 2)
        composeTestRule.waitForIdle()

        // Só a primeira palavra do novo texto deve estar visível
        composeTestRule.onNodeWithText("Segundo", substring = true).assertExists()
        composeTestRule.onNodeWithText("bloco", substring = true).assertDoesNotExist()
    }

    @Test
    fun `onFinished e chamado novamente apos mudanca de texto`() {
        val onFinished = mockk<() -> Unit>(relaxed = true)
        var currentText by mutableStateOf("Texto um")

        composeTestRule.setContent {
            MaterialTheme {
                TypingText(text = currentText, intervalMs = 100L, onFinished = onFinished)
            }
        }

        // Primeira animação
        drainAnimation()
        verify(exactly = 1) { onFinished() }

        // Segunda animação
        currentText = "Texto dois"
        composeTestRule.waitForIdle()
        drainAnimation()

        verify(exactly = 2) { onFinished() }
    }


    // =========================================================================
    // 6. CASOS DE BORDA
    // =========================================================================

    @Test
    fun `texto com espacos duplos e normalizado corretamente`() {
        setContent("Olá  mundo", intervalMs = 100L)

        drainAnimation()

        composeTestRule.onNodeWithText("Olá mundo", substring = true).assertExists()
    }

    @Test
    fun `texto com espacos no inicio e fim e normalizado`() {
        setContent("  Olá mundo  ", intervalMs = 100L)

        drainAnimation()

        composeTestRule.onNodeWithText("Olá mundo", substring = true).assertExists()
    }

    @Test
    fun `intervalo minimo de 1ms completa animacao sem erros`() {
        setContent("Olá mundo Compose", intervalMs = 1L)

        drainAnimation()

        composeTestRule.onNodeWithText("Olá mundo Compose", substring = true).assertExists()
    }

    @Test
    fun `texto longo completa animacao corretamente`() {
        val longText = (1..20).joinToString(" ") { "palavra$it" }
        setContent(longText, intervalMs = 10L)

        drainAnimation()

        composeTestRule.onNodeWithText("palabra1", substring = true).assertExists()
        composeTestRule.onNodeWithText("palavra20", substring = true).assertExists()
    }
}