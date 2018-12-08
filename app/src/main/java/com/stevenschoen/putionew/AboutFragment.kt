package com.stevenschoen.putionew

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import com.stevenschoen.putionew.R

class AboutFragment : Fragment() {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    val view = inflater.inflate(R.layout.about, container, false)

    val backView: View = view.findViewById(R.id.about_back)
    backView.setOnClickListener {
      NavUtils.navigateUpFromSameTask(activity!!)
    }

    val versionView: TextView = view.findViewById(R.id.about_version)
    try {
      val version = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName
      versionView.text = getString(R.string.version_x, version)
    } catch (e: PackageManager.NameNotFoundException) {
      e.printStackTrace()
    }

    val githubView: View = view.findViewById(R.id.about_github)
    githubView.setOnClickListener {
      startActivity(
          Intent(Intent.ACTION_VIEW)
              .setData(Uri.parse("https://github.com/DSteve595/Put.io"))
      )
    }

    val emailView: View = view.findViewById(R.id.about_email)
    emailView.setOnClickListener {
      val sendEmailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:steven@stevenschoen.com"))
          .putExtra(Intent.EXTRA_EMAIL, arrayOf("steven@stevenschoen.com"))
          .putExtra(Intent.EXTRA_SUBJECT, "Put.io for Android")
      startActivity(Intent.createChooser(sendEmailIntent, getString(R.string.email_the_developer)))
    }

    val translateView: View = view.findViewById(R.id.about_translate)
    translateView.setOnClickListener {
      startActivity(
          Intent(Intent.ACTION_VIEW)
              .setData(Uri.parse("https://crowdin.com/project/putio-for-android"))
      )
    }

    val visitView: View = view.findViewById(R.id.about_visit)
    visitView.setOnClickListener {
      startActivity(
          Intent(Intent.ACTION_VIEW)
              .setData(Uri.parse("https://put.io/"))
      )
    }

    return view
  }
}
