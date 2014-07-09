/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class CompositeState implements ComponentState {
   private static final long serialVersionUID = 1L;
   protected ArrayList<ComponentState> myStates;
   // optional list of sub-components used to facilitate list merging
   protected ArrayList<Object> myComps; 

   public CompositeState() {
      myStates = new ArrayList<ComponentState>();
   }

//   public CompositeState (boolean hasComps) {
//      myStates = new ArrayList<ComponentState>();
//      if (hasComps) {
//         myComps = new ArrayList<Object>();
//      }
//      else {
//         myComps = null;
//      }      
//   }

   public CompositeState (int capacity) {
      myStates = new ArrayList<ComponentState>(capacity);
   }

   public Iterator<ComponentState> getStates() {
      return myStates.iterator();
   }

   public ComponentState getState (int i) {
      return myStates.get (i);
   }

   public void addComponents (Collection<? extends Object> comps) {
      if (myComps == null) {
         myComps = new ArrayList<Object>();
      }
      myComps.addAll (comps);
   }

   public void addComponent (Object comp) {
      if (myComps == null) {
         myComps = new ArrayList<Object>();
      }
      myComps.add (comp);
   }

   public ArrayList<Object> getComponents() {
      return myComps;
   }

   public int numComponents() {
      return myComps != null ? myComps.size() : 0;
   }

   public void addState (ComponentState state) {
      myStates.add (state);
   }

   public void removeState (ComponentState state) {
      myStates.remove (state);
   }

   public int numSubStates() {
      return myStates.size();
   }

   public void clear() {
      myStates.clear();
   }
   
   /** 
    * {@inheritDoc}
    */
   public boolean equals (ComponentState state) {
      if (state instanceof CompositeState) {
         ArrayList<ComponentState> otherStates = ((CompositeState)state).myStates;
         if (otherStates.size() != myStates.size()) {
            System.out.println (
               "size=" + myStates.size() + " other.size=" + otherStates.size());
            return false;
         }
         for (int i=0; i<myStates.size(); i++) {
            ComponentState substate = myStates.get(i);
            ComponentState otherSubstate = otherStates.get(i);
            if ((substate == null) != (otherSubstate == null)) {
               System.out.println (
                  "substate="+substate+" other.substate="+otherSubstate);
               return false;
            }
            if (substate != null && !substate.equals (otherSubstate)) {
               System.out.println ("substates not equal");
               return false;
            }
         }
         return true;
      }
      else {
         System.out.println ("not composite state");
         return false;
      }
   }


   private void writeString (DataOutputStream dos, String str)
      throws IOException {
      dos.writeInt (str.length());
      for (int i=0; i<str.length(); i++) {
         dos.writeChar (str.charAt (i));
      }
   }

   private String readString (DataInputStream dis)
      throws IOException {
      int len = dis.readInt ();
      StringBuilder buf = new StringBuilder();
      for (int i=0; i<len; i++) {
         buf.append (dis.readChar());
      }
      return buf.toString();
   }

   public void writeBinary (DataOutputStream dos) throws IOException {
      dos.writeInt (myStates.size());
      for (ComponentState substate : myStates) {
         writeString (dos, substate.getClass().getName());
         System.out.println ("writing " + substate.getClass().getName());
         substate.writeBinary (dos);
      }
   }

   public void readBinary (DataInputStream dis) throws IOException {
      int numsub = dis.readInt();
      myStates.clear();
      for (int i=0; i<numsub; i++) {
         String className = readString (dis);
         Class cls = null;
         try {
            cls = Class.forName (className);
         }
         catch (Exception e) {
            throw new IllegalStateException (
               "Class "+className+" not found");
         }
         ComponentState substate = null;
         try {
            substate = (ComponentState)cls.newInstance();
         }
         catch (ClassCastException e) {
            throw new IllegalStateException (
               "Class "+className+" not an instance of ComponentState");
         }
         catch (Exception e) {
            throw new IllegalStateException (
               "Class "+className+" cannot be instantiated");
         }
         substate.readBinary (dis);
         myStates.add (substate);
      }
   }

   public void set (ComponentState stateToCopy) {
      if (!(stateToCopy instanceof CompositeState)) {
         throw new IllegalArgumentException (
            "state to copy is not a CompositeState");
      }
      set ((CompositeState)stateToCopy);
   }

   public void set (CompositeState state) {
      if (state.numSubStates() != numSubStates()) {
         throw new IllegalArgumentException ("new state has "
         + state.numSubStates() + " sub-states vs. " + numSubStates());
      }
      for (int i = 0; i < numSubStates(); i++) {
         myStates.get (i).set (state.myStates.get (i));
      }
   }

   public CompositeState duplicate() {
      CompositeState state = new CompositeState();
      state.myStates = new ArrayList<ComponentState>();
      for (ComponentState substate : myStates) {
         state.myStates.add (substate.duplicate());
      }
      return state;
   }
   
   /**
    * Debugging method to print the structure of a composite state
    */
   public void printState (int indent) {
      for (ComponentState substate : myStates) {
         for (int i=0; i<indent; i++) {
            System.out.print (" ");
         }
         System.out.println (substate.getClass());
         if (substate instanceof CompositeState) {
            ((CompositeState)substate).printState (indent + 2);
         }
      }
   }
   
}
