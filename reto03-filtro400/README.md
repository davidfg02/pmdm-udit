# Filtro400

Aplicación Android que filtra llamadas entrantes no deseadas **antes de que suenen**. Nació en la Fase 1 como una única regla (bloquear los números que empiezan por `400`) y ha evolucionado hasta un sistema de filtrado con listas, categorías, bloqueos temporales y franjas horarias.

**Tecnologías:** Kotlin · Jetpack Compose · Room (con KSP) · Corrutinas y Flow · MVVM · `CallScreeningService`

## Funcionalidades

- Bloqueo automático por prefijo (`400`).
- Números bloqueados con motivo y categoría (Spam, Telemarketing, Fraude, Otro…), permanentes o **temporales** (caducan solos).
- **Lista blanca**: números que nunca se bloquean.
- **Franjas horarias**: el bloqueo solo se aplica dentro de las franjas activas, incluidas las que cruzan la medianoche (22:00–08:00).
- **Historial** de llamadas bloqueadas con contador de no leídas.
- **Estadísticas** de bloqueos.
- Búsqueda y filtrado por texto, categoría y estado; importación y exportación en **CSV**.
- **Probador de números**: comprueba qué haría la app con un número sin recibir una llamada real.

## Cómo decide la app

Cada llamada entrante pasa por estas reglas, en este orden:

1. **Normalización** del número: se quitan espacios y guiones y los prefijos `+34` y `0034`.
2. **Lista blanca**: si el número está, la llamada pasa siempre.
3. **Reglas de bloqueo**: prefijo `400` o número en la lista de bloqueados (los temporales caducados se ignoran).
4. **Franja horaria**: aunque coincida una regla, solo se bloquea si estamos dentro de una franja activa (o si el modo por franjas está desactivado).

## Estructura del proyecto

| Archivo | Responsabilidad |
|---|---|
| `FiltroLlamadasService.kt` | Servicio que recibe cada llamada y decide si se bloquea |
| `ReglaBloqueo.kt`, `FranjaHoraria.kt` | Reglas puras (normalizar, prefijo, franjas), sin dependencias de Android |
| `AppDatabase.kt`, `BloqueoDao.kt`, `LlamadaDao.kt` | Base de datos Room (versión 3, con migraciones) |
| `BloqueosRepository.kt`, `LlamadasRepository.kt` | Lógica de datos sin depender de Compose |
| `BloqueadosPreferencias.kt`, `PermitidosPreferencias.kt`, `FranjasPreferencias.kt` | Copia rápida de las listas para que el servicio responda a tiempo |
| `MainActivity.kt` y `Pantalla*.kt` | Interfaz con Jetpack Compose |

## Cómo ejecutarla

1. Abre el proyecto en Android Studio y espera a que Gradle sincronice.
2. Ejecútala en un móvil o emulador con Android 10 (API 29) o superior.
3. Pulsa **Activar** en la pantalla principal y acepta el diálogo del sistema para conceder a la app el rol de filtro de llamadas.

---

# Preguntas del reto

## 1. ¿Qué es `CallScreeningService` y qué función cumple en Android?

`CallScreeningService` es una clase del sistema Android (paquete `android.telecom`) que permite a una app **examinar las llamadas entrantes antes de que suenen** y decidir qué hacer con ellas. Android llama automáticamente a su método `onScreenCall(callDetails)` en cada llamada entrante, y la app responde con un `CallResponse` que indica si la llamada debe:

- **Rechazarse** (`setRejectCall`).
- **No sonar** (`setDisallowCall`).
- **No generar notificación de llamada perdida** (`setSkipNotification`).

En Filtro400, `FiltroLlamadasService` extiende esta clase: obtiene el número con `callDetails.handle`, aplica las reglas y responde con `respondToCall`. Para registrarlo se declara en el `AndroidManifest.xml` con el permiso `BIND_SCREENING_SERVICE` y el `intent-filter` de `android.telecom.CallScreeningService`.

## 2. ¿Por qué necesitamos solicitar el rol `ROLE_CALL_SCREENING`?

Porque filtrar llamadas es una capacidad **sensible**: una app que puede rechazar llamadas o ver los números que llaman afecta directamente a la privacidad y al uso del teléfono. Android no permite que cualquier app se declare filtro de llamadas solo con registrar el servicio, sino que exige que **el usuario lo autorice de forma explícita** mediante un *rol*.

Los roles los gestiona `RoleManager`. Nuestra app comprueba si ya tiene el rol con `isRoleHeld(ROLE_CALL_SCREENING)` y, si no, lanza el diálogo del sistema con `createRequestRoleIntent(ROLE_CALL_SCREENING)`. Sin el rol, el servicio está declarado pero **Android no lo activa nunca**, y la app no recibe ninguna llamada. Además, el usuario puede revocarlo en cualquier momento desde los ajustes.

