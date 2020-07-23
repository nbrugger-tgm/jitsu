package com.niton.parser.token;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Tokenizer.AssignedToken;

import java.util.List;
import java.util.Stack;

public class TokenStream {
    private List<AssignedToken> tokens;
    private final Stack<Integer> levelIndexes = new Stack<>();
    private int recursionLevelLimit = 500;

    public TokenStream(List<AssignedToken> tokens) {
        this.tokens = tokens;
        levelIndexes.push(0);
    }
    /**
     * Marks the next token as consumed
     */
    protected final void increase() {
        levelIndexes.set(levelIndexes.size()-1,index()+1);
    }

    /**
     * @return the index of the token you are at
     */
    public int index() {
        return levelIndexes.get(levelIndexes.size()-1);
    }

    /**
     * @return the index
     */
    public void index(int index) {
        levelIndexes.set(levelIndexes.size()-1,index);
    }
    /**
     * Returns the current marked assigned token and jumps one further
     */
    public AssignedToken next() throws ParsingException {
        try {
            AssignedToken tkn = tokens.get(index());
            increase();
            return tkn;
        }catch (IndexOutOfBoundsException e){
            throw new ParsingException("No more tokens aviable");
        }
    }
    public void elevate() throws ParsingException {
        levelIndexes.push(index());
        if(levelIndexes.size()>= recursionLevelLimit)
            throw new ParsingException("Max Recursions reached ("+recursionLevelLimit+")");
    }
    public void commit(){
        int val = levelIndexes.pop();
        index(val);
    }
    public void rollback(){
        levelIndexes.pop();
    }
    public AssignedToken get(int index){
        return tokens.get(index);
    }
    public int level(){
       return levelIndexes.size();
    }

    public int size() {
        return tokens.size();
    }
}
