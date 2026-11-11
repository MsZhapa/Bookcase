# V2 migration and private Google access

## Current verified state — November 11, 2026

The owner-approved October 26 inventory replaces the unreconciled staging data:
257 listed entries become 262 physical books after expanding grouped volumes
and excluding painting supplies. V2 now has 260 works, 262 editions, 262 copies,
one wishlist entry, one lending history record and eight position counters.
Cube counts are 30, 32, 30, 45, 33, 31, 35 and 26, with continuous source-order
positions. The historical 507 figure below counts stored records, not ownership.

The replacement retained 137 copy IDs, added 125 missing books and removed 370
excluded/duplicate copies after explicit approval. Personal data on the two
excluded records was disclosed and retained in private backups. Wishlist and
lending history were preserved; the original default database is unchanged.
Exact edition details and ISBNs were not guessed for new inventory entries.

The debug build installed on OnePlus. Real Google sign-in succeeded for the
configured owner, verified 262 copies, and activated V2. The server acknowledged
activation on November 11, 2026. Force-stop/relaunch retained the session;
eight cube counts and Cube 1 source order were verified on the phone. The local
Cube 5 “Travel” label remained intact. No Google account was added to the emulator.

The corrected-inventory emulator test passed: full restore/fingerprint, all
262 hydrated copies and positions, grouped volumes, legitimate duplicate titles,
separate editions, search, wishlist/history and repeat-import idempotence. Its
first attempt timed out connecting to the local test database; rerunning through
adb reverse passed. The app and instrumentation builds passed.

The library remains owner-only. Supporting other users with separate private
libraries is future work; no access rules were broadened for this test.

## Historical staging evidence (before inventory reconciliation)


The following section records the initial November 10–11 staging checks.
OnePlus sign-in and activation were subsequently completed as described above.

## Result

The existing separate Standard Firestore database `books` in europe-north2 now
contains 170 works, 171 editions, 507 normalized bookCopies, one wishlist entry,
one lending history record and one shelf-position counter: 851 library records.
Verified original and normalized backups are also preserved outside Git under
`~/Library/Application Support/Bookcase/Backups`, with owner-only file permissions.

The original `(default)` database in eur3 was only read. All original library
fields matched the verified backup after migration. No original records were
deleted, merged in place, moved, or overwritten, and its rules were unchanged.

The separate database also has administrator-owned `privateConfig/access` and
`libraryStatus/current` documents for access control and migration readiness.
These are infrastructure, excluded from portable library backups.

## Migration behavior

- Stable work IDs use normalized title and author, stripping only explicit
  trailing numbered edition suffixes. Missing title/author stays copy-specific.
- Equivalent ISBN-10/13 formats share an edition only when edition metadata
  agrees. Conflicting ISBN metadata, invalid ISBNs, orphan catalogue records and
  occupied known shelf positions block migration for review.
- Unknown ISBNs do not establish edition identity. Identical edition metadata
  can share an edition; different metadata remains separate.
- Every physical-copy document keeps its source document ID, cube number,
  known position, notes, loan state/history links and page/percentage progress.
- 504 copies have no recorded position and 502 have no validated ISBN. Missing
  information was not invented. Stored copy counts are not a physical inventory
  reconciliation; actual shelf placement still needs checking.
- Each converted copy contains an immutable `migrationArchive` string with
  explicitly typed portable JSON for its exact source record, including original
  edition/work records for normalized sources. Timestamp precision and unknown
  original fields are preserved. Bounded string encoding avoids Firestore rule
  expression limits that affected nested archive validation.
- Work identity is shared by migration and future additions/edits. Adding a
  different edition reuses the work; changing an edition's author links it to
  the appropriate work without changing other editions' metadata.
- The administrative staging script checks the original against the saved
  backup, rejects unrelated/conflicting target records, and uses create-only
  writes with preconditions. It verifies every target record and rereads the
  source. Repeat staging reported zero new records and an unchanged source.

