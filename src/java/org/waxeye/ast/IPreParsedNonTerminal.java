/*
 * Waxeye Parser Generator
 * www.waxeye.org
 * Copyright (C) 2008-2025 Nico Verwer
 * Licensed under the MIT license. See 'LICENSE' for details.
 */
package org.waxeye.ast;

/**
 * An AST node with a pre-parsed non-terminal.
 *
 * @author Nico Verwer
 */
public interface IPreParsedNonTerminal<ExtendedData>
{
  /**
   * Gets the pre-parsed non-terminal name.
   *
   * @return The pre-parsed non-terminal name.
   */
  String getName();

  /**
   * Gets the extended data associated with this pre-parsed non-terminal.
   *
   * @return The extended data associated with this pre-parsed non-terminal.
   */
  ExtendedData getExtendedData();

}
