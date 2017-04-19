# Put.io for Android!

This app is unofficial and is not affiliated with Put.io in any way.

Bug reports and pull requests always welcome.

Free on the Play Store: https://play.google.com/store/apps/details?id=com.stevenschoen.putionew


### Coming soon:
	
Other languages (please contribute translations!)

<a href='https://pledgie.com/campaigns/24005'><img alt='Click here to lend your support to: Put.io for Android and make a donation at pledgie.com !' src='https://pledgie.com/campaigns/24005.png?skin_name=chrome' border='0' ></a>

## Build instructions:

- Clone and update submodule dependencies:
```
git clone https://github.com/DSteve595/Put.io.git
cd Put.io/
git submodule init && git submodule update
```
- Register an API key with [put.io](https://put.io/v2/docs/gettingstarted.html).
- Place your API key and client ID into `ApiKey.java`.
- Build project via gradle.

### Libraries used:

Picasso by Square: http://square.github.io/picasso/

Retrofit by Square: http://square.github.io/retrofit/

Android Priority Job Queue: https://github.com/path/android-priority-jobqueue

EventBus by greenrobot: http://greenrobot.github.io/EventBus/
	
PagerSlidingTabStrip by Andreas Stuetz: https://github.com/astuetz/PagerSlidingTabStrip
	
aFileChooser by Paul Burke: https://github.com/iPaulPro/aFileChooser

CWAC-MediaRouter by CommonsWare: https://github.com/commonsguy/cwac-mediarouter

Play Services by Google: https://developer.android.com/google/play-services/index.html
	
Android Support Library by Google: http://developer.android.com/tools/support-library/index.html
