package com.example.bookcase;
import android.content.Intent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;

public final class LibraryMenu {
    private LibraryMenu() {}
    public static void attach(AppCompatActivity screen) {
        View anchor = screen.findViewById(R.id.libraryMenuButton);
        anchor.setVisibility(View.VISIBLE);
        anchor.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(new ContextThemeWrapper(screen,
                    androidx.appcompat.R.style.Theme_AppCompat_Light), anchor);
            menu.inflate(R.menu.library_actions);
            menu.setOnMenuItemClickListener(item -> {
                Class<?> target;
                if (item.getItemId() == R.id.actionReadingOverview) target = ReadingOverviewActivity.class;
                else if (item.getItemId() == R.id.actionLoanOverview) target = LoansActivity.class;
                else if (item.getItemId() == R.id.actionAccount) target = AccountActivity.class;
                else if (item.getItemId() == R.id.actionRestoreLibrary) target = LibraryRestoreActivity.class;
                else if (item.getItemId() == R.id.actionExportLibrary) target = LibraryExportActivity.class;
                else if (item.getItemId() == R.id.actionBrowseEditions) target = EditionsActivity.class;
                else if (item.getItemId() == R.id.actionCustomizeCubes) target = CubeSettingsActivity.class;
                else if (item.getItemId() == R.id.actionWishlist) target = WishlistActivity.class;
                else return false;
                screen.startActivity(new Intent(screen, target)); return true;
            });
            menu.show();
        });
    }
}
