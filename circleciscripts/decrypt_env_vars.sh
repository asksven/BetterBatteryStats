#!/bin/bash

# decrypt the file that was create with
# openssl aes-256-cbc -e -in secret-env-plain -out secret-env-cipher -k KEY

echo "Decrypting"
openssl aes-256-cbc -d -md md5 -in secret-env-cipher -out secret-env-plain -k $KEY
ls -l secret-*
