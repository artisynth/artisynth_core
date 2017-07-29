package maspack.render.GL.GL3;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLCapabilities;

import maspack.render.RenderInstances;
import maspack.render.RenderObject;
import maspack.render.VertexIndexArray;
import maspack.render.GL.GLSharedResources;

public class GL3SharedResources extends GLSharedResources {

   // Shared VBOs/VAOs
   private GL3SharedPrimitiveManager primManager;
   private GL3VertexAttributeMap vertexAttributes;
   private GL3SharedRenderObjectManager groManager;
   private GL3SharedVertexIndexArrayManager viaManager;

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
      viaManager = new GL3SharedVertexIndexArrayManager ();
      
      addGarbageSource (primManager);
      addGarbageSource (groManager);
      addGarbageSource (viaManager);
   }


   @Override
   public void dispose(GL gl) {

      GL3 gl3 = gl.getGL3 ();
      // clear shared info
      primManager.dispose(gl3);
      groManager.dispose (gl3);
      viaManager.dispose (gl3);
   }

   public GL3SharedRenderObjectPrimitives getPrimitives(GL3 gl, RenderObject robj) {
      return groManager.getPrimitives (gl, robj);
   }
   
   public GL3SharedRenderObjectLines getLines(GL3 gl, RenderObject robj) {
      return groManager.getLines (gl, robj);
   }
   
   public GL3SharedRenderObjectPoints getPoints(GL3 gl, RenderObject robj) {
      return groManager.getPoints (gl, robj);
   }
   
   public GL3SharedRenderInstances getInstances(GL3 gl, RenderInstances rinst) {
      return groManager.getInstances(gl, rinst);
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
   
   public GL3SharedVertexIndexArray getVertexIndexArray(GL3 gl, VertexIndexArray via) {
      return viaManager.getElementArray (gl, via);
   }
}
