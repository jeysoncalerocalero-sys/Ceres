package com.venice.ceres

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

    private var hasAccess by mutableStateOf(false)

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasAccess = Patcher.hasAccess() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasAccess = Patcher.hasAccess()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { Screen(hasAccess) { permLauncher.launch(allFilesIntent()) } }
            }
        }
    }

    private fun allFilesIntent(): Intent {
        val i = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        return i.setData(Uri.parse("package:$packageName"))
    }
}

@Composable
private fun Screen(hasAccess: Boolean, askPermission: () -> Unit) {
    val ctx = LocalContext.current
    var features by remember { mutableStateOf(Config.allMax()) }
    var packages by remember { mutableStateOf(Patcher.installedPackages()) }
    var selected by remember { mutableStateOf(packages.firstOrNull()) }
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

        if (!hasAccess) {
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PASO 1: dar permiso", fontWeight = FontWeight.Bold)
                    Text("Necesita Acceso a todos los archivos para escribir en la carpeta de Free Fire.",
                        fontSize = 13.sp)
                    Button(onClick = askPermission) { Text("DAR PERMISO") }
                }
            }
        } else {
            Text("OK Permiso concedido", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }

        HorizontalDivider()

        Text("PASO 2: juego detectado", fontWeight = FontWeight.Bold)
        if (packages.isEmpty()) {
            Text("No encontre Free Fire. Abrelo una vez y vuelve.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        } else {
            packages.forEach { pkg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == pkg, onClick = { selected = pkg })
                    Text(if (pkg.endsWith("max")) "Free Fire MAX" else "Free Fire")
                }
            }
        }

        HorizontalDivider()

        Text("PASO 3: todo al maximo", fontWeight = FontWeight.Bold)
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { features = Config.allMax() }
        ) { Text("PONER TODO AL MAXIMO", fontWeight = FontWeight.Bold) }

        features.forEach { f ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.name, fontWeight = FontWeight.SemiBold)
                    Text(f.desc, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = f.enabled,
                    onCheckedChange = { v ->
                        features = features.map {
                            if (it.name == f.name) it.copy(enabled = v) else it
                        }
                    }
                )
            }
        }

        HorizontalDivider()

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val pkg = selected
                if (pkg == null) Toast.makeText(ctx, "Elige un juego", Toast.LENGTH_SHORT).show()
                else {
                    log = Patcher.apply(pkg, Config.buildIni(features))
                    Toast.makeText(ctx, "Aplicado. Reinicia Free Fire.", Toast.LENGTH_LONG).show()
                }
            }
        ) { Text("APLICAR", fontWeight = FontWeight.Bold) }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { selected?.let { log = Patcher.restore(it) } }
        ) { Text("RESTAURAR ORIGINAL") }

        Text(log, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
