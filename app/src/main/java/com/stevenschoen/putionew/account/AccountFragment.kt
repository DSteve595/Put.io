package com.stevenschoen.putionew.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.putioApp
import com.trello.rxlifecycle3.components.support.RxFragment
import com.trello.rxlifecycle3.kotlin.bindToLifecycle
import io.reactivex.android.schedulers.AndroidSchedulers

class AccountFragment : RxFragment() {

  private var utils: PutioUtils? = null

  private lateinit var diskUsageView: TextView
  private lateinit var emailView: TextView
  private lateinit var usernameView: TextView
  private lateinit var manageOnWebView: View

  private lateinit var diskUsageDrawable: DiskUsageDrawable

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.utils = putioApp.putioUtils
  }

  override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val view = inflater.inflate(R.layout.account, container, false)

    diskUsageView = view.findViewById(R.id.account_diskusage)
    usernameView = view.findViewById(R.id.account_username)
    emailView = view.findViewById(R.id.account_email)
    manageOnWebView = view.findViewById(R.id.account_manageonweb)

    diskUsageDrawable = DiskUsageDrawable(context!!)
    diskUsageView.background = diskUsageDrawable

    manageOnWebView.setOnClickListener {
      startActivity(Intent(Intent.ACTION_VIEW, "https://app.put.io/settings/".toUri()))
    }

    invalidateAccountInfo()

    return view
  }

  fun invalidateAccountInfo() {
    utils!!.restInterface.account()
        .bindToLifecycle(this)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ accountInfoResponse ->
          val account = accountInfoResponse.info
          val disk = account.disk

          usernameView.text = account.username
          emailView.text = account.mail
          diskUsageDrawable.usedFraction = disk.used.toFloat() / disk.size
          diskUsageView.text = getString(
              R.string.x_of_x_free,
              PutioUtils.humanReadableByteCount(disk.avail, false),
              PutioUtils.humanReadableByteCount(disk.size, false)
          )
        }, { throwable -> PutioUtils.getRxJavaThrowable(throwable).printStackTrace() })
  }
}
