package artisynth.core.materials;

import java.io.*;
import java.util.*;

import artisynth.core.util.ScalableUnits;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.PropertyChangeEvent;

import maspack.properties.*;
import maspack.util.*;

public abstract class MaterialBase
   implements CompositeProperty, Scannable, ScalableUnits, Clonable {

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

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {

      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void scan (ReaderTokenizer rtok, Object ref) 
      throws IOException {

      getAllPropertyInfo().setDefaultValues (this);
      getAllPropertyInfo().setDefaultModes (this);
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!getAllPropertyInfo().scanProp (this, rtok)) {
            throw new IOException ("unexpected input: " + rtok);
         }
      }
   }

   public static MaterialBase updateMaterial (
      HasProperties comp, String matName, 
      MaterialBase oldMat, MaterialBase newMat) {
      
      if (comp.getAllPropertyInfo().get(matName) == null) {
         throw new IllegalArgumentException (
            "component does not contain a '"+matName+"' property");
      }
      if (newMat != null) {
         newMat = newMat.clone();
         PropertyUtils.updateCompositeProperty (comp, matName, null, newMat);
      }
      else if (oldMat != null) {
         PropertyUtils.updateCompositeProperty (
            comp, matName, oldMat, null);
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

   public static boolean tangentSymmetryChanged (
      MaterialBase mat1, MaterialBase mat2) {
      boolean sym1 = mat1 == null || mat1.hasSymmetricTangent();
      boolean sym2 = mat2 == null || mat2.hasSymmetricTangent();
      return sym1 != sym2;
   }

   protected void notifyHostOfPropertyChange (String name) {
      if (myPropHost instanceof PropertyChangeListener) {
         ((PropertyChangeListener)myPropHost).propertyChanged (
            new PropertyChangeEvent (this, name));
      }
   }
}
   
   
