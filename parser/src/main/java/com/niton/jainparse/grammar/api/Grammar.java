package com.niton.jainparse.grammar.api;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.types.*;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenable;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * A Grammar is a rule how to collect tokens together and handle them
 *
 * @author Nils
 * @version 2019-05-28
 */
@Getter
public abstract class Grammar<R extends AstNode<T>, T extends Enum<T> & Tokenable> {
    private String name;
    @Getter
    private @Nullable String displayName;

    public static<T extends Enum<T> & Tokenable> Builder<T> build(GrammarName string) {
        return build(string.getName());
    }

    public static<T extends Enum<T> & Tokenable> Builder<T> build(String string) {
        return new Builder<>(string);
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?, T> anyExcept(Grammar<?, T> except) {
        return new AnyExceptGrammar<>(except);
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?, T> reference(String name) {
        return new GrammarReferenceGrammar<T>(name);
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?, ?> reference(GrammarName name) {
        return new GrammarReferenceGrammar<T>(name.getName());
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?,T> reference(Grammar<?,T> name) {
        return new GrammarReferenceGrammar<>(name.getName());
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?,T> ignore(Grammar<?,T> g) {
        return new IgnoreGrammar<>(g);
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?,T> anyOf(Grammar<?,T>... grammars) {
        return new MultiGrammar<>(grammars);
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?,T> optional(Grammar<?,T> g) {
        return new OptionalGrammar<>(g);
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?,T> not(Grammar<?,T> g) {
        return new NotGrammar<>(g);
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?,T> repeat(Grammar<?,T> g) {
        return new RepeatGrammar<>(g, 0);
    }

    public static<T extends Enum<T> & Tokenable> Grammar<?,T> keyword(String keyword) {
        return new KeywordGrammar<T>(keyword);
    }

    public static <T extends Enum<T> & Tokenable> Grammar<?,T> token(T token) {
        return new TokenGrammar<>(token);
    }

    public static<T extends Enum<T> & Tokenable> Grammar<?,T> object(LinkedHashMap<String, ? extends Grammar<?,T>> properties) {
        var chain = new ChainGrammar<T>();
        properties.forEach((name, grammar) -> chain.addGrammar(grammar, name));
        return chain;
    }

    public static<T extends Enum<T> & Tokenable> Grammar<?,T> first(String name, Grammar<?,T> grammar) {
        var chainGrammar = new ChainGrammar<T>();
        chainGrammar.addGrammar(grammar, name);
        return chainGrammar;
    }

    public Grammar<?,T> ignore() {
        return new IgnoreGrammar<>(this);
    }

    public Grammar<?,T> optional() {
        return new OptionalGrammar<>(this);
    }

    public Grammar<?,T> repeat() {
        return repeat(0);
    }

    public Grammar<?,T> repeat(int minimum) {
        return new RepeatGrammar<>(this, minimum);
    }

    public void map(GrammarReferenceMap<T> ref) {
        ref.map(this);
    }

    public boolean parsable(@NonNull TokenStream<T> tokens, @NonNull GrammarReference<T> ref) {
        try {
            tokens.elevate();
        } catch (Exception e) {
            throw new RuntimeException("Evaluating " + name + " failed", e);
        }
        try {
            GrammarMatcher<R, T> matcher = createExecutor();
            matcher.setOriginGrammarName(getName());
            matcher.setIdentifier(getIdentifier());
            return matcher.parse(tokens, ref).wasParsed();
        } finally {
            tokens.rollback();
        }
    }

    @NotNull
    public Grammar<?,T> namedCopy(@NotNull GrammarName variable) {
        var copy = copy();
        copy.setName(variable.getName());
        return copy;
    }

    @NotNull
    public Grammar<?,T> namedCopy(@NotNull String variable) {
        var copy = copy();
        copy.setName(variable);
        return copy;
    }

    protected abstract Grammar<?,T> copy();

    /**
     * To see what a executor is look at {@link GrammarMatcher}
     */
    protected abstract GrammarMatcher<R, T> createExecutor();

    /**
     * @param name the name to set
     */
    public Grammar<R,T> setName(String name) {
        this.name = name;
        return this;
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull TokenStream<T> tokens, @NotNull GrammarReference<T> ref) {
        GrammarMatcher<R, T> matcher = createExecutor();
        matcher.setOriginGrammarName(getName());
        matcher.setIdentifier(getIdentifier());
        return matcher.parse(tokens, ref);
    }

    public abstract boolean isLeftRecursive(GrammarReference<T> ref);

    public Grammar<?,T> merged() {
        return new MergedGrammar(this);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + getName() + ")";
    }

    public Grammar<?,T> or(Grammar<?,T>... alternatives) {
        var combined = new Grammar<?>[alternatives.length + 1];
        combined[0] = this;
        System.arraycopy(alternatives, 0, combined, 1, alternatives.length);
        return new MultiGrammar<>(combined);
    }

    public ChainGrammar then(Grammar<?> tokenDefiner) {
        var chain = new ChainGrammar();
        chain.addGrammar(this);
        chain.addGrammar(tokenDefiner);
        return chain;
    }

    public ChainGrammar then(Tokenable tokenDefiner) {
        return then(token(tokenDefiner));
    }

    public ChainGrammar then(String name, Grammar<?> tokenDefiner) {
        var chain = new ChainGrammar();
        chain.addGrammar(this);
        chain.addGrammar(tokenDefiner, name);
        return chain;
    }

    public Grammar<R,T> named(String name) {
        this.name = name;
        return this;
    }

    public Grammar<R,T> named(GrammarName name) {
        this.name = name.getName();
        return this;
    }

    public Grammar<R,T> display(String name) {
        this.displayName = name;
        return this;
    }

    @NotNull
    public String getIdentifier() {
        if (getDisplayName() != null) {
            return getDisplayName();
        }
        if (getName() != null)
            return getName();
        return this.getClass().getSimpleName();
    }

    @Data
    public static class ParsingProbe {
        private final boolean parsable;
        private final ParsingException exception;
    }

    public static class Builder<T extends Enum<T> & Tokenable> {
        private final ChainGrammar<T> chain = new ChainGrammar<>();
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

        public RuleApplier grammar(Grammar<?,T> g) {
            return new RuleApplier(g);
        }

        public RuleApplier token(Tokenable g) {
            return new RuleApplier(g, new Builder.TokenGrammarConverter());
        }

        public RuleApplier token(String g) {
            return new RuleApplier(g, new Builder.TokenNameGrammarConverter());
        }

        public RuleApplier grammars(Grammar<?,T>... g) {
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

        public ChainGrammar<T> get() {
            return chain;
        }

        private interface GrammarConverter<T,K extends Enum<K> & Tokenable> {
            Grammar<?,K> toGrammar(T s);
        }

        //This classes convert The specific types into grammars
        private static class ReferenceGrammarConverter<T extends Enum<T> & Tokenable> implements GrammarConverter<String, T> {
            @Override
            public Grammar<?,T> toGrammar(String s) {
                return new GrammarReferenceGrammar<>(s);
            }
        }

        //This classes convert The specific types into grammars
        private static class NamedGrammarConverter<T extends Enum<T> & Tokenable> implements GrammarConverter<GrammarName,T> {
            @Override
            public Grammar<?,T> toGrammar(GrammarName s) {
                return new GrammarReferenceGrammar(s.getName());
            }
        }

        private static class TokenGrammarConverter<T extends Enum<T> & Tokenable> implements GrammarConverter<Tokenable> {
            public Grammar<?,T> toGrammar(T s) {
                return new TokenGrammar<>(s);
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
