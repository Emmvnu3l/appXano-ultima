package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.myapplication.R
import android.widget.ImageView

class ImageViewerDialogFragment : DialogFragment() {
    // DialogFragment a pantalla completa para visualizar una lista de imágenes (URLs)
    // usando ViewPager2. Carga las imágenes con Coil y permite iniciar en un índice dado.
    private lateinit var pager: ViewPager2
    private var images: List<String> = emptyList()
    private var startIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lee los argumentos: lista de URLs y el índice inicial.
        // requireArguments() asegura que existan; lanzará si no se pasó Bundle.
        val args = requireArguments()
        images = args.getStringArrayList(ARG_IMAGES)?.toList() ?: emptyList()
        startIndex = args.getInt(ARG_INDEX, 0)
        // Estilo de diálogo: pantalla completa sin barra de título.
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla el layout del visor de imágenes que contiene el ViewPager2 (R.id.pagerImages).
        return inflater.inflate(R.layout.dialog_image_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Configura el ViewPager2 con un adaptador simple basado en ImageView.
        pager = view.findViewById(R.id.pagerImages)
        pager.adapter = ImagePagerAdapter(images)
        // Posiciona el pager en la imagen inicial sin animación.
        pager.setCurrentItem(startIndex, false)

        // Cerrar al tocar en la raíz del diálogo (opcional).
        // Nota: El ViewPager2 consume los toques sobre las imágenes; este listener
        // actúa sobre el resto del fondo del diálogo.
        view.setOnClickListener { dismiss() }
    }

    private class ImagePagerAdapter(private val urls: List<String>) : RecyclerView.Adapter<ImageViewHolder>() {
        // Crea un ImageView a pantalla completa por cada página.
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val iv = LayoutInflater.from(parent.context).inflate(R.layout.item_fullscreen_image, parent, false) as ImageView
            return ImageViewHolder(iv)
        }
        // El número de páginas corresponde al número de URLs.
        override fun getItemCount(): Int = urls.size
        // Carga la URL en el ImageView del ViewHolder usando Coil.
        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.bind(urls[position])
        }
    }

    private class ImageViewHolder(private val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
        // Bind simple: Coil resuelve caché/red y dibuja la imagen.
        // Puedes añadir placeholders o crossfade si lo deseas.
        fun bind(url: String) {
            imageView.load(url)
        }
    }

    companion object {
        private const val ARG_IMAGES = "images"
        private const val ARG_INDEX = "index"
        // Fábrica: crea el fragment con argumentos (lista de URLs y página inicial).
        // Uso típico:
        // ImageViewerDialogFragment.newInstance(urls, 0).show(fragmentManager, "image_viewer")
        fun newInstance(urls: List<String>, startIndex: Int): ImageViewerDialogFragment {
            val f = ImageViewerDialogFragment()
            val b = Bundle()
            b.putStringArrayList(ARG_IMAGES, ArrayList(urls))
            b.putInt(ARG_INDEX, startIndex)
            f.arguments = b
            return f
        }
    }
}