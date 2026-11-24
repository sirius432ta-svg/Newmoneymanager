package com.example.moneymanager

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val Bg = Color(0xFF111827)
private val CardBg = Color(0xFF1F2937)
private val Blue = Color(0xFF60A5FA)
private val Green = Color(0xFF34D399)
private val Red = Color(0xFFF87171)
private val Muted = Color(0xFF9CA3AF)

enum class AppLang { EN, RU, HE }
enum class Page { HOME, HISTORY, STATS, GOALS, SETTINGS }
data class Tx(val id: Long, val title: String, val amountAgorot: Long, val income: Boolean, val category: String, val date: Long)

class FinanceDb(context: Context) : SQLiteOpenHelper(context, "money_manager.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            amount_agorot INTEGER NOT NULL,
            income INTEGER NOT NULL,
            category TEXT NOT NULL,
            date INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE goals (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            target_agorot INTEGER NOT NULL,
            saved_agorot INTEGER NOT NULL DEFAULT 0
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun addTx(title: String, amount: Long, income: Boolean, category: String) {
        val v = ContentValues().apply {
            put("title", title); put("amount_agorot", amount); put("income", if (income) 1 else 0)
            put("category", category); put("date", System.currentTimeMillis())
        }
        writableDatabase.insert("transactions", null, v)
    }
    fun allTx(): List<Tx> {
        val out = mutableListOf<Tx>()
        readableDatabase.rawQuery("SELECT id,title,amount_agorot,income,category,date FROM transactions ORDER BY date DESC", null).use { c ->
            while (c.moveToNext()) out += Tx(c.getLong(0), c.getString(1), c.getLong(2), c.getInt(3) == 1, c.getString(4), c.getLong(5))
        }
        return out
    }
    fun totals(): Pair<Long, Long> {
        var inc = 0L; var exp = 0L
        allTx().forEach { if (it.income) inc += it.amountAgorot else exp += it.amountAgorot }
        return inc to exp
    }
    fun addGoal(title: String, target: Long) {
        val v = ContentValues().apply { put("title", title); put("target_agorot", target); put("saved_agorot", 0L) }
        writableDatabase.insert("goals", null, v)
    }
    fun goals(): List<Triple<String, Long, Long>> {
        val out = mutableListOf<Triple<String, Long, Long>>()
        readableDatabase.rawQuery("SELECT title,target_agorot,saved_agorot FROM goals ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out += Triple(c.getString(0), c.getLong(1), c.getLong(2))
        }
        return out
    }
}

private val words = mapOf(
    "app" to listOf("Money Manager", "Менеджер денег", "מנהל כספים"),
    "home" to listOf("Home", "Главная", "בית"),
    "history" to listOf("History", "История", "היסטוריה"),
    "stats" to listOf("Statistics", "Статистика", "סטטיסטיקה"),
    "goals" to listOf("Goals", "Цели", "יעדים"),
    "settings" to listOf("Settings", "Настройки", "הגדרות"),
    "balance" to listOf("Total balance", "Общий баланс", "יתרה כוללת"),
    "income" to listOf("Income", "Доходы", "הכנסות"),
    "expenses" to listOf("Expenses", "Расходы", "הוצאות"),
    "add" to listOf("Add transaction", "Добавить операцию", "הוסף פעולה"),
    "addIncome" to listOf("Add income", "Добавить доход", "הוסף הכנסה"),
    "addExpense" to listOf("Add expense", "Добавить расход", "הוסף הוצאה"),
    "amount" to listOf("Amount (₪)", "Сумма (₪)", "סכום (₪)"),
    "description" to listOf("Description", "Описание", "תיאור"),
    "category" to listOf("Category", "Категория", "קטגוריה"),
    "save" to listOf("Save", "Сохранить", "שמור"),
    "cancel" to listOf("Cancel", "Отмена", "ביטול"),
    "noTx" to listOf("No transactions yet", "Пока нет операций", "אין פעולות עדיין"),
    "language" to listOf("Language", "Язык", "שפה"),
    "monthlyBudget" to listOf("Monthly budget", "Бюджет на месяц", "תקציב חודשי"),
    "coming" to listOf("This section is ready for the next development step.", "Этот раздел будет расширен на следующем этапе.", "החלק הזה יורחב בשלב הפיתוח הבא."),
    "newGoal" to listOf("New savings goal", "Новая цель накоплений", "יעד חיסכון חדש"),
    "target" to listOf("Target amount (₪)", "Целевая сумма (₪)", "סכום יעד (₪)")
)
private fun tr(key: String, lang: AppLang) = words[key]?.get(lang.ordinal) ?: key

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FinanceDb(this)
        setContent { MoneyManagerApp(db) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyManagerApp(db: FinanceDb) {
    var lang by remember { mutableStateOf(AppLang.EN) }
    var page by remember { mutableStateOf(Page.HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }
    val rtl = lang == AppLang.HE
    val totals = remember(refresh) { db.totals() }
    val transactions = remember(refresh) { db.allTx() }
    val goals = remember(refresh) { db.goals() }
    val balance = totals.first - totals.second

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg, surface = CardBg, primary = Blue,
            onBackground = Color.White, onSurface = Color.White
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Scaffold(
                containerColor = Bg,
                topBar = {
                    TopAppBar(
                        title = { Text(tr("app", lang), fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg, titleContentColor = Color.White)
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = CardBg, contentColor = Blue) {
                        val entries = listOf(
                            Page.HOME to Icons.Default.Home,
                            Page.HISTORY to Icons.Default.ReceiptLong,
                            Page.STATS to Icons.Default.BarChart,
                            Page.GOALS to Icons.Default.Flag,
                            Page.SETTINGS to Icons.Default.Settings
                        )
                        entries.forEach { (p, icon) ->
                            NavigationBarItem(
                                selected = page == p,
                                onClick = { page = p },
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(tr(p.name.lowercase(), lang), fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Blue, indicatorColor = Color(0xFF263B56), unselectedIconColor = Muted, selectedTextColor = Blue, unselectedTextColor = Muted)
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (page == Page.HOME || page == Page.HISTORY) {
                        FloatingActionButton(onClick = { showAdd = true }, containerColor = Blue, contentColor = Color.White) {
                            Icon(Icons.Default.Add, contentDescription = tr("add", lang))
                        }
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                    when (page) {
                        Page.HOME -> Dashboard(lang, balance, totals, transactions.take(5), onAdd = { showAdd = true })
                        Page.HISTORY -> History(lang, transactions)
                        Page.STATS -> Statistics(lang, totals, transactions)
                        Page.GOALS -> Goals(lang, goals, onAdd = { showGoal = true })
                        Page.SETTINGS -> Settings(lang, onLang = { lang = it })
                    }
                }
            }
            if (showAdd) AddTransactionDialog(lang, onDismiss = { showAdd = false }, onSave = { title, amount, income, category ->
                db.addTx(title, amount, income, category); refresh++; showAdd = false
            })
            if (showGoal) AddGoalDialog(lang, onDismiss = { showGoal = false }, onSave = { title, target ->
                db.addGoal(title, target); refresh++; showGoal = false
            })
        }
    }
}

@Composable
fun MoneyCard(label: String, value: String, color: Color = Color.White) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(value, color = color, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        }
    }
}
private fun ils(agorot: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale("he", "IL"))
    nf.minimumFractionDigits = 2; nf.maximumFractionDigits = 2
    return "₪" + nf.format(agorot / 100.0)
}

@Composable
fun Dashboard(lang: AppLang, balance: Long, totals: Pair<Long, Long>, recent: List<Tx>, onAdd: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    MoneyCard(tr("balance", lang), ils(balance), Blue)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) { MoneyCard(tr("income", lang), ils(totals.first), Green) }
        Box(Modifier.weight(1f)) { MoneyCard(tr("expenses", lang), ils(totals.second), Red) }
    }
    Spacer(Modifier.height(20.dp))
    Text(tr("history", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(8.dp))
    if (recent.isEmpty()) Text(tr("noTx", lang), color = Muted, modifier = Modifier.padding(vertical = 20.dp))
    else LazyColumn { items(recent) { TransactionRow(it) } }
}

@Composable
fun TransactionRow(tx: Tx) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (tx.income) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = if (tx.income) Green else Red)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(tx.category, color = Muted, fontSize = 12.sp)
        }
        Text((if (tx.income) "+" else "−") + ils(tx.amountAgorot), color = if (tx.income) Green else Red, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = Color(0xFF374151))
}

