package artisynth.core.materials;

import java.io.*;
import java.util.*;

import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;
import maspack.properties.*;
import maspack.util.*;

public abstract class MaterialBase
   implements CompositeProperty, PostScannable, ScalableUnits, Clonable,
              HasMaterialState {

   protected PropertyInfo myPropInfo;
   protected HasProperties myPropHost;

   public PropertyInfo getPropertyInfo ()
    { 
      return myPropInfo;
    }

   public void setPropertyInfo (PropertyInfo info)
    {
      myPropInfo = info;
    }

   public HasProperties getPropertyHost()
    {
      return myPropHost;
    }

   public void setPropertyHost (HasProperties newParent)
    {
      myPropHost = newParent;
    }

   public static PropertyList myProps = new PropertyList(MaterialBase.class);

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public boolean hasProperty (String name) {
      return getAllPropertyInfo().get(name) != null;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MaterialBase clone() {
      MaterialBase mat = null;
      try 
       { mat = (MaterialBase)super.clone();
       }
      catch (CloneNotSupportedException e)
       { throw new InternalErrorException (
            "cannot clone super in MaterialBase");
       }      
      return mat;
   }

   public boolean isWritable() {
      return true;
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {
      CompositeComponent ancestor = ComponentUtils.castRefToAncestor(ref);
      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      writeItems (pw, fmt, ancestor);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      // if keyword is a property name, try scanning that
      rtok.nextToken();
      if (ScanWriteUtils.scanProperty (rtok, this, tokens)) {
         return true;
      }
      rtok.pushBack();
      return false;
   }

   public void scan (ReaderTokenizer rtok, Object ref) 
      throws IOException {
      Deque<ScanToken> tokens = (Deque<ScanToken>)ref;
      if (tokens == null) {
         tokens = new ArrayDeque<> ();
      }
      getAllPropertyInfo().setDefaultValues (this);
      getAllPropertyInfo().setDefaultModes (this);
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN);
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!scanItem (rtok, tokens)) {
            throw new IOException ("unexpected input: " + rtok);
         }
      }
      tokens.offer (ScanToken.END); // terminator token
   }

    protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (ScanWriteUtils.postscanPropertyValue (tokens, ancestor)) {
         return true;
      }
      return false;
   }  

   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      ScanWriteUtils.postscanBeginToken (tokens, this);
      while (tokens.peek() != ScanToken.END) {
         if (!postscanItem (tokens, ancestor)) {
            throw new IOException (
               "Unexpected token for " + 
               ComponentUtils.getDiagnosticName(this) + ": " + tokens.poll());
         }
      }      
      tokens.poll(); // eat END token      
   }

   public static <T extends MaterialBase> T updateMaterial (
      HasProperties comp, String matName, 
      MaterialBase oldMat, T newMat) {
      
      if (comp.getAllPropertyInfo().get(matName) == null) {
         throw new IllegalArgumentException (
            "component does not contain a '"+matName+"' property");
      }
      if (newMat != null) {
         newMat = (T)newMat.clone();
         PropertyUtils.updateCompositeProperty (comp, matName, null, newMat);
      }
      else if (oldMat != null) {
         PropertyUtils.updateCompositeProperty (comp, matName, oldMat, null);
      }
      return newMat;
   }     

   /** 
    * Returns true if the tangent matrix for this material is symmetric.  While
    * this is normally true, some special materials (such as those whose stress
    * is not derived from a conservative energy funtion) may have a non-symmetric
    * tangent, in which case this function should be overridden to return false.
    *
    * @return true if the tangent matrix for this material is symmetric.
    */
   public boolean hasSymmetricTangent() {
      return true;
   }

   public void scaleDistance (double s) {
      // TODO material specific scaling in sub-classes
   }

   public void scaleMass (double s) {
      // TODO material specific scaling in sub-classes
   }

   protected static boolean tangentSymmetryChanged (
      MaterialBase mat1, MaterialBase mat2) {
      boolean sym1 = (mat1 == null || mat1.hasSymmetricTangent());
      boolean sym2 = (mat2 == null || mat2.hasSymmetricTangent());
      return sym1 != sym2;
   }

   protected static boolean stateChanged (
      MaterialBase mat1, MaterialBase mat2) {
      
      boolean hasState1 = (mat1 != null && mat1.hasState());
      boolean hasState2 = (mat2 != null && mat2.hasState());
      if (hasState1 || hasState2) {
         if (hasState1 && hasState2) {
            // if both materials have state, then state will be assumed to be
            // unchanged only if both have the same class
            return mat1.getClass() != mat2.getClass();
         }
         else {
            // one material has state and the other doesn't means state changed
            return true;
         }
      }
      else {
         return false;
      }
   }

   public static MaterialChangeEvent symmetryOrStateChanged (
      String name, MaterialBase mat1, MaterialBase mat2) {
      
      boolean stateChanged = stateChanged (mat1, mat2);
      boolean tangentSymmetryChanged = tangentSymmetryChanged (mat1, mat2);
      
      if (stateChanged || tangentSymmetryChanged) {
         return new MaterialChangeEvent (
            null, name, stateChanged, tangentSymmetryChanged);
      }
      else {
         return null;
      }
   }

   protected void notifyHostOfPropertyChange (
      String name, MaterialBase mat1, MaterialBase mat2) {
      
      if (myPropHost instanceof PropertyChangeListener) {
         boolean stateChanged = stateChanged (mat1, mat2);
         boolean tangentSymmetryChanged = tangentSymmetryChanged (mat1, mat2);
         MaterialChangeEvent e = 
            new MaterialChangeEvent (
               this, name, stateChanged, tangentSymmetryChanged);
         ((PropertyChangeListener)myPropHost).propertyChanged (e);
      }
   }

   protected void notifyHostOfPropertyChange (String name) {

      if (myPropHost instanceof PropertyChangeListener) {
         ((PropertyChangeListener)myPropHost).propertyChanged (
            new MaterialChangeEvent (this, name, false, false));
      }
   }

   public boolean hasState() {
      return false;
   }

   public MaterialStateObject createStateObject() {
      return null;
   }

   public void advanceState (MaterialStateObject state, double t0, double t1) {
   }
}
   
   


