package com.stevenschoen.putionew

import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.stevenschoen.putionew.cast.BaseCastActivity
import com.stevenschoen.putionew.files.FilesFragment
import com.stevenschoen.putionew.files.FolderLoader
import com.stevenschoen.putionew.fragments.AccountFragment
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.model.transfers.PutioTransfer
import com.stevenschoen.putionew.transfers.TransfersFragment
import com.stevenschoen.putionew.transfers.add.AddTransferActivity
import com.stevenschoen.putionew.tv.TvActivity

class PutioActivity : BaseCastActivity() {

    companion object {
        const val EXTRA_GO_TO_TAB = "go_to_tab"

        const val TAB_ACCOUNT = 0
        const val TAB_FILES = 1
        const val TAB_TRANSFERS = 2
        
        const val FRAGTAG_ACCOUNT = "account"
        const val FRAGTAG_FILES = "files"
        const val FRAGTAG_TRANSFERS = "transfers"

        const val STATE_CURRENT_TAB = "current_tab"
        
        const val noNetworkIntent = "com.stevenschoen.putionew.nonetwork"
    }

    var init = false

    val sharedPrefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    lateinit var bottomNavView: AHBottomNavigation

    lateinit var addTransferView: View
    var showingAddTransferFab = true

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (UIUtils.isTV(this)) {
            val tvIntent = Intent(this, TvActivity::class.java)
            startActivity(tvIntent)
            finish()
            return
        }

        val application = application as PutioApplication
        if (application.isLoggedIn) {
            init(savedInstanceState)
        } else {
            val setupIntent = Intent(this, LoginActivity::class.java)
            startActivity(setupIntent)

            finish()
            return
        }

        intent?.let { handleIntent(it) }

        val noNetworkIntentFilter = IntentFilter(
                PutioActivity.noNetworkIntent)

