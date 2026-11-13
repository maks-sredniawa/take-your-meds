# Phase 1 — A Medicine reminds at its Times, and Today says what has been Taken

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). Relies on
ADR-0001 (the schema gate this phase's migration crosses, §2), ADR-0003 (the sweep as the backstop),
ADR-0009 (a Dose is derived; only the answer and a pending Snooze are stored), ADR-0010 (one exact
alarm, with a fallback the user can see) and ADR-0011 (Amount and Note stay text the app never
reads). Amends ADR-0003, ADR-0007, ADR-0009 and ADR-0010 (*Documents this phase amends*).
[`CONTEXT.md`](../CONTEXT.md) is owed two things: what *Due* means against the clock, since a Dose
before its Time is neither Due nor Taken and Today has to show it; and *Re-plan*, the code word this
phase and ADR-0010 already use.

**What closing this phase means.** Today the app is a list of Medicines with a name, an Amount and a
Note, a daily WorkManager sweep that derives nothing, and an exact-alarm grant nobody asks for. After
it, a Medicine carries one or more Times every day; at each Time a Reminder heads up on the
`reminders` channel, through one exact alarm, or through `setAndAllowWhileIdle` with a banner on
Today when the grant is refused; the Reminder carries Taken and Snooze; Today is the home screen and
lists the day's Doses as Due or Taken; and the first Schedule saved is the moment the app asks for
notifications and exact alarms. The difference a device can be asked about: does a Reminder arrive on
its minute in deep Doze, after a reboot, after an update, and how late does it arrive with the grant
refused (*Readings*).

`PLAN.md` holds, with three corrections found while planning. **First**, it has no paragraph for
phases 1 and 2 below its Status list, only the Status entries; this phase adds its paragraph when it
closes. **Second**, the Status entry's *how old a Due Dose may be* is not a phase-2 placeholder: the
number this phase picks is the moment phase 2 will call Missed, so phase 2 inherits it rather than
inventing a second one (§5). **Third**, the first schema migration moves from phase 3 to here (§2,
decided 2026-11-13), and Phase 3's Status entry and ADR-0009's last consequence change with it.

---

## 0. What this phase inherits

**The reminder machinery is a proven path with nothing on it.** Phase 0 kept `work/` so this phase
would build on a mechanism already shown to run, survive a reboot and refuse a database it must not
open. What exists and what changes:

| File | Today | After phase 1 |
| --- | --- | --- |
| `work/ReminderSweep.kt` | one-time work at 09:00 that re-enqueues itself; ensures the channels; derives nothing | the same slot and chain; its body re-plans (arms the one alarm) and posts nothing (§5) |
| `work/ExactAlarms.kt` | `canScheduleExactAlarms()` and `openExactAlarmSettings()`, no caller arms an alarm | the planner reads the grant at every arm; the ask and Today's banner call the hand-off |
| `work/NotificationPermission.kt` | the app's one `POST_NOTIFICATIONS` ask, spent from Settings' *Remind me* switch | spent from the first Schedule's save instead (§4); the Settings call goes with the switch (§4) |
| `work/ReminderChannels.kt` | `reminders` at `IMPORTANCE_HIGH`, read on a fresh install (phase 0, R2) | unchanged; its description copy stops saying "daily check" |
| `work/BootReceiver.kt`, `PackageReplacedReceiver.kt` | ensure the sweep is enqueued | a full re-plan: re-post what is still Due, then arm. `BootReceiver`'s own KDoc already says it: AlarmManager restores nothing |
| `work/ReceiverWork.kt` | `goAsync` around a suspend block | reused by every new receiver |
| `work/BatteryExemption.kt`, `Autostart.kt` | the hand-offs, unused | unused: phase 2's setup screen |
| `data/AppPreferences.kt` | `remindersEnabled`, default false | `remindersEnabled` and `Keys.REMINDERS_ENABLED` deleted, with the switch (§4) |
| `AppContainer.takenDoseCount` | `flowOf(0)` | a count of Taken `DoseRecord`s, so the tip ask can come due (ADR-0007) |
| `Navigation.kt`, `NavigationKeys.kt` | back stack rooted at `Medicines` | rooted at `Today`, the first tab (phase 0 said Today would go in front) |
| `ui/medicines/MedicineEditor*` | name, Amount, Note | plus the Medicine's Times |

**The asymmetry that shapes the phase: the grant can be read back, delivery cannot.**
`canScheduleExactAlarms()` and `areNotificationsEnabled()` answer truthfully on every resume, so a
refusal of either becomes a banner the app can stand behind. Whether a Reminder actually arrived on
its minute in deep Doze, after a reboot or after an update is something no code in the app can read;
only a device can. That is why the gate (§1) and the readings carry this phase, and why the tests
(*Tests*) cover the arithmetic that decides *which* minute, never whether the phone honoured it.

**The second asymmetry: Phase P put 0.2.0 on internal testing.** It holds schema 1 (`medicines`
only), it is a release build, and a release build has no destructive fallback (ADR-0001). Whatever
this phase does to the schema, that install is the one existing database it can break (§2).

**A promise already made in copy:** the privacy policy says notifications are asked "only if you
turn reminders on", and Settings says reminders are "a single daily check". Both stop being true in
this phase and move with it (*Documents this phase amends*).

## What is in, and what is deliberately not

**In:** a Schedule per Medicine with one or more Times, every day; Doses derived from it (ADR-0009)
and the pure functions that decide which Dose exists, when its Reminder is due, and when a re-plan
still re-posts it, each with its test; one exact alarm re-planned on every firing, answer, Snooze,
Schedule save, Medicine delete, boot, update, clock change, time-zone change and grant change, falling
back to `setAndAllowWhileIdle` (ADR-0010); the Reminder on `reminders` with Taken and Snooze; Today
as the home screen, with each Dose Due or Taken and Taken answerable there; the notifications ask and
the exact-alarm hand-off at the first Schedule's save, with what the user sees on a refusal; the tip
ask's count wired to Taken Doses.

**Not in, and each with a reason rather than a phase:**

- **Missed and the Follow-up.** Missed is the state after the Follow-up, and the Follow-up is a
  second Reminder time on the same alarm with its own copy and its own timing. This phase fixes the
  number Missed will start from (§5) and leaves the state to the phase that adds the Reminder that
  precedes it.
- **Skip.** An answer with its own copy and its own History meaning (`CONTEXT.md`: a choice, not an
  absence). The `DoseRecord` answer is an enum stored by name, so adding `Skipped` costs no
  migration.
- **Days other than every day, an End date, As needed.** Phase 3 owns them with History. The
  Schedule's shape takes them as added columns, not a rebuild.
- **The battery-optimisation and autostart setup screen.** `PLAN.md` rule 4 puts it after the first
  Reminder has fired, in phase 2. Until then a Xiaomi without autostart can stay silent; named in
  *Known gaps*.
- **Changing an answer after the fact.** Today offers Undo after marking Taken there (§6);
  revising yesterday's answer, or a Taken tapped on a Reminder, belongs with History.

**The tempting one: `setAlarmClock()`.** It is the most reliable delivery Android has in deep Doze,
and the first time a Reminder arrives a minute late in a reading somebody will reach for it. It puts
an alarm icon in the status bar and a "next alarm" into the clock app for every Dose, which reads as
the app pretending to be an alarm clock, and it needs the same `SCHEDULE_EXACT_ALARM` grant, so it
fixes nothing for the refused case. ADR-0010 rejected it; it comes back only through the gate's third
verdict, as a new ADR. Its cousin, one alarm per Dose instead of one alarm for the earliest, is the
other tempting shortcut and is ADR-0003's rejected alternative under another name.

---

## Checkpoints

Each checkpoint leaves the app working and ships its copy complete in English and Polish
(`scripts/translation-gate.py` at merge).

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | **The gate** — is one exact alarm on its minute in deep Doze, including the second of two Reminders five minutes apart? | the probe, `chore:`; the verdict is a reading | — |
| **B** | **The schema and the derivation** — Schedule, `DoseRecord`, Snooze; the pure functions and their tests | `feat:` | — |
| **C** | **Today and the Times** — Times in the Medicine editor; Today in front, Due and Taken, Taken from Today; the tip count | `feat:` | B |
| **D** | **The Reminder** — the planner, the alarm receiver, every re-plan trigger, the fallback, the notification with Taken | `feat:` | A's verdict, B |
| **E** | **The asks** — notifications and exact alarms at the first Schedule's save; Today's two banners | `feat:` | C, D |
| **F** | **Snooze** — the action, the stored row, the re-plan that keeps it | `feat:` | D |
| **G** | **The readings** — R1–R11 on the emulators; the documents | `docs:` | E, F |

**The gate first, and alone.** A is a debug-only probe and an afternoon of readings. If its third
verdict comes in, D is written on a different mechanism, and every line of D written before the
reading is a bet. B and C touch no alarm, so they run beside A without betting on it.

**B before everything that shows a Dose,** because the derivation is the safety value (ADR-0009) and
its test enters with it (`PLAN.md` rule 3). C and D are its two readers.

**C before D** puts the derivation on screen before any alarm depends on it: a wrong Dose on Today is
a visible bug, the same wrong Dose behind an alarm is a Reminder that never comes.

**D and E are split by cost** (`PLAN.md` rule 2): D is the expensive, device-bound half; E is two
screens and two banners. They are one feature to a user: on API 33+ D alone posts nothing to a
fresh install, because nothing has asked for notifications yet. D is read on the emulator with
`pm grant` and `appops`; E is what makes it work for somebody who never ran `adb`.

**No release PR merges from B until E has merged.** A release carrying schema 2 freezes it (§2), so
the hold keeps it amendable through C, D and E, the checkpoints most likely to find a column missing.
It also keeps a release from shipping Times that remind nothing (after C) or Reminders nobody has
been asked to allow (after D).

**F after D** because a Snooze is one more Reminder time on D's alarm (ADR-0010) and changes nothing
D builds.

---

## 1. The gate: is one exact alarm on its minute in deep Doze?

**The gate question** is (a). Two candidates were weighed:

- **(a)** does a `setExactAndAllowWhileIdle` alarm start the killed process and post a
  heads-up Reminder within 60 s of its time in deep Doze, including the second of two alarms five
  minutes apart? Only this one can veto the phase: ADR-0010's whole mechanism stands on it.
- **(b)** how late does the `setAndAllowWhileIdle` fallback land in deep Doze with the grant refused?
  Its answer changes copy and how hard the app pushes the grant, never the mechanism, so it is taken
  under the same apparatus as G3 and decides between two banner bodies already written (§6), not
  whether the phase proceeds.

The rest of this section is written for (a).

### The question, in the form that can be measured

On an API 36 emulator and an API 26 emulator, with the debug build's process killed (not
force-stopped, which cancels alarms), the screen off, the battery unplugged and the device forced
into deep Doze: does an alarm armed with `setExactAndAllowWhileIdle` for T start the process, and is
a notification on `reminders` posted within 60 s of T? And when a second alarm is armed from inside
the first firing for T + 5 min, is that one within 60 s too? Five minutes is two Medicines at 9:00 and
9:05, the closest pair a user will plausibly set, and it is the case Doze's allow-while-idle rate
limit is known to stretch on older API levels.

