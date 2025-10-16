# Documentación y comentarios del código

Este README recopila y explica los comentarios añadidos en el proyecto para entender cómo funcionan la autenticación, Retrofit, subida de imágenes, modelos y la UI.

## Arquitectura de red
- `Authorization` (Bearer): El `AuthInterceptor` agrega el encabezado `Authorization: Bearer <token>` automáticamente a todas las requests del cliente autenticado.
- `AuthInterceptor`: Intercepta la request y añade el header si `TokenManager.getToken()` devuelve un token. Evita repetir el header en cada llamada.
- `RetrofitClient`: Centraliza la creación de clientes Retrofit con `OkHttpClient` (timeouts, logging y, si aplica, `AuthInterceptor`) y `GsonConverterFactory`.
- `baseUrl`: Cada grupo de endpoints (auth, store) tiene su propia `baseUrl`; los métodos del servicio sólo definen el path relativo.

## Servicios de API
- `AuthService`:
  - `login(@Body LoginRequest)`: POST sin token; `@Body` serializa el data class a JSON.
  - `signup(@Body SignupRequest)`: POST sin token; registra y devuelve `AuthResponse` (token y user).
  - `me()`: GET con token; devuelve `User` del contexto autenticado.
- `ProductService`:
  - `getProducts()`: GET `/product` con token.
  - `createProductFull(@Body CreateProductFullRequest)`: POST `/product` con token; envía datos del producto y `images` como `ImagePayload`.
- `UploadService`:
  - `uploadImage(@Part MultipartBody.Part)`: `@Multipart` (multipart/form-data) para subir binarios; devuelve `ProductImage`.
- `CategoryService`:
  - `createCategory(@Body CategoryCreateRequest)`: POST `/category` con token; crea una categoría con `name`, `description` e `image` (como `ImagePayload`).

## Subida de imágenes y MIME
- `AddProductFragment`:
  - Selector: `ActivityResultContracts.GetMultipleContents` con filtro `image/*` para elegir varias imágenes.
  - Flujo de creación:
    1) Se suben las imágenes como `MultipartBody.Part` (el `RequestBody` usa el MIME real, p.ej. `image/jpeg`).
    2) La respuesta `ProductImage` se transforma a `ImagePayload` (referencia: path, mime, size, etc.).
    3) Se envía `CreateProductFullRequest` con la lista de `ImagePayload` a `POST /product`.
- `MIME`: “media type” del archivo (`image/jpeg`, `image/png`); se usa para crear el `RequestBody` correcto y derivar extensiones de archivo.

## Modelos de producto e imágenes
- `CreateProductFullRequest`: Request para crear producto; incluye campos básicos y `images: List<ImagePayload>`.
- `CreateProductResponse`: Respuesta de creación de producto; retorno de `ProductService.createProductFull`.
- `ProductImage`: Modelo de respuesta de `UploadService.uploadImage` (binarios subidos). Contiene `id`, `url`, `path`, `mime`, `size`.
- `ImagePayload`: Referencia (no binario) que se envía dentro de `CreateProductFullRequest` para asociar imágenes; contiene `access`, `path`, `name`, `type`, `size`, `mime`, `meta`.

## Fragmentos y Activities
- `RegisterActivity`:
  - UI: `WindowCompat.setDecorFitsSystemWindows(window, true)` para evitar solapamientos con barras del sistema.
  - Validaciones: `validateEmail` y `validatePassword` se ejecutan en `submitRegister()` antes de llamar a `signup`.
  - Servicio: `RetrofitClient.createAuthService(this@RegisterActivity)` (sin token) para login/signup.
- `ProfileFragment`:
  - Binding: `binding.tvFirstNameValue.text = me.firstName ?: ""` evita `null` al mostrar datos.
  - Memoria: en `onDestroyView` se anula `_binding` para evitar fugas.
