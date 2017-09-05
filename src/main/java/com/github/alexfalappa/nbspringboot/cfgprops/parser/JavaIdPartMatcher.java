package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import org.parboiled.MatcherContext;
import org.parboiled.matchers.CustomMatcher;

/**
 * A custom matcher delegating to {@code Character.isJavaIdentifierPart()}.
 *
 * @author Alessandro Falappa
 */
public class JavaIdPartMatcher extends CustomMatcher {

    public JavaIdPartMatcher() {
        super("JavaIdPart");
    }

    @Override
    public boolean isSingleCharMatcher() {
        return true;
    }

    @Override
    public boolean canMatchEmpty() {
        return false;
    }

    @Override
    public boolean isStarterChar(char c) {
        return acceptChar(c);
    }

    @Override
    public char getStarterChar() {
        return 'a';
    }

    @Override
    public final <V> boolean match(MatcherContext<V> context) {
        if (!acceptChar(context.getCurrentChar())) {
            return false;
        }
        context.advanceIndex(1);
        context.createNode();
        return true;
    }

    private boolean acceptChar(char c) {
        return Character.isJavaIdentifierPart(c);
    }
}
