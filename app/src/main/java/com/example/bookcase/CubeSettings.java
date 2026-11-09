package com.example.bookcase;
import android.content.Context;
import android.content.SharedPreferences;
public final class CubeSettings {
    private CubeSettings() {}
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("cube_layout",Context.MODE_PRIVATE); }
    public static String name(Context c,int cube) {
        if (cube<1 || cube>8) return "Cube "+cube;
        return CubeLayout.label(prefs(c).getString("name"+cube,""),cube);
    }
    public static String numbered(Context c,int cube) {
        String name=name(c,cube), fallback="Cube "+cube;
        return name.equalsIgnoreCase(fallback) ? fallback : name+" ("+fallback+")";
    }
    public static String[] options(Context c) {
        String[] names=new String[8]; for (int i=0;i<8;i++) names[i]=numbered(c,i+1); return names;
    }
    public static int columns(Context c) { return Math.max(1,Math.min(4,prefs(c).getInt("columns",2))); }
    public static void save(Context c,String[] names,int columns) {
        CubeLayout.validate(names,columns); SharedPreferences.Editor edit=prefs(c).edit();
        for (int i=0;i<8;i++) edit.putString("name"+(i+1),CubeLayout.label(names[i],i+1));
        edit.putInt("columns",columns).apply();
    }
    public static String location(Context c,int cube,long position) {
        return c.getString(R.string.named_copy_location,numbered(c,cube),position>0
                ? c.getString(R.string.copy_position,position) : c.getString(R.string.position_unknown));
    }
    public static String copyLabel(Context c,String title,int cube,long position) { return title+"\n"+location(c,cube,position); }
}
