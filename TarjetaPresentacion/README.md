# 📱 Tarjeta de Presentación

Aplicación Android desarrollada con **Kotlin y Jetpack Compose** como proyecto de la asignatura de **Programación Multimedia y Dispositivos Móviles (PMDM)** de **2º DAM en UDIT**.

La aplicación funciona como una **tarjeta de presentación digital**, mostrando información personal y proporcionando acceso directo a los principales perfiles profesionales, además de permitir descargar el currículum vitae desde el propio dispositivo.

---

## 📋 Descripción

**TarjetaPresentacion** es una aplicación Android sencilla y orientada a la presentación de un perfil profesional.

La pantalla principal muestra:

* 👤 Foto de perfil
* 🧑‍💻 Nombre y perfil profesional
* 🐙 Acceso al perfil de GitHub
* 💼 Acceso al perfil de LinkedIn
* 📄 Opción para descargar el CV en formato PDF

El proyecto está desarrollado utilizando **Jetpack Compose**, el toolkit moderno de Android para construir interfaces de usuario de forma declarativa.

---

## ✨ Funcionalidades

### 👤 Perfil personal

La aplicación muestra la información básica del usuario mediante una interfaz sencilla y centrada:

* Foto de perfil
* Nombre
* Rol profesional/educativo

### 🐙 Perfil de GitHub

El botón **"Mi perfil de GitHub"** utiliza un `Intent` de Android con `ACTION_VIEW` para abrir el perfil de GitHub en el navegador predeterminado del dispositivo.

### 💼 Perfil de LinkedIn

El botón **"Mi perfil de LinkedIn"** funciona de forma similar, utilizando un `Intent` para abrir el perfil de LinkedIn.

### 📄 Descarga del CV

El botón **"Descargar CV"** utiliza:

```kotlin
ActivityResultContracts.CreateDocument("application/pdf")
```

Esto permite abrir el selector de archivos del sistema para que el usuario pueda elegir dónde guardar el currículum.

El PDF se encuentra incluido dentro de los recursos de la aplicación:

```text
app/src/main/res/raw/cv_david.pdf
```

Al seleccionar una ubicación, la aplicación copia el archivo PDF desde los recursos internos al destino elegido y muestra un mensaje confirmando que el CV se ha guardado correctamente.

---

## 🛠️ Tecnologías utilizadas

| Tecnología          | Uso                                 |
| ------------------- | ----------------------------------- |
| **Kotlin**          | Lenguaje principal                  |
| **Jetpack Compose** | Construcción de la interfaz         |
| **Material 3**      | Componentes y diseño visual         |
| **Android SDK**     | Desarrollo de la aplicación         |
| **Gradle**          | Gestión del proyecto y dependencias |
| **Android Studio**  | Entorno de desarrollo               |


---

## 📂 Organización del proyecto

La estructura principal del proyecto es la siguiente:

```text
TarjetaPresentacion/
│
├── app/
│   │
│   ├── src/
│   │   ├── androidTest/
│   │   │
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/tarjetapresentacion/
│   │   │   │       │
│   │   │   │       ├── MainActivity.kt
│   │   │   │       │
│   │   │   │       └── ui/
│   │   │   │           └── theme/
│   │   │   │               ├── Color.kt
│   │   │   │               ├── Theme.kt
│   │   │   │               └── Type.kt
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── foto_perfil.jpg
│   │   │   │   │   ├── ic_launcher_background.xml
│   │   │   │   │   └── ic_launcher_foreground.xml
│   │   │   │   │
│   │   │   │   ├── mipmap-*/
│   │   │   │   │   └── iconos de la aplicación
│   │   │   │   │
│   │   │   │   ├── raw/
│   │   │   │   │   └── cv_david.pdf
│   │   │   │   │
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   │
│   │   │   │   └── xml/
│   │   │   │       ├── backup_rules.xml
│   │   │   │       └── data_extraction_rules.xml
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/
│   │       └── java/
│   │           └── .../ExampleUnitTest.kt
│   │
│   └── build.gradle.kts
│
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── .gitignore
```

