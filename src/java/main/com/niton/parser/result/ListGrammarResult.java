package com.niton.parser.result;

import com.niton.parser.GrammarResult;
import com.niton.parser.token.Tokenizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ListGrammarResult extends GrammarResult {
    private List<GrammarResult> list = new LinkedList<>();

    public void clear() {
        list.clear();
    }

    public GrammarResult get(int index) {
        return list.get(index);
    }

    public void add(GrammarResult element) {
        list.add( element);
    }

    public GrammarResult remove(int index) {
        return list.remove(index);
    }

    public List<GrammarResult> getList() {
        return list;
    }

    public void setList(List<GrammarResult> list) {
        this.list = list;
    }

    @Override
    public Collection<? extends Tokenizer.AssignedToken> join() {
        if(list.size() == 0)
            return new ArrayList<>(0);
        return list.stream().map(e -> e.join()).reduce((a, b) -> {
            a.addAll(b);
            return a;
        }).get();
    }
}
