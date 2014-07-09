/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.ipopt;


public class IpoptTest
{
   boolean debug = true;
   public static void main(String[] args)
   {
      IpoptExample ipopt;
      try
      {
	 ipopt = new IpoptExample();
	 ipopt.create();
	 ipopt.solve();
      }
      catch (UnsupportedOperationException e)
      {
	 System.out.println("IpoptTest: unable to open IpoptInterface.");
	 System.err.println(e.getMessage());
	 throw e; 
      }
   }
 
}
