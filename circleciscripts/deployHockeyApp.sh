#!/usr/bin/env bash

#==========================================================================================
# Originate Android CircleCI Continuous Deployment
# deployHockeyApp.sh
#
# This script uploads an exported .apk file for FeedApp app to Hockeyapp using
#    HockeyAppToken : $HOCKEYAPP_TOKEN
#    Team Number    : $HOCKEYAPP_TEAM_ID
#    Notes          : $HOCKEYAPP_VERSION_NOTES
#    ipa            : $HOCKEYAPP_EXPORT_APK_PATH
#    App            : $HOCKEYAPP_APP_IDENTIFIER
#    tags           : $HOCKEYAPP_TAGS
#
#
#==========================================================================================

###########################################################################################
# upload to App $HOCKEYAPP_APP_IDENTIFIER
#
# Using HockeyApp API: Apps http://support.hockeyapp.net/kb/api/api-apps
#
# status=2          # make version available for download
# notify=1          # notify all testers that can install this app
# notes_type=0      # for Textile
# platform=android  # signifies that app is an android app
# ipa               # .apk export path set in the CircleCI's environment variables
# tags              # app tag
# teams             # team number
# release_type=2    # beta
# X-HockeyAppToken: token of app owner

function uploadToHockeyApp {

  GIT_COMPARE_KEY=${CIRCLE_COMPARE_URL##*/}
  GIT_PRETTY_COMMIT_LOG=$(echo "<ul>$(git log ${GIT_COMPARE_KEY} --pretty=format:'<li>[%ad] %s (%an)</li>' --date=short)</ul>" | tr -d '\n')

  HOCKEYAPP_NOTES_HEADER="**Built on:** $(date +"%a %d-%b-%Y %I:%M %p")
  **Branch:** $(git rev-parse --abbrev-ref HEAD)
  **Commit:** $(git rev-parse --short HEAD)"

  HOCKEYAPP_NOTES_HEADER_HTML=${HOCKEYAPP_NOTES_HEADER//$'\n'/<br>}
  HOCKEYAPP_NOTES="${HOCKEYAPP_NOTES_HEADER_HTML} ${GIT_PRETTY_COMMIT_LOG}"

  HOCKEYAPP_EXPORT_APK_PATH=`find ./app/build/outputs/apk -name betterbatterystats_xdaedition_debug_*.apk`

  curl --verbose \
       --fail \
       --form "status=2" \
       --form "notify=1" \
       --form "notes=${HOCKEYAPP_NOTES}" \
       --form "platform=Android" \
       --form "notes_type=0" \
       --form "ipa=@${HOCKEYAPP_EXPORT_APK_PATH}" \
       --form "tags=${HOCKEYAPP_TAGS}" \
       --form "teams=${HOCKEYAPP_TEAM_ID}" \
       --form "release_type=2" \
       --header "X-HockeyAppToken: ${HOCKEYAPP_TOKEN}" \
       "https://upload.hockeyapp.net/api/2/apps/${HOCKEYAPP_APP_IDENTIFIER}/app_versions/upload"
}