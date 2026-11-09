package com.example.bookcase;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class WishlistTest {
    @Test public void canonicalIsbnMakesConcurrentAddsTargetSameDocument(){
        WishItem a=WishItem.create("Book","Author","0-306-40615-2"),b=WishItem.create("Other metadata","Author","9780306406157");
        assertEquals(a.getId(),b.getId());assertEquals("9780306406157",a.getIsbn());
    }
    @Test public void titleOnlyKeysAreStableSafeAndKeepDifferentEditionsSeparate(){
        WishItem a=WishItem.create(" Design 2nd Edition "," AUTHOR ",""),b=WishItem.create("design 2nd edition","author",null),
                c=WishItem.create("Design 5th Edition","Author",null);
        assertEquals(a.getId(),b.getId());assertNotEquals(a.getId(),c.getId());assertTrue(a.getId().matches("title-[0-9a-f]{64}"));
    }
    @Test public void ownedMatchWarnsWithoutChangingOwnedBookOrWish(){
        Book owned=new Book("id","Book","Author","","",false,69,3);owned.setIsbn("0306406152");owned.setPosition(42);owned.setNotes("keep");
        WishItem wish=WishItem.create("Another title","","9780306406157");
        assertEquals(WishlistMatches.Match.ISBN,WishlistMatches.owned(wish,Arrays.asList(owned)));
        assertEquals(WishlistMatches.Match.SIMILAR_TITLE,WishlistMatches.owned(WishItem.create("book","author",null),Arrays.asList(owned)));
        assertEquals(WishlistMatches.Match.NONE,WishlistMatches.owned(WishItem.create("book","",null),Arrays.asList(owned)));
        assertEquals(42,owned.getPosition());assertEquals(69,owned.getProgress());assertEquals("keep",owned.getNotes());
    }
    @Test public void searchesTitleAuthorAndFormattedOrEquivalentIsbn(){
        WishItem wish=WishItem.create("The Book","Writer","9780306406157");
        for(String query:Arrays.asList("book"," WRITER ","978-0306","0306406152"))assertTrue(WishlistMatches.matches(wish,query));
        assertFalse(WishlistMatches.matches(wish,"unrelated"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsBlankTitle(){WishItem.create("  ","","");}
    @Test(expected=IllegalArgumentException.class) public void rejectsInvisibleTitle(){WishItem.create("\u00a0","","");}
    @Test(expected=IllegalArgumentException.class) public void rejectsInvalidIsbn(){WishItem.create("Book","Author","123");}
}
