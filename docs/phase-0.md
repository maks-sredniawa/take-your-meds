# Phase 0 — The app is Take your meds: a list of Medicines, with nothing left of the template's photos

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). Relies on
ADR-0001 (schema 1 is redefined, not migrated: nothing has shipped), ADR-0005 and ADR-0010 (the
`reminders` channel), and amends ADR-0002, ADR-0005 and ADR-0007. ADR-0003 needs no phase-0
amendment, though `PLAN.md` lists one: its 2026-11-11 amendment already records the sweep re-arming
the alarm, and raising the channel is ADR-0010's decision. [`CONTEXT.md`](../CONTEXT.md) is
owed nothing: Medicine, Amount and Note are already there, and this phase uses no other domain word.

**What closing this phase means.** Today the app is the template renamed: a list of Items with a
title, notes and a picture, a manual backup screen, and a quiet reminders channel. After it, the app
lists Medicines with a name, an Amount and a Note, in English and Polish; no code, permission,
provider or policy sentence about photos or the manual backup is left; Auto Backup carries the
database alone; the `reminders` channel exists at high importance on a fresh install; and the
launcher, the Play icon and maksforge.com show the app's own mark in its own palette. Every one of
those is a question a device or an artifact answers (*Readings*).

`PLAN.md`'s paragraph holds, with one correction besides ADR-0003 above: the tip ask's measure is
ten Taken Doses, and Taken Doses do not exist until phase 1's `DoseRecord`. Phase 0 renames the
measure and moves the card; phase 1 gives it a count (§3).

---

## 0. What this phase inherits

The bootstrap left every gate green on the placeholder: the schema gate on `items`, the translation
gate on `item_*` strings, the edge-to-edge nightly on the Items screens, `aab-permissions.py` on the
template's permission set. Each is mechanical and each points at the template until this phase
repoints it, so a green build proves nothing about the app this phase produces. The readings below
are what does.

The asymmetry: everything removed here is free to remove, because no install exists. After Phase P's
first upload a dropped column is a migration and a dropped permission is a Console change. So the
strip is done in full now, not trimmed later.

## What is in, and what is deliberately not

**In:** the photo pipeline and the manual export/restore deleted, per
[`optional-modules.md`](optional-modules.md); `AppBackupAgent` kept for the database alone; Items
become Medicines across entity, DAO, screens, routes, strings, the debug seeder and
`scripts/edge-to-edge.py`'s scenes; the `reminders` channel at `IMPORTANCE_HIGH`; the tip ask
measured in Taken Doses; the palette and the mark, and the app's entry on maksforge.com and in the
profile README.

**Not in, and each with a reason rather than a phase:**

- **Schedules, Times, Doses, Today.** Phase 1's mechanism. A Medicine screen without them is the
  honest phase-0 app: a list the next phase hangs Schedules on.
- **`minSdk`.** Phase 2 freezes it with the permission set, because phase 1's exact-alarm and
  notification asks are the `SDK_INT` branches that decide it. 26 stands until then.
- **Deleting the tip.** It stays (ADR-0007); `PLAN.md` already counts its ask in Taken Doses.
- **Room and the reminder machinery.** Both stay: Doses are history read by day and by Medicine
  (ADR-0009), and phase 1 builds on `work/`.

**The tempting one: keeping the photo pipeline "for later" — a picture of the box is a real
feature.** It is not free to keep. It carries `FileProvider`, a Data safety answer, Coil, a privacy
policy paragraph, and a column that a later removal turns into a destructive migration. If a picture
of a Medicine is ever wanted, the pipeline comes back from the template with its own ADR; the
amendment to ADR-0002 says where.

---

## Checkpoints

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | **The strip** — photos and the manual backup out, the agent down to the database | `refactor:` | — |
| **B** | **Medicines** — the domain, its screens, both languages, the seeder and the nightly's scenes | `feat:` | A |
| **C** | **The `reminders` channel at high importance** | `feat:` | — **done 2026-11-12** |
| **D** | **The palette and the mark** — and the app on maksforge.com | `feat:` | the owner's choice — **done 2026-11-12** |

No gate: nothing in this phase can veto the rest. Every removal is the template's own documented
strip, and the one platform fact it relies on (importance is fixed at creation) is ADR-0003's.

**A before B** because B's entity has no `imagePath` and B's editor no picker; done the other way
round, B writes code A deletes. **C stands alone** and can merge at any point before Phase P. **D
went first**, because it waited only on the owner's choice and touched nothing A or B touch.

---

## 1. The strip (A)

