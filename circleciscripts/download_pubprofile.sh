#!/bin/bash
curl -O ${KEYSTORE_URI}/sa-google-play.json-cipher

if [ -e sa-google-play.json-cipher ]; then openssl aes-256-cbc -d -md md5 -in sa-google-play.json-cipher -k $KEY >> ./app/sa-google-play.json; fi

