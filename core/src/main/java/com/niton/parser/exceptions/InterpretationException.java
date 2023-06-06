package com.niton.parser.exceptions;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.LocatableReducedNode;

public class InterpretationException extends RuntimeException {
    private final AstNode.Location location;
    private final int context;
    public InterpretationException(String message, LocatableReducedNode nodeValue, int context) {
        super(message);
        this.context = context;
        this.location = nodeValue.getLocation();
    }
    public String getSyntaxErrorMessage(String text){
        return location.markInText(text,context, getMessage());
    }
}
