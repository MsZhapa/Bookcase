package com.example.bookcase;
import org.junit.Test;
import java.io.StringWriter;
import java.io.Writer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class LibraryJsonTest {
    @Test public void preservesPhysicalCopiesMetadataAndUnknownPositions() throws Exception {
        Book a=new Book("a", "A \"title\"\n", "Ø Author", "Text\\body", "https://example.test/cover", true, 100, 3);
        a.setNotes("Personal note\nØ quote");
        a.setDueDate(new com.google.firebase.Timestamp(new java.util.Date(12345)));
        a.setIsbn("0306406152"); a.setPageCount(123); a.setPosition(42);
        a.setLendingStatus("LENT"); a.setBorrowerName("Friend"); a.setNormalizedCopy(true);
        Book b=new Book("b",a.getTitle(),a.getAuthor(),null,null,false,0,1);
        StringWriter out=new StringWriter(); LibraryJson.write(out,Arrays.asList(a,b),1000);
        String json=out.toString();
        assertTrue(json.contains("\"copyCount\":2"));
        assertTrue(json.contains("\"sourceCollection\":\"bookCopies\""));
        assertTrue(json.contains("\"sourceCollection\":\"books\""));
        assertTrue(json.contains("\"isbn\":\"0306406152\""));
        assertTrue(json.contains("\"position\":42"));
        assertTrue(json.contains("\"position\":0"));
        assertTrue(json.contains("\"borrowerName\":\"Friend\""));
        assertTrue(json.contains("\"description\":null"));
        assertTrue(json.contains("Ø Author"));
        assertTrue(json.contains("\"notes\":\"Personal note\\nØ quote\""));
        assertTrue(json.contains("\"dueDateEpochMillis\":12345"));
        assertTrue(json.indexOf("\"id\":\"a\"")<json.indexOf("\"id\":\"b\""));
    }
    @Test public void escapesJsonStringsAndHandlesEmptyCatalogue() throws Exception {
        assertEquals("\"a\\\"\\\\\\n\\r\\t\\u0001\"",LibraryJson.quote("a\"\\\n\r\t\u0001"));
        assertEquals("null",LibraryJson.quote(null));
        assertEquals("\"\\ud83d\\udcda\"",LibraryJson.quote("📚"));
        StringWriter out=new StringWriter(); LibraryJson.write(out,Collections.emptyList(),5);
        assertEquals("{\"formatVersion\":1,\"exportedAtEpochMillis\":5,\"copyCount\":0,\"copies\":[]}\n",out.toString());
    }
    @Test(expected=IOException.class) public void outputFailuresAreNotReportedAsSuccess() throws Exception {
        LibraryJson.write(new Writer() {
            public void write(char[] c,int o,int n) throws IOException {throw new IOException("Disk full");}
            public void flush() {} public void close() {}
        }, Collections.emptyList(),0);
    }
}
