# Library menu and page-based reading progress

Implemented 3 November 2026.

## Behaviour

The cube selector’s three overview/export buttons have moved into a three-dot
menu at the far right of the toolbar. The menu contains Reading overview,
Loan overview and Export library. It is also available in book lists, including
cube lists. The search magnifier and per-cube add button remain accessible.
The title now fits within the remaining toolbar width.

A book detail has an Update reading progress action. The reader enters the
page reached; a known page total is prefilled, or the reader supplies it once.
Percentage is calculated and shown immediately. Add Book and Edit Book use the
same page inputs instead of percentage sliders. The read checkbox can mark a
book complete; reaching the last page also marks it complete. Page 0 means
unread. Invalid, negative, overflowing or out-of-range page values are rejected.
A positive current page requires a total so no percentage is fabricated.
Finished/unread books can be saved without a known total.

Percentages are stored as whole numbers for compatibility with the existing
filters and overview. They are truncated, with a minimum of 1% once a positive
page is entered; they reach 100% only at the final page or explicit completion.
For example, page 15 of 422 is displayed as 3%.

Each physical copy owns nullable currentPage and readingPageCount fields.
The latter is the page total used for that copy’s progress, and can override the
bibliographic edition page count without changing other copies. The percentage
and read flag remain stored for existing screens and filters. Legacy records
with only a percentage keep it until pages or completion are supplied; no page
number is reverse-calculated from an old percentage. Blank page entry preserves
an existing legacy percentage, while a recorded bookmark requires an explicit
page (including 0 to reset it). Page entry drafts survive screen recreation.

Progress-only saves use a transaction that updates only the selected copy’s
progress/read/page fields, including the legacy isRead alias when applicable.
They do not modify metadata, notes, loans, ISBNs, edition links or shelf positions.
Add/edit flows keep their established saving behaviour and preserve the new page
fields. Export includes currentPage and readingPageCount as nullable numbers.
No database migration or shared-edition page-count rewrite is performed.

The existing Bookcase default Firestore database was inspected read-only and
confirmed as Standard edition. No Firebase configuration, security rules,
indexes, database instance or SDK dependency was changed.

## Validation

- Final debug build succeeded. All 35 unit tests passed: 24 existing tests plus
  11 new page-progress tests. They cover ordinary calculations, completion,
  old percentages, unknown totals, invalid/negative pages, missing totals,
  cleared bookmarks, long-book rounding, integer overflow and copy isolation.
  Export tests also verify the new numeric page fields.
- Installed on the connected CPH2409 without clearing app data. Visually checked
  the menu, page-progress screen and Add/Edit controls, including text contrast.
- All three menu actions opened correctly on the phone. The old large button
  rows were absent, and search was still available. The menu also worked from
  global search/book lists.
- Opened the old 69%-progress record: it still showed 69% with no guessed page
  number or fabricated page total.
- The user saved page 15 of 422 for Cube 1, Position 1. Detail/list/editor showed
  3%, and reopening loaded the saved page and total.
- Entering page 423 for 422 total pages was rejected without a database write.
  Reopening still showed page 15. Toggling Finished previewed 100%; toggling it
  back restored the 3% preview without saving those temporary UI changes.
- Edit Book loaded page 15/422 and contained no percentage slider. Add Book
  defaulted to page 0 and also used page inputs. These checks created no book and
  did not save metadata edits.
- Position 2 stayed unread at 0%, with its empty notes and position unchanged.
  A fresh 507-copy export was parsed independently. Compared with the preceding
  snapshot, only the chosen progress/read fields changed on Position 1; all
  previous metadata, IDs, notes, cube numbers, lending fields and positions were
  identical. The old 69% record remained unchanged. The export is in Downloads:
  Bookcase-library-2026-11-03-092832.json.
- Android lint completed analysis and remains blocked by the pre-existing Glide
  NotificationPermission error. No new lint error or blanket suppression was
  introduced. Existing Gradle/deprecation warnings remain.
- No destructive database change, migration, test book or test loan was made.
  The real progress entry was entered and saved by the user. Unrelated IDE
  settings and .kotlin files are excluded from the commit.

## Files changed

- MENU_PAGE_PROGRESS.md
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/example/bookcase/AdminActivity.java
- app/src/main/java/com/example/bookcase/Book.java
- app/src/main/java/com/example/bookcase/BookAdapter.java
- app/src/main/java/com/example/bookcase/BookCopy.java
- app/src/main/java/com/example/bookcase/BookDetailActivity.java
- app/src/main/java/com/example/bookcase/BookRepository.java
- app/src/main/java/com/example/bookcase/CubesActivity.java
- app/src/main/java/com/example/bookcase/EditBookActivity.java
- app/src/main/java/com/example/bookcase/LibraryJson.java
- app/src/main/java/com/example/bookcase/LibraryMenu.java
- app/src/main/java/com/example/bookcase/MainActivity.java
- app/src/main/java/com/example/bookcase/PageProgressForm.java
- app/src/main/java/com/example/bookcase/ReadingProgress.java
- app/src/main/java/com/example/bookcase/ReadingProgressActivity.java
- app/src/main/res/drawable/ic_library_more.xml
- app/src/main/res/layout/activity_admin.xml
- app/src/main/res/layout/activity_book_detail.xml
- app/src/main/res/layout/activity_cubes.xml
- app/src/main/res/layout/activity_edit_book.xml
- app/src/main/res/layout/activity_reading_progress.xml
- app/src/main/res/layout/page_progress_fields.xml
- app/src/main/res/layout/toolbar_layout.xml
- app/src/main/res/menu/library_actions.xml
- app/src/main/res/values/strings.xml
- app/src/test/java/com/example/bookcase/LibraryJsonTest.java
- app/src/test/java/com/example/bookcase/ReadingProgressTest.java
