# A Dose is derived from its Schedule, and only the user's answer is stored

## Context

A Schedule produces Doses forever. Storing each Dose ahead of time means a table to fill, extend and
rewrite whenever a Schedule changes, and a Reminder for a row that no longer matches the Schedule.
Storing nothing means "did I take it this morning?" has no answer. ADR-0003 already says the OS
schedule is only a cache of a derivation.

## Decision

**Due and Missed are computed; Taken and Skipped are stored.** A Dose is identified by its Medicine
and its scheduled date-time. The Doses of a day are derived from the Schedules in force that day. A
`DoseRecord` row is written only when the user answers: it holds the Medicine's id, the scheduled
date-time, the answer, the time of the answer, and a copy of the Medicine's name and Amount as they
were.

- **Changing a Schedule changes the future only.** Past Doses with a record keep it; past Doses
  without one were Missed under the Schedule then in force, so each Schedule keeps a valid-from date
  and an edit starts a new one instead of rewriting the old.
- **Deleting a Medicine keeps its records**, which is why the name and Amount are copied into them.
- **A late answer is allowed**: marking yesterday's Missed Dose as Taken writes a record with the real
  answer time. History shows both times.

## Alternatives

**Materialise Doses a week ahead.** Easy queries, and every Schedule edit becomes a reconciliation of
rows that might already have answers, the same cancel-and-re-enqueue problem ADR-0003 avoided.

**Store only the Schedule and a "last taken" timestamp.** Enough for a reminder app, not for one whose
point is answering "did I take it?" about any day.

## Consequences

- Today and History are queries plus a derivation, not a table read. The derivation is a pure function
  of Schedules, a day and the clock, and it is the safety value this app has: **the phase that
  introduces it introduces its test** (`CONTEXT.md`'s naming rule), including a Time near midnight,
  a daylight-saving change and a Schedule edited mid-day.
- Schedule edits never touch `DoseRecord`, so the first migration (phase 3) is additive.