@Composable
fun History(lang: AppLang, transactions: List<Tx>) {
    Spacer(Modifier.height(12.dp))
    Text(tr("history", lang), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    if (transactions.isEmpty()) Text(tr("noTx", lang), color = Muted, modifier = Modifier.padding(16.dp))
    else LazyColumn { items(transactions) { TransactionRow(it) } }
}

@Composable
fun Statistics(lang: AppLang, totals: Pair<Long, Long>, transactions: List<Tx>) {
    Spacer(Modifier.height(16.dp))
    Text(tr("stats", lang), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    MoneyCard(tr("income", lang), ils(totals.first), Green)
    Spacer(Modifier.height(10.dp))
    MoneyCard(tr("expenses", lang), ils(totals.second), Red)
    Spacer(Modifier.height(16.dp))
    Text(if (transactions.isEmpty()) tr("noTx", lang) else tr("coming", lang), color = Muted)
    // The full monthly/category analytics chart is a planned next implementation stage.
}

@Composable
fun Goals(lang: AppLang, goals: List<Triple<String, Long, Long>>, onAdd: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(tr("goals", lang), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = onAdd) { Icon(Icons.Default.Add, null); Text(tr("newGoal", lang)) }
    }
    if (goals.isEmpty()) Text(tr("coming", lang), color = Muted)
    goals.forEach { (name, target, saved) ->
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${ils(saved)} / ${ils(target)}", color = Muted)
                LinearProgressIndicator(progress = { if (target > 0) (saved.toFloat() / target).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = Green)
            }
        }
    }
}

