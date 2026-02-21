package com.udd.forensic.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Parses a user's advanced search query string into tokens and converts
 * the infix expression to postfix (Reverse Polish Notation) using the
 * Shunting-yard algorithm.
 *
 * Supported syntax:
 *   - AND, OR, NOT operators
 *   - Parentheses for grouping: ( )
 *   - Field-specific search: field:value or field:"phrase"
 *   - Phrase search: "exact phrase"
 *
 * Example input:  ((malware:WannaCry OR description:"enkripcija fajlova") AND NOT classification:Spyware)
 * Postfix output: [malware:WannaCry] [description:"enkripcija fajlova"] [OR] [classification:Spyware] [NOT] [AND]
 */
public class QueryParser {

    private QueryParser() {
        // Utility class
    }

    // ==================== 1. Tokenization ====================

    /**
     * Tokenizes the input query string into a list of Token objects.
     * Handles quoted phrases, field:value syntax, operators, and parentheses.
     */
    public static List<Token> tokenize(String query) {
        List<Token> tokens = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return tokens;
        }

        char[] chars = query.toCharArray();
        int i = 0;

        while (i < chars.length) {
            char c = chars[i];

            // Skip whitespace
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Parentheses
            if (c == '(') {
                tokens.add(Token.operator(Token.Type.LEFT_PAREN));
                i++;
                continue;
            }
            if (c == ')') {
                tokens.add(Token.operator(Token.Type.RIGHT_PAREN));
                i++;
                continue;
            }

            // Quoted phrase: "some phrase here"
            if (c == '"') {
                i++; // skip opening quote
                StringBuilder phrase = new StringBuilder();
                while (i < chars.length && chars[i] != '"') {
                    phrase.append(chars[i]);
                    i++;
                }
                if (i < chars.length) {
                    i++; // skip closing quote
                }
                tokens.add(Token.operand(phrase.toString(), null, true));
                continue;
            }

            // Read a word (could be operator, field:value, or plain term)
            StringBuilder word = new StringBuilder();
            while (i < chars.length && !Character.isWhitespace(chars[i])
                    && chars[i] != '(' && chars[i] != ')' && chars[i] != '"') {
                word.append(chars[i]);
                i++;
            }

            String w = word.toString();

            // Check for operators
            if (w.equalsIgnoreCase("AND")) {
                tokens.add(Token.operator(Token.Type.AND));
            } else if (w.equalsIgnoreCase("OR")) {
                tokens.add(Token.operator(Token.Type.OR));
            } else if (w.equalsIgnoreCase("NOT")) {
                tokens.add(Token.operator(Token.Type.NOT));
            } else {
                // Check for field:value or field:"phrase"
                int colonIndex = w.indexOf(':');
                if (colonIndex > 0 && colonIndex < w.length() - 1) {
                    String field = w.substring(0, colonIndex);
                    String value = w.substring(colonIndex + 1);

                    // Handle field:"phrase" where phrase continues after the colon
                    if (value.startsWith("\"")) {
                        // The value is a quoted phrase starting here
                        StringBuilder phraseValue = new StringBuilder(value.substring(1));
                        // Check if the phrase ends within the same token
                        if (phraseValue.toString().endsWith("\"")) {
                            phraseValue.deleteCharAt(phraseValue.length() - 1);
                            tokens.add(Token.operand(phraseValue.toString(), field, true));
                        } else {
                            // Continue reading until closing quote
                            while (i < chars.length && chars[i] != '"') {
                                phraseValue.append(chars[i]);
                                i++;
                            }
                            if (i < chars.length) {
                                i++; // skip closing quote
                            }
                            tokens.add(Token.operand(phraseValue.toString(), field, true));
                        }
                    } else {
                        tokens.add(Token.operand(value, field, false));
                    }
                } else {
                    // Plain search term
                    tokens.add(Token.operand(w));
                }
            }
        }

        return tokens;
    }

    // ==================== 2. Infix to Postfix (Shunting-yard) ====================

    /**
     * Converts an infix token list to postfix (Reverse Polish Notation)
     * using Dijkstra's Shunting-yard algorithm.
     *
     * Operator precedence: NOT (3) > AND (2) > OR (1)
     */
    public static List<Token> convertToPostfix(List<Token> infixTokens) {
        List<Token> output = new ArrayList<>();
        Stack<Token> operatorStack = new Stack<>();

        for (Token token : infixTokens) {
            if (token.isOperand()) {
                // Operands go directly to output
                output.add(token);
            } else if (token.isLeftParen()) {
                // Left parenthesis goes on the operator stack
                operatorStack.push(token);
            } else if (token.isRightParen()) {
                // Pop operators until we find matching left parenthesis
                while (!operatorStack.isEmpty() && !operatorStack.peek().isLeftParen()) {
                    output.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty()) {
                    operatorStack.pop(); // discard the left parenthesis
                }
            } else if (token.isOperator()) {
                // Pop operators with higher or equal precedence
                while (!operatorStack.isEmpty()
                        && operatorStack.peek().isOperator()
                        && operatorStack.peek().precedence() >= token.precedence()) {
                    output.add(operatorStack.pop());
                }
                operatorStack.push(token);
            }
        }

        // Pop remaining operators
        while (!operatorStack.isEmpty()) {
            Token t = operatorStack.pop();
            if (!t.isLeftParen()) {
                output.add(t);
            }
        }

        return output;
    }

    /**
     * Convenience method: tokenize and convert to postfix in one step.
     */
    public static List<Token> parse(String query) {
        List<Token> tokens = tokenize(query);
        return convertToPostfix(tokens);
    }
}
