# Documentación Técnica: Proyecto "Yomar te Presta"

Este archivo resume el contexto técnico, la arquitectura y los protocolos críticos del proyecto para garantizar la consistencia en el desarrollo con cualquier modelo de IA.

## 1. Stack Tecnológico
- **Lenguaje:** Kotlin.
- **UI:** Jetpack Compose (Material3).
- **Arquitectura:** MVVM (Model-View-ViewModel).
- **Backend:** Firebase (Auth, Firestore, Storage, Messaging).
- **Asincronía:** Kotlin Coroutines & Flow (`StateFlow` para estados de UI).
- **Inyección de Dependencias:** `MainViewModel` recibe `FirebaseRepository` por constructor.

## 2. Estructura de Paquetes
- `ui/`: Pantallas organizadas por funcionalidad (`auth`, `admin`, `loan`, `payment`, `profile`, `dashboard`).
- `data/model/`: Modelos de datos (`User`, `Loan`, `ContractSigned`, `Settings`).
- `data/repository/`: `FirebaseRepository` (única fuente de verdad para acceso a datos).
- `services/`: `MyFirebaseMessagingService` para notificaciones push.

## 3. Protocolos de Desarrollo (Reglas de Oro)
- **Prohibición de Refactorización No Solicitada:** NUNCA modifiques firmas de métodos o estructuras que ya funcionan sin aprobación explícita.
- **Edición Segura:** SIEMPRE usar `write_file` o `replace_text`. Nunca usar comandos de terminal para editar código.
- **Código Intocable:** Respetar bloques marcados con `// Approved` o `// Stable`.
- **UI Framework:** Usar siempre los componentes de diseño definidos (`GlassCard`, `GoldMate`, `GraySmoke`).

## 4. Protocolo de Comunicación (App <-> Firebase)
- **Sincronización Web-App:** 
    - Usar `.update()` preferentemente.
    - Usar `SetOptions.merge()` si se usa `.set()` en perfiles para no borrar campos no mapeados (como `fcmToken`).
- **Fechas:** Todo formato de fecha en Firestore debe ser **ISO 8601** (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`) en UTC.
- **Validaciones:** 
    - No permitir cambios de estado de vales (ej. a `paid`) sin validar la existencia de comprobantes (`repaymentCaptureUrl`).
    - Validar longitud mínima de campos críticos (ej. cédula > 6 dígitos, teléfono > 10 dígitos) antes de guardar perfil.

## 5. Navegación e Identidad
- **Admin vs Empleado:** Identificar el rol mediante `currentUser.email`. El admin tiene acceso a `AdminDashboardScreen` y `admin_edit_user/{userId}`.
- **Multi-Usuario:** El sistema debe limpiar estados (`_user.value = null`, etc.) al hacer logout en `MainViewModel` para evitar fuga de datos entre usuarios en el mismo dispositivo.
- **Edición de Perfil:** La pantalla `ProfileScreen` reutiliza la lógica para el propio usuario y para el administrador editando a otros, usando `isAdminEdit` como flag de estado.
