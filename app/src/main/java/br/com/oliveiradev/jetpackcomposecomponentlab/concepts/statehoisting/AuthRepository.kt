package br.com.oliveiradev.jetpackcomposecomponentlab.concepts.statehoisting

data class User(
    val id: String,
    val email: String
)

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult
}

class AuthRepositoryImpl(
    // Aqui entrariam suas dependências, ex: ApiService, SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            // Simula chamada de rede
            // val response = apiService.login(LoginRequest(email, password))
            // AuthResult.Success(response.toUser())

            // Exemplo de simulação:
            if (email == "erro@erro.com") {
                AuthResult.Error("Credenciais inválidas.")
            } else {
                AuthResult.Success(User(id = "1", email = email))
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Erro desconhecido.")
        }
    }
}