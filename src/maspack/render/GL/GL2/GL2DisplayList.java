package maspack.render.GL.GL2;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import maspack.render.GL.GLResourceBase;

public class GL2DisplayList extends GLResourceBase {

   private int listId;
   private int count;
   
   public GL2DisplayList(int id) {
      this(id, 1);
   }
   
   public GL2DisplayList(int baseId, int count) {
      listId = baseId;
      this.count = count;
   }
   
   public void compile(GL2 gl) {
      compile(gl, 0);
   }
   
   public void compile(GL2 gl, int idx) {
      gl.glNewList (listId+idx, GL2.GL_COMPILE);
   }
   
   public void compileAndExecute(GL2 gl) {
      compileAndExecute (gl, 0);
   }
   
   public void compileAndExecute(GL2 gl, int idx) {
      gl.glNewList (listId+idx, GL2.GL_COMPILE_AND_EXECUTE);
   }
   
   public void end(GL2 gl) {
      gl.glEndList();
   }
   
   public void execute(GL2 gl) {
      execute(gl, 0);
   }
   
   public void execute(GL2 gl, int idx) {
      gl.glCallList (listId+idx);
   }
   
   public void executeAll(GL2 gl) {
      for (int i=0; i<count; ++i) {
         gl.glCallList (listId+i);
      }
   }
   
   public int getListId() {
      return listId;
   }
   
   public int getCount() {
      return count;
   }

   @Override
   protected void internalDispose (GL gl) {
      if (listId >= 0) {
         System.out.println ("DisplayList " + listId + " disposed (" + this + ")" );
         GL2 gl2 = (GL2)gl;
         gl2.glDeleteLists (listId, count);
         listId = 0;
      }
   }
   
   @Override
   public void release () {
      System.out.println ("DisplayList " + listId + " released (" + this + ")" );
      super.release ();
   }
   
   @Override
   public GL2DisplayList acquire () {
      System.out.println ("DisplayList " + listId + " acquired (" + this + ")" );
      return (GL2DisplayList)super.acquire ();
   }
   
   public static GL2DisplayList allocate(GL2 gl, int count) {
      int baseId = gl.glGenLists (count);
      GL2DisplayList list = new GL2DisplayList (baseId, count);
      return list;
   }
}
