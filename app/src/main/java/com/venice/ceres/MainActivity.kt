package com.venice.ceres

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private var tree by mutableStateOf<Uri?>(null)
    private var folder by mutableStateOf("")

    private val picker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
            }
            Patcher.saveTree(this, uri)
            tree = uri
            folder = Patcher.treeName(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = Patcher.loadTree(this)
        tree = saved
        folder = saved?.let { Patcher.treeName(it) } ?: ""

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Screen(
                        tree = tree,
                        folder = folder,
                        pickFolder = { picker.launch(null) },
                        forget = {
                            Patcher.clearTree(this@MainActivity)
                            tree = null
                            folder = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Screen(
    tree: Uri?,
    folder: String,
    pickFolder: () -> Unit,
    forget: () -> Unit
) {
    val ctx = LocalContext.current
    var features by remember { mutableStateOf(Config.allMax()) }
    var log by remember { mutableStateOf("Listo.") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "CERES", fontSize = 30.sp, fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Todo al maximo · Headshot Lock · Aim Lock descarado",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        Text("PASO 1 · elegir la carpeta de Free Fire", fontWeight = FontWeight.Bold)
        Text(
            "Toca ELEGIR CARPETA. En el selector navega a: Android > data > com.dts.freefireth. " +
                "Al final toca USAR ESTA CARPETA y luego PERMITIR.",
            fontSize = 13.sp
        )
        Button(onClick = pickFolder, modifier = Modifier.fillMaxWidth()) {
            Text("ELEGIR CARPETA", fontWeight = FontWeight.Bold)
        }
        Text(
            if (tree == null) "Sin carpeta elegida" else "Carpeta lista: " + folder,
            fontSize = 13.sp,
            color = if (tree == null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
        if (tree != null) {
            OutlinedButton(onClick = forget, modifier = Modifier.fillMaxWidth()) {
                Text("CAMBIAR CARPETA")
            }
        }

        HorizontalDivider()

        Text("PASO 2 · poderes (todos al maximo)", fontWeight = FontWeight.Bold)
        Button(onClick = { features = Config.allMax() }, modifier = Modifier.fillMaxWidth()) {
            Text("PONER TODO AL MAXIMO", fontWeight = FontWeight.Bold)
        }
        features.forEach { f ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.name, fontWeight = FontWeight.SemiBold)
                    Text(f.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = f.enabled,
                    onCheckedChange = { v ->
                        features = features.map { if (it.name == f.name) it.copy(enabled = v) else it }
                    }
                )
            }
        }

        HorizontalDivider()

        Text("PASO 3 · aplicar", fontWeight = FontWeight.Bold)
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val t = tree
                if (t == null) {
                    Toast.makeText(ctx, "Primero elige la carpeta", Toast.LENGTH_SHORT).show()
                } else {
                    log = Patcher.apply(ctx, t, Config.buildIni(features))
                    Toast.makeText(ctx, "Aplicado. Cierra y abre Free Fire.", Toast.LENGTH_LONG).show()
                }
            }
        ) { Text("APLICAR TODO", fontWeight = FontWeight.Bold) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    val t = tree
                    log = if (t == null) "Elige la carpeta primero" else Patcher.verify(ctx, t)
                }
            ) { Text("COMPROBAR") }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    val t = tree
                    log = if (t == null) "Elige la carpeta primero" else Patcher.restore(ctx, t)
                }
            ) { Text("RESTAURAR") }
        }

        HorizontalDivider()

        Text("Registro", fontWeight = FontWeight.Bold)
        Text(log, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }
}
