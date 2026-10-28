package com.example.bookcase;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import java.util.*;

/** Transitional repository: legacy books stay in place; new writes use normalized collections. */
public class BookRepository {
    private final FirebaseFirestore db;
    public BookRepository() { this(FirebaseFirestore.getInstance()); }
    public BookRepository(FirebaseFirestore db) { this.db = db; }
    public DocumentReference copyRef(String id, boolean normalized) {
        return db.collection(normalized ? "bookCopies" : "books").document(id);
    }
    public static final Comparator<Book> SHELF_ORDER = Comparator
            .comparingInt(Book::getLocation)
            .thenComparingLong(b -> b.getPosition() > 0 ? b.getPosition() : Long.MIN_VALUE)
            .thenComparing(Book::getId);

    public Task<Book> load(String id, boolean normalized) {
        return copyRef(id, normalized).get().continueWithTask(task -> {
            DocumentSnapshot doc = task.getResult();
            if (!doc.exists()) throw new IllegalStateException("Book not found");
            return hydrate(doc, normalized);
        });
    }
    private Task<Book> hydrate(DocumentSnapshot doc, boolean normalized) {
        if (!normalized) {
            Book book = doc.toObject(Book.class);
            book.setId(doc.getId()); // Document ID, never a stale embedded ID.
            return Tasks.forResult(book);
        }
        BookCopy copy = doc.toObject(BookCopy.class);
        return db.collection("editions").document(copy.getEditionId()).get().continueWith(task -> {
            DocumentSnapshot editionDoc = task.getResult();
            if (!editionDoc.exists()) throw new IllegalStateException("Edition not found");
            BookEdition edition = editionDoc.toObject(BookEdition.class);
            Book book = new Book(doc.getId(), edition.getTitle(), edition.getAuthor(),
                    edition.getDescription(), edition.getImageUrl(), copy.isRead(),
                    copy.getProgress(), copy.getCubeNumber());
            book.setIsbn(edition.getIsbn()); book.setPageCount(edition.getPageCount());
            book.setLendingStatus(copy.getLendingStatus()); book.setBorrowerName(copy.getBorrowerName());
            book.setPosition(copy.getPosition()); book.setNormalizedCopy(true);
            return book;
        });
    }
    public Task<List<Book>> getBooks(Integer cube) {
        Query legacy = db.collection("books");
        Query copies = db.collection("bookCopies");
        if (cube != null) {
            legacy = legacy.whereEqualTo("location", cube);
            copies = copies.whereEqualTo("cubeNumber", cube);
        }
        Task<QuerySnapshot> oldTask = legacy.get(), newTask = copies.get();
        return Tasks.whenAllSuccess(oldTask, newTask).continueWithTask(ignored -> {
            // Propagate either read failure; never show an incomplete library as a success.
            List<Task<Book>> reads = new ArrayList<>();
            for (DocumentSnapshot doc : oldTask.getResult()) reads.add(hydrate(doc, false));
            for (DocumentSnapshot doc : newTask.getResult()) reads.add(hydrate(doc, true));
            return Tasks.<Book>whenAllSuccess(reads).continueWith(task -> {
                List<Book> books = task.getResult();
                books.sort(SHELF_ORDER);
                return books;
            });
        });
    }
    private long nextPosition(DocumentSnapshot counter) {
        Long last = counter.getLong("lastPosition");
        return Math.addExact(last == null ? 0 : last, 1);
    }
    public Task<Void> add(Book book) {
        final String isbn;
        try {
            isbn = Isbn.normalize(book.getIsbn());
        } catch (IllegalArgumentException invalid) {
            return Tasks.forException(invalid);
        }
        book.setIsbn(isbn);
        if (isbn.isEmpty()) return addWithEdition(book, null);
        // Compare on read so formatted and ISBN-10 legacy editions need no migration.
        return db.collection("editions").get().continueWithTask(task -> {
            for (DocumentSnapshot edition : task.getResult()) {
                if (Isbn.equivalent(isbn, edition.getString("isbn")))
                    return addWithEdition(book, edition.getId());
            }
            return addWithEdition(book, null);
        });
    }

