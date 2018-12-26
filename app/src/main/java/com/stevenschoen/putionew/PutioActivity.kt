package com.stevenschoen.putionew

import android.app.SearchManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stevenschoen.putionew.account.AccountFragment
import com.stevenschoen.putionew.cast.BaseCastActivity
import com.stevenschoen.putionew.files.FileDownloadsMaintenanceService
import com.stevenschoen.putionew.files.FilesFragment
import com.stevenschoen.putionew.files.FolderLoader
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.model.transfers.PutioTransfer
import com.stevenschoen.putionew.transfers.TransfersFragment
import com.stevenschoen.putionew.transfers.add.AddTransferActivity
import com.stevenschoen.putionew.tv.TvActivity
import java.util.concurrent.TimeUnit

class PutioActivity : BaseCastActivity() {

  var init = false

  val sharedPrefs by lazy { PreferenceManager.getDefaultSharedPreferences(this)!! }

  lateinit var bottomNavView: BottomNavigationView

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

    intent?.let(::handleIntent)

    val noNetworkIntentFilter = IntentFilter(
        PutioActivity.noNetworkIntent
    )

    registerReceiver(noNetworkReceiver, noNetworkIntentFilter)

    val jobDispatcher = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    if (jobDispatcher.allPendingJobs.none {
          it.id == FileDownloadsMaintenanceService.FILE_DOWNLOADS_MAINTENANCE_JOB_ID
        }) {
      jobDispatcher.schedule(
          JobInfo.Builder(
              FileDownloadsMaintenanceService.FILE_DOWNLOADS_MAINTENANCE_JOB_ID,
              ComponentName(this, FileDownloadsMaintenanceService::class.java)
          )
              .setPeriodic(TimeUnit.DAYS.toMillis(2))
              .setPersisted(true)
              .setRequiresDeviceIdle(true)
              .build()
      )
    }

    jobDispatcher.schedule(
        JobInfo.Builder(
            FileDownloadsMaintenanceService.FILE_DOWNLOADS_MAINTENANCE_JOB_ID,
            ComponentName(this, FileDownloadsMaintenanceService::class.java)
        )
            .setOverrideDeadline(5000)
            .setPersisted(true)
            .setRequiresDeviceIdle(true)
            .build()
    )
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
        if (intent.action == Intent.ACTION_SEARCH) {
          val query = intent.getStringExtra(SearchManager.QUERY)
          filesFragment.addSearch(query)
        }
      }
    }
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

    setupLayout()

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

      selectTab(TAB_FILES, false)
    }

    addTransferView = findViewById(R.id.main_addtransfer)
    addTransferView.setOnClickListener {
      var destinationFolder: PutioFile? = null
      if (bottomNavView.selectedItemId == TAB_FILES) {
        destinationFolder = (filesFragment.currentPage as FilesFragment.Page.File).file
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
    return when (bottomNavView.selectedItemId) {
      TAB_ACCOUNT -> false
      TAB_FILES -> {
        val filesFragment = filesFragment
        if (filesFragment.isSelecting) {
          false
        } else {
          val currentPage = filesFragment.currentPage
          when (currentPage) {
            null -> true
            is FilesFragment.Page.Search -> false
            is FilesFragment.Page.File -> currentPage.file.isFolder
          }
        }
      }
      TAB_TRANSFERS -> true
      else -> true
    }
  }

  fun logOut() {
    (application as PutioApplication).putioUtils = null
    sharedPrefs!!.edit().remove("token").apply()
    FolderLoader.DiskCache(this).deleteCache()
    finish()
    startActivity(intent)
  }

  private fun setupLayout() {
    val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
    setSupportActionBar(toolbar)

    bottomNavView = findViewById(R.id.main_bottom_nav)
    bottomNavView.apply {
      menu.add(0, TAB_ACCOUNT, 0, R.string.account).apply {
        setIcon(R.drawable.ic_nav_account)
      }
      menu.add(0, TAB_FILES, 0, R.string.files).apply {
        setIcon(R.drawable.ic_nav_files)
      }
      menu.add(0, TAB_TRANSFERS, 0, R.string.transfers).apply {
        setIcon(R.drawable.ic_nav_transfers)
      }
      setOnNavigationItemSelectedListener { item ->
        post { updateAddTransferFab(true) }
        showFragment(item.itemId, true)
        true
      }
      setOnNavigationItemReselectedListener { item ->
        if (item.itemId == TAB_FILES) {
          filesFragment.goBackToRoot()
        }
      }
    }
  }

  fun showFilesAndGoToFile(parentId: Long, id: Long) {
    selectTab(TAB_FILES, true)
    filesFragment.goToFile(parentId, id)
  }

  private val accountFragment: AccountFragment
    get() = supportFragmentManager.findFragmentByTag(FRAGTAG_ACCOUNT) as AccountFragment

  private val filesFragment: FilesFragment
    get() = supportFragmentManager.findFragmentByTag(FRAGTAG_FILES) as FilesFragment

  private val transfersFragment: TransfersFragment
    get() = supportFragmentManager.findFragmentByTag(FRAGTAG_TRANSFERS) as TransfersFragment

  private val noNetworkReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      transfersFragment.setHasNetwork(false)
    }
  }

  override fun onBackPressed() {
    if (bottomNavView.selectedItemId == TAB_FILES) {
      if (!filesFragment.goBack(true)) {
        super.onBackPressed()
      }
    } else {
      super.onBackPressed()
    }
  }

  private fun showFragment(position: Int, animate: Boolean) {
    val transaction = supportFragmentManager.beginTransaction()
    if (animate) {
      transaction.setCustomAnimations(R.animator.bottomnav_enter, R.animator.bottomnav_exit)
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
    if (bottomNavView.selectedItemId != position) {
      bottomNavView.selectedItemId = position
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

  companion object {
    const val EXTRA_GO_TO_TAB = "go_to_tab"

    const val TAB_ACCOUNT = 0
    const val TAB_FILES = 1
    const val TAB_TRANSFERS = 2

    private const val FRAGTAG_ACCOUNT = "account"
    private const val FRAGTAG_FILES = "files"
    private const val FRAGTAG_TRANSFERS = "transfers"

    const val noNetworkIntent = "com.stevenschoen.putionew.nonetwork"
  }

}