- `MainActivity`:
  - Tras login, se llama a `/auth/me` con `createAuthServiceAuthenticated(...)` para asegurar y guardar el `role` del usuario.
- `ProductDetailActivity`:
  - También usa `WindowCompat.setDecorFitsSystemWindows(window, true)` para que la UI no invada las barras del sistema.
- `AddProductFragment`:
  - Detalles del flujo de subida y mapeo de imágenes (ver sección Subida de imágenes y MIME).
- `CreateCategoryFragment`:
  - Propósito: crear categorías (nombre, descripción) y asociar una imagen subida previamente vía `/upload/image`.
  - `onViewCreated`: configura el `MaterialToolbar` con flecha de regreso, el selector de imagen (`ActivityResultContracts.GetContent` con `image/*`) y el botón "Crear categoría".
  - Flujo de creación:
    1) Se sube la imagen seleccionada a `/upload/image` usando `UploadService`.
    2) Se transforma la respuesta `ProductImage` a `ImagePayload`.
    3) Se envía `CategoryCreateRequest` a `POST /category` usando `CategoryService`.
  - UI: usa `FrameLayout` + `ScrollView` y un `ProgressBar` centrado como overlay (`id: progress`) controlado por `setLoading(true/false)`.
 - `ImageViewerDialogFragment` (visor de imágenes):
   - Propósito: DialogFragment a pantalla completa que muestra una lista de imágenes con `ViewPager2`.
   - `onCreate`: lee `ARG_IMAGES` (lista de URLs) y `ARG_INDEX` (página inicial), fija estilo full-screen.
   - `onCreateView`: infla `dialog_image_viewer` que contiene el `ViewPager2` (`R.id.pagerImages`).
   - `onViewCreated`: configura el `ViewPager2` con `ImagePagerAdapter`, posiciona en `startIndex`, y añade `view.setOnClickListener { dismiss() }` para cerrar al tocar el fondo.
   - `ImagePagerAdapter`: crea `ImageView` a partir de `item_fullscreen_image` y usa `Coil.load(url)` en el `ViewHolder`.
   - `newInstance(urls, startIndex)`: método fábrica que empaqueta argumentos y retorna el fragment listo para `show(fm, "image_viewer")`.
   - Uso típico: `ImageViewerDialogFragment.newInstance(urls, 0).show(supportFragmentManager, "image_viewer")`.

## Menú lateral (NavigationView)
- `nav_drawer_menu.xml`: Se configura el menú (home, products, profile, add product, logout) y se asigna en el layout con `app:menu="@menu/nav_drawer_menu"`.
- `HomeActivity`: Usa `setNavigationItemSelectedListener` para navegar según el ítem seleccionado; marca el ítem activo y cierra el drawer.
- Ítems adicionales:
  - `nav_create_category`: abre `CreateCategoryFragment` para crear categorías.
- `LogoutActivity`: Usa el mismo menú para consistencia visual; marca `nav_logout` como seleccionado.

## Glosario rápido
- `Authorization` (Bearer): Encabezado HTTP con token JWT; lo agrega `AuthInterceptor`.
- `@Body`: Indica que Retrofit serializa el data class a JSON en el cuerpo del POST.
- `@Multipart` / `@Part`: Envío de binarios como multipart/form-data.
- `MIME`: Tipo de contenido del archivo (`image/jpeg`); usado al crear `RequestBody` y nombres.
- `this@RegisterActivity`: Referencia explícita al `Context` de la Activity en una lambda/ámbito anidado.

