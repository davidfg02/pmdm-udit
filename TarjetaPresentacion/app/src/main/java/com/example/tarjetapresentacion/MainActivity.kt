package com.example.tarjetapresentacion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tarjetapresentacion.ui.theme.TarjetaPresentacionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Aplicamos el tema de colores del proyecto a todo lo de dentro
            TarjetaPresentacionTheme {
                // Surface = el "lienzo" de fondo que ocupa toda la pantalla
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Aquí llamamos a NUESTRA función, la que dibuja la tarjeta
                    TarjetaPresentacion()
                }
            }
        }
    }
}

@Composable
fun TarjetaPresentacion() {
    // LocalContext: así un Composable "pide prestado" el contexto de Android.
    // Lo necesitamos para abrir el navegador y guardar el archivo.
    val context = LocalContext.current

    // Selector de archivos del sistema: el usuario elige dónde guardar el CV.
    // Funciona en cualquier versión de Android y no necesita permisos.
    val guardarCv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                context.resources.openRawResource(R.raw.cv_david).use { input ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "CV guardado", Toast.LENGTH_SHORT).show()
        }
    }

    // COLUMN: apila los elementos de arriba a abajo (como un flexbox vertical)
    Column(
        modifier = Modifier
            .fillMaxSize() // ocupa toda la pantalla
            .padding(16.dp), // margen para que nada toque los bordes
        horizontalAlignment = Alignment.CenterHorizontally, // centra en el eje X
        verticalArrangement = Arrangement.Center // centra en el eje Y
    ) {

        // IMAGE: la foto de perfil requiere un archivo "foto_perfil" dentro de res/drawable
        Image(
            painter = painterResource(id = R.drawable.foto_perfil),
            contentDescription = "Foto de perfil de usuario",
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        // Hueco vacío entre la imagen y el texto
        Spacer(modifier = Modifier.height(24.dp))

        // TEXT: nombre
        Text(
            text = "David Fraile",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        // TEXT: rol o profesión
        Text(
            text = "Estudiante DAM",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        // Hueco más grande antes de los botones
        Spacer(modifier = Modifier.height(32.dp))

        // Column solo para los botones, con 12 dp de separación entre ellos
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    // ACTION_VIEW: le decimos a Android "quiero VER este recurso" y el sistema decide que app usar (normalmente el navegador)
                    // Uri.parse convierte el texto de la URL en el formato que android entiende
                    // startActivity lanza esa acción
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/davidfg02"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF552352), // Fondo del botón
                    contentColor = Color.White // Texto del boton
                )
            ) {
                Text(text = "Mi perfil de GitHub")
            }

            Button(
                onClick = {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.linkedin.com/in/david-fraile-dfg02")
                        )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0A66C2), // fondo del botón
                    contentColor = Color.White // color del texto
                )
            ) {
                Text(text = "Mi perfil de LinkedIn")
            }

            Button(
                onClick = {
                    // Abre el selector del sistema para guardar el CV
                    guardarCv.launch("cv_david.pdf")
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Descargar CV")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TarjetaPreview() {
    TarjetaPresentacionTheme {
        TarjetaPresentacion()
    }
}