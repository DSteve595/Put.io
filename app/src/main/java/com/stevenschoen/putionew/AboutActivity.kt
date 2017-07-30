package com.stevenschoen.putionew

import android.os.Bundle
import com.stevenschoen.putionew.cast.BaseCastActivity

class AboutActivity : BaseCastActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.aboutactivity)
        if (UIUtils.isTablet(this)) {
            window.setLayout(PutioUtils.pxFromDp(this, 380f).toInt(), PutioUtils.pxFromDp(this, 500f).toInt())
        } else {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    override val castMiniControllerContainerId: Int?
        get() = R.id.about_castbar_holder
}