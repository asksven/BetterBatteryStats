*Build* [![CircleCI](https://circleci.com/gh/asksven/BetterBatteryStats/tree/master.svg?style=svg)](https://circleci.com/gh/asksven/BetterBatteryStats/tree/master)

#License
BetterBatteryStats is an open source project unter the terms of the Apache 2.0 License. The license does not apply to the use of the names "BetterBatteryStats" and "Better Battery Stats", nor to the icon / artwork created for BetterBatteryStats. 

# Build
In order to build (with gradle / Android Studio) following changes to the local project are required

## google-services.jon
BBS uses Firebase Analytics. You will need to create a `google-services.json` in (follow the Firebase instructions):
- `/app`
- `/app/src/xdaedition`
- `/app/src/gplay`

## Signing

The signing config uses environment variables:
```
    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE_RELEASE"))
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
        debug {
            storeFile file(System.getenv("KEYSTORE_DEBUG"))
        }
    }
```

- `KEYSTORE_RELEASE` points to the release `.keystore` file
- `KEYSTORE_DEBUG` points to the debug `.keystore` file
- `KEY_ALIAS`  defines the alias name
- `KEY_PASSWORD` is the keystore password


 
# Continuous Integration

The continuous integration (in this example CircleCI) needs to have access to some private settings.

## google-services.json

The encrypted files (`google-services.json-cipher`) are located in `/app`, `/app/src/gplay` and `/app/src/xdaedition`.

See also https://github.com/circleci/encrypted-files.

### Encrypt

`openssl aes-256-cbc -e -in secret-env-plain -out secret-env-cipher -k $KEY`

### Decrypt (on CircleCI, as defined in `circle.yml` and using an env-variable `KEY`)

`openssl aes-256-cbc -d -in secret-file-cipher -out secret-file-plain -k $KEY`

## The signing keys

The environment variables `$KEYSTORE_RELEASE`, `$KEYSTORE_DEBUG`, `$KEY_ALIAS` and `$KEY_PASSWORD` must be set in CircleCI.

As the signing keys are not in the github repo a script `circleciscripts/download_keystore.sh` does the job of downloading and decrypting the keys at build-time.
For that to happen following addition environment variables must be set:
- `$KEYSTORE_RELEASE` the name of the release keystore-file
- `$KEYSTORE_DEDUB` the name of the debug keystore-file
- `$KEYSTORE_URI` a public URI from where the files can be downloaded using http
- `$KEY` the key to decrypt the keystores (same env var as for `google-services.json`)

