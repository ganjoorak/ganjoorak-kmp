package net.ganjoorak.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.account.AccountException
import net.ganjoorak.app.data.account.AuthValidation
import net.ganjoorak.app.data.account.Quote
import net.ganjoorak.app.data.account.QuotesPage
import net.ganjoorak.app.data.account.formatStoryTime
import net.ganjoorak.app.data.account.poemRef
import net.ganjoorak.app.ui.components.BorderedCard
import net.ganjoorak.app.ui.theme.LocalPoemStyle

/**
 * The signed-in reader's own fragments, newest first, with delete confirmation
 * matching the website's QuotesPanel on «حساب من».
 */
fun LazyListScope.ownerQuotesSection(
    username: String,
    onOpenPoem: (PoemRef) -> Unit,
) {
    item(key = "quotes-panel") {
        OwnerQuotesList(username, onOpenPoem)
    }
}

@Composable
private fun OwnerQuotesList(username: String, onOpenPoem: (PoemRef) -> Unit) {
    val api = AppGraph.instance.accounts.api
    var quotes by remember(username) { mutableStateOf<List<Quote>>(emptyList()) }
    var next by remember(username) { mutableStateOf<Long?>(null) }
    var loaded by remember(username) { mutableStateOf(false) }
    var loading by remember(username) { mutableStateOf(false) }
    var error by remember(username) { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load(before: Long? = null) {
        if (loading) return
        loading = true
        error = ""
        scope.launch {
            try {
                val page: QuotesPage = api.userQuotes(username, before)
                quotes = if (before == null) page.quotes else quotes + page.quotes.filter { added -> quotes.none { it.id == added.id } }
                next = page.next
                loaded = true
            } catch (caught: AccountException) {
                if (before == null && caught.status == 404) {
                    loaded = true
                } else {
                    error = caught.message?.takeIf { it.isNotBlank() } ?: AuthValidation.NetworkError
                }
            } catch (_: Exception) {
                error = AuthValidation.NetworkError
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(username) { load() }

    Column(
        Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!loaded && error.isEmpty()) {
            Text("در حال بارگذاری…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (quotes.isEmpty()) {
            Text(
                "هنوز تکه‌ای نگذاشته‌اید. در صفحهٔ شعر چند مصراع را انتخاب کنید و «در صفحهٔ من» را بزنید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            quotes.forEach { quote ->
                QuoteCard(
                    quote = quote,
                    canDelete = true,
                    onOpen = { quote.poemRef()?.let(onOpenPoem) },
                    onDelete = {
                        try {
                            api.deleteQuote(quote.id)
                            quotes = quotes.filter { it.id != quote.id }
                            error = ""
                        } catch (caught: AccountException) {
                            error = caught.message?.takeIf { it.isNotBlank() } ?: "خطایی رخ داد."
                        }
                    },
                )
            }
        }
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (next != null) {
            OutlinedButton(onClick = { load(next) }, enabled = !loading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("نمایش بیشتر")
            }
        }
    }
}

@Composable
private fun QuoteCard(quote: Quote, canDelete: Boolean, onOpen: () -> Unit, onDelete: suspend () -> Unit) {
    var confirming by remember(quote.id) { mutableStateOf(false) }
    var busy by remember(quote.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val poemStyle = LocalPoemStyle.current
    BorderedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                quote.lines.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = poemStyle.fontFamily),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    TextButton(onClick = onOpen, enabled = quote.poemRef() != null) {
                        Text("${quote.poetName} · ${quote.poemTitle}", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        formatStoryTime(quote.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (canDelete) {
                    if (confirming) {
                        Button(
                            onClick = {
                                busy = true
                                scope.launch {
                                    onDelete()
                                    busy = false
                                    confirming = false
                                }
                            },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("حذف") }
                        TextButton(onClick = { confirming = false }, enabled = !busy) { Text("انصراف") }
                    } else {
                        IconButton(onClick = { confirming = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "حذف این تکه")
                        }
                    }
                }
            }
        }
    }
}
