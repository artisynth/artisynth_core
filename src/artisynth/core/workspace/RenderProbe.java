/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.workspace;

import artisynth.core.driver.Main;
import artisynth.core.driver.ViewerManager;
import artisynth.core.moviemaker.MovieMaker;
import artisynth.core.probes.OutputProbe;
import artisynth.core.util.TimeBase;

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

         if (movieMaker.isGrabbing() && !movieMaker.isStopRequestPending()) {
            movieMaker.println ("grab at t=" + t);
            ViewerManager vm = myMain.getViewerManager();
            if (vm != null) {
               // HACK. Do this to call prerender *before* the movie maker grab
               // method gets invoked. Otherwise, movie frames may not reflect
               // the most up-to-date state. This will slow down the movie
               // making (by requiring an extra prerender()), but otherwise
               // should work.
               vm.render();
            }
            try {
               movieMaker.grab ();
            }
            catch (Exception e) {
               e.printStackTrace();
               movieMaker.println ("\nERROR grabbing movie frame");
            }
            if (movieMaker.getRequestedStopTime() > 0) {
               // if explicit stop time set, issue a stop request time
               // equals or exceeds this time
               if (TimeBase.compare (t, movieMaker.getRequestedStopTime()) >= 0) {
                  movieMaker.requestStop();
               }
            }
         }
      }
   }
}
