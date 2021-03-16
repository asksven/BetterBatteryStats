*Build* [![CircleCI](https://circleci.com/gh/asksven/BetterBatteryStats/tree/master.svg?style=svg)](https://circleci.com/gh/asksven/BetterBatteryStats/tree/master)

# License
BetterBatteryStats is an open-source project unter the terms of the Apache 2.0 License.
The license does not apply to the use of the names "BetterBatteryStats" and "Better Battery Stats", nor to the icon / artwork created for BetterBatteryStats.

# Build
In order to build (with gradle / Android Studio), following changes to the local project are required.

## Signing
The signing config uses environment variables:
```
    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE_RELEASE") ?: "app.keystore")
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
        debug {
            if (file(System.getenv("KEYSTORE_DEBUG") ?: "app.keystore").exists()) {
                storeFile = file(System.getenv("KEYSTORE_DEBUG") ?: "app.keystore")
            }
        }
    }
```

- `KEYSTORE_RELEASE` points to the release `.keystore` file
- `KEYSTORE_DEBUG` points to the debug `.keystore` file
- `KEY_ALIAS` defines the alias name
- `KEY_PASSWORD` is the keystore password

 
# Continuous Integration
The continuous integration (in this example CircleCI) needs to have access to some private settings.

## Google play publishing

### Publishing profile
The encrypted file (`sa-google-play.json-cipher`) is located in `/app` and is referenced by the gradle build.
(See also https://github.com/Triple-T/gradle-play-publisher)

### Deploy task
In `circle.yml` we define that all the Google Play publishing (to beta) is triggered on tag `release-*`.

## Encrypt
`openssl aes-256-cbc -e -in secret-env-plain -out secret-env-cipher -k $KEY`
(See also https://github.com/circleci/encrypted-files)

## Decrypt (on CircleCI, as defined in `circle.yml` and using an env-variable `KEY`)
`openssl aes-256-cbc -d -in secret-file-cipher -out secret-file-plain -k $KEY`

## The signing keys
The environment variables `$KEYSTORE_RELEASE`, `$KEYSTORE_DEBUG`, `$KEY_ALIAS`, `$KEY_PASSWORD`, and `$KEYSTORE_PASSWORD` must be set.

There variables are set in `secret-env-plain` (not part of the project for obvious reasons).

In order to run your own build, create a file `secret-env-plain` and set the variables:
```
export KEYSTORE_PASSWORD=<your-keystore-pwd>
export KEY_PASSWORD=<your-key-pwd>
export KEY_ALIAS=<your-key-alias>
export KEYSTORE_DEBUG=<name-of-debug-keystore>
export KEYSTORE_RELEASE=<name-of-release-keystore>
```
and then encrypt this file using `openssl aes-256-cbc -e -in secret-env-plain -out secret-env-cipher -k $KEY`

The decyption is done in the pipeline using the script `circleciscripts/decrypt_env_vars.sh` with the `$KEY` stored in circle-ci's env vars.

As the signing keys are not in the GitHub repo, a script (`circleciscripts/download_keystore.sh`) does the job of downloading and decrypting the keys at build-time.
For that to happen, the following addition environment variables must be set:
- `$KEYSTORE_URI` a public URI from where the files can be downloaded using http
- `$KEY` the key to decrypt the keystores (same env var as for `google-services.json`)
