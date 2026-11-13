# Changelog

## [0.4.0](https://github.com/maks-sredniawa/take-your-meds/compare/v0.3.0...v0.4.0) (2026-11-13)


### Features

* snooze a Reminder for ten minutes ([4278191](https://github.com/maks-sredniawa/take-your-meds/commit/427819112d457e5a9bc8df2c35c4c2f3b6261c54))


### Bug Fixes

* arm the alarm before the Snooze cleanup, and let no cleanup failure stop a re-plan ([4b1d930](https://github.com/maks-sredniawa/take-your-meds/commit/4b1d930ab871bcb672e78b00b246adbe5273f885))
* delete a Medicine under the planner's lock ([72804b6](https://github.com/maks-sredniawa/take-your-meds/commit/72804b66be15dd99784269603033d0904cd465bf))
* post an overdue Reminder once when a queued firing follows a re-plan ([cc24fc2](https://github.com/maks-sredniawa/take-your-meds/commit/cc24fc207bc75815713f04bfc72961fa8ad1e836))
* say the next dose is tomorrow when Times exist but none falls today ([df6a897](https://github.com/maks-sredniawa/take-your-meds/commit/df6a897f1cd6e868dab557c23a9d800b1c379a96))
* show Snoozed until on Today only while that time is ahead ([5af0df5](https://github.com/maks-sredniawa/take-your-meds/commit/5af0df5382fa12f80c6455ee7a803537a4d8944c))

## [0.3.0](https://github.com/maks-sredniawa/take-your-meds/compare/v0.2.0...v0.3.0) (2026-11-13)


### Features

* add schema 2 with Schedules, Dose records and Snoozes ([41c244d](https://github.com/maks-sredniawa/take-your-meds/commit/41c244d320558156cd45ab72c36c1f6121b97a0f))
* add Today as the home tab and a Medicine's Times in the editor ([162265d](https://github.com/maks-sredniawa/take-your-meds/commit/162265da48b9c9d7d2b77fc045538da6695757dd))
* ask for notifications and exact alarms at the first Schedule's save, and say on Today when either is refused ([bb1dc64](https://github.com/maks-sredniawa/take-your-meds/commit/bb1dc646ce1e35c350c164a11212385c8aa8efd0))
* derive Doses, Reminder times and re-posts from the Schedules ([b0c4552](https://github.com/maks-sredniawa/take-your-meds/commit/b0c4552db6cd45a95712d3497faf413e7d050a36))
* remind each Dose at its Time through one exact alarm ([81bb9db](https://github.com/maks-sredniawa/take-your-meds/commit/81bb9dbd29c1ad670d08d6b6e760c2377aa0dedd))
* **support:** adopt the shared Help and feedback from android-starter ([7a29ce1](https://github.com/maks-sredniawa/take-your-meds/commit/7a29ce18038288845130b9bb9f9e6413caa254d5))
* **support:** link licence terms and show licence names on Credits ([305a4c5](https://github.com/maks-sredniawa/take-your-meds/commit/305a4c561820a6b8b6467fb675fae23800c79bad))
* **support:** open Credits with the people who made the app ([fbb563a](https://github.com/maks-sredniawa/take-your-meds/commit/fbb563a1a986bb1499ccb78cb4891b05b7d09524))
* **support:** re-sync Help and feedback with android-starter's address row and copy ([deb07ac](https://github.com/maks-sredniawa/take-your-meds/commit/deb07ace12c958b19c8c491d2bcb03e17768af14))


### Bug Fixes

* draw the banner's action in the banner's own on-colour ([f446c82](https://github.com/maks-sredniawa/take-your-meds/commit/f446c826bb69b7a35bd2e8367696814f72e4b16f))
* fail the backup run when the WAL checkpoint cannot complete ([3ed9d32](https://github.com/maks-sredniawa/take-your-meds/commit/3ed9d32b850021d984fb68ccb631bd359f852c7c))
* give the unreadable-database screen its own title ([00d05f4](https://github.com/maks-sredniawa/take-your-meds/commit/00d05f40a3af0bf86dcb7cc029c7d605fd7c1077))
* keep a database the backup checkpoint cannot open ([7e70fc0](https://github.com/maks-sredniawa/take-your-meds/commit/7e70fc0ea38d6b11f45a803bb3822a73d3c84310))
* keep the backup agent in restricted mode so failing a run cannot end the app ([492fe2d](https://github.com/maks-sredniawa/take-your-meds/commit/492fe2da6841cfbd232c251dd958784ea3848c7d))
* keep the plain debug seed and put Today's Schedules in a today variant ([eb97ee3](https://github.com/maks-sredniawa/take-your-meds/commit/eb97ee3f3a2c3529234d0997a0e45d3b58777544))
* offer Undo once per answer and read Today's clock once ([7e85c8d](https://github.com/maks-sredniawa/take-your-meds/commit/7e85c8d576caeb7c83bdce3b8551bf7e27d8f136))
* post an overdue armed alarm's Reminders on any re-plan and answer under the planner's lock ([3a1df3f](https://github.com/maks-sredniawa/take-your-meds/commit/3a1df3fd2c90ba0cdcf2570e46acc574b8dbf4f5))
* read the schema version through the WAL before Room opens ([c791d80](https://github.com/maks-sredniawa/take-your-meds/commit/c791d805f11f1e4f3ec219b39095cf483cf350c7))
* **support:** say when the language report finds no mail app, and keep the licence screen safe ([c8c4524](https://github.com/maks-sredniawa/take-your-meds/commit/c8c4524c493d6af101831806e8e18724b920d8e1))
* tell TalkBack that tapping a Time changes it ([4f65f25](https://github.com/maks-sredniawa/take-your-meds/commit/4f65f257cce03d84a4aa92bdf7716b4e7f5cf160))
* tell the user an unreadable database could not be read, not its version ([dfa3c70](https://github.com/maks-sredniawa/take-your-meds/commit/dfa3c705e0e5abf73f5736483b63f5b9299e246f))

## [0.2.0](https://github.com/maks-sredniawa/take-your-meds/compare/v0.1.0...v0.2.0) (2026-11-12)


### Features

* capsule-with-check mark on a sage palette ([3862255](https://github.com/maks-sredniawa/take-your-meds/commit/386225596758d16ef048866162b3a6f76c9264f8))
* Items become Medicines with an Amount and a Note ([4a82c41](https://github.com/maks-sredniawa/take-your-meds/commit/4a82c41394a864280bbc5d8281f42e5ddcd353ea))
* raise the reminders channel to high importance ([b848a5a](https://github.com/maks-sredniawa/take-your-meds/commit/b848a5a37682c1a49d59170c1cb3ee7bc03b9f47))

## Changelog

Nothing released yet.
