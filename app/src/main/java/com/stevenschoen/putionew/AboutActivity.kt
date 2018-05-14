package com.stevenschoen.putionew

import android.os.Bundle
import com.stevenschoen.putionew.cast.BaseCastActivity

class AboutActivity : BaseCastActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.aboutactivity)
  }

  override val castMiniControllerContainerId: Int?
    get() = R.id.about_castbar_holder
}
