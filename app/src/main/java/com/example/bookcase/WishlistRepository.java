package com.example.bookcase;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;
import java.util.*;
public final class WishlistRepository {
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    public Task<List<WishItem>> get() {
        return db.collection("wishlist").get().continueWith(task -> {
            List<WishItem> items=new ArrayList<>();
            for (DocumentSnapshot doc:task.getResult()) {
                WishItem wish=doc.toObject(WishItem.class); wish.setId(doc.getId()); items.add(wish);
            }
            items.sort(Comparator.comparingLong(WishItem::getCreatedAt).reversed().thenComparing(WishItem::getId)); return items;
        });
    }
    public Task<Void> add(WishItem wish) {
        DocumentReference ref=db.collection("wishlist").document(wish.getId());
        return db.runTransaction(tx -> {
            if (tx.get(ref).exists()) throw new IllegalStateException("This book is already on your wishlist");
            tx.set(ref,wish); return null;
        });
    }
    public Task<Void> remove(String id) { return db.collection("wishlist").document(id).delete(); }
}
