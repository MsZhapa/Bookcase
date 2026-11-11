package com.example.bookcase;
/** Stable work identity shared by migration, additions and edition edits. */
public final class CatalogueIdentity {
    private CatalogueIdentity() {}
    public static String workId(String title,String author,String copyId) {
        String base=CatalogueKeys.fold(EditionGroups.baseTitle(title)),writer=CatalogueKeys.fold(author);
        return "work-"+CatalogueKeys.hash(base.isEmpty() || writer.isEmpty() ? "copy:"+copyId : base+"\u0000"+writer);
    }
}
