# Full backup and restore — implementation and verification

Current update, November 11, 2026: backups and restores are scoped to the signed-in
account. Original recovery databases deny client access. See ACCOUNTS.md.

Historical verification, 10 November 2026. Implements the backup/import item from the original
Bookcase roadmap. App changes are on develop.

## Behavior

Export library now saves version 2 full backups of the app's seven collections:
books, works, editions, bookCopies, lendingRecords, wishlist and cubePositions.
The file also contains this device's eight cube names and grid columns.
Cover links are included; binary image files are excluded. Files contain notes
and borrower names, so the export screen asks the reader to keep them private.

Restore library opens the Android file picker and displays a read-only preview:
missing records to add, matching records to skip, conflicts that block import,
and additional physical copies sharing an ISBN. ISBN matches never collapse
multiple copies. Copy IDs, normalized links, notes, progress, active/history
loans, cube numbers and known positions are preserved. Unknown positions stay
unknown. An occupied known position blocks a new copy instead of moving books.

Cube names/layout are optional and require separate confirmation. A no-op restore
stays disabled unless local settings or shelf counters need restoration.

Server-only reads avoid exporting or previewing an incomplete offline cache.
Restore rechecks the preview against the current library and uses create-only
single-record transactions under the private V2 rules. Matching records are untouched;
different existing records block the import. Position counters only advance and
are checked between chunks to detect concurrent additions/moves. Dependencies
are read again before a copy is added. No records are deleted by restore.

Large restores are resumable rather than globally atomic. If interrupted, some
missing records can already exist; select the same file again to preview and
finish the remainder. The screen reports this possibility instead of claiming
rollback. Reimporting a completed backup creates no additional copies.

## Validation

- Debug build passed. At that batch, all 69 JVM tests passed, with zero failures or errors.
- Six integration tests ran on the Android 16 emulator using named Firebase
  apps connected exclusively to the local Firestore emulator and isolated
  demo project IDs. No integration test used the production Firebase app.
- Integration cases: 509-copy restore across multiple chunks; repeat import
  without writes; conflict blocking without writes; stale preview rejection;
  interruption/resume; concurrent counter-change stop/resume; and repair of a
  missing counter without duplicate copies. Large restore and repeat import
  share one test, as do the counter stop/resume checks.
- Date round trips use Firestore's microsecond precision. Unsupported finer
  timestamps are rejected before import instead of producing false conflicts.
- Android file picker export saved a full backup of the then-stored 507 copy records (before inventory reconciliation),
  two works, two editions, one lending record, one wishlist entry and one counter.
  Reopening it showed zero missing records, 513 matching non-counter records,
  and zero conflicts. The real library was only read; restore was not confirmed.
- Emulator UI checked the disabled no-op action, optional cube-settings
  confirmation and Cancel. Conflicting, truncated and version 1 files were
  blocked with restore disabled. A temporary emulator connection failure
  produced the error state; preview succeeded after reconnecting.
- Android 15/16 shared-toolbar insets keep controls below the status bar and
  content above the navigation bar. Earlier Android versions keep their layout.
- git diff --check passed. Lint has only the existing Glide
  NotificationPermission error; no new lint errors were introduced.

## Limits and operational notes

- Supports app-generated version 2 full backups, not version 1 catalogue-only
  exports or arbitrary CSV/import formats. Old exports lack normalized links
  and full lending history; create a new full backup before migration.
- Input is bounded to 10,000 records and roughly 20 million JSON characters,
  with limits on nesting, strict field types, duplicate-key/ID checks, valid
  cube/page values and required work/edition/active-loan links. Unsupported
  field types fail explicitly; they are never silently dropped.
- Records with conflicts are not overwritten. A separate reviewed migration
  or reconciliation workflow is still needed for the V2 architecture work.
- Backups gather online collection reads; they are not a database-wide snapshot
  transaction. Avoid editing the library on another device while backing up or
  restoring. Import detects stale previews and rechecks affected dependencies
  and shelf counters, but is not an exclusive database maintenance lock.
- Production database selection, schema layout, rules, indexes and deployed
  services were not changed. No destructive database operations were performed.
- Gson 2.10.1 is now a direct dependency, matching the version already resolved
  transitively through the existing Firestore dependency.

## Reproducing emulator restore tests

Start a local Firestore emulator on port 8080 with a demo project. Build the debug
app and Android test APK, install both on the Android emulator, and run:

```text
adb -s <emulator-serial> shell am instrument -w \
  -e class com.example.bookcase.BackupEmulatorTest \
  com.example.bookcase.test/androidx.test.runner.AndroidJUnitRunner
```

Tests require the local emulator at 10.0.2.2:8080. The test class always creates
named demo Firebase apps and sets the emulator host before accessing records.
Gradle's connected-test wrapper needed an uncached UTP plugin in offline mode,
so the installed instrumentation tests were run directly through adb.

## Files changed

- `BACKUP_RESTORE.md`
- `ROADMAP.md`
- `app/build.gradle`
- `app/src/androidTest/java/com/example/bookcase/BackupEmulatorTest.java`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/bookcase/BackupJson.java`
- `app/src/main/java/com/example/bookcase/BackupRepository.java`
- `app/src/main/java/com/example/bookcase/InsetToolbar.java`
- `app/src/main/java/com/example/bookcase/LibraryBackup.java`
- `app/src/main/java/com/example/bookcase/LibraryExportActivity.java`
- `app/src/main/java/com/example/bookcase/LibraryMenu.java`
- `app/src/main/java/com/example/bookcase/LibraryRestoreActivity.java`
- `app/src/main/java/com/example/bookcase/RestorePlan.java`
- `app/src/main/res/layout/activity_library_restore.xml`
- `app/src/main/res/layout/toolbar_layout.xml`
- `app/src/main/res/menu/library_actions.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/example/bookcase/BackupTest.java`

The V2/auth batch extends validation to 93 JVM tests, 13 emulator regression tests
and 55 private-rule checks; see MIGRATION_V2.md. Real Google/device activation subsequently passed on OnePlus; account-scoped
login/register/sign-out and isolation are documented in ACCOUNTS.md.
