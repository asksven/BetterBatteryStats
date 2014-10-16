#License
BetterBatteryStats is an open source project unter the terms of the Apache 2.0 License. The license does not apply to the use of the names "BetterBatteryStats" and "Better Battery Stats", nor to the icon / artwork created for BetterBatteryStats. 

# Credits
This project makes use of:

* [google gson][] licensed under the terms of the [Apache License Version][apache-license] 2.0,
  See libs/LICENSE_GSON
* [achartengine][] and [androidplot][] (see libs/LICENSE_ACHARTENGINE and libs/ANDROIDPLOT)
* AndroidCommon: https://github.com/asksven/AndroidCommon
* libsuperuser (indirectly through AndroidCommon)
* LocalepluginLib (http://www.twofortyfouram.com/developer) added as a sub-project in BetterBatteryStats/LocalePluginLib
* ckChangelog (https://github.com/cketti/ckChangeLog)

[google gson]: http://code.google.com/p/google-gson/downloads/detail?name=google-gson-1.7.1-release.zip
[achartengine]: http://code.google.com/p/achartengine/
[androidplot]: http://androidplot.com/
[apache-license]: http://www.apache.org/licenses/

# Building BetterBatteryStats

BetterBatteryStats builds with `ant`
To build it requires dependent projects (libraries) to be built with ant as
well:

* AndroidCommon (https://github.com/asksven/AndroidCommon)

If dependent build.xml files are missing they can be create with

    android update project --path .
(see "[using ant to automate building android][using-ant-to-automate-building-android]" for more
info)

[using-ant-to-automate-building-android]: http://www.androidengineer.com/2010/06/using-ant-to-automate-building-android.html
