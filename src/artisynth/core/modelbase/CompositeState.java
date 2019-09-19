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
import java.io.PrintWriter;
import java.util.*;

import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scan;

public class CompositeState implements ComponentState {
   private static final long serialVersionUID = 1L;
   protected ArrayList<ComponentState> myStates;
   protected boolean myAnnotatedP = false;

   public CompositeState() {
      myStates = new ArrayList<ComponentState>();
   }

   public CompositeState (boolean annotated) {
      myStates = new ArrayList<ComponentState>();
      myAnnotatedP = annotated;
   }

   public CompositeState (int capacity) {
      myStates = new ArrayList<ComponentState>(capacity);
   }

   public Iterator<ComponentState> getStates() {
      return myStates.iterator();
   }

   public ComponentState getState (int i) {
      return myStates.get (i);
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
   public boolean equals (ComponentState state, StringBuilder msg) {
      if (state instanceof CompositeState) {
         ArrayList<ComponentState> otherStates = ((CompositeState)state).myStates;
         if (myStates.size() != otherStates.size()) {
            if (msg != null) {
               msg.append (
                  "num states = "+myStates.size()+" vs. "+otherStates.size()+"\n");
            }
            return false;
         }
         for (int i=0; i<myStates.size(); i++) {
            ComponentState substate = myStates.get(i);
            ComponentState otherSubstate = otherStates.get(i);
            if ((substate == null) != (otherSubstate == null)) {
               if (msg != null) {
                  msg.append (
                     "substate="+substate+" other.substate="+otherSubstate+"\n");
               }
               return false;
            }
            if (substate != null && !substate.equals (otherSubstate, msg)) {
               if (msg != null) {
                  msg.append ("substate "+i+" not equal\n");
               }               
               return false;
            }
         }
         return true;
      }
      else {
         if (msg != null) {
            msg.append ("other state is not a CompositeState\n");
         }
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
         substate.writeBinary (dos);
      }
   }
   
   private ComponentState createSubState (Class<?> clazz) {
      ComponentState substate = null;
      try {
         substate = (ComponentState)clazz.newInstance();
      }
      catch (ClassCastException e) {
         throw new IllegalStateException (
            "Class "+clazz.getName()+" not an instance of ComponentState");
      }
      catch (Exception e) {
         throw new IllegalStateException (
            "Class "+clazz.getName()+" cannot be instantiated");
      }
      if (isAnnotated()) {
         substate.setAnnotated (true);
      }
      return substate;
   }
   
   public void readBinary (DataInputStream dis) throws IOException {
      int numsub = dis.readInt();
      while (myStates.size() < numsub) {
         myStates.add (null);
      }
      while (myStates.size() > numsub) {
         myStates.remove (myStates.size()-1);
      }
      //setAnnotated (false);
      for (int i=0; i<numsub; i++) {
         String className = readString (dis);
         Class<?> clazz = null;
         try {
            clazz = Class.forName (className);
         }
         catch (Exception e) {
            throw new IllegalStateException (
               "Class "+className+" not found");
         }
         ComponentState substate = myStates.get(i);
         if (substate == null || substate.getClass() != clazz) {
            substate = createSubState (clazz);
            myStates.set (i, substate);
         }
         substate.readBinary (dis);
      }
   }

   public boolean isWritable() {
      return true;
   }
   
   public void write (PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {
      
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.println ("[");
      for (ComponentState substate : myStates) {
         pw.print (substate.getClass().getName()+" ");
         substate.write (pw, fmt, ref);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myStates.clear();
      setAnnotated (false);
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         Class<?> clazz = Scan.scanClass (rtok);
         ComponentState substate = createSubState (clazz);
         substate.scan (rtok, ref);
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
      state.myAnnotatedP = myAnnotatedP;
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
         System.out.println (substate);
         if (substate instanceof CompositeState) {
            ((CompositeState)substate).printState (indent + 2);
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isAnnotated () {
      return myAnnotatedP;
   }  
   
   /**
    * {@inheritDoc}
    */
   public void setAnnotated (boolean annotated) {
      myAnnotatedP = annotated;
      for (ComponentState substate : myStates) {
         substate.setAnnotated (annotated);
      }
   }
}