### The apparatus

`app/src/debug/…/debug/AlarmProbeReceiver.kt`, beside `SeedReceiver` and exported the same way in the
debug manifest, so `adb shell am broadcast` reaches it and no release build carries it. One broadcast
arms an alarm N minutes ahead, exact or fallback per an extra; the firing logs
`AlarmProbe armedFor=<epoch ms> firedAt=<epoch ms> lateMs=<ms>` under the tag `AlarmProbe`, posts a
notification on `reminders`, and with the `chain` extra arms the next one five minutes on. It logs
times only, never a Medicine. It stays in `src/debug/` until G closes, then goes: D's own receiver
logs the same line, and a second alarm path left in the debug build is one the readings could mistake
for the real one.

### The verdicts, decided now rather than argued about later

1. **G1 within 60 s in three runs out of three on both API levels, and G2's second alarm within 60 s
   on API 36** → D is built on ADR-0010 as written. G2's lateness on API 26, whatever it is, goes
   into *Known gaps* with the number, and into `DOD.md`'s `minSdk` item as evidence for phase 2.
2. **G1 holds, and G2's second alarm is more than 60 s late on API 36** → D is still one exact alarm,
   since a firing posts every Dose between its armed time and now (ADR-0010), so the second Dose
   arrives at the second alarm's real time and is not lost. The measured lateness goes into *Known
   gaps*, and the Snooze length (§7) may not be shorter than it.