## RecyclerView, Adapter y ViewHolder
- `RecyclerView`: componente UI de AndroidX para mostrar listas o rejillas de forma eficiente. Recicla las vistas de ítem (no las crea/desecha constantemente) y delega el posicionamiento a un `LayoutManager` (lineal, grid, etc.). Se alimenta mediante un `Adapter` que crea y vincula ítems.
- `Adapter`: puente entre tus datos y la UI del `RecyclerView`. Define cómo crear cada ítem (`onCreateViewHolder`) y cómo enlazar datos a ese ítem (`onBindViewHolder`). También gestiona tipos de vista y notifica cambios (`notifyDataSetChanged`, `notifyItemInserted`, etc.).
- `ViewHolder`: objeto que mantiene referencias a las vistas del ítem (por ejemplo, `ImageView`, `TextView`). Evita búsquedas repetidas (`findViewById`) y expone un método de enlace (bind) para dibujar datos en el ítem.

### Uso en este proyecto
- `ProductsFragment` + `ProductAdapter`:
  - `ProductsFragment` carga datos desde `ProductService.getProducts()` y los muestra con un `RecyclerView` usando `GridLayoutManager` (rejilla).
  - `ProductAdapter` extiende `RecyclerView.Adapter` y maneja dos tipos de ítems: un header (barra de búsqueda) y la tarjeta de producto. En el `ViewHolder` de producto se enlazan nombre, precio e imagen (con Coil), además de manejar cantidad y el botón de “Añadir al carrito”.
- `ImageViewerDialogFragment` (con `ViewPager2`):
  - `ViewPager2` está basado internamente en `RecyclerView`. Aquí definimos `ImagePagerAdapter : RecyclerView.Adapter<ImageViewHolder>` y un `ImageViewHolder` que infla `item_fullscreen_image`.
  - En `onBindViewHolder` se llama a `imageView.load(url)` (Coil) para cargar la imagen de cada página.

En resumen: `RecyclerView` organiza la lista/galería, el `Adapter` traduce tus datos en vistas, y el `ViewHolder` optimiza y aplica los datos a cada ítem. `ViewPager2` reusa esta misma arquitectura de forma interna.

## Flecha de retroceso (arrow_back)
- Recurso: se añadió `ic_arrow_back.xml` en `app/src/main/res/drawable/` (vector de 24dp) para usarlo como icono de navegación.
- Layout: en `fragment_add_product.xml` se insertó un `MaterialToolbar` al inicio del contenido y se configuró `app:navigationIcon="@drawable/ic_arrow_back"`.
- Lógica: en `AddProductFragment.onViewCreated` se conectó el clic de la flecha con `requireActivity().onBackPressedDispatcher.onBackPressed()` para volver a la pantalla anterior.
- Alternativa: si en el futuro se usa Navigation Component, la acción puede ser `findNavController().popBackStack()`.
## Archivos comentados
- `api/RetrofitClient.kt`: Explicación de clientes, baseUrl, interceptor y servicios.
- `api/AuthInterceptor.kt`: Rol del interceptor.
- `api/AuthService.kt`: Uso de `@Body` y endpoints.
- `api/ProductService.kt`: Endpoints de productos.
- `api/UploadService.kt`: Subida multipart.
- `api/CategoryService.kt`: Endpoint de creación de categorías.
- `ui/CreateCategoryFragment.kt`: Flujo de subir imagen y crear categoría.
- `model/Category.kt`, `model/CategoryCreateRequest.kt`, `model/CategoryCreateResponse.kt`: Modelos para categorías y request/response.
- `ui/RegisterActivity.kt`: Flujo y validaciones.
- `ui/ProfileFragment.kt`: Binding y memoria.
- `ui/AddProductFragment.kt`: Selección/subida/mapeo de imágenes.
- `ui/HomeActivity.kt`: Manejo del menú lateral.
- `ui/LogoutActivity.kt`: Consistencia de menú y selección.
- `model/CreateProductFullRequest.kt`: Campos y relación con `ImagePayload`.
- `model/CreateProductResponse.kt`: Rol como respuesta.
- `model/ProductImage.kt`: Datos de imagen subida.
- `model/ImagePayload.kt`: Referencia a imágenes subidas.

Si quieres que lleve estos comentarios a otras partes específicas del proyecto o ampliar alguna sección del README, dímelo y lo hago.