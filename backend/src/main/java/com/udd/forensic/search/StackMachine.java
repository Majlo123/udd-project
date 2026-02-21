package com.udd.forensic.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

import java.util.List;
import java.util.Stack;

/**
 * Stack Machine that takes a postfix (RPN) list of tokens and builds
 * an Elasticsearch Query object.
 *
 * Algorithm:
 *   1. Iterate through postfix tokens
 *   2. If OPERAND: create a MatchQuery (or MatchPhraseQuery) and push to stack
 *   3. If AND: pop two queries, combine with BoolQuery(must), push result
 *   4. If OR: pop two queries, combine with BoolQuery(should), push result
 *   5. If NOT: pop one query, wrap with BoolQuery(mustNot), push result
 *   6. Final: single Query remains on stack
 */
public class StackMachine {

    /** Default fields to search when no field is specified */
    private static final List<String> DEFAULT_FIELDS = List.of(
            "content", "description", "forensicInvestigator",
            "organization", "malwareName", "classification"
    );

    private StackMachine() {
        // Utility class
    }

    /**
     * Builds an Elasticsearch Query from a postfix token list.
     *
     * @param postfixTokens tokens in Reverse Polish Notation
     * @return compiled Elasticsearch Query
     */
    public static Query buildQuery(List<Token> postfixTokens) {
        if (postfixTokens == null || postfixTokens.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        Stack<Query> stack = new Stack<>();

        for (Token token : postfixTokens) {
            if (token.isOperand()) {
                Query query = buildOperandQuery(token);
                stack.push(query);
            } else if (token.isOperator()) {
                switch (token.getType()) {
                    case AND -> {
                        if (stack.size() < 2) {
                            throw new IllegalStateException("AND operator requires two operands");
                        }
                        Query right = stack.pop();
                        Query left = stack.pop();
                        Query andQuery = BoolQuery.of(b -> b
                                .must(left, right)
                        )._toQuery();
                        stack.push(andQuery);
                    }
                    case OR -> {
                        if (stack.size() < 2) {
                            throw new IllegalStateException("OR operator requires two operands");
                        }
                        Query right = stack.pop();
                        Query left = stack.pop();
                        Query orQuery = BoolQuery.of(b -> b
                                .should(left, right)
                                .minimumShouldMatch("1")
                        )._toQuery();
                        stack.push(orQuery);
                    }
                    case NOT -> {
                        if (stack.isEmpty()) {
                            throw new IllegalStateException("NOT operator requires one operand");
                        }
                        Query operand = stack.pop();
                        Query notQuery = BoolQuery.of(b -> b
                                .mustNot(operand)
                        )._toQuery();
                        stack.push(notQuery);
                    }
                    default -> throw new IllegalStateException("Unknown operator: " + token.getType());
                }
            }
        }

        if (stack.size() != 1) {
            throw new IllegalStateException(
                    "Invalid query expression: stack should have exactly 1 element but has " + stack.size()
            );
        }

        return stack.pop();
    }

    // ==================== Private Helpers ====================

    /**
     * Creates the appropriate Elasticsearch query for a single operand token.
     * - If the token is a phrase -> MatchPhraseQuery
     * - If the token has a specific field -> MatchQuery on that field
     * - Otherwise -> MultiMatchQuery across all default fields with fuzziness
     */
    private static Query buildOperandQuery(Token token) {
        String field = token.getField();
        String value = token.getValue();

        if (token.isPhrase()) {
            // Phrase query - exact phrase match
            if (field != null) {
                return MatchPhraseQuery.of(mp -> mp
                        .field(field)
                        .query(value)
                )._toQuery();
            } else {
                // Phrase across multiple fields: use bool/should with match_phrase on each field
                BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
                for (String defaultField : DEFAULT_FIELDS) {
                    boolBuilder.should(MatchPhraseQuery.of(mp -> mp
                            .field(defaultField)
                            .query(value)
                    )._toQuery());
                }
                return boolBuilder.minimumShouldMatch("1").build()._toQuery();
            }
        } else {
            // Regular term query with fuzziness
            if (field != null) {
                return MatchQuery.of(m -> m
                        .field(field)
                        .query(value)
                        .fuzziness("AUTO")
                )._toQuery();
            } else {
                // MultiMatch across all default fields with fuzziness
                return MultiMatchQuery.of(mm -> mm
                        .fields(DEFAULT_FIELDS)
                        .query(value)
                        .fuzziness("AUTO")
                        .type(TextQueryType.BestFields)
                )._toQuery();
            }
        }
    }
}
