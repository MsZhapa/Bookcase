package com.example.bookcase;

import java.util.*;

/** Deterministic conversion into a separate database; never mutates its source snapshot. */
public final class MigrationPlan {
    public final LibraryBackup source, target;
    public final String sourceFingerprint;
    public final List<String> blockers=new ArrayList<>();
    public final Set<String> groupedWorks=new TreeSet<>();
    public int unknownPositions, missingIsbns, uncertainWorks;
    public MigrationPlan(LibraryBackup source) {
        source.validate(); this.source=source; sourceFingerprint=BackupJson.fingerprint(source);
        target=new LibraryBackup(); target.exportedAt=source.exportedAt;
        target.cubeNames=source.cubeNames.clone(); target.columns=source.columns;
        for(String collection:Arrays.asList("wishlist","lendingRecords","cubePositions"))
            for(Map.Entry<String,Map<String,Object>> doc:source.collections.get(collection).entrySet()) target.collections.get(collection).put(doc.getKey(),new TreeMap<>(doc.getValue()));
        Set<String> usedEditions=new HashSet<>(),usedWorks=new HashSet<>();
        Map<String,Integer> workCopyCounts=new HashMap<>();
        for(String collection:Arrays.asList("books","bookCopies")) for(Map.Entry<String,Map<String,Object>> entry:source.collections.get(collection).entrySet()) {
            String id=entry.getKey(), sourceKey=collection+"/"+id; Map<String,Object> original=entry.getValue();
            Map<String,Object> edition=original,oldWork=null;
            if(collection.equals("bookCopies")) {
                String oldEdition=LibraryBackup.text(original.get("editionId")); usedEditions.add(oldEdition);
                edition=source.get("editions",oldEdition); String oldWorkId=LibraryBackup.text(edition.get("workId"));
                usedWorks.add(oldWorkId); oldWork=source.get("works",oldWorkId);
            }
            String title=LibraryBackup.text(edition.get("title")),author=LibraryBackup.text(edition.get("author"));
            String foldedTitle=CatalogueKeys.fold(EditionGroups.baseTitle(title)),foldedAuthor=CatalogueKeys.fold(author);
            boolean uncertain=foldedTitle.isEmpty() || foldedAuthor.isEmpty();
            String workId=CatalogueIdentity.workId(title,author,id);
            if(uncertain) uncertainWorks++;
            String isbn="";
            try { isbn=Isbn.normalize(LibraryBackup.text(edition.get("isbn"))); }
            catch(IllegalArgumentException invalid) { blockers.add(sourceKey+": invalid ISBN needs review"); }
            if(isbn.isEmpty()) missingIsbns++;
            Map<String,Object> metadata=new TreeMap<>();
            for(String field:Arrays.asList("title","author","description","imageUrl")) metadata.put(field,edition.get(field));
            metadata.put("isbn",isbn); metadata.put("pageCount",LibraryBackup.number(edition.get("pageCount"),0));
            String editionId=isbn.isEmpty() ? "edition-"+CatalogueKeys.hash(BackupJson.encode(metadata,0).toString()) : "isbn-"+isbn;
            // Unknown authors/titles must never accidentally share a catalogue identity.
            if(uncertain && isbn.isEmpty()) editionId="edition-"+CatalogueKeys.hash("copy:"+id);
            metadata.put("id",editionId); metadata.put("workId",workId);
            Map<String,Object> previous=target.get("editions",editionId);
            if(previous!=null && !BackupJson.same(previous,metadata)) blockers.add(sourceKey+": ISBN metadata differs; choose the correct edition details before migration");
            else target.collections.get("editions").put(editionId,metadata);
            Map<String,Object> work=target.get("works",workId);
            if(work==null) {
                work=new TreeMap<>(); work.put("id",workId); work.put("title",EditionGroups.baseTitle(title)); work.put("author",edition.get("author")); work.put("description",edition.get("description"));
                target.collections.get("works").put(workId,work);
            } else if(!Objects.equals(work.get("description"),edition.get("description"))) work.put("description",null);
            if(workCopyCounts.merge(workId,1,Integer::sum)>1) groupedWorks.add(EditionGroups.baseTitle(title)+" — "+author);
            Map<String,Object> copy=new TreeMap<>();
            if(collection.equals("bookCopies")) copy.putAll(original);
            else for(String field:Arrays.asList("lendingStatus","borrowerName","activeLoanId","lentDate","dueDate","notes","progress","currentPage","readingPageCount")) if(original.containsKey(field)) copy.put(field,original.get(field));
            copy.put("id",id); copy.put("editionId",editionId);
            long cube=LibraryBackup.number(original.get(collection.equals("books") ? "location" : "cubeNumber"),0),position=LibraryBackup.number(original.get("position"),0);
            copy.put("cubeNumber",cube); copy.put("position",position);
            copy.put("read",original.containsKey("read") ? original.get("read") : Boolean.TRUE.equals(original.get("isRead")));
            if(!copy.containsKey("progress")) copy.put("progress",0L);
            if(!copy.containsKey("lendingStatus")) copy.put("lendingStatus","AVAILABLE");
            if(position==0) unknownPositions++;
            // The exact original record is retained for audit/rollback, including unknown fields.
            if(!original.containsKey("migrationArchive")) {
                Map<String,Object> archive=new TreeMap<>(); archive.put("sourceCollection",collection); archive.put("sourceId",id); archive.put("fields",original);
                if(collection.equals("bookCopies")) { archive.put("edition",edition); archive.put("work",oldWork); }
                String archiveJson=BackupJson.encode(archive,0).toString();
                LibraryBackup.require(archiveJson.length()<=200000,"An original record is too large to archive safely");
                copy.put("migrationArchive",archiveJson);
            }
            target.collections.get("bookCopies").put(id,copy);
            String counterId=Long.toString(cube); Map<String,Object> counter=target.get("cubePositions",counterId);
            long old=counter==null ? 0 : LibraryBackup.number(counter.get("lastPosition"),0);
            if(position>old) { if(counter==null) { counter=new TreeMap<>(); target.collections.get("cubePositions").put(counterId,counter); } counter.put("lastPosition",position); }
        }
        for(String id:new ArrayList<>(target.collections.get("cubePositions").keySet())) {
            long cursor=LibraryBackup.number(target.get("cubePositions",id).get("lastPosition"),0);
            if(cursor==0) target.collections.get("cubePositions").remove(id);
            else target.collections.get("cubePositions").put(id,new TreeMap<>(Collections.singletonMap("lastPosition",cursor)));
        }
        if(usedEditions.size()!=source.collections.get("editions").size() || usedWorks.size()!=source.collections.get("works").size())
            blockers.add("Unlinked work or edition records need review; no catalogue records will be silently discarded");
        if(blockers.isEmpty()) {
            target.validate();
            RestorePlan shelfCheck=new RestorePlan(target,new LibraryBackup());
            blockers.addAll(shelfCheck.conflicts);
        }
        LibraryBackup.require(target.copies()==source.copies(),"Migration changed the physical-copy count");
    }
    public boolean ready() { return blockers.isEmpty(); }
    public String describe() {
        StringBuilder text=new StringBuilder("Current library: "+source.copies()+" physical copies.\n\nV2 copy:\n"+target.collections.get("works").size()+" works\n"+target.collections.get("editions").size()+" editions\n"+target.collections.get("bookCopies").size()+" separate physical copies\n"+target.collections.get("wishlist").size()+" wishlist entries\n"+target.collections.get("lendingRecords").size()+" lending history records\n\n");
        text.append(unknownPositions).append(" copies have no recorded shelf position. These stay unknown; no positions are guessed.\n");
        text.append(missingIsbns).append(" copies have no validated ISBN. They are not assumed to be the same edition.\n");
        if(uncertainWorks>0) text.append(uncertainWorks).append(" copies lack a title or author and stay separate.\n");
        text.append("\nCopy IDs, cube numbers, reading progress, notes and loans are retained. Original records are archived with each converted copy.\n");
        text.append("\nWorks group matching titles and authors, ignoring explicit numbered edition suffixes. Edition metadata stays separate. Review these groups:\n");
        if(groupedWorks.isEmpty()) text.append("No multi-copy groups.\n");
        for(String title:groupedWorks) text.append("• ").append(title).append('\n');
        if(!ready()) { text.append("\nMigration blocked:\n"); for(String reason:blockers) text.append("• ").append(reason).append('\n'); }
        text.append("\nThis prepares a separate V2 copy. Your current library stays active. Creating a copy does not switch databases or delete the original.");
        return text.toString();
    }
    public void verify(LibraryBackup actual) {
        LibraryBackup.require(ready(),"Migration needs review"); actual.validate();
        for(String collection:LibraryBackup.COLLECTIONS) {
            LibraryBackup.require(actual.collections.get(collection).keySet().equals(target.collections.get(collection).keySet()),"V2 record counts or IDs differ in "+collection);
            for(String id:target.collections.get(collection).keySet()) LibraryBackup.require(BackupJson.same(target.get(collection,id),actual.get(collection,id)),"V2 record verification failed: "+collection+"/"+id);
        }
    }
}
