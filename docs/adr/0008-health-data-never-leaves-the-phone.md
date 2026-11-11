# Health data never leaves the phone

## Context

A list of someone's medicines and the times they take them is health data. Play treats it as a
sensitive category, a listing that says "no ads, no tracking" is only believable if the manifest
backs it, and the template already removes the `INTERNET` permission that the billing library merges
(ADR-0007). A medication app has three standing temptations to add a network: a drug database to
autocomplete names, sharing with a caregiver, and cloud sync.

## Decision

**The app never opens a network connection.** `INTERNET` stays removed in the manifest, and
`scripts/aab-permissions.py` keeps it in FORBIDDEN, so a dependency that merges it fails the build.
Medicines and Doses live in the app's Room database and leave it only through Android's own backup
and device-to-device transfer, which the user controls in system settings.

Play's Data safety answer is therefore **no data collected, no data shared**.

## Alternatives

**A drug-name database fetched on demand.** Nicer typing, and the first feature that would send what
the user is taking to a server. A bundled list was also weighed and lost on ADR-0011: it implies the
app knows what a medicine is.

**Caregiver sharing.** The most-requested feature in this category, and impossible without a server
or a peer-to-peer channel, either of which turns the listing's promise into a policy.

**Optional cloud sync behind a switch.** An off-by-default switch still means the permission is
declared and the Data safety answer changes for everybody.

## Consequences

- A lost phone without Android backup loses the history. Phase 0 drops the manual export and keeps
  `AppBackupAgent` for the database (checkpointed, ADR-0005), so Auto Backup and transfer are the
  only off-phone copies; the app says so once, in Settings.
- Support cannot see anything. A problem report is whatever the user chooses to write in the mail
  hand-off.
- Any later feature that needs the network needs a new ADR that supersedes this one, not an exception
  in a phase file.
