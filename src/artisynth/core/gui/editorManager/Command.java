/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import artisynth.core.modelbase.*;

public interface Command {
   /**
    * Executes this command.
    */
   public void execute();

   /**
    * Undoes the result of executing this command.
    */
   public void undo();

   /**
    * Gets the name associated with this command. This name will be used in
    * creating entries in the undo menu.
    * 
    * @return name associated with this command
    */
   public String getName();

   // /**
   // * Sets a state to be associated with this command. This is used by the
   // undo
   // * manager whenever model state is saved before command execution and
   // * restored after undo. The caller should not modify the state after
   // * invoking this method, and therefore there should be no need to make a
   // * defensive copy.
   // *
   // * @param state state to be stored with this command
   // */
   // public void setState (CompositeState state);

   // /**
   // * Returns the most recent value of state that was set with {@link
   // #setState
   // * setState}.
   // *
   // * @return recent value of state
   // */
   // public CompositeState getState();

}
