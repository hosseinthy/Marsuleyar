package com.morsulehyar.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ParcelItem(
    val id: Long,
    val tracking: String,
    val recipient: String,
    val status: String
)

private class ParcelDatabase(context: Context) :
    SQLiteOpenHelper(context, "morsulehyar.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE parcels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tracking TEXT NOT NULL,
                recipient TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'ثبت‌شده'
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {}

    fun addParcel(tracking: String, recipient: String) {
        val values = ContentValues().apply {
            put("tracking", tracking.trim())
            put("recipient", recipient.trim())
            put("status", "ثبت‌شده")
        }
        writableDatabase.insertOrThrow("parcels", null, values)
    }

    fun getParcels(search: String = ""): List<ParcelItem> {
        val result = mutableListOf<ParcelItem>()
        val pattern = "%${search.trim()}%"

        readableDatabase.query(
            "parcels",
            arrayOf("id", "tracking", "recipient", "status"),
            "tracking LIKE ? OR recipient LIKE ?",
            arrayOf(pattern, pattern),
            null, null, "id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += ParcelItem(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
                )
            }
        }
        return result
    }

    fun updateStatus(id: Long, status: String) {
        val values = ContentValues().apply { put("status", status) }
        writableDatabase.update(
            "parcels", values, "id = ?", arrayOf(id.toString())
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = ParcelDatabase(applicationContext)
        setContent {
            MaterialTheme {
                ParcelScreen(database)
            }
        }
    }
}

@Composable
private fun ParcelScreen(database: ParcelDatabase) {
    var search by remember { mutableStateOf("") }
    var parcels by remember { mutableStateOf(database.getParcels()) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun refresh() {
        parcels = database.getParcels(search)
    }

    val statuses = listOf(
        "ثبت‌شده", "در حال ارسال", "تحویل‌شده", "مرجوع‌شده"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp)
    ) {
        Text("مرسوله‌یار", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("مدیریت مرسوله‌ها")
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it; refresh() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("جست‌وجوی کد یا گیرنده") },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("افزودن مرسوله")
        }

        Spacer(Modifier.height(12.dp))
        Text("تعداد مرسوله‌ها: ${parcels.size}")

        if (parcels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("مرسوله‌ای پیدا نشد.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(parcels, key = { it.id }) { parcel ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                parcel.recipient,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("کد مرسوله: ${parcel.tracking}")
                            Text("وضعیت: ${parcel.status}")

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(onClick = {
                                val index = statuses.indexOf(parcel.status)
                                    .coerceAtLeast(0)
                                database.updateStatus(
                                    parcel.id,
                                    statuses[(index + 1) % statuses.size]
                                )
                                refresh()
                            }) {
                                Text("تغییر وضعیت")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var tracking by remember { mutableStateOf("") }
        var recipient by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("مرسوله جدید") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tracking,
                        onValueChange = { tracking = it },
                        label = { Text("کد مرسوله") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text("نام گیرنده") },
                        singleLine = true
                    )
                    if (error.isNotBlank()) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tracking.isBlank() || recipient.isBlank()) {
                        error = "کد مرسوله و نام گیرنده را وارد کنید."
                    } else {
                        database.addParcel(tracking, recipient)
                        search = ""
                        parcels = database.getParcels()
                        showAddDialog = false
                    }
                }) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
