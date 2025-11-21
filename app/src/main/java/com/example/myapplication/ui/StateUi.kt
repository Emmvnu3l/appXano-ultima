package com.example.myapplication.ui

import android.view.View
import com.example.myapplication.databinding.ViewStateBinding

object StateUi {
    fun showLoading(state: ViewStateBinding) {
        state.root.visibility = View.VISIBLE
        state.progress.visibility = View.VISIBLE
        state.tvError.visibility = View.GONE
        state.tvEmpty.visibility = View.GONE
        state.btnRetry.visibility = View.GONE
    }

    fun showError(state: ViewStateBinding, message: String, showRetry: Boolean = true) {
        state.root.visibility = View.VISIBLE
        state.progress.visibility = View.GONE
        state.tvEmpty.visibility = View.GONE
        state.tvError.visibility = View.VISIBLE
        state.tvError.text = message
        state.btnRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    fun showEmpty(state: ViewStateBinding) {
        state.root.visibility = View.VISIBLE
        state.progress.visibility = View.GONE
        state.tvError.visibility = View.GONE
        state.tvEmpty.visibility = View.VISIBLE
        state.btnRetry.visibility = View.GONE
    }

    fun hide(state: ViewStateBinding) {
        state.root.visibility = View.GONE
        state.progress.visibility = View.GONE
        state.tvError.visibility = View.GONE
        state.tvEmpty.visibility = View.GONE
        state.btnRetry.visibility = View.GONE
    }
}