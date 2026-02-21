package com.udd.forensic.search;

import lombok.Getter;

/**
 * Represents a token in the parsed search query.
 * Tokens can be operands (search terms/phrases) or operators (AND, OR, NOT, parentheses).
 */
@Getter
public class Token {

    public enum Type {
        OPERAND,
        AND,
        OR,
        NOT,
        LEFT_PAREN,
        RIGHT_PAREN
    }

    private final Type type;
    private final String value;   // Search term text (for operands)
    private final String field;   // Optional field name, e.g., "classification" in "classification:Ransomware"
    private final boolean phrase;  // Whether the value is a phrase (was quoted)

    private Token(Type type, String value, String field, boolean phrase) {
        this.type = type;
        this.value = value;
        this.field = field;
        this.phrase = phrase;
    }

    // ==================== Factory Methods ====================

    public static Token operand(String value, String field, boolean phrase) {
        return new Token(Type.OPERAND, value, field, phrase);
    }

    public static Token operand(String value) {
        return new Token(Type.OPERAND, value, null, false);
    }

    public static Token operator(Type type) {
        if (type == Type.OPERAND) {
            throw new IllegalArgumentException("Use operand() factory method for operands");
        }
        return new Token(type, null, null, false);
    }

    // ==================== Helper Methods ====================

    public boolean isOperand() {
        return type == Type.OPERAND;
    }

    public boolean isOperator() {
        return type == Type.AND || type == Type.OR || type == Type.NOT;
    }

    public boolean isLeftParen() {
        return type == Type.LEFT_PAREN;
    }

    public boolean isRightParen() {
        return type == Type.RIGHT_PAREN;
    }

    /**
     * Returns the precedence of the operator.
     * NOT > AND > OR (higher number = higher precedence).
     */
    public int precedence() {
        return switch (type) {
            case NOT -> 3;
            case AND -> 2;
            case OR -> 1;
            default -> 0;
        };
    }

    @Override
    public String toString() {
        if (isOperand()) {
            String fieldPrefix = field != null ? field + ":" : "";
            String quoteWrapper = phrase ? "\"" : "";
            return fieldPrefix + quoteWrapper + value + quoteWrapper;
        }
        return type.name();
    }
}
