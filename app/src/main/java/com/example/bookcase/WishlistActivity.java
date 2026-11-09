package com.example.bookcase;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.*;
import java.util.*;
public class WishlistActivity extends CatalogueToolActivity {
    private final List<WishItem> all=new ArrayList<>(), visible=new ArrayList<>();
    private final List<Book> owned=new ArrayList<>();
    private WishAdapter adapter; private String query=""; private boolean loaded;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);title(R.string.wishlist);if (state!=null) query=state.getString("query","");
        search(query,text->{query=text;render();});adapter=new WishAdapter();list.setAdapter(adapter);
        View add=findViewById(R.id.addBookToCubeButton);add.setVisibility(View.VISIBLE);add.setContentDescription(getString(R.string.add_wish));
        add.setOnClickListener(v->startActivity(new Intent(this,WishAddActivity.class)));retry.setOnClickListener(v->load());
    }
    @Override protected void onResume() {super.onResume();load();}
    private void load() {
        loaded=false;status.setText(R.string.wishlist_loading);retry.setVisibility(View.GONE);
        Task<List<WishItem>> wishes=new WishlistRepository().get();Task<List<Book>> library=new BookRepository().getBooks(null);
        Tasks.whenAllSuccess(wishes,library).addOnSuccessListener(ignored->{
            all.clear();all.addAll(wishes.getResult());owned.clear();owned.addAll(library.getResult());loaded=true;render();
        }).addOnFailureListener(e->{status.setText(R.string.wishlist_load_failed);retry.setVisibility(View.VISIBLE);});
    }
    private void render() {
        if (!loaded) return;visible.clear();for (WishItem wish:all) if (WishlistMatches.matches(wish,query)) visible.add(wish);
        adapter.notifyDataSetChanged();status.setText(all.isEmpty()?getString(R.string.wishlist_empty)
                :visible.isEmpty()?getString(R.string.no_matching_books):getString(R.string.wishlist_count,visible.size(),all.size()));
    }
    @Override protected void onSaveInstanceState(Bundle state) {state.putString("query",query);super.onSaveInstanceState(state);}
    private class WishAdapter extends RecyclerView.Adapter<WishHolder> {
        @NonNull public WishHolder onCreateViewHolder(@NonNull ViewGroup parent,int type) {
            return new WishHolder(LayoutInflater.from(WishlistActivity.this).inflate(R.layout.wishlist_item,parent,false));
        }
        public int getItemCount(){return visible.size();}
        public void onBindViewHolder(@NonNull WishHolder h,int position){
            WishItem wish=visible.get(position);h.title.setText(wish.getTitle());h.author.setText(wish.getAuthor());h.author.setVisibility(wish.getAuthor()==null || wish.getAuthor().isEmpty()?View.GONE:View.VISIBLE);
            h.isbn.setText(wish.getIsbn()==null || wish.getIsbn().isEmpty()?getString(R.string.isbn_not_recorded):getString(R.string.edition_isbn,wish.getIsbn()));
            WishlistMatches.Match match=WishlistMatches.owned(wish,owned);
            h.owned.setVisibility(match==WishlistMatches.Match.NONE?View.GONE:View.VISIBLE);
            h.owned.setText(match==WishlistMatches.Match.ISBN?getString(R.string.wish_owned_isbn):match==WishlistMatches.Match.SIMILAR_TITLE?getString(R.string.wish_owned_similar):"");
            h.add.setOnClickListener(v->{Intent add=new Intent(WishlistActivity.this,AdminActivity.class);
                add.putExtra("wishlistTitle",wish.getTitle());add.putExtra("wishlistAuthor",wish.getAuthor());add.putExtra("wishlistIsbn",wish.getIsbn());startActivity(add);});
            h.remove.setEnabled(true);
            h.remove.setOnClickListener(v->new AlertDialog.Builder(WishlistActivity.this).setMessage(R.string.wish_remove_confirm)
                    .setNegativeButton(R.string.cancel_action,null).setPositiveButton(R.string.wish_remove,(d,w)->{
                        h.remove.setEnabled(false);new WishlistRepository().remove(wish.getId()).addOnSuccessListener(ignored->load())
                                .addOnFailureListener(e->{h.remove.setEnabled(true);Toast.makeText(WishlistActivity.this,R.string.wish_remove_failed,Toast.LENGTH_LONG).show();});
                    }).show());
        }
    }
    private static class WishHolder extends RecyclerView.ViewHolder {
        final TextView title,author,isbn,owned;final Button add,remove;
        WishHolder(View view){super(view);title=view.findViewById(R.id.wishTitle);author=view.findViewById(R.id.wishAuthor);isbn=view.findViewById(R.id.wishIsbn);owned=view.findViewById(R.id.wishOwned);add=view.findViewById(R.id.wishAddToLibrary);remove=view.findViewById(R.id.wishRemove);}
    }
}
