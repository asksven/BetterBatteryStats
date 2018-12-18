#!/bin/bash
# we don't download as the cipher-file is in the repo
# curl -O ${KEYSTORE_URI}/sa-google-play.json-cipher

if [[ -e ./app/sa-google-play.json-cipher ]]; then openssl aes-256-cbc -d -md md5 -in ./app/sa-google-play.json-cipher -k ${KEY} >> ./app/sa-google-play.json; fi

