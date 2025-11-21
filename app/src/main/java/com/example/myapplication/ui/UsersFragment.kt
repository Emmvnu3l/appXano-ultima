package com.example.myapplication.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import retrofit2.HttpException
import retrofit2.Response
import com.example.myapplication.ui.StateUi
import com.example.myapplication.databinding.FragmentUsersBinding
import com.example.myapplication.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsersFragment : Fragment() {
    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: UsersAdapter
    private var page = 1
    private var loading = false
    private var endReached = false
    private var searchJob: Job? = null
    private var currentQuery: String? = null
    private var itemsPerPage: Int = 10
    private var totalUsers: Int? = null

    private data class CacheEntry(var total: Int?, val pages: MutableMap<Int, List<User>>)
    private val pageCache: MutableMap<String, CacheEntry> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        val tm = TokenManager(requireContext())
        val isAdmin = tm.isAdmin()
        if (!isAdmin) {
            binding.recycler.visibility = View.GONE
            binding.state.tvError.visibility = View.VISIBLE
            binding.state.tvError.text = "Acceso restringido a administradores"
            return
        }

        adapter = UsersAdapter(
            onToggleBlocked = { u, checked -> confirmBlockToggle(u, checked) },
            onEdit = { showEditDialog(it) },
            onView = { showDetail(it) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refresh() }

        val sizes = listOf(10, 25, 50)
        val sizeAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sizes)
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spPageSize.adapter = sizeAdapter
        val idx = sizes.indexOf(itemsPerPage).let { if (it >= 0) it else 0 }
        binding.spPageSize.setSelection(idx)
        binding.spPageSize.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newSize = sizes[position]
                if (newSize != itemsPerPage) {
                    itemsPerPage = newSize
                    refresh()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        binding.btnPrev.setOnClickListener {
            if (page > 1) fetch(page - 1, append = false)
        }
        binding.btnNext.setOnClickListener {
            val total = totalUsers
            if (total != null) {
                val maxPage = kotlin.math.max(1, (total + itemsPerPage - 1) / itemsPerPage)
                if (page < maxPage) fetch(page + 1, append = false)
            } else {
                fetch(page + 1, append = false)
            }
        }
        binding.btnGoPage.setOnClickListener {
            val txt = binding.etPage.text?.toString()?.trim()
            val num = txt?.toIntOrNull()
            val total = totalUsers
            val maxPage = if (total != null) kotlin.math.max(1, (total + itemsPerPage - 1) / itemsPerPage) else null
            if (num != null && num >= 1 && (maxPage == null || num <= maxPage)) {
                fetch(num, append = false)
            } else {
                binding.state.tvError.visibility = View.VISIBLE
                binding.state.tvError.text = "Número de página inválido"
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    refresh()
                }
            }
        })

        val prefs = requireContext().getSharedPreferences("users_prefs", android.content.Context.MODE_PRIVATE)
        itemsPerPage = prefs.getInt("pageSize", itemsPerPage)
        val savedPage = prefs.getInt("page", 1)
        binding.etSearch.setText(prefs.getString("search", "") ?: "")
        binding.etNameFilter.setText(prefs.getString("name", "") ?: "")
        binding.etEmailFilter.setText(prefs.getString("email", "") ?: "")

        val statuses = listOf("", "active", "inactive")
        val stAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        stAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spStatusFilter.adapter = stAdapter
        val savedStatus = prefs.getString("status", "") ?: ""
        binding.spStatusFilter.setSelection(statuses.indexOf(savedStatus).let { if (it >= 0) it else 0 })

        binding.btnToggleFilters.setOnClickListener {
            binding.filtersPanel.visibility = if (binding.filtersPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        binding.btnApplyFilters.setOnClickListener {
            savePrefs()
            refresh()
        }

        page = savedPage
        setupSearchAutocomplete()

        refresh()
    }

    private fun setLoading(loading: Boolean) {
        this.loading = loading
        if (loading) StateUi.showLoading(binding.state) else StateUi.hide(binding.state)
        binding.swipeRefresh.isRefreshing = false
        binding.btnPrev.isEnabled = !loading
        binding.btnNext.isEnabled = !loading
        binding.btnGoPage.isEnabled = !loading
    }

    private fun refresh() {
        page = 1
        endReached = false
        fetch(page, append = false)
    }

    // Scroll infinito eliminado para evitar bucle al final de la lista

    private fun fetch(targetPage: Int, append: Boolean) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    val key = (currentQuery ?: "") + "|" + itemsPerPage
                    val cached = pageCache[key]
                    val cachedPage = cached?.pages?.get(targetPage)
                    if (cachedPage != null) {
                        totalUsers = cached.total
                        cachedPage
                    } else {
                        val svcPrimary = RetrofitClient.createUserService(requireContext())
                        val resp: Response<List<User>> = try {
                            svcPrimary.list(targetPage, itemsPerPage, null, currentQuery, getStatusFilter())
                        } catch (e: Exception) {
                            val isNotFound = (e is HttpException && e.code() == 404)
                            if (isNotFound) {
                                val svcAlt = RetrofitClient.createUserServiceAuth(requireContext())
                                svcAlt.list(targetPage, itemsPerPage, null, currentQuery, getStatusFilter())
                            } else throw e
                        }
                        val body = resp.body() ?: emptyList()
                        val headers = resp.headers()
                        totalUsers = parseTotal(headers)
                        val entry = cached ?: CacheEntry(totalUsers, mutableMapOf())
                        entry.total = totalUsers
                        entry.pages[targetPage] = body
                        pageCache[key] = entry
                        body
                    }
                }
                val maxPage = totalUsers?.let { kotlin.math.max(1, (it + itemsPerPage - 1) / itemsPerPage) }
                if (maxPage != null && targetPage > maxPage) {
                    endReached = true
                } else {
                    endReached = list.isEmpty()
                    page = targetPage
                }
                adapter.setData(list, append)
                binding.state.tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                updateRangeLabel()
                savePrefs()
            } catch (e: Exception) {
                binding.state.tvError.visibility = View.VISIBLE
                binding.state.tvError.text = com.example.myapplication.api.NetworkError.message(e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updateRangeLabel() {
        val start = (page - 1) * itemsPerPage + 1
        val end = ((page - 1) * itemsPerPage + adapter.itemCount).coerceAtLeast(start)
        val total = totalUsers
        binding.tvRange.text = if (total != null) "Mostrando ${start}-${end} de ${total} usuarios" else "Mostrando ${start}-${end}"
        val maxPage = total?.let { kotlin.math.max(1, (it + itemsPerPage - 1) / itemsPerPage) } ?: 1
        binding.tvPages.text = "Página ${page} de ${maxPage}"
        binding.btnPrev.isEnabled = page > 1
        binding.btnNext.isEnabled = page < maxPage
    }

    private fun parseTotal(headers: okhttp3.Headers): Int? {
        val h1 = headers["X-Total-Count"] ?: headers["x-total-count"]
        if (!h1.isNullOrBlank()) return h1.toIntOrNull()
        val cr = headers["Content-Range"] ?: headers["content-range"]
        if (!cr.isNullOrBlank()) {
            val slashIdx = cr.lastIndexOf('/')
            if (slashIdx >= 0 && slashIdx + 1 < cr.length) {
                return cr.substring(slashIdx + 1).trim().toIntOrNull()
            }
        }
        return null
    }

    private fun confirmBlockToggle(u: User, checked: Boolean) {
        val text = if (checked) "bloquear" else "desbloquear"
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar")
            .setMessage("¿Deseas ${text} a ${u.name}?")
            .setPositiveButton("Sí") { _, _ -> toggleBlocked(u, checked) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun toggleBlocked(u: User, checked: Boolean) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createUserService(requireContext())
                withContext(Dispatchers.IO) {
                    service.update(u.id, com.example.myapplication.model.UserUpdateRequest(
                        name = null,
                        email = null,
                        avatar = null,
                        blocked = checked
                    ))
                }
            } catch (e: Exception) {
                binding.state.tvError.visibility = View.VISIBLE
                binding.state.tvError.text = com.example.myapplication.api.NetworkError.message(e)
            } finally {
                setLoading(false)
                refresh()
            }
        }
    }

    private fun showEditDialog(u: User) {
        // Para mantener ligereza, mostramos un diálogo simple con edición de nombre/email
        val dlg = EditUserDialog.newInstance(u)
        dlg.onSubmit = { req ->
            setLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.createUserService(requireContext())
                    withContext(Dispatchers.IO) { service.update(u.id, req) }
                } catch (e: Exception) {
                    binding.state.tvError.visibility = View.VISIBLE
                    binding.state.tvError.text = com.example.myapplication.api.NetworkError.message(e)
                } finally {
                    setLoading(false)
                    refresh()
                }
            }
        }
        dlg.show(parentFragmentManager, "editUser")
    }

    private fun showDetail(u: User) {
        val role = u.role ?: ""
        val status = u.status ?: (if (u.blocked) "Bloqueado" else "")
        val created = u.createdAt?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(it)) } ?: ""
        val msg = "Nombre: ${u.name}\nEmail: ${u.email}\nRol: ${role}\nEstado: ${status}\nCreado: ${created}\nDirección: ${u.shippingAddress ?: ""}\nTeléfono: ${u.phone ?: ""}"
        AlertDialog.Builder(requireContext())
            .setTitle("Detalle de usuario")
            .setMessage(msg)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun savePrefs() {
        val prefs = requireContext().getSharedPreferences("users_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("page", page)
            .putInt("pageSize", itemsPerPage)
            .putString("search", binding.etSearch.text?.toString()?.trim())
            .putString("name", binding.etNameFilter.text?.toString()?.trim())
            .putString("email", binding.etEmailFilter.text?.toString()?.trim())
            .putString("status", getStatusFilter() ?: "")
            .apply()
    }

    private fun getStatusFilter(): String? {
        val v = binding.spStatusFilter.selectedItem?.toString()?.trim()
        return if (v.isNullOrEmpty()) null else v
    }

    private fun setupSearchAutocomplete() {
        val suggestions = mutableSetOf<String>()
        pageCache.values.forEach { entry ->
            entry.pages.values.flatten().forEach { u ->
                suggestions.add(u.name)
                suggestions.add(u.email)
            }
        }
        val list = suggestions.filter { it.isNotBlank() }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, list)
        binding.etSearch.setAdapter(adapter)
        binding.etSearch.threshold = 2
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}