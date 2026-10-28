# Ordered copies: incremental transition

New entries now use `works/{id}`, `editions/{id}` (`workId`), and
`bookCopies/{copyId}` (`editionId`, `cubeNumber`, `position`). BookCopy has no
row/top field. An exact existing ISBN edition is reused; entries without ISBN
receive independent catalog records. Concurrent first additions for an unseen
ISBN can create separate editions; later catalog reconciliation can link them.
Edition metadata remains in the existing BookEdition model during this transition.
Editing it affects every copy linked to that edition. Reading progress and lending
belong to the physical copy.

Positions are positive increasing sequence numbers, allocated atomically through
`cubePositions/{cubeNumber}.lastPosition`. Additions and moves append; a move to
the same cube preserves position. Gaps after moves/deletes are intentional.
Loans and returns never alter cube or position. Input/import order should flatten
the inventory as back row, front row, then top, without saving those categories.

Legacy `books` are read alongside new copies and are updated in place. Nothing is
migrated or deleted on startup. Legacy books without position sort first by
Firestore document ID, followed by positioned entries. This fallback is stable,
but cannot reconstruct physical order that was never stored. Assigning the old
inventory's exact positions requires the complete inventory/import and an explicit
migration step; do not derive shelf order from titles. Any future importer that
writes explicit positions must also seed the cube counter to the maximum used
position. Do not create a normalized copy of a legacy entry without planning how
to avoid displaying both.

Existing lending records retain `bookId`; new records also include `bookCopyId`
with the same physical-copy ID. Active loans, borrower names, Timestamp dates,
due dates, return history, and activeLoanId remain supported. Lend and return
writes are transactional. Existing legacy IDs are taken from document paths,
not potentially stale embedded IDs. Confirmed copy deletion leaves catalog and
lending history intact.

## Validation and rollout

- Incremental debug builds and local unit tests pass with the Android Studio JDK.
- Ordering tests cover per-cube positions and stable legacy fallback.
- No app was installed/launched and no live Firestore data was changed.
- Before device testing, verify the deployed Firestore rules permit the user's
  existing authorization model for `works`, `editions`, `bookCopies`, and
  `cubePositions`, in addition to `books` and `lendingRecords`. Rules were not
  available in this repository and have not been changed or deployed.
- Test add, duplicate ISBN copy, edit, same/different cube moves, lend, return,
  due date/history, and confirmed delete against an emulator or separate Firebase
  project before rollout. Local tests do not verify server rules or transactions.
- Existing Gradle deprecation warnings are unrelated to this change.

Pre-change Java source backup is in this chat workspace under
`outputs/bookcase-before-ordered-copies`. Existing uncommitted icons, layouts,
manifest, and other user edits were preserved; no Git commit was created.
