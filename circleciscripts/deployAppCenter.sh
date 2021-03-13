#!/usr/bin/env bash

function uploadToAppCenter {
  echo "Upload to appcenter"
  APPCENTER_APK=`find ./app/build/outputs/apk -name betterbatterystats_xdaedition_debug_*.apk`
  echo "APK: ${APPCENTER_APK}"

  GIT_COMPARE_KEY=${CIRCLE_COMPARE_URL##*/}
  GIT_PRETTY_COMMIT_LOG=$(echo "<ul>$(git log ${GIT_COMPARE_KEY} --pretty=format:'<li>[%ad] %s (%an)</li>' --date=short)</ul>" | tr -d '\n')

  RELEASENOTES_HEADER="**Built on:** $(date +"%a %d-%b-%Y %I:%M %p")
  **Branch:** $(git rev-parse --abbrev-ref HEAD)
  **Commit:** $(git rev-parse --short HEAD)"

  RELEASENOTES_HEADER_HTML=${HOCKEYAPP_NOTES_HEADER//$'\n'/<br>}
  RELEASENOTES="${HOCKEYAPP_NOTES_HEADER_HTML} ${GIT_PRETTY_COMMIT_LOG}"

  npm install --prefix=$HOME/.local --global appcenter-cli
  appcenter login --disable-telemetry --token ${APPCENTER_TOKEN}
  appcenter distribute release \
    --app ${APPCENTER_APP} \
    --file ${APPCENTER_APK} \
    --group "${APPCENTER_GROUP}" \
    --release-notes "${RELEASENOTES}"
}