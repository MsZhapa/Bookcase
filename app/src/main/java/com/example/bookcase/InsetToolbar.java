package com.example.bookcase;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

/** Android 15+ draws behind system bars; keep shared header controls below them. */
public class InsetToolbar extends LinearLayout {
    public InsetToolbar(Context context,AttributeSet attrs) {
        super(context,attrs);
        if(android.os.Build.VERSION.SDK_INT>=35) {
            final int left=getPaddingLeft(),top=getPaddingTop(),right=getPaddingRight(),bottom=getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(this,(view,insets)-> {
                Insets bars=insets.getInsets(WindowInsetsCompat.Type.statusBars()|WindowInsetsCompat.Type.displayCutout());
                view.setPadding(left+bars.left,top+bars.top,right+bars.right,bottom);
                android.view.View content=getRootView().findViewById(android.R.id.content);
                if(content!=null) {
                    int navigation=insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    content.setPadding(content.getPaddingLeft(),content.getPaddingTop(),content.getPaddingRight(),navigation);
                }
                return insets;
            });
        }
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); ViewCompat.requestApplyInsets(this); }
}
