# Library search

Press the magnifying glass at the right of the title on the cube selector to
search all books, or inside a selected cube to reveal search within that cube.
Title and author matching is case-insensitive. ISBN search ignores spaces and
hyphens, supports partial values, and uses the existing Isbn.equivalent helper
for equivalent ISBN-10/ISBN-13 values. Clearing restores the repository order.
Search state survives activity recreation. Counts refer to physical copies.

Search only filters the existing BookRepository results. It neither changes
Firestore records nor merges copies, reorders shelves, or alters duplicate ISBN
validation. Existing legacy and normalized copies remain individually clickable.

Validation on 29 October 2026:
- assembleDebug and testDebugUnitTest passed: 10 tests, zero failures.
- New tests cover title/author, partial/formatted/equivalent ISBNs, missing fields,
  no matches, clearing, copy identity, order, cube and position preservation.
- Installed on connected CPH2409 using an in-place update, preserving app data.
- Device loaded 506 copies globally and 91 in Cube 1.
- Microeconomics and Mankiw each returned 3 copies; a nonsense query returned
  no matches; clearing restored 506. Opening a result showed Current Cube: 1.
- Magnifier buttons verified on phone: selector opens global search (Mankiw: 3
  of 506); Cube 1 initially hides search, then its magnifier reveals it (Mankiw:
  3 of 91). Both buttons sit at the right of the toolbar title.
- Formatted ISBN prefix 978- returned 4 copies on the device.
- lintDebug completed analysis but failed on the existing NotificationPermission
  issue attributed to Glide NotificationTarget in the unchanged manifest. It
  reported 1 error, 169 warnings, and 1 hint; no lint baseline was introduced.
- Android Studio launch was attempted, but macOS reported its executable missing.
  Build and device tests used the installed Android Studio JDK and Android SDK.

No database migrations, additions, edits, or deletions were performed for testing.
Live add/move/lend/delete operations were not exercised. Existing ISBN and shelf
ordering unit tests passed. Unrelated IDE settings and .kotlin files were excluded.
