package maspack.render.GL.GL2;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

public class GL2PrimitiveManager extends DisplayListManager {

   public static class SphereKey {
      private int slices;
      private int levels;
      public SphereKey(int slices, int levels) {
         this.slices = slices;
         this.levels = levels;
      }
      public int getSlices() {
         return slices;
      }
      public int getLevels() {
         return levels;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + levels;
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) || (getClass() != obj.getClass())) {
            return false;
         }
         SphereKey other = (SphereKey)obj;
         return equals(other.slices, other.levels);
      }

      public boolean equals(int slices, int levels) {
         if ((this.levels != levels) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }

   public static class SpindleKey {
      private int slices;
      private int levels;
      public SpindleKey(int slices, int levels) {
         this.slices = slices;
         this.levels = levels;
      }
      public int getSlices() {
         return slices;
      }
      public int getLevels() {
         return levels;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + levels;
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) || (getClass() != obj.getClass())) {
            return false;
         }
         SpindleKey other = (SpindleKey)obj;
         return equals(other.slices, other.levels);
      }

      public boolean equals(int slices, int levels) {
         if ((this.levels != levels) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }

   public static class CylinderKey {
      private int slices;
      boolean capped;
      public CylinderKey(int slices, boolean capped) {
         this.slices = slices;
         this.capped = capped;
      }
      public int getSlices() {
         return slices;
      }
      public boolean isCapped() {
         return capped;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + (capped ? 1231 : 1237);
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) ||  (getClass() != obj.getClass())) {
            return false;
         }
         CylinderKey other = (CylinderKey)obj;
         return equals(other.slices, other.capped);
      }

      public boolean equals(int slices, boolean capped) {
         if ((this.capped != capped) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }

   public static class ConeKey {
      private int slices;
      boolean capped;
      public ConeKey(int slices, boolean capped) {
         this.slices = slices;
         this.capped = capped;
      }
      public int getSlices() {
         return slices;
      }
      public boolean isCapped() {
         return capped;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + (capped ? 1231 : 1237);
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) ||  (getClass() != obj.getClass())) {
            return false;
         }
         CylinderKey other = (CylinderKey)obj;
         return equals(other.slices, other.capped);
      }

      public boolean equals(int slices, boolean capped) {
         if ((this.capped != capped) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }

   private DisplayListInfo lastEllipsoid;
   private DisplayListInfo lastSphere;
   private DisplayListInfo lastCylinder;
   private DisplayListInfo lastCone;

   private GLU glu;
   private GLUquadric mySphereQuad;

   public GL2PrimitiveManager() {
      super();
      lastEllipsoid = null;
      lastSphere = null;
      lastCylinder = null;
      lastCone = null;
      glu = new GLU();
   }

   public int getSphereDisplayList (GL2 gl, int slices, int levels) {
      if (lastSphere != null) {
         SphereKey key = (SphereKey)(lastSphere.key);
         if (key.equals(slices, levels)) {
            lastSphere.incrementUseCount();
            return lastSphere.getList();
         }
      }

      SphereKey key = new SphereKey(slices, levels);
      DisplayListInfo li = getListInfo(key);
      if (li == null) {
         li = createSphere(gl, key, slices, levels);
         putListInfo(key, li);
      }
      li.incrementUseCount();
      lastSphere = li;
      return li.getList();
   }

   private DisplayListInfo createSphere(GL2 gl, Object key, int slices, int levels) {
      DisplayListInfo li = allocDisplayList(gl, key, null);
      if (li != null) {
         gl.glNewList (li.getList(), GL2.GL_COMPILE);
         if (mySphereQuad == null) {
            mySphereQuad = glu.gluNewQuadric();
         }
         glu.gluSphere (mySphereQuad, 1.0, slices, levels);
         gl.glEndList();
      }
      return li;
   }

   public int getSpindleDisplayList (GL2 gl, int slices, int levels) {
      if (lastEllipsoid != null) {
         SpindleKey key = (SpindleKey)(lastEllipsoid.key);
         if (key.equals(slices, levels)) {
            lastEllipsoid.incrementUseCount();
            return lastEllipsoid.getList();
         }
      }

      SpindleKey key = new SpindleKey(slices, levels);
      DisplayListInfo li = getListInfo(key);
      if (li == null) {
         li = createSpindle(gl, key, slices, levels);
         putListInfo(key, li);
      }
      li.incrementUseCount();
      lastEllipsoid = li;
      return li.getList();
   }
   
   private DisplayListInfo createSpindle(GL2 gl, Object key, int slices, int levels) {
      DisplayListInfo li = allocDisplayList(gl, key, null);
      if (li != null) {
         gl.glNewList (li.getList(), GL2.GL_COMPILE);
         
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
         
         gl.glEndList();
      }
      return li;
   }

   public int getCylinderDisplayList (GL2 gl, int slices, boolean capped) {
      if (lastCylinder != null) {
         CylinderKey key = (CylinderKey)(lastCylinder.key);
         if (key.equals(slices, capped)) {
            lastCylinder.incrementUseCount();
            return lastCylinder.getList();
         }
      }

      CylinderKey key = new CylinderKey(slices, capped);
      DisplayListInfo li = getListInfo(key);
      if (li == null) {
         li = createCylinder(gl, key, slices, capped);
         putListInfo(key, li);
      }
      li.incrementUseCount();
      lastCylinder = li;
      return li.getList();
   }

   
   private DisplayListInfo createCylinder(GL2 gl, Object key, int slices, boolean capped) {
      DisplayListInfo li = allocDisplayList(gl, key, null);
      if (li != null) {
         gl.glNewList (li.getList(), GL2.GL_COMPILE);
        
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
         
         gl.glEndList();
      }
      return li;
   }

  
   public int getConeDisplayList (GL2 gl, int slices, boolean capped) {
      if (lastCone != null) {
         ConeKey key = (ConeKey)(lastCone.key);
         if (key.equals(slices, capped)) {
            lastCone.incrementUseCount();
            return lastCone.getList();
         }
      }

      ConeKey key = new ConeKey(slices, capped);
      DisplayListInfo li = getListInfo(key);
      if (li == null) {
         li = createCone(gl, key, slices, capped);
         putListInfo(key, li);
      }
      li.incrementUseCount();
      lastCone = li;
      return li.getList();
   }
   
   private DisplayListInfo createCone(GL2 gl, Object key, int slices, boolean capped) {
      DisplayListInfo li = allocDisplayList(gl, key, null);
      if (li != null) {
         gl.glNewList (li.getList(), GL2.GL_COMPILE);
        
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
         
         gl.glEndList();
      }
      return li;
   }

}
