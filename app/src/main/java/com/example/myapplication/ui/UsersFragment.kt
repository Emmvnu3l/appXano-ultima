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
            onChangeStatus = { u, status -> confirmChangeStatus(u, status) },
            onEdit = { showEditDialog(it) },
            onView = { showDetail(it) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refresh() }
        binding.state.btnRetry.setOnClickListener { refresh() }

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

        val statuses = listOf("", "active", "disconnected", "blocked")
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
                if (!isOnline()) {
                    binding.state.tvError.visibility = View.VISIBLE
                    binding.state.tvError.text = "Sin conexión a Internet"
                    return@launch
                }
                val tm = TokenManager(requireContext())
                android.util.Log.d("UsersFragment", "Fetching users... Token present: ${!tm.getToken().isNullOrBlank()}")
                
                val list = withContext(Dispatchers.IO) {
                    val key = (currentQuery ?: "") + "|" + itemsPerPage
                    val cached = pageCache[key]
                    val cachedPage = cached?.pages?.get(targetPage)
                    if (cachedPage != null) {
                        totalUsers = cached.total
                        cachedPage
                    } else {
                        val svcPrimary = RetrofitClient.createUserService(requireContext())
                        var resp: Response<List<User>> = svcPrimary.list(targetPage, itemsPerPage, null, currentQuery, getStatusFilter())
                        if (!resp.isSuccessful && resp.code() == 404) {
                            android.util.Log.d("UsersFragment", "404 on singular /user, trying plural /users...")
                            resp = svcPrimary.listPlural(targetPage, itemsPerPage, null, currentQuery, getStatusFilter())
                        }
                        
                        if (!resp.isSuccessful) {
                            val errorBody = resp.errorBody()?.string()
                            android.util.Log.e("UsersFragment", "Response not successful: ${resp.code()} - $errorBody")
                            throw HttpException(resp)
                        }

                        var body = resp.body() ?: emptyList()
                        if (body.isEmpty()) {
                            val svc = RetrofitClient.createUserService(requireContext())
                            val offset = (targetPage - 1) * itemsPerPage
                            val alt = svc.listOffsetLimit(offset, itemsPerPage, null, currentQuery, getStatusFilter())
                            if (alt.isSuccessful) {
                                body = alt.body() ?: emptyList()
                            } else {
                                android.util.Log.w("UsersFragment", "Fallback offset/limit failed: ${alt.code()}")
                            }
                        }
                        android.util.Log.d("UsersFragment", "Users fetched: ${body.size}")
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
                if (adapter.itemCount == 0) {
                    binding.state.tvEmpty.text = buildEmptyMessage()
                    binding.state.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.state.tvEmpty.visibility = View.GONE
                    binding.state.tvError.visibility = View.GONE
                }
                updateRangeLabel()
                savePrefs()
            } catch (e: Exception) {
                android.util.Log.e("UsersFragment", "Error fetching users", e)
                binding.state.tvError.visibility = View.VISIBLE
                val msg = com.example.myapplication.api.NetworkError.message(e)
                binding.state.tvError.text = msg
                if (e is HttpException && e.code() == 401) {
                    binding.state.btnRetry.text = "Iniciar sesión"
                    binding.state.btnRetry.visibility = View.VISIBLE
                    binding.state.btnRetry.setOnClickListener {
                        startActivity(android.content.Intent(requireContext(), MainActivity::class.java))
                    }
                }
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

    private fun buildEmptyMessage(): String {
        val q = currentQuery
        val st = getStatusFilter()
        return when {
            !q.isNullOrBlank() && !st.isNullOrBlank() -> "Sin resultados para \"$q\" con estado $st"
            !q.isNullOrBlank() -> "Sin resultados para \"$q\""
            !st.isNullOrBlank() -> "Sin resultados con estado $st"
            else -> "No hay usuarios que mostrar"
        }
    }

    private fun isOnline(): Boolean {
        val cm = requireContext().getSystemService(android.net.ConnectivityManager::class.java)
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun confirmChangeStatus(u: User, status: String) {
        val text = if (status == "blocked") "bloquear" else "desbloquear"
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar")
            .setMessage("¿Deseas ${text} a ${u.name}?")
            .setPositiveButton("Sí") { _, _ -> changeStatus(u, status) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun changeStatus(u: User, status: String) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                android.util.Log.d("UsersFragment", "status change start user=${u.id} -> $status")
                val svc = com.example.myapplication.api.RetrofitClient.createMembersServiceAuthenticated(requireContext())
                val resp = withContext(Dispatchers.IO) {
                    svc.updateUserStatus(com.example.myapplication.api.UpdateUserStatusRequest(
                        user_id = u.id,
                        status = status
                    ))
                }
                if (!resp.isSuccessful) {
                    val msg = resp.errorBody()?.string() ?: "Error"
                    throw retrofit2.HttpException(retrofit2.Response.error<okhttp3.ResponseBody>(resp.code(), okhttp3.ResponseBody.create(null, msg)))
                }
                android.util.Log.d("UsersFragment", "status change success user=${u.id} status=$status")
                adapter.setBlocked(u.id, status == "blocked")
                android.widget.Toast.makeText(requireContext(), "Estado actualizado", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.state.tvError.visibility = View.VISIBLE
                binding.state.tvError.text = com.example.myapplication.api.NetworkError.message(e)
                android.util.Log.e("UsersFragment", "status change error", e)
                adapter.setBlocked(u.id, u.blocked)
            } finally {
                setLoading(false)
                // Evitar flicker: dejamos actualización optimista y recarga manual por el usuario
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