@Composable
fun Settings(lang: AppLang, onLang: (AppLang) -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text(tr("settings", lang), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    Text(tr("language", lang), color = Muted)
    listOf(AppLang.EN to "English", AppLang.RU to "Русский", AppLang.HE to "עברית").forEach { (l, label) ->
        Row(Modifier.fillMaxWidth().clickable { onLang(l) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = lang == l, onClick = { onLang(l) })
            Text(label, color = Color.White)
        }
    }
    Text("Currency: ILS (₪)", color = Muted, modifier = Modifier.padding(top = 16.dp))
    Text("Data is stored locally on this device.", color = Muted, fontSize = 12.sp)
}

@Composable
fun AddTransactionDialog(lang: AppLang, onDismiss: () -> Unit, onSave: (String, Long, Boolean, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var income by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text(tr("add", lang), color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !income, onClick = { income = false }, label = { Text(tr("addExpense", lang)) })
                    FilterChip(selected = income, onClick = { income = true }, label = { Text(tr("addIncome", lang)) })
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(tr("description", lang)) }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, label = { Text(tr("amount", lang)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(tr("category", lang)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val shekels = amount.replace(',', '.').toDoubleOrNull()
                if (title.isNotBlank() && shekels != null && shekels > 0) onSave(title.trim(), (shekels * 100).toLong(), income, category.ifBlank { "Other" })
            }) { Text(tr("save", lang), color = Blue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("cancel", lang), color = Muted) } }
    )
}

@Composable
fun AddGoalDialog(lang: AppLang, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBg,
        title = { Text(tr("newGoal", lang), color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(tr("description", lang)) })
                OutlinedTextField(value = target, onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, label = { Text(tr("target", lang)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = target.replace(',', '.').toDoubleOrNull()
                if (title.isNotBlank() && v != null && v > 0) onSave(title.trim(), (v * 100).toLong())
            }) { Text(tr("save", lang), color = Blue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("cancel", lang), color = Muted) } }
    )
}