Per [`optional-modules.md`](optional-modules.md), *Photos* and *Backup export/restore*, with one
departure: `AppBackupAgent` stays, because static backup rules cannot checkpoint Room's WAL
(ADR-0005), and its own reason does not depend on the manual half.

| Goes | Stays |
| --- | --- |
| `media/`, `MediaFilesTest`, `res/xml/file_paths.xml`, the `FileProvider`; `coil.compose`, `exifinterface` | `AppBackupAgent`, `onFullBackup` staging only the checkpointed database |
| `ui/backup/`, Settings' *Backup* row, the `backup_*` strings | `data/backup/WalCheckpoint.kt` and the database install on restore |
| `FileSwap.kt`'s media half (`mediaFilesIn`, `installFile`, the media `.part` sweep); the `test/…/data/backup/` tests of what goes (`BackupManifestTest`, `BackupRestorerTest`, `BackupScopeTest`, `AutoBackupTest`'s thumbnail-budget and marker cases) | `FileSwap.kt`'s `installDatabase` |
| `BackupExporter`, `BackupManifest`, `BackupRestorer`, `BackupScope`; the thumbnail budget and the auto-backup and over-quota markers, whose only reader was the Backup screen | `kotlinx.serialization` (Navigation 3) |
| `ui/common/Share.kt`, `ShareBook.kt` and their tests, `revokeUnpickedShare()`, `ShareChosenReceiver`, the `SEND` `<queries>` | `ui/common/Intents.kt`, `Surfaces.kt` |
| `scripts/upgrade-diff.py`; `ManifestWiringTest`'s share case | `ManifestWiringTest`'s boot and update cases |

The agent still leaves the preferences file out, so the tip's once-per-install state stays per install
(ADR-0007). The privacy policy and `play-app-content.md` lose their photo, camera and sharing
sentences in the same commit (`optional-modules.md`, *Around the code* 4).

## 2. Medicines (B)

`ItemEntity` becomes `MedicineEntity`, table `medicines`:

| Column | Type | Note |
| --- | --- | --- |
| `id` | `String` UUID | as the template's, for the reasons in its KDoc |
| `name` | `String` | required, trimmed, not blank |
| `amount` | `String` | required free text, never parsed (ADR-0011) |
| `note` | `String?` | null when empty |
| `createdAt` | `Instant` | indexed; the list's order |

**Schema 1 is redefined, not migrated.** `APP_SCHEMA_VERSION` stays 1 and `app/schemas/*/1.json` is
re-exported: nothing has shipped, so there is nothing to migrate from. This is the last time that is
true; ADR-0001's rule binds from the first upload, and `PLAN.md` brings its 1.0 clause forward to
phase 2. A device or emulator holding the template's schema 1 uninstalls the app before B's build
goes on, rather than upgrading: the shape changes while the version stays 1, so Room fails the
identity-hash check on open, and the destructive fallback does not cover that.

`ui/items/` becomes `ui/medicines/` (list, detail, editor), the routes follow, and the tab is
*Medicines* until phase 1 puts Today in front of it. The debug seeder seeds a handful of Medicines;
`scripts/edge-to-edge.py`'s `SCENES`, `TAB_BAR`, `HOME_TAB` and `SEED_WALK` walk the new screens the
same week (`DOD.md`).

Copy, English (Polish from it, per [`translator-brief.md`](translator-brief.md)); the Amount label
never says *dose*, and nothing says *should* (`CONTEXT.md`):

| Key | English |
| --- | --- |
| `tab_medicines` | Medicines |
| `medicine_add` | Add a medicine |
| `medicine_detail_title` | Medicine |
| `medicine_name_label` | Name |
| `medicine_amount_label` | Amount |
| `medicine_amount_hint` | For example 1 tablet, 5 ml |
| `medicine_note_label` | Note |
| `medicine_note_hint` | For example with food |
| `medicines_empty` | No medicines yet. Tap + to add the first one. |
| `medicines_count` (plural) | %d medicine / %d medicines |

## 3. The tip ask (B)

`TIP_ASK_AFTER_ITEMS` becomes `TIP_ASK_AFTER_TAKEN_DOSES` (10) and `tipAskDue`'s count parameter
becomes `takenDoses`. The card moves with the home screen to `ui/medicines/` and its strings to
`tip_ask_*`. The count comes from a `takenDoseCount: Flow<Int>` on the container, `flowOf(0)` until
phase 1's `DoseRecord` supplies it, so in phase 0 the ask is never due. That is correct, not a gap:
ADR-0007's measure is use, and before phase 1 nothing is used. ADR-0007 gets the amendment.

## 4. The `reminders` channel (C)

