package com.example.myapplication.ui
// Fragment de perfil: obtiene el usuario autenticado vía /auth/me y
// muestra campos con View Binding (por ejemplo, firstName/lastName/email).
// Usa binding.tvFirstNameValue.text = me.firstName ?: "" para evitar nulls en UI.
// En onDestroyView se anula _binding para evitar fugas de memoria.

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createAuthServiceAuthenticated(requireContext())
                val me = withContext(Dispatchers.IO) { service.me() }

                // Encabezado con nombre
                val headerName = listOfNotNull(me.firstName?.takeIf { it.isNotBlank() }, me.lastName?.takeIf { it.isNotBlank() })
                    .takeIf { it.isNotEmpty() }?.joinToString(" ") ?: me.name
                binding.tvHeaderName.text = headerName

                // Métricos (por ahora placeholders; conectar a backend cuando estén disponibles)
                binding.tvPurchases.text = "02"
                binding.tvPoints.text = "200"
                binding.tvCoupons.text = "01"

                // Lista de datos
                binding.tvFirstNameValue.text = me.firstName ?: ""
                binding.tvLastNameValue.text = me.lastName ?: ""
                binding.tvEmailValue.text = me.email ?: ""
                binding.tvShippingAddressValue.text = me.shippingAddress ?: ""
                binding.tvPhoneValue.text = me.phone ?: ""
            } catch (e: Exception) {
                // Fallback a datos locales si falla la consulta
                val tm = TokenManager(requireContext())
                val name = tm.getName().orEmpty()
                val parts = name.trim().split(" ")
                val first = parts.firstOrNull().orEmpty()
                val last = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
                val headerName = listOf(first.takeIf { it.isNotBlank() }, last.takeIf { it.isNotBlank() })
                    .filterNotNull().takeIf { it.isNotEmpty() }?.joinToString(" ") ?: name
                binding.tvHeaderName.text = headerName

                binding.tvPurchases.text = "0"
                binding.tvPoints.text = "0"
                binding.tvCoupons.text = "0"

                binding.tvFirstNameValue.text = first
                binding.tvLastNameValue.text = last
                binding.tvEmailValue.text = tm.getEmail().orEmpty()
                binding.tvShippingAddressValue.text = ""
                binding.tvPhoneValue.text = ""
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}