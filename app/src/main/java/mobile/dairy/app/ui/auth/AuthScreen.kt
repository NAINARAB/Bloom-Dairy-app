package mobile.dairy.app.ui.auth

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import mobile.dairy.app.R
import mobile.dairy.app.data.AuthRepository
import mobile.dairy.app.data.AppDatabase
import mobile.dairy.app.data.newId
import mobile.dairy.app.domain.LocalUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.security.MessageDigest

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val localPrefs: mobile.dairy.app.services.LocalPrefs,
    private val database: AppDatabase,
) : ViewModel() {

    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val resetSent = MutableStateFlow(false)

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            try {
                block()
            } catch (e: Exception) {
                error.value = AuthRepository.friendlyError(e)
            } finally {
                busy.value = false
            }
        }
    }

    fun signIn(email: String, password: String) = run { authRepository.signIn(email, password) }
    fun signUp(name: String, email: String, password: String) = run { authRepository.signUp(name, email, password) }
    fun reset(email: String) = run { authRepository.sendReset(email); resetSent.value = true }
    fun google(context: Context, webClientId: String) = run { authRepository.signInWithGoogle(context, webClientId) }
    
    private fun hash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun localSignUp(name: String, phone: String, pass: String) = run {
        val id = newId()
        database.localUserDao().upsertLocalUser(
            LocalUser(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                passwordHash = hash(pass),
                firebaseUid = null,
                createdAt = System.currentTimeMillis()
            )
        )
        localPrefs.setActiveLocalUserId(id)
        localPrefs.setOnlineMode(false)
    }

    fun localSignIn(phone: String, pass: String) = run {
        val users = database.localUserDao().getAllLocalUsers().firstOrNull() ?: emptyList()
        val user = users.find { it.phone == phone.trim() }
        if (user != null && user.passwordHash == hash(pass)) {
            localPrefs.setActiveLocalUserId(user.id)
            localPrefs.setOnlineMode(false)
        } else {
            error.value = "Incorrect phone number or password."
        }
    }
}

private enum class Mode { WELCOME, SIGN_IN, SIGN_UP, RESET, OFFLINE_SIGN_IN, OFFLINE_SIGN_UP }

@Composable
fun AuthScreen(vm: AuthViewModel = hiltViewModel()) {
    var mode by rememberSaveable { mutableStateOf(Mode.WELCOME) }
    val busy by vm.busy.collectAsStateSafe(false)
    val error by vm.error.collectAsStateSafe(null)
    val resetSent by vm.resetSent.collectAsStateSafe(false)
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id_bloom)

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun validEmail() = Regex("^\\S+@\\S+\\.\\S+$").matches(email.trim())
    fun validPhone() = phone.trim().length >= 7

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("\uD83C\uDF31", fontSize = 52.sp)
            Spacer(Modifier.height(12.dp))
            Text("Bloom", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                when (mode) {
                    Mode.WELCOME -> "A private daily diary that helps you understand yourself, stay close to your goals, and grow a little every day."
                    Mode.SIGN_IN -> "Welcome back."
                    Mode.SIGN_UP -> "Private to you, synced across your devices."
                    Mode.RESET -> if (resetSent) "Check your inbox — a reset link is on its way." else "Enter your email and we'll send a reset link."
                    Mode.OFFLINE_SIGN_UP -> "Create a local profile to use Bloom completely offline."
                    Mode.OFFLINE_SIGN_IN -> "Welcome back to your local profile."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            when (mode) {
                Mode.WELCOME -> {
                    Button(
                        onClick = { vm.google(context, webClientId) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    ) { Text("Continue with Google") }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { mode = Mode.SIGN_IN },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Use email instead") }
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = { mode = Mode.OFFLINE_SIGN_IN },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Use offline mode", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Your entries are private to your account. Always.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Mode.SIGN_IN, Mode.SIGN_UP -> {
                    if (mode == Mode.SIGN_UP) {
                        OutlinedTextField(
                            value = name, onValueChange = { name = it },
                            label = { Text("Name") },
                            placeholder = { Text("What should Bloom call you?") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            localError = when {
                                mode == Mode.SIGN_UP && name.trim().length < 2 -> "Tell us what to call you."
                                !validEmail() -> "Enter a valid email address."
                                password.length < 6 -> "Passwords need at least 6 characters."
                                else -> null
                            }
                            if (localError == null) {
                                if (mode == Mode.SIGN_IN) vm.signIn(email, password)
                                else vm.signUp(name, email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    ) {
                        Text(if (mode == Mode.SIGN_IN) "Sign in" else "Create account")
                    }
                    TextButton(onClick = { mode = Mode.RESET }) { Text("Forgot password?") }
                    TextButton(onClick = {
                        mode = if (mode == Mode.SIGN_IN) Mode.SIGN_UP else Mode.SIGN_IN
                    }) {
                        Text(if (mode == Mode.SIGN_IN) "New here? Create an account" else "I already have an account")
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { mode = Mode.WELCOME }) { Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }

                Mode.OFFLINE_SIGN_UP, Mode.OFFLINE_SIGN_IN -> {
                    if (mode == Mode.OFFLINE_SIGN_UP) {
                        OutlinedTextField(
                            value = name, onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            localError = when {
                                mode == Mode.OFFLINE_SIGN_UP && name.trim().length < 2 -> "Name is required."
                                !validPhone() -> "Enter a valid phone number."
                                password.length < 4 -> "Password needs at least 4 characters."
                                else -> null
                            }
                            if (localError == null) {
                                if (mode == Mode.OFFLINE_SIGN_IN) vm.localSignIn(phone, password)
                                else vm.localSignUp(name, phone, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    ) {
                        Text(if (mode == Mode.OFFLINE_SIGN_IN) "Sign in locally" else "Create local profile")
                    }
                    TextButton(onClick = {
                        mode = if (mode == Mode.OFFLINE_SIGN_IN) Mode.OFFLINE_SIGN_UP else Mode.OFFLINE_SIGN_IN
                    }) {
                        Text(if (mode == Mode.OFFLINE_SIGN_IN) "New here? Create local profile" else "I already have a local profile")
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { mode = Mode.WELCOME }) { Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }

                Mode.RESET -> {
                    if (!resetSent) {
                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                if (validEmail()) vm.reset(email) else localError =
                                    "Enter a valid email address."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) { Text("Send reset link") }
                    }
                    TextButton(onClick = { mode = Mode.SIGN_IN }) { Text("Back to sign in") }
                }
            }

            val shown = localError ?: error
            if (shown != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    shown,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Smooth full-screen loading overlay
        mobile.dairy.app.ui.components.GlobalLoadingOverlay(busy, "Authenticating...")
    }
}

/** collectAsState with an initial value, shared by all screens. */
@Composable
fun <T> Flow<T>.collectAsStateSafe(initial: T): State<T> =
    produceState(initialValue = initial, this) {
        collect { value = it }
    }
