# Bookcase original feature checklist

Reviewed 10 November 2026 against the repository and device checks.
Source: the user-pasted **Features list** in the ChatGPT conversation
**learning plan**. This preserves the original scope rather than substituting
new feature recommendations. Initial time estimates are historical planning
estimates, not new completion promises.

| Original item | Status | Evidence / remaining work |
|---|---|---|
| Create BookWork.java (1–2 h) | Delivered | Model exists with ID, title, author and description. |
| Work → Edition → Copy relationships (4–6 h) | Partial | New records use linked works, editions and physical copies. Legacy books are still supported. Different ISBN editions have not been reconciled into shared canonical work records. |
| Separate V2 Firebase database/import (2–4 h) | Pending | App still uses its established default database. No cutover or bulk import has been performed. |
| Migrate existing CRUD screens (4–6 h) | Partial | Screens support old and normalized records; existing data has not been bulk migrated. |
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
| Testing, bug fixes and release build (5–8 h) | Partial | 69 local tests and six isolated restore tests on the Android emulator pass for delivered work. Signed release validation and the existing Glide NotificationPermission lint failure remain. |

## Current completed batch

- Full version 2 backups, including wishlist, local cube settings and lending history.
- Restore preview, conflict blocking, create-only import and resumable large-library restore.
- Android 15/16 system-bar spacing for shared header controls and bottom content.

The preceding batch delivered edition browsing, eight-cube customization and a separate wishlist.

## Next original-list work

1. Plan the isolated V2 database and controlled migration of legacy inventory
   into shared works/editions/copies, using the verified full backup first.
   Preview canonical edition reconciliation and physical shelf positions before
   any migration writes. Destructive changes require the user’s approval.
2. Verify older physical shelf positions and counts against the actual inventory.
3. Finish the app-wide UI/error-state review and release checks, including
   resolving the existing lint issue and validating a signed release build.

Wishlist data uses the existing Firebase database. Cube labels and grid layout
remain local preferences and are now included in full backups. Restoring them
is a separate, explicit option. Restore never replaces an existing book, edition,
wishlist entry or lending record; differences block restore. Position counters
only advance. Earlier version 1 catalogue-only exports cannot be restored by
this importer; create a version 2 full backup instead. Backups include cover
links, not image files, and require an internet connection.
