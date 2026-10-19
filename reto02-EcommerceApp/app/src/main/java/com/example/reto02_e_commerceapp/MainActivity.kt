package com.example.reto02_e_commerceapp
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
private val Azul = Color(0xFF76A6FF)
private const val URL_BANNER =
    "https://davidfg02.github.io/Albas-en-el-aula-old/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Permite que la interfaz se extienda por detrás de las barras del sistema.
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Azul
                ) {
                    PantallaPrincipal()
                }
            }
        }
    }
}
data class Producto(
    val nombre: String,
    val precio: String,
    val imanResId: Int,
    val descripcion: String,
    val url: String
)

private val catalogoRecursos = listOf(

    // Primer producto.
    Producto(
        nombre = "Cuaderno de Trabajo",
        precio = "5.99 €",
        imanResId = R.drawable.cuaderno_trabajo,
        descripcion =
            "Cuaderno de actividades para trabajar los contenidos educativos.",
        url =
            "https://eduki.com/es/material/1955611/organizacion-temas-1"
    ),
    Producto(
        nombre = "Flashcard Desarrollo",
        precio = "4.99 €",
        imanResId = R.drawable.flashcard_bloque_desarrollo,
        descripcion =
            "Tarjetas de estudio sobre el desarrollo infantil.",
        url =
            "https://eduki.com/es/material/1956878/flashcards-bloque-desarrollo"
    ),
    Producto(
        nombre = "Flashcard Socialización",
        precio = "4.99 €",
        imanResId = R.drawable.flashcard_bloque_socializacion_salud,
        descripcion =
            "Tarjetas para repasar socialización, salud y contenidos relacionados.",
        url =
            "https://eduki.com/es/material/1956860/flashcards-bloque-socializacion-y-salud"
    ),
    Producto(
        nombre = "Preguntas Tribunal",
        precio = "3.99 €",
        imanResId = R.drawable.preguntas_tribunal,
        descripcion =
            "Tarjetas para preparar las preguntas que pueda hacer el tribunal.",
        url =
            "https://eduki.com/es/material/1955589/preguntas-tribunal-oposiciones"
    ),
    Producto(
        nombre = "Flashcard Temas",
        precio = "4.99 €",
        imanResId = R.drawable.flashcard_temas,
        descripcion = "Tarjetas para repasar los temas.",
        url =
            "https://eduki.com/es/material/1956850/flashcard-bloque-didactica-temas-6-12-15"
    )
)

// Función que dibuja el catálogo y recibe la lista de productos que debe mostrar.
@Composable
fun CatalogoRecursos(
    productos: List<Producto>
) {
    val context = LocalContext.current

    // Estado que guarda el texto escrito por el usuario en el buscador.
    // remember conserva el valor mientras se recompone esta interfaz.
    var busqueda by remember {
        mutableStateOf("")
    }
    // Filtra los productos según su nombre.
    // ignoreCase = true permite buscar sin distinguir mayúsculas y minúsculas.
    val productosFiltrados = productos.filter {
        it.nombre.contains(busqueda, ignoreCase = true)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Evita que el contenido quede debajo de la barra superior.
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.logo
            ),
            // Descripción para accesibilidad.
            contentDescription = "Banner promocional de OpoApp",

            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp
                ) // Añade separación respecto a los bordes y al contenido superior.
                .clickable {
                    // Crea una acción para abrir una página web.
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(URL_BANNER)
                    )
                    // Ejecuta la acción y abre el enlace en una aplicación compatible.
                    context.startActivity(intent)
                },
            // Recorta la imagen para cubrir el espacio sin deformarla.
            contentScale = ContentScale.Crop
        )
        // BUSCADOR: permite localizar productos por su nombre.
        OutlinedTextField(
            value = busqueda, // Texto que aparece actualmente en el campo.

            // Actualiza el estado cada vez que el usuario escribe o borra texto.
            // Al cambiar busqueda, Compose vuelve a dibujar los elementos necesarios.
            onValueChange = {
                busqueda = it
            },
            label = {
                Text("Buscar productos")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 8.dp
                ),
            // Redondea las esquinas del campo.
            shape = RoundedCornerShape(20.dp),

            // Impide que el texto introducido ocupe varias líneas.
            singleLine = true,

            // Personaliza los colores del buscador.
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Azul,
                unfocusedLabelColor = Azul,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Azul
            )
        )

        // Comprueba si la búsqueda ha devuelto algún resultado.
        if (productosFiltrados.isEmpty()) {

            Text(
                text = "No se encontraron productos.",
                modifier = Modifier.padding(16.dp),
                color = Color.White
            )
        } else {
            // Cuadrícula desplazable que muestra los productos filtrados.
            LazyVerticalGrid(
                // Crea tantas columnas como permita el espacio disponible.
                // Cada columna tendrá un ancho mínimo de 250 dp.
                columns = GridCells.Adaptive(minSize = 250.dp),

                // Ocupa el espacio restante debajo del banner y del buscador.
                modifier = Modifier.weight(1f),

                // Añade márgenes alrededor del contenido de la cuadrícula.
                contentPadding = PaddingValues(16.dp),

                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Genera una tarjeta por cada producto que coincide con la búsqueda.
                items(productosFiltrados) { producto ->

                    // Dibuja la tarjeta utilizando los datos del producto actual.
                    TarjetaProducto(producto = producto)
                }
            }
        }
    }
}

@Composable
fun TarjetaProducto(
    producto: Producto
) {
    // Obtiene el contexto necesario para abrir el enlace del producto.
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Image(
                painter = painterResource(
                    id = producto.imanResId
                ),
                // Utiliza el nombre del producto como descripción accesible.
                contentDescription = producto.nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = producto.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Azul,
                )
                Text(
                    text = producto.descripcion,
                    fontSize = 13.sp,
                    color = Azul,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = producto.precio,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Azul,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(
                    onClick = {
                        // Crea una acción para abrir la URL del producto actual.
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(producto.url)
                        )
                        // Abre el enlace en el navegador o aplicación compatible.
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Azul,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 10.dp
                    )
                ) {
                    Text(
                        text = "Ver producto",
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// Función que controla la portada inicial y el acceso al catálogo.
@Composable
fun PantallaPrincipal() {

    // Estado que indica si debe mostrarse la portada.
    // Empieza siendo true para mostrarla al iniciar la pantalla.
    var mostrarPortada by remember {
        mutableStateOf(true)
    }

    // Ejecuta una tarea cuando se inicia esta composición.
    LaunchedEffect(Unit) {

        delay(1000) // Espera 1s
        // Cambia el estado para dejar de mostrar la portada.
        mostrarPortada = false
    }
    // Si mostrarPortada es true, se enseña la pantalla inicial.
    if (mostrarPortada) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📚 OpoApp",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    } else {
        CatalogoRecursos(
            productos = catalogoRecursos
        )
    }
}