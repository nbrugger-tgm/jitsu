package com.niton.parser.tests;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.TokenSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

public class TokenSourceTest {
    @Test
    public void smallChunkTest() throws IOException, ParsingException {
        StringReader reader = new StringReader("...WORT WORT98764ZAHL");
        TokenSource source = new TokenSource(reader);
        assertEquals("", source.getBuffer());
        source.setChunkSize(1);
        assertEquals("", source.getBuffer());
        int i;
        for (i = 0;i<source.size();i++){
            System.out.println(source.get(i));
        }
        assertEquals(8, i);
        assertEquals(source.get(5).value, "WORT");
    }
    @Test
    public void avgChunkTest() throws IOException, ParsingException {
        StringReader reader = new StringReader("...WORT WORT98764ZAHL");
        TokenSource source = new TokenSource(reader);
        assertEquals("", source.getBuffer());
        source.setChunkSize(8);
        assertEquals("", source.getBuffer());
        int i;
        for (i = 0;i<source.size();i++){
            System.out.println(source.get(i));
        }
        assertEquals(8, i);
        assertEquals(source.get(5).value, "WORT");
    }
    @Test
    public void bigChunkTest() throws IOException, ParsingException {
        StringReader reader = new StringReader("...WORT WORT98764ZAHL");
        TokenSource source = new TokenSource(reader);
        assertEquals("", source.getBuffer());
        int i;
        for (i = 0;i<source.size();i++){
            System.out.println(source.get(i));
        }
        assertEquals(8, i);
        assertEquals(source.get(5).value, "WORT");
    }
}
