# Releasing Hevyn’t

Hevyn’t uses semantic versions for GitHub APK releases. The first release is
`v1.0.0` with Android `versionCode = 1`.

## Permanent signing identity

Release APKs must always be signed with the same key. The local signing files
are stored in the ignored `.signing/` directory:

- `.signing/hevynt-release.jks`
- `.signing/keystore.properties`

Neither file may be committed. Back up both files together in an encrypted
password manager or encrypted offline storage. Losing the key or its password
prevents future APKs from updating existing installations.

The release certificate SHA-256 fingerprint is:

```text
B9:46:3D:29:FD:C8:3B:A2:0B:B6:04:9A:23:D9:D4:D8:74:D0:89:6A:6E:87:F0:9B:02:DD:01:A4:67:61:3E:7A
```

Compare this fingerprint when verifying every future release APK.

The local properties file has this format:

```properties
storeFile=.signing/hevynt-release.jks
storePassword=LOCAL_SECRET
keyAlias=hevynt-release
keyPassword=LOCAL_SECRET
```

## Build and verify

From the repository root, run:

```bash
./gradlew clean test lintRelease assembleRelease
```

The signed APK is generated under:

```text
app/build/outputs/apk/release/
```

Install and test that APK on a real Android device before publishing it.

## Versioning

For every release, update both values in `app/build.gradle.kts`:

- Increase `versionCode` by at least one. It must never decrease or be reused.
- Set `versionName` to the public semantic version, for example `1.0.1`.

Commit and push the version change, then create an annotated matching tag:

```bash
git tag -a v1.0.1 -m "Hevyn’t v1.0.1"
git push origin main
git push origin v1.0.1
```

Create a GitHub Release from the tag and attach the signed APK plus its SHA-256
checksum. Do not attach the keystore or signing properties.