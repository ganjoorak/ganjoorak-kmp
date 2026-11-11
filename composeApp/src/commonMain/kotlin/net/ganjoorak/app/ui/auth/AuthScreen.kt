package net.ganjoorak.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.platform.copyText
import net.ganjoorak.app.ui.components.BorderedCard

/**
 * Login, sign-up and forgot-password, matching the website's AccountPages:
 * same fields, copy, validation, recovery-code panel, and `/api/account` calls.
 * There is no social or email sign-in on the site either.
 */
@Composable
fun AuthScreen(
    startSignup: Boolean,
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
) {
    val graph = AppGraph.instance
    val vm = viewModel(key = "auth-$startSignup") {
        AuthViewModel(graph.accounts, startSignup, onSignedIn)
    }
    val state by vm.ui.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("گنجورک", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "راحت‌تر شعر بخوانیم و شعر گوش دهیم.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )
            BorderedCard(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(state.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    state.description?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    when (state.view) {
                        AuthView.RecoveryCode -> RecoveryCodePanel(state.issuedCode, "ادامه", vm::finishRecovery)
                        AuthView.Reset -> ResetForm(state, vm)
                        AuthView.Form -> AuthForm(state, vm)
                    }
                }
            }
            Text(
                "ganjoorak.ir",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

@Composable
private fun AuthForm(state: AuthUiState, vm: AuthViewModel) {
    val isSignup = state.mode == AuthMode.Signup
    UsernameField(state.username, vm::setUsername, showHint = isSignup)
    PasswordField(
        label = "گذرواژه",
        value = state.password,
        onChange = vm::setPassword,
        newPassword = isSignup,
        showHint = isSignup,
        imeAction = ImeAction.Done,
        onDone = vm::submit,
    )
    if (!isSignup) {
        TextButton(onClick = vm::showReset, modifier = Modifier.fillMaxWidth()) {
            Text("گذرواژه را فراموش کرده‌اید؟")
        }
    }
    if (state.error.isNotBlank()) {
        Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
    Button(onClick = vm::submit, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
        Text(if (isSignup) "ساخت حساب" else "ورود")
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (isSignup) "حساب دارید؟" else "حساب ندارید؟",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { vm.show(if (isSignup) AuthMode.Login else AuthMode.Signup) }) {
            Text(if (isSignup) "ورود" else "ساخت حساب")
        }
    }
}

@Composable
private fun ResetForm(state: AuthUiState, vm: AuthViewModel) {
    UsernameField(state.username, vm::setUsername, showHint = false)
    Column {
        Text("کد بازیابی", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = state.recoveryInput,
            onValueChange = vm::setRecoveryInput,
            singleLine = true,
            placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    PasswordField(
        label = "گذرواژهٔ تازه",
        value = state.password,
        onChange = vm::setPassword,
        newPassword = true,
        showHint = true,
        imeAction = ImeAction.Done,
        onDone = vm::submit,
    )
    if (state.error.isNotBlank()) {
        Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
    Button(onClick = vm::submit, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
        Text("تغییر گذرواژه")
    }
    TextButton(onClick = vm::backToLogin, modifier = Modifier.fillMaxWidth()) { Text("بازگشت به ورود") }
}

@Composable
private fun UsernameField(value: String, onChange: (String) -> Unit, showHint: Boolean) {
    Column {
        Text("نام کاربری", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showHint) {
            Text(
                "حروف لاتین، رقم یا _ (۳ تا ۳۲ نویسه)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    newPassword: Boolean,
    showHint: Boolean,
    imeAction: ImeAction,
    onDone: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "پنهان‌کردن گذرواژه" else "نمایش گذرواژه",
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showHint) {
            Text(
                "دست‌کم ۸ نویسه",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun RecoveryCodePanel(code: String, doneLabel: String, onDone: () -> Unit) {
    var copied by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    Text(
        "اگر گذرواژه را فراموش کنید، با نام کاربری و همین کد می‌توانید گذرواژهٔ تازه بگذارید. این کد فقط همین یک بار نشان داده می‌شود؛ آن را جایی امن نگه دارید.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            code,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = {
            copyText(code)
            copied = true
        }) {
            Text(if (copied) "کپی شد" else "کپی")
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = saved, onCheckedChange = { saved = it })
        Text("کد را جایی امن نگه داشتم", style = MaterialTheme.typography.bodyMedium)
    }
    Button(onClick = onDone, enabled = saved, modifier = Modifier.fillMaxWidth()) { Text(doneLabel) }
}
