/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * General interface that identifies Probes, Controllers, and Monitors.
 */
public interface ModelAgent extends ModelComponent, HasState {

   public void initialize (double t0);
   
   public void setModel (Model m);

   public Model getModel();

   public void dispose();
   
   /**
    * Returns true if this agent is active.
    * 
    * @return true if active
    */
   public boolean isActive();
   
}
