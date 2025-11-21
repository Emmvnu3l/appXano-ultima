package com.example.myapplication.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.myapplication.databinding.DialogEditUserBinding
import com.example.myapplication.model.User
import com.example.myapplication.model.UserUpdateRequest

class EditUserDialog : DialogFragment() {
    private lateinit var binding: DialogEditUserBinding
    var onSubmit: ((UserUpdateRequest) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogEditUserBinding.inflate(LayoutInflater.from(requireContext()))
        val user = requireArguments().getSerializable("user") as User
        binding.etName.setText(user.name)
        binding.etEmail.setText(user.email)
        binding.etFirstName.setText(user.firstName ?: "")
        binding.etLastName.setText(user.lastName ?: "")
        val roles = listOf("user", "admin")
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRole.adapter = roleAdapter
        val roleIndex = roles.indexOf(user.role ?: "user").let { if (it >= 0) it else 0 }
        binding.spRole.setSelection(roleIndex)
        val statuses = listOf("active", "inactive")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spStatus.adapter = statusAdapter
        val statusIndex = statuses.indexOf(user.status ?: "active").let { if (it >= 0) it else 0 }
        binding.spStatus.setSelection(statusIndex)
        binding.etShippingAddress.setText(user.shippingAddress ?: "")
        binding.etPhone.setText(user.phone ?: "")
        return AlertDialog.Builder(requireContext())
            .setTitle("Editar usuario")
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val req = UserUpdateRequest(
                    name = binding.etName.text?.toString(),
                    email = binding.etEmail.text?.toString(),
                    avatar = null,
                    blocked = null,
                    firstName = binding.etFirstName.text?.toString(),
                    lastName = binding.etLastName.text?.toString(),
                    role = binding.spRole.selectedItem?.toString(),
                    status = binding.spStatus.selectedItem?.toString(),
                    shippingAddress = binding.etShippingAddress.text?.toString(),
                    phone = binding.etPhone.text?.toString()
                )
                onSubmit?.invoke(req)
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    companion object {
        fun newInstance(u: User): EditUserDialog {
            val d = EditUserDialog()
            val args = Bundle()
            args.putSerializable("user", u)
            d.arguments = args
            return d
        }
    }
}