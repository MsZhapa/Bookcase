package com.example.bookcase;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class CubeLayoutTest {
    @Test public void allEightDefaultsRemainAndNamesAreCleaned(){
        for(int i=1;i<=8;i++)assertEquals("Cube "+i,CubeLayout.label("",i));
        assertEquals("Study shelf",CubeLayout.label(" Study   shelf ",1));
        CubeLayout.validate(new String[8],1);CubeLayout.validate(new String[8],4);
    }
    @Test public void countsPhysicalCopiesIncludingLentBooksWithoutChangingLocations(){
        Book a=new Book("a","Same","","","",false,0,1),b=new Book("b","Same","","","",false,0,1),
                c=new Book("c","Other","","","",false,0,8),invalid=new Book();
        b.setLendingStatus("LENT");a.setPosition(42);
        int[] counts=CubeLayout.counts(Arrays.asList(a,b,c,invalid));
        assertEquals(2,counts[0]);assertEquals(1,counts[7]);assertEquals(3,Arrays.stream(counts).sum());
        assertEquals(42,a.getPosition());assertEquals(1,b.getLocation());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsUnsupportedGrid(){CubeLayout.validate(new String[8],5);}
    @Test(expected=IllegalArgumentException.class) public void rejectsMissingDefaultCubes(){CubeLayout.validate(new String[7],2);}
    @Test(expected=IllegalArgumentException.class) public void rejectsOversizedName(){CubeLayout.label("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",1);}
}
