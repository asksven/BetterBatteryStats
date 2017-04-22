#!/bin/bash
curl -O ${KEYSTORE_URI}/${KEYSTORE_RELEASE}-cypher
curl -O ${KEYSTORE_URI}/${KEYSTORE_DEBUG}-cypher

if [ -e ${KEYSTORE_RELEASE}-cypher ]; then openssl aes-256-cbc -d -in ${KEYSTORE_RELEASE}-cipher -k $KEY >> $HOME/BetterBatteryStats/app/$KEYSTORE_RELEASE; fi
if [ -e ${KEYSTORE_DEBUG}-cypher ]; then openssl aes-256-cbc -d -in ${KEYSTORE_DEBUG}-cipher -k $KEY >> $HOME/BetterBatteryStats/app/$KEYSTORE_DEBUG; fi

