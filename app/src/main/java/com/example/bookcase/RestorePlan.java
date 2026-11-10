package com.example.bookcase;

import java.util.*;

/** Pure preview. Matching ISBNs are information, never grounds to merge physical copies. */
public final class RestorePlan {
    public final List<String> missing=new ArrayList<>(), conflicts=new ArrayList<>();
    public final long[] counters=new long[8];
    public final String currentFingerprint;
    public int matching, additionalIsbnCopies, counterChanges;
    public RestorePlan(LibraryBackup backup,LibraryBackup current) {
        backup.validate(); currentFingerprint=BackupJson.fingerprint(current);
        Map<String,String> occupied=new HashMap<>(); Set<String> currentIsbns=new HashSet<>();
        for(String source:Arrays.asList("books","bookCopies")) for(Map.Entry<String,Map<String,Object>> doc:current.collections.get(source).entrySet()) {
            String slot=slot(source,doc.getValue()); if(slot!=null) occupied.put(slot,source+"/"+doc.getKey());
            String isbn=isbn(source,doc.getValue(),current); if(!isbn.isEmpty()) currentIsbns.add(isbn);
        }
        for(int i=0;i<8;i++) {
            Map<String,Object> a=current.get("cubePositions",""+(i+1)), b=backup.get("cubePositions",""+(i+1));
            counters[i]=Math.max(a==null ? 0 : LibraryBackup.number(a.get("lastPosition"),0),b==null ? 0 : LibraryBackup.number(b.get("lastPosition"),0));
        }
        for(String source:LibraryBackup.COLLECTIONS) {
            if(source.equals("cubePositions")) continue;
            for(Map.Entry<String,Map<String,Object>> doc:backup.collections.get(source).entrySet()) {
                String key=source+"/"+doc.getKey(); Map<String,Object> old=current.get(source,doc.getKey()), fields=doc.getValue();
                if(old!=null) { if(BackupJson.same(old,fields)) matching++; else conflicts.add(key+": existing record differs"); continue; }
                missing.add(key);
                if(source.equals("books") || source.equals("bookCopies")) {
                    if(current.get(source.equals("books") ? "bookCopies" : "books",doc.getKey())!=null) conflicts.add(key+": copy ID exists in the other library format");
                    String slot=slot(source,fields), owner=slot==null ? null : occupied.putIfAbsent(slot,key);
                    if(owner!=null && !owner.equals(key)) conflicts.add(key+": shelf position is already occupied");
                    String isbn=isbn(source,fields,backup); if(!isbn.isEmpty() && !currentIsbns.add(isbn)) additionalIsbnCopies++;
                }
            }
        }
        for(LibraryBackup data:Arrays.asList(current,backup)) for(String source:Arrays.asList("books","bookCopies")) for(Map<String,Object> fields:data.collections.get(source).values()) {
            int cube=(int)LibraryBackup.number(fields.get(source.equals("books") ? "location" : "cubeNumber"),0);
            if(cube>=1 && cube<=8) counters[cube-1]=Math.max(counters[cube-1],LibraryBackup.number(fields.get("position"),0));
        }
        for(int i=0;i<8;i++) {
            LibraryBackup.require(counters[i]<Long.MAX_VALUE,"Shelf counter cannot be advanced safely");
            Map<String,Object> counter=current.get("cubePositions",""+(i+1));
            if(counters[i]>(counter==null ? 0 : LibraryBackup.number(counter.get("lastPosition"),0))) counterChanges++;
        }
    }
    public boolean canRestore() { return conflicts.isEmpty(); }
    private static String slot(String source,Map<String,Object> fields) {
        long position=LibraryBackup.number(fields.get("position"),0); if(position<=0) return null;
        return LibraryBackup.number(fields.get(source.equals("books") ? "location" : "cubeNumber"),0)+":"+position;
    }
    private static String isbn(String source,Map<String,Object> fields,LibraryBackup data) {
        if(source.equals("bookCopies")) fields=data.get("editions",LibraryBackup.text(fields.get("editionId")));
        if(fields==null) return "";
        try { return Isbn.normalize(LibraryBackup.text(fields.get("isbn"))); } catch(IllegalArgumentException ignored) { return ""; }
    }
    public String describe(LibraryBackup backup) {
        StringBuilder text=new StringBuilder("Backup contains "+backup.copies()+" physical copies, "+backup.collections.get("wishlist").size()+(backup.collections.get("wishlist").size()==1 ? " wishlist entry and " : " wishlist entries and ")+backup.collections.get("lendingRecords").size()+(backup.collections.get("lendingRecords").size()==1 ? " lending record." : " lending records.")+"\n\n");
        text.append(missing.size()).append(" missing records to add\n").append(matching).append(" matching records to leave unchanged\n").append(conflicts.size()).append(" conflicts\n");
        if(counterChanges>0) text.append("\nShelf order will be reserved in ").append(counterChanges).append(" cubes to protect restored positions.\n");
        if(additionalIsbnCopies>0) text.append("\n").append(additionalIsbnCopies).append(" additional copies share an ISBN. They stay separate physical copies.\n");
        text.append("\nExisting records will never be overwritten or deleted. Unknown shelf positions stay unknown. Cover links are included; image files are not.\n");
        if(!conflicts.isEmpty()) { text.append("\nRestore is blocked. Resolve these differences before importing:\n"); for(int i=0;i<Math.min(10,conflicts.size());i++) text.append("• ").append(conflicts.get(i)).append('\n'); }
        return text.toString();
    }
}
