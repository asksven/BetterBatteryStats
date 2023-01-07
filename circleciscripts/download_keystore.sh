#!/bin/bash

if [ -e ${KEYSTORE_RELEASE}-cipher ]; then openssl enc -in ${KEYSTORE_RELEASE}-cipher -out ./app/$KEYSTORE_RELEASE -k $KEY; fi
if [ -e ${KEYSTORE_DEBUG}-cipher ]; then openssl enc -in ${KEYSTORE_DEBUG}-cipher -out ./app/$KEYSTORE_DEBUG -k $KEY; fi

ls -la ${KEYSTORE_RELEASE}*
ls -la ${KEYSTORE_DEBUG}*


