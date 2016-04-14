/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), C Antonio Sanchez
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;

import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.LineStyle;
import maspack.properties.CompositeProperty;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.ArraySupport;
import maspack.util.Clonable;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;

public class RenderMappings implements CompositeProperty, Scannable, Clonable {

   PropertyInfo myPropInfo;
   HasProperties myPropHost;

   public PropertyInfo getPropertyInfo() {
      return myPropInfo;
   }

   public void setPropertyInfo (PropertyInfo info) {
      myPropInfo = info;
   }

   public HasProperties getPropertyHost() {
      return myPropHost;
   }

   public void setPropertyHost (HasProperties newParent) {
      myPropHost = newParent;
   }

   protected TextureMapProps myTextureMapProps;
   protected static TextureMapProps defaultTextureMapProps = null;
   
   protected NormalMapProps myNormalMapProps;
   protected static NormalMapProps defaultNormalMapProps = null;

   protected BumpMapProps myBumpMapProps;
   protected static BumpMapProps defaultBumpMapProps = null;
   
   public static PropertyList myProps = new PropertyList (RenderMappings.class);

   protected boolean myTextureMapPropsInactive = true;
   protected boolean myNormalMapPropsInactive = true;
   protected boolean myBumpMapPropsInactive = true;

   static {
      myProps.add (
         "textureMapping",
         "diffuse texture mapping properties", defaultTextureMapProps);
      myProps.add (
         "normalMapping",
         "normal texture mapping properties", defaultNormalMapProps);
      myProps.add (
         "bumpMapping",
         "bump texture mapping properties", defaultBumpMapProps);
   }
   
   public RenderMappings() {
      setDefaultModes();
      setDefaultValues();
   }

   public RenderMappings (RenderMappings props) {
      this();
      set (props);
   }


   public TextureMapProps getTextureMapping() {
      return myTextureMapProps;
   }

   public void setTextureMapping (TextureMapProps props) {
      if (getAllPropertyInfo().get ("textureMapping") == null) {
         return;
      }
      if (props == null) {
         PropertyUtils.updateCompositeProperty (
            this, "textureMapping", myTextureMapProps, null);
         myTextureMapProps = null;
      }
      else {
         if (myTextureMapProps == null) {
            myTextureMapProps = new TextureMapProps();
            myTextureMapProps.set (props);
            PropertyUtils.updateCompositeProperty (
               this, "textureMapping", null, myTextureMapProps);
         }
         else {
            myTextureMapProps.set (props);
            PropertyUtils.updateCompositeProperty (myTextureMapProps);
         }
      }
   }
   
   public NormalMapProps getNormalMapping() {
      return myNormalMapProps;
   }

   public void setNormalMapping (NormalMapProps props) {
      if (getAllPropertyInfo().get ("normalMapping") == null) {
         return;
      }
      if (props == null) {
         PropertyUtils.updateCompositeProperty (
            this, "normalMapping", myNormalMapProps, null);
         myNormalMapProps = null;
      }
      else {
         if (myNormalMapProps == null) {
            myNormalMapProps = new NormalMapProps();
            myNormalMapProps.set (props);
            PropertyUtils.updateCompositeProperty (
               this, "normalMapping", null, myNormalMapProps);
         }
         else {
            myNormalMapProps.set (props);
            PropertyUtils.updateCompositeProperty (myNormalMapProps);
         }
      }
   }
   
   public BumpMapProps getBumpMapping() {
      return myBumpMapProps;
   }

   public void setBumpMapping (BumpMapProps props) {
      if (getAllPropertyInfo().get ("bumpMapping") == null) {
         return;
      }
      if (props == null) {
         PropertyUtils.updateCompositeProperty (
            this, "bumpMapping", myBumpMapProps, null);
         myNormalMapProps = null;
      }
      else {
         if (myBumpMapProps == null) {
            myBumpMapProps = new BumpMapProps();
            myBumpMapProps.set (props);
            PropertyUtils.updateCompositeProperty (
               this, "bumpMapping", null, myBumpMapProps);
         }
         else {
            myBumpMapProps.set (props);
            PropertyUtils.updateCompositeProperty (myBumpMapProps);
         }
      }
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
   }

   protected void setDefaultValues() {
      myTextureMapProps = defaultTextureMapProps;
      myNormalMapProps = defaultNormalMapProps;
      myBumpMapProps = defaultBumpMapProps;
   }

   public void set (RenderMappings r) {
      myTextureMapPropsInactive = r.myTextureMapPropsInactive;
      setTextureMapping (r.myTextureMapProps);
      myNormalMapPropsInactive = r.myNormalMapPropsInactive;
      setNormalMapping (r.myNormalMapProps);
      myBumpMapPropsInactive = r.myBumpMapPropsInactive;
      setBumpMapping (r.myBumpMapProps);
   }

