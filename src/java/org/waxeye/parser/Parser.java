/*
 * Waxeye Parser Generator
 * www.waxeye.org
 * Copyright (C) 2008-2010 Orlando Hill
 * Licensed under the MIT license. See 'LICENSE' for details.
 */
package org.waxeye.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;

import org.waxeye.ast.AST;
import org.waxeye.ast.Char;
import org.waxeye.ast.Empty;
import org.waxeye.ast.IAST;
import org.waxeye.ast.Position;
import org.waxeye.ast.PreParsedNonTerminal;
import org.waxeye.input.InputBuffer;
import org.waxeye.input.IParserInput;

/**
 * Implements the logic of the parser.
 *
 * @param <E> The node types for the AST.
 *
 * @author Orlando Hill
 */
public abstract class Parser <E extends Enum<?>> implements IParser<E>
{
    /** The empty node. */
    private final IAST<E> empty;

    /** The char type. */
    private final E charType;

    /** The pre-parsed non-terminal type. */
    private final E preParsedNonTerminalType;

    /** The pos type. */
    private final E posType;

    /** The neg type. */
    private final E negType;

    /** The automata of the parser. */
    private final List<FA<E>> automata;

    /** Whether to check that all input gets parsed. */
    private boolean eofCheck;

    /** Debugging flag. */
    private boolean debug = false;

    /** The starting automaton. */
    private final int start;

    /**
     * Creates a new Parser.
     *
     * @param automata The automata of the parser.
     * @param eofCheck Whether to check that all input gets parsed.
     * @param start The starting automaton.
     * @param emptyType The empty type.
     * @param charType The char type.
     * @param preParsedNonTerminalType The pre-parsed non-terminal type.
     * @param posType The positive check type.
     * @param negType The negative check type.
     */
    public Parser(final List<FA<E>> automata, final boolean eofCheck,
        final int start,
        final E emptyType, final E charType, final E preParsedNonTerminalType, final E posType, final E negType)
    {
        this.automata = automata;
        this.eofCheck = eofCheck;
        this.start = start;
        this.empty = new Empty<E>(emptyType);
        this.charType = charType;
        this.preParsedNonTerminalType = preParsedNonTerminalType;
        this.posType = posType;
        this.negType = negType;
    }

    /** Set whether the parser checks that all input is consumed. */
    public void setEofCheck(boolean eofCheck) {
      this.eofCheck = eofCheck;
    }

    /** Set whether debugging is enabled. */
    public void setDebug(boolean debug) {
      this.debug = debug;
    }

    /** {@inheritDoc} */
    @Override
    public final ParseResult<E> parse(final char[] input)
    {
        return new InnerParser<Void>(new InputBuffer(input), null).parse();
    }

    /** {@inheritDoc} */
    @Override
    public final ParseResult<E> parse(final String input)
    {
        return new InnerParser<Void>(new InputBuffer(input.toCharArray()), null).parse();
    }

    /** {@inheritDoc} */
    @Override
    public final <ExtendedData>
                 ParseResult<E> parse(final IParserInput<ExtendedData> input)
    {
        return new InnerParser<ExtendedData>(input, null).parse();
    }

    /** {@inheritDoc} */
    @Override
    public final <ExtendedData>
                 ParseResult<E> parse(final IParserInput<ExtendedData> input, BiFunction<String, IParserInput<ExtendedData>, Integer> preparsedNonTerminalAt)
    {
        return new InnerParser<ExtendedData>(input, preparsedNonTerminalAt).parse();
    }

    /**
     * A hidden inner class so that we can visit the transition costs without exposing things to the API user.
     *
     * @author Orlando Hill
     */
    private final class InnerParser<ExtendedData> implements ITransitionVisitor<E>
    {
        /** The input to parse. */
        private final IParserInput<ExtendedData> input;

