#!/bin/bash

openssl aes-256-cbc -d -md md5 -in ./app/google-services.json-cipher -k ${KEY} >> ./app/google-services.json
openssl aes-256-cbc -d -md md5 -in ./app/src/gplay/google-services.json-cipher -k ${KEY} >> ./app/src/gplay/google-services.json
openssl aes-256-cbc -d -md md5 -in ./app/src/xdaedition/google-services.json-cipher -k ${KEY} >> ./app/src/xdaedition/google-services.json

