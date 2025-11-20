package com.example.myapplication.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
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
        return AlertDialog.Builder(requireContext())
            .setTitle("Editar usuario")
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val req = UserUpdateRequest(
                    name = binding.etName.text?.toString(),
                    email = binding.etEmail.text?.toString(),
                    avatar = null
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