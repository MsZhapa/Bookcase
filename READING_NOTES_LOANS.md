# Reading overview, copy notes and loan overview

Implemented 3 November 2026.

## Behaviour

Reading overview is available from the cube selector. It counts physical copies
using the same reading-state rules as the existing filters. Reading states are
mutually exclusive; lending is independent. Tapping a count opens the full
library with that filter selected. Counts refresh when returning to the screen.
Loading and read failures disable the counts and offer a retry.

Personal notes open from a book’s detail screen. The editor identifies the title,
cube and stored position. Each physical copy owns its own notes; the description,
shared edition and other copies are unaffected. Notes support multiline Unicode
text up to 20,000 UTF-16 code units. Drafts are retained using SavedStateHandle
and the editor’s ViewModel. Leaving a changed draft or reloading it asks before
discarding it. Loading failures disable saving. A transaction checks the note
still matches the version originally loaded and updates only the notes field on
the selected books or bookCopies document. A concurrent edit is rejected rather
than silently overwritten; the draft stays available for review or copying.

Loan overview lists currently lent physical copies, with borrower, cube/position,
due date, and overdue/due-today labels. Overdue loans sort first, then due dates,
then shelf order; undated loans sort last. Show overdue only filters the list.
Due dates are compared as calendar dates in the device time zone, including DST:
a book due today is not overdue merely because its timestamp is earlier today.
Tapping a copy opens the existing book detail and lending/return actions.
The overview reads current loan fields already stored on physical copies; it
neither reconstructs nor changes lending history. It refreshes on return.

Catalogue export now also includes personal notes and current loan timestamps
as nullable epoch milliseconds. The JSON format gains additive fields without
changing copy identifiers. It remains a catalogue snapshot, excluding image
files, full lending history, original work/edition links and allocation counters.

## Validation

- Final assembleDebug and testDebugUnitTest passed: 24 tests, no failures/errors.
- Nine new tests cover reading totals/empty libraries, notes-only updates,
  independent copy defaults, stale notes, text length, due-today handling,
  missing dates, local time zones, DST, returned loans, urgency ordering and
  separate physical copies. Existing search, ISBN, shelf ordering and export
  tests remain passing. Export assertions now include notes and due timestamps.
- Installed the final APK on the connected CPH2409 without clearing app data.
- Phone reading overview showed 507 copies: 506 unread, one in progress, zero
  finished and zero lent. Tapping In progress opened its one matching copy.
- Loan overview showed zero active and zero overdue loans. Overdue-only displayed
  the correct empty state. There were no real active loans to exercise due-date
  rows against live data; due-date and ordering branches are covered by tests.
- The user saved a real note on the normalized copy in Cube 1, Position 1.
  Reopening the editor loaded it. Position 2’s note remained empty. Comparing
  both exported copies against the preceding snapshot confirmed that all prior
  metadata, IDs, cube numbers, reading states and positions were unchanged.
- Saved and parsed a fresh export with 507 unique source/ID pairs. The note was
  attached only to Position 1, and the other copy remained separate and unchanged.
  The test file is in the phone’s Downloads folder: Bookcase-library-2026-11-03-085102.json.
- On the phone, a temporary unsaved draft triggered the leave confirmation;
  Cancel retained the draft. Confirming Reload restored the saved note. This
  draft check made no database write. The final notes layout and cube selector
  were visually inspected; the notes editor shows copy identity and readable text.
- Android lint completed analysis and remains blocked by the existing Glide
  NotificationPermission error. No new lint error or blanket suppression was
  introduced. Existing build/deprecation warnings remain.
- No migration, bulk rewrite, database deletion, test loan or test book was
  created. The live note was entered and saved by the user. Notes use an additive
  field on existing copy documents. ISBN matching, duplicate-add validation,
  position allocation, moves, edits and lending transactions remain intact.
- Unrelated IDE settings and .kotlin files are excluded from the commit.

## Files changed

- READING_NOTES_LOANS.md
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/example/bookcase/Book.java
- app/src/main/java/com/example/bookcase/BookAdapter.java
- app/src/main/java/com/example/bookcase/BookCopy.java
- app/src/main/java/com/example/bookcase/BookDetailActivity.java
- app/src/main/java/com/example/bookcase/BookRepository.java
- app/src/main/java/com/example/bookcase/CopyNotesActivity.java
- app/src/main/java/com/example/bookcase/CubesActivity.java
- app/src/main/java/com/example/bookcase/LibraryJson.java
- app/src/main/java/com/example/bookcase/LoanRules.java
- app/src/main/java/com/example/bookcase/LoansActivity.java
- app/src/main/java/com/example/bookcase/MainActivity.java
- app/src/main/java/com/example/bookcase/NoteUpdate.java
- app/src/main/java/com/example/bookcase/ReadingOverviewActivity.java
- app/src/main/java/com/example/bookcase/ReadingSummary.java
- app/src/main/res/layout/activity_book_detail.xml
- app/src/main/res/layout/activity_copy_notes.xml
- app/src/main/res/layout/activity_cubes.xml
- app/src/main/res/layout/activity_loans.xml
- app/src/main/res/layout/activity_reading_overview.xml
- app/src/main/res/layout/book_item.xml
- app/src/main/res/values/strings.xml
- app/src/test/java/com/example/bookcase/LibraryJsonTest.java
- app/src/test/java/com/example/bookcase/LoanRulesTest.java
- app/src/test/java/com/example/bookcase/NoteUpdateTest.java
- app/src/test/java/com/example/bookcase/ReadingSummaryTest.java
