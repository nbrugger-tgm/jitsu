package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReferenceMap;
import com.niton.jainparse.token.ListTokenStream;
import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RepeatMatcherTest {
    private final static Location ANY_LOCATION = Location.of(0, 0, 0, 0);

    @Test
    void testInstantFail()  {
        var subGrammar = mock(Grammar.class);
        var matcher = new RepeatMatcher(subGrammar, 0);
        when(subGrammar.parse(any(), any())).thenReturn(ParsingResult.error(new ParsingException("", "", Location.oneChar(0, 0))));
        var result = matcher.parse(new ListTokenStream(List.of()), new GrammarReferenceMap());
        assertEquals(0, result.unwrap().subNodes.size());
    }

    @Test
    void testWorking()  {
        var subGrammar = mock(Grammar.class);
        var matcher = new RepeatMatcher(subGrammar, 0);
        var tokenResult = new TokenNode(List.of(new AssignedToken("NUMBER", "1")), ANY_LOCATION);
        var tokenResult2 = new TokenNode(List.of(new AssignedToken("LETTERS", "ABC")), ANY_LOCATION);
        var streamMock = mock(TokenStream.class);
        when(streamMock.index()).thenReturn(1, 2, 3, 4);
        when(subGrammar.parse(any(), any()))
                .thenReturn(tokenResult, tokenResult2)
                .thenThrow(new ParsingException("", "", 0, 0, 0));
        var result = matcher.parse(streamMock, new GrammarReferenceMap());
        assertEquals(2, result.subNodes.size());
        assertThat(result.subNodes)
                .containsExactly(tokenResult, tokenResult2);
        assertThat(result.naming)
                .as("indexes should be used as names")
                .containsKeys("0", "1");
    }
}