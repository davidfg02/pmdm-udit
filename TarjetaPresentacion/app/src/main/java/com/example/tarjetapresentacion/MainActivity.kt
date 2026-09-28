package com.example.tarjetapresentacion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    //LocalContext: asi un Composable "pide prestado" el contexto de Android. Lo necesitamos para poder abrir el navegador desde el botón
    val context = LocalContext.current
    // COLUMN: apila los elementos de arriba a abajo (como un flexbox vertical)
    Column(
        modifier = Modifier
            .fillMaxSize() // ocupa toda la pantalla
            .padding(16.dp), // margen para que nada toque los bordes
        horizontalAlignment = Alignment.CenterHorizontally, // centra en el eje X
        verticalArrangement = Arrangement.Center // centra en el eje Y
    ) {

        //IMAGE: la foto de perfil requiere un archivo "foto_perfil" dentro de re/drawable
        Image(
            painter = painterResource(id = R.drawable.foto_perfil),
            contentDescription = "Foto de perfil de usuario",
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        //Hueco vacío entre la imagen y el texto
        Spacer(modifier = Modifier.height(24.dp))

        //TEXT: nombre
        Text(
            text = "David Fraile",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        //TEXT: rol o profesión
        Text(
            text = "Estudiante DAM",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        //Hueco más grande antes del texto (32)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // ACTION_VIEW: le decimos a Android "quiero VER este recurso" y el sistema decide que app usar (normalmente el navegador)
                // Uri.parse convierte el texto de la URL en el formato que android entiende
                // startActivity lanza esa acción
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/davidfg02"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(text = "Mi perfil de GitHub")
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