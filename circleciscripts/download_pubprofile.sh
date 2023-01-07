#!/bin/bash
# we don't download as the cipher-file is in the repo
# curl -O ${KEYSTORE_URI}/sa-google-play.json-cipher

if [ -e ./app/sa-google-play.json-cipher ]; then openssl enc -in ./app/sa-google-play.json-cipher -out ./app/sa-google-play.json -k $KEY; fi

