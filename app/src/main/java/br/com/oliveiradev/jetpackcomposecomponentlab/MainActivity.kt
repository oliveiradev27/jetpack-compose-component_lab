package br.com.oliveiradev.jetpackcomposecomponentlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting.AuthRepositoryImpl
import br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting.LoginScreen
import br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting.LoginViewModel
import br.com.oliveiradev.jetpackcomposecomponentlab.ui.components.ExpandableCard
import br.com.oliveiradev.jetpackcomposecomponentlab.ui.components.TypingText
import br.com.oliveiradev.jetpackcomposecomponentlab.ui.theme.JetpackComposeComponentLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetpackComposeComponentLabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier.height(32.dp))
        Text(text = "Componente de Texto Dinâmico")
        Spacer(modifier.height(16.dp))

        TypingText(
            text = "Hello $name! Sou o Dollynho, seu amiguinho! vamos brincar?!",
            intervalMs = 500L,
            showCursor = false,
            modifier = modifier
        )

        Spacer(modifier.height(32.dp))
        Text(text = "Form de Login para consolidar conceito de State Hoisting")
        Spacer(modifier.height(16.dp))

        LoginScreen(
            viewModel = LoginViewModel(authRepository = AuthRepositoryImpl()),
            onLoginSuccess = {}
        )

        Spacer(modifier.height(32.dp))
        Text(text = "Card expansível")
        Spacer(modifier.height(16.dp))
        ExpandableCard(
            title = "Título do card",
            initiallyExpanded = false
        ) {
            Text(text = "Conteúdo do card expansível.")
        }
     }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    JetpackComposeComponentLabTheme {
        val viewModel = LoginViewModel(authRepository = AuthRepositoryImpl())
        LoginScreen(onLoginSuccess = {}, viewModel)
    }
}