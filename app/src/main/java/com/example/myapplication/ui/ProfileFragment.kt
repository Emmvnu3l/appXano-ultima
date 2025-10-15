package com.example.myapplication.ui

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
                val service = RetrofitClient.createAuthService(requireContext())
                val me = withContext(Dispatchers.IO) { service.me() }
                binding.tvFirstName.text = "Nombre(s): ${me.firstName ?: ""}"
                binding.tvLastName.text = "Apellidos: ${me.lastName ?: ""}"
                binding.tvEmail.text = "Email: ${me.email ?: ""}"
                binding.tvShippingAddress.text = "Dirección de envío: ${me.shippingAddress ?: ""}"
                binding.tvPhone.text = "Teléfono: ${me.phone ?: ""}"
            } catch (e: Exception) {
                // Fallback a datos locales si falla la consulta
                val tm = TokenManager(requireContext())
                val name = tm.getName().orEmpty()
                val parts = name.trim().split(" ")
                val first = parts.firstOrNull().orEmpty()
                val last = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
                binding.tvFirstName.text = "Nombre(s): $first"
                binding.tvLastName.text = "Apellidos: $last"
                binding.tvEmail.text = "Email: ${tm.getEmail().orEmpty()}"
                binding.tvShippingAddress.text = "Dirección de envío: "
                binding.tvPhone.text = "Teléfono: "
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}