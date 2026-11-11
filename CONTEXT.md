# Vocabulary

**The rule this file exists to enforce: one concept, one word, everywhere.** The entity, the DAO, the
route key, the string resource and the sentence in the UI all use it. When the domain word and the
user-facing word genuinely differ, both are written here with the difference stated.

## The domain

| Term | Means | Not |
| --- | --- | --- |
| **Medicine** | one thing the user takes, as they name it: "Metformin", "Vitamin D". Holds its Amount, an optional Note and its Schedules: at most one in force at a time, earlier ones kept from their valid-from date (ADR-0009) | drug, medication, pill, prescription |
| **Amount** | what is taken each time, as free text the user writes: "1 tablet", "5 ml", "2 puffs". The app never parses or checks it (ADR-0011) | dose, dosage, quantity |
| **Note** | the user's own reminder text for a Medicine: "with food" | instructions, directions |
| **Schedule** | when a Medicine is due: one or more Times of day, the Days it applies on (every day, chosen weekdays, every N days) and an optional End date | plan, regimen, routine |
| **As needed** | a Medicine with no Schedule: logged when taken, never reminded | PRN (fine in code comments, never in UI) |
| **Dose** | one occasion a Medicine is due: a Medicine at a date and time. **Derived** from the Schedule, never stored as a plan (ADR-0009) | intake, event, occurrence |
| **Due** / **Taken** / **Skipped** / **Missed** | a Dose's state. *Taken* and *Skipped* are the user's answers and are stored; *Due* and *Missed* are computed from the clock. *Skipped* is a choice; *Missed* is no answer at all | done, ignored, late |
| **Reminder** | the notification for one Dose, at its time, carrying Taken, Snooze and (later) Skip | alert, alarm (except where the Android API is meant) |
| **Snooze** | push the Reminder back by a fixed time, without answering the Dose. Stored until the Dose is answered (ADR-0009) | postpone, delay |
| **Follow-up** | the one extra Reminder for a Dose still unanswered after a set time; after it the Dose becomes Missed | repeat, nag, escalation |
| **Today** | the home screen: every Dose of the current day with its state | dashboard, overview |
| **Supply** | how many units of a Medicine are left, counted down by each Taken Dose; with a *running-low* threshold. Optional per Medicine (phase 4) | stock, inventory, refill |
| **Tip** | a one-off payment to the developer through Google Play that **unlocks nothing** (ADR-0007). The home screen mentions it once per install, after 30 days and ten Taken Doses; Help and feedback offers it for good | donation, support the project, premium, supporter |

## Words we deliberately avoid

- **"Dose" is the occasion, never the amount.** "Take your 9:00 dose" is right; "dose: 2 tablets" is
  the Amount. The two collide in everyday speech and in Polish (*dawka*), so the UI uses *Amount* /
  *ilość* for the second.
- **Nothing in the UI sounds like advice.** No "should", "recommended", "safe", "overdose", "interaction"
  (ADR-0011). The app says what the user told it and when.
- **"Missed" never blames.** It is a state, not a judgement: no streaks, scores or red failure copy.
- **"Delete" a Medicine is irreversible and says so: its Schedules go, so History keeps only its
  answered Doses, Taken and Skipped, and not its Missed ones** (ADR-0009). To stop reminders without
  losing the Medicine, the user ends its Schedule.

## Naming conventions in code

Some names in this codebase are deliberately generic — `AppDatabase`, `AppTheme`, `AppPreferences`,
`AppContainer`, `MainApplication`, `MainActivity`. They stay correct whatever the app is called, so
renaming the app is a package move rather than a repo-wide find-and-replace. Don't rename them to
match the product; name the *domain* types after the domain instead.
