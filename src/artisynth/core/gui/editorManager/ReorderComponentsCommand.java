/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import artisynth.core.modelbase.*;
import maspack.util.*;
import java.util.*;

public class ReorderComponentsCommand implements Command {
   private String myName;
   private ComponentList<?> myList;
   private int[] myIndices;

   public ReorderComponentsCommand (
      String name, ComponentList<?> list, int[] indices) {
      myName = name;
      myList = list;
      myIndices = Arrays.copyOf(indices, indices.length);
   }

   public void execute() {
      myList.reorderComponents (myIndices);
   }

   public void undo() {
      int[] revIdxs = new int[myIndices.length];
      for (int i=0; i<myIndices.length; i++) {
         revIdxs[myIndices[i]] = i;
      }
      myList.reorderComponents (revIdxs);
   }

   public String getName() {
      return myName;
   }
}
