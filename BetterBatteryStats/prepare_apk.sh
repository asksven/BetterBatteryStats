#!/bin/bash

VERSION=`cat AndroidManifest.xml | grep -o "versionName=\".*\"" | sed 's/"/ /g' | awk {'print $2'}`
echo "Detected Version $VERSION"
#cp ./bin/BetterBatteryStats-debug.apk ./BetterBatteryStats_xdaedition_$VERSION.apk
cp ./bin/BetterBatteryStats.apk ./BetterBatteryStats_xdaedition_$VERSION.apk
