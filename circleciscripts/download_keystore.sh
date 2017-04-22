# use curl to download a keystore from $KEYSTORE_URI, if set,
# to the path/filename set in $KEYSTORE_RELEASE and/or $KEYSTORE_DEBUG.

if [[ $KEYSTORE_RELEASE && ${KEYSTORE_RELEASE} && $KEYSTORE_URI && ${KEYSTORE_URI} ]]
then
    echo "Keystore detected - downloading..."
    curl -L -o ${KEYSTORE_RELEASE}-cypher ${KEYSTORE_URI}
else
    echo "Keystore uri not set.  .APK artifact will not be signed."
fi

if [[ $KEYSTORE_DEBUG && ${KEYSTORE_DEBUG} && $KEYSTORE_URI && ${KEYSTORE_URI} ]]
then
    echo "Keystore detected - downloading..."
    curl -L -o ${KEYSTORE_DEBUG}-cypher ${KEYSTORE_URI}
else
    echo "Keystore uri not set.  .APK artifact will not be signed."
fi

if [ -e ${KEYSTORE_RELEASE} ] then openssl aes-256-cbc -d -in ${KEYSTORE_RELEASE}-cipher -k $KEY >> $(KEYSTORE_RELEASE}; fi;
if [ -e ${KEYSTORE_DEBUG} ] then openssl aes-256-cbc -d -in ${KEYSTORE_DEBUG}-cipher -k $KEY >> $(KEYSTORE_DEBUG}; fi;