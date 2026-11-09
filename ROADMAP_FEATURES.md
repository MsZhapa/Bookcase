# Original roadmap batch — implementation and verification

Verified 9 November 2026. Scope comes from the original user-pasted feature list in the learning plan conversation; see ROADMAP.md for the complete checklist and unfinished items.

## Delivered

- Browse editions: read-only title/author grouping, explicit numbered edition suffix handling, original copy identity and shelf ordering preserved. No database merge.
- Cube customization: names and 1–4 display columns for the eight fixed cubes, with physical-copy counts. Preferences are local to each device.
- Separate wishlist: add/search, deterministic duplicate prevention, owned-book warnings, confirmed removal, and prefill of the existing add-book form. Prefill does not create an owned copy.

## Validation

- Debug build succeeded; 51 unit tests passed with zero failures/errors.
- Emulator: all eight counts matched the 507-copy baseline; three-column layout and cube name survived restart; custom names appeared in the cube screen and add-book picker. Layout restored to two columns after checking.
- Emulator: Interaction Design showed six distinct copies, retaining the named second and fifth editions.
- Emulator: the wishlist entry saved on the phone loaded successfully; duplicate save was rejected; add-book prefill and cancellation of removal passed. No owned copy was added.
- Earlier phone checks passed for edition browsing, wishlist save/duplicate/prefill/removal cancellation, and the user-selected Travel name for Cube 5. Final checks used the emulator at the user's request after USB disconnected.
- Independent read-only Firestore comparison matched all 507 owned copies to the pre-test export, including copy-specific fields. Owned documents and edition timestamps showed no writes during testing. The separate wishlist had one entry; the duplicate attempt did not update it.
- git diff --check passed.
- Lint remains blocked by the pre-existing Glide NotificationPermission error. This batch does not resolve that dependency issue.

No database migration, deletion, rules change, or database cutover was performed. Wishlist and cube preferences are not yet included in the owned-catalogue export. Import/restore and canonical work reconciliation remain on the original roadmap.

## Files changed

- `ROADMAP.md`
- `ROADMAP_FEATURES.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/bookcase/AdminActivity.java`
- `app/src/main/java/com/example/bookcase/BookAdapter.java`
- `app/src/main/java/com/example/bookcase/BookDetailActivity.java`
- `app/src/main/java/com/example/bookcase/CatalogueKeys.java`
- `app/src/main/java/com/example/bookcase/CatalogueToolActivity.java`
- `app/src/main/java/com/example/bookcase/CopyNotesActivity.java`
- `app/src/main/java/com/example/bookcase/CubeLayout.java`
- `app/src/main/java/com/example/bookcase/CubeSettings.java`
- `app/src/main/java/com/example/bookcase/CubeSettingsActivity.java`
- `app/src/main/java/com/example/bookcase/CubesActivity.java`
- `app/src/main/java/com/example/bookcase/EditBookActivity.java`
- `app/src/main/java/com/example/bookcase/EditionGroups.java`
- `app/src/main/java/com/example/bookcase/EditionsActivity.java`
- `app/src/main/java/com/example/bookcase/LibraryMenu.java`
- `app/src/main/java/com/example/bookcase/MainActivity.java`
- `app/src/main/java/com/example/bookcase/ReadingProgressActivity.java`
- `app/src/main/java/com/example/bookcase/WishAddActivity.java`
- `app/src/main/java/com/example/bookcase/WishItem.java`
- `app/src/main/java/com/example/bookcase/WishlistActivity.java`
- `app/src/main/java/com/example/bookcase/WishlistMatches.java`
- `app/src/main/java/com/example/bookcase/WishlistRepository.java`
- `app/src/main/res/layout/activity_catalogue_tool.xml`
- `app/src/main/res/layout/activity_cube_settings.xml`
- `app/src/main/res/layout/activity_cubes.xml`
- `app/src/main/res/layout/activity_wish_add.xml`
- `app/src/main/res/layout/book_item.xml`
- `app/src/main/res/layout/edition_group_item.xml`
- `app/src/main/res/layout/wishlist_item.xml`
- `app/src/main/res/menu/library_actions.xml`
- `app/src/main/res/values/catalogue_ids.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/example/bookcase/CubeLayoutTest.java`
- `app/src/test/java/com/example/bookcase/EditionGroupsTest.java`
- `app/src/test/java/com/example/bookcase/WishlistTest.java`
