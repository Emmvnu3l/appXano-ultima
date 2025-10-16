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
    private lateinit var pager: ViewPager2
    private var images: List<String> = emptyList()
    private var startIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        images = args.getStringArrayList(ARG_IMAGES)?.toList() ?: emptyList()
        startIndex = args.getInt(ARG_INDEX, 0)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_image_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pager = view.findViewById(R.id.pagerImages)
        pager.adapter = ImagePagerAdapter(images)
        pager.setCurrentItem(startIndex, false)

        // Cerrar al tocar fuera del paginador (opcional)
        view.setOnClickListener { dismiss() }
    }

    private class ImagePagerAdapter(private val urls: List<String>) : RecyclerView.Adapter<ImageViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val iv = LayoutInflater.from(parent.context).inflate(R.layout.item_fullscreen_image, parent, false) as ImageView
            return ImageViewHolder(iv)
        }
        override fun getItemCount(): Int = urls.size
        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.bind(urls[position])
        }
    }

    private class ImageViewHolder(private val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
        fun bind(url: String) {
            imageView.load(url)
        }
    }

    companion object {
        private const val ARG_IMAGES = "images"
        private const val ARG_INDEX = "index"
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