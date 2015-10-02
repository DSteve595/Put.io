#Put.io for Android!

This app is unofficial and is not affiliated with Put.io in any way.

Bug reports and pull requests always welcome.

Free on the Play Store: https://play.google.com/store/apps/details?id=com.stevenschoen.putionew


###Coming soon:

Other languages (please contribute translations!)

<a href='https://pledgie.com/campaigns/24005'><img alt='Click here to lend your support to: Put.io for Android and make a donation at pledgie.com !' src='https://pledgie.com/campaigns/24005.png?skin_name=chrome' border='0' ></a>

##Build Instructions

- Clone and update submodule dependencies:
```
git clone https://github.com/DSteve595/Put.io.git
cd Put.io/
git submodule init && git submodule update
```
- Register for an API key from the [put.io website](https://put.io/v2/docs/gettingstarted.html)
- Rename the `ApiKey.java.in` into `ApiKey.java`
- Place the API key and client ID you received from put.io into each constant respectively.
- Import project into Android Studio.
- Build project via gradle to pull any missing dependencies before the build.

###Libraries used:

Picasso by Square: http://square.github.io/picasso/

Retrofit by Square: http://square.github.io/retrofit/

Android Priority Job Queue: https://github.com/path/android-priority-jobqueue

EventBus by greenrobot: http://greenrobot.github.io/EventBus/

PagerSlidingTabStrip by Andreas Stuetz: https://github.com/astuetz/PagerSlidingTabStrip

aFileChooser by Paul Burke: https://github.com/iPaulPro/aFileChooser

CWAC-MediaRouter by CommonsWare: https://github.com/commonsguy/cwac-mediarouter

Play Services by GOogle: https://developer.android.com/google/play-services/index.html

Android Support Library by Google: http://developer.android.com/tools/support-library/index.html
