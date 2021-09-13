package artisynth.core.driver;

import java.awt.Color;
import javax.swing.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.widgets.*;
import maspack.render.*;
import maspack.render.GL.GLViewer.*;
import maspack.render.GL.GLViewer;

import java.io.IOException;
import java.io.PrintWriter;

import artisynth.core.util.*;

/**
 * Preferences related to the viewer that are specific to OpenGL
 */
public class ViewerOpenGLPrefs extends Preferences {

   protected ViewerManager myViewerManager;

   private boolean myTransparencyFaceCulling =
      ViewerManager.DEFAULT_TRANSPARENCY_FACE_CULLING;
   private boolean myTranparencyBlending = ViewerManager.DEFAULT_BLENDING;
   private BlendFactor myBlendSourceFactor =
      ViewerManager.DEFAULT_BLEND_SOURCE_FACTOR;
   private BlendFactor myBlendDestFactor =
      ViewerManager.DEFAULT_BLEND_DEST_FACTOR;

   static PropertyList myProps = new PropertyList (ViewerOpenGLPrefs.class);

   static {
      myProps.add(
         "transparencyFaceCulling", "allow transparency face culling",
         ViewerManager.DEFAULT_TRANSPARENCY_FACE_CULLING);
      myProps.add(
         "transparencyBlending", "enable/disable blending",
         ViewerManager.DEFAULT_BLENDING);
      myProps.add(
         "blendSourceFactor", "source transparency blending",
         ViewerManager.DEFAULT_BLEND_SOURCE_FACTOR);
      myProps.add (
         "blendDestFactor", "destination transparency blending",
         ViewerManager.DEFAULT_BLEND_DEST_FACTOR);
   }

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public ViewerOpenGLPrefs (ViewerManager vm) {
      myViewerManager = vm;      
   }

   public boolean getTransparencyFaceCulling () {
      return myTransparencyFaceCulling;
   }

   public void setTransparencyFaceCulling (boolean transparencyFaceCulling) {
      myTransparencyFaceCulling = transparencyFaceCulling;
   }

   public boolean getTransparencyBlending () {
      return myTranparencyBlending;
   }

   public void setTransparencyBlending (boolean blending) {
      myTranparencyBlending = blending;
   }

   public BlendFactor getBlendSourceFactor () {
      return myBlendSourceFactor;
   }

   public void setBlendSourceFactor (BlendFactor blendSourceFactor) {
      myBlendSourceFactor = blendSourceFactor;
   }

   public BlendFactor getBlendDestFactor () {
      return myBlendDestFactor;
   }

   public void setBlendDestFactor (BlendFactor blendDestFactor) {
      myBlendDestFactor = blendDestFactor;
   }

   public void applyToCurrent() {
      myViewerManager.setTransparencyFaceCulling (getTransparencyFaceCulling());
      myViewerManager.setTransparencyBlending (getTransparencyBlending());
      myViewerManager.setBlendSourceFactor (getBlendSourceFactor());
      myViewerManager.setBlendDestFactor (getBlendDestFactor());
   }

   public void setFromCurrent() {
      setTransparencyFaceCulling (myViewerManager.getTransparencyFaceCulling());
      setTransparencyBlending (myViewerManager.getTransparencyBlending());
      setBlendSourceFactor (myViewerManager.getBlendSourceFactor());
      setBlendDestFactor (myViewerManager.getBlendDestFactor());
   }

   protected PropertyPanel createEditingPanel() {
      PropertyPanel panel = createDefaultEditingPanel();
      addLoadApplyButtons (panel);
      return panel;
   }

}
