/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/** 
 * This interface provides a fix to the Java Cloneable interface in
 * that any class implementing it is guaranteed to have a public
 * <code>clone</code> method.
 */
public interface Clonable extends Cloneable {
   
   public Object clone() throws CloneNotSupportedException;

}