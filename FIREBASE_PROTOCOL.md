# Protocolo de Comunicación: App <-> Firebase (Yomar te Presta)

Este documento establece las reglas estrictas para las operaciones de lectura, escritura y sincronización entre la aplicación Android y Firebase (Firestore/Storage).

## 1. Reglas Generales de Firestore
- **Sincronización Web-App:** Dado que el sistema debe ser compatible con una plataforma Web, **NUNCA** sobrescribas documentos completos con `.set()` si el objetivo es actualizar un campo único.
    - **Regla:** Usa `.update()` para cambios específicos de campos.
    - **Regla:** Si usas `.set()` en un perfil de usuario, **DEBES** incluir `SetOptions.merge()` para evitar la eliminación accidental de campos (como el `fcmToken` o configuraciones web).
- **Fechas:** Todo campo de fecha debe almacenarse como un String en formato **ISO 8601** (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`) en UTC para garantizar la interoperabilidad con servicios externos.

## 2. Estructura de Datos
- **Colección `users`:**
    - ID: Documento = `uid` de Firebase Auth.
    - Campos: `firstName`, `lastName`, `cedula`, `phone`, `bank`, `pagoMovilId`, `pagoMovilBank`, `pagoMovilPhone`, `idVerificationStatus`, etc.
- **Colección `loans`:**
    - ID: Documento generado automáticamente (ID de Firestore).
    - Campo `status`: Define el flujo del vale (`pending`, `approved`, `repayment_pending`, `paid`, `rejected`, `overdue`).
- **Colección `settings`:**
    - ID: Documento `general`.
    - Campos: `bcvRate`, `adminBank`, etc.

## 3. Protocolos de Escritura Segura
- **Validación de Archivos:** Antes de realizar cualquier acción de estado (ej. `updateLoanStatus` para `paid`), el repositorio debe verificar la existencia del campo de prueba (`repaymentCaptureUrl`). **NO** se permite avanzar estados sin soporte visual.
- **Transacciones de Estado:** Al actualizar el estado de un vale (`updateLoanStatus`):
    - Si se marca como `paid`, se debe calcular automáticamente el nuevo saldo y, si es necesario, actualizar el nivel del usuario basándose en el conteo de vales pagados.
    - Siempre usar `FieldValue.delete()` si un campo de comprobante se limpia tras una operación.

## 4. Gestión de Archivos (Firebase Storage)
- **Rutas de Almacenamiento:**
    - `id_documents/{uid}_{timestamp}`: Imágenes de cédulas/identidad.
    - `signatures/{loanId}.png`: Firmas digitales.
    - `repayments/{loanId}_{timestamp}`: Comprobantes de pago.
- **Regla:** El URL de descarga debe obtenerse inmediatamente después de la subida y guardarse en el documento de Firestore correspondiente antes de completar la transacción de estado.

## 5. Reactividad
- **Flujos:** Todas las consultas deben exponerse mediante `Flow` utilizando `callbackFlow`.
- **Cierre:** Siempre implementar `awaitClose { subscription.remove() }` para evitar fugas de memoria al cerrar las pantallas.
