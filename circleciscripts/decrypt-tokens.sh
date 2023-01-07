#!/bin/bash

openssl enc -in ./app/google-services.json-cipher -out ./app/google-services.json -d -aes256 -k $KEY