## 🎨 Interfaz

La interfaz está construida completamente con **Jetpack Compose**.

La estructura visual principal utiliza un `Column`, que permite colocar los elementos verticalmente:

```text
┌─────────────────────────────┐
│                             │
│        Foto de perfil       │
│                             │
│        David Fraile         │
│        Estudiante DAM       │
│                             │
│   ┌─────────────────────┐   │
│   │ Mi perfil de GitHub │   │
│   └─────────────────────┘   │
│                             │
│   ┌─────────────────────┐   │
│   │ Mi perfil LinkedIn  │   │
│   └─────────────────────┘   │
│                             │
│   ┌─────────────────────┐   │
│   │     Descargar CV    │   │
│   └─────────────────────┘   │
│                             │
└─────────────────────────────┘
```
---

## 🔗 Navegación mediante Intents

Para abrir los perfiles externos se utiliza el sistema de `Intent` de Android.

Por ejemplo, para GitHub:

```kotlin
val intent =
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://github.com/davidfg02")
    )

context.startActivity(intent)
```

Android se encarga de determinar qué aplicación puede gestionar la URL, normalmente el navegador instalado en el dispositivo.

Este mismo mecanismo se utiliza para abrir LinkedIn.

---

## 📄 Sistema de descarga del CV

La aplicación no necesita solicitar permisos de almacenamiento para guardar el CV.

En su lugar, utiliza el selector de documentos proporcionado por Android:

```kotlin
ActivityResultContracts.CreateDocument("application/pdf")
```

El flujo es:

```text
Usuario pulsa "Descargar CV"
              ↓
Se abre el selector de archivos
              ↓
Usuario selecciona ubicación y nombre
              ↓
Android devuelve una URI
              ↓
La aplicación abre un OutputStream
              ↓
Se copia cv_david.pdf
              ↓
Se muestra "CV guardado"
```

Esto permite utilizar el sistema de almacenamiento de Android de forma segura y compatible con las versiones modernas del sistema operativo.

---

## 🚀 Instalación y ejecución

### Requisitos

Para ejecutar el proyecto se necesita:

* **Android Studio**
* **JDK 17**
* Android SDK compatible con **API 37**
* Un dispositivo Android físico o un emulador

### 1. Clonar el repositorio

```bash
git clone https://github.com/davidfg02/pmdm-udit.git
```

### 2. Abrir el proyecto

Abrir en Android Studio la carpeta:

```text
pmdm-udit/TarjetaPresentacion
```

Android Studio descargará automáticamente las dependencias necesarias mediante Gradle.

### 3. Sincronizar Gradle

Esperar a que Android Studio complete la sincronización del proyecto.

### 4. Ejecutar la aplicación

Seleccionar un dispositivo físico o crear/iniciar un emulador y pulsar:

```text
Run ▶
```


En Windows:

```bash
gradlew.bat installDebug
```

---

## 🔧 Personalización

La aplicación puede adaptarse fácilmente para utilizarse como una tarjeta profesional de cualquier persona.

Algunos elementos que se pueden modificar desde `MainActivity.kt` son:

### Nombre

```kotlin
Text(
    text = "David Fraile"
)
```

### Rol

```kotlin
Text(
    text = "Estudiante DAM"
)
```

### Foto

Sustituir:

```text
app/src/main/res/drawable/foto_perfil.jpg
```

por otra imagen y mantener el mismo nombre, o modificar la referencia utilizada en `painterResource`.

### GitHub

Modificar la URL:

```kotlin
https://github.com/davidfg02
```

### LinkedIn

Modificar la URL correspondiente al perfil de LinkedIn.

### CV

Sustituir:

```text
app/src/main/res/raw/cv_david.pdf
```

por otro documento PDF y actualizar el nombre utilizado en el código si es necesario.

---