package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.api.NetworkError
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.FragmentProfileDetailsBinding
import com.example.myapplication.model.UserUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileDetailsFragment : Fragment() {
    private var _binding: FragmentProfileDetailsBinding? = null
    private val binding get() = _binding!!

    private var selectedRegionId: Int? = null
    private var selectedComunaId: Int? = null
    private var regionsCache: List<com.example.myapplication.model.Region> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        setupValidation()
        setupRegionComuna()
        loadDraft()
        prefillFromUser()

        binding.btnCancel.setOnClickListener { loadDraft() }
        binding.btnSave.setOnClickListener { submit() }
    }

    private fun setupValidation() {
        val draftWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { saveDraft() }
        }
        binding.etName.addTextChangedListener(draftWatcher)

        binding.etEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val txt = s?.toString().orEmpty()
                val ok = android.util.Patterns.EMAIL_ADDRESS.matcher(txt).matches()
                binding.tilEmail.error = if (ok || txt.isEmpty()) null else "Email inválido"
                saveDraft()
            }
        })

        binding.etPhone.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val digits = s?.toString()?.filter { it.isDigit() } ?: ""
                binding.tilPhone.error = if (digits.isEmpty() || digits.length >= 7) null else "Teléfono inválido"
                saveDraft()
            }
        })

        binding.etAddress.addTextChangedListener(draftWatcher)
    }

    private fun setupRegionComuna() {
        lifecycleScope.launch {
            try {
                val geo = RetrofitClient.createGeoService(requireContext())
                val regions = withContext(Dispatchers.IO) { geo.getRegions() }
                regionsCache = regions
                val regionNames = regions.map { it.name }
                val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, regionNames)
                binding.spRegion.setAdapter(adapter)
                binding.spRegion.setOnItemClickListener { _, _, pos, _ ->
                    val r = regions[pos]
                    selectedRegionId = r.id
                    selectedComunaId = null
                    binding.spComuna.setText("", false)
                    lifecycleScope.launch { loadComunas(r.id) }
                    saveDraft()
                }
            } catch (_: Exception) { }
        }
    }

    private suspend fun loadComunas(regionId: Int) {
        try {
            val geo = RetrofitClient.createGeoService(requireContext())
            val comunas = withContext(Dispatchers.IO) { geo.getComunas(regionId) }
            val names = comunas.map { it.name }
            val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
            binding.spComuna.setAdapter(adapter)
            binding.spComuna.setOnItemClickListener { _, _, pos, _ ->
                selectedComunaId = comunas[pos].id
                saveDraft()
            }
            val target = selectedComunaId
            if (target != null) {
                val idx = comunas.indexOfFirst { it.id == target }
                if (idx >= 0) binding.spComuna.setText(comunas[idx].name, false)
            }
        } catch (_: Exception) { }
    }

    private fun prefillFromUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val svc = RetrofitClient.createAuthServiceAuthenticated(requireContext())
                val me = withContext(Dispatchers.IO) { svc.me() }
                val name = me.name ?: ""
                val email = me.email ?: ""
                val phone = me.phone ?: ""
                val address = me.shippingAddress ?: ""
                if (binding.etName.text.isNullOrEmpty()) binding.etName.setText(name)
                if (binding.etEmail.text.isNullOrEmpty()) binding.etEmail.setText(email)
                if (binding.etPhone.text.isNullOrEmpty()) binding.etPhone.setText(phone)
                if (binding.etAddress.text.isNullOrEmpty()) binding.etAddress.setText(address)

                val comunaId = me.comuna
                if (comunaId != null) {
                    val geo = RetrofitClient.createGeoService(requireContext())
                    val comuna = withContext(Dispatchers.IO) { geo.getComuna(comunaId) }
                    selectedComunaId = comuna.id
                    selectedRegionId = comuna.regionId
                    val regions = regionsCache
                    if (regions.isEmpty()) {
                        val fetched = withContext(Dispatchers.IO) { geo.getRegions() }
                        regionsCache = fetched
                    }
                    val region = regionsCache.firstOrNull { it.id == selectedRegionId }
                    if (region != null) {
                        binding.spRegion.setText(region.name, false)
                        loadComunas(region.id)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun submit() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val address = binding.etAddress.text?.toString()?.trim().orEmpty()
        val req = UserUpdateRequest(
            name = name.takeIf { it.isNotEmpty() },
            email = email.takeIf { it.isNotEmpty() },
            avatar = null,
            blocked = null,
            firstName = null,
            lastName = null,
            role = null,
            status = null,
            shippingAddress = address.takeIf { it.isNotEmpty() },
            phone = phone.takeIf { it.isNotEmpty() },
            comuna = selectedComunaId
        )

        val phoneDigits = phone.filter { it.isDigit() }
        if (phoneDigits.isNotEmpty() && phoneDigits.length < 7) {
            binding.tilPhone.error = "Teléfono inválido"
            return
        }
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email inválido"
            return
        }

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tm = TokenManager(requireContext())
                val id = tm.getUserId()
                if (id != null) {
                    val svc = RetrofitClient.createUserService(requireContext())
                    withContext(Dispatchers.IO) { svc.update(id, req) }
                    android.widget.Toast.makeText(requireContext(), "Cambios guardados", android.widget.Toast.LENGTH_SHORT).show()
                    clearDraft()
                    parentFragmentManager.popBackStack()
                } else {
                    val authed = RetrofitClient.createAuthServiceAuthenticated(requireContext())
                    withContext(Dispatchers.IO) { authed.updateMe(req) }
                    android.widget.Toast.makeText(requireContext(), "Cambios guardados", android.widget.Toast.LENGTH_SHORT).show()
                    clearDraft()
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), NetworkError.message(e), android.widget.Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
        binding.btnCancel.isEnabled = !loading
    }

    private fun saveDraft() {
        val prefs = requireContext().getSharedPreferences("profile_draft", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("name", binding.etName.text?.toString())
            .putString("email", binding.etEmail.text?.toString())
            .putString("phone", binding.etPhone.text?.toString())
            .putString("address", binding.etAddress.text?.toString())
            .putInt("region", selectedRegionId ?: -1)
            .putInt("comuna", selectedComunaId ?: -1)
            .apply()
    }

    private fun loadDraft() {
        val prefs = requireContext().getSharedPreferences("profile_draft", android.content.Context.MODE_PRIVATE)
        binding.etName.setText(prefs.getString("name", "") ?: "")
        binding.etEmail.setText(prefs.getString("email", "") ?: "")
        binding.etPhone.setText(prefs.getString("phone", "") ?: "")
        binding.etAddress.setText(prefs.getString("address", "") ?: "")
        selectedRegionId = prefs.getInt("region", -1).let { if (it >= 0) it else null }
        selectedComunaId = prefs.getInt("comuna", -1).let { if (it >= 0) it else null }
    }

    private fun clearDraft() {
        val prefs = requireContext().getSharedPreferences("profile_draft", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
