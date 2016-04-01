package maspack.render.GL.GL3;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLCapabilities;

import maspack.render.RenderObject;
import maspack.render.GL.GLSharedResources;

public class GL3SharedResources extends GLSharedResources {

   // Shared VBOs/VAOs
   private GL3SharedPrimitiveManager primManager;
   private GL3VertexAttributeMap vertexAttributes;
   private GL3SharedRenderObjectManager groManager;

   public GL3SharedResources(GLCapabilities cap, GL3VertexAttributeMap attributes) {
      super(cap);
      this.vertexAttributes = attributes;
      GL3PrimitiveFactory primFactory = new GL3PrimitiveFactory (
         vertexAttributes.getPosition (), 
         vertexAttributes.getNormal (), 
         vertexAttributes.getColor (),
         vertexAttributes.getTexcoord ());
      
      primManager = new GL3SharedPrimitiveManager(primFactory);
      groManager = new GL3SharedRenderObjectManager (vertexAttributes);
      
      addGarbageSource (primManager);
      addGarbageSource (groManager);
   }


   @Override
   public void dispose(GL gl) {

      GL3 gl3 = gl.getGL3 ();
      // clear shared info
      primManager.dispose(gl3);
   }

   public GL3SharedObject getSphere(GL3 gl, int slices, int levels) {
      return primManager.getSphere(gl, slices, levels);
   }

   public GL3SharedObject getSpindle(GL3 gl, int slices, int levels) {
      return primManager.getSpindle(gl, slices, levels);
   }

   public GL3SharedObject getCylinder(GL3 gl, int slices, boolean capped) {
      return primManager.getCylinder(gl, slices, capped);
   }

   public GL3SharedObject getCone(GL3 gl, int slices, boolean capped) {
      return primManager.getCone(gl, slices, capped);
   }

   public GL3SharedObject getAxes(GL3 gl, boolean drawx, boolean drawy, boolean drawz) {
      return primManager.getAxes(gl, drawx, drawy, drawz);
   }
   
   public GL3SharedRenderObjectIndexed getIndexed(GL3 gl, RenderObject robj) {
      return groManager.getIndexed (gl, robj);
   }
   
   public GL3SharedRenderObjectLines getLines(GL3 gl, RenderObject robj) {
      return groManager.getLines (gl, robj);
   }
   
   public GL3SharedRenderObjectPoints getPoints(GL3 gl, RenderObject robj) {
      return groManager.getPoints (gl, robj);
   }

   public GL3SharedPrimitiveManager getSharedPrimitiveManager () {
      return primManager;
   }
   
   public GL3SharedRenderObjectManager getSharedRenderObjectManager () {
      return groManager;
   }
   
   public GL3VertexAttributeInfo getVertexAttribute(String name) {
      return vertexAttributes.get (name);
   }

   public GL3VertexAttributeInfo getVertexPositionAttribute () {
      return vertexAttributes.getPosition ();
   }

   public GL3VertexAttributeInfo getVertexNormalAttribute () {
      return vertexAttributes.getNormal ();
   }
   
   public GL3VertexAttributeInfo getVertexColorAttribute () {
      return vertexAttributes.getColor ();
   }

   public GL3VertexAttributeInfo getVertexTexcoordAttribute () {
      return vertexAttributes.getTexcoord ();
   }
}
