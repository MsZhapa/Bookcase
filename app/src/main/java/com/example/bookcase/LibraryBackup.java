package com.example.bookcase;

import java.util.*;

/** Complete app records, kept separate from the older flattened catalogue export. */
public final class LibraryBackup {
    public static final List<String> COLLECTIONS = Collections.unmodifiableList(Arrays.asList(
            "works", "editions", "lendingRecords", "books", "bookCopies", "wishlist", "cubePositions"));
    public final Map<String, Map<String, Map<String,Object>>> collections = new TreeMap<>();
    public String[] cubeNames = new String[8];
    public int columns = 2;
    public long exportedAt;
    public LibraryBackup() { for (String name : COLLECTIONS) collections.put(name, new TreeMap<>()); for(int i=0;i<8;i++) cubeNames[i]="Cube "+(i+1); }
    public Map<String,Object> get(String collection, String id) { return collections.get(collection).get(id); }
    public int copies() { return collections.get("books").size()+collections.get("bookCopies").size(); }
    public int documents() { int count=0; for (Map<?,?> docs:collections.values()) count+=docs.size(); return count; }
    static void require(boolean condition, String reason) { if (!condition) throw new IllegalArgumentException(reason); }
    static String text(Object value) { return value instanceof String ? (String)value : ""; }
    static long number(Object value, long fallback) {
        if (value==null) return fallback;
        require(value instanceof Long || value instanceof Integer, "Expected a whole number");
        return ((Number)value).longValue();
    }
    static void id(String value) {
        require(value!=null && !value.isEmpty() && !value.contains("/") && !value.equals(".") && !value.equals("..")
                && !value.matches("__.*__") && value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=1500, "Invalid record ID");
    }
    public void validate() {
        require(collections.keySet().equals(new HashSet<>(COLLECTIONS)), "Unsupported backup collections");
        require(documents()<=10000, "Backup exceeds 10,000 records");
        CubeLayout.validate(cubeNames,columns);
        for(String copyId:collections.get("books").keySet()) require(!collections.get("bookCopies").containsKey(copyId),"Copy ID exists in both library formats");
        for (String collection:COLLECTIONS) for (Map.Entry<String,Map<String,Object>> entry:collections.get(collection).entrySet()) {
            id(entry.getKey()); Map<String,Object> fields=entry.getValue();
            require(fields!=null, "Missing record fields");
            for(String name:Arrays.asList("id","title","author","isbn","description","imageUrl","notes","lendingStatus","borrowerName","activeLoanId","editionId","workId","bookId","bookCopyId","status"))
                if(fields.get(name)!=null) require(fields.get(name) instanceof String,"Invalid text field: "+name);
            for(String name:Arrays.asList("lentDate","dueDate","returnedDate"))
                if(fields.get(name)!=null) require(fields.get(name) instanceof com.google.firebase.Timestamp,"Invalid date field");
            for(String name:Arrays.asList("pageCount","currentPage","readingPageCount"))
                if(fields.get(name)!=null) require(number(fields.get(name),0)>=0 && number(fields.get(name),0)<=Integer.MAX_VALUE,"Invalid page number");
            if (collection.equals("books") || collection.equals("bookCopies")) {
                long cube=number(fields.get(collection.equals("books") ? "location" : "cubeNumber"),0);
                require(cube>=1 && cube<=8, "A copy has an invalid cube");
                require(number(fields.get("position"),0)>=0, "A copy has a negative position");
                long progress=number(fields.get("progress"),0);
                require(progress>=0 && progress<=100, "Invalid reading percentage");
                require(number(fields.get("currentPage"),0)>=0 && number(fields.get("readingPageCount"),0)>=0, "Invalid reading pages");
                if (fields.get("currentPage")!=null && fields.get("readingPageCount")!=null)
                    require(number(fields.get("currentPage"),0)<=number(fields.get("readingPageCount"),0), "Page exceeds total pages");
                if (fields.containsKey("read")) require(fields.get("read") instanceof Boolean, "Invalid reading status");
                if (collection.equals("bookCopies")) require(get("editions",text(fields.get("editionId")))!=null, "A copy is missing its edition");
                String loan=text(fields.get("activeLoanId"));
                if (!loan.isEmpty()) {
                    Map<String,Object> record=get("lendingRecords",loan);
                    require(record!=null && entry.getKey().equals(record.get("bookId")), "A copy is missing its active loan");
                }
            }
            if (collection.equals("editions")) require(get("works",text(fields.get("workId")))!=null, "An edition is missing its work");
            if(fields.get("createdAt")!=null) require(number(fields.get("createdAt"),0)>=0,"Invalid creation date");
            if (collection.equals("wishlist")) {
                WishItem expected=WishItem.create(text(fields.get("title")),text(fields.get("author")),text(fields.get("isbn")));
                require(entry.getKey().equals(expected.getId()), "Invalid wishlist identity");
            }
            if (collection.equals("cubePositions")) {
                require(entry.getKey().matches("[1-8]"), "Invalid cube counter");
                require(number(fields.get("lastPosition"),0)>=0 && number(fields.get("lastPosition"),0)<Long.MAX_VALUE, "Invalid cube position counter");
            }
        }
    }
}
