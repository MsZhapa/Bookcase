# Bookcase original feature checklist

Reviewed 10–11 November 2026 against the repository and device checks.
Source: the user-pasted **Features list** in the ChatGPT conversation
**learning plan**. This preserves the original scope rather than substituting
new feature recommendations. Initial time estimates are historical planning
estimates, not new completion promises.

| Original item | Status | Evidence / remaining work |
|---|---|---|
| Create BookWork.java (1–2 h) | Delivered | Model exists with ID, title, author and description. |
| Work → Edition → Copy relationships (4–6 h) | Implemented and staged | The separate V2 dataset has 170 works, 171 editions and 507 linked copies. Add/edit operations reuse stable work identities. Real Google sign-in and device activation await OnePlus testing. |
| Separate V2 Firebase database/import (2–4 h) | Copy verified; activation pending | Existing Standard database books contains the verified normalized copy. Original default database remains unchanged. Google access is configured; account sign-in and activation are deferred to OnePlus testing at the user’s request. |
| Migrate existing CRUD screens (4–6 h) | Implemented; phone verification pending | Screens use one database router and require the allowed Google session for V2. Emulator checks cover hydrated copies, notes, page progress, moves, additional copies, edition edits and preserved lending data. |
| Duplicate ISBN detection (2–3 h) | Delivered | ISBN-10/13 equivalence, checksum validation and duplicate-copy warning are implemented. |
| Add multiple physical copies (2–4 h) | Delivered | Copies keep separate IDs, positions, notes, lending and progress. |
| Group different editions by title (2–3 h) | Delivered browse view | Browse editions groups title/author variants with explicit edition suffixes removed. Each copy remains individually accessible; data is not merged. Unknown ISBNs are labelled rather than counted as invented editions. |
| Lending system with Lent out tag (3–5 h) | Delivered | Lend/return, borrower, dates/history, row tags and loan overview exist. |
| Page-based progress and percentage (1–2 h) | Delivered | Page input calculates percentage and preserves older percentage-only records. |
| Preserve eight cubes and allow customization (3–5 h) | Delivered labels/layout | Eight numeric cube IDs remain fixed; names and 1–4 display columns can be customized on this phone. Adding/deleting cubes is outside this implementation. |
| Update cube placement and copy counts (2–3 h) | Partial | Moves preserve/append stored order and counts refresh from physical-copy records. Old inventory records still need verified physical positions; counts reflect stored records, not a reconciled shelf inventory. |
| Separate wishlist (3–5 h) | Delivered | Separate wishlist collection, add/search/remove confirmation, duplicate prevention, owned-book warning and existing add-book prefill. Wishlist entries do not affect owned counts. |
| Search/filter improvements (3–5 h) | Delivered | Title, author, formatted/equivalent ISBN, cube scope and reading/lending filters. |
| Library statistics and summaries (2–4 h) | Delivered | Reading counts and cube copy counts with drill-down and error states. |
| Export and import library data (3–5 h) | Delivered for full backups | Version 2 backs up works, editions, legacy books, physical copies, lending history, wishlist and local cube settings. Previewed restore adds missing records, skips matching records and blocks conflicts. Image files and older catalogue-only imports are excluded. |
| UI polish and empty/error states (3–5 h) | Partial | New tools have readable inputs, loading/retry/empty feedback. A complete app-wide review remains. |
| Testing, bug fixes and release build (5–8 h) | Partial | 93 local tests, 13 isolated backup/migration emulator tests, a final targeted editing test, and 55 private-rule checks pass for delivered work. Signed release validation and the existing Glide NotificationPermission lint failure remain. |

## Current completed batch

- Normalized and verified a separate V2 copy: 170 works, 171 editions and all 507 stored copies.
- Preserved source records as immutable portable JSON archives on converted copies.
- Private Google sign-in, owner-only rules, signed-out screen protection and verified V2 activation.
- Source database retained, with no book deletions or in-place migration.

Actual Google account sign-in and V2 activation will be tested on the OnePlus,
not by adding an account to the emulator. This remains a required check before
calling the migration cutover complete. See MIGRATION_V2.md.

## Next original-list work

1. On OnePlus: install the new build, sign in with the chosen Google account,
   verify library access and activate V2. If the original library changed while waiting, refresh migration before the
   first switch; the app blocks stale activation. Original and V2 databases are
   separate and are not synchronized after cutover.
2. Verify older physical shelf positions and counts against the actual inventory.
   504 stored copies have no recorded position; migration has not guessed them.
3. Finish the app-wide UI/error-state review and signed release checks, including
   resolving the existing Glide lint error and registering the release certificate
   for Google sign-in. Review how to retire/restrict the original archive after
   cutover; its original access rules have not been changed.

Full backups include works, editions, physical copies, legacy records if present,
wishlist, lending history and local cube settings. Original-record archives are
included. Cover image files and server-owned access/readiness configuration are
excluded. Restore adds missing records, skips matches and blocks conflicts.
Position counters only advance. Cube names/layout remain local device preferences
and restore separately. Earlier catalogue-only exports cannot be restored by the
full-backup importer. Private library access now requires Google sign-in and an
administrator-issued migration-readiness receipt.
