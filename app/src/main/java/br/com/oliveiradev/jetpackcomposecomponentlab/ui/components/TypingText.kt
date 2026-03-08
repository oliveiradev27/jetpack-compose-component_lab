package br.com.oliveiradev.jetpackcomposecomponentlab.ui.components


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

// =============================================================================
// COMPONENTE PRINCIPAL
// =============================================================================

/**
 * Exibe um texto com animação de "digitação" palavra por palavra.
 *
 * Como funciona:
 * 1. O texto é dividido em palavras via split(" ").
 * 2. Um LaunchedEffect roda uma coroutine que, a cada [intervalMs] milissegundos,
 *    incrementa o índice de palavras visíveis.
 * 3. Quando todas as palavras forem exibidas, [onFinished] é invocado.
 * 4. Um segundo efeito controla o cursor piscante enquanto a animação ocorre.
 *
 * @param text        O texto completo a ser animado.
 * @param modifier    Modifier padrão do Compose para customização externa.
 * @param style       Estilo tipográfico do Text (padrão: bodyMedium do MaterialTheme).
 * @param intervalMs  Intervalo em milissegundos entre cada palavra (padrão: 30ms).
 * @param showCursor  Se deve exibir o cursor piscante durante a animação (padrão: true).
 * @param onFinished  Callback invocado uma única vez quando a animação terminar.
 */
@Composable
fun TypingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    intervalMs: Long = 30L,
    showCursor: Boolean = true,
    onFinished: () -> Unit = {}
) {
    // -------------------------------------------------------------------------
    // PASSO 1 — Dividir o texto em palavras
    // Usamos filter { it.isNotEmpty() } para evitar strings vazias caso haja
    // espaços duplos ou espaços no início/fim.
    // -------------------------------------------------------------------------
    val words = remember(text) {
        text.trim().split(" ").filter { it.isNotEmpty() }
    }

    // -------------------------------------------------------------------------
    // PASSO 2 — Estado: quantas palavras já foram reveladas
    // Começa em 0 (nenhuma palavra visível).
    // -------------------------------------------------------------------------
    var visibleWordCount by remember(text) { mutableIntStateOf(0) }

    // -------------------------------------------------------------------------
    // PASSO 3 — Estado: animação concluída
    // Usado para parar o cursor e garantir que onFinished seja chamado só uma vez.
    // -------------------------------------------------------------------------
    var isFinished by remember(text) { mutableStateOf(false) }

    // -------------------------------------------------------------------------
    // PASSO 4 — Estado: cursor piscante visível ou não
    // -------------------------------------------------------------------------
    var cursorVisible by remember { mutableStateOf(true) }

    // -------------------------------------------------------------------------
    // PASSO 5 — Efeito principal: revela as palavras uma a uma
    //
    // LaunchedEffect(text) garante que a animação reinicie sempre que o texto
    // mudar. A key "text" é o gatilho — se o texto não mudar, o efeito não
    // é relançado.
    //
    // O loop:
    //   - Reseta o estado para o início (útil em recomposições com texto novo).
    //   - Itera de 1 até words.size, fazendo delay entre cada palavra.
    //   - Ao terminar, marca isFinished = true e chama o callback.
    // -------------------------------------------------------------------------
    LaunchedEffect(text) {
        visibleWordCount = 0
        isFinished = false

        for (i in 1..words.size) {
            delay(intervalMs)          // aguarda o intervalo configurado
            visibleWordCount = i       // revela mais uma palavra
        }

        isFinished = true
        onFinished()                   // 🔔 callback para o chamador
    }

    // -------------------------------------------------------------------------
    // PASSO 6 — Efeito do cursor piscante
    //
    // Roda em paralelo com o efeito principal.
    // Enquanto a animação não terminar, alterna cursorVisible a cada 500ms.
    // Quando isFinished = true, esconde o cursor definitivamente.
    // -------------------------------------------------------------------------
    LaunchedEffect(isFinished) {
        if (!isFinished) {
            while (true) {
                delay(500L)
                cursorVisible = !cursorVisible
            }
        } else {
            cursorVisible = false      // esconde o cursor ao terminar
        }
    }

    // -------------------------------------------------------------------------
    // PASSO 7 — Montar o texto visível
    //
    // Pegamos apenas as primeiras [visibleWordCount] palavras e juntamos
    // com espaço. Depois, concatenamos o cursor se necessário.
    // -------------------------------------------------------------------------
    val displayText = buildString {
        append(words.take(visibleWordCount).joinToString(" "))

        if (showCursor && !isFinished) {
            append(if (cursorVisible) " |" else "  ")  // cursor com largura fixa
        }
    }

    // -------------------------------------------------------------------------
    // PASSO 8 — Renderizar
    // -------------------------------------------------------------------------
    Text(
        text = displayText,
        modifier = modifier,
        style = style
    )
}

@Preview(showBackground = true)
@Composable
fun TypingTextPreview() {
    TypingText(
        "Olá eu sou o Dollynho, seu amiguinho. vamos brincar?",
        showCursor = false,
        intervalMs = 0
    )
}



