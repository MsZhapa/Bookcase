# Bookcase original feature checklist

Reviewed 9 November 2026 against the repository and device checks.
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
| Export and import library data (3–5 h) | Partial | Owned-copy catalogue JSON export exists. Import/restore, wishlist export and cube-layout backup are still pending. |
| UI polish and empty/error states (3–5 h) | Partial | New tools have readable inputs, loading/retry/empty feedback. A complete app-wide review remains. |
| Testing, bug fixes and release build (5–8 h) | Partial | 51 local tests and device checks pass for delivered work. Signed release validation and the existing Glide NotificationPermission lint failure remain. |

## Current completed batch

- Edition grouping as a read-only catalogue view; no canonical database merges.
- Custom names/layout for the eight default cubes, with refreshed copy counts.
- A separate wishlist, with safe reuse of the existing add-book workflow.

## Next original-list work

1. Design library import with a preview, duplicate/edition reconciliation and
   explicit shelf positions/counter checks before any writes.
2. Plan the isolated V2 database and controlled migration of legacy inventory
   into shared works/editions/copies. Existing library data must be preserved;
   destructive changes require the user’s approval.
3. Finish the app-wide UI/error-state review and release checks, including
   resolving the existing lint issue and validating a signed release build.

Wishlist data uses the existing Firebase database. Cube labels and grid layout
are local display preferences. The owned-catalogue export currently excludes
wishlist entries, cube preferences, cover-image files and full lending history.