        /** The function that tests for pre-parsed non-terminals.
         * Function to decide if a non-terminal is present at an input position.
         * The first parameter is the name of a pre-parsed non-terminal, as specified by the grammar.
         * The second parameter is the current input, which holds its own position.
         * The function returns the number of character positions within the pre-parsed non-terminal,
         * or -1 if there is no pre-parsed non-terminal with the given name at the given position.
         */
        private BiFunction<String, IParserInput<ExtendedData>, Integer> preparsedNonTerminalAt;

        /** The automata stack. */
        private final Stack<FA<E>> faStack;

        /** The result cache. */
        private final HashMap<CacheKey, CacheItem<E, ExtendedData>> cache;

        /** The line number. */
        private int line;

        /** The column number. */
        private int column;

        /** Whether the last character was a carriage return. */
        private boolean lastCR;

        /** The position of the deepest error. */
        private int errorPos;

        /** The line of the deepest error. */
        private int errorLine;

        /** The column of the deepest error. */
        private int errorCol;

        /** The nt deepest error. */
        private String errorNT;

        /** For debugging, keep the depth of the parser. */
        private int parseDepth = 0;

        /**
         * Creates a new Parser.
         *
         * @param input The input to parse.
         */
        InnerParser(final IParserInput<ExtendedData> input, final BiFunction<String, IParserInput<ExtendedData>, Integer> preparsedNonTerminalAt)
        {
            this.input = input;
            this.preparsedNonTerminalAt = preparsedNonTerminalAt;
            this.faStack = new Stack<FA<E>>();
            this.cache = new HashMap<CacheKey, CacheItem<E, ExtendedData>>();
            this.line = 1;
            this.column = 0;
            this.lastCR = false;
            this.errorPos = 0;
            this.errorLine = 1;
            this.errorCol = 0;
            this.errorNT = automata.get(start).getType().name();
        }

        /**
         * Parses the input.
         *
         * @return The result of the parse.
         */
        ParseResult<E> parse()
        {
            IAST<E> ast = matchAutomaton(start);
            ParseError error = null;

            if (ast == null)
            {
                // Create a parse error
                error = new ParseError(errorPos, errorLine, errorCol, errorNT);
            }
            else
            {
                // Check that all input was consumed
                if (eofCheck && input.peek() != IParserInput.EOF)
                {
                    // Create a parse error - Not all input consumed
                    error = new ParseError(errorPos, errorLine, errorCol, errorNT);
                    ast = null;
                }
            }

            return new ParseResult<E>(ast, error);
        }

        /**
         * Restores the input position to the given values.
         *
         * @param pos The position.
         *
         * @param line The line.
         *
         * @param col The column.
         *
         * @param cr Whether the last character was a CR.
         */
        private void restorePos(final int pos, final ExtendedData extendedData, final int line, final int col, final boolean cr)
        {
            this.input.setPosition(pos);
            this.input.setExtendedData(extendedData);
            this.line = line;
            this.column = col;
            this.lastCR = cr;
        }

