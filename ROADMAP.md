# Bookcase original feature checklist

Reviewed 10–11 November 2026 against the repository and device checks.
Source: the user-pasted **Features list** in the ChatGPT conversation
**learning plan**. This preserves the original scope rather than substituting
new feature recommendations. Initial time estimates are historical planning
estimates, not new completion promises.

| Original item | Status | Evidence / remaining work |
|---|---|---|
| Create BookWork.java (1–2 h) | Delivered | Model exists with ID, title, author and description. |
| Work → Edition → Copy relationships (4–6 h) | Delivered and activated | The owner-approved October 26 inventory has 260 works, 262 editions and 262 linked copies. Real Google sign-in and V2 activation passed on OnePlus. |
| Separate V2 Firebase database/import (2–4 h) | Delivered and activated | Standard database books contains the reconciled 262-book inventory; real owner Google sign-in and activation passed. Original default database remains unchanged. |
| Migrate existing CRUD screens (4–6 h) | Implemented; private browsing verified on phone | Screens use one database router and require the allowed Google session for V2. Emulator checks cover hydrated copies, notes, page progress, moves, additional copies, edition edits and preserved lending data. |
| Duplicate ISBN detection (2–3 h) | Delivered | ISBN-10/13 equivalence, checksum validation and duplicate-copy warning are implemented. |
| Add multiple physical copies (2–4 h) | Delivered | Copies keep separate IDs, positions, notes, lending and progress. |
| Group different editions by title (2–3 h) | Delivered browse view | Browse editions groups title/author variants with explicit edition suffixes removed. Each copy remains individually accessible; data is not merged. Unknown ISBNs are labelled rather than counted as invented editions. |
| Lending system with Lent out tag (3–5 h) | Delivered | Lend/return, borrower, dates/history, row tags and loan overview exist. |
| Page-based progress and percentage (1–2 h) | Delivered | Page input calculates percentage and preserves older percentage-only records. |
| Preserve eight cubes and allow customization (3–5 h) | Delivered labels/layout | Eight numeric cube IDs remain fixed; names and 1–4 display columns can be customized on this phone. Adding/deleting cubes is outside this implementation. |
| Update cube placement and copy counts (2–3 h) | Delivered against master inventory | All 262 books match the selected October 26 list with sequential positions and verified counts in eight cubes. Local cube labels remain intact. |
| Separate wishlist (3–5 h) | Delivered | Separate wishlist collection, add/search/remove confirmation, duplicate prevention, owned-book warning and existing add-book prefill. Wishlist entries do not affect owned counts. |
| Search/filter improvements (3–5 h) | Delivered | Title, author, formatted/equivalent ISBN, cube scope and reading/lending filters. |
| Library statistics and summaries (2–4 h) | Delivered | Reading counts and cube copy counts with drill-down and error states. |
| Export and import library data (3–5 h) | Delivered for full backups | Version 2 backs up works, editions, legacy books, physical copies, lending history, wishlist and local cube settings. Previewed restore adds missing records, skips matching records and blocks conflicts. Image files and older catalogue-only imports are excluded. |
| UI polish and empty/error states (3–5 h) | Partial | New tools have readable inputs, loading/retry/empty feedback. A complete app-wide review remains. |
| Testing, bug fixes and release build (5–8 h) | Partial | 93 local tests, 13 isolated backup/migration emulator tests, a final targeted editing test, and 55 private-rule checks pass for delivered work. Signed release validation and the existing Glide NotificationPermission lint failure remain. |

## Latest account batch

- New welcome/login/register UI; direct transition to cubes and a Sign out menu item.
- Every Google or email/password account uses its own UID-scoped bookcase.
- Existing 262 books moved to their account; recovery copies are closed to clients.
- 90 JVM tests, 14 emulator tests and 54 auth/rules checks pass; OnePlus Google login and sign-out passed.
- See ACCOUNTS.md for files, behavior and verification.

## Inventory batch (before account expansion)

- Reconciled the approved October 26 source into 262 physical books, 260 works and 262 editions; removed reviewed extras from private V2.
- Preserved source records as immutable portable JSON archives on converted copies.
- Private Google sign-in, owner-only rules, signed-out screen protection and verified V2 activation.
- Source database retained, with no book deletions or in-place migration.

Real Google account sign-in, V2 activation, restart/session retention and cube
counts passed on OnePlus. The actual 262-copy replacement passed an isolated
emulator restore, hydration, search, position and repeat-import test. See MIGRATION_V2.md.

## Next original-list work

1. Finish the app-wide UI/error-state review and signed release checks, including
   resolving the existing Glide lint error and registering the release certificate
   for Google sign-in.
2. Original archives are retained but now deny application access. Plan archive
   retention separately; active account libraries and archives are not synchronized.
3. Account expansion is delivered: email/password registration/login/reset and
   Google login, with each account owning its UID-scoped bookcase. Review the
   prototype rules before broad distribution. See ACCOUNTS.md.

Full backups include works, editions, physical copies, legacy records if present,
wishlist, lending history and local cube settings. Original-record archives are
included. Cover image files and server-owned access/readiness configuration are
excluded. Restore adds missing records, skips matches and blocks conflicts.
Position counters only advance. Cube names/layout remain local device preferences
and restore separately. Earlier catalogue-only exports cannot be restored by the
full-backup importer. Private library access now requires Google sign-in and an
administrator-issued migration-readiness receipt.
