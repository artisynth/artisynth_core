/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.workspace;

import artisynth.core.driver.Main;
import artisynth.core.moviemaker.MovieMaker;
import artisynth.core.probes.OutputProbe;

public class RenderProbe extends OutputProbe {
   
   private Main myMain;

   public RenderProbe (Main main, double interval) {
      super();
      setStopTime (Double.POSITIVE_INFINITY);
      setUpdateInterval (interval);
      myMain = main;
   }

   public void apply (double t) {

      if (myMain != null) {
         myMain.rerender();
         MovieMaker movieMaker = myMain.getMovieMaker();

         if (movieMaker.isGrabbing ()) {
            System.out.println ("grab at t=" + t);
            try {
               movieMaker.grab ();
            }
            catch (Exception e) {
               e.printStackTrace();
               System.out.println ("ERROR grabbing movie frame");
            }
         }
      }
   }
}
