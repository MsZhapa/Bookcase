package com.example.bookcase;
import org.junit.Test;
import static org.junit.Assert.*;
public class CatalogueIdentityTest {
    @Test public void editionsShareStableWorkIdentity() { assertEquals(CatalogueIdentity.workId("Book 2nd Edition","Author","one"),CatalogueIdentity.workId("Book 5th Edition","AUTHOR","two")); }
    @Test public void missingAuthorsStaySeparate() { assertNotEquals(CatalogueIdentity.workId("Book",null,"one"),CatalogueIdentity.workId("Book",null,"two")); }
    @Test public void differentAuthorsStaySeparate() { assertNotEquals(CatalogueIdentity.workId("Book","A","one"),CatalogueIdentity.workId("Book","B","two")); }
}
