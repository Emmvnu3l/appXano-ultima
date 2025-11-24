# Proyecto E-commerce Android (Kotlin + Xano)

Este proyecto es una aplicación de comercio electrónico nativa para Android escrita en Kotlin, que utiliza Xano como backend NoSQL.

## 1. Pasos de Configuración

### Requisitos Previos
- **Android Studio**: Ladybug o superior.
- **JDK**: Java 11 o superior (configurado en Gradle).
- **Dispositivo/Emulador**: Android 7.0 (API 24) o superior.

### Configuración del Proyecto
1.  **Clonar/Abrir**: Abre la carpeta del proyecto en Android Studio.
2.  **Sincronizar Gradle**: Deja que Android Studio descargue las dependencias.
3.  **Verificar `local.properties`**: Asegúrate de que apunte a tu SDK de Android.
4.  **Compilar**: Ejecuta `Build > Make Project` para asegurar que no hay errores de compilación.

### Configuración del Backend (Xano)
El proyecto ya está configurado para apuntar a las siguientes instancias de Xano. No se requiere configuración adicional en el código a menos que cambien los endpoints.

## 2. Variables y URLs del Backend

Las URLs base están definidas en `app/build.gradle.kts` y expuestas vía `BuildConfig`.

| Servicio | Variable | URL Base |
| :--- | :--- | :--- |
| **Autenticación** | `AUTH_BASE_URL` | `https://x8ki-letl-twmt.n7.xano.io/api:QGdanllI/` |
| **Tienda (Productos/Ordenes)** | `STORE_BASE_URL` | `https://x8ki-letl-twmt.n7.xano.io/api:3BVxr_UT/` |
| **Usuarios** | `USER_BASE_URL` | `https://x8ki-letl-twmt.n7.xano.io/api:xfq7gh7l/` |

> **Nota**: El endpoint de usuarios (`/user`) requiere un token de autenticación válido obtenido a través del servicio de Autenticación.

## 3. Usuarios de Prueba

Utiliza estas credenciales para probar los diferentes roles en la aplicación:

### Administrador
- **Email**: `emm.moreno@duocuc.cl`
- **Password**: `Brilin1*`
- **Capacidades**: Gestión de usuarios, ver todas las órdenes, editar productos.

### Cliente (Usuario)
- **Email**: `usuario@duocuc.cl`
- **Password**: `Brilin1*`
- **Capacidades**: Comprar productos, ver historial de órdenes propias, editar perfil propio.

## 4. Almacenamiento de Imágenes

Las imágenes de los productos y avatares se almacenan directamente en **Xano**.

- **Subida**: Se utiliza el endpoint `POST /upload/image` (definido en `UploadService`).
- **Formato**: Las imágenes se envían como `multipart/form-data`.
- **Respuesta**: Xano devuelve un objeto con la URL pública de la imagen, que luego se guarda en el registro del producto o usuario.