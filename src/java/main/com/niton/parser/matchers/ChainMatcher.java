package com.niton.parser.matchers;

import com.niton.parser.GrammarMatcher;
import com.niton.parser.token.TokenStream;
import com.niton.parser.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammars.ChainGrammar;
import com.niton.parser.Grammar;
import com.niton.parser.GrammarResult;
import com.niton.parser.result.SuperGrammarResult;

import java.util.List;

/**
 * Used to build a grammar<br>
 * Tests all Grammars in the chain after each other
 *
 * @author Nils
 * @version 2019-05-28
 */
public class ChainMatcher extends GrammarMatcher<SuperGrammarResult> {

    ChainGrammar chain;

    /**
     * Creates an Instance of ChainExecutor.java
     *
     * @param chain
     * @author Nils Brugger
     * @version 2019-06-08
     */
    public ChainMatcher(ChainGrammar chain) {
        this.chain = chain;
        setOriginGrammarName(chain.getName());
    }

    /**
     * @throws ParsingException
     * @see GrammarMatcher (java.util.List,GrammarReference)
     */
    @Override
    public SuperGrammarResult process(TokenStream tokens, GrammarReference reference) throws ParsingException {
        SuperGrammarResult gObject = new SuperGrammarResult();
        int i = 0;
        for (Grammar grammar : getChain()) {
            try{

                GrammarResult res = grammar.parse(tokens, reference);
                String name;
                if((name =chain.getNaming().get(i)) != null)
                    gObject.name(name, res);
                else
                    gObject.add(res);
                tokens.commit();
                i++;
            }catch (Exception e){
                tokens.rollback();
                throw e;
            }
        }
        return gObject;
    }

    /**
     * @return the chain
     */
    public List<Grammar> getChain() {
        return chain.getChain();
    }
}