3. **G1 late by more than 60 s in any run, or the process not started at all** → D stops before it
   starts. A new ADR superseding ADR-0010 weighs `setAlarmClock()` against what G1 measured. B, C and
   E's screens continue; the phase's Readings wait for the new mechanism.

G3 is taken with the same apparatus but is not a verdict input: it chooses between the two fallback
banner bodies in §6, by a threshold also fixed now (15 minutes).

### The verdict, taken *(2026-11-13)*

> **Verdict 1 on API 36; API 26 is unread, not failed.** The third verdict did not come in. Verdict
> 1's API 26 condition cannot be met on the emulators: the API 26 image never puts AlarmManager into
> idle, so its runs are not deep-Doze readings. Whether D waits for an API 26 reading on a real phone
> is the owner's call.

**API 36 (G1, G2): verdict 1's conditions hold.** G1 fired 322, 312 and 508 ms late, three runs of
three, each with `deviceidle get deep` reading `IDLE` at the firing, the process started from killed
and `posted=true`. G2's second alarm, armed from inside the first firing, fired 5 ms and 6 ms late in
two runs, with Doze forced across both firings. The alarms read `exactAllowReason=permission
flags=0x5` in `dumpsys alarm`, and the second ones carried a live `device_idle` policy term, so
AlarmManager was idling. D is built on ADR-0010 as written.

**API 26: not a reading.** The `urgent-api26` image (`android-26` `google_apis` x86_64, the only API 26
image on this machine) has no significant-motion sensor: `dumpsys deviceidle` shows
`mMotionSensor=null`. On Android 8 that makes `DeviceIdleController` skip `AlarmManager.setIdleUntil`,
so `deviceidle get deep` reads `IDLE` while `dumpsys alarm` shows no *Idling until* and
`mAllowWhileIdleMinTime=+5s0ms`, not the 9-minute idle limit. Its G1 (31, 65, 28 ms) and G2 second
alarms (0, 1 ms) are what an awake API 26 phone does, and say nothing about the allow-while-idle
limit G2 was set to catch. `dumpsys deviceidle step deep` and a fresh `force-idle deep` did not change
it. *Known gaps* and `DOD.md`'s `minSdk` item carry that the number is still owed.

**G3: the first banner body.** The fallback landed 2 min 15.5 s, 2 min 16.0 s and 1 min 44.0 s late on
API 36, inside 15 minutes in every run, so §6 takes the first body.

**What neither verdict anticipated:**

- **G3 measures the fallback's window, and the window grows with the lead.** Each fallback alarm read
  `window=+2m14s999ms` in `dumpsys alarm`, which is three quarters of the probe's three-minute lead,
  and every landing fell inside it. Other apps' inexact alarms on the same phone read windows of
  22 min 30 s and 1 h. A real Reminder armed hours ahead, such as 21:00 for 9:00, may land far later
  than G3 shows. R4 should arm with a realistic lead before §6's choice is treated as settled.
- **A never-opened fresh install's alarm is deferred about a year.** It sat in standby bucket 50
  (*never*), and `dumpsys alarm` read `app_standby=+364d`. One launch moved it to *active*. A user
  always opens the app to save a Schedule, so the risk lands on readings, not users: every D and E
  reading starts by opening the app once.
- **`am kill <pkg>` leaves a cached process alive on API 26.** `am kill --user 0 <pkg>` kills it.
  Confirm `pidof` reads empty either way, as *Readings* already says.

## 2. The schema (B)

Three tables join `medicines`, under ADR-0009's content.

| Table | Columns | Rules |
| --- | --- | --- |
| `schedules` | `id` TEXT UUID; `medicineId` TEXT → `medicines.id` **ON DELETE CASCADE**, indexed; `times` TEXT; `validFrom` TEXT; `validUntil` TEXT, nullable; `createdAt` INTEGER | `times` is the Times as `HH:mm`, sorted, distinct, comma-joined, never empty: a Schedule is never queried by one of its Times, so a column beats a child table. `validFrom` is the local date-time of the save (ADR-0009 amendment, §3); `validUntil` is the local date-time of the edit that ended it, null while in force. At most one row per Medicine is in force (*Editing a Schedule*, below) |
| `dose_records` | `medicineId` TEXT, **no foreign key**; `scheduledAt` TEXT; `answer` TEXT; `answeredAt` INTEGER; `medicineName` TEXT; `medicineAmount` TEXT; primary key (`medicineId`, `scheduledAt`); index on `scheduledAt` | no cascade, so a deleted Medicine's answers survive (ADR-0009). `answer` is an enum by name, `Taken` only until phase 2. Inserted with `IGNORE`: a Dose answered twice keeps its first answer and its first time |
| `snoozes` | `medicineId` TEXT → `medicines.id` **ON DELETE CASCADE**; `scheduledAt` TEXT; `until` INTEGER; primary key (`medicineId`, `scheduledAt`) | an answer deletes the row, a second Snooze replaces it (ADR-0009) |

**`scheduledAt` and `validFrom` are local date-times stored as fixed-width text**,
`yyyy-MM-dd'T'HH:mm` through one formatter in `Converters`, never `LocalDateTime.toString()`, which
drops `:00` seconds on some values and keeps them on others: the day query is a string range
(`>= '2026-11-13T00:00' AND < '2026-11-14T00:00'`), and only a fixed width sorts as time. A local
date-time and not an instant, because a Time is wall-clock in the phone's current zone (ADR-0009).

**Decided by the owner, 2026-11-13: migrate to schema 2, not redefine schema 1.** That means **bump
`APP_SCHEMA_VERSION` to 2 with `MIGRATION_1_2`**, three `CREATE TABLE`s and their indices transcribed
from the exported `2.json`, under the five items of `DOD.md`'s standing schema gate. ADR-0001 still
permits redefining schema 1, because nothing has reached a stranger. But 0.2.0 is a release build on
internal testing: a phone holding it that takes this phase's build over the top reads `user_version`
1, the launch gate answers `Open`, and Room throws on the identity hash, because a release build has
no fallback. Redefining is safe only if every phone holding 0.2.0 uninstalls first, and that is a
promise a docs line cannot keep. The migration is the cheap kind (`Migrations.kt`: nothing existing
is read or rewritten), it gives R3 a real schema bump to cross through `PackageReplacedReceiver`,
which is the path ADR-0003 says the receiver exists for, and it rehearses the gate phase 2 makes
binding. The cost: the "first migration" in `PLAN.md`'s Phase 3 and ADR-0009 becomes the second, and
both are amended.

