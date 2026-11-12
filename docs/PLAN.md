# Roadmap

Sequence and status only. The reasoning behind a decision lives in [`docs/adr/`](adr/); the words
live in [`CONTEXT.md`](../CONTEXT.md); commands, layout and house rules live in
[`CLAUDE.md`](../CLAUDE.md).

**This file is the record, not the worklist.** What is still open — the boxes to tick, with the
device state and the commands to read it — lives in [`DOD.md`](DOD.md), which stays short enough to
open every session. Read the phase you are in here; read `DOD.md` to know what to do next.

**A phase being planned or built gets its own file** — `phase-N.md` beside this one — so working on
it costs the phase rather than the whole history. This file stays deliberately thin: one paragraph
per phase, no task lists. The detail is written when the phase opens, not now. The app this template
was extracted from and the first app built from it settled on exactly this split independently.
[`phase-template.md`](phase-template.md) is the shape a phase file settled into; copy it on the day.

## Status

- [x] **Phase 0** — Skeleton, done 2026-11-12 ([`phase-0.md`](phase-0.md)): Items become Medicines with an Amount and a Note; the photo pipeline
  and the manual backup export/restore stripped, `AppBackupAgent` kept for the database alone (Room
  and the reminder machinery stay); the `reminders` channel raised to high importance; the tip ask
  counted in Taken Doses; the palette and the mark. ADR-0002, 0005 and 0007 amended; ADR-0003 needed
  none. The themed launcher icon is still unread: the emulator's launcher tints no icon
- [ ] **Phase P** — The pipeline: upload key, Play entry, Play secrets, a green internal upload
- [ ] **Phase 1** — The mechanism on the phone: a Medicine with one or more Times every day; a
  Reminder at the minute through an exact alarm, falling back when the grant is refused (ADR-0010);
  Taken and Snooze on the notification; Today showing each Dose as Due or Taken (ADR-0009); how
  old a Due Dose may be before a re-plan stops re-posting its Reminder, since Missed waits for phase 2.
  Asks: notifications and exact alarms, when the first Schedule is saved. Readings: a Reminder on
  time in deep Doze, after a reboot, after an app update, and with the exact-alarm grant refused
- [ ] **Phase 2** — Safe for a stranger ← **the door: closed testing opens here.** The Follow-up and
  Missed; Skip; editing a Schedule without rewriting past Doses, and deleting a Medicine with its
  answered Doses kept in History, behind a confirmation that says it cannot be undone (`CONTEXT.md`); the setup screen for battery optimisation and autostart, with what
  a refusal costs; first run; the
  not-a-medical-device line (ADR-0011). It freezes the `applicationId`, `minSdk` and the permission set,
  and brings ADR-0001's 1.0 rule forward to the door: a tester's Dose history is real, so every
  schema change after it carries a tested migration. Readings: Xiaomi with and without autostart — the one reading that
  needs a real phone
- [ ] **Phase 3** — Schedules beyond daily: chosen weekdays, every N days, an End date; As-needed
  Medicines; History per Medicine. The first schema migration, under ADR-0001's gate
- [ ] **Phase 4** — Supply: a count per Medicine, decreased by each Taken Dose, and a running-low
  Reminder
- [ ] **Phase N** — Ship shape: listing in English and Polish, screenshots, the Health apps
  declaration, Data safety ("no data collected"), the App content answers

---

## Rules that cut across every phase

### 1. Decide where the door is, early

**The door** is the first build a stranger installs — closed testing, for a new personal Play
account. Everything that reaches a tester's phone is harder to change afterwards: a stored key, a
permission, a `minSdk`, the `applicationId`. So name the phase the door sits after, and write down what
it freezes. The closed-test window is calendar time and other people's replies, so later phases can
run while it waits.

**Do not plan a decision on the testers' answers.** The first app built from this template left two
phases' open questions — defaults, whether a behaviour was too clever — to be asked of its closed-test
testers. The survey that did go out asked none of them, and both closed *unanswered, on the values
that shipped*. What carried
the decisions was the device readings. Decide on a reading, ship a default, and let the mail hand-off
(`ui/support/`, live from the door) tell you if it is wrong.

### 2. When the two halves of a phase cost wildly different amounts, they are two phases

The first app built from this template split every one of its middle phases along cost or along
kind, because in each case a bundle of a cheap item with an expensive one — or a feature with the
safety work it depends on — hid which half was blocking. However neatly one sentence describes both, two costs are two phases.

### 3. A phase proves itself on the phone, not in a test

Every phase ends with readings taken off a real device and written into its `phase-N.md`, with the
command that produced them. A green test run says the code does what the test asked; the phone says
what the ROM allowed. Vendor ROMs are where those two part company.