## 3. ¿Por qué no necesitamos un servidor externo para realizar el filtrado?

Porque toda la información necesaria para decidir está en el propio dispositivo:

- **El número entrante** nos lo entrega Android localmente a través de `CallScreeningService`.
- **Las reglas** (prefijo, lista de bloqueados, lista blanca y franjas horarias) se guardan en el móvil, en Room y en una copia rápida en `SharedPreferences`.
- **La decisión** es una comparación sencilla de cadenas y horas, que se resuelve en milisegundos.

Hacerlo en local tiene ventajas importantes:

- **Velocidad:** Android exige una respuesta rápida; una petición de red añadiría latencia y podría hacer que la llamada suene antes de decidir.
- **Privacidad:** los números que llaman nunca salen del teléfono.
- **Funciona sin conexión** y no tiene costes de infraestructura.

La contrapartida es que la app solo conoce las reglas que el usuario ha definido. Un servidor externo solo sería necesario para funciones adicionales, como consultar una base de datos antispam compartida.

## 4. ¿Qué ocurre si el número telefónico está guardado en los contactos?

**La app no lo filtra.** Android solo entrega a `CallScreeningService` las llamadas de números que **no están en la agenda** del usuario. Si el número es un contacto, la llamada suena con normalidad aunque coincida con una regla (por ejemplo, un contacto cuyo número empieza por `400`), y `onScreenCall` no llega a ejecutarse.

Esto tiene sentido desde el punto de vista de la privacidad: el sistema asume que una llamada de un contacto es deseada y evita que una app de terceros pueda interferir en ellas o consultar quién llama sin ser un número desconocido. También significa que Filtro400 **no necesita el permiso `READ_CONTACTS`** y no accede a la agenda.

Esta limitación es relevante para las pruebas: el **probador de números** de la app aplica siempre las reglas, así que puede indicar "se bloquearía" para un número que, si estuviera guardado en los contactos, sí sonaría en una llamada real.

## 5. ¿Qué diferencias existen entre comprobar un número mediante una función y bloquear realmente una llamada?

Comprobar un número es **calcular una decisión**; bloquear una llamada es **ejecutar esa decisión sobre el sistema telefónico**.

| | Comprobar mediante una función (probador) | Bloquear una llamada real |
|---|---|---|
| **Qué hace** | Devuelve un resultado (`BLOQUEADA`, `FUERA_DE_FRANJA`, permitida) y un texto explicativo | Rechaza la llamada, evita que suene y que genere notificación |
| **Quién lo inicia** | El usuario, pulsando "Comprobar número" | Android, al entrar una llamada, llamando a `onScreenCall` |
| **Requisitos** | Ninguno: es una función normal | Rol `ROLE_CALL_SCREENING`, servicio registrado en el manifiesto y el número fuera de los contactos |
| **Efectos** | Ninguno sobre la llamada ni sobre el historial | Responde con `CallResponse` y guarda la llamada en el historial |
| **Tiempo** | Sin restricción | Debe responder en milisegundos o Android lo ignora |
| **Pruebas** | Fácil de probar con JUnit sin emulador | Requiere un dispositivo y una llamada real |

Por eso en el código la decisión (`ReglaBloqueo`, `ReglaHoraria`) está **separada de Android**: puede probarse sin llamadas reales, pero el servicio sigue siendo el único que consigue que una llamada se bloquee de verdad. Una comprobación correcta en el probador no garantiza el bloqueo si la app no tiene el rol o el número está en los contactos.

## 6. ¿Qué mejoras incorporaría en una futura versión?

- **Unificar la lógica de decisión.** Ahora el servicio y el probador comprueban las reglas por separado; un único motor de filtrado compartido evitaría duplicación y diferencias entre lo que muestra el probador y lo que ocurre de verdad.
- **Prefijos configurables.** El prefijo `400` está fijo en el código; el usuario debería poder añadir y priorizar sus propios prefijos desde la interfaz.
- **Normalización internacional.** Solo se eliminan `+34` y `0034`; habría que gestionar otros países y formatos.
- **Números ocultos o privados**, con una regla propia configurable.
- **Tests automáticos** de las reglas (`ReglaBloqueo`, `ReglaHoraria`) y de las migraciones de Room.
- **Una única fuente de verdad** para las listas, evitando mantener sincronizadas la copia en `SharedPreferences` y la base de datos Room.
- **Notificación opcional** al bloquear una llamada, y acceso rápido para añadir un número a la lista blanca desde el historial.
- **Copia de seguridad y restauración** de toda la configuración (bloqueados, lista blanca, categorías y franjas).
- **Contador de intentos y detección de campañas de spam** (muchas llamadas del mismo prefijo en poco tiempo).
- **Antispam externo opcional**, con una lista de números sospechosos que se actualice periódicamente (esto sí requeriría una fuente externa).
- **Mejoras de accesibilidad y modo oscuro** revisados en todas las pantallas.