Every schema change in this phase lands in B's one version (`Migrations.kt`, *Batch a phase's changes
into one version*), and schema 2 stays amendable only until a release carries it. *Checkpoints*
holds every release PR from B until E has merged, so a column C, D or E finds missing amends B's
migration and `2.json`, not a 3. Once a release carries schema 2 it is frozen: a column F or later
finds missing is `MIGRATION_2_3`.

**Editing a Schedule starts a new one** (ADR-0009: "an edit starts a new one instead of rewriting the
old"). A save that changes a Medicine's Times sets the Schedule in force's `validUntil` to the save's
local date-time and inserts a new Schedule whose `validFrom` is that same instant; a save that
removes every Time only ends the old one; a save that leaves the Times as they were writes no
Schedule. So Doses already Due or Taken today stay as they were, under the Schedule that made them,
and a Time added for earlier today first comes Due tomorrow. A Time removed for later today has no
Dose today.

`DoseRecordDao` and `ScheduleDao` return `Flow` for screens and have one-shot `suspend` reads for the
receivers, the same split as `MedicineDao`.

## 3. The derivation (B)

Pure functions in `data/Doses.kt`, taking a clock and a zone as arguments, never reading either:

| Function | Answers | Fails toward |
| --- | --- | --- |
| `dosesOn(day, schedules, records, snoozes)` | every Dose of a local date, each with its Medicine, its `scheduledAt`, its answer if any and its Reminder time (the Time, or the Snooze's `until`) | a Dose that does not exist (never reminded, not on Today) or exists twice (two Reminders for one Dose) |
| `stateAt(dose, now)` | Due once its Time has passed unanswered, Taken once answered, neither before its Time | Due shown before the Time, inviting an early answer that silences the Reminder |
| `nextReminderTime(now, …)` | the earliest Reminder time after `now` across all Medicines, unanswered Doses only, today and the next day | an alarm armed past a Dose, which then never reminds |
| `dosesToPost(armedFor, now, …)` | the unanswered Doses whose Reminder time is in `[armedFor, now]` and inside the re-post bound (§5) | a Dose between the armed time and a late landing left unposted |
| `dosesToRepost(now, …)` | for a re-plan with no armed time: every Due Dose inside the re-post bound (§5) | a stale Reminder posted as if current, or a fresh one not posted |

Rules the functions encode, each a test case (*Tests*):

- **A Schedule has Doses only from its `validFrom` up to its `validUntil`.** A Medicine saved at
  10:00 with a 9:00 Time starts tomorrow at 9:00; it is not Due at 10:00 for a Dose that never
  existed. ADR-0009 says valid-from *date*; this phase makes it the save's local date-time, and amends
  the ADR.
- **An edit mid-day changes only what is still ahead.** At noon, adding 8:00 and 21:00 to a 9:00
  Schedule leaves the 9:00 Dose Due (or Taken) under the old Schedule, adds a 21:00 Dose today, and
  starts 8:00 tomorrow; removing 21:00 at noon removes only today's 21:00.
- **A daylight-saving gap** (02:30 on the spring-forward day) is one Dose, reminded at the instant
  `ZonedDateTime.of` resolves it to (03:30). **An overlap** (01:30 twice on the fall-back day) is one
  Dose, reminded once, at the first.
- **A time-zone change** moves no Dose: `scheduledAt` is wall-clock, so an answered 9:00 stays
  answered, and an unanswered one is reminded at 9:00 in the new zone.
- **A record whose Time is no longer in the Schedule** still appears on its day as Taken.
- **`nextReminderTime` is strictly after `now`.** A firing that re-arms at its own minute would loop;
  one that skips a Dose falling on that same minute loses it, which is why the firing posts
  `[armedFor, now]` before it arms past `now`.

## 4. The asks (E)

`PLAN.md` rule 4, in this phase's two rows. Both are made **once, at the save of the first Schedule**:
"first" is read from the data (no Schedule existed before the save), so nothing new is stored, and a
user who deletes every Medicine and starts again is asked again.

| | API 26–30 | API 31–32 | API 33 | API 34+ |
| --- | --- | --- | --- | --- |
| Notifications | no runtime permission: nothing asked; if switched off in settings, Today's blocked banner | same | the app's own explanation, then the system dialog through `rememberNotificationPermissionAsk` | same |
| Exact alarms | always allowed: nothing asked | granted by default: asked only if `canScheduleExactAlarms()` reads false | same | **refused by default** for a new install: the app's own explanation, then `openExactAlarmSettings()`; re-read on resume |
| On a refusal | — | Today: *Reminders may be late* (exact) | Today: *Reminders will not arrive* (notifications), and the exact hand-off is not offered after it, since an on-time notification nobody can see buys nothing | same |

The explanation is a screen of the app's own, pushed after the editor's save and before Today: the
system dialog is never the first thing the user sees (ADR-0003). *Not now* on either step goes to
Today, where the banner is the route back. **No ask happens before a Medicine has a Time**: saving a
Medicine without Times asks for nothing, and a phone that never sets a Time never sees either
screen.

What the user sees after a refusal is Today's banners (§6). Both are re-read on every resume, never
remembered from the ask's outcome, the idiom `SettingsScreen` already uses for notifications.

**Decided by the owner, 2026-11-13: the Settings *Remind me* switch goes.** It is the template's, it defaults to off, and it
spends the notifications ask the moment it is switched on, which is not the moment rule 4's table
names. With Times on a Medicine, a global off-switch is a second way to stop Reminders beside
`CONTEXT.md`'s (end the Schedule), and one that fails silently: switched off once and forgotten, it
leaves every Schedule saved and nothing reminded. So **remove the switch, its preference key and
its banner**, and let Today's banners carry the refusal states. What goes, as `optional-modules.md`'s
reminders recipe lists it:

- Settings' *Reminders* section: the `SwitchRow`, its `rememberNotificationPermissionAsk` call, and
  the `WarningBanner` with its resumed re-read, which moves to Today (§6);
  `SettingsUiState.remindersEnabled` and `SettingsViewModel.setRemindersEnabled`.
- `AppPreferences.remindersEnabled`, `setRemindersEnabled`, `Keys.REMINDERS_ENABLED` and its place in
  `Keys.SETTINGS`, and the reminder lines in `AppPreferencesTest`.
- Every `settings_reminders*` string in every language, English and Polish: a Polish string left
  behind is an orphan `TranslationTest` fails on.
- "reminders" in `settings_restore_hint` and `settings_restore_confirm_body`, in every language, or
  *Restore default settings* promises to reset a setting that is gone.

Copy, English:

| Key | English |
| --- | --- |
| `ask_notifications_title` | Allow reminders |
| `ask_notifications_body` | Take your meds shows a notification at each time you set. Android asks you to allow that first. |
| `ask_notifications_action` | Continue |
| `ask_exact_title` | Let reminders arrive on the minute |
| `ask_exact_body` | Without this, Android can deliver a reminder several minutes late. The next screen has a switch for Take your meds. |
| `ask_exact_action` | Open settings |
| `ask_not_now` | Not now |

## 5. The Reminder and the re-plan (D)

**One alarm, one `PendingIntent`.** `work/ReminderPlanner.kt` arms `setExactAndAllowWhileIdle` for
`nextReminderTime`, or `setAndAllowWhileIdle` when `canScheduleExactAlarms()` reads false at that
moment, on a single `PendingIntent` (one request code, `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`)
carrying the time it was armed for, so arming again replaces the alarm rather than adding one. No
Doses ahead means no alarm, and the previous one cancelled. `ReminderAlarmReceiver` (exported false)
posts `dosesToPost(armedFor, now)` and re-plans.

**Every entry point asks `schemaBlocksBackgroundWork()` first** (ADR-0001): the alarm, the action
receiver, the time-change receiver and the grant receiver all start the process with nobody looking.

The re-plan, by trigger:

| Trigger | Re-posts still-Due Doses? | Then |
| --- | --- | --- |
| the alarm firing | only `[armedFor, now]` | arm the next |
| Taken (notification or Today), Snooze | no; that Dose's Reminder is cancelled | arm the next |
| Schedule saved, Medicine deleted | no; a deleted Medicine's Reminders are cancelled | arm the next |
| boot, update (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`) | **yes**, `dosesToRepost` | arm the next; ensure the sweep |
| `TIME_SET`, `TIMEZONE_CHANGED` | **yes** | arm the next |
| `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` | no | arm the next, now exact |
| the daily sweep | no | arm the next: the backstop after a revoked grant (ADR-0003's amendment) |
| the app opened | no; Today shows what is Due | arm the next: covers a force-stop |

A re-plan re-posts only where there was no armed time to start from (ADR-0010). The sweep and the app
opening do not re-post, so a Reminder the user swiped away stays away until the Follow-up exists.

**Decided by the owner, 2026-11-13: how old a Due Dose may be before a re-plan stops re-posting its
Reminder.** **2 hours after its Reminder time, and never once a later Dose of the same Medicine has
reached its Time**, one constant `DUE_REPOST_LIMIT` that phase 2 adopts as the moment the Follow-up
has run out and the Dose becomes Missed, so the app has one number for "this Dose is no longer
current", not two. The direction that matters: a bound too long lets a reboot at 20:00 post the 9:00
Reminder looking current, an hour before the 21:00 one, which is how a Reminder becomes a second dose
of the same Medicine; the next-Dose cap closes that whatever the number. A bound too short leaves a
phone switched off over 9:00 and on at 9:30 silent, which is the case ADR-0010 re-posts for. Two
hours covers a night's power-off ending in the morning and a dead battery charged over breakfast. The
same bound applies to `dosesToPost`, so a fallback alarm landing very late does not post a stale
Reminder either. Today is not bound by it: the Dose stays Due there, answerable, all day.

**The Reminder.** Posted on `reminders` under a **tag** built from its Dose
(`dose:<medicineId>:<scheduledAt>`) and a fixed id, so posting the same Dose again replaces its
Reminder (ADR-0010) and no hash can make two Doses share one. `setOnlyAlertOnce`, so a re-post of a
Reminder still in the tray does not sound twice. `CATEGORY_REMINDER`. `VISIBILITY_PRIVATE` with a
public version that names no Medicine, which defers to the user's own lock-screen setting: on a phone
set to hide sensitive content, the lock screen says a Reminder is due and nothing more. Tapping it
opens Today through an activity `PendingIntent`, never a receiver trampoline (refused from API 31).
Taken is a broadcast action to `ReminderActionReceiver` (exported false), whose `Intent` data is a
`dose:` URI built from the same key: `PendingIntent` equality ignores extras, so two Reminders'
*Taken* buttons sharing one `Intent` would record the wrong Medicine.

Taken writes the `DoseRecord` with the Medicine's name and Amount copied in (ADR-0009), deletes any
Snooze, cancels the Reminder and re-plans, all inside `rebuildInBackground`.

The receivers this adds go in `ManifestWiringTest` beside boot and update. No new permission: the
merged set stays the eight `aab-permissions.py` read in phase 0, R1a, and R10 confirms it.

Logging: the planner and both receivers log `Reminders armedFor=… firedAt=… posted=<n> exact=<bool>`
with times and counts only, never a Medicine's name or Amount (ADR-0008): the readings need the line,
and logcat is not the place for health data.

Copy, English:

| Key | English |
| --- | --- |
| `reminder_text` | %1$s · your %2$s dose |
| `reminder_text_public` | A dose is due at %1$s |
| `reminder_action_taken` | Taken |
| `channel_reminders_description` | A notification at each time a medicine is due. |

`reminder_text`'s arguments are the Amount and the Time; the title is the Medicine's name, not a
string. Nothing says *should* or *take now* (`CONTEXT.md`).

## 6. Today and the Times (C, E)

**The Times** go in the Medicine editor: add a Time, remove a Time, every day. Saving with no Times
saves a Medicine with no Schedule, which reminds nothing (and is still indistinguishable from As
needed until phase 3). The Medicine's detail screen shows its Times.

**Today** is the first tab and the root of the back stack; Medicines is the second. It lists every
Dose of the current local date from `dosesOn`, in Time order, each showing:

- the Medicine's name, its Amount and the Time;
- its state: **Due** (Time passed, unanswered), **Taken** with the answer time, or neither for a
  Dose still ahead, which shows only its Time;
- for a Due Dose with a pending Snooze, the time it is snoozed until;
- **Mark as taken** on a Due Dose only. A Dose still ahead has no button: an early answer would
  silence its Reminder (§3, `stateAt`). The button appears once the Dose is Due; the Reminder's Taken
  action is unchanged. Marking Taken on Today offers *Undo* in a long-duration snackbar, which
  deletes the record just written; there is no later un-answering in this phase.

Above the list, re-read on every resume and shown only while at least one Schedule exists:

- **Notifications blocked or the `reminders` channel muted** → *Reminders will not arrive*, with the
  hand-off to the app's or the channel's settings (the logic `SettingsScreen` already has, moved).
- **API 31+ and `canScheduleExactAlarms()` false** → *Reminders may be late*, with
  `openExactAlarmSettings()`. Its body is one of two, chosen by G3 against 15 minutes: the first if
  the fallback landed within 15 minutes in every run, the second if not.

The tip ask card moves from `ui/medicines/` to Today, the home screen it belongs to (ADR-0007), and
`takenDoseCount` becomes the count of Taken records. `scripts/edge-to-edge.py`'s `TAB_BAR` and
`HOME_TAB` become *Today*, its `SCENES` gain Today (empty, seeded, with each banner) and the editor
with Times, and the debug seeder seeds Schedules and a Taken record so a seeded Today shows both
states.

**Decided by the owner, 2026-11-13, from a design pass (Fable): the layout of Today and of the
Times.**

- **Today groups Doses under one header per Time**, in Time order. The header is the Time in
  `titleMedium`, `onSurface`. A snoozed Dose stays under its original Time and shows *Snoozed until
  HH:MM*.
- **Due** is a filled `Card` with the word *Due* and a filled-tonal *Mark as taken* button, at least
  48dp, end-aligned on its own line; the card itself is not clickable. **Upcoming** is an
  `OutlinedCard` with no state label and no button. **Taken** shows a check icon and *Taken at
  HH:MM*, with no button. A row never disappears or re-sorts when answered. Colour is never the only
  cue, and Due is not tinted amber.
- **Both banners:** the blocked banner (`error`) sits above the late banner. The late banner is on
  `tertiaryContainer`, which needs a tone parameter on `WarningBanner` in `ui/common/Surfaces.kt`
  (today it is `errorContainer` only).
- **The editor's Times** are a section between Note and Save, under a `SectionHeader` (*Times*), with
  *Every day* in `onSurfaceVariant`. One row per Time, at least 48dp: a tap edits it in the M3
  `TimePicker` dialog, and a trailing icon button removes it (content description *Remove 08:00*).
  *Add a time* opens the picker. Rows stay sorted, and a duplicate Time is ignored. With no Times the
  empty line reads *No times set. This medicine is not reminded.*

Copy, English:

| Key | English |
| --- | --- |
| `tab_today` | Today |
| `today_empty` | Nothing scheduled today. Give a medicine its times in Medicines. |
| `dose_due` | Due |
| `dose_taken_at` | Taken at %1$s |
| `dose_snoozed_until` | Snoozed until %1$s |
| `dose_mark_taken` | Mark as taken |
| `dose_marked_taken` | Marked as taken |
| `dose_undo` | Undo |
| `today_blocked_title` | Reminders will not arrive |
| `today_blocked_body` | Android is set to hide reminders from this app, so nothing will be shown when a dose is due. |
| `today_blocked_action` | Open notification settings |
| `today_late_title` | Reminders may be late |
| `today_late_body_minutes` | Android lets this app set only inexact alarms, so a reminder can arrive a few minutes after its time. |
| `today_late_body_long` | Android lets this app set only inexact alarms, so a reminder can arrive well after its time, sometimes by an hour or more. |
| `today_late_action` | Allow exact alarms |
| `schedule_times_label` | Times |
| `schedule_add_time` | Add a time |
| `schedule_remove_time` | Remove %1$s |
| `schedule_time_picker_title` | Choose a time |
| `schedule_time_picker_confirm` | OK |
| `schedule_every_day` | Every day |
| `schedule_no_times` | No times set. This medicine is not reminded. |
| `medicine_times` | Every day at %1$s |

## 7. Snooze (F)

The Reminder's second action. It writes the `snoozes` row (`until` = now + the length), cancels the
Reminder, and re-plans; `nextReminderTime` reads the row, so the Snooze's end is a Reminder time on
the same alarm (ADR-0010) and survives a reboot or an update through `dosesToRepost`. A second Snooze
replaces the row; an answer, from the notification or Today, deletes it. The Snooze end is an
instant, not a local date-time: ten minutes are ten minutes across a clock change.

**Decided by the owner, 2026-11-13: the Snooze length is fixed, 10
minutes**, as `CONTEXT.md` already defines Snooze ("by a fixed time"). One button with the number on
it is answerable from the lock screen without reading a choice; a chosen length is a preference with
its own row in Settings and its own copy, and can be added later without touching the schema. Ten,
not five, because Doze's allow-while-idle limit has historically been one alarm per nine minutes per
app, and a Snooze shorter than that is the case most likely to land late; under the gate's second
verdict the length may not be shorter than the lateness measured. Not fifteen, because a Snooze is
"in a moment", and a person who wanted an hour would leave the Reminder in the tray.

Copy, English:

| Key | English |
| --- | --- |
| `reminder_action_snooze` | Snooze %d min |

---

## Tests

`PLAN.md` rule 3: the phase that introduces a safety value introduces its test. This phase introduces
the first ones the app has. Each is a JVM test over a pure function with an injected clock and zone.

| Test | Proves | Fails in the direction of |
| --- | --- | --- |
| `DoseDerivationTest` (`dosesOn`, `stateAt`) | two Times; a Schedule saved mid-day has no Dose before `validFrom`; an edit mid-day keeps today's earlier Doses under the old Schedule, adds a later Time today and starts an earlier one tomorrow; 23:59 and 00:00; a DST gap (one Dose, at the shifted instant) and overlap (one Dose, once); a zone change moves no Dose; a record whose Time was removed still shows Taken; a Dose ahead is not Due | a Dose that never exists, so never reminds, or exists twice, so reminds twice |
| `NextReminderTest` (`nextReminderTime`) | the earliest across Medicines; answered Doses excluded; a Snooze earlier than the next Time wins; after the day's last Time, tomorrow's first; strictly after `now`; no Doses means none | an alarm armed past a Dose, which then never reminds |
| `DosesToPostTest` (`dosesToPost`) | `[armedFor, now]` inclusive at both ends; two Medicines at the same minute both posted; a late landing posts every Dose it covers; answered Doses skipped | a Dose between the armed time and a late landing left unposted |
| `RepostBoundTest` (`dosesToRepost`, the bound in `dosesToPost`) | posted just inside `DUE_REPOST_LIMIT`, not at or past it; not once a later Dose of the same Medicine has reached its Time; measured from the Reminder time, so a Snooze's end counts | **chiefly** a stale Reminder posted as current, which can become a second dose of the same Medicine; secondarily a phone off over 9:00 left silent |
| `DoseKeyTest` (the tag and the `dose:` URI) | round-trips to the same Medicine and `scheduledAt`; differs for two Medicines at one minute and one Medicine at two Times | *Taken* recorded against the wrong Dose: the app saying a medicine was taken that was not |

Instrumented, beside `MedicineDaoTest`: `DoseRecordDaoTest` proves a Dose answered twice keeps one
record with its first answer time (fails toward a Taken count of two, which the tip ask reads now and
Supply will count down twice in phase 4), and that deleting a Medicine deletes its Schedules and
Snoozes and keeps its records. `MigrationTest` 1 → 2 reads the Medicines back, and
`SchemaGateTest` asserts the gate lets 1 → 2 through (`DOD.md`'s standing gate, items 3 and 4).
`ManifestWiringTest` gains the new receivers.

Device behaviour is not here: whether the alarm fired on its minute is a reading.

## Documents this phase amends

- **ADR-0003**: dated amendment. `BootReceiver` and `PackageReplacedReceiver` now re-plan and re-post
  rather than doing almost nothing; the sweep arms and does not re-post.
- **ADR-0007**: dated amendment. The tip ask card moves from `ui/medicines/` to Today, the new home
  screen, and its count of recorded items becomes the count of Taken `DoseRecord`s.
- **ADR-0009**: dated amendment. `validFrom` is the save's local date-time, not a date, so a Schedule
  saved mid-day has no earlier Dose that day; an edit ends the Schedule in force at the save's
  instant (`validUntil`) and starts the new one at that same instant, so today's Doses before the
  edit keep the old Schedule. Its last consequence's "first migration (phase 3)" becomes the second.
- **ADR-0010**: dated amendment. The re-post bound and the next-Dose cap, the same bound on a late
  firing, the tag-keyed Reminder, the private lock-screen version, and the gate's verdict.
- **`CONTEXT.md`**: *Due* is a Dose whose Time has passed unanswered, and a Dose before its Time has
  no state; *Re-plan* in the domain table as a code word, never in the UI.
- **`PLAN.md`**: the Phase 1 Status entry closed with its readings; a *Phase 1* paragraph; Phase 3's
  "first schema migration" becomes the second.
- **`docs/optional-modules.md`**: *Reminders and WorkManager* stops listing Settings' *Reminders*
  section and the reminders in the restore strings, which this phase removes (§4), and points at the
  first Schedule's ask as the shape to copy for a first ask.
- **`docs/privacy-policy.md` and `docs/play-app-content.md`, together**: notifications are asked when
  the first Medicine gets a Time; alarms and reminders (`SCHEDULE_EXACT_ALARM`) gets its own line
  in the policy, and the Console's permission rows say when each is asked.
- **`docs/translator-brief.md`**: the Polish for Dose, Due, Taken, Snooze and Reminder fixed once,
  so a Reminder and Today agree and *dose* never translates as the Amount (`CONTEXT.md`).
- **`CLAUDE.md`**: Layout's `work/` line names the planner and the receivers.
- **`DOD.md`**: the API 26 readings as evidence in *Decide `minSdk`*; nothing to tick and nothing in
  *Before the next upload*, since the permission set does not change.
- **`scripts/edge-to-edge.py`**: `TAB_BAR`, `HOME_TAB`, `SCENES`, `SEED_WALK` (§6).

## Known gaps, deliberately

- **Nothing reminds between a reboot and the first unlock.** `BOOT_COMPLETED` arrives after unlock,
  and the database lives in credential-encrypted storage. Closing it means a device-protected copy of
  the Schedules and a `LOCKED_BOOT_COMPLETED` path, for a phone left locked after a restart.
- **A revoked exact-alarm grant silences the app until the next sweep or the next open.** Revoking
  cancels the alarm and stops the app with no broadcast (ADR-0003's amendment); the sweep at 09:00 is
  the backstop, so the gap can be up to a day. Closing it would take a second, WorkManager-armed
  layer per Reminder, the per-Dose work ADR-0003 rejected.
- **A force-stop does the same until the app is opened or the phone restarts.** Android cancels a
  force-stopped app's alarms; nothing the app owns runs until then.
- **Xiaomi and kin without autostart.** The ROM does not start the process for the alarm; phase 2's
  setup screen owns the hand-off and says what it costs, and only `scripts/device-gate.py` reads it.
- **An unanswered Dose stays Due all day, and leaves Today at midnight.** No Missed until phase 2 and
  no History until phase 3; a 23:30 Dose unanswered at midnight is answerable only from its Reminder,
  if it is still in the tray.
- **A Taken tapped by mistake on a Reminder cannot be undone in this phase.** Undo exists only on
  Today's snackbar; changing an answer after the fact belongs with History.
- **Close pairs on API 26–30**, by whatever G2 measures there (gate verdict 1). Still unmeasured:
  the API 26 emulator image cannot put AlarmManager into idle (§1, *The verdict, taken*), so the
  number needs a real API 26–30 phone.

## Readings

`PLAN.md` rule 3: filled in as the phase runs, from the device, with the command that read it — not
"it looked right". Derivations are proven by a test, not recorded here.

On the emulators (`emulator-test`), API 36 and API 26, debug build unless the row says otherwise;
`<pkg>` is `com.maksforge.takeyourmeds.debug`. A Xiaomi reading is phase 2's.

Device state each reading depends on, and how it is set:

- **Deep Doze:** `adb shell dumpsys battery unplug`, `adb shell input keyevent KEYCODE_SLEEP`,
  `adb shell dumpsys deviceidle force-idle deep`; confirm `adb shell dumpsys deviceidle get deep`
  reads `IDLE`. `adb shell dumpsys deviceidle unforce` and `dumpsys battery reset` after.
- **Process killed, not stopped:** press Home, `adb shell am kill <pkg>`, confirm
  `adb shell pidof <pkg>` prints nothing. **Never `am force-stop`**: it cancels the alarm, and the
  reading is then of nothing.
- **The armed alarm:** `adb shell dumpsys alarm | grep -A6 <pkg>` shows its time and whether it is
  exact; read it before every timed reading, so a reading that fires nothing is not mistaken for an
  alarm that was never armed.
- **The grant:** API 34+ fresh installs read refused; `adb shell appops set <pkg> SCHEDULE_EXACT_ALARM
  allow|deny` sets it, and a deny stops the app, as on a phone.
- **Notifications:** `adb shell pm grant|revoke <pkg> android.permission.POST_NOTIFICATIONS` for D's
  readings before E exists; E's readings start from a fresh install instead.
- **Fire times:** `adb logcat -s AlarmProbe Reminders` for the logged `armedFor`, `firedAt` and
  `lateMs`, and `adb shell dumpsys notification --noredact | grep -A3 'dose:'` for the posted
  Reminder.

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| G1 | The gate: an exact alarm in deep Doze, process killed, posts within 60 s of its time, three runs | `adb shell am broadcast -n <pkg>/app.takeyourmeds.debug.AlarmProbeReceiver -f 0x00000020 --ei minutes 3 --ez exact true`, then deep Doze and kill as above, then `adb logcat -s AlarmProbe` | 2026-11-13. **API 36** (`pixel-api36`, grant `allow`): `lateMs=322`, `312`, `508`; `deviceidle get deep` `IDLE` at each firing, `posted=true`, a new pid each time. **API 26** (`urgent-api26`): `lateMs=31`, `65`, `28`, **not deep Doze**: `mMotionSensor=null`, no *Idling until* in `dumpsys alarm` (§1, *The verdict, taken*). `-f 0x00000020` is needed because a fresh install is a stopped package. On API 26 the kill is `am kill --user 0 <pkg>`. |
| G2 | The gate: the second of two exact alarms five minutes apart, in deep Doze | as G1 with `--ez chain true` | 2026-11-13. **API 36**, two runs with Doze forced across both firings: first `lateMs=259` and ≤318 (that log line rotated out, so the figure is the notification's `when`), second **`5`** and **`6`**. The second alarm was armed between the firings, `flags=0x5` with a live `device_idle` term. **API 26**: second `0` and `1`, not deep Doze, as G1. |
| G3 | The fallback's lateness in deep Doze with the grant refused, three runs (chooses §6's banner body) | `appops … deny`, then as G1 with `--ez exact false` | 2026-11-13, **API 36**: `lateMs=135510`, `136010`, `104047` (≤ 2 min 16 s). Each alarm read `window=+2m14s999ms flags=0x20`. The third run is on a read-only `fold-api36`, the same image and fingerprint, after another session reinstalled over `pixel-api36`; it read *Idling until* in `dumpsys alarm` and was opened once to leave standby bucket 50. Under 15 min, so §6 takes the first body. API 26 not run: there is no grant to refuse below API 31. |
| R1 | A Reminder on its minute in deep Doze, from the real planner | a Medicine with a Time three minutes ahead, deep Doze, kill, `adb logcat -s Reminders` | |
| R2a | After a reboot, a Reminder whose Time falls after boot arrives on its minute | arm, `adb reboot`, `adb logcat -s Reminders` | |
| R2b | After a reboot, a Dose whose Time passed while the phone was off is re-posted at boot inside the bound, and one past the bound is not | Times one before and one beyond the bound, `adb emu kill` over both, boot, `dumpsys notification` | |
| R3a | After an update, a Reminder armed before it arrives on its minute | arm, `adb install -r` the next build, `adb logcat -s Reminders` | |
| R3b | An upgrade from 0.2.0's schema keeps the Medicines and opens (`DOD.md`'s standing gate, item 5) | `git checkout v0.2.0`, `./gradlew installDebug -PreleaseShapedDebug`, two Medicines, then this phase's build `-PreleaseShapedDebug` over it, open | |
| R4 | With the grant refused, the Reminder arrives through the fallback, its lateness logged; Today shows *Reminders may be late*; granting through the hand-off removes the banner on resume and the next alarm reads exact | fresh install on API 36, a Time ahead, `logcat`, then the banner's action, then `dumpsys alarm` | |
| R5 | *Taken* on a Reminder with the process dead and the screen locked writes the record, removes the Reminder and shows Taken on Today | tap from the lock screen, then pull the database **with its `-wal`** and `PRAGMA wal_checkpoint(TRUNCATE)` on the host, `SELECT * FROM dose_records` | |
| R6 | Snooze returns the Reminder ten minutes later, and survives a reboot during it | tap Snooze, `adb reboot`, `adb logcat -s Reminders` | |
| R7 | A time-zone change re-plans: an answered 9:00 is not reminded again, an unanswered one is reminded at 9:00 in the new zone | automatic zone off, zone changed in Settings → System → Date & time; the command recorded here when taken | |
| R8 | The asks: on API 36 the first Schedule's save shows the app's screen, then the dialog, then the exact-alarm hand-off; a refusal of each shows its banner; on API 26 nothing is asked | fresh install, add a Medicine with a Time, screenshots of each step | |
| R9 | No ask before a Time: a Medicine saved without Times asks nothing | fresh install, add a Medicine with no Time, `adb shell dumpsys package <pkg> \| grep POST_NOTIFICATIONS` still not granted, no dialog | |
| R10 | The merged permission set is unchanged | `python3 scripts/aab-permissions.py` against `bundleRelease` | |
| R11 | Today and the editor with Times render edge to edge, English and Polish, light and dark | `python3 scripts/edge-to-edge.py` | |