## Private access and activation

Google sign-in is enabled in the existing Firebase project. The debug signing
certificate is registered and the Android configuration now contains the OAuth
client IDs. Sign-in uses Credential Manager's explicit Google button flow with
a fresh nonce, then exchanges the Google ID token with Firebase Authentication.
The chosen account's email is stored only in server-owned access configuration,
not hard-coded in app source or the rules.

The V2 rules require a verified Google identity matching that configured email.
Signed-out users, other accounts, unverified identities and password-provider
tokens cannot read or write the private library. Clients cannot change the owner
configuration or forge a verified migration-readiness receipt. Record validators
apply to creates and updates; audit archives cannot be changed on updates.

All screens route through BookcaseDatabase and close private screens when the
allowed session ends. Startup is account-gated; it does not expose the original
library as a fallback. V2 activation remains disabled until real sign-in and
server access/readiness verification succeed. The account screen then enables
Use private V2 library. Before the first switch, both source and target are
rechecked against the reviewed snapshot fingerprints. Intervening edits block
activation until migration is refreshed. The owner acknowledges first cutover
once using a server timestamp; verification fingerprints stay immutable. Local activation binds the device to the verified UID.
The private Firestore instance disables on-disk persistence to avoid leaving
another account's library in a persistent cache after sign-out.

Readiness is administrator-issued, so another device can join the existing V2
library after the owner has edited it; devices do not repeatedly compare the
now-active V2 library to an older source snapshot. Before that first activation,
the fresh fingerprint checks protect edits made while waiting for OnePlus testing. The original and V2 copies
are not synchronized. Upgrade before editing on another device. The original
database retains its existing access policy; its unauthenticated read endpoint
still returned 200 in the control check. Restricting/retiring that old archive is
a separate reviewed step after OnePlus cutover, not an automatic data deletion.

## Validation

- Debug app and Android test APK built successfully. All 93 JVM tests passed.
- 13 Android 16 emulator regression tests passed: six backup/restore tests and
  seven migration tests. These use named demo Firebase apps and a local emulator,
  never production source/target instances. Large migration compared all 509
  hydrated test copies, their ordering, personal state and original source data.
- A final targeted emulator test passed for notes, page progress, moves,
  additional copies, shared-work reuse across editions and author/edition edits.
- 55 private-rule checks passed on an isolated local project, including all 851
  real-data-shaped migrated records restored under the owner rules, access denial
  for other identities, schema/type/size validation, immutable archives and
  timestamps, protected readiness configuration and monotonic counters.
- Full restore uses single-record transactions to fit archive validation and
  rule access budgets. It is resumable rather than globally atomic; interrupted
  restores retain completed additions, skip matches and never overwrite conflicts.
- Rules compiled successfully in Firebase's deployment dry run and were deployed
  only to database books. The deployed V2 signed-out read endpoint returned 403.
- Signed-out emulator startup and Back/relaunch show the account screen; V2
  activation is disabled. No real Google account/device sign-in was attempted.
- Actual Google account selection, Firebase token exchange, production owner
  access, device activation and sign-out/re-entry remain pending OnePlus testing.
- git diff --check passed. Lint still has the pre-existing Glide
  NotificationPermission error. No new lint errors were found.

## Configuration and reproducibility

- firestore.v2.rules and firebase.v2.json target only the separate books database.
- An administrator provisions privateConfig/access.ownerEmail and the readiness
  receipt after record-by-record migration verification; clients cannot bootstrap
  themselves as owner or declare their own migration complete.
- tools/stage-v2.cjs defaults to dry-run. --apply creates missing records only.
  It requires app-generated source/converted backups and the logged-in Firebase
  CLI runtime via BOOKCASE_FIREBASE_TOOLS. It never changes rules or app routing.
- tools/test-private-rules.cjs uses Node built-ins and unsigned mock identities
  only against 127.0.0.1:8080 and project demo-bookcase-private. It never sends
  mock tokens to production. An optional converted-backup file exercises all
  migrated archive records; no real backup data is committed in this repository.
