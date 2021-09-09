/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.util.*;

import artisynth.core.driver.Main;
import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;
import maspack.util.*;

public class UndoManager {

   public interface CommandFilter {
      boolean accept (Command cmd);
   }

   private class CommandStatePair {
      CommandStatePair (Command cmd, CompositeState state) {
         myCmd = cmd;
         myState = state;
      }

      Command myCmd;
      CompositeState myState;
   }

   private LinkedList<CommandStatePair> commands;
   private int myDepth = 10;

   public UndoManager() {
      commands = new LinkedList<CommandStatePair>();
   }

   public void setDepth (int depth) {
      if (depth < 0) {
         throw new IllegalArgumentException ("depth must be non-negative");
      }
      myDepth = depth;
      while (commands.size() > depth) {
         commands.removeFirst();
      }
   }

   public int getDepth() {
      return myDepth;
   }

//   /**
//    * Add a set of commands that have been executed and should be undone
//    * together.
//    * 
//    * @param newCommands
//    * The commands that have been executed.
//    */
//   public void addCommand (ArrayList<Command> newCommands) {
//      commands.add (new CommandStatePair (newCommands, null));
//      if (commands.size() > myDepth) {
//         commands.removeFirst();
//      }
//   }

   /**
    * Add a single command that has been executed and should be undone on it's
    * own.
    * 
    * @param newCommand
    * The command that has been executed.
    */
   public void addCommand (Command newCommand) {
      addCommand (newCommand, null);
   }

   /**
    * Add a single command that has been executed and should be undone on it's
    * own.
    * 
    * @param newCommand
    * The command that has been executed.
    */
   public void addCommand (Command newCommand, CompositeState state) {
      commands.add (new CommandStatePair (newCommand, state));
      if (commands.size() > myDepth) {
         commands.removeFirst();
      }
   }

   /**
    * Undo the most recently executed set of commands.
    */
   public void undoLastCommand() {
      if (commands.size() > 0) {
         CommandStatePair cmdState = commands.removeLast();
         cmdState.myCmd.undo();
         RootModel rootModel = Main.getMain().getRootModel();
         if (rootModel == null) {
            throw new InternalErrorException ("rootModel is null");
         }
         if (cmdState.myState != null) {
            rootModel.setState (cmdState.myState);
            rootModel.initialize (Main.getMain().getTime());
         }
         rootModel.rerender();
      }
   }

   public Command getLastCommand() {
      if (commands.size() > 0) {
         CommandStatePair cmdState = commands.getLast();
         return cmdState.myCmd;
      }
      else {
         return null;
      }
   }

   /**
    * Clear all commands
    */
   public void clearCommands() {
      commands.clear();
   }

   /**
    * Clear all commands indicated by a command filter.
    */
   public void clearCommands (CommandFilter filter) {
      ListIterator<CommandStatePair> li = commands.listIterator();
      while (li.hasNext()) {
         CommandStatePair cmdp = li.next();
         if (filter.accept (cmdp.myCmd)) {
            li.remove();
         }
      }
   }

   public boolean hasCommandToUndo() {
      if (commands.size() > 0) {
         return true;
      }
      return false;
   }

   public void execute (Command cmd) {
      cmd.execute();
      addCommand (cmd, null);
   }

   public CompositeState getModelState() {
      CompositeState state = null;
      RootModel rootModel = Main.getMain().getRootModel();
      if (rootModel != null) {
         state = (CompositeState)rootModel.createState(
            null);
         rootModel.getState (state);
      }
      return state;
   }

   public void saveStateAndExecute (Command cmd) {
      CompositeState state = getModelState();
      cmd.execute();
      addCommand (cmd, state);
   }
}