    private Task<Void> addWithEdition(Book book, String existingEditionId) {
        String id = book.getId();
        // Reuse an equivalent ISBN edition when present; never merge works by title.
        BookWork work = new BookWork(id, book.getTitle(), book.getAuthor(), book.getDescription());
        BookEdition edition = new BookEdition(id, id, book.getIsbn(), book.getTitle(),
                book.getAuthor(), book.getDescription(), book.getImageUrl(), book.getPageCount());
        DocumentReference counter = db.collection("cubePositions").document("" + book.getLocation());
        return db.runTransaction(tx -> {
            DocumentSnapshot current = tx.get(copyRef(id, true));
            DocumentSnapshot count = tx.get(counter);
            DocumentSnapshot existingEdition = existingEditionId == null ? null
                    : tx.get(db.collection("editions").document(existingEditionId));
            if (existingEdition != null && !existingEdition.exists())
                throw new IllegalStateException("Edition no longer exists; try again");
            if (current.exists()) throw new IllegalStateException("Copy already exists");
            long position = nextPosition(count);
            BookCopy copy = new BookCopy(id, existingEditionId == null ? id : existingEditionId, book.getLocation(), position);
            copy.setRead(book.isRead()); copy.setProgress(book.getProgress());
            if (existingEditionId == null) {
                tx.set(db.collection("works").document(id), work);
                tx.set(db.collection("editions").document(id), edition);
            }
            tx.set(copyRef(id, true), copy);
            tx.set(counter, Collections.singletonMap("lastPosition", position), SetOptions.merge());
            return null;
        });
    }
    public Task<Void> move(String id, boolean normalized, int cube) {
        return save(id, normalized, null, cube);
    }
    /** Metadata updates and moves commit together; lending fields are never overwritten. */
    public Task<Void> save(String id, boolean normalized, Book edited, int cube) {
        DocumentReference ref = copyRef(id, normalized);
        DocumentReference counter = db.collection("cubePositions").document("" + cube);
        return db.runTransaction(tx -> {
            DocumentSnapshot current = tx.get(ref);
            if (!current.exists()) throw new IllegalStateException("Book not found");
            Long oldCube = current.getLong(normalized ? "cubeNumber" : "location");
            boolean moved = oldCube == null || oldCube != cube;
            DocumentSnapshot count = moved ? tx.get(counter) : null;
            DocumentSnapshot edition = normalized && edited != null
                    ? tx.get(db.collection("editions").document(current.getString("editionId"))) : null;
            Map<String, Object> updates = new HashMap<>();
            if (moved) {
                long position = nextPosition(count);
                updates.put(normalized ? "cubeNumber" : "location", cube);
                updates.put("position", position);
                tx.set(counter, Collections.singletonMap("lastPosition", position), SetOptions.merge());
            }
            if (edited != null) {
                updates.put("read", edited.isRead());
                updates.put("progress", edited.getProgress());
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("title", edited.getTitle()); metadata.put("author", edited.getAuthor());
                metadata.put("description", edited.getDescription()); metadata.put("imageUrl", edited.getImageUrl());
                if (normalized) {
                    if (!edition.exists()) throw new IllegalStateException("Edition not found");
                    tx.update(edition.getReference(), metadata);
                    Map<String, Object> work = new HashMap<>(metadata); work.remove("imageUrl");
                    tx.update(db.collection("works").document(edition.getString("workId")), work);
                } else {
                    updates.putAll(metadata);
                    // Original documents may contain either spelling from earlier versions.
                    updates.put("isRead", edited.isRead());
                }
            }
            if (!updates.isEmpty()) tx.update(ref, updates);
            return null;
        });
    }
}
