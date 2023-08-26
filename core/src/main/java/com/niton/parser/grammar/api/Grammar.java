package com.niton.parser.grammar.api;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.SequenceNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.types.*;
import com.niton.parser.token.TokenReference;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenable;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * A Grammar is a rule how to collect tokens together and handle them
 *
 * @author Nils
 * @version 2019-05-28
 */
public abstract class Grammar<R extends AstNode> {
    private String name;

    public static Builder build(GrammarName string) {
        return build(string.getName());
    }

    public static Builder build(String string) {
        return new Builder(string);
    }

    public static Grammar<?> anyExcept(Grammar<?> except) {
        return new AnyExceptGrammar(except);
    }

    public static Grammar<?> reference(String name) {
        return new GrammarReferenceGrammar(name);
    }

    public static Grammar<?> reference(GrammarName name) {
        return new GrammarReferenceGrammar(name.getName());
    }

    public static Grammar<?> reference(Grammar name) {
        return new GrammarReferenceGrammar(name.getName());
    }

    public static Grammar<?> ignore(Grammar<?> g) {
        return new IgnoreGrammar(g);
    }

    public static Grammar<?> anyOf(Grammar<?>... grammars) {
        return new MultiGrammar(grammars);
    }

    public static Grammar<?> optional(Grammar<?> g) {
        return new OptionalGrammar(g);
    }

    public static Grammar<?> repeat(Grammar<?> g) {
        return new RepeatGrammar(g, 0);
    }

    public static Grammar<?> token(TokenReference token) {
        return token(token.getName());
    }

    public static Grammar<?> token(String token) {
        return new TokenGrammar(token);
    }

    public static Grammar<?> keyword(String keyword) {
        return new KeywordGrammar(keyword);
    }

    public static Grammar<?> token(Tokenable token) {
        return token(token.name());
    }

    public static Grammar<?> object(LinkedHashMap<String, ? extends Grammar<?>> properties) {
        var chain = new ChainGrammar();
        properties.forEach((name, grammar) -> chain.addGrammar(grammar, name));
        return chain;
    }

    public Grammar<?> anyExcept() {
        return new AnyExceptGrammar(this);
    }

    public Grammar<?> ignore() {
        return new IgnoreGrammar(this);
    }

    public Grammar<?> optional() {
        return new OptionalGrammar(this);
    }

    public Grammar<?> repeat() {
        return repeat(0);
    }
    public Grammar<?> repeat(int minimum) {
        return new RepeatGrammar(this,minimum);
    }

    public void map(GrammarReferenceMap ref) {
        ref.map(this);
    }

    public ParsingProbe parsable(@NonNull TokenStream tokens, @NonNull GrammarReference ref) {
        try {
            tokens.elevate();
        } catch (Exception e) {
            throw new RuntimeException("Evaluating " + name + " failed", e);
        }
        try {
            GrammarMatcher<R> matcher = createExecutor();
            matcher.setOriginGrammarName(getName());
            matcher.setIdentifier(getIdentifier());
            var result = matcher.parse(tokens, ref);
            return new ParsingProbe(true, result.getParsingException());
        } catch (ParsingException pex) {
            return new ParsingProbe(false, pex);
        } finally {
            tokens.rollback();
        }
    }

    public static Grammar<?> first(String name, Grammar<?> grammar) {
        var chainGrammar = new ChainGrammar();
        chainGrammar.addGrammar(grammar, name);
        return chainGrammar;
    }

    @NotNull
    public Grammar<?> namedCopy(@NotNull GrammarName variable) {
        var copy = copy();
        copy.setName(variable.getName());
        return copy;
    }
    @NotNull
    public Grammar<?> namedCopy(@NotNull String variable) {
        var copy = copy();
        copy.setName(variable);
        return copy;
    }
    protected abstract Grammar<?> copy();

    @Data
    public static class ParsingProbe{
        private final boolean parsable;
        private final ParsingException exception;
    }

    /**
     * To see what a executor is look at {@link GrammarMatcher}
     */
    protected abstract GrammarMatcher<R> createExecutor();

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     * @return
     */
    public Grammar<R> setName(String name) {
        this.name = name;
        return this;
    }

