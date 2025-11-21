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
import com.example.myapplication.model.User
import com.example.myapplication.model.UserUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var currentUser: User? = null

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
        binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        binding.btnCancel.setOnClickListener { restoreFields() }
        binding.btnSave.setOnClickListener { submitUpdate() }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createAuthServiceAuthenticated(requireContext())
                val me = withContext(Dispatchers.IO) { service.me() }
                currentUser = me

                val headerName = me.name ?: ""
                binding.tvHeaderName.text = headerName

                // Métricos
                binding.tvPurchases.text = "02"
                binding.tvPoints.text = "200"
                binding.tvCoupons.text = "01"

                binding.etFirstName.setText(me.firstName ?: "")
                binding.etLastName.setText(me.lastName ?: "")
                binding.tvEmailValue.text = me.email ?: ""
                binding.etShippingAddress.setText(me.shippingAddress ?: "")
                binding.etPhone.setText(me.phone ?: "")
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

                binding.etFirstName.setText(first)
                binding.etLastName.setText(last)
                binding.tvEmailValue.text = tm.getEmail().orEmpty()
                binding.etShippingAddress.setText("")
                binding.etPhone.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
        binding.btnCancel.isEnabled = !loading
    }

    private fun restoreFields() {
        val u = currentUser
        if (u != null) {
            binding.etFirstName.setText(u.firstName ?: "")
            binding.etLastName.setText(u.lastName ?: "")
            binding.etShippingAddress.setText(u.shippingAddress ?: "")
            binding.etPhone.setText(u.phone ?: "")
        }
    }

    private fun submitUpdate() {
        val tm = TokenManager(requireContext())
        val id = tm.getUserId() ?: currentUser?.id
        if (id == null) {
            android.widget.Toast.makeText(requireContext(), "Usuario no identificado", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val req = UserUpdateRequest(
            name = null,
            email = null,
            avatar = null,
            blocked = null,
            firstName = binding.etFirstName.text?.toString()?.trim().orEmpty().takeIf { it.isNotEmpty() },
            lastName = binding.etLastName.text?.toString()?.trim().orEmpty().takeIf { it.isNotEmpty() },
            role = null,
            status = null,
            shippingAddress = binding.etShippingAddress.text?.toString()?.trim().orEmpty().takeIf { it.isNotEmpty() },
            phone = binding.etPhone.text?.toString()?.trim().orEmpty().takeIf { it.isNotEmpty() }
        )
        if (!validate(req)) return
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val svcPrimary = RetrofitClient.createUserService(requireContext())
                val user = withContext(Dispatchers.IO) { svcPrimary.update(id, req) }
                currentUser = user
                android.widget.Toast.makeText(requireContext(), "Perfil actualizado", android.widget.Toast.LENGTH_SHORT).show()
                restoreFields()
            } catch (e: Exception) {
                try {
                    val svcAlt = RetrofitClient.createUserServiceAuth(requireContext())
                    val user = withContext(Dispatchers.IO) { svcAlt.update(id, req) }
                    currentUser = user
                    android.widget.Toast.makeText(requireContext(), "Perfil actualizado", android.widget.Toast.LENGTH_SHORT).show()
                    restoreFields()
                } catch (e2: Exception) {
                    android.widget.Toast.makeText(requireContext(), com.example.myapplication.api.NetworkError.message(e2), android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                setLoading(false)
            }
        }
    }

    private fun validate(req: UserUpdateRequest): Boolean {
        val phone = req.phone?.filter { it.isDigit() } ?: ""
        if (phone.isNotEmpty() && phone.length < 7) {
            android.widget.Toast.makeText(requireContext(), "Teléfono inválido", android.widget.Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}