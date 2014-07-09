/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

public interface FastMarcherUtility {

   public double distance(int p1Idx, int p2Idx);
   public int getNeighbour(int pIdx, int neighbourIdx);
   public int getNumNeighbours(int pIdx);
   
}
