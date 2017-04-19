#!/bin/bash
for filename in find $HOME/BetterBatteryStats/app/build/outputs/apk -name "*.apk"; do rename 's/(.*)\.apk/$1-$CIRCLE_BUILD_NUM.apk/' $filename; done;