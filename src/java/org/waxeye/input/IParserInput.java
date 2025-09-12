/*
 * Waxeye Parser Generator
 * www.waxeye.org
 * Copyright (C) 2008-2010 Orlando Hill
 * Licensed under the MIT license. See 'LICENSE' for details.
 */
package org.waxeye.input;

/**
 * An interface for parser input.
 * This is a version that supports extended data, which is useful for pre-parsed non-terminals.
 *
 * @author Orlando Hill
 * @author Nico Verwer
 */
public interface IParserInput<ExtendedData>
{
    /** The end of the file marker. */
    int EOF = -1;

    /**
     * Gets the next character from the input and moves the position forward 1.
     *
     * @return The next character or EOF if end of input reached.
     */
    int consume();

    /**
     * Gets the next character from the input but maintains the position.
     *
     * @return The next character or EOF if end of input reached.
     */
    int peek();

    /**
     * Gets the position marker in the input.
     *
     * @return Returns the position marker in the input.
     */
    int getPosition();

    /**
     * Sets the position marker of the input to the given value.
     *
     * If the position given is less than 0 then the position is set to 0.
     *
     * @param position The position to set.
     */
    void setPosition(int position);

    /**
     * Gets the extended data associated with the input.
     *
     * @return The extended data associated with the input.
     */
    ExtendedData getExtendedData();

    /**
     * Sets the extended data associated with the input.
     * This should always be done together with a call to setPosition().
     *
     * @param data The extended data to associate with the input.
     */
    void setExtendedData(ExtendedData data);
}