**Done 2026-11-12.** `ReminderChannels.kt` creates `reminders` at `IMPORTANCE_HIGH` (ADR-0010), so a
Reminder heads up and sounds. Same id: nothing has been uploaded, and a phone holding an earlier
build is reinstalled rather than upgraded, because importance cannot be raised on an existing
channel.

## 5. The palette and the mark (D)

**Done 2026-11-12.** The owner chose from three marks and three palettes a design pass put in front
of them: a capsule with a check punched through it, a Dose answered Taken without any sign of medical
authority (ADR-0011), on a sage palette whose amber tertiary carries the caution states (a Missed
Dose, the exact-alarm banner) without alarm red. Geometry and provenance are in `art/README.md`, the
seeds in `scripts/gen_scheme.py`. The feature graphic's wordmark now shrinks to clear the mark, which
this app's name was too long to do at the template's fixed size.

On maksforge.com the icon is `apps/web/public/assets/icons/take-your-meds.webp` in the home page repo,
and the app moved from Ideas to In development there and in the profile README; `docs/_config.yml`
already said `status: building`.

---

## Tests

No safety value enters in this phase: a Medicine is stored text, and the first value that decides
whether a Reminder fires is phase 1's Dose derivation. `TipAskTest` follows the rename, and
`ItemDaoTest` follows it to `MedicineDaoTest`.

## Documents this phase amends

- ADR-0002: dated amendment, the pipeline removed and where to take it from if a picture comes back.
- ADR-0005: dated amendment, the manual half removed; the agent stages the database alone.
- ADR-0007: dated amendment, the measure is ten Taken Doses, supplied from phase 1.
- `CLAUDE.md`: the Photo Picker and Coil rows of Stack; the media-path, `MediaFiles` and
  missing-media house rules; `media/` and `data/backup/` in Layout.
- `docs/privacy-policy.md` and `docs/play-app-content.md`, together: photos, camera and sharing out.
- `scripts/aab-permissions.py`: whatever the strip removes from the merged manifest, in the same
  commit, run against a local `bundleRelease`.
- `DOD.md`: tick *Delete what the app does not need*, *Replace the placeholder domain*, *Rewrite
  edge-to-edge's SCENES*, *Choose the palette*, *Replace the placeholder mark* and *Put the app on
  maksforge.com* as each closes.

## Known gaps, deliberately

- **A Medicine with no Schedule is, for one phase, indistinguishable from an As-needed one.** Phase
  1 adds the Schedule; until then nothing reminds and nothing claims to.
- **No restore of media on a new phone**: there is no media. The restore path's only job is the
  database, and R3 reads it.

## Readings

On the emulator (`emulator-test`), fresh install of the debug build unless the row says otherwise.

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1a | The merged manifest asks for no `CAMERA` | `python3 scripts/aab-permissions.py` against `bundleRelease` | **8 permissions, no `CAMERA`** (against `bundleDebug`: no upload key exists yet, 2026-11-12) |
| R1b | The merged manifest carries no `FileProvider` and no `SEND` or `SEND_MULTIPLE` query (`SENDTO` stays: *Report a problem*) | `./gradlew assembleDebug`, then `$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer manifest print app/build/outputs/apk/debug/app-debug.apk \| grep -E 'FileProvider\|action\.SEND(_MULTIPLE)?"'` prints nothing | **Nothing: no `FileProvider`, no `SEND` query** (2026-11-12) |
| R2 | `reminders` exists at importance 4 (HIGH) on a fresh install, once the sweep has run: the sweep creates it, not process start | install, open the app once, read the sweep's job id from `adb shell dumpsys jobscheduler`, `adb shell cmd jobscheduler run -f -n androidx.work.systemjobscheduler <pkg> <id>`, confirm `WM-WorkerWrapper: Starting work` in logcat (`CLAUDE.md`, *Environment notes*), then `adb shell dumpsys notification --noredact \| grep "mId='reminders'"` | **mImportance=4** (API 36 and API 26 emulators, 2026-11-12, PR #6's run) |
| R3 | Medicines survive a backup and restore through the agent | add two, `adb shell bmgr backupnow <pkg>`, uninstall, reinstall, `adb shell bmgr restore <token> <pkg>`, open the app | |
| R4 | The Medicines screens render edge to edge in both languages, light and dark | `python3 scripts/edge-to-edge.py` | |
| R5 | The launcher shows the new mark, themed and unthemed | screenshot of the app drawer, API 33+ themed icons on | **Unthemed: the mark, check as a hole** (API 33 emulator, 2026-11-12). **Themed: not readable** — the emulator's launcher tinted no icon at all; needs Pixel Launcher or a real phone |
