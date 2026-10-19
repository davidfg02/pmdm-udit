import 'package:flutter/material.dart';

// main(): el punto de arranque de CUALQUIER app Flutter
//Siempre existe un runApp() = onCreate() en Kotlin.

void main() {
  runApp(const BurgerShopApp());
}

//App raiz

class BurgerShopApp extends StatelessWidget {
  // const + super.key es nuestro constructor. Flutter pide siempre ese comando.
  const BurgerShopApp(super.key);

  //build() es el método que todo widget debe tener.
  //Aquí es donde dibujas todo lo que se ve en la pantalla
  @override
  Widget build(BuildContext context) {
    //MaterialApp: el "contenedor raíz" de toda la app
    return MaterialApp(
      debugShowCheckedModeBanner: false, //quita la cinta roja
      title: 'Burger Shop',

      theme: ThemeData(
        //COLOR DE LA APP: esta es la unica línea para cambiar el color de la app
        colorSchemeSeed: Colors.deepOrange,
        useMaterial3: true,
      ),
      home: const PantallaCatalogo(), //La primera pantalla que se ve al abrir el archivo
    );
  }
}

//MODELO DE DATOS
//En Dart no existe dataClass como en Kotlin: una clase normal con un constructor que recibe los campos para hacer el mismo trabajo

class Producto {
  // final: el valor no se cambia una vez creado el objeto (val en Kotlin)
  final String nombre;
  final String precio; //al modificar el carrito de compras, modificamos la variable
  final String imagenAsset;

  //constructor: requiered obliga a pasar siempre estos 3 datos al crear un producto
  Producto({
    required this.nombre,
    required this.precio,
    required this.imagenAsset,
  });
}

//DATOS DE PRUEBA
//Los mismos productos en Kotlin apuntando a los mismos archivos qe tenemos en nuestro assets
//final list Producto = lista que no se puede reasignar pero contenido si podríamos modificarlo
final List<Producto> catalogoHamburguesas = [
  Producto(
    nombre: 'Clásica con queso',
    precio: '7.90€',
    imagenAsset: 'assets/images/burger_clasica.jpg',
  ),
  Producto(
    nombre: 'BBQ burger',
    precio: '6.90€',
    imagenAsset: 'assets/images/burger_bbq.jpg',
  ),
  Producto(
    nombre: 'Burger doble',
    precio: '8.90€',
    imagenAsset: 'assets/images/burger_doble.jpg',
  ),
  Producto(
    nombre: 'Picante jalapeño',
    precio: '7.80€',
    imagenAsset: 'assets/images/burger_picante.jpg',
  ),
  Producto(
    nombre: 'Burger de pollo',
    precio: '5.90€',
    imagenAsset: 'assets/images/burger_pollo.jpg',
  ),
  Producto(
    nombre: 'Burger vegetariana',
    precio: '5.00€',
    imagenAsset: 'assets/images/burger_vegetariana.jpg',
  ),
];

//PANTALLA 1: CATÁLOGO RESPONSIVE (RETO 1)
class PantallaCatalogo extends StatelessWidget {
  const PantallaCatalogo({super.key});

  @override
  Widget build(BuildContext context) {
    //Scaffold: la estructura estandar de una pantalla Android y IOS.

    return Scaffold(
      //barra superior con el titulo
      appBar: AppBar(
          title: const Text('🍔 BurgerShop')),

      //LayoutBuilder: nos da el anccho disponible de la pantalla
      body: LayoutBuilder(
        builder: (context, constraints) {
          //Mismo calculo que GridCells.Adaptative(minSize = 250.dp)
          //Cuantas yarjetas de 250, caben en el ancho disponible
          const double anchoMinimo = 250;
          const double espacio = 16;
          final double anchoUtil = constraints.maxWidth - (espacio * 2);
          int columnas = ((anchoUtil + espacio) / (anchoMinimo + espacio))
              .floor();
          if (columnas < 1) columnas = 1; //mínimo una columna

          //GridViewBuilder: Equivalente a LazyVerticalGrid
          return GridView.builder(
              padding: const EdgeInsets.all(16),
              //Margen alrededor de la rejilla
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: columnas, //Móvil vertical 1. Horizontal: 3
                mainAxisSpacing: espacio, //Espacio vertical entre tarjetas
                crossAxisSpacing: espacio, //Espacio horizontal entre columnas

                //Alto fijo de cada tarjeta
                mainAxisExtent: 340,
              ),
              itemCount: catalogoHamburguesas.length,
              //cuantos elementos tiene la lista
              itemBuilder: (context, index) {
                final producto = catalogoHamburguesas[index];
                return TarjetaProducto(producto: producto);
              },
          );
        },
      ),
    );
  }
}