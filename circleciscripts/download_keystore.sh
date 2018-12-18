#!/bin/bash
curl -O ${KEYSTORE_URI}/${KEYSTORE_RELEASE}-cipher
curl -O ${KEYSTORE_URI}/${KEYSTORE_DEBUG}-cipher

if [[ -e ${KEYSTORE_RELEASE}-cipher ]]; then openssl aes-256-cbc -d -md md5 -in ${KEYSTORE_RELEASE}-cipher -k ${KEY} >> ./app/${KEYSTORE_RELEASE}; fi
if [[ -e ${KEYSTORE_DEBUG}-cipher ]]; then openssl aes-256-cbc -d -md md5 -in ${KEYSTORE_DEBUG}-cipher -k ${KEY} >> ./app/${KEYSTORE_DEBUG}; fi