        /**
         * Matches the automaton at the given index.
         *
         * @param index The index.
         *
         * @return The result.
         */
        private IAST<E> matchAutomaton(final int index)
        {
            final int startPos = input.getPosition();
            final ExtendedData extendedData = input.getExtendedData();
            final CacheKey key = new CacheKey(index, startPos);
            final CacheItem<E, ExtendedData> cachedItem = cache.get(key);

            if (cachedItem != null)
            {
                restorePos(cachedItem.getPosition(), cachedItem.getExtendedData(), cachedItem.getLine(), cachedItem.getColumn(), cachedItem.getLastCR());
                return cachedItem.getResult();
            }

            final int startLine = line;
            final int startCol = column;
            final boolean startCR = lastCR;
            final FA<E> automaton = automata.get(index);
            final E type = automaton.getType();
            final int mode = automaton.getMode();

            if (debug) System.out.println("  ".repeat(parseDepth++) + "[" + line+"/"+column + "] try " + type.name() + " at pos " + startPos);

            faStack.push(automaton);
            final List<IAST<E>> res = matchState(0);
            faStack.pop();

            IAST<E> value;

            if (type.equals(posType))
            {
                restorePos(startPos, extendedData, startLine, startCol, startCR);

                if (res == null)
                {
                    value = null;
                }
                else
                {
                    value = empty;
                }
            }
            else
            {
                if (type.equals(negType))
                {
                    restorePos(startPos, extendedData, startLine, startCol, startCR);

                    if (res == null)
                    {
                        value = empty;
                    }
                    else
                    {
                        updateError();
                        value = null;
                    }
                }
                else
                {
                    if (res == null)
                    {
                        updateError();
                        value = null;
                    }
                    else
                    {
                        switch (mode)
                        {
                            case FA.VOID:
                            {
                                value = empty;
                                break;
                            }
                            case FA.PRUNE:
                            {
                                switch (res.size())
                                {
                                    case 0:
                                    {
                                        value = empty;
                                        break;
                                    }
                                    case 1:
                                    {
                                        value = res.get(0);
                                        break;
                                    }
                                    default:
                                    {
                                        value = new AST<E>(type, res, new Position(startPos, input.getPosition()));
                                        break;
                                    }
                                }
                                break;
                            }
                            default:
                            {
                                value = new AST<E>(type, res, new Position(startPos, input.getPosition()));
                                break;
                            }
                        }
                    }
                }
            }

            cache.put(key, new CacheItem<E, ExtendedData>(value, input.getPosition(), input.getExtendedData(), line, column, lastCR));

            if (debug) System.out.println("  ".repeat(--parseDepth) + "[" + line+"/"+column + "] " + type.name() + " result: " + (value == null ? "null" : value.getType().name()) + " at pos " + startPos + " to " + input.getPosition());

            return value;
        }

        /**
         * Matches the state at the given index.
         *
         * @param index The index.
         *
         * @return The result.
         */
        private List<IAST<E>> matchState(final int index)
        {
            final State<E> state = faStack.peek().getStates().get(index);
            final List<IAST<E>> res = matchEdges(state.getEdges(), 0);

            if (res == null)
            {
                if (state.isMatch())
                {
                    return new ArrayList<IAST<E>>();
                }
                else
                {
                    return null;
                }
            }
            else
            {
                return res;
            }
        }

        /**
         * Matches the given edges starting from the given index.
         *
         * @param edges The edges.
         *
         * @param index The index.
         *
         * @return The result.
         */
        private List<IAST<E>> matchEdges(final List<Edge<E>> edges, final int index)
        {
            if (index < edges.size())
            {
                String typeName = faStack.peek().getType().name();
                if (debug) System.out.println("  ".repeat(parseDepth++) + "[" + line+"/"+column + "]" + " try edge " + (index+1) + " of " + edges.size() + " for " + typeName);
                final List<IAST<E>> res = matchEdge(edges.get(index));
                if (debug) System.out.println("  ".repeat(--parseDepth) + "[" + line+"/"+column + "]" + " edge " + (index+1) + " of " + edges.size() + " for " + typeName + " : " + (res == null ? "null" : res.size() + " nodes"));

                if (res == null)
                {
                    return matchEdges(edges, index + 1);
                }
                else
                {
                    return res;
                }
            }
            else
            {
                return null;
            }
        }

        /**
         * Matches the given edge.
         *
         * @param edge The edge.
         *
         * @return The result.
         */
        private List<IAST<E>> matchEdge(final Edge<E> edge)
        {
            final int startPos = input.getPosition();
            final ExtendedData extendedData = input.getExtendedData();
            final int startLine = line;
            final int startCol = column;
            final boolean startCR = lastCR;
            final IAST<E> res = edge.getTrans().acceptVisitor(this);

            if (res == null)
            {
                return null;
            }
            else
            {
                final List<IAST<E>> transRes = matchState(edge.getState());

                if (transRes == null)
                {
                    restorePos(startPos, extendedData, startLine, startCol, startCR);
                    return null;
                }
                else
                {
                    if (edge.isVoided() || res.equals(empty))
                    {
                        return transRes;
                    }
                    else
                    {
                        // Note: If we were to memoize state results,
                        //       this would need to be changed.
                        transRes.add(0, res);
                        return transRes;
                    }
                }
            }
        }

