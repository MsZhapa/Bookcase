# Locations, scanning, filters and catalogue export

Implemented 2 November 2026.

## Behaviour

- Book rows show each copy’s cube and saved position. Records without a positive
  position say “Position not recorded”; no position is inferred from list order.
- Reading labels and a status dropdown are available in both cube lists and
  global search: All books, Unread, In progress, Finished, Lent out. Finished means
  the stored read flag is set or progress is at least 100%. Lending is independent
  of reading status. Search and filtering retain the repository order and copy IDs.
- “Scan ISBN barcode” in Add a New Book reads EAN-13 book barcodes, checks them
  using the existing ISBN validator, and fills the ISBN field. “Find Book” remains
  an explicit action so scanning does not overwrite edited metadata. Saving still
  uses the existing duplicate ISBN detection and multiple-copy support. Invalid
  product barcodes are rejected; cancellation leaves entered fields intact, and
  scanner errors retain manual entry as a fallback.
- “Export library” on the cube selector saves the complete catalogue, regardless
  of search or filters, via Android’s file picker. JSON contains a version,
  export time, copy count, source collection, IDs, metadata, cover URLs, cube,
  stored position, read/progress and current lending status/borrower name.
  An export survives activity rotation through its ViewModel. File writes run
  off the UI thread. Read/write failures are reported instead of claiming success.
- This is a portable catalogue snapshot, not a full Firestore backup or restore
  feature: it excludes downloaded cover images, lending history, original
  edition/work relationships, and cube allocation counters. Failed saves may
  leave an incomplete destination file; the UI explicitly reports failure.

Scanner implementation follows the official Google Code Scanner API:
https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner
Google Play services supplies the scanner; its module may need a first download.

## Validation

- Debug APK assembled successfully. All 15 local unit tests passed, including
  the existing ISBN and shelf-order regression tests, search tests, two new filter
  tests, and three new export tests. Export tests cover separate physical copies,
  source identifiers, missing positions, null metadata, special characters,
  Unicode, an empty catalogue and output failures.
- Installed and exercised on connected CPH2409 with an in-place update.
- Cube 1 initially loaded 91 copies: Unread showed 90 and In progress showed one
  at 69%. Finished and Lent out correctly showed no matches in that cube.
- Searching Mankiw returned three distinct copies. Unread retained all three;
  Finished returned no matches. Clearing/resetting retained the full cube order.
- Global search loaded 506 copies at test time. Error-correction search showed two
  individual copies in Cube 1 at stored positions 1 and 2. Older records displayed
  “Position not recorded”. Screen layout and readable text were visually checked.
- Saved a real export to the phone’s Downloads folder. Read it back and parsed
  it with an independent JSON parser: 506 records, 506 unique source/ID pairs,
  504 legacy books plus two normalized copies; all cube numbers and both positions
  were present, and the entire list retained repository shelf order.
- The user physically scanned a book and confirmed the camera filled ISBN
  9780316172325. The existing Find Book action was exercised. The user subsequently
  operated the app, and Cube 1 then showed 92 copies. The test export reflects the
  earlier 506-copy snapshot; no test book was automatically deleted.
- Android lint was run. The toolbar tint issue was fixed. The remaining failure
  is the pre-existing NotificationPermission report attributed to Glide’s
  NotificationTarget; lint reports 1 error and 166 warnings. No permissions or
  blanket lint suppressions were added to conceal that existing issue.
- No migration or destructive database operation was performed. BookRepository,
  Isbn, BookCopy and duplicate-save logic were left unchanged. Unrelated IDE
  settings and .kotlin files are excluded from the commit.

## Files changed

- app/build.gradle — Google Code Scanner dependency.
- app/src/main/AndroidManifest.xml — scanner module metadata and export screen.
- app/src/main/java/com/example/bookcase/AdminActivity.java — validated scan input.
- app/src/main/java/com/example/bookcase/BookAdapter.java — copy location/read labels.
- app/src/main/java/com/example/bookcase/BookFilters.java — copy-preserving status rules.
- app/src/main/java/com/example/bookcase/CubesActivity.java — export entry point.
- app/src/main/java/com/example/bookcase/MainActivity.java — combined search/status UI.
- app/src/main/java/com/example/bookcase/LibraryExportActivity.java — file-picker export.
- app/src/main/java/com/example/bookcase/LibraryJson.java — versioned JSON snapshot.
- app/src/main/res/layout/activity_admin.xml — scan button.
- app/src/main/res/layout/activity_cubes.xml — export button.
- app/src/main/res/layout/activity_main.xml — filter dropdown.
- app/src/main/res/layout/activity_library_export.xml — export explanation/status.
- app/src/main/res/layout/filter_item.xml — readable dropdown items.
- app/src/main/res/layout/book_item.xml — readable status colour.
- app/src/main/res/layout/toolbar_layout.xml — compatible magnifier tint.
- app/src/main/res/values/strings.xml — labels, filters and feedback.
- app/src/test/java/com/example/bookcase/BookFiltersTest.java — filtering regressions.
- app/src/test/java/com/example/bookcase/LibraryJsonTest.java — export regressions.
- LIBRARY_FEATURES.md — usage, validation and file list.
