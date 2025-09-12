/*
 * Waxeye Parser Generator
 * www.waxeye.org
 * Copyright (C) 2008-2010 Nico Verwer
 * Licensed under the MIT license. See 'LICENSE' for details.
 */
package org.waxeye.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * An AST node with a pre-parsed non-terminal.
 *
 * @param <E> The node types for the AST.
 *
 * @author Nico Verwer
 */
public class PreParsedNonTerminal <E extends Enum<?>, ExtendedData> implements IAST<E>, IPreParsedNonTerminal<ExtendedData>
{
  /** The type of AST node. */
  private final E type;

  /** The name of the pre-parsed non-terminal. */
  private final String name;

  /** The position of the AST. */
  private Position position;

  /** The extended data associated with the pre-parsed non-terminal. */
  private ExtendedData extendedData;


  /**
   * Creates a new PreParsedNonTerminal AST.
   *
   * @param type The type of the AST.
   * @param name The name of the pre-parsed non-terminal.
   * @param position The position of the AST.
   */
  public PreParsedNonTerminal(E type, String name, Position position, ExtendedData extendedData)
  {
    this.type = type;
    this.name = name;
    this.position = position;
    this.extendedData = extendedData;

    assert invariants();
  }

  /**
   * Checks the invariants of the object.
   *
   * @return <code>true</code>.
   */
  private boolean invariants()
  {
      assert type != null;
      assert name != null;
      assert position != null;

      return true;
  }

  @Override
  public Position getPosition()
  {
    return position;
  }

  @Override
  public List<IAST<E>> getChildren()
  {
    return new ArrayList<IAST<E>>();
  }

  @Override
  public E getType()
  {
    return type;
  }

  @Override
  public String childrenAsString()
  {
    return "";
  }

  @Override
  public void acceptASTVisitor(IASTVisitor visitor)
  {
    visitor.visitPreParsedNonTerminal(this);

  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public ExtendedData getExtendedData()
  {
    return extendedData;
  }

}
