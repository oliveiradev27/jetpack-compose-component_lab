package br.com.oliveiradev.jetpackcomposecomponentlab.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.oliveiradev.jetpackcomposecomponentlab.ui.components.ExpandableCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Estratégia de teste — ExpandableCard
 *
 * 1. ESTADO INICIAL
 *    - Título é sempre exibido.
 *    - Conteúdo NÃO é exibido quando initiallyExpanded = false.
 *    - Conteúdo é exibido quando initiallyExpanded = true.
 *
 * 2. INTERAÇÃO
 *    - Clicar no botão de expandir revela o conteúdo.
 *    - Clicar novamente recolhe o conteúdo.
 *
 * 3. ACESSIBILIDADE / SEMÂNTICA
 *    - O ícone de ação alterna a descrição de conteúdo "Expandir seção" / "Recolher seção".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExpandableCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        title: String = "Título do card",
        initiallyExpanded: Boolean = false,
        bodyText: String = "Conteúdo interno do card"
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                ExpandableCard(
                    title = title,
                    initiallyExpanded = initiallyExpanded
                ) {
                    Text(text = bodyText)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 1. ESTADO INICIAL
    // -------------------------------------------------------------------------

    @Test
    fun `titulo e exibido mesmo quando card esta recolhido`() {
        setContent(title = "Detalhes", initiallyExpanded = false)

        composeTestRule.onNodeWithText("Detalhes").assertExists()
    }

    @Test
    fun `conteudo nao e exibido quando initiallyExpanded e false`() {
        val body = "Texto secreto"
        setContent(initiallyExpanded = false, bodyText = body)

        composeTestRule.onNodeWithTag("ExpandableCardContent").assertDoesNotExist()
        composeTestRule.onNodeWithText(body).assertDoesNotExist()
    }

    @Test
    fun `conteudo e exibido quando initiallyExpanded e true`() {
        val body = "Texto visivel"
        setContent(initiallyExpanded = true, bodyText = body)

        composeTestRule.onNodeWithTag("ExpandableCardContent").assertExists()
        composeTestRule.onNodeWithText(body).assertExists()
    }

    // -------------------------------------------------------------------------
    // 2. INTERACAO — toggle expandir recolher
    // -------------------------------------------------------------------------

    @Test
    fun `clicar no icone expande o card exibindo o conteudo`() {
        val body = "Lorem ipsum"
        setContent(initiallyExpanded = false, bodyText = body)

        composeTestRule.onNodeWithTag("ExpandableCardContent").assertDoesNotExist()

        composeTestRule.onNode(hasContentDescription("Expandir seção")).performClick()

        composeTestRule.onNodeWithTag("ExpandableCardContent").assertExists()
        composeTestRule.onNodeWithText(body).assertExists()
    }

    @Test
    fun `clicar duas vezes alterna entre expandido e recolhido`() {
        val body = "Texto alternado"
        setContent(initiallyExpanded = false, bodyText = body)

        // 1º clique — expande
        composeTestRule.onNode(hasContentDescription("Expandir seção")).performClick()
        composeTestRule.onNodeWithTag("ExpandableCardContent").assertExists()

        // 2º clique — recolhe
        composeTestRule.onNode(hasContentDescription("Recolher seção")).performClick()
        composeTestRule.onNodeWithTag("ExpandableCardContent").assertDoesNotExist()
        composeTestRule.onNodeWithText(body).assertDoesNotExist()
    }

    // -------------------------------------------------------------------------
    // 3. ACESSIBILIDADE — contentDescription do icone
    // -------------------------------------------------------------------------

    @Test
    fun `icone usa descricao de expandir quando card esta recolhido`() {
        setContent(initiallyExpanded = false)

        composeTestRule.onNode(hasContentDescription("Expandir seção")).assertExists()
    }

    @Test
    fun `icone usa descricao de recolher quando card esta expandido`() {
        setContent(initiallyExpanded = true)

        composeTestRule.onNode(hasContentDescription("Recolher seção")).assertExists()
    }
}