        registerReceiver(noNetworkReceiver, noNetworkIntentFilter)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (init) {
            val goToTab = intent.getIntExtra(EXTRA_GO_TO_TAB, -1)
            intent.removeExtra(EXTRA_GO_TO_TAB)
            if (goToTab != -1) {
                selectTab(goToTab, true)
            }

            if (intent.action != null) {
                if (intent.action == Intent.ACTION_SEARCH && filesFragment != null) {
                    val query = intent.getStringExtra(SearchManager.QUERY)
                    filesFragment!!.addSearch(query)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(STATE_CURRENT_TAB, bottomNavView.currentItem)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_putio, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_logout -> {
                logOut()
                return true
            }
            R.id.menu_about -> {
                val aboutIntent = Intent(this@PutioActivity, AboutActivity::class.java)
                startActivity(aboutIntent)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun init(savedInstanceState: Bundle?) {
        init = true

        setContentView(R.layout.main)

        if (savedInstanceState == null) {
            val accountFragment = Fragment.instantiate(this, AccountFragment::class.java.name) as AccountFragment
            val filesFragment = FilesFragment.newInstance(this, null)
            val transfersFragment = Fragment.instantiate(this, TransfersFragment::class.java.name) as TransfersFragment
            supportFragmentManager.beginTransaction()
                    .add(R.id.main_content_holder, accountFragment, FRAGTAG_ACCOUNT)
                    .detach(accountFragment)
                    .add(R.id.main_content_holder, filesFragment, FRAGTAG_FILES)
                    .detach(filesFragment)
                    .add(R.id.main_content_holder, transfersFragment, FRAGTAG_TRANSFERS)
                    .detach(transfersFragment)
                    .commitNow()
        }

        setupLayout()

        val navItem = if (savedInstanceState != null) {
            savedInstanceState.getInt(STATE_CURRENT_TAB, TAB_FILES)
        } else {
            TAB_FILES
        }
        selectTab(navItem, false)

        addTransferView = findViewById(R.id.main_addtransfer)
        addTransferView.setOnClickListener {
            var destinationFolder: PutioFile? = null
            if (bottomNavView.currentItem == TAB_FILES) {
                destinationFolder = filesFragment!!.currentPage!!.file
            }
            val addTransferIntent = Intent(this@PutioActivity, AddTransferActivity::class.java)
            if (destinationFolder != null) {
                addTransferIntent.putExtra(AddTransferActivity.EXTRA_PRECHOSEN_DESTINATION_FOLDER, destinationFolder)
            }
            startActivity(addTransferIntent)
        }
        updateAddTransferFab(false)
    }

    override fun onAttachFragment(fragment: Fragment?) {
        super.onAttachFragment(fragment)
        if (fragment!!.tag != null) {
            when (fragment.tag) {
                FRAGTAG_FILES -> (fragment as FilesFragment).callbacks = object : FilesFragment.Callbacks {
                    override fun onSelectionStarted() {
                        if (init) {
                            updateAddTransferFab(true)
                        }
                    }

                    override fun onSelectionEnded() {
                        updateAddTransferFab(true)
                    }

                    override fun onCurrentFileChanged() {
                        updateAddTransferFab(true)
                    }
                }
                FRAGTAG_TRANSFERS -> {
                    (fragment as TransfersFragment).callbacks = object : TransfersFragment.Callbacks {
                        override fun onTransferSelected(transfer: PutioTransfer) {
                            showFilesAndGoToFile(transfer.saveParentId, transfer.fileId)
                        }
                    }
                }
            }
        }
    }

    private fun updateAddTransferFab(animate: Boolean) {
        val shouldShow = shouldShowAddTransferFab()
        if (shouldShow && !showingAddTransferFab) {
            showingAddTransferFab = true
            if (animate) {
                addTransferView.animate()
                        .setInterpolator(FastOutSlowInInterpolator())
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
            } else {
                addTransferView.alpha = 1f
                addTransferView.scaleX = 1f
                addTransferView.scaleY = 1f
            }
            addTransferView.isEnabled = true
            addTransferView.isFocusable = true
            addTransferView.isClickable = true
        } else if (!shouldShow && showingAddTransferFab) {
            showingAddTransferFab = false
            if (animate) {
                addTransferView.animate()
                        .setInterpolator(FastOutSlowInInterpolator())
                        .alpha(0f)
                        .scaleX(0f)
                        .scaleY(0f)
            } else {
                addTransferView.alpha = 0f
                addTransferView.scaleX = 0f
                addTransferView.scaleY = 0f
            }
            addTransferView.isEnabled = false
            addTransferView.isFocusable = false
            addTransferView.isClickable = false
        }
    }

    private fun shouldShowAddTransferFab(): Boolean {
        when (bottomNavView.currentItem) {
            TAB_ACCOUNT -> return false
            TAB_FILES -> {
                val filesFragment = filesFragment
                if (filesFragment!!.isSelecting) {
                    return false
                } else {
                    val currentPage = filesFragment.currentPage
                    if (currentPage == null) {
                        return true
                    } else {
                        if (currentPage.type === FilesFragment.Page.Type.Search) {
                            return false
                        } else {
                            return currentPage.file!!.isFolder
                        }
                    }
                }
            }
            TAB_TRANSFERS -> return true
            else -> return true
        }
    }

    fun logOut() {
        sharedPrefs!!.edit().remove("token").apply()
        FolderLoader.DiskCache(this).deleteCache()
        finish()
        startActivity(intent)
    }

    private fun setupLayout() {
        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        bottomNavView = findViewById<AHBottomNavigation>(R.id.main_bottom_nav)
        bottomNavView.defaultBackgroundColor = Color.parseColor("#F8F8F8")
        bottomNavView.accentColor = Color.BLACK
        bottomNavView.inactiveColor = Color.parseColor("#80000000")
        bottomNavView.addItem(AHBottomNavigationItem(getString(R.string.account), R.drawable.ic_nav_account))
        bottomNavView.addItem(AHBottomNavigationItem(getString(R.string.files), R.drawable.ic_nav_files))
        bottomNavView.addItem(AHBottomNavigationItem(getString(R.string.transfers), R.drawable.ic_nav_transfers))
        bottomNavView.setOnTabSelectedListener(AHBottomNavigation.OnTabSelectedListener { position, wasSelected ->
            bottomNavView.post { updateAddTransferFab(true) }
            when (position) {
                TAB_ACCOUNT, TAB_FILES, TAB_TRANSFERS -> {
                    showFragment(position, true)
                    if (position == TAB_FILES && wasSelected) {
                        filesFragment?.goBackToRoot()
                    }
                    return@OnTabSelectedListener true
                }
            }
            false
        })
    }

    fun showFilesAndGoToFile(parentId: Long, id: Long) {
        selectTab(TAB_FILES, true)
        filesFragment!!.goToFile(parentId, id);
    }

    private val accountFragment: AccountFragment
        get() = supportFragmentManager.findFragmentByTag(FRAGTAG_ACCOUNT) as AccountFragment

    private val filesFragment: FilesFragment?
        get() = supportFragmentManager.findFragmentByTag(FRAGTAG_FILES) as FilesFragment

    private val transfersFragment: TransfersFragment
        get() = supportFragmentManager.findFragmentByTag(FRAGTAG_TRANSFERS) as TransfersFragment

    private val noNetworkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            transfersFragment.setHasNetwork(false)
        }
    }

    override fun onBackPressed() {
        if (bottomNavView.currentItem == TAB_FILES) {
            if (!filesFragment!!.goBack(true)) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun showFragment(position: Int, animate: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
        if (animate) {
            transaction.setCustomAnimations(R.anim.bottomnav_enter, R.anim.bottomnav_exit)
        }
        when (position) {
            TAB_ACCOUNT -> {
                transaction.attach(accountFragment)
                transaction.detach(filesFragment)
                transaction.detach(transfersFragment)
            }
            TAB_FILES -> {
                transaction.detach(accountFragment)
                transaction.attach(filesFragment)
                transaction.detach(transfersFragment)
            }
            TAB_TRANSFERS -> {
                transaction.detach(accountFragment)
                transaction.detach(filesFragment)
                transaction.attach(transfersFragment)
            }
        }
        transaction.commitNow()
    }

    private fun selectTab(position: Int, animate: Boolean) {
        if (bottomNavView.currentItem != position) {
            bottomNavView.setCurrentItem(position, false)
            showFragment(position, animate)
        }
    }

    override fun onDestroy() {
        if (init) {
            unregisterReceiver(noNetworkReceiver)
        }

        super.onDestroy()
    }

    override val castMiniControllerContainerId = R.id.holder_castbar
}