#!/bin/bash

if [ -e ${KEYSTORE_RELEASE}-cipher ]; then openssl enc -in ${KEYSTORE_RELEASE}-cipher -out ./app/$KEYSTORE_RELEASE -d -aes256 -k $KEY; fi
if [ -e ${KEYSTORE_DEBUG}-cipher ]; then openssl enc -in ${KEYSTORE_DEBUG}-cipher -out ./app/$KEYSTORE_DEBUG -d -aes256 -k $KEY; fi

ls -la ${KEYSTORE_RELEASE}*
ls -la ${KEYSTORE_DEBUG}*
ls -la ./app/${KEYSTORE_RELEASE}*
ls -la ./app/${KEYSTORE_DEBUG}*


