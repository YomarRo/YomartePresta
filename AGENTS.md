# Reglas de Desarrollo para Yomar te Presta

Este archivo contiene las directivas fundamentales para cualquier asistente de IA que trabaje en este proyecto. **DEBEN** ser seguidas estrictamente.

## 1. Arquitectura y Stack Tecnológico
- **Arquitectura:** MVVM (Model-View-ViewModel).
- **UI Framework:** Jetpack Compose exclusivamente.
- **Gestión de Estado:** `StateFlow` expuesto desde el `ViewModel`, consumido con `collectAsStateWithLifecycle()` en la UI.
- **Backend:** Firebase (Auth, Firestore, Storage).
- **Inyección de Dependencias:** Usar `FirebaseRepository` inyectado.

## 2. Reglas de Estilo y Preservación de Código
- **Prohibición de Refactorización No Solicitada:** NUNCA modifiques, renombres o reestructures funciones, variables o clases que no estén directamente relacionadas con la tarea actual.
- **Respeto a la UI:** Si existe un sistema de diseño definido (`Themes`, `Colors`, `Type`, `GlassCard`), DEBES usarlo. No crees estilos "ad-hoc" ni cambies modificadores de layout ya aprobados (padding, alineación, etc.) a menos que sea explícitamente solicitado.
- **Código Intocable:** Si encuentras un bloque de código marcado con `// Approved` o `// Stable`, es intocable. No cambies su firma ni comportamiento.

## 3. Manejo de Errores y Calidad
- **Async:** Usar exclusivamente Kotlin Coroutines. Evitar callbacks anidados.
- **Manejo de Errores:** No usar try-catch genéricos. Implementar estados de error definidos o clases Result.
- **Seguridad:** En el panel de Administración (`AdminDashboardScreen.kt`), las validaciones (verificación de comprobantes, aprobación de vales) deben ser estrictas: no permitir acciones sin datos de soporte.

## 4. Instrucciones para Edición
- **Buffer-Safe:** Siempre usa `write_file` o `replace_text`. NUNCA uses comandos de shell (`sed`, `awk`) para modificar archivos, ya que destruyen la historia del IDE.
- **Contexto:** Antes de realizar cualquier cambio, analiza el archivo completo para identificar patrones existentes. Ante la duda, pregunta antes de romper la compatibilidad.
- **Concisión:** Sé útil pero conciso. Prioriza la consistencia con el código existente sobre cualquier optimización estilística personal.
