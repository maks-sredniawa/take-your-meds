# Take your meds

Take your meds reminds you to take your medicine on time. Add each medicine with its dose and
times, get a notification at the minute, mark the dose taken with one tap, and see at a glance what
today still holds.

Everything stays on the phone: no account, no server, and the app never opens a network connection.
Free, no ads, no tracking. It records what you tell it and gives no medical advice.

## Status

Just bootstrapped from [android-starter](https://github.com/maks-sredniawa/android-starter). The release
pipeline and quality gates are real; the screens are still the template's placeholder app. Nothing
is on Play yet.

## Contributing

Take your meds is source-available, not open source: read it, verify it, file issues. Pull requests are not
accepted — see [LICENSE](LICENSE).

## Build

```bash
./gradlew assembleDebug          # build
./gradlew installDebug           # build + install on the connected phone
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests — needs a device
./gradlew spotlessApply          # format (the CI gate is spotlessCheck)
python3 scripts/project.py       # what the toolchain thinks this app is called
```

Commit subjects are [Conventional Commits](https://www.conventionalcommits.org); release-please
derives the version and `CHANGELOG.md` from them. See [docs/RELEASING.md](docs/RELEASING.md).
