# Account sign-in and per-user bookcases

Implemented November 11, 2026.

Signed-out readers see a cream-and-green welcome screen with email/password login,
account creation, password confirmation, password reset, and Continue with Google.
Successful login opens the cube screen directly; returning signed-in readers go
straight to their books. The library menu has Add book and Sign out. There is no
account/migration page, activation button, or private-library explanation in the UI.

## Storage and access

Every account uses the same `books` database layout:
`users/{Firebase Auth UID}/{collection}/{document ID}`. The seven library
collections keep the existing works, editions, copies, loans, wishlist and shelf
counter schemas. New users start empty; eight cube views do not create fake books.
Authorization uses the authenticated UID, not an email allowlist or an owner role.
Google and email/password sessions use the same paths and rules.

Repositories capture their account path once. Sign-out/account changes clear the
screen stack, and pending operations cannot redirect their writes into the next
account. The add-book form retains its account across delayed cover uploads;
edit and restore operations also retain their original scope. Local cube names
and columns are keyed by account. Existing device settings copy only to the UID
already recorded for that installation, without an email-specific exception.

The existing catalogue was copied and field-verified under its signed-in account:
262 books, 260 works, 262 editions, one wishlist entry, one returned loan and eight
counters. Books, cube positions and metadata were not reset or guessed. Before
deploying, a repeat comparison found no records to create and no differences.
Private before/after backups are saved outside Git in Bookcase's Application
Support backup folder.

Previous root records and configuration remain recovery data. Client access to
the old root catalogue in `books` and all records in `(default)` is denied.
The app no longer reads readiness receipts, owner configuration, or the original
database. No recovery records were deleted.

Firebase email/password authentication was enabled with a targeted configuration
update that preserved Google sign-in. Rules validate schemas and relationships
inside each UID subtree; other users, unauthenticated requests, anonymous-provider
tokens, role assignment and undefined paths are denied.

## Verification

- 90 JVM tests passed. Three obsolete single-owner session tests were removed
  with the unused policy; stable catalogue identity tests remain.
- 14 Android emulator tests passed together: the real registration/login/sign-out
  UI, invalid email/password confirmation, new empty libraries, separate account
  books with the same copy ID, rejection of an old repository after switching,
  account-specific cube settings, and existing backup/migration regression tests.
- 54 localhost checks passed for rule compilation, cross-account reads/writes,
  schema constraints, references, archives, counters, loans, wishlist,
  registration, login, wrong passwords, and reset requests. Test users and reset
  emails exist only in emulators, not production.
- Debug app and instrumentation builds passed. A debug-only network configuration
  permits HTTP solely to localhost/emulator hosts; release traffic policy is unchanged.
- Production rules compiled in dry runs and deployed to the explicit Bookcase
  project. Signed-out requests to old catalogue paths returned 403 in both databases.
- OnePlus: updated app installed; existing account loaded the eight expected
  counts (30, 32, 30, 45, 33, 31, 35, 26), including its Travel label. Sign out
  returned to the new welcome screen. Continue with Google opened the account's
  books directly without an activation step. Email registration/reset were tested
  in emulators; production provider configuration was verified.
- Welcome layout was visually reviewed on the emulator; the draft plus icon was
  replaced with an open-book mark. Existing toolbar colors match the new palette.

Signed release checks and registering the release signing certificate remain
release work. The rules are a tested prototype for review before broad distribution.

## Main files

- `AccountActivity`, launcher alias `WelcomeActivity`, welcome layout/resources:
  login, registration, reset and new visual design.
- `LibraryStore`, `BookcaseDatabase`, `BookcaseActivity`: account routing and guards.
- Book/backup/wishlist repositories, add/edit/detail/restore screens: scoped reads,
  writes, references and delayed operations.
- `CubeSettings`, `LibraryMenu`, menu resources: account settings, Add book, Sign out.
- `firestore.v2.rules`, `firestore.archive.rules`, deployment configurations:
  per-user authorization and closed recovery data.
- `AccountsEmulatorTest`, `tools/test-private-rules.cjs`: account-flow and isolation tests.

Implementation references: [Firebase Android password authentication](https://firebase.google.com/docs/auth/android/password-auth)
and [Identity Platform configuration updates](https://docs.cloud.google.com/identity-platform/docs/reference/rest/v2/projects/updateConfig).
