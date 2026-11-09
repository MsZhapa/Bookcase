package com.example.bookcase;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class EditionGroupsTest {
    private Book book(String id,String title,String author,String isbn,int cube,long position){
        Book book=new Book(id,title,author,"","",false,0,cube);book.setIsbn(isbn);book.setPosition(position);return book;
    }
    @Test public void groupsExplicitEditionsWithoutMergingOrMovingCopies(){
        Book a=book("a","Interaction Design 2nd Edition","Sharp, Rogers, and Preece",null,2,8);
        Book b=book("b","Interaction Design 5th Edition","Sharp, Rogers, and Preece",null,1,3);
        List<Book> input=Arrays.asList(a,b);List<EditionGroups.Group> groups=EditionGroups.group(input);
        assertEquals(1,groups.size());assertEquals("Interaction Design",groups.get(0).title);
        assertEquals(Arrays.asList(b,a),groups.get(0).copies);assertSame(a,input.get(0));
        assertEquals(8,a.getPosition());assertEquals(2,a.getLocation());assertEquals("Interaction Design 2nd Edition",a.getTitle());
    }
    @Test public void sameTitleDifferentAuthorsAndMissingMetadataStaySeparate(){
        List<Book> books=Arrays.asList(book("a","Same","First",null,1,0),book("b","Same","Second",null,1,0),
                book("c",null,null,null,1,0),book("d",null,null,null,1,0));
        assertEquals(4,EditionGroups.group(books).size());
    }
    @Test public void canonicalIsbnsCountEditionsAndMissingIsbnsAreNotInvented(){
        List<Book> books=Arrays.asList(book("a","Same","Author","0306406152",1,1),
                book("b"," SAME ","author","978-0-306-40615-7",1,2),book("c","Same","Author",null,1,3));
        EditionGroups.Group group=EditionGroups.group(books).get(0);
        assertEquals(3,group.copies.size());assertEquals(1,group.isbns.size());assertEquals(1,group.withoutIsbn);
    }
    @Test public void onlyTrailingExplicitEditionLabelsAreRemoved(){
        assertEquals("Book",EditionGroups.baseTitle("Book (2nd edition)"));
        assertEquals("Book",EditionGroups.baseTitle("Book edition 5"));
        assertEquals("The Edition of Dreams",EditionGroups.baseTitle("The Edition of Dreams"));
        assertEquals("1984",EditionGroups.baseTitle("1984"));
    }
}