- Credential Manager 1.6.0 and Google ID 1.1.1 are pinned. The latter supports the
  current Kotlin compiler; Google ID 1.2.1 required incompatible Kotlin metadata.
  Existing Firebase Auth resolves to 23.2.0 through the app's BoM.
- Release signing certificate registration and signed-build validation remain
  part of the original release work.

Implementation references: [Firebase Android Google authentication](https://firebase.google.com/docs/auth/android/google-signin)
and [Android Google button flow](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation).

These are tested prototype Security Rules for the configured single-account
library. Review and verify them before broadly sharing the app.

## Changed files

- `MIGRATION_V2.md`
- `ROADMAP.md`
- `app/build.gradle`
- `app/google-services.json`
- `app/src/androidTest/java/com/example/bookcase/BackupEmulatorTest.java`
- `app/src/androidTest/java/com/example/bookcase/MigrationEmulatorTest.java`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/bookcase/AccountActivity.java`
- `app/src/main/java/com/example/bookcase/AdminActivity.java`
- `app/src/main/java/com/example/bookcase/BackupJson.java`
- `app/src/main/java/com/example/bookcase/BackupRepository.java`
- `app/src/main/java/com/example/bookcase/BookCopy.java`
- `app/src/main/java/com/example/bookcase/BookDetailActivity.java`
- `app/src/main/java/com/example/bookcase/BookRepository.java`
- `app/src/main/java/com/example/bookcase/BookcaseActivity.java`
- `app/src/main/java/com/example/bookcase/BookcaseDatabase.java`
- `app/src/main/java/com/example/bookcase/CatalogueIdentity.java`
- `app/src/main/java/com/example/bookcase/CatalogueToolActivity.java`
- `app/src/main/java/com/example/bookcase/CopyNotesActivity.java`
- `app/src/main/java/com/example/bookcase/CubeSettingsActivity.java`
- `app/src/main/java/com/example/bookcase/CubesActivity.java`
- `app/src/main/java/com/example/bookcase/EditBookActivity.java`
- `app/src/main/java/com/example/bookcase/FirestoreHelper.java`
- `app/src/main/java/com/example/bookcase/LibraryBackup.java`
- `app/src/main/java/com/example/bookcase/LibraryExportActivity.java`
- `app/src/main/java/com/example/bookcase/LibraryMenu.java`
- `app/src/main/java/com/example/bookcase/LibraryRestoreActivity.java`
- `app/src/main/java/com/example/bookcase/LoansActivity.java`
- `app/src/main/java/com/example/bookcase/MainActivity.java`
- `app/src/main/java/com/example/bookcase/MigrationPlan.java`
- `app/src/main/java/com/example/bookcase/MigrationRepository.java`
- `app/src/main/java/com/example/bookcase/ReadingOverviewActivity.java`
- `app/src/main/java/com/example/bookcase/ReadingProgressActivity.java`
- `app/src/main/java/com/example/bookcase/WelcomeActivity.java`
- `app/src/main/java/com/example/bookcase/WishAddActivity.java`
- `app/src/main/java/com/example/bookcase/WishlistRepository.java`
- `app/src/main/res/layout/activity_account.xml`
- `app/src/main/res/menu/library_actions.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/example/bookcase/CatalogueIdentityTest.java`
- `app/src/test/java/com/example/bookcase/MigrationPlanTest.java`
- `firebase.v2.json`
- `firestore.v2.rules`
- `tools/stage-v2.cjs`
- `tools/test-private-rules.cjs`

- `BACKUP_RESTORE.md`
- `app/src/main/java/com/example/bookcase/MigrationReadiness.java`
- `app/src/test/java/com/example/bookcase/MigrationReadinessTest.java`

One-time migration/staging remains an administrative tool. The normal app menu
adds Account and private library rather than exposing database migration controls.
