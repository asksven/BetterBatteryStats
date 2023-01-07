#!/bin/bash

# decrypt the file that was create with
# openssl aes-256-cbc -e -in secret-env-plain -out secret-env-cipher -k $KEY

echo "Decrypting"
openssl enc -in secret-env-cipher -out secret-env-plain -d -aes256 -k $KEY
ls -l secret-*
