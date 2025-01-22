package org.waxeye.parser;

import org.waxeye.ast.IAST;

public class PreParsedNonTerminalTransition <E extends Enum<?>> implements ITransition<E>
{

  /** The name of the pre-parsed non-terminal. */
  private final String name;

  public PreParsedNonTerminalTransition(final String name)
  {
    this.name = name;
  }

  /**
   * Returns the name of the pre-parsed non-terminal.
   *
   * @return the name of the pre-parsed non-terminal.
   */
  public String getName()
  {
    return name;
  }

  @Override
  public IAST<E> acceptVisitor(ITransitionVisitor<E> visitor)
  {
    return visitor.visitPreParsedNonTerminalTransition(this);
  }

}
