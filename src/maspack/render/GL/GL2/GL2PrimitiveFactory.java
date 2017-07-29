package maspack.render.GL.GL2;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

public class GL2PrimitiveFactory {

   private static GLU glu;
   private static GLUquadric mySphereQuad;

   public static GL2DisplayList createSphere(GL2 gl, int slices, int levels) {
      
      if (glu == null) {
         glu = new GLU();
      }
      if (mySphereQuad == null) {
         mySphereQuad = glu.gluNewQuadric();
      }
      
      GL2DisplayList list = GL2DisplayList.allocate (gl, 1);
      list.compile (gl);
         glu.gluSphere (mySphereQuad, 1.0, slices, levels);
      list.end (gl);
      
      return list;
   }

   public static GL2DisplayList createSpindle(GL2 gl, int slices, int levels) {
      
      GL2DisplayList list = GL2DisplayList.allocate (gl, 1);
      list.compile (gl);
         double s0 = 0;
         double c0 = 1;
         for (int slice = 0; slice < slices; slice++) {
            double ang = (slice + 1) * 2 * Math.PI / slices;
            double c1 = Math.cos (ang);
            double s1 = Math.sin (ang);

            gl.glBegin (GL2.GL_TRIANGLE_STRIP);
            for (int j = 0; j <= levels; j++) {
               double h = j * 1.0 / levels;
               double r = 1 * Math.sin (h * Math.PI / 1.0);
               double drdh = Math.PI * Math.cos (h * Math.PI / 1.0);
               gl.glNormal3d (c0, s0, -drdh);
               gl.glVertex3d (c0 * r, s0 * r, h);
               gl.glNormal3d (c1, s1, -drdh);
               gl.glVertex3d (c1 * r, s1 * r, h);
            }
            gl.glEnd();

            s0 = s1;
            c0 = c1;
         }
      list.end (gl);
      return list;
   }

   public static GL2DisplayList createCylinder(GL2 gl, int slices, boolean capped) {
      
      GL2DisplayList list = GL2DisplayList.allocate (gl, 1);
      list.compile (gl);
         // draw sides
         gl.glBegin(GL2.GL_TRIANGLE_STRIP);
         double c1,s1;
         for (int i = 0; i <= slices; i++) {
            double ang = i / (double)slices * 2 * Math.PI;
            c1 = Math.cos(ang);
            s1 = Math.sin(ang);
            gl.glNormal3d(c1, s1, 0);
            gl.glVertex3d (c1, s1, 1);
            gl.glVertex3d (c1, s1, 0);
         }
         gl.glEnd();

         if (capped) { // draw top cap first
            gl.glBegin (GL2.GL_TRIANGLE_FAN);
            gl.glNormal3d (0, 0, 1);
            for (int i = 0; i < slices; i++) {
               double ang = i / (double)slices * 2 * Math.PI;
               c1 = Math.cos(ang);
               s1 = Math.sin(ang);
               gl.glVertex3d (c1, s1, 1);
            }
            gl.glEnd();
            
            // now draw bottom cap
            gl.glBegin (GL2.GL_TRIANGLE_FAN);
            gl.glNormal3d (0, 0, -1);
            for (int i = 0; i < slices; i++) {
               double ang = i / (double)slices * 2 * Math.PI;
               c1 = Math.cos(ang);
               s1 = Math.sin(ang);
               gl.glVertex3d (c1, -s1, 0);
            }
            gl.glEnd();
         } // end caps
         
      list.end (gl);
      return list;
   }

   public static GL2DisplayList createCone(GL2 gl, int slices, boolean capped) {
      GL2DisplayList list = GL2DisplayList.allocate (gl, 1);
      list.compile (gl);
      
         // draw sides
         gl.glBegin(GL2.GL_TRIANGLE_STRIP);

         double c1,s1;
         double r2 = 1.0/Math.sqrt(2);
         for (int i = 0; i <= slices; i++) {
            double ang = i / (double)slices * 2 * Math.PI;
            c1 = Math.cos(ang);
            s1 = Math.sin(ang);
            gl.glNormal3d(c1*r2, s1*r2, r2);
            gl.glVertex3d (c1, s1, 1);
            gl.glVertex3d (c1, s1, 0);
         }

         gl.glEnd();

         if (capped) { 
            // bottom cap
            gl.glBegin (GL2.GL_TRIANGLE_FAN);
            gl.glNormal3d (0, 0, -1);
            for (int i = 0; i < slices; i++) {
               double ang = i / (double)slices * 2 * Math.PI;
               c1 = Math.cos(ang);
               s1 = Math.sin(ang);
               gl.glVertex3d (c1, -s1, 0);
            }
            gl.glEnd();
         } // end caps
         
      list.end (gl);
      return list;
   }
   
   /**
    * Box from (-1,-1,-1) to (1,1,1)
    * @param gl context
    * @return created box
    */
   public static GL2DisplayList createCube(GL2 gl) {
      GL2DisplayList list = GL2DisplayList.allocate (gl, 1);
      list.compile (gl);

      // draw sides
      gl.glBegin(GL2.GL_QUADS);
      // y = 1.0
      gl.glNormal3f(0.0f, 1.0f, 0.0f);
      gl.glVertex3f( 1.0f, 1.0f, -1.0f);
      gl.glVertex3f(-1.0f, 1.0f, -1.0f);
      gl.glVertex3f(-1.0f, 1.0f,  1.0f);
      gl.glVertex3f( 1.0f, 1.0f,  1.0f);

      // y = -1.0
      gl.glNormal3f( 0.0f, -1.0f,  0.0f);
      gl.glVertex3f( 1.0f, -1.0f,  1.0f);
      gl.glVertex3f(-1.0f, -1.0f,  1.0f);
      gl.glVertex3f(-1.0f, -1.0f, -1.0f);
      gl.glVertex3f( 1.0f, -1.0f, -1.0f);

      // z = 1.0
      gl.glNormal3f(0.0f, 0.0f, 1.0f);
      gl.glVertex3f( 1.0f,  1.0f, 1.0f);
      gl.glVertex3f(-1.0f,  1.0f, 1.0f);
      gl.glVertex3f(-1.0f, -1.0f, 1.0f);
      gl.glVertex3f( 1.0f, -1.0f, 1.0f);

      // z = -1.0
      gl.glNormal3f(0.0f, 0.0f, -1.0f);
      gl.glVertex3f( 1.0f, -1.0f, -1.0f);
      gl.glVertex3f(-1.0f, -1.0f, -1.0f);
      gl.glVertex3f(-1.0f,  1.0f, -1.0f);
      gl.glVertex3f( 1.0f,  1.0f, -1.0f);

      // x = -1.0
      gl.glNormal3f(-1.0f, 0.0f, 0.0f);
      gl.glVertex3f(-1.0f,  1.0f,  1.0f);
      gl.glVertex3f(-1.0f,  1.0f, -1.0f);
      gl.glVertex3f(-1.0f, -1.0f, -1.0f);
      gl.glVertex3f(-1.0f, -1.0f,  1.0f);

      // x = 1.0f
      gl.glNormal3f(1.0f, 0.0f, 0.0f);
      gl.glVertex3f(1.0f,  1.0f, -1.0f);
      gl.glVertex3f(1.0f,  1.0f,  1.0f);
      gl.glVertex3f(1.0f, -1.0f,  1.0f);
      gl.glVertex3f(1.0f, -1.0f, -1.0f);
      gl.glEnd ();

      list.end (gl);
      return list;
   }

}
