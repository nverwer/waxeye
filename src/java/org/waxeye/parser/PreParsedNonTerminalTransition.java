package org.waxeye.parser;

import org.waxeye.ast.IAST;

public class PreParsedNonTerminalTransition <E extends Enum<?>> implements ITransition<E>
{

  /** The name of the pre-parsed non-terminal. */
  private final String nonTerminalName;

  /** The index of the automaton. */
  private final int index;

  public PreParsedNonTerminalTransition(final String nonTerminalName, final int index)
  {
    this.nonTerminalName = nonTerminalName;
    this.index = index;
  }

  /**
   * Returns the name of the pre-parsed non-terminal.
   *
   * @return the name of the pre-parsed non-terminal.
   */
  public String getNonTerminalName()
  {
    return nonTerminalName;
  }

  /**
   * Returns the index.
   *
   * @return Returns the index.
   */
  public int getIndex()
  {
      return index;
  }

  @Override
  public IAST<E> acceptVisitor(ITransitionVisitor<E> visitor)
  {
    return visitor.visitPreParsedNonTerminalTransition(this);
  }

}
