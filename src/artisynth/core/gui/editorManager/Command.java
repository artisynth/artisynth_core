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
}
