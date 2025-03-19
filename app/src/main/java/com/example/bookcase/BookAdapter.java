package com.example.bookcase;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> bookList;
    private Context context;

    public BookAdapter(Context context, List<Book> bookList) {
        this.context = context;
        this.bookList = bookList;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.book_item, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = bookList.get(position);

        // Ensure TextView is not null before setting text
        if (holder.titleTextView != null) {
            holder.titleTextView.setText(book.getTitle());
        }
        if (holder.authorTextView != null) {
            holder.authorTextView.setText("by " + book.getAuthor());
        }

        // Load image using Glide
        if (holder.bookImageView != null) {
            Glide.with(context)
                    .load(book.getImageUrl())
                    .placeholder(R.drawable.books1)
                    .into(holder.bookImageView);
        }

        // Open BookDetailActivity on click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("id", book.getId());
            intent.putExtra("title", book.getTitle());
            intent.putExtra("author", book.getAuthor());
            intent.putExtra("description", book.getDescription());
            intent.putExtra("imageUrl", book.getImageUrl());
            intent.putExtra("location", book.getLocation());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, authorTextView;
        ImageView bookImageView;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);

            // Ensure these IDs match those in item_book.xml
            titleTextView = itemView.findViewById(R.id.titleTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            bookImageView = itemView.findViewById(R.id.bookImageView);
        }
    }
}