   protected boolean equalsOrBothNull(Object a, Object b) {
      if (a == b) {
         return true;
      } else if (a == null || b == null) {
         return false;
      }
      return a.equals (b);
   }
   
   public boolean equals (RenderMappings r) {
      if (myTextureMapPropsInactive != r.myTextureMapPropsInactive) {
         return false;
      }
      else if (!myTextureMapPropsInactive &&
               !equalsOrBothNull (r.myTextureMapProps, myTextureMapProps)) {
         return false;
      }
      if (myNormalMapPropsInactive != r.myNormalMapPropsInactive) {
         return false;
      }
      else if (!myNormalMapPropsInactive &&
               !equalsOrBothNull(myNormalMapProps, r.myNormalMapProps)) {
         return false;
      }
      if (myBumpMapPropsInactive != r.myBumpMapPropsInactive) {
         return false;
      }
      else if (!myBumpMapPropsInactive &&
               !equalsOrBothNull(myBumpMapProps, r.myBumpMapProps)) {
         return false;
      }
      return true;
   }

   public static boolean equals (RenderMappings r1, RenderMappings r2) {
      if (r1 == null && r2 == null) {
         return true;
      }
      else if (r1 != null && r2 != null) {
         return r1.equals (r2);
      }
      else {
         return false;
      }
   }

