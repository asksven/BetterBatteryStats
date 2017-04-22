#!/usr/bin/env bash

for filename in $(find $HOME/BetterBatteryStats/app/build/outputs/apk -name "*.apk"); do rename -v 's/(.*)\.apk/$1-$ENV{"CIRCLE_BUILD_NUM"}.apk/' $filename; done;