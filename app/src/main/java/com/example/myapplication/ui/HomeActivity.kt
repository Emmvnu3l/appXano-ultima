package com.example.myapplication.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
        // Opción 3: desactivar edge-to-edge para evitar solapamientos con barras del sistema.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Abrir destinos iniciales según extras
        val productsQuery = intent?.getStringExtra("products_query")
        val openProducts = intent?.getBooleanExtra("open_products", false) ?: false
        val openAddProduct = intent?.getBooleanExtra("open_add_product", false) ?: false
        val openCreateCategory = intent?.getBooleanExtra("open_create_category", false) ?: false
        when {
            openCreateCategory -> {
                replaceFragment(CreateCategoryFragment())
                binding.navView.setCheckedItem(R.id.nav_create_category)
            }
            openAddProduct -> {
                replaceFragment(AddProductFragment())
                binding.navView.setCheckedItem(R.id.nav_add_product)
            }
            openProducts || !productsQuery.isNullOrEmpty() -> {
                replaceFragment(ProductsFragment.newInstance(productsQuery))
                binding.navView.setCheckedItem(R.id.nav_products)
            }
            else -> {
                // Fragment por defecto
                replaceFragment(HomeFragment())
                binding.navView.setCheckedItem(R.id.nav_home)
            }
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            // El menú lateral (NavigationView) infla @menu/nav_drawer_menu y aquí resolvemos cada item.
            // Al seleccionar, se reemplaza el Fragment y se marca el ítem activo.
            val handled = when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_products -> replaceFragment(ProductsFragment.newInstance(null))
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                R.id.nav_add_product -> replaceFragment(AddProductFragment())
                R.id.nav_create_category -> replaceFragment(CreateCategoryFragment())
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
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        // Usamos el contenedor FragmentContainerView (id: fragmentContainer)
        // Reemplazos sin back stack son adecuados para destinos top-level del Drawer
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
        configureToolbarForFragment(fragment)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun configureToolbarForFragment(fragment: Fragment) {
        val childScreen = fragment is CreateCategoryFragment || fragment is AddProductFragment
        if (childScreen) {
            // Mostrar flecha atrás y bloquear el drawer
            toggle.isDrawerIndicatorEnabled = false
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.toolbar.navigationIcon = AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back)
            binding.toolbar.setNavigationOnClickListener {
                // Volver al Home y restaurar el menú
                replaceFragment(HomeFragment())
                binding.navView.setCheckedItem(R.id.nav_home)
                restoreHamburger()
            }
        } else {
            restoreHamburger()
        }
    }

    private fun restoreHamburger() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        toggle.isDrawerIndicatorEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        // Limpiar cualquier icono manual previo; el toggle aplicará el indicador
        binding.toolbar.navigationIcon = null
        toggle.syncState()
        // Asegurar que el click abra el Drawer en modo hamburguesa
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }
}