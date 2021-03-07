package com.niton.parser;

import com.niton.parser.result.*;

import java.util.ArrayList;
import java.util.List;

public class ResultResolver {
    private static  boolean resolveAny = true;

    public static GrammarResult resolve(GrammarResult res) {
        while (res != null && res instanceof OptionalGrammarResult) {
            res = ((OptionalGrammarResult) res).getValue();
        }
        return res;
    }

    public static List resolveList(ListGrammarResult res) {
        List<Object> list = new ArrayList();
        for (GrammarResult obj : res.getList()) {
            obj = resolve(obj);
            if (obj instanceof ListGrammarResult)
                list.add(resolveList((ListGrammarResult) obj));
            else
                list.add(obj);
        }
        return list;
    }

    public static void setResolveAny(boolean resolveAny) {
        ResultResolver.resolveAny = resolveAny;
    }

    public static Object getReturnValue(GrammarResult g) {
        if (g instanceof SuperGrammarResult)
            return g;
        if (g instanceof AnyGrammarResult) {
            if (resolveAny)
                return getReturnValue(((AnyGrammarResult) g).getRes());
            return g;
        }
        if (g instanceof TokenGrammarResult)
            return g.joinTokens();
        if (g instanceof OptionalGrammarResult)
            return getReturnValue(((OptionalGrammarResult) g).getValue());
        if (g instanceof ListGrammarResult)
            return resolveList((ListGrammarResult) g);
        return null;
    }
}
