package com.example.bookcase;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;
public class EditionsActivity extends CatalogueToolActivity {
    private final List<Book> books=new ArrayList<>(), copies=new ArrayList<>();
    private final List<EditionGroups.Group> groups=new ArrayList<>();
    private String query="", groupKey;
    private boolean loaded;
    private BookAdapter copyAdapter;
    private GroupAdapter groupAdapter;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); title(R.string.browse_editions);
        groupKey=getIntent().getStringExtra("groupKey");
        if (state!=null) query=state.getString("query","");
        search(query,text->{ query=text; render(); });
        if (groupKey==null) { groupAdapter=new GroupAdapter(); list.setAdapter(groupAdapter); }
        else { copyAdapter=new BookAdapter(this,copies); copyAdapter.showEditions(); list.setAdapter(copyAdapter); }
        retry.setOnClickListener(v->load());
    }
    @Override protected void onResume() { super.onResume(); load(); }
    private void load() {
        loaded=false; retry.setVisibility(View.GONE); status.setText(R.string.loading_books);
        new BookRepository().getBooks(null).addOnSuccessListener(result->{ books.clear();books.addAll(result);loaded=true;render(); })
                .addOnFailureListener(e->{ status.setText(R.string.overview_failed);retry.setVisibility(View.VISIBLE); });
    }
    private void render() {
        if (!loaded) return;
        if (groupKey==null) {
            groups.clear(); groups.addAll(EditionGroups.group(BookSearch.filter(books,query)));
            groupAdapter.notifyDataSetChanged();
            status.setText(groups.isEmpty()?getString(R.string.no_matching_books):getString(R.string.edition_groups_count,groups.size()));
        } else {
            EditionGroups.Group selected=null;
            for (EditionGroups.Group group:EditionGroups.group(books)) if (group.key.equals(groupKey)) selected=group;
            copies.clear();
            if (selected!=null) {
                copies.addAll(BookSearch.filter(selected.copies,query));
                ((TextView)findViewById(R.id.screenTitle)).setText(selected.title);
            }
            copyAdapter.notifyDataSetChanged(); status.setText(copies.isEmpty()?getString(R.string.no_matching_books):getString(R.string.group_copies_count,copies.size()));
        }
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("query",query);super.onSaveInstanceState(state); }
    private class GroupAdapter extends RecyclerView.Adapter<GroupHolder> {
        @NonNull public GroupHolder onCreateViewHolder(@NonNull ViewGroup parent,int type) {
            return new GroupHolder(LayoutInflater.from(EditionsActivity.this).inflate(R.layout.edition_group_item,parent,false));
        }
        public int getItemCount() { return groups.size(); }
        public void onBindViewHolder(@NonNull GroupHolder h,int position) {
            EditionGroups.Group group=groups.get(position);
            h.title.setText(group.title);h.author.setText(group.author);
            String text=getString(R.string.group_copies_count,group.copies.size());
            text+=group.isbns.isEmpty()?" · "+getString(R.string.isbn_not_recorded):" · "+getString(R.string.known_isbn_editions,group.isbns.size());
            if (!group.isbns.isEmpty() && group.withoutIsbn>0) text+=" · "+getString(R.string.copies_without_isbn,group.withoutIsbn);
            h.meta.setText(text);
            h.itemView.setOnClickListener(v->{ Intent intent=new Intent(EditionsActivity.this,EditionsActivity.class);intent.putExtra("groupKey",group.key);startActivity(intent); });
        }
    }
    private static class GroupHolder extends RecyclerView.ViewHolder {
        final TextView title,author,meta;
        GroupHolder(View view) { super(view);title=view.findViewById(R.id.groupTitle);author=view.findViewById(R.id.groupAuthor);meta=view.findViewById(R.id.groupMeta); }
    }
}
