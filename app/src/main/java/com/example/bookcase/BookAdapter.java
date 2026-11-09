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
    private boolean showLoanDetails;
    private boolean showEditionDetails;
    public void showEditions() { showEditionDetails=true; }

    public BookAdapter(Context context, List<Book> bookList) {
        this.context = context;
        this.bookList = bookList;
    }

    public BookAdapter(Context context, List<Book> books, boolean showLoanDetails) {
        this(context, books); this.showLoanDetails = showLoanDetails;
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

        if ("LENT".equals(book.getLendingStatus())) {
            holder.lendingStatusTextView.setVisibility(View.VISIBLE);
        } else {
            holder.lendingStatusTextView.setVisibility(View.GONE);
        }

        // Ensure TextView is not null before setting text
        if (holder.titleTextView != null) {
            holder.titleTextView.setText(book.getTitle());
        }
        if (holder.authorTextView != null) {
            holder.authorTextView.setText("by " + book.getAuthor());
        }

        holder.locationTextView.setText(CubeSettings.location(context,book.getLocation(),book.getPosition()));
        holder.readStatusTextView.setText(book.getCurrentPage()!=null ? PageProgressForm.describe(context,book)
                : BookFilters.finished(book) ? context.getString(R.string.status_finished)
                : book.getProgress()>0 ? context.getString(R.string.status_progress,book.getProgress())
                : context.getString(R.string.status_unread));

        holder.loanInfoTextView.setVisibility(showLoanDetails ? View.VISIBLE : View.GONE);
        if (showLoanDetails) {
            String borrower = book.getBorrowerName();
            if (borrower == null || borrower.trim().isEmpty()) borrower = context.getString(R.string.borrower_unknown);
            long now = System.currentTimeMillis();
            java.util.TimeZone zone = java.util.TimeZone.getDefault();
            boolean overdue = LoanRules.overdue(book, now, zone);
            String due = book.getDueDate() == null ? context.getString(R.string.no_due_date)
                    : context.getString(R.string.loan_due,
                            android.text.format.DateFormat.getDateFormat(context).format(book.getDueDate().toDate()));
            if (overdue) due += " · " + context.getString(R.string.loan_overdue);
            else if (LoanRules.dueToday(book, now, zone)) due += " · " + context.getString(R.string.loan_due_today);
            holder.loanInfoTextView.setText(context.getString(R.string.loan_borrower, borrower) + "\n" + due);
            holder.loanInfoTextView.setTextColor(android.graphics.Color.parseColor(overdue ? "#B71C1C" : "#424242"));
        }

        holder.editionInfoTextView.setVisibility(showEditionDetails ? View.VISIBLE : View.GONE);
        holder.editionInfoTextView.setText(book.getIsbn()==null || book.getIsbn().isEmpty()
                ? context.getString(R.string.isbn_not_recorded) : context.getString(R.string.edition_isbn,book.getIsbn()));
        // Load image using Glide
        if (holder.bookImageView != null) {
            Glide.with(context)
                    .load(book.getImageUrl())
                    .placeholder(R.drawable.books1)
                    .into(holder.bookImageView);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("normalizedCopy", book.isNormalizedCopy());
            intent.putExtra("bookId", book.getId()); // ✅ Pass correct book ID
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, authorTextView, lendingStatusTextView, locationTextView, readStatusTextView, loanInfoTextView, editionInfoTextView;
        ImageView bookImageView;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);

            // Ensure these IDs match those in item_book.xml
            editionInfoTextView = itemView.findViewById(R.id.editionInfoTextView);
            loanInfoTextView = itemView.findViewById(R.id.loanInfoTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            readStatusTextView = itemView.findViewById(R.id.readStatusTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            bookImageView = itemView.findViewById(R.id.bookImageView);
            lendingStatusTextView = itemView.findViewById(R.id.lendingStatusTextView);
        }
    }
}
