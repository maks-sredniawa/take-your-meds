# A Reminder is an exact alarm, with a fallback the user can see

## Context

ADR-0003 reserves exact alarms for the things whose lateness has consequences. A medicine taken at
the wrong time is that case, and the whole app. Since Android 14 `SCHEDULE_EXACT_ALARM` is refused by
default for new installs, and `USE_EXACT_ALARM` is permitted by Play only for alarm clocks and
calendars.

## Decision

**One exact alarm** (`setExactAndAllowWhileIdle`), scheduled from ADR-0009's derivation, is armed
for the earliest upcoming Reminder time across all Medicines: a Dose's Time, a stored Snooze's end
or a Follow-up. The alarm carries the time it was armed for, and each firing posts a Reminder for
every unanswered Dose whose Reminder time falls between that time and now, so two Medicines at 9:00,
or a fallback alarm landing after a later Dose, post every Dose they cover.

A re-plan after a boot, an update or a clock change has no armed time to start from, because the
alarm that would have covered the gap is gone. It first posts a Reminder for every Dose still Due
(its Reminder time passed, unanswered, not yet Missed), so a phone switched off over 9:00 still
reminds at power-on. A Reminder's notification id is derived from its Dose, so posting it again
replaces it rather than stacking a second one.

Each firing, answer, Snooze, edit, boot, update, clock or time-zone change (`TIME_SET`,
`TIMEZONE_CHANGED`) and change of the exact-alarm grant
(`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`) re-plans it. Revoking the grant cancels the
alarm and stops the app with no broadcast, so ADR-0003's daily sweep re-arms it as the backstop.
With the grant refused it falls back to `setAndAllowWhileIdle`, and Today shows a banner saying
Reminders may be late, with the hand-off to grant it.

**`SCHEDULE_EXACT_ALARM` only**, asked when `docs/PLAN.md`'s asks table (rule 4) says.
`USE_EXACT_ALARM` is never declared.

**The `reminders` channel is raised to high importance in phase 0**, before Phase P's first upload,
because importance is fixed at creation and can only be lowered afterwards (ADR-0003). Raising it
under the same id is simpler than a new id: nothing has been uploaded yet, and a phone holding an
earlier build is reinstalled.

Snooze and the Follow-up are Reminder times on the same alarm, not separate jobs. A Snooze's end is
read from its stored row (ADR-0009), so a re-plan keeps it.

## Alternatives

**`setAlarmClock()`.** The most reliable delivery in Doze, but it puts an alarm icon in the status bar
and a "next alarm" into the clock for every Dose, which reads as the app pretending to be an alarm
clock.

**A WorkManager job per Dose.** Batched and deferred by design; a 9:00 Dose at 9:20 is the failure the
app exists to prevent.

**A foreground service waiting for the next Dose.** Reliable and expensive: a permanent notification
and a foreground-service declaration for a few events a day.

## Consequences

- Vendor ROMs can still kill delivery without autostart. Phase 2's setup screen owns the hand-off and
  says what a refusal costs; only `scripts/device-gate.py` can read it.
- Exactness is proven by readings, not tests: phase 1 reads a Reminder in deep Doze, after reboot,
  after an update, and with the grant refused.

## Amendment, 2026-11-13

- **The gate's verdict** (phase 1, §1): one exact alarm holds on API 36 in deep Doze, including the
  second of two five minutes apart. API 26–30 is unread on the emulators and owed before phase 2.
- **The re-post bound.** A re-plan re-posts a Due Dose only within `DUE_REPOST_LIMIT` (2 hours) of
  its Reminder time, and never once a later Dose of the same Medicine has reached its Time. The same
  bound limits what a late firing posts, so a fallback alarm landing very late posts no stale
  Reminder. Today is not bound by it.
- **A Reminder is keyed by a tag**, `dose:<medicineId>:<scheduledAt>`, under one fixed id, and its
  Taken action carries the same key as its `Intent` data, so two Doses never share a notification or
  an action.
- **The lock screen** shows a public version that names no Medicine, deferring to the user's own
  setting for sensitive content.
- **An alarm replaced before it lands still posts.** Arming replaces the pending alarm, so the planner
  stores the instant it armed for, and any re-plan that finds it passed posts what that alarm would
  have, under the same bound, before arming the next.
- **One Reminder per Medicine in the tray** (owner, 2026-11-13). Posting a Dose's Reminder removes the
  same Medicine's earlier Reminders; those Doses stay Due on Today.
