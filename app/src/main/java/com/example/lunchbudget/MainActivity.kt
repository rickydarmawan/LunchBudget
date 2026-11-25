package com.example.lunchbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Navy = Color(0xFF172D45)
private val Green = Color(0xFF159B68)
private val Pale = Color(0xFFF3F6FA)
private val Ink = Color(0xFF1D2939)
private val Muted = Color(0xFF667085)
private val Red = Color(0xFFD64545)

data class MonthConfig(val budget: Long, val workdays: Int)
data class Expense(val date: String, val amount: Long)

class LocalStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("lunch_budget_local", 0)

    fun config(month: String): MonthConfig {
        val raw = prefs.getString("config_$month", null) ?: return MonthConfig(500_000, 20)
        return try {
            val j = JSONObject(raw)
            MonthConfig(j.optLong("budget", 500_000), j.optInt("workdays", 20))
        } catch (_: Exception) { MonthConfig(500_000, 20) }
    }

    fun saveConfig(month: String, config: MonthConfig) {
        prefs.edit().putString("config_$month",
            JSONObject().put("budget", config.budget).put("workdays", config.workdays).toString()
        ).apply()
    }

    fun expenses(month: String): List<Expense> {
        val raw = prefs.getString("expenses_$month", "{}") ?: "{}"
        return try {
            val j = JSONObject(raw)
            j.keys().asSequence().map { Expense(it, j.optLong(it)) }.sortedByDescending { it.date }.toList()
        } catch (_: Exception) { emptyList() }
    }

    fun saveExpense(month: String, date: String, amount: Long) {
        val j = try { JSONObject(prefs.getString("expenses_$month", "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        j.put(date, amount)
        prefs.edit().putString("expenses_$month", j.toString()).apply()
    }

    fun deleteExpense(month: String, date: String) {
        val j = try { JSONObject(prefs.getString("expenses_$month", "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        j.remove(date)
        prefs.edit().putString("expenses_$month", j.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LocalStore(this)
        setContent { LunchBudgetApp(store) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunchBudgetApp(store: LocalStore) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var refresh by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf("home") }
    var showSettings by remember { mutableStateOf(false) }
    var showEntry by remember { mutableStateOf(false) }

    val monthKey = month.toString()
    val config = remember(monthKey, refresh) { store.config(monthKey) }
    val expenses = remember(monthKey, refresh) { store.expenses(monthKey) }
    val today = LocalDate.now()
    val todayKey = today.toString()
    val todayExpense = expenses.firstOrNull { it.date == todayKey }
    val totalSpent = expenses.sumOf { it.amount }
    val remainingMoney = config.budget - totalSpent
    val remainingDays = (config.workdays - expenses.size).coerceAtLeast(0)
    val daysForToday = if (todayExpense == null) remainingDays.coerceAtLeast(1) else (remainingDays + 1).coerceAtLeast(1)
    val todayAllowance = if (todayExpense == null) (remainingMoney.coerceAtLeast(0) / daysForToday) else {
        val beforeToday = remainingMoney + todayExpense.amount
        beforeToday.coerceAtLeast(0) / daysForToday
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy, secondary = Green, background = Pale, surface = Color.White,
            onBackground = Ink, onSurface = Ink
        )
    ) {
        Scaffold(
            containerColor = Pale,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("LunchBudget", fontWeight = FontWeight.Bold, color = Navy, fontSize = 21.sp)
                            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))),
                                color = Muted, fontSize = 12.sp)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = Navy)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Pale)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = tab == "home", onClick = { tab = "home" },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) }, label = { Text("Dashboard") })
                    NavigationBarItem(selected = tab == "history", onClick = { tab = "history" },
                        icon = { Icon(Icons.Default.History, contentDescription = null) }, label = { Text("Riwayat") })
                }
            },
            floatingActionButton = {
                if (tab == "home") {
                    FloatingActionButton(onClick = { showEntry = true }, containerColor = Green, contentColor = Color.White) {
                        Icon(Icons.Default.Add, contentDescription = "Catat makan siang")
                    }
                }
            }
        ) { padding ->
            if (tab == "home") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("TOTAL BUDGET BULAN INI", color = Color(0xFFB8C9D9), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(rupiah(config.budget), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                val progress = if (config.budget > 0) (totalSpent.toFloat() / config.budget).coerceIn(0f, 1f) else 0f
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = Color(0xFF55D69A), trackColor = Color(0xFF40556A))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Terpakai", color = Color(0xFFB8C9D9), fontSize = 11.sp)
                                        Text(rupiah(totalSpent), color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Sisa budget", color = Color(0xFFB8C9D9), fontSize = 11.sp)
                                        Text(rupiah(remainingMoney), color = if (remainingMoney >= 0) Color(0xFF55D69A) else Color(0xFFFF8D8D),
                                            fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Hari kerja", "${config.workdays}", "hari", Modifier.weight(1f))
                            StatCard("Tersisa", "$remainingDays", "hari belum dicatat", Modifier.weight(1f))
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("BUDGET HARI INI", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (todayExpense == null) rupiah(todayAllowance) else rupiah(todayExpense.amount),
                                    color = Navy, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                if (todayExpense == null) {
                                    Text("Dana tersedia hari ini berdasarkan sisa budget dan hari kerja.", color = Muted, fontSize = 12.sp)
                                    Button(onClick = { showEntry = true }, modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(14.dp)) {
                                        Text("Catat makan siang")
                                    }
                                } else {
                                    val diff = todayAllowance - todayExpense.amount
                                    val status = when { diff > 0 -> "Hemat ${rupiah(diff)}"; diff == 0L -> "Pas dengan jatah"; else -> "Melebihi ${rupiah(-diff)}" }
                                    Text(status, color = if (diff >= 0) Green else Red, fontWeight = FontWeight.SemiBold)
                                    Text("Pengeluaran hari ini sudah tercatat.", color = Muted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    item {
                        Text("Ringkasan bulan ini", color = Navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Rata-rata / catatan", if (expenses.isEmpty()) "—" else rupiah(totalSpent / expenses.size), "",
                                Modifier.weight(1f))
                            StatCard("Hari tercatat", "${expenses.size}", "hari", Modifier.weight(1f))
                        }
                    }
                    item {
                        Text("Catatan terbaru", color = Navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    items(expenses.take(3)) { expense -> ExpenseRow(expense) }
                    if (expenses.isEmpty()) {
                        item { Text("Belum ada pengeluaran. Tekan tombol + untuk mulai mencatat.", color = Muted, fontSize = 13.sp) }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("Riwayat pengeluaran", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Navy) }
                    item { Text("Catatan makan siang bulan ${month.format(DateTimeFormatter.ofPattern("MMMM", Locale("id", "ID")))}", color = Muted) }
                    if (expenses.isEmpty()) item { Text("Belum ada riwayat untuk bulan ini.", color = Muted) }
                    items(expenses) { expense ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(formatDate(expense.date), fontWeight = FontWeight.SemiBold, color = Ink)
                                    Text("Makan siang", color = Muted, fontSize = 12.sp)
                                }
                                Text(rupiah(expense.amount), color = Navy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            SettingsDialog(config, onDismiss = { showSettings = false }, onSave = { budget, days ->
                store.saveConfig(monthKey, MonthConfig(budget, days))
                refresh++
                showSettings = false
            })
        }
        if (showEntry) {
            EntryDialog(todayKey, todayExpense?.amount, onDismiss = { showEntry = false }, onSave = { amount ->
                store.saveExpense(monthKey, todayKey, amount)
                refresh++
                showEntry = false
            }, onDelete = {
                store.deleteExpense(monthKey, todayKey)
                refresh++
                showEntry = false
            })
        }
    }
}

@Composable
fun StatCard(title: String, value: String, suffix: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Muted, fontSize = 11.sp)
            Text(value, color = Navy, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (suffix.isNotBlank()) Text(suffix, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(formatDate(expense.date), color = Ink, fontWeight = FontWeight.SemiBold)
                Text("Makan siang", color = Muted, fontSize = 12.sp)
            }
            Text(rupiah(expense.amount), color = Navy, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsDialog(config: MonthConfig, onDismiss: () -> Unit, onSave: (Long, Int) -> Unit) {
    var budget by remember { mutableStateOf(config.budget.toString()) }
    var days by remember { mutableStateOf(config.workdays.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Atur budget dan jumlah hari kerja untuk bulan ini.", color = Muted, fontSize = 13.sp)
                OutlinedTextField(budget, { budget = it.filter(Char::isDigit) }, label = { Text("Budget bulanan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(days, { days = it.filter(Char::isDigit) }, label = { Text("Jumlah hari kerja") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val b = budget.toLongOrNull() ?: 0
                val d = days.toIntOrNull() ?: 0
                if (b > 0 && d > 0) onSave(b, d)
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun EntryDialog(date: String, existing: Long?, onDismiss: () -> Unit, onSave: (Long) -> Unit, onDelete: () -> Unit) {
    var amount by remember { mutableStateOf(existing?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Catat makan siang" else "Ubah pengeluaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatDate(date), color = Muted, fontSize = 13.sp)
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit) },
                    label = { Text("Pengeluaran (Rp)") },
                    placeholder = { Text("Contoh: 20000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { amount.toLongOrNull()?.takeIf { it >= 0 }?.let(onSave) }) { Text("Simpan") }
        },
        dismissButton = {
            Row {
                if (existing != null) TextButton(onClick = onDelete) { Text("Hapus") }
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        }
    )
}

fun rupiah(value: Long): String {
    val f = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    f.maximumFractionDigits = 0
    f.minimumFractionDigits = 0
    return f.format(value).replace("Rp", "Rp ")
}

fun formatDate(raw: String): String = try {
    LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale("id", "ID")))
} catch (_: Exception) { raw }
