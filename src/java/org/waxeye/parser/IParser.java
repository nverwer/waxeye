/*
 * Waxeye Parser Generator
 * www.waxeye.org
 * Copyright (C) 2008-2010 Orlando Hill
 * Licensed under the MIT license. See 'LICENSE' for details.
 */
package org.waxeye.parser;

import java.util.function.BiFunction;

import org.waxeye.input.IParserInput;

/**
 * An interface to a Waxeye parser.
 *
 * @param <E> The node types for the AST.
 *
 * @author Orlando Hill
 */
public interface IParser <E extends Enum<?>>
{
    /**
     * Parses the input.
     *
     * @param input The input to parse.
     *
     * @return A ParseResult with either an AST or an error.
     */
    ParseResult<E> parse(char[] input);

    /**
     * Parses the input.
     *
     * @param input The input to parse.
     *
     * @return A ParseResult with either an AST or an error.
     */
    ParseResult<E> parse(String input);

    /**
     * Parses the input.
     *
     * @param input The input to parse.
     *
     * @return A ParseResult with either an AST or an error.
     */
    <ExtendedData> ParseResult<E> parse(IParserInput<ExtendedData> input);

    /**
     * Parses the input, recognizing pre-parsed non-terminals.
     *
     * @param input The input to parse.
     * @param preparsedNonTerminalAt A function that, given the name of a pre-parsed non-terminal and a position in the input,
     *   returns the length of the pre-parsed non-terminal at that position, or null if there is none.
     *
     * @return A ParseResult with either an AST or an error.
     */
    <ExtendedData>
    ParseResult<E> parse(IParserInput<ExtendedData> input, BiFunction<String, IParserInput<ExtendedData>, Integer> preparsedNonTerminalAt);

}
