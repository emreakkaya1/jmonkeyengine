/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.g3d.scene;

import java.nio.Buffer;

/**
 *
 * @author lex
 */
public abstract class IndexBuffer {
    public abstract int get(int i);
    public abstract void put(int i, int value);
    public abstract int size();
    public abstract Buffer getBuffer();
}