# License
This project is licensed under the terms of the Apache License Version 2.0, http://www.apache.org/licenses/

The name "BetterBatteryStats" as well as the icons (graphics/icon.xcf and logo.xcf) as well as their derivates (res) are copyrighted and shall not be reused. 

# Credits
This project makes use of:

* [google gson][] licensed under the terms of the [Apache License Version][apache-license] 2.0,
  See libs/LICENSE_GSON
* [achartengine][] and [androidplot][] (see libs/LICENSE_ACHARTENGINE and libs/ANDROIDPLOT)
* ActionbarSherlock. ActionBarSherlock can be obtained here:
  https://github.com/JakeWharton/ActionBarSherlock
  Read this to learn how to integration ActionBarSherlock into this project.
* AndroidCommon: https://github.com/asksven/AndroidCommon
* libsuperuser (indirectly through AndroidCommon)

[google gson]: http://code.google.com/p/google-gson/downloads/detail?name=google-gson-1.7.1-release.zip
[achartengine]: http://code.google.com/p/achartengine/
[androidplot]: http://androidplot.com/
[apache-license]: http://www.apache.org/licenses/

# Building BetterBatteryStats

BetterBatteryStats builds with `ant`
To build it requires dependent projects (libraries) to be built with ant as
well:

* ActionbarSherlock
* AndroidCommon

If dependent build.xml files are missing they can be create with

    android update project --path .
(see "[using ant to automate building android][using-ant-to-automate-building-android]" for more
info)

[using-ant-to-automate-building-android]: http://www.androidengineer.com/2010/06/using-ant-to-automate-building-android.html