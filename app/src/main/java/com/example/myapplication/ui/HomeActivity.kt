package com.example.myapplication.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private val tagHome = "HomeFragment"
    private val tagProducts = "ProductsFragment"
    private val tagCategories = "CategoriesFragment"
    private val tagProfile = "ProfileFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        try {
            binding = ActivityHomeBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (t: Throwable) {
            android.util.Log.e("HomeActivity", "init failed", t)
            val tv = android.widget.TextView(this).apply {
                text = t.localizedMessage ?: "Error"
                setBackgroundColor(android.graphics.Color.WHITE)
                setTextColor(android.graphics.Color.RED)
                textSize = 16f
                gravity = android.view.Gravity.CENTER
            }
            setContentView(tv)
            return
        }
        val tm = com.example.myapplication.api.TokenManager(this)
        val isAdmin = tm.isAdmin()

        if (!isAdmin) {
            // LÓGICA CORREGIDA:
            // Solo ocultamos las opciones de GESTIÓN (Admin).
            // Dejamos visible 'nav_orders' para que el usuario vea sus compras.
            val menu = binding.navView.menu

            // Nota: Si quieres que el usuario vea "Inicio" (Home), borra la siguiente línea también.
            menu.removeItem(R.id.nav_home)

            menu.removeItem(R.id.nav_manage_products)
            menu.removeItem(R.id.nav_manage_categories)
            menu.removeItem(R.id.nav_users)

            // HEMOS ELIMINADO: menu.removeItem(R.id.nav_orders)
            // Ahora los pedidos son visibles para todos.
        }

        // Configurar Toolbar + Drawer toggle
        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configurar visibilidad y click del botón de carrito en la toolbar
        val actionCart = binding.toolbar.findViewById<View>(R.id.actionCart)
        if (isAdmin) {
            actionCart?.visibility = View.GONE
        } else {
            actionCart?.visibility = View.VISIBLE
            actionCart?.setOnClickListener { NavigationHelper.openCart(this) }
        }

        // Inicializa con estado del fragmento actual más adelante

        // Abrir destinos iniciales según extras
        val productsQuery = intent?.getStringExtra("products_query")
        val openProducts = intent?.getBooleanExtra("open_products", false) ?: false
        val openAddProduct = intent?.getBooleanExtra("open_add_product", false) ?: false
        val openCreateCategory = intent?.getBooleanExtra("open_create_category", false) ?: false
        val openProfileDetails = intent?.getBooleanExtra("open_profile_details", false) ?: false

        when {
            openCreateCategory -> {
                switchToFragment(tagCategories) { CategoriesFragment() }
                supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .replace(binding.fragmentContainer.id, CreateCategoryFragment())
                    .commit()
            }
            openAddProduct -> {
                switchToFragment(tagProducts) { ProductsFragment.newInstance(null) }
                supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .replace(binding.fragmentContainer.id, AddProductFragment())
                    .commit()
            }
            openProfileDetails -> {
                switchToFragment(tagProfile) { ProfileFragment() }
                binding.navView.setCheckedItem(R.id.nav_profile)
                supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .replace(binding.fragmentContainer.id, ProfileDetailsFragment())
                    .commit()
            }
            intent.getBooleanExtra("open_edit_product", false) -> {
                val p = intent.getSerializableExtra("product") as? com.example.myapplication.model.Product
                if (p != null) {
                    switchToFragment(tagProducts) { ProductsFragment.newInstance(null) }
                    supportFragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .replace(binding.fragmentContainer.id, EditProductFragment.newInstance(p))
                        .commit()
                }
            }
            intent.getBooleanExtra("open_orders", false) -> {
                val frag = OrdersFragment()
                val args = android.os.Bundle()
                val limit = intent.getIntExtra("orders_limit", -1)
                if (limit > 0) args.putInt("limit", limit)
                frag.arguments = args
                switchToFragment("OrdersFragment") { frag }
                binding.navView.setCheckedItem(R.id.nav_orders)
            }
            intent.getBooleanExtra("open_users", false) -> {
                switchToFragment("UsersFragment") { UsersFragment() }
                binding.navView.setCheckedItem(R.id.nav_users)
            }
            openProducts || !productsQuery.isNullOrEmpty() -> {
                switchToFragment(tagProducts) { ProductsFragment.newInstance(productsQuery) }
                binding.navView.setCheckedItem(R.id.nav_products)
            }
            else -> {
                // Vista por defecto
                switchToFragment(tagCategories) { CategoriesFragment() }
                binding.navView.setCheckedItem(R.id.nav_categories)
            }
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.nav_home -> switchToFragment(tagHome) { HomeFragment() }
                R.id.nav_products -> switchToFragment(tagProducts) { ProductsFragment.newInstance(null) }
                R.id.nav_cart -> { NavigationHelper.openCart(this); true }
                R.id.nav_categories -> { NavigationHelper.openCategories(this); true }
                R.id.nav_profile -> switchToFragment(tagProfile) { ProfileFragment() }

                // Opciones solo para Admin
                R.id.nav_manage_products -> if (isAdmin) switchToFragment("ProductManagementFragment") { ProductManagementFragment() } else false
                R.id.nav_manage_categories -> if (isAdmin) switchToFragment("CategoryManagementFragment") { CategoryManagementFragment() } else false
                R.id.nav_users -> if (isAdmin) switchToFragment("UsersFragment") { UsersFragment() } else false

                // CORREGIDO: Orders ahora es accesible para todos (quitamos el "if isAdmin")
                R.id.nav_orders -> switchToFragment("OrdersFragment") { OrdersFragment() }

                R.id.nav_logout -> {
                    startActivity(android.content.Intent(this, LogoutActivity::class.java))
                    true
                }
                else -> false
            }
            if (handled) {
                binding.navView.setCheckedItem(item.itemId)
                binding.drawerLayout.closeDrawers()
            }
            handled
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        if (isFinishing || isDestroyed) return false
        if (supportFragmentManager.isStateSaved) return false
        hideKeyboard()
        return try {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(binding.fragmentContainer.id, fragment)
                .commit()
            configureToolbarForFragment(fragment)
            configureFabForFragment(fragment)
            logMem("replaceFragment")
            true
        } catch (t: Throwable) {
            android.util.Log.e("HomeActivity", "replaceFragment failed", t)
            false
        }
    }

    private fun switchToFragment(tag: String, supplier: () -> Fragment): Boolean {
        if (isFinishing || isDestroyed) return false
        if (supportFragmentManager.isStateSaved) return false
        hideKeyboard()
        val fm = supportFragmentManager
        val containerId = binding.fragmentContainer.id
        val tx = fm.beginTransaction().setReorderingAllowed(true)
        var target = fm.findFragmentByTag(tag)
        if (target == null) {
            target = supplier()
            tx.add(containerId, target, tag)
        }
        fm.fragments.forEach { f ->
            if (f.isAdded && f !== target) tx.hide(f)
        }
        return try {
            tx.show(target).commit()
            configureToolbarForFragment(target)
            configureFabForFragment(target)
            logMem("switchToFragment:$tag")
            true
        } catch (t: Throwable) {
            android.util.Log.e("HomeActivity", "switchToFragment failed", t)
            false
        }
    }

    private fun configureToolbarForFragment(fragment: Fragment) {
        val childScreen = fragment is CreateCategoryFragment || fragment is AddProductFragment || fragment is EditProductFragment
        if (childScreen) {
            toggle.isDrawerIndicatorEnabled = false
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.toolbar.navigationIcon = AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back)
            binding.toolbar.setNavigationOnClickListener {
                if (fragment is AddProductFragment || fragment is EditProductFragment) {
                    switchToFragment("ProductManagementFragment") { ProductManagementFragment() }
                    binding.navView.setCheckedItem(R.id.nav_manage_products)
                    restoreHamburger()
                } else if (fragment is CreateCategoryFragment) {
                    switchToFragment("CategoryManagementFragment") { CategoryManagementFragment() }
                    binding.navView.setCheckedItem(R.id.nav_manage_categories)
                    restoreHamburger()
                } else {
                    switchToFragment(tagCategories) { CategoriesFragment() }
                    binding.navView.setCheckedItem(R.id.nav_categories)
                    restoreHamburger()
                }
            }
        } else {
            restoreHamburger()
        }

        val actionCart = binding.toolbar.findViewById<View>(R.id.actionCart)
        if (fragment is CartFragment) {
            actionCart?.visibility = View.GONE
        } else {
            val isAdminToolbar = com.example.myapplication.api.TokenManager(this).isAdmin()
            if (isAdminToolbar) {
                actionCart?.visibility = View.GONE
            } else {
                actionCart?.visibility = View.VISIBLE
                actionCart?.setOnClickListener { NavigationHelper.openCart(this) }
            }
        }
    }

    private fun restoreHamburger() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        toggle.isDrawerIndicatorEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.toolbar.navigationIcon = null
        toggle.syncState()
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            val view = currentFocus ?: binding.root
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (_: Exception) {}
    }

    private fun logMem(source: String) {
        val rt = Runtime.getRuntime()
        val max = rt.maxMemory() / (1024 * 1024)
        val total = rt.totalMemory() / (1024 * 1024)
        val free = rt.freeMemory() / (1024 * 1024)
        android.util.Log.i("HomeActivity", "$source mem MB max=$max total=$total free=$free")
    }

    fun showOverlayLoading() {
        try { StateUi.showLoading(binding.stateOverlay) } catch (_: Exception) {}
    }
    fun showOverlayError(message: String) {
        try { StateUi.showError(binding.stateOverlay, message, showRetry = false) } catch (_: Exception) {}
    }
    fun hideOverlay() {
        try { StateUi.hide(binding.stateOverlay) } catch (_: Exception) {}
    }

    private fun configureFabForFragment(fragment: Fragment) {
        val fab = binding.fabProfile
        when (fragment) {
            is ProfileFragment -> {
                fab.visibility = View.VISIBLE
                fab.setImageResource(R.drawable.ic_user)
                fab.contentDescription = "Abrir perfil"
                val badge = fab.getTag(R.id.tag_cart_badge) as? com.google.android.material.badge.BadgeDrawable
                badge?.isVisible = false
                val listener = fab.getTag(R.id.tag_cart_prefs_listener) as? android.content.SharedPreferences.OnSharedPreferenceChangeListener
                if (listener != null) {
                    try {
                        val cm = CartManager(this)
                        cm.unregisterListener(listener)
                    } catch (_: Exception) {}
                }
                fab.setOnClickListener { NavigationHelper.openProfileDetails(this) }
                fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
            }
            is CategoriesFragment -> {
                fab.visibility = View.VISIBLE
                fab.setImageResource(R.drawable.ic_cart)
                fab.contentDescription = "Abrir carrito"
                NavigationHelper.setupCartFab(this, fab)
                fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
            }
            is ProductsFragment -> {
                fab.visibility = View.VISIBLE
                fab.setImageResource(R.drawable.ic_cart)
                fab.contentDescription = "Abrir carrito"
                NavigationHelper.setupCartFab(this, fab)
                fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
            }
            else -> {
                fab.visibility = View.GONE
            }
        }
    }
    companion object {
        fun fabModeForFragment(fragment: Fragment): String {
            return when (fragment) {
                is ProfileFragment -> "profile"
                is CategoriesFragment -> "cart"
                is ProductsFragment -> "cart"
                else -> "hidden"
            }
        }
    }
}
