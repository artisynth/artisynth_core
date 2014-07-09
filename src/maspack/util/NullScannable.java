/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * This is a marker interface indicating that the implementing class does not
 * use the <code>ref</code> argument in its
 * {@link maspack.util.Scannable#scan scan} and
 * {@link maspack.util.Scannable#write write} routines.
 */
public interface NullScannable extends Scannable {
}
