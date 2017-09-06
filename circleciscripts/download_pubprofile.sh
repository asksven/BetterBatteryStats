#!/bin/bash
curl -O ${KEYSTORE_URI}/sa-google-play.json-cipher

if [ -e sa-google-play.json-cipher ]; then openssl aes-256-cbc -d -in sa-google-play.json-cipher -k $KEY >> $HOME/BetterBatteryStats/app/sa-google-play.json; fi