        /**
         * Updates the line and column numbers.
         *
         * @param ch The character being consumed.
         */
        private void updateLineCol(final char ch)
        {
            if (ch == '\r')
            {
                line++;
                column = 0;
                lastCR = true;
            }
            else
            {
                if (ch == '\n')
                {
                    if (!lastCR)
                    {
                        line++;
                        column = 0;
                    }
                }
                else
                {
                    column++;
                }

                lastCR = false;
            }
        }

        /**
         * Updates the error info if needed.
         */
        private void updateError()
        {
            if (errorPos < input.getPosition())
            {
                errorPos = input.getPosition();
                errorLine = line;
                errorCol = column;
                errorNT = faStack.peek().getType().name();
            }
        }

        /** {@inheritDoc} */
        @Override
        public IAST<E> visitAutomatonTransition(final AutomatonTransition<E> t)
        {
            if (debug) System.out.println("  ".repeat(parseDepth) + "[" + line+"/"+column + "]" + " automaton: " + automata.get(t.getIndex()).getType().name());
            return matchAutomaton(t.getIndex());
        }

        /** {@inheritDoc} */
        @Override
        public IAST<E> visitCharTransition(final CharTransition<E> t)
        {
            if (input.peek() != IParserInput.EOF)
            {
                final char c = (char) input.peek();

                final String displayChar = (c == '\n') ? "\\n" : (c == '\r') ? "\\r" : (c == '\t') ? "\\t" : Character.toString(c);
                if (t.withinSet(c))
                {
                    if (debug) System.out.println("  ".repeat(parseDepth) + "[" + line+"/"+column + "]" + " char match: '" + displayChar + "'");
                    input.consume();
                    updateLineCol(c);
                    return new Char<E>(c, charType, input.getPosition());
                }
                else
                {
                    if (debug) System.out.println("  ".repeat(parseDepth) + "[" + line+"/"+column + "]" + " no char match: '" + displayChar + "'");
                    updateError();
                    return null;
                }
            }
            else
            {
                if (debug) System.out.println("  ".repeat(parseDepth) + "[" + line+"/"+column + "]" + " no char match: end of input");
                updateError();
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public IAST<E> visitWildCardTransition(final WildCardTransition<E> t)
        {
            if (input.peek() == IParserInput.EOF)
            {
                if (debug) System.out.println("  ".repeat(parseDepth) + "[" + line+"/"+column + "]" + ". no match: end of input");
                updateError();
                return null;
            }

            final char c = (char) input.consume();
            if (debug) System.out.println("  ".repeat(parseDepth) + "[" + line+"/"+column + "]" + ". match: '" + c + "'");
            updateLineCol(c);
            return new Char<E>(c, charType, input.getPosition());
        }

        /** {@inheritDoc} */
        @Override
        public IAST<E> visitPreParsedNonTerminalTransition(PreParsedNonTerminalTransition<E> t)
        {
          int startPos = input.getPosition();
          int skipChars = (preparsedNonTerminalAt == null) ? -1 : preparsedNonTerminalAt.apply(t.getName(), input);
          if (skipChars >= 0)
          {
              int endPos = startPos + skipChars;
              // Get the corresponding SmaxElement for the pre-parsed non-terminal, before the input position is changed, which may reset the extended data.
              ExtendedData correspondingSmaxElement = input.getExtendedData();
              // Skip past the characters that have been recognized earlier, as a pre-parsed non-terminal.
              input.setPosition(endPos);
              // Return an instance of the pre-parsed non-terminal.
              if (debug) System.out.println("  ".repeat(parseDepth) + "[" + line+"/"+column + "]" + " match: <" + t.getName() + ">");
              return new PreParsedNonTerminal<E, ExtendedData>(preParsedNonTerminalType, t.getName(), new Position(startPos, endPos), correspondingSmaxElement);
          }
          if (debug) System.out.println("  ".repeat(parseDepth) + "[" + line+"/"+column + "]" + " no match: <" + t.getName() + ">");
          updateError();
          return null;
        }
    }
}