    @NotNull
    public R parse(@NonNull TokenStream tokens, @NotNull GrammarReference ref)
            throws ParsingException {
        GrammarMatcher<R> matcher = createExecutor();
        matcher.setOriginGrammarName(getName());
        matcher.setIdentifier(getIdentifier());
        return matcher.parse(tokens, ref);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.getClass().getSimpleName(), getName());
    }

    public Grammar<?> or(Grammar<?>... alternatives) {
        var combined = new Grammar<?>[alternatives.length + 1];
        combined[0] = this;
        System.arraycopy(alternatives, 0, combined, 1, alternatives.length);
        return new MultiGrammar(combined);
    }

    public Grammar<?> then(Grammar<?> tokenDefiner) {
        var chain = new ChainGrammar();
        chain.addGrammar(this);
        chain.addGrammar(tokenDefiner);
        return chain;
    }
    public Grammar<?> then(Tokenable tokenDefiner) {
        return then(token(tokenDefiner));
    }
    public Grammar<?> then(String name, Grammar<?> tokenDefiner) {
        var chain = new ChainGrammar();
        chain.addGrammar(this);
        chain.addGrammar(tokenDefiner, name);
        return chain;
    }

    public Grammar<R> named(String name) {
        this.name = name;
        return this;
    }

    @NotNull
    public Grammar<R> named(GrammarName name) {
        this.name = name.getName();
        return this;
    }

    public String getIdentifier() {
        if (getName() != null)
            return getName();
        return this.getClass().getSimpleName();
    }

    public static class Builder {
        private final ChainGrammar chain = new ChainGrammar();
        @Getter
        @Setter
        private boolean directRecursion = true;

        public Builder(String name) {
            chain.setName(name);
        }

        public RuleApplier grammar(GrammarName n) {
            return grammar(n.getName());
        }

        public RuleApplier grammar(String g) {
            if (!directRecursion && chain.getName().equals(g)) {
                throw new IllegalArgumentException("directRecursion forbidden! (" + g + " grammar in " + g + " grammar). This can be enabled using 'setDirectRecursion(boolean)'");
            }
            return new RuleApplier(g, new Builder.ReferenceGrammarConverter());
        }

        public RuleApplier grammar(Grammar<?> g) {
            return new RuleApplier(g);
        }

        public RuleApplier token(Tokenable g) {
            return new RuleApplier(g, new Builder.TokenGrammarConverter());
        }

        public RuleApplier token(TokenReference g) {
            return token(g.getName());
        }

        public RuleApplier token(String g) {
            return new RuleApplier(g, new Builder.TokenNameGrammarConverter());
        }

        public RuleApplier grammars(Grammar<?>... g) {
            return new MultiRuleApplier(g);
        }

        public RuleApplier grammars(String... g) {
            for (String s : g) {
                if (s.equals(chain.getName()) && !directRecursion) {
                    throw new IllegalArgumentException(String.format(
                            "directRecursion forbidden! (%s grammar in %s grammar). " +
                                    "This can be enabled using 'setDirectRecursion(boolean)'",
                            chain.getName(),
                            chain.getName()
                    ));
                }
            }

            return new MultiRuleApplier(new Builder.ReferenceGrammarConverter(), g);
        }

        public RuleApplier grammars(GrammarName... g) {
            return new MultiRuleApplier(new Builder.NamedGrammarConverter(), g);
        }

        public RuleApplier tokens(Tokenable... g) {
            return new MultiRuleApplier(new Builder.TokenGrammarConverter(), g);
        }

        public RuleApplier tokens(String... g) {
            return new MultiRuleApplier(new Builder.TokenNameGrammarConverter(), g);
        }

        public RuleApplier keyword(String keyword) {
            return new RuleApplier(Grammar.keyword(keyword));
        }

        public Grammar<SequenceNode> get() {
            return chain;
        }

        private interface GrammarConverter<T> {
            Grammar<?> toGrammar(T s);
        }

        //This classes convert The specific types into grammars
        private static class ReferenceGrammarConverter implements GrammarConverter<String> {
            @Override
            public Grammar<?> toGrammar(String s) {
                return new GrammarReferenceGrammar(s);
            }
        }

        //This classes convert The specific types into grammars
        private static class NamedGrammarConverter implements GrammarConverter<GrammarName> {
            @Override
            public Grammar<?> toGrammar(GrammarName s) {
                return new GrammarReferenceGrammar(s.getName());
            }
        }

        private static class TokenGrammarConverter implements GrammarConverter<Tokenable> {
            public Grammar<?> toGrammar(Tokenable s) {
                return new TokenGrammar(s.name());
            }
        }

        private static class TokenNameGrammarConverter implements GrammarConverter<String> {
            public Grammar<?> toGrammar(String s) {
                return new TokenGrammar(s);
            }
        }

        private static class EnumGrammarConverter implements GrammarConverter<Enum<?>> {
            @Override
            public Grammar<?> toGrammar(Enum<?> s) {
                return new GrammarReferenceGrammar(s.name());
            }
        }

        /**
         * The matcher provides all the adding methodes for each grammar.
         * So it is provided for tokens strings and grammars each. And it is not neccessary to add 3
         * methodes
         */
        public class RuleApplier {
            private Grammar<?> g;

            public <T> RuleApplier(T s, GrammarConverter<T> converter) {
                g = converter.toGrammar(s);
            }

            public RuleApplier(Grammar<?> t) {
                g = t;
            }

            /**
             * @see IgnoreGrammar
             */
            public RuleApplier ignore() {
                g = new IgnoreGrammar(g);
                return this;
            }

            /**
             * @see RepeatGrammar
             */
            public RuleApplier repeat() {
                g = new RepeatGrammar(g, 0);
                return this;
            }

            /**
             * @see OptionalGrammar
             */
            public RuleApplier optional() {
                g = new OptionalGrammar(g);
                return this;
            }

            public RuleApplier anyExcept() {
                g = new AnyExceptGrammar(g);
                return this;
            }

            public Builder add() {
                chain.addGrammar(g);
                return Builder.this;
            }

            public Builder add(String message) {
                return name(message);
            }

            /**
             * Names and adds the grammar. This makes it possible to fetch the match later on
             *
             * @param name the name to give
             */
            public synchronized Builder name(String name) {
                chain.addGrammar(g, name);
                return Builder.this;
            }
        }

        public class MultiRuleApplier extends RuleApplier {
            public <T> MultiRuleApplier(GrammarConverter<T> converter, T... s) {
                this(Arrays.stream(s).map(converter::toGrammar).toArray(i -> new Grammar<?>[i]));
            }

            public MultiRuleApplier(Grammar<?>... t) {
                super(new MultiGrammar(t));
            }
        }
    }
}

