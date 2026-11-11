package com.example.bookcase;
import org.junit.Test;
import static org.junit.Assert.*;
public class CatalogueIdentityTest {
    @Test public void editionsShareStableWorkIdentity() { assertEquals(CatalogueIdentity.workId("Book 2nd Edition","Author","one"),CatalogueIdentity.workId("Book 5th Edition","AUTHOR","two")); }
    @Test public void missingAuthorsStaySeparate() { assertNotEquals(CatalogueIdentity.workId("Book",null,"one"),CatalogueIdentity.workId("Book",null,"two")); }
    @Test public void differentAuthorsStaySeparate() { assertNotEquals(CatalogueIdentity.workId("Book","A","one"),CatalogueIdentity.workId("Book","B","two")); }
    @Test public void signedOutAndWrongIdentityCannotResumePrivateScreens() { assertFalse(CatalogueIdentity.session(true,"owner",null,true,true)); assertFalse(CatalogueIdentity.session(true,"owner","other",true,true)); }
    @Test public void passwordAndUnverifiedSessionsCannotResumePrivateScreens() { assertFalse(CatalogueIdentity.session(true,"owner","owner",true,false)); assertFalse(CatalogueIdentity.session(true,"owner","owner",false,true)); }
    @Test public void verifiedOwnerAndOriginalLibrarySessionsWork() { assertTrue(CatalogueIdentity.session(true,"owner","owner",true,true)); assertTrue(CatalogueIdentity.session(false,null,null,false,false)); }
}