   public boolean equals (Object obj) {
      if (obj instanceof RenderMappings) {
         return equals ((RenderMappings)obj);
      }
      else {
         return false;
      }
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

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      setDefaultValues();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!getAllPropertyInfo().scanProp (this, rtok)) {
            throw new IOException ("unexpected input: " + rtok);
         }
      }
   }

   private static RenderMappings initRenderMappings (
      RenderMappings props, HasProperties host) {
      if (host != null) {
         PropertyUtils.updateInheritedProperties (props, host, "renderMappings");
      }
      return props;
   }

   /**
    * Creates a new RenderMappings. If the supplied host is non-null, then it is
    * used to initialize any inherited properties. The property name associated
    * with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * @return render properties appropriate for lines
    */
   public static RenderMappings createRenderMappings (HasProperties host) {
      return initRenderMappings (new RenderMappings(), host);
   }

   /**
    * Scales the properties that are associated with distance. This includes the
    * sphere radius and cylinder radius.
    * 
    * @param s
    * scale factor
    */
   public void scaleDistance (double s) {
   }

   public String toString() {
      StringBuffer buf = new StringBuffer (1024);
      buf.append ("TextureMapProps=" + myTextureMapProps + ", ");
      buf.append ("NormalMapProps=" + myNormalMapProps + ", ");
      buf.append ("BumpMapProps=" + myBumpMapProps + ", ");
      return buf.toString();
   }
   
   public RenderMappings copy() {
      return clone();
   }

   public RenderMappings clone() {
      RenderMappings props = null;
      try {
         props = (RenderMappings)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException (
            "cannot clone super in RenderMappings");
      }

      props.myPropInfo = null;
      props.myPropHost = null;
      // XXX what about mapping props?
      
      props.set(this);
      return props;
   }
 
   protected static RenderMappings createAndAssignProps (HasRenderMappings r) {
      RenderMappings props = new RenderMappings();
      if (r.getRenderMappings() != null) {
         props.set (r.getRenderMappings());
      }
      return props;
   }


   private static TextureMapProps createAndAssignTextureMapProps (
      RenderMappings props) {
      TextureMapProps tprops = props.getTextureMapping();
      if (tprops == null) {
         tprops = new TextureMapProps();
         props.setTextureMapping (tprops);
      }
      return tprops;
   }
   
   private static NormalMapProps createAndAssignNormalMapProps (
      RenderMappings props) {
      NormalMapProps tprops = props.getNormalMapping();
      if (tprops == null) {
         tprops = new NormalMapProps();
         props.setNormalMapping (tprops);
      }
      return tprops;
   }

   private static BumpMapProps createAndAssignBumpMapProps (
      RenderMappings props) {
      BumpMapProps tprops = props.getBumpMapping();
      if (tprops == null) {
         tprops = new BumpMapProps();
         props.setBumpMapping (tprops);
      }
      return tprops;
   }

   public static void setTextureEnabled (HasRenderMappings r, boolean enabled) {
      RenderMappings props = createAndAssignProps (r);
      TextureMapProps tprops = createAndAssignTextureMapProps(props);
      tprops.setEnabled (enabled);
      r.setRenderMappings (props);
   }

   public static void setTextureEnabledMode (
      HasRenderMappings r, PropertyMode mode) {
      RenderMappings props = createAndAssignProps (r);
      TextureMapProps tprops = createAndAssignTextureMapProps (props);
      tprops.setEnabledMode (mode);
      r.setRenderMappings (props);
   }

   public static void setTextureFileName (
      HasRenderMappings r, String fileName) {
      RenderMappings props = createAndAssignProps (r);
      TextureMapProps tprops = createAndAssignTextureMapProps (props);
      tprops.setFileName (fileName);
      r.setRenderMappings (props);
   }

   public static void setTextureFileNameMode (
      HasRenderMappings r, PropertyMode mode) {
      RenderMappings props = createAndAssignProps (r);
      TextureMapProps tprops = createAndAssignTextureMapProps (props);
      tprops.setFileNameMode (mode);
      r.setRenderMappings (props);
   }
   
   public static void setNormalMapEnabled (
      HasRenderMappings r, boolean enabled) {
      RenderMappings props = createAndAssignProps (r);
      NormalMapProps tprops = createAndAssignNormalMapProps(props);
      tprops.setEnabled (enabled);
      r.setRenderMappings (props);
   }

   public static void setNormalMapEnabledMode (
      HasRenderMappings r, PropertyMode mode) {
      RenderMappings props = createAndAssignProps (r);
      NormalMapProps tprops = createAndAssignNormalMapProps (props);
      tprops.setEnabledMode (mode);
      r.setRenderMappings (props);
   }

   public static void setNormalMapFileName (
      HasRenderMappings r, String fileName) {
      RenderMappings props = createAndAssignProps (r);
      NormalMapProps tprops = createAndAssignNormalMapProps (props);
      tprops.setFileName (fileName);
      r.setRenderMappings (props);
   }

   public static void setNormalMapFileNameMode (
      HasRenderMappings r, PropertyMode mode) {
      RenderMappings props = createAndAssignProps (r);
      NormalMapProps tprops = createAndAssignNormalMapProps (props);
      tprops.setFileNameMode (mode);
      r.setRenderMappings (props);
   }
   
   public static void setBumpMapEnabled (
      HasRenderMappings r, boolean enabled) {
      RenderMappings props = createAndAssignProps (r);
      BumpMapProps tprops = createAndAssignBumpMapProps(props);
      tprops.setEnabled (enabled);
      r.setRenderMappings (props);
   }

   public static void setBumpMapEnabledMode (
      HasRenderMappings r, PropertyMode mode) {
      RenderMappings props = createAndAssignProps (r);
      BumpMapProps tprops = createAndAssignBumpMapProps (props);
      tprops.setEnabledMode (mode);
      r.setRenderMappings (props);
   }

   public static void setBumpMapFileName (
      HasRenderMappings r, String fileName) {
      RenderMappings props = createAndAssignProps (r);
      BumpMapProps tprops = createAndAssignBumpMapProps (props);
      tprops.setFileName (fileName);
      r.setRenderMappings (props);
   }

   public static void setBumpMapFileNameMode (
      HasRenderMappings r, PropertyMode mode) {
      RenderMappings props = createAndAssignProps (r);
      BumpMapProps tprops = createAndAssignBumpMapProps (props);
      tprops.setFileNameMode (mode);
      r.setRenderMappings (props);
   }
   
   /**
    * Creates a set of render properties and initializes for use with
    * the specified host
    * 
    * @param host host object that has render properties
    * @param props the properties to copy and assign
    * @return the created render properties
    */
   public static RenderMappings createAndInitRenderMappings(
      HasRenderMappings host, 
      RenderMappings props) {
      if (props == null) {
         host.setRenderMappings(null);
      } else {
         props = props.clone(); // create clone
            
         // propagate values
         if (host instanceof HasProperties) {
            RenderMappings.initRenderMappings(props, (HasProperties)host);
            PropertyUtils.updateCompositeProperty (
               (HasProperties)host, "renderMappings", null, props);
         }
      }
      return props;
   }

   public static RenderMappings updateRenderMappings (
      HasProperties comp,
      RenderMappings mappings, RenderMappings setMappings) {

      if (mappings == null ||
          mappings == setMappings) {
         mappings = new RenderMappings();
         mappings.set (setMappings);
         PropertyUtils.updateCompositeProperty (
            comp, "renderMappings", null, mappings);
      }
      else {
         mappings.set (setMappings);
         PropertyUtils.updateCompositeProperty (mappings);
      }
      return mappings;
   }

}
