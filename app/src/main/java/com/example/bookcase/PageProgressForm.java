package com.example.bookcase;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public final class PageProgressForm {
    private final EditText page, total;
    private final TextView preview;
    private final CheckBox finished;
    private Book original = new Book();
    public PageProgressForm(View root, CheckBox finished) {
        page=root.findViewById(R.id.currentPageInput); total=root.findViewById(R.id.totalPagesInput);
        preview=root.findViewById(R.id.calculatedProgress); this.finished=finished;
        TextWatcher watcher=new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { refresh(); }
            public void afterTextChanged(Editable value) {}
        };
        page.addTextChangedListener(watcher); total.addTextChangedListener(watcher);
        finished.setOnCheckedChangeListener((button, checked) -> refresh());
    }
    public void bind(Book book) {
        original=book;
        page.setText(book.getCurrentPage()==null ? "" : book.getCurrentPage().toString());
        total.setText(ReadingProgress.total(book)>0 ? ""+ReadingProgress.total(book) : "");
        finished.setChecked(BookFilters.finished(book)); refresh();
    }
    public void lookupTotal(int pages) { total.setText(pages>0 ? ""+pages : ""); }
    private Integer number(EditText input, boolean count) {
        String text=input.getText().toString().trim();
        if (text.isEmpty()) return null;
        try {
            int value=Integer.parseInt(text);
            if (value<0 || (count && value==0)) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException bad) {
            throw new IllegalArgumentException(count ? "Enter a positive total page count" : "Enter a valid page number");
        }
    }
    public ReadingProgress.State value() {
        return ReadingProgress.calculate(original, number(page,false), number(total,true), finished.isChecked());
    }
    public boolean apply(Book book) {
        try { value().apply(book); page.setError(null); total.setError(null); return true; }
        catch (IllegalArgumentException bad) { preview.setText(bad.getMessage()); page.setError(bad.getMessage()); return false; }
    }
    private void refresh() {
        page.setEnabled(!finished.isChecked());
        try {
            ReadingProgress.State state=value();
            if (state.page==null && !state.finished) preview.setText(original.getProgress()>0
                    ? preview.getContext().getString(R.string.legacy_percentage,original.getProgress())
                    : preview.getContext().getString(R.string.enter_current_page));
            else preview.setText(preview.getContext().getString(R.string.calculated_percentage,state.percent));
        } catch (IllegalArgumentException bad) { preview.setText(bad.getMessage()); }
    }
    public void save(Bundle state) {
        state.putString("pageDraft",page.getText().toString()); state.putString("totalDraft",total.getText().toString());
        state.putBoolean("finishedDraft",finished.isChecked());
    }
    public void restore(Bundle state) {
        if (state!=null && state.containsKey("pageDraft")) {
            page.setText(state.getString("pageDraft")); total.setText(state.getString("totalDraft"));
            finished.setChecked(state.getBoolean("finishedDraft")); refresh();
        }
    }
    public static String describe(Context context, Book book) {
        int percent=BookFilters.finished(book) ? 100 : book.getProgress();
        if (BookFilters.finished(book) && ReadingProgress.total(book)==0) return context.getString(R.string.finished_progress);
        if (book.getCurrentPage()!=null) {
            return ReadingProgress.total(book)>0 ? context.getString(R.string.page_status,
                    book.getCurrentPage(),ReadingProgress.total(book),percent)
                    : context.getString(R.string.page_status_unknown,book.getCurrentPage());
        }
        return context.getString(R.string.calculated_percentage,percent);
    }
}
