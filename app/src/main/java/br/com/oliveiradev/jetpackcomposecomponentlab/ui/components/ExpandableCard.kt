package br.com.oliveiradev.jetpackcomposecomponentlab.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Card expansível reutilizável com título sempre visível e conteúdo colapsável.
 *
 * Comportamento:
 * - Exibe sempre o [title] na área de cabeçalho do card.
 * - Um [IconButton] à direita alterna entre os ícones "arrowDown" e "arrowUp"
 *   (implementados aqui como ExpandMore / ExpandLess).
 * - Quando expandido, o [content] é exibido logo abaixo do cabeçalho.
 *
 * Uso típico:
 *
 * ExpandableCard(
 *     title = "Detalhes do pedido"
 * ) {
 *     Text("Conteúdo interno do card...")
 * }
 */
@Composable
fun ExpandableCard(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    val icon = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                    val description = if (isExpanded) {
                        "Recolher seção"
                    } else {
                        "Expandir seção"
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = description
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("ExpandableCardContent")
                ) {
                    content()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableCardCollapsedPreview() {
    MaterialTheme {
        ExpandableCard(
            title = "Informações adicionais",
            initiallyExpanded = false
        ) {
            Text("Este é o conteúdo que será exibido quando o card estiver expandido.")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableCardExpandedPreview() {
    MaterialTheme {
        ExpandableCard(
            title = "Informações adicionais",
            initiallyExpanded = true
        ) {
            Text("Este é o conteúdo que será exibido quando o card estiver expandido.")
        }
    }
}

