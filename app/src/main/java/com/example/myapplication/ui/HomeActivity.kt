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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
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
            // Fallback: si por cualquier motivo permanecemos en HomeActivity, restringir menú
            val menu = binding.navView.menu
            menu.removeItem(R.id.nav_home)
            menu.removeItem(R.id.nav_manage_products)
            menu.removeItem(R.id.nav_manage_categories)
            menu.removeItem(R.id.nav_orders)
            menu.removeItem(R.id.nav_users)
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
        
        // Configurar click del botón de carrito en la toolbar
        binding.toolbar.findViewById<View>(R.id.actionCart)?.setOnClickListener {
            NavigationHelper.openCart(this)
        }

        // Abrir destinos iniciales según extras
        val productsQuery = intent?.getStringExtra("products_query")
        val openProducts = intent?.getBooleanExtra("open_products", false) ?: false
        val openAddProduct = intent?.getBooleanExtra("open_add_product", false) ?: false
        val openCreateCategory = intent?.getBooleanExtra("open_create_category", false) ?: false
        when {
            openCreateCategory -> {
                replaceFragment(CreateCategoryFragment())
            }
            openAddProduct -> {
                replaceFragment(AddProductFragment())
                // No marcamos nada específico o quizás products
            }
            intent.getBooleanExtra("open_edit_product", false) -> {
                val p = intent.getSerializableExtra("product") as? com.example.myapplication.model.Product
                if (p != null) {
                    replaceFragment(EditProductFragment.newInstance(p))
                }
            }
            intent.getBooleanExtra("open_orders", false) -> {
                val frag = OrdersFragment()
                val args = android.os.Bundle()
                val limit = intent.getIntExtra("orders_limit", -1)
                if (limit > 0) args.putInt("limit", limit)
                frag.arguments = args
                replaceFragment(frag)
                binding.navView.setCheckedItem(R.id.nav_orders)
            }
            intent.getBooleanExtra("open_users", false) -> {
                replaceFragment(UsersFragment())
                binding.navView.setCheckedItem(R.id.nav_users)
            }
            openProducts || !productsQuery.isNullOrEmpty() -> {
                replaceFragment(ProductsFragment.newInstance(productsQuery))
                binding.navView.setCheckedItem(R.id.nav_products)
            }
            else -> {
                // Cambiado: ahora la vista por defecto es CategoriesFragment
                replaceFragment(CategoriesFragment())
                binding.navView.setCheckedItem(R.id.nav_categories)
            }
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_products -> replaceFragment(ProductsFragment.newInstance(null))
                R.id.nav_categories -> replaceFragment(CategoriesFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                R.id.nav_manage_products -> if (isAdmin) replaceFragment(ProductManagementFragment()) else false
                R.id.nav_manage_categories -> if (isAdmin) replaceFragment(CategoryManagementFragment()) else false
                R.id.nav_orders -> if (isAdmin) replaceFragment(OrdersFragment()) else false
                R.id.nav_users -> if (isAdmin) replaceFragment(UsersFragment()) else false
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
                    // Si hay fragments en la pila, volver atrás
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
        // Limpiar back stack al cambiar de sección principal del menú
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
        configureToolbarForFragment(fragment)
        return true
    }

    private fun configureToolbarForFragment(fragment: Fragment) {
        val childScreen = fragment is CreateCategoryFragment || fragment is AddProductFragment || fragment is EditProductFragment
        if (childScreen) {
            toggle.isDrawerIndicatorEnabled = false
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.toolbar.navigationIcon = AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back)
            binding.toolbar.setNavigationOnClickListener {
                // Volver según el contexto. Si es Edit/Add product, volver a ProductManagement
                if (fragment is AddProductFragment || fragment is EditProductFragment) {
                     replaceFragment(ProductManagementFragment())
                     binding.navView.setCheckedItem(R.id.nav_manage_products)
                     restoreHamburger()
                } else if (fragment is CreateCategoryFragment) {
                     replaceFragment(CategoryManagementFragment())
                     binding.navView.setCheckedItem(R.id.nav_manage_categories)
                     restoreHamburger()
                } else {
                    replaceFragment(CategoriesFragment()) 
                    binding.navView.setCheckedItem(R.id.nav_categories)
                    restoreHamburger()
                }
            }
        } else {
            restoreHamburger()
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
}