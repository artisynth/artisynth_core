package maspack.properties;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a basic CompositeProperty, supporting writing of all
 * properties and of copying of propeties/modes during clone
 * @author Antonio
 *
 */
public class CompositePropertyBase implements CompositeProperty {

   PropertyInfo myPropInfo;
   HasProperties myPropHost;

   public PropertyInfo getPropertyInfo() {
      return myPropInfo;
   }

   public void setPropertyInfo(PropertyInfo info) {
      myPropInfo = info;
   }

   public HasProperties getPropertyHost() {
      return myPropHost;
   }

   public void setPropertyHost(HasProperties newParent) {
      myPropHost = newParent;
   }

   public static PropertyList myProps = new PropertyList(
      CompositePropertyBase.class);

   public Property getProperty(String name) {
      return PropertyList.getProperty(name, this);
   }

   public boolean hasProperty(String name) {
      return getAllPropertyInfo().get(name) != null;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public boolean isWritable() {
      return true;
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {

      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ref);
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
   
   @Override
   public CompositePropertyBase clone() {
      CompositePropertyBase copy;
      
      // set all properties and modes
      try {
         copy = (CompositePropertyBase)super.clone();
         PropertyList info = getAllPropertyInfo();
         for (int i=0; i<info.size(); ++i) {
            PropertyDesc desc = info.get(i);
            desc.setValue(copy, desc.getValue(this));
            if (desc.isInheritable()) {
               desc.setMode(copy, desc.getMode(this));
            }
         }
         
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
      return copy;
   }
   
}
