# A Reminder is an exact alarm, with a fallback the user can see

## Context

ADR-0003 reserves exact alarms for the things whose lateness has consequences. A medicine taken at
the wrong time is that case, and the whole app. Since Android 14 `SCHEDULE_EXACT_ALARM` is refused by
default for new installs, and `USE_EXACT_ALARM` is permitted by Play only for alarm clocks and
calendars.

## Decision

**Each next Dose is one exact alarm** (`setExactAndAllowWhileIdle`), scheduled from ADR-0009's
derivation: only the earliest upcoming Dose across all Medicines has an alarm, and each firing, answer,
Snooze, edit, boot and update recomputes it. With the grant refused it falls back to
`setAndAllowWhileIdle`, and Today shows a banner saying Reminders may be late, with the hand-off to
grant it.

**`SCHEDULE_EXACT_ALARM` only**, asked as phase 1's table says. `USE_EXACT_ALARM` is never declared.

**The Reminder channel is created at high importance** and once, because importance can only be
lowered afterwards (ADR-0003).

Snooze and the Follow-up are further exact alarms on the same recomputation, not separate jobs.

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
