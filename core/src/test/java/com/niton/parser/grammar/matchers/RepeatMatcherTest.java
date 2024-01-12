package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReferenceMap;
import com.niton.parser.token.ListTokenStream;
import com.niton.parser.token.Location;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer.AssignedToken;
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
        var matcher = new RepeatMatcher(subGrammar, minimum);
        when(subGrammar.parse(any(), any())).thenThrow(new ParsingException("", "", 0, 0, 0));
        var result = matcher.parse(new ListTokenStream(List.of()), new GrammarReferenceMap());
        assertEquals(0, result.subNodes.size());
    }

    @Test
    void testWorking()  {
        var subGrammar = mock(Grammar.class);
        var matcher = new RepeatMatcher(subGrammar, minimum);
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