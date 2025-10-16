package com.example.myapplication.model
// Payload para asociar imágenes a un producto en la creación/edición.
// Es una referencia (no binario) a lo que se subió antes: path, mime, etc.

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ImagePayload(
    @SerializedName("access") val access: String = "public",
    @SerializedName("path") val path: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("mime") val mime: String? = null,
    @SerializedName("meta") val meta: Map<String, Any> = emptyMap()
) : Serializable