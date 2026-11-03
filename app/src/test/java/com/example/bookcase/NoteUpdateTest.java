package com.example.bookcase;
import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;
public class NoteUpdateTest {
    @Test public void updateContainsOnlyNotesAndPreservesExactDraft() {
        String draft="  My thoughts\nØ Quotes 📚  ";
        Map<String,Object> update=NoteUpdate.fields(null,"",draft);
        assertEquals(1,update.size()); assertEquals(draft,update.get("notes"));
        assertFalse(update.containsKey("position")); assertFalse(update.containsKey("editionId"));
        assertEquals("",NoteUpdate.fields("previous","previous","").get("notes"));
    }
    @Test(expected=IllegalStateException.class) public void rejectsStaleNoteRatherThanOverwritingAnotherEdit() {
        NoteUpdate.fields("newer note","old note","my draft");
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsTooLongNote() {
        NoteUpdate.fields("","",new String(new char[20001]));
    }
    @Test public void modelDefaultsAreSafeAndCopiesHaveIndependentNotes() {
        BookCopy a=new BookCopy(),b=new BookCopy(); assertEquals("",a.getNotes());
        a.setNotes("copy a"); assertEquals("",b.getNotes());
        Book book=new Book();book.setNotes(null);assertEquals("",book.getNotes());
    }
}
