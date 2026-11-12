# A Dose is derived from its Schedule, and only the user's answer and a pending Snooze are stored

## Context

A Schedule produces Doses forever. Storing each Dose ahead of time means a table to fill, extend and
rewrite whenever a Schedule changes, and a Reminder for a row that no longer matches the Schedule.
Storing nothing means "did I take it this morning?" has no answer. ADR-0003 already says the OS
schedule is only a cache of a derivation.

## Decision

**Due and Missed are computed; Taken and Skipped are stored.** A Dose is identified by its Medicine
and its scheduled date-time, a local date-time: a Time is wall-clock in the phone's current zone,
so a 9:00 Medicine is due at 9:00 wherever the phone is. The Doses of a day are derived from the
Schedules in force that day. A `DoseRecord` row is written only when the user answers: it holds the
Medicine's id, the scheduled date-time, the answer, the time of the answer, and a copy of the
Medicine's name and Amount as they were.

- **Changing a Schedule changes the future only.** Past Doses with a record keep it; past Doses
  without one were Missed under the Schedule then in force, so each Schedule keeps a valid-from date
  and an edit starts a new one instead of rewriting the old.
- **Deleting a Medicine keeps only its answered Doses.** Its Schedules go with it, so its Missed
  Doses leave History. Its `DoseRecord`s stay: their Medicine id does not cascade, which is why the
  name and Amount are copied into them.
- **A pending Snooze is stored**, because its time cannot be derived: a `Snooze` row holds the
  Medicine id, the scheduled date-time and the snoozed-until time. The derivation reads it, so a
  re-plan (another answer, a reboot, an update) keeps the snoozed Reminder. An answer deletes it, a
  second Snooze replaces it, and deleting the Medicine deletes it.
- **A late answer is allowed**: marking yesterday's Missed Dose as Taken writes a record with the real
  answer time. History shows both times.

## Alternatives

**Materialise Doses a week ahead.** Easy queries, and every Schedule edit becomes a reconciliation of
rows that might already have answers, the same cancel-and-re-enqueue problem ADR-0003 avoided.

**Store only the Schedule and a "last taken" timestamp.** Enough for a reminder app, not for one whose
point is answering "did I take it?" about any day.

## Consequences

- Today and History are queries plus a derivation, not a table read. A Dose's state is a pure function
  of Schedules, `DoseRecord`s, a day and the clock, and its Reminder time also reads the `Snooze`
  rows. That function is the safety value this app has: **the phase that introduces it introduces its
  test** (`docs/PLAN.md`, rule 3: *A phase proves itself on the phone,
  not in a test*), including a Time near midnight, a daylight-saving change, a time-zone change and a
  Schedule edited mid-day.
- Schedule edits never touch `DoseRecord`, so the first migration (phase 3) is additive.
