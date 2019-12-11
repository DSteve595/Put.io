# Put.io for Android!

This app is unofficial and is not affiliated with Put.io in any way.

Bug reports and pull requests always welcome.

Free on the Play Store: https://play.google.com/store/apps/details?id=com.stevenschoen.putionew


### Coming soon:
	
More languages (please [contribute translations](https://crowdin.com/project/putio-for-android)!)

## Build instructions:

- Register an API key with [put.io](https://app.put.io/settings/account/oauth/apps).
- Place your API key and client ID into `app/src/main/res/secrets.xml`.
- Comment out these 2 lines in `app/build.gradle` (don't commit these changes):
    - `apply plugin: 'io.fabric'`
    - `apply plugin: 'com.google.gms.google-services'`
- Build project via gradle.
