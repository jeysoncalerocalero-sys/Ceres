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

    private var treeUri by mutableStateOf<Uri?>(null)

    private val picker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) { }
            Patcher.saveUri(this, uri)
            treeUri = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        treeUri = Patcher.loadUri(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { Screen(treeUri) { picker.launch(null) } }
            }
        }
    }
}

@Composable
private fun Screen(treeUri: Uri?, pickFolder: () -> Unit) {
    val ctx = LocalContext.current
    var features by remember { mutableStateOf(Config.allMax()) }
    var log by remember { mutableStateOf("Listo.") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("CERES", fontSize = 30.sp, fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary)
        Text("Headshot Lock · Auto Headshot · Aim Lock descarado",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider()

        Text("PASO 1: elegir carpeta de Free Fire", fontWeight = FontWeight.Bold)
        Text("Toca el boton. En la pantalla que sale navega a: Android > data > com.dts.freefireth  y toca USAR ESTA CARPETA.",
            fontSize = 13.sp)
        Button(onClick = pickFolder, modifier = Modifier.fillMaxWidth()) {
            Text("ELEGIR CARPETA", fontWeight = FontWeight.Bold)
        }
        Text(
            if (treeUri == null) "Ninguna carpeta elegida aun"
            else "Carpeta elegida correctamente",
            fontSize = 13.sp,
            color = if (treeUri == null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )

        HorizontalDivider()

        Text("PASO 2: todo al maximo", fontWeight = FontWeight.Bold)
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

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val u = treeUri
                if (u == null) {
                    Toast.makeText(ctx, "Primero elige la carpeta", Toast.LENGTH_SHORT).show()
                } else {
                    log = Patcher.apply(ctx, u, Config.buildIni(features))
                    Toast.makeText(ctx, "Aplicado", Toast.LENGTH_SHORT).show()
                }
            }
        ) { Text("APLICAR TODO", fontWeight = FontWeight.Bold) }

        HorizontalDivider()

        Text("Registro", fontWeight = FontWeight.Bold)
        Text(log, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