**But the rule has a second clause, and leaving it out is how a roadmap arrives at no tests at all.**
Most of the risk in an app this size is device behaviour, and none of that is reachable by a unit
test — but the pure functions that *compute or bound a safety value* are the exception, and each of
them fails in exactly the direction the app exists to prevent. So: device behaviour is proven by
measurement; a safety value is proven by test, and **the phase that introduces one introduces its
test**. For the first app built from this template that came to five tests across the whole roadmap
— a window that crosses midnight, two deadlines resolved against a clock, a ramp whose bottom end
turns the backlight off over a live touchscreen, the width of a window that swallows touches, and a
restored intent that had to be refused. Not a testing culture. Five tests standing in front of the
properties `CLAUDE.md` calls load-bearing.

⚠ **Reconcile the list when the roadmap closes, and expect the count to be wrong.** That app planned
four and shipped five: the fifth arrived because a device reading found a fault no test had been
written for. That is the rule working, not an exception to it — the top of a ramp is not visible on a
device until it strands somebody.

### 4. Ask for nothing before the feature that needs it

An app of this shape ends up asking the user for three or four things, and most of them are hand-offs
to settings screens whose result cannot be read back reliably — `appops` has been caught lying about
`SYSTEM_ALERT_WINDOW` on a real phone. So the sequence is a rule rather than a phase:

**Every ask waits for the moment the feature needing it is switched on. The phase that introduces an
ask owns its hand-off, its re-read, and what the user sees when the answer is no.**

| Ask | Moment it is made | Phase |
| --- | --- | --- |
| Notifications (`POST_NOTIFICATIONS`, API 33+) | the first Schedule is saved, from the app's own screen | 1 |
| Exact alarms (`SCHEDULE_EXACT_ALARM`, off by default on API 34+) | right after notifications, as a hand-off to system settings; re-read with `canScheduleExactAlarms()` | 1 |
| Battery optimisation exemption | the setup screen, offered after the first Reminder has fired | 2 |
| Autostart (Xiaomi and kin) | the same setup screen; cannot be re-read, so the row states what a refusal costs and claims nothing | 2 |

No ask comes before the user has a Medicine with a Schedule, and an As-needed Medicine asks for
nothing.

**The third clause is the one that gets left out, and it is the one that generates support mail.**
These denials nearly all fail *silently*: a denied notification permission leaves a foreground
service running with no way to stop it, a denied autostart leaves reboot restore never firing, a
denied battery exemption leaves a schedule never firing. In each case the app knows and the user does
not. A re-read tells the app; **nothing tells the person holding the phone** unless a screen does.
`ui/common/Surfaces.kt`'s `WarningBanner` is the idiom for a state the app can re-read; a permanent
row that makes no claim is the idiom for one it cannot.

**Autostart is the case where even the re-read is out of reach.** It is not an appop and there is no
API for it, so no code of yours can tell a granted state from a denied one — only
`scripts/device-gate.py`, over `adb`, run by you. The clause survives it: the phase still owns the
hand-off and still owns saying what a denial costs, without claiming to know the answer. What changes
is only who can check.

**The same logic applies to side effects, not only to permissions.** A feature that makes one of the
phone's own controls go inert is a system control that appears broken, caused by you. The phase that
introduces the behaviour owns saying so.

---

## Phase 0 — Skeleton

The template's Items become Medicines (name, Amount, Note), so the placeholder domain is gone before
anything is built on it. Photos go: a Medicine has no picture, and the camera-free media pipeline is
weight with a Data safety answer attached. The manual export/restore goes with it, because its
reason was media; ADR-0005 gets an amendment for the manual half going. `AppBackupAgent` stays,
stripped to the database: static backup rules cannot checkpoint Room's WAL, which is the agent's
reason (ADR-0005). It still leaves preferences out, so the tip's once-per-install state stays per
install (ADR-0007). Auto Backup and device-to-device transfer carry the database to a new phone.
Room stays, because Doses are history read by day and by Medicine (ADR-0009). The reminder machinery
stays and is what phase 1 builds on; its `reminders` channel goes to high importance here, before
Phase P's first upload fixes it on any phone (ADR-0010). The tip ask's measure of use becomes ten
Taken Doses instead of ten Items (`TIP_ASK_AFTER_ITEMS`), since few users have ten Medicines. The
palette and the mark are chosen here, and the mark becomes the icon on maksforge.com and in the
profile README.

## Phase P — The pipeline

`DOD.md`'s *Before the first upload*: the upload key and its off-machine backup, the Play entry for
`com.maksforge.takeyourmeds`, the Play secrets, and a green internal upload of phase 0's build.
`RELEASE_PLEASE_TOKEN` and the repository ruleset are already in place (2026-11-11).

## Not in this plan

- **A drug database, interaction checks, dosing advice or any reading of the Amount** (ADR-0011).
- **Sharing with a caregiver, accounts, sync or a web view.** Each needs a server or `INTERNET`
  (ADR-0008).
- **Health Connect or any other export of Doses to another app.** It would make the Doses leave the
  app's own storage, which is the one promise the listing makes.
- **Streaks, scores or adherence percentages.** "Missed" is a state, not a grade (`CONTEXT.md`).
