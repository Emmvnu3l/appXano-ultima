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
        adapter = UsersAdapter(
            onToggleBlocked = { u, checked -> confirmBlockToggle(u, checked) },
            onEdit = { showEditDialog(it) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refresh() }

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

        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dy > 0) {
                    val lm = rv.layoutManager as LinearLayoutManager
                    val visible = lm.childCount
                    val total = lm.itemCount
                    val first = lm.findFirstVisibleItemPosition()
                    if (!loading && !endReached && visible + first >= total - 2) loadMore()
                }
            }
        })

        refresh()
    }

    private fun setLoading(loading: Boolean) {
        this.loading = loading
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.swipeRefresh.isRefreshing = false
    }

    private fun refresh() {
        page = 1
        endReached = false
        fetch(page, append = false)
    }

    private fun loadMore() {
        if (loading) return
        fetch(page + 1, append = true)
    }

    private fun fetch(targetPage: Int, append: Boolean) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createUserService(requireContext())
                val list = withContext(Dispatchers.IO) {
                    val q = currentQuery
                    if (q != null) service.search(q, targetPage, 10) else service.list(targetPage, 10, null)
                }
                if (list.isEmpty()) endReached = true else page = targetPage
                adapter.setData(list, append)
                binding.tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = "Error cargando usuarios: ${e.message}"
            } finally {
                setLoading(false)
            }
        }
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
                withContext(Dispatchers.IO) { if (checked) service.block(u.id) else service.unblock(u.id) }
            } catch (e: Exception) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = "Acción fallida: ${e.message}"
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
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = "Edición fallida: ${e.message}"
                } finally {
                    setLoading(false)
                    refresh()
                }
            }
        }
        dlg.show(parentFragmentManager, "editUser")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}