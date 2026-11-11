# The app records what the user tells it and never advises

## Context

Anything that reads like medical guidance (a suggested dose, a warning about two medicines, "you can
take another") makes the app a medical device in the regulatory sense and a liability in the
ordinary one. Play's Health apps policy also asks every health app to say whether it is one.

## Decision

**The app stores the user's words and the clock, and makes no claim about medicine.** The Amount and
Note are free text, never parsed, validated or compared. No list of medicine names ships or is
fetched. No copy uses advisory words (`CONTEXT.md`, *Words we deliberately avoid*). First run and the
listing say once, plainly: this is a reminder, not medical advice and not a medical device; ask a
doctor or pharmacist about your medicines.

## Alternatives

**Interaction warnings from a bundled dataset.** Useful, and wrong the first time the dataset is
stale or the user's spelling differs, with the app's name on the advice.

**Structured Amounts ("2 × 500 mg") to drive Supply.** Supply (phase 4) instead counts *units* the
user declares per Medicine, so the Amount stays text.

## Consequences

- Autocomplete only ever offers names the user has already typed.
- Supply counts down by a number the user sets per Medicine ("each Dose uses 1"), not one read from
  the Amount.
