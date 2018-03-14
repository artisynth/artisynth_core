package maspack.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer.DrawMode;
import maspack.util.Disposable;
import maspack.util.DisposeObservable;
import maspack.util.DisposeObserver.DisposeObserverImpl;
import maspack.util.Versioned;

/**
 * Object containing information used for rendering, including
 * <ul>
 * <li><em>attribute data</em>, including positions, and (optionally) normals, colors, and texture coordinates.</li>
 * <li><em>vertex data</em>, where each vertex points to a single position, as well as (optionally) a single normal, color, and texture attribute.</li>
 * <li><em>primitive data</em>, consisting of zero or more "groups" of points, lines, and triangles.</li>
 * </ul>
 *
 */
public class RenderObject implements Versioned, DisposeObservable, Disposable {

   private static int nextIdNumber = 0;
   
//   /**
//    * During construction, allows automatic generation of primitives
//    */
//   public enum BuildMode {
//      POINTS,
//      LINES,
//      LINE_STRIP,
//      LINE_LOOP,
//      TRIANGLES,
//      TRIANGLE_STRIP,
//      TRIANGLE_FAN
//   }
   
  
   /**
    * Used for uniquely identifying a RenderObject, and checking
    * validity (can be safely shared)
    */
   public static class RenderObjectIdentifier extends DisposeObserverImpl {
      private int id;
      private RenderObjectIdentifier(int id) {
         this.id = id;
      }
      
      /**
       * Unique identifying number
       */
      public int getId() {
         return id;
      }
      
      @Override
      protected void dispose() {
         super.dispose();
      }
      
   }
   
   /**
    * Keeps track of versions for detecting changes.  Can be
    * cloned so renderers can keep track of the latest versions
    * they have observed.
    */
   public static class RenderObjectVersion {
      
      private int positionsVersion;
      private int normalsVersion;
      private int colorsVersion;
      private int texturesVersion;
      private int verticesVersion;
      private int pointsVersion;
      private int linesVersion;
      private int trianglesVersion;
      private int totalVersion;
      
      private RenderObjectVersion() {
         positionsVersion = 0;
         normalsVersion = 0;
         colorsVersion = 0;
         texturesVersion = 0;
         verticesVersion = 0;
         pointsVersion = 0;
         linesVersion = 0;
         trianglesVersion = 0;
         totalVersion = 0;
      }
      
      public int getPositionsVersion() {
         return positionsVersion;
      }
      
      public int getNormalsVersion() {
         return normalsVersion;
      }
      
      public int getColorsVersion() {
         return colorsVersion;
      }
      
      public int getTextureCoordsVersion() {
         return texturesVersion;
      }
      
      public int getVerticesVersion() {
         return verticesVersion;
      }
      
      public int getPointsVersion() {
         return pointsVersion;
      }
      
      public int getLinesVersion() {
         return linesVersion;
      }
      
      public int getTrianglesVersion() {
         return trianglesVersion;
      }
      
      public int getVersion() {
         return totalVersion;
      }
      
      @Override
      protected RenderObjectVersion clone() {
         RenderObjectVersion c = new RenderObjectVersion();
         c.positionsVersion = positionsVersion;
         c.normalsVersion = normalsVersion;
         c.colorsVersion = colorsVersion;
         c.texturesVersion = texturesVersion;
         c.verticesVersion = verticesVersion;
         c.pointsVersion = pointsVersion;
         c.linesVersion = linesVersion;
         c.trianglesVersion = trianglesVersion;
         c.totalVersion = totalVersion;
         return c;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + positionsVersion;
         result = prime * result + normalsVersion;
         result = prime * result + colorsVersion;
         result = prime * result + texturesVersion;
         result = prime * result + verticesVersion;
         result = prime * result + pointsVersion;
         result = prime * result + linesVersion;
         result = prime * result + trianglesVersion;
         result = prime * result + totalVersion;
         return result;
      }
      
      public boolean equals(RenderObjectVersion v) {
         if (  totalVersion != v.totalVersion
            || positionsVersion != v.positionsVersion 
            || normalsVersion != v.normalsVersion
            || colorsVersion != v.colorsVersion 
            || texturesVersion != v.texturesVersion
            || verticesVersion != v.verticesVersion
            || pointsVersion != v.pointsVersion
            || linesVersion != v.linesVersion
            || trianglesVersion != v.trianglesVersion ) {
            return false;
         }
         return true;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null || getClass() != obj.getClass()) {
            return false;
         }
         
         RenderObjectVersion other = (RenderObjectVersion)obj;
         return equals(other);
      }      
   }
   
   /**
    * Stores exposable state of the object, tracking the 
    * current primitive group indices.
    * 
    */
   public static class RenderObjectState {     
      private int numPointGroups;
      private int numLineGroups;
      private int numTriangleGroups;
     
      private int pointGroupIdx;
      private int lineGroupIdx;
      private int triangleGroupIdx;
      
      int numPositions;
      int numNormals;
      int numColors;
      int numTexcoords;
      
      private RenderObjectState() {
         numPointGroups = 0;
         numLineGroups = 0;
         numTriangleGroups = 0;

         pointGroupIdx = -1;
         lineGroupIdx = -1;
         triangleGroupIdx = -1;
         
         numPositions = 0;
         numNormals = 0;
         numColors = 0;
         numTexcoords = 0;
      }

      public int numPointGroups() {
         return numPointGroups;
      }

      public int numLineGroups() {
         return numLineGroups;
      }

      public int numTriangleGroups() {
         return numTriangleGroups;
      }

      public int getPointGroupIdx() {
         return pointGroupIdx;
      }

      public int getLineGroupIdx() {
         return lineGroupIdx;
      }

      public int getTriangleGroupIdx() {
         return triangleGroupIdx;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + lineGroupIdx;
         result = prime * result + numLineGroups;
         result = prime * result + numPointGroups;
         result = prime * result + numTriangleGroups;
         result = prime * result + pointGroupIdx;
         result = prime * result + triangleGroupIdx;
         result = prime * result + numPositions;
         result = prime * result + numNormals;
         result = prime * result + numColors;
         result = prime * result + numTexcoords;
         return result;
      }

      public boolean equals(RenderObjectState other) {
         if ( pointGroupIdx != other.pointGroupIdx
            || lineGroupIdx != other.lineGroupIdx
            || triangleGroupIdx != other.triangleGroupIdx
            || numPointGroups != other.numPointGroups
            || numLineGroups != other.numLineGroups
            || numTriangleGroups != other.numTriangleGroups
            || numPositions != other.numPositions
            || numNormals != other.numNormals
            || numColors != other.numColors
            || numTexcoords != other.numTexcoords) {
            return false;
         }
         return true;
      }
      
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null || getClass() != obj.getClass()) {
            return false;
         }
         RenderObjectState other = (RenderObjectState)obj;
         return equals(other);
      }
      
      public RenderObjectState clone() {
         RenderObjectState c = new RenderObjectState();
         c.numPointGroups = numPointGroups;
         c.numLineGroups = numLineGroups;
         c.numTriangleGroups = numTriangleGroups;
         c.pointGroupIdx = pointGroupIdx;
         c.lineGroupIdx = lineGroupIdx;
         c.triangleGroupIdx = triangleGroupIdx;
         c.numPositions = numPositions;
         c.numNormals = numNormals;
         c.numColors = numColors;
         c.numTexcoords = numTexcoords;
         return c;
      }

      public boolean hasPositions() {
         return numPositions>0;
      }
      
      public boolean hasNormals() {
         return numNormals>0;
      }
      
      public boolean hasColors() {
         return numColors>0;
      }
      
      public boolean hasTextureCoords() {
         return numTexcoords>0;
      }
      
   }
   
   RenderObjectIdentifier idInfo;
   RenderObjectVersion versionInfo;
   RenderObjectState stateInfo;
     
   ArrayList<float[]> positions;
   ArrayList<float[]> normals;
   ArrayList<byte[]> colors;
   ArrayList<float[]> texcoords;

   int currentPositionIdx;
   int currentNormalIdx;
   int currentColorIdx;
   int currentTextureIdx;

   // indicators that attributes have been modified
   boolean positionsModified;
   boolean normalsModified;
   boolean colorsModified;
   boolean texturesModified;
   
   DrawMode buildMode;
   int buildModeStart;  // starting number of vertices when build mode began

   // stride and offset for particular attributes
   int vertexCapacity = 0;
   int vertexStride = 0;
   int vertexPositionOffset = -1;
   int vertexNormalOffset = -1;
   int vertexColorOffset = -1;
   int vertexTexcoordOffset = -1;
   int[] vertices;      // position/normal/color/texture (if available)
   int vertexBufferMask = 0;
   static final byte VERTEX_POSITIONS = 0x1;
   static final byte VERTEX_NORMALS = 0x2;
   static final byte VERTEX_COLORS = 0x4;
   static final byte VERTEX_TEXCOORDS = 0x8;
   int numVertices;
   boolean verticesModified;

   static final int POINT_STRIDE = 1;
   static final int LINE_STRIDE = 2;
   static final int TRIANGLE_STRIDE = 3;
   ArrayList<VertexIndexArray> points;
   ArrayList<VertexIndexArray> lines;
   ArrayList<VertexIndexArray> triangles;

   // pointers to positions in points, lines, triangles
   VertexIndexArray currentPointGroup;
   VertexIndexArray currentLineGroup;
   VertexIndexArray currentTriangleGroup;

   boolean pointsModified;
   boolean linesModified;
   boolean trianglesModified;
   boolean totalModified;
   boolean istransient;
   
   ReentrantReadWriteLock lock;
   
   public RenderObject() {

      idInfo = new RenderObjectIdentifier(nextIdNumber++);
      versionInfo = new RenderObjectVersion();
      stateInfo = new RenderObjectState();
      istransient = false;
      lock = new ReentrantReadWriteLock();
      
      clearAll();

   }
   
   /**
    * Locks object, prevents further modifications until object is unlocked
    */
   public void readLock() {
      lock.readLock().lock();
   }
   
   /**
    * Locks object, prevents further modifications until object is unlocked
    */
   public void readUnlock() {
      lock.readLock().unlock();
   }
   
   /**
    * Acquires the write lock
    */
   protected void writeLock() {
      lock.writeLock().lock();
   }
   
   /**
    * Releases the write lock
    */
   protected void writeUnlock() {
      lock.writeLock().unlock();
   }
   
   /**
    * Returns a special object to be used as a unique identifier for this
    * RenderObject.  It contains a unique ID number, as well as a flag
    * for determining whether the object still persists and is valid.
    * This should be as the key in HashMaps etc... so that the original
    * RenderObject can be cleared and garbage-collected when it runs out 
    * of scope. 
    */
   public RenderObjectIdentifier getIdentifier() {
      return idInfo;
   }
   
   /**
    * Returns an immutable copy of all version information in this RenderObject,
    * safe for sharing between threads.  This can be used to detect whether
    * the RenderObject has been modified since last observed.
    */
   public RenderObjectVersion getVersionInfo() {
      
      getVersion(); // trigger update of all version numbers
      RenderObjectVersion v = versionInfo.clone();
      
      return v;
   }
   
   public RenderObjectState getStateInfo() {
      
      RenderObjectState s = stateInfo.clone();
      
      return s;
   }

   //=========================================================================
   // Positions, Normals, Colors, Textures
   //=========================================================================
   /**
    * Hint for ensuring sufficient storage for positions
    * @param cap capacity
    */
   public void ensurePositionCapacity(int cap) {
      writeLock();
      positions.ensureCapacity (cap);
      writeUnlock();
   }

   /**
    * Adds an indexable 3D position
    * @param px x coordinate
    * @param py y coordinate
    * @param pz z coordinate
    * @return the index of the position added
    */
   public int addPosition (float px, float py, float pz) {
      return addPosition (new float[]{px,py,pz});
   }
   
   /**
    * Adds a position by reference.  If the position is modified outside of
    * this render object, then you must manually flag the change using {@link
    * #notifyPositionsModified()}.  Otherwise, renderers are free to assume the
    * positions have not changed.
    * @param xyz position vector
    * @return an index referring to the added position
    */
   public int addPosition (float[] xyz) {
      writeLock();
      int pidx = addPositionInternal (xyz);
      writeUnlock();
      return pidx;      
   }
   
   private int addPositionInternal (float[] xyz) {
      int pidx = stateInfo.numPositions;
      positions.add (xyz);
      stateInfo.numPositions++;
      currentPositionIdx = pidx;
      notifyPositionsModifiedInternal ();
      return pidx;      
   }

   /**
    * Adds an indexable 3D position
    * @param pos coordinates of the position
    * @return the index of the position added
    */
   public int addPosition(Vector3d pos) {
      return addPosition (
         new float[]{(float)pos.x, (float)pos.y, (float)pos.z});
   }      

   /**
    * Sets the current position to be used in following vertices, 
    * based on position index. 
    * @param pidx index of a previously added position
    */
   public void setCurrentPosition(int pidx) {
      if (pidx >= 0) {
         if (pidx >= positions.size()) {
            throw new IllegalArgumentException (
               "Position "+pidx+" is not defined");
         }
         currentPositionIdx = pidx;
      }
      else {
         currentPositionIdx = -1;
      }
   }

   /**
    * Returns the index associated with the current position, or -1
    * if there is no current position.
    * 
    * @return current position index
    */
   public int getCurrentPosition() {
      return currentPositionIdx;
   }

   /**
    * Updates the values of the position with index pidx.
    * @param pidx position to modify
    * @param px x coordinate
    * @param py y coordinate
    * @param pz z coordinate
    */
   public void setPosition(int pidx, float px, float py, float pz) {
      setPosition (pidx, new float[]{px,py,pz});
   }
   
   /**
    * Updates the values of the position with index pidx.
    * @param pidx position to modify
    * @param pos new position coordinates
    */
   public void setPosition(int pidx, Vector3d pos) {
      setPosition (pidx, new float[]{(float)pos.x, (float)pos.y, (float)pos.z});
   }
   
   /**
    * Updates the values of the position with index pidx, to the provide
    * values by reference.  
    * 
    * @param pidx position to modify
    * @param pos new position values by reference
    */
   public void setPosition(int pidx, float[] pos) {
      writeLock();
      positions.set(pidx, pos);
      notifyPositionsModifiedInternal ();
      writeUnlock();
   }

   /**
    * Whether or not any positions have been defined.
    */
   public boolean hasPositions() {
      if (positions == null) {
         return false;
      }
      return (positions.size () > 0);
   }

   /**
    * Number of positions defined.
    */
   public int numPositions() {
      return stateInfo.numPositions;
   }

   /**
    * Retrieves the position at the supplied index.  If the returned position
    * is modified, then {@link #notifyPositionsModified()} must be manually called.
    * @param pidx position index
    * @return position {x,y,z}
    */
   public float[] getPosition(int pidx) {
        // prevent potential crash on positions
      float[] pos = getPositionInternal (pidx);
      return pos;
   }
   
   private float[] getPositionInternal(int pidx) {
      if (pidx < 0) {
         return null;
      }
      return positions.get(pidx);
   }

   /**
    * Retrieves the full list of positions.  This list should not
    * be modified.
    * 
    * @return list of positions.
    */
   public List<float[]> getPositions() {
      if (hasPositions()) {
         return Collections.unmodifiableList(positions);
      }
      return null;
   }

   private void notifyPositionsModifiedInternal() {
      positionsModified = true;
      totalModified = true;
   }
   
   /**
    * Indicate that the positions have been modified.
    */
   public void notifyPositionsModified() {
      writeLock();
      notifyPositionsModifiedInternal ();
      writeUnlock();
   }

   /**
    * Returns the latest positions version number,
    * for use in detecting if changes are present.
    */
   public int getPositionsVersion() {
      if (positionsModified) {
         versionInfo.positionsVersion++;
         positionsModified = false;
      }
      return versionInfo.positionsVersion;
   }

   /**
    * Hint for ensuring sufficient storage for normals
    * @param cap capacity
    */
   public void ensureNormalCapacity(int cap) {
      writeLock();
      normals.ensureCapacity (cap);
      writeUnlock();
   }

   /**
    * Adds an indexable 3D normal.
    * @param nx x component
    * @param ny y component
    * @param nz z component
    * @return the index of the normal added
    */
   public int addNormal(float nx, float ny, float nz) {
      return addNormal(new float[]{nx,ny,nz});
   }
   
   /**
    * Adds an indexable 3D normal by reference.  If the normal
    * is modified outside of this render object, this object
    * must be notified with {@link #notifyNormalsModified()}.
    * Otherwise, renders are free to assume there has been
    * no change.
    * @param nrm the normal to add
    * @return the index of the normal added
    */
   public int addNormal(float[] nrm) {
      writeLock();
      int nidx = stateInfo.numNormals;
      normals.add (nrm);
      stateInfo.numNormals++;
      currentNormalIdx = nidx;
      notifyNormalsModifiedInternal();
      writeUnlock();
      return nidx;
   }

   /**
    * Adds an indexable 3D normal
    * @param nrm coordinates of the normal
    * @return the index of the normal added
    */
   public int addNormal(Vector3d nrm) {
      return addNormal (
         new float[]{(float)nrm.x, (float)nrm.y, (float)nrm.z});
   }   
   
   /**
    * Sets the current normal to be used in following vertices, 
    * based on normal index. 
    * @param nidx index of a previously added normal
    */
   public void setCurrentNormal(int nidx) {
      if (nidx >= 0) {
         if (nidx >= normals.size()) {
            throw new IllegalArgumentException (
               "Normal "+nidx+" is not defined");
         }
         currentNormalIdx = nidx;
      }
      else {
         currentNormalIdx = -1;
      }
   }

   /**
    * Returns the index associated with the current normal, or -1
    * if there is no current normal.
    * 
    * @return current normal index
    */
   public int getCurrentNormal() {
      return currentNormalIdx;
   }

   /**
    * Updates the values of the normal with index nidx.
    * @param nidx normal to modify
    * @param nx x component
    * @param ny y component
    * @param nz z component
    */
   public void setNormal(int nidx, float nx, float ny, float nz) {
      setNormal(nidx, new float[]{nx,ny,nz});
   }
   
   /**
    * Updates the values of the normal with index nidx.
    * @param nidx normal to modify
    * @param nrm new normal coordinates
    */
   public void setNormal(int nidx, Vector3d nrm) {
      setNormal(nidx, new float[]{(float)nrm.x, (float)nrm.y, (float)nrm.z});
   }
   
   /**
    * Updates the new normal, by reference, with index nidx.
    * @param nidx normal to modify
    * @param nrm the new normal
    */
   public void setNormal(int nidx, float[] nrm) {
      writeLock();
      normals.set(nidx, nrm);
      notifyNormalsModifiedInternal();
      writeUnlock();
   }

   /**
    * Whether or not any normals have been defined.
    */
   public boolean hasNormals() {
      if (normals == null) {
         return false;
      }
      return (normals.size () > 0);
   }

   /**
    * Number of normals defined.
    */
   public int numNormals() {
      return stateInfo.numNormals;
   }

   /**
    * Retrieves the normal at the supplied index.  If the returned
    * normal is modified, then {@link #notifyNormalsModified()} must
    * be called.
    * @param nidx normal index
    * @return normal {x,y,z}
    */
   public float[] getNormal(int nidx) {
      float[] nrm = getNormalInternal (nidx);
      return nrm;
   }
   
   private float[] getNormalInternal(int nidx) {
      if (nidx < 0) {
         return null;
      }
      float[] nrm = normals.get(nidx);
      return nrm;
   }

   /**
    * Retrieves the full list of normals.  If the contents
    * of this list are modified, the method {@link #notifyNormalsModified()}
    * must be called. 
    * 
    * @return list of normals
    */
   public List<float[]> getNormals() {
      if (hasNormals()) {
         return Collections.unmodifiableList(normals);
      }
      return null;
   }
   
   /**
    * Indicate that the normals have been modified.
    */
   private void notifyNormalsModifiedInternal() {
      normalsModified = true;
      totalModified = true;
   }
   
   /**
    * Indicate that the normals have been modified.
    */
   public void notifyNormalsModified() {
      writeLock();
      notifyNormalsModifiedInternal ();
      writeUnlock();
   }

 
   /**
    * Returns the latest triangles version number,
    * for use in detecting if changes are present.
    */
   public int getNormalsVersion() {
      if (normalsModified) {
         versionInfo.normalsVersion++;
         normalsModified = false;
      }
      return versionInfo.normalsVersion;
   }

   /**
    * Hint for ensuring sufficient storage for colors
    * @param cap capacity
    */
   public void ensureColorCapacity(int cap) {
      writeLock();
      colors.ensureCapacity (cap);
      writeUnlock();
   }

   /**
    * Adds an indexable color
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    * @return the index of the color added
    */
   public int addColor(byte r, byte g, byte b, byte a) {
      return addColor(new byte[]{r,g,b,a});
   }
   
   /**
    * Adds an indexable color
    * @param r red [0-255]
    * @param g green [0-255]
    * @param b blue [0-255]
    * @param a alpha [0-255]
    * @return the index of the color added
    */
   public int addColor(int r, int g, int b, int a) {
      return addColor(new byte[]{(byte)r,(byte)g,(byte)b,(byte)a});
   }
   
   /**
    * Adds an indexable color
    * @param r red [0-1]
    * @param g green [0-1]
    * @param b blue  [0-1]
    * @param a alpha  [0-1]
    * @return the index of the color added
    */
   public int addColor(float r, float g, float b, float a) {
      return addColor(new byte[]{
         (byte)(255*r),(byte)(255*g),(byte)(255*b),(byte)(255*a)});
   }

   /**
    * Adds an indexable color.
    * @param rgba 4-float vector
    * @return the index of the color added
    */
   public int addColor(float[] rgba) {
      float alpha = 1f;
      if (rgba.length > 3) {
         alpha = rgba[3];
      }
      return addColor(new byte[]{
         (byte)(255*rgba[0]),(byte)(255*rgba[1]),(byte)(255*rgba[2]),(byte)(255*alpha)});
   }
   
   /**
    * Adds an indexable color
    * @param color color from which RGBA values are determines
    */
   public int addColor(Color color) {
      return addColor(
         new byte[]{
                    (byte)color.getRed(), (byte)color.getGreen(), 
                    (byte)color.getBlue(), (byte)color.getAlpha()});
   }
   
   /**
    * Adds an indexable color by reference.  If the color is modified
    * outside of this object, then {@link #notifyColorsModified()} must
    * be called.  Otherwise, renderers are free to assume the render object
    * has not changed.
    * @param rgba {red, green, blue, alpha}
    * @return the index of the color added
    */
   public int addColor(byte[] rgba) {
      writeLock();
      int cidx = stateInfo.numColors;
      colors.add (rgba);
      stateInfo.numColors++;
      currentColorIdx = cidx;
      notifyColorsModifiedInternal ();
      writeUnlock();
      return cidx;
   }
   
   private void notifyColorsModifiedInternal() {
      colorsModified = true;
      totalModified = true;
   }
   
   public void notifyColorsModified() {
      writeLock();
      notifyColorsModifiedInternal ();
      writeUnlock();
   }
   
   /**
    * Sets the current color to be used in following vertices
    * based on color index.
    * @param cidx index of a previously added color
    */
   public void setCurrentColor(int cidx) {
      if (cidx >= 0) {
         if (cidx >= colors.size()) {
            throw new IllegalArgumentException (
               "Color "+cidx+" is not defined");
         }
         currentColorIdx = cidx;
      }
      else {
         currentColorIdx = -1;
      }
   }
   
   /**
    * Returns the index associated with the current color, or -1
    * if there is no current color.
    * 
    * @return current color index
    */
   public int getCurrentColor() {
      return currentColorIdx;
   }

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public void setColor(int cidx, byte r, byte g, byte b, byte a) {
      setColor(cidx, new byte[]{r,g,b,a});
   }

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public void setColor(int cidx, int r, int g, int b, int a) {
      setColor(cidx, (byte)r, (byte)g, (byte)b, (byte)a);
   }

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public void setColor(int cidx, float r, float g, float b, float a) {
      setColor(cidx, (byte)(255*r), (byte)(255*g), (byte)(255*b), (byte)(255*a));
   }
   
   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param color new color values
    */
   public void setColor(int cidx, Color color) {
      setColor(cidx, 
         new byte[]{
                    (byte)color.getRed(), (byte)color.getGreen(), 
                    (byte)color.getBlue(), (byte)color.getAlpha()});
   }
   
   /**
    * Updates the values of the color, by reference, with index cidx.
    * @param cidx color to modify
    * @param rgba {red, green, blue, alpha}
    */
   public void setColor(int cidx, byte[] rgba) {
      writeLock();
      colors.set(cidx, rgba);
      notifyColorsModifiedInternal();
      writeUnlock();
   }

   /**
    * Whether or not any colors have been defined.
    */
   public boolean hasColors() {
      if (colors == null) {
         return false;
      }
      return (colors.size () > 0);
   }

   /**
    * Number of colors defined
    */
   public int numColors() {
      return stateInfo.numColors;
   }

   /**
    * Retrieves the color at the supplied index.  If the returned color
    * is modified, then {@link #notifyColorsModified()} must be manually called.
    * @param cidx color index
    * @return color {red, green, blue, alpha}
    */
   public byte[] getColor(int cidx) {
      byte[] c = getColorInternal (cidx);
      return c;
   }
   
   private byte[] getColorInternal(int cidx) {
      if (cidx < 0) {
         return null;
      }
      return colors.get(cidx);
   }

   /**
    * Retrieves the full list of Colors.  This list should not
    * be modified.  
    * 
    * @return list of colors.
    */
   public List<byte[]> getColors() {
      return Collections.unmodifiableList(colors);
   }

   /**
    * Returns the latest colors version number,
    * for use in detecting if changes are present.
    */
   public int getColorsVersion() {
      if (colorsModified) {
         versionInfo.colorsVersion++;
         colorsModified = false;
      }
      return versionInfo.colorsVersion;
   }

   /**
    * Hint for ensuring sufficient storage for texture coordinates
    * @param cap capacity
    */
   public void ensureTextureCoordCapacity(int cap) {
      writeLock();
      texcoords.ensureCapacity (cap);
      writeUnlock();
   }

   /**
    * Adds an indexable 2D texture coordinate
    * @param tx x coordinate
    * @param ty y coordinate
    * @return the index of the texture coordinate added
    */
   public int addTextureCoord(float tx, float ty) {
      return addTextureCoord (new float[]{tx,ty});
   }
   
   /**
    * Adds an indexable 2D texture coordinate
    * @param xy texture coordinates
    * @return the index of the texture coordinates added
    */
   public int addTextureCoord(Vector2d xy) {
      return addTextureCoord (
         new float[]{(float)xy.x, (float)xy.y});
   }   
   
   /**
    * Adds an indexable 2D texture coordinate by reference.
    * If the texture coordinates are modified outside of this object,
    * then {@link #notifyTextureCoordsModified()} must be called.  Otherwise,
    * renderers are free to assume the render object has not changed.
    * @param xy x and y coordinate values
    * @return the index of the texture coordinate added
    */
   public int addTextureCoord(float[] xy) {
      writeLock();
      int tidx = stateInfo.numTexcoords;
      texcoords.add (xy);
      stateInfo.numTexcoords++;
      currentTextureIdx = tidx;
      notifyTextureCoordsModifiedInternal();
      writeUnlock();
      return tidx;
   }
   
   private void notifyTextureCoordsModifiedInternal() {
      texturesModified = true;
      totalModified = true;
   }
   
   public void notifyTextureCoordsModified() {
      writeLock();
      notifyColorsModifiedInternal ();
      writeUnlock();
   }

   /**
    * Sets the current texture coordinates to be used in following vertices,
    * based on texture coordinate index.
    * @param tidx index of a previously added texture coordinate
    */
   public void setCurrentTextureCoord(int tidx) {
      if (tidx >= 0) {
         if (tidx >= texcoords.size()) {
            throw new IllegalArgumentException (
               "Texture coordinate "+tidx+" is not defined");
         }
         currentTextureIdx = tidx;
      }
      else {
         currentTextureIdx = -1;
      }
   }

   /**
    * Returns the index associated with the current texture coordinate, or -1
    * if there is no current texture coordinate.
    * 
    * @return current texture coordinate index
    */
   public int getCurrentTextureCoord() {
      return currentTextureIdx;
   }

   /**
    * Updates the values of the texture coordinate with index tidx.
    * @param tidx coordinate index
    * @param tx x coordinate
    * @param ty y coordinate
    */
   public void setTextureCoord(int tidx, float tx, float ty) {
      setTextureCoord (tidx, new float[] {tx,ty});
   }
   
   /**
    * Updates the values of the texture coordinate with index tidx.
    * @param tidx coordinate index
    * @param xy new texture coordinates
    */
   public void setTextureCoord(int tidx, Vector2d xy) {
      setTextureCoord (tidx, new float[] {(float)xy.x,(float)xy.y});
   }
   
   /**
    * Updates the values of the texture coordinate with index tidx by reference.
    * @param tidx coordinate index
    * @param xy x and y coordinate values
    */
   public void setTextureCoord(int tidx, float[] xy) {
      writeLock();
      texcoords.set(tidx, xy);
      notifyTextureCoordsModifiedInternal ();
      writeUnlock();
   }

   /**
    * Whether or not any texture coordinates have been defined.
    */
   public boolean hasTextureCoords() {
      if (texcoords == null) {
         return false;
      }
      return (texcoords.size () > 0);
   }

   /**
    * Number of texture coordinates defined.
    */
   public int numTextureCoords() {
      return stateInfo.numTexcoords;
   }

   /**
    * Retrieves the texture coordinate at the supplied index.  If the returned texture
    * coordinate is modified, then {@link #notifyTextureCoordsModified()} must be manually called.
    * @param tidx position index
    * @return texture coordinate {x,y}
    */
   public float[] getTextureCoord(int tidx) {
      if (tidx < 0) {
         return null;
      }
      float[] t = getTextureCoordInternal (tidx);
      return t;
   }
   
   private float[] getTextureCoordInternal(int tidx) {
      if (tidx < 0) {
         return null;
      }
      return texcoords.get(tidx);
   }

   /**
    * Retrieves the full list of texture coordinates.  This list should not
    * be modified. 
    * 
    * @return list of texture coordinates.
    */
   public List<float[]> getTextureCoords() {
      if (hasTextureCoords()) {
         return Collections.unmodifiableList(texcoords);
      }
      return null;
   }

   /**
    * Returns the latest texture coordinates version number,
    * for use in detecting if changes are present.
    */
   public int getTextureCoordsVersion() {
      if (texturesModified) {
         versionInfo.texturesVersion++;
         texturesModified = false;
      }
      return versionInfo.texturesVersion;
   }

   //=========================================================================
   // Vertices
   //=========================================================================

   private void maybeGrowAdjustVertices(int cap) {
      
      // maintain capacity      
      boolean vHasPositions = ((vertexBufferMask & VERTEX_POSITIONS) != 0);
      boolean vHasNormals = ((vertexBufferMask & VERTEX_NORMALS) != 0);
      boolean vHasColors = ((vertexBufferMask & VERTEX_COLORS) != 0);
      boolean vHasTexcoords = ((vertexBufferMask & VERTEX_TEXCOORDS) != 0);
      
      boolean rHasPositions = hasPositions();
      boolean rHasNormals = hasNormals();
      boolean rHasColors = hasColors();
      boolean rHasTexcoords = hasTextureCoords ();
      
      // need to expand?
      int ncap = vertexCapacity;      // start with old capacity
      if (ncap - cap < 0) { // overflow-conscious
         ncap = vertexCapacity + (vertexCapacity >> 1);  // grow by 1.5
      }
      // if still less
      if (ncap - cap < 0) {
         ncap = cap;  // at least hold minimum requested
      }
      
      int vcap = ncap*vertexStride;
      
      // if new vertex array will contain new information, we need to shift some of the data
      if (vHasPositions != rHasPositions ||
          vHasNormals != rHasNormals ||
          vHasColors != rHasColors ||
          vHasTexcoords != rHasTexcoords) {
         
         int newVertexStride = 0;
         int newPositionOffset = -1;
         int newNormalOffset = -1;
         int newColorOffset = -1;
         int newTexcoordOffset = -1;
         byte newVertexBufferMask = 0;
         
         if (rHasPositions) {
            newPositionOffset = newVertexStride++;
            newVertexBufferMask |= VERTEX_POSITIONS;
         }
         if (rHasNormals) {
            newNormalOffset = newVertexStride++;
            newVertexBufferMask |= VERTEX_NORMALS;
         }
         if (rHasColors) {
            newColorOffset = newVertexStride++;
            newVertexBufferMask |= VERTEX_COLORS;
         }
         if (rHasTexcoords) {
            newTexcoordOffset = newVertexStride++;
            newVertexBufferMask |= VERTEX_TEXCOORDS;
         }
         
         // resize and adjust
         int[] newVerts = new int[ncap*newVertexStride];
         Arrays.fill (newVerts, -1);  // indicative of missing value
         
         if (vHasPositions) {
            int nidx = newPositionOffset;
            int oidx = vertexPositionOffset;
            for (int i=0; i<numVertices; ++i) {
               newVerts[nidx] = vertices[oidx];
               nidx += newVertexStride;
               oidx += vertexStride;
            }
         }
         
         if (vHasNormals) {
            int nidx = newNormalOffset;
            int oidx = vertexNormalOffset;
            for (int i=0; i<numVertices; ++i) {
               newVerts[nidx] = vertices[oidx];
               nidx += newVertexStride;
               oidx += vertexStride;
            }
         }
         
         if (vHasColors) {
            int nidx = newColorOffset;
            int oidx = vertexColorOffset;
            for (int i=0; i<numVertices; ++i) {
               newVerts[nidx] = vertices[oidx];
               nidx += newVertexStride;
               oidx += vertexStride;
            }
         }
         
         if (vHasNormals) {
            int nidx = newTexcoordOffset;
            int oidx = vertexTexcoordOffset;
            for (int i=0; i<numVertices; ++i) {
               newVerts[nidx] = vertices[oidx];
               nidx += newVertexStride;
               oidx += vertexStride;
            }
         }
         
         vertexStride = newVertexStride;
         vertexBufferMask = newVertexBufferMask;
         vertexPositionOffset = newPositionOffset;
         vertexNormalOffset = newNormalOffset;
         vertexColorOffset = newColorOffset;
         vertexTexcoordOffset = newTexcoordOffset;
         vertices = newVerts;
         
      } 
      
      // otherwise, we may need to grow array
      else if (vcap > vertices.length) {
         vertices = Arrays.copyOf (vertices, vcap);
         
         int vstart = numVertices*vertexStride;
         // fill extra with -1
         for (int i=vstart; i<vcap; ++i) {
            vertices[i] = -1;
         }
      }
      vertexCapacity = ncap;
   }
   
   /**
    * Hint for ensuring sufficient storage for vertices
    * @param cap capacity
    */
   public void ensureVertexCapacity(int cap) {
      writeLock();
      maybeGrowAdjustVertices (cap);
      writeUnlock();
   }
   
   /**
    * Adds a vertex using the currently active position, normal, color,
    * and texture coordinate (if available).  
    * @return the index of the newly created vertex.
    */
   public int addVertex() {
      return addVertex(currentPositionIdx, currentNormalIdx, 
         currentColorIdx, currentTextureIdx);
   }
   
   /**
    * Adds a vertex using the supplied position index.
    * @return the index of the newly created vertex.
    */
   public int addVertex(int pidx) {
      return addVertex(pidx, currentNormalIdx, 
         currentColorIdx, currentTextureIdx);
   }

   /**
    * Adds a vertex using the supplied position and normal indices.
    * @return the index of the newly created vertex.
    */
   public int addVertex(int pidx, int nidx) {
      return addVertex(pidx, nidx, 
         currentColorIdx, currentTextureIdx);
   }
      
   /**
    * Adds a vertex using the position, normal, color and texture
    * coordinates identified by index number.  Negative index values are 
    * ignored.  
    * @param pidx position index
    * @param nidx normal index
    * @param cidx color index
    * @param tidx texture coordinate index
    * @return the index of the newly created vertex.
    */
   public int addVertex(int pidx, int nidx, int cidx, int tidx) {
      writeLock();
      int vidx = addVertexInternal (pidx, nidx, cidx, tidx);
      writeUnlock();
      
      return vidx;
   }
   
   private int addVertexInternal(int pidx, int nidx, int cidx, int tidx) {
      
      int vidx = numVertices++;
      
      maybeGrowAdjustVertices (numVertices);
      int baseIdx = vidx*vertexStride;
      if (vertexPositionOffset >= 0) {
         vertices[baseIdx+vertexPositionOffset] = pidx;
      }
      if (vertexNormalOffset >= 0) {
         vertices[baseIdx+vertexNormalOffset] = nidx;
      }
      if (vertexColorOffset >= 0) {
         vertices[baseIdx+vertexColorOffset] = cidx;
      }
      if (vertexTexcoordOffset >= 0) {
         vertices[baseIdx+vertexTexcoordOffset] = tidx;
      }
      
      verticesModified = true;
      totalModified = true;
      
      return vidx;
   }

   /**
    * Add a vertex at the supplied position, using the currently active
    * normal, color and texture coordinate (if available).  A new position
    * is created to accommodate the vertex.
    * @param px x coordinate
    * @param py y coordinate
    * @param pz z coordinate
    * @return vertex index
    */
   public int vertex(float px, float py, float pz) {
      return vertex(new float[] {px,py,pz});
   }
   
   /**
    * Add a vertex at the supplied position, using the currently active
    * normal, color and texture coordinate (if available).  A new position
    * is created to accommodate the vertex.
    * @param pos position coordinates
    * @return vertex index
    */
   public int vertex(Vector3d pos) {
      return vertex(new float[] {(float)pos.x, (float)pos.y, (float)pos.z});
   }
   
   private int vertexInternal(float[] xyz) {
      int pIdx = addPositionInternal(xyz);
      return addVertexInternal(pIdx, currentNormalIdx, currentColorIdx, currentTextureIdx);
   }
   
   /**
    * Add a vertex at the supplied position using the currently active
    * normal, color and texture coordinate (if available).  A new position
    * is created, by reference, to accommodate the vertex.
    * @see #addPosition(float[])
    * @see #addVertex(int)
    * @param xyz x, y, and z coordinate values
    * @return vertex index
    */
   public int vertex(float[] xyz) {
      writeLock();
      int vidx = vertexInternal(xyz);
      writeUnlock();
      return vidx;
   }
   
   /**
    * Modify the attribute indices of a particular vertex
    * @param vidx index of vertex to modify
    * @param pidx new position index
    * @param nidx new normal index
    * @param cidx new color index
    * @param tidx new texture coordinate index
    */
   public void setVertex(int vidx, int pidx, int nidx, int cidx, int tidx) {
      
      writeLock();
      maybeGrowAdjustVertices (numVertices);
      
      int baseIdx = vidx*vertexStride;
      if (vertexPositionOffset >= 0) {
         vertices[baseIdx+vertexPositionOffset] = pidx;
      }
      if (vertexNormalOffset >= 0) {
         vertices[baseIdx+vertexNormalOffset] = nidx;
      }
      if (vertexColorOffset >= 0) {
         vertices[baseIdx+vertexColorOffset] = cidx;
      }
      if (vertexTexcoordOffset >= 0) {
         vertices[baseIdx+vertexTexcoordOffset] = tidx;
      }
      
      verticesModified = true;
      totalModified = true;
      
      writeUnlock();
   }

   /**
    * Returns the number of vertices, as defined by unique sets of position,
    * normal, color, and texture indices.
    */
   public int numVertices() {
      return numVertices;
   }

   /**
    * Returns the position of the supplied vertex
    */
   public float[] getVertexPosition(int vidx) {
      if (vertexPositionOffset < 0) {
         return null;
      }
      
      int idx = vertices[vidx*vertexStride+vertexPositionOffset];
      float[] pos = getPositionInternal(idx);
      
      return pos;
   }

   /**
    * Returns the normal of the supplied vertex
    */
   public float[] getVertexNormal(int vidx) {
      if (vertexNormalOffset < 0) {
         return null;
      }
      int idx = vertices[vidx*vertexStride+vertexNormalOffset];
      float[] nrm = getNormal(idx);
      
      return nrm;
   }

   /**
    * Returns the color of the supplied vertex
    */
   public byte[] getVertexColor(int vidx) {
      if (vertexColorOffset < 0) {
         return null;
      }
      int idx = vertices[vidx*vertexStride+vertexColorOffset];
      byte[] clr = getColorInternal(idx);
      
      return clr;
   }

   /**
    * Returns the texture coordinate of the supplied vertex
    */
   public float[] getVertexTextureCoord(int vidx) {
      if (vertexTexcoordOffset < 0) {
         return null;
      }
      
      int idx = vertices[vidx*vertexStride+vertexTexcoordOffset];
      float[] tex = getTextureCoord(idx);
      
      return tex;
   }

   /**
    * Raw pointer to vertex data (mainly used by renderers).
    * Offsets and strides for various attributes can be
    * queried with getVertex[attribute]Offset() and
    * {@link #getVertexStride()}.
    * 
    * @return the underlying vertex buffer
    */
   public int[] getVertexBuffer() {
      if (verticesModified) {
         if (vertices.length != (vertexStride*numVertices)) {
            vertices = Arrays.copyOf (vertices, vertexStride*numVertices);
            vertexCapacity = numVertices;
         }
      }
      return vertices;
   }
   
   public int getVertexStride() {
      return vertexStride;
   }
   
   public int getVertexPositionOffset() {
      return vertexPositionOffset;
   }
   
   public int getVertexNormalOffset() {
      return vertexNormalOffset;
   }
   
   public int getVertexColorOffset() {
      return vertexColorOffset;
   }
   
   public int getVertexTextureCoordOffset() {
      return vertexTexcoordOffset;
   }

   /**
    * Returns the latest vertices version number,
    * for use in detecting if changes are present.
    */
   public int getVerticesVersion() {
      if (verticesModified) {
         versionInfo.verticesVersion++;
         verticesModified = false;
      }
      return versionInfo.verticesVersion;
   }
   
   /**
    * Start automatically building primitives for every added vertex,
    * using a specified mode.  Primitives are only guaranteed to be
    * complete after a call to {@link #endBuild()}.
    * @param mode mode for adding consecutive primitives
    */
   public void beginBuild(DrawMode mode) {
      buildModeStart = numVertices();
      buildMode = mode;
   }
   
   /**
    * End automatically building primitives.
    */
   public void endBuild() {
      if (buildMode == null) {
         return;
      }
      
      writeLock();
      int vStart = buildModeStart;
      int vEnd = numVertices()-1;
      buildModeStart = -1;
      
      switch (buildMode) {
         case POINTS:
            addPointsInternal(vStart, vEnd);
            break;
         case LINES:
            addLinesInternal(vStart,vEnd);
            break;
         case LINE_LOOP:
            addLineLoopInternal(vStart,vEnd);
            break;
         case LINE_STRIP:
            addLineStripInternal(vStart,vEnd);
            break;
         case TRIANGLES:
            addTrianglesInternal(vStart,vEnd);
            break;
         case TRIANGLE_FAN:
            addTriangleFanInternal(vStart,vEnd);
            break;
         case TRIANGLE_STRIP:
            addTriangleStripInternal(vStart,vEnd);
            break;
         default:
            // nothing
            break;
         
      }
      buildMode = null;
      writeUnlock();
   }
   
   public DrawMode getBuildMode() {
      return buildMode;
   }

   //=========================================================================
   // Primitives: points, lines, triangles
   //=========================================================================

   /**
    * Informs the render object that points have been modified outside of its control.
    */
   public void notifyPointsModified() {
      writeLock();
      pointsModified = true;
      totalModified = true;
      writeUnlock();
   }
   
   private void notifyPointsModifiedInternal() {
      pointsModified = true;
      totalModified = true;
   }
   
   /**
    * Creates a point primitive at the supplied vertex.
    * @param vidx vertex index for point
    */
   public void addPoint(int vidx) {
      // start first point group
      writeLock();
      addPointInternal(vidx);
      writeUnlock();
   }
   
   private void addPointInternal(int vidx) {
      if (stateInfo.numPointGroups == 0) {
         createPointGroupInternal();
      }
      currentPointGroup.add(vidx);
      notifyPointsModifiedInternal();
   }

   /**
    * Creates point primitives at the supplied vertex locations.
    * @param vidxs array of vertex indices at which to create point primitives.
    */
   public void addPoints(int... vidxs) {
      writeLock();
      addPointsInternal(vidxs);
      writeUnlock();
   }
   
   
   private void addPointsInternal(int... vidxs) {
      for (int v : vidxs) {
         addPointInternal(v);
      }
   }

   /**
    * Creates a point primitive at the supplied position.  A vertex is first
    * created using the currently active normal, color and texture coordinate
    * (if available).  
    * @param v array of length 3 (x,y,z)
    */
   public void addPoint(float[] v) {
      writeLock();
      int pIdx = addPositionInternal(v);
      int vidx = addVertexInternal(pIdx, currentNormalIdx, 
         currentColorIdx, currentTextureIdx);
      addPointInternal(vidx);
      writeUnlock();
   }

   /**
    * Creates a set of vertices and point primitives at the supplied positions.
    * @see #addPoint(float[])
    * @param pnts positions at which points and vertices should be created
    */
   public void addPoints(Iterable<float[]> pnts) {
      writeLock();
      for (float[] v : pnts) {
         int pIdx = addPositionInternal(v);
         int vidx = addVertexInternal(pIdx, currentNormalIdx, 
            currentColorIdx, currentTextureIdx);
         addPointInternal(vidx);
      }
      writeUnlock();
   }

   /**
    * Indicates whether any point primitives have been defined.
    */
   public boolean hasPoints() {
      return (numPointGroups() > 0);
   }

   /**
    * Number of point primitives defined.
    */
   public int numPoints() {
      if (currentPointGroup == null) {
         return 0;
      }
      return currentPointGroup.size()/POINT_STRIDE;
   }
   
   /**
    * Returns a list of vertex indices of all point primitives defined
    * in the current group.
    * Points are separated by a stride that can be queried with
    * {@link #getPointStride()}.
    *
    * @return an array of vertex indices defining the points
    */
   public int[] getPoints() {
      int[] out = null;
      if (hasPoints()) {
         currentPointGroup.trimToSize();
         out = currentPointGroup.getArray ();
      }
      return out;
   }
   
   /**
    * Returns the stride used within the point buffer.
    * @return point buffer stride
    * @see #getPoints(int)
    */
   public int getPointStride() {
      return POINT_STRIDE;
   }
   

   /**
    * Hint for ensuring sufficient storage for points
    * @param cap capacity
    */
   public void ensurePointCapacity(int cap) {
      writeLock();
      if (stateInfo.numPointGroups == 0) {
         createPointGroupInternal();
      }
      currentPointGroup.ensureCapacity(cap);
      writeUnlock();
   }
   
   /**
    * Creates a new group of points that can be rendered.  By default,
    * only one point group exists.
    * @return the index of the new group of points
    * @see #pointGroup(int)
    */
   public int createPointGroup() {
      writeLock();
      int idx = createPointGroupInternal ();
      writeUnlock();
      return idx;
   }
   
   private int createPointGroupInternal() {
      VertexIndexArray newPoints = new VertexIndexArray();
      points.add(newPoints);
      currentPointGroup = newPoints;
      int idx = stateInfo.numPointGroups;
      stateInfo.pointGroupIdx = idx;
      stateInfo.numPointGroups++;
      notifyPointsModifiedInternal ();
      return idx;
   }

   /**
    * Sets the current active point group for rendering.
    * @param setIdx index of active point group.
    */
   public void pointGroup(int setIdx) {
      writeLock();
      stateInfo.pointGroupIdx = setIdx;
      if (points != null) {
         currentPointGroup = points.get(stateInfo.pointGroupIdx);
      }
      writeUnlock();
   }

   /**
    * Returns the index of the currently active point group.
    */
   public int getPointGroupIdx() {
      return stateInfo.pointGroupIdx;
   }

   /**
    * The number of point groups available.
    */
   public int numPointGroups() {
      return stateInfo.numPointGroups;
   }

   /**
    * Number of point primitives defined in a point group.
    */
   public int numPoints(int pgroup) {
      if (points == null || points.size() <= pgroup) {
         return 0;
      }
      int np = points.get(pgroup).size()/POINT_STRIDE;
      
      return np;
   }

   /**
    * Returns a list of vertex indices of all point primitives defined
    * in a given point group.
    * Points are separated by a stride that can be queried with
    * {@link #getPointStride()}.
    * 
    * @param pgroup point group
    * @return an array of vertex indices defining the points
    */
   public int[] getPoints(int pgroup) {
      int[] pnts = null;
      if (hasPoints()) {
         
         VertexIndexArray pg = points.get (pgroup);
         pg.trimToSize ();
         pnts = pg.getArray ();
         
      }
      return pnts;
   }

   /**
    * Returns the latest points version number,
    * for use in detecting if changes are present.
    */
   public int getPointsVersion() {
      if (pointsModified) {
         
         versionInfo.pointsVersion++;
         pointsModified = false;
         
      }
      return versionInfo.pointsVersion;
   }
   
   /**
    * Informs the render object that lines have been modified outside of its
    * control.
    */
   public void notifyLinesModified() {
      writeLock();
      notifyLinesModifiedInternal ();
      writeUnlock();
   }
   
   private void notifyLinesModifiedInternal() {
      linesModified = true;
      totalModified = true;
   }

   private void addLinePair(int[] vidxs) {
      writeLock();
      if (stateInfo.numLineGroups == 0)  {
         createLineGroupInternal();
      }
      currentLineGroup.addAll(vidxs);
      notifyLinesModifiedInternal ();
      writeUnlock();
   }
   
   private void addLinePairInternal(int v0, int v1) {
      if (stateInfo.numLineGroups == 0)  {
         createLineGroupInternal();
      }
      currentLineGroup.add(v0);
      currentLineGroup.add(v1);
      notifyLinesModifiedInternal ();
   }

   /**
    * Creates a line primitive between the supplied vertices.
    * @param v0idx vertex index for start of line
    * @param v1idx vertex index for end of line
    */
   public void addLine(int v0idx, int v1idx) {
      writeLock();
      addLinePairInternal(v0idx, v1idx);
      writeUnlock();
   }

   /**
    * Creates a set of line primitives between pairs of vertices as specified
    * by the supplied set of vertex indices.  For example, supplying 
    * {1,2,3,4} will create two line segments: 1-2, 3-4.
    * @param vidxs vertex indices, in pairs, defining line segments.  
    */
   public void addLines(int... vidxs) {
      writeLock();
      for (int i=0; i<vidxs.length-1; i+=2) {
         addLinePairInternal(vidxs[i], vidxs[i+1]);
      }
      writeUnlock();
   }
   
   /**
    * Creates a set of line primitives between pairs of vertices as specified
    * by the supplied vertex range.  For example, supplying 
    * {1,4} will create two line segments: 1-2, 3-4.
    * @param vStart starting vertex
    * @param vEnd ending vertex  
    */
   public void addLines(int vStart, int vEnd) {
      writeLock();
      addLinesInternal(vStart, vEnd);
      writeUnlock();
   }
   
   private void addLinesInternal(int vStart, int vEnd) {
      for (int i=vStart; i<vEnd; i+=2) {
         addLinePairInternal(i, i+1);
      }
   }

   /**
    * Creates a set of line primitives between the supplied pairs of vertices.
    * @param lines int pairs of vertex indices defining line segments.
    */
   public void addLines(Iterable<int[]> lines) {
      writeLock();
      for (int[] line : lines) {
         addLinePair(line);
      }
      writeUnlock();
   }
   
   /**
    * Creates a set of connected line segments between neighboring vertices
    * in the supplied list of vertex indices.  For example, supplying {1,2,3,4}
    * will create three line segments: 1-2, 2-3, 3-4.
    * @param vidxs vertex indices specifying connectivity
    */
   public void addLineStrip(int... vidxs) {
      writeLock();
      for (int i=0; i<vidxs.length-1; ++i) {
         addLinePairInternal(vidxs[i], vidxs[i+1]);
      }
      writeUnlock();
   }
   
   /**
    * Creates a set of connected line primitives between neighboring vertices 
    * as specified by the supplied vertex range.  For example, supplying 
    * {1,4} will create three line segments: 1-2, 2-3, 3-4.
    * @param vStart starting vertex
    * @param vEnd ending vertex  
    */
   public void addLineStrip(int vStart, int vEnd) {
      writeLock();
      addLineStripInternal(vStart, vEnd);
      writeUnlock();
   }
   
   private void addLineStripInternal(int vStart, int vEnd) {
      for (int i=vStart; i<vEnd; ++i) {
         addLinePairInternal(i, i+1);
      }
   }

   /**
    * Creates a set of connected line segments between neighboring vertices
    * in the supplied list of vertex indices.  For example, supplying {1,2,3,4}
    * will create four line segments: 1-2, 2-3, 3-4, 4-1.
    * @param vidxs vertex indices specifying connectivity
    */
   public void addLineLoop(int... vidxs) {
      writeLock();
      for (int i=0; i<vidxs.length-1; ++i) {
         addLinePairInternal(vidxs[i], vidxs[i+1]);
      }
      if (vidxs.length > 1) {
         addLinePairInternal(vidxs[vidxs.length-1], vidxs[0]);
      }
      writeUnlock();
   }
   
   /**
    * Creates a set of line primitives between neighboring vertices as specified
    * by the supplied vertex range.  For example, supplying 
    * {1,4} will create four line segments: 1-2, 2-3, 3-4, 4-1
    * @param vStart starting vertex
    * @param vEnd ending vertex  
    */
   public void addLineLoop(int vStart, int vEnd) {
      writeLock();
      addLineLoopInternal(vStart, vEnd);
      writeUnlock();
   }
   
   private void addLineLoopInternal(int vStart, int vEnd) {
      for (int i=vStart; i<vEnd; ++i) {
         addLinePairInternal(i, i+1);
      }
      if (vEnd != vStart) {
         addLinePairInternal(vEnd, vStart);
      }
   }
   

   /**
    * Creates a line primitive between two new vertices defined
    * at the supplied locations.  The currently active normal, color and texture
    * coordinates will be used when creating the vertices.
    * @param v0 length 3 array for position of starting vertex (x,y,z)
    * @param v1 length 3 array for position of ending vertex (x,y,z)
    */
   public void addLine(float[] v0, float[] v1) {
      writeLock();
      int v0idx = vertexInternal(v0);
      int v1idx = vertexInternal(v1);
      addLine(v0idx, v1idx);
      writeUnlock();
   }

   /**
    * Indicates whether any line primitives have been defined
    */
   public boolean hasLines() {
      return (numLineGroups() > 0);
   }

   /**
    * Number of line primitives defined
    */
   public int numLines() {
      if (currentLineGroup == null) {
         return 0;
      }
      return currentLineGroup.size()/LINE_STRIDE;
   }

   /**
    * Returns a list of vertex indices of all line primitives defined
    * in the current group.
    * Lines are separated by a stride that can be queried with
    * {@link #getLineStride()}.
    * 
    * @return an array of vertex indices defining the lines
    */
   public int[] getLines() {
      if (hasLines()) {
         currentLineGroup.trimToSize ();
         return currentLineGroup.getArray ();
      }
      return null;
   }
   
   /**
    * @return stride between line primitives
    */
   public int getLineStride() {
      return LINE_STRIDE;
   }
   
   /**
    * Hint for ensuring sufficient storage for lines
    * @param cap capacity
    */
   public void ensureLineCapacity(int cap) {
      writeLock();
      if (stateInfo.numLineGroups == 0) {
         createLineGroupInternal();
      }
      currentLineGroup.ensureCapacity(cap);
      writeUnlock();
   }
   
   private int createLineGroupInternal() {
      VertexIndexArray newLines = new VertexIndexArray();
      lines.add(newLines);
      currentLineGroup = newLines;
      int idx = stateInfo.numLineGroups;
      stateInfo.lineGroupIdx = idx;
      stateInfo.numLineGroups++;
      notifyLinesModifiedInternal ();
      return idx;
   }

   /**
    * Creates a new group of lines that can be rendered.  By default,
    * only one line group exists.
    * @return the index of the new group of lines
    * @see #lineGroup(int)
    */
   public int createLineGroup() {
      writeLock();
      int idx = createLineGroupInternal ();
      writeUnlock();
      return idx;
   }

   /**
    * Sets the current active line group for rendering.
    * @param setIdx index of active line group.
    */
   public void lineGroup(int setIdx) {
      writeLock();
      stateInfo.lineGroupIdx = setIdx;
      if (lines != null) {
         currentLineGroup = lines.get(stateInfo.lineGroupIdx);
      }
      writeUnlock();
   }

   /**
    * Returns the index of the currently active line group.
    */
   public int getLineGroupIdx() {
      return stateInfo.lineGroupIdx;
   }

   /**
    * The number of line groups available.
    */
   public int numLineGroups() {
      return stateInfo.numLineGroups;
   }

   /**
    * Number of line primitives defined in a group.
    */
   public int numLines(int lgroup) {
      if (lines == null || lines.size() <= lgroup) {
         return 0;
      }
      int n = lines.get(lgroup).size()/LINE_STRIDE;
      
      return n;
   }
 
   /**
    * Returns a list of vertex indices of all point primitives defined
    * in the requested group.
    * Lines are separated by a stride that can be queried with
    * {@link #getPointStride()}.
    * 
    * @param lgroup line group index
    * @return an array of vertex indices defining the lines
    */
   public int[] getLines(int lgroup) {
      int[] out = null;
      if (hasLines()) {
         VertexIndexArray ll = lines.get (lgroup);
         ll.trimToSize ();
         out = ll.getArray ();
         
      } 
      return out;
   }

   /**
    * Returns the latest lines version number,
    * for use in detecting if changes are present.
    */
   public int getLinesVersion() {
      if (linesModified) {
         
         versionInfo.linesVersion++;
         linesModified = false;
         
      }
      return versionInfo.linesVersion;
   }

   /**
    * Informs the render object that triangles have been modified outside of its control.
    */
   public void notifyTrianglesModified() {
      writeLock();
      notifyTrianglesModifiedInternal ();
      writeUnlock();
   }
   
   private void notifyTrianglesModifiedInternal() {
      trianglesModified = true;
      totalModified = true;
   }
   
   private void addTriangleTripleInternal(int[] vidxs) {
      if (stateInfo.numTriangleGroups == 0) {
         createTriangleGroupInternal();
      }
      currentTriangleGroup.addAll(vidxs);
      notifyTrianglesModifiedInternal ();
   }
   
   private void addTriangleTripleInternal(int v0, int v1, int v2) {
      if (stateInfo.numTriangleGroups == 0) {
         createTriangleGroupInternal();
      }
      currentTriangleGroup.add(v0);
      currentTriangleGroup.add(v1);
      currentTriangleGroup.add(v2);
      notifyTrianglesModifiedInternal ();
   }

   /**
    * Creates a triangle primitive between supplied vertices (CCW order).
    * @param v0idx first vertex
    * @param v1idx second vertex
    * @param v2idx third vertex
    */
   public void addTriangle(int v0idx, int v1idx, int v2idx) {
      writeLock();
      addTriangleTripleInternal(new int[] {v0idx, v1idx, v2idx});
      writeUnlock();
   }

   /**
    * Creates a set of triangle primitives between triples of vertices as 
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create two triangles: 1-2-3, 4-5-6.
    * @param vidxs vertex indices, in triples, defining triangles.
    */
   public void addTriangles(int... vidxs) {
      writeLock();
      for (int i=0; i<vidxs.length-2; i+=3) {
         addTriangleTripleInternal(new int[]{vidxs[i], vidxs[i+1], vidxs[i+2]});
      }
      writeUnlock();
   }
   
   /**
    * Creates a set of triangle primitives between triples of vertices as 
    * supplied by the given vertex range.  For example supplying {1, 6} will
    * create two triangles: 1-2-3, 4-5-6.
    * @param vStart starting vertex index
    * @param vEnd ending vertex index
    */
   public void addTriangles(int vStart, int vEnd) {
      writeLock();
      addTrianglesInternal (vStart, vEnd);
      writeUnlock();
   }
   
   private void addTrianglesInternal(int vStart, int vEnd) {
      for (int i=vStart; i<vEnd-1; i+=3) {
         addTriangleTripleInternal(new int[]{i, i+1, i+2});
      }
   }

   /**
    * Creates a set of triangle primitives forming a fan using the
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create four triangles: 1-2-3, 1-3-4, 1-4-5, 1-5-6.
    * @param vidxs vertex indices defining triangles.
    */
   public void addTriangleFan(int... vidxs) {
      writeLock();
      for (int i=2; i<vidxs.length; ++i) {
         addTriangleTripleInternal(new int[]{vidxs[0], vidxs[i-1], vidxs[i]});
      }
      writeUnlock();
   }
   
   /**
    * Creates a set of triangle primitives forming a fan using the
    * supplied vertex range.  For example supplying {1, 6} will
    * create four triangles: 1-2-3, 1-3-4, 1-4-5, 1-5-6.
    * @param vStart starting vertex index
    * @param vEnd ending vertex index
    */
   public void addTriangleFan(int vStart, int vEnd) {
      writeLock();
      addTriangleFanInternal(vStart, vEnd);
      writeUnlock();
   }
   
   private void addTriangleFanInternal(int vStart, int vEnd) {
      for (int i=vStart+2; i<=vEnd; ++i) {
         addTriangleTripleInternal(new int[]{vStart, i-1, i});
      }
   }

   /**
    * Creates a set of triangle primitives forming a strip using the
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create four triangles: 1-2-3, 3-2-4, 3-4-5, 5-4-6
    * @param vidxs vertex indices defining triangles.
    */
   public void addTriangleStrip(int... vidxs) {
      writeLock();
      // add pairs of triangles
      for (int i=2; i<vidxs.length-1; i+=2) {
         addTriangleTripleInternal(new int[]{vidxs[i-2], vidxs[i-1], vidxs[i]});
         addTriangleTripleInternal(new int[]{vidxs[i], vidxs[i-1], vidxs[i+1]});
      }
      // add last triangle
      int i = vidxs.length-1;
      if (i > 2 && i % 2 == 0) {
         addTriangleTripleInternal(new int[]{vidxs[i-2], vidxs[i-1], vidxs[i]});
      }
      writeUnlock();
   }
   
   /**
    * Creates a set of triangle primitives forming a strip using the
    * supplied by vertex range.  For example supplying {1,6} will
    * create four triangles: 1-2-3, 3-2-4, 3-4-5, 5-4-6
    * @param vStart starting vertex index
    * @param vEnd ending vertex index
    */
   public void addTriangleStrip(int vStart, int vEnd) {
      writeLock();
      addTriangleStripInternal(vStart, vEnd);
      writeUnlock();
   }
   
   private void addTriangleStripInternal(int vStart, int vEnd) {
      // add pairs of triangles
      for (int i=vStart+2; i<vEnd; i+=2) {
         addTriangleTripleInternal(new int[]{i-2, i-1, i});
         addTriangleTripleInternal(new int[]{i, i-1, i+1});
      }
      // add last triangle
      int i = vEnd-vStart;
      if (i > 2 && i % 2 == 0) {
         addTriangleTripleInternal(new int[]{vEnd-2, vEnd-1, vEnd});
      }
   }

   /**
    * Creates a set of triangle primitives between supplied triples of 
    * vertices.
    * @param tris int triples of vertex indices defining triangles (CCW)
    */
   public void addTriangles(Iterable<int[]> tris) {
      writeLock();
      for (int[] tri : tris) {
         addTriangleTripleInternal(tri);
      }
      writeUnlock();
   }

   /**
    * Creates a triangle primitive between three new vertices defined at the
    * supplied locations.  The currently active normal, color, and texture
    * coordinates will be used when creating the vertices.
    * @param v0 {x,y,z} position of first vertex
    * @param v1 {x,y,z} position of second vertex
    * @param v2 {x,y,z} position of third vertex
    */
   public void addTriangle(float[] v0, float[] v1, float[] v2) {
      writeLock();
      int v0idx = vertexInternal(v0);
      int v1idx = vertexInternal(v1);
      int v2idx = vertexInternal(v2);
      addTriangleTripleInternal (v0idx, v1idx, v2idx);
      writeUnlock();
   }

   /**
    * Indicates whether any triangle primitives have been defined.
    */
   public boolean hasTriangles() {
      return (numTriangleGroups() > 0);
   }

   /**
    * Number of triangle primitives defined.
    */
   public int numTriangles() {
      if (currentTriangleGroup == null) {
         return 0;
      }
      return currentTriangleGroup.size()/TRIANGLE_STRIDE;
   }

   /**
    * Returns a list of vertex indices of all triangle primitives defined
    * in the current group.
    * Triangles are separated by a stride that can be queried with
    * {@link #getTriangleStride()}.
    * 
    * @return an array of vertex indices defining the triangles
    */
   public int[] getTriangles() {
      if (hasTriangles()) {
         currentTriangleGroup.trimToSize ();
         return currentTriangleGroup.getArray ();
      } 
      return null;
   }
   
   /**
    * @return stride between triangle primitives
    * @see #getTriangles()
    */
   public int getTriangleStride() {
      return TRIANGLE_STRIDE;
   }

   /**
    * Hint for ensuring sufficient storage for triangles
    * @param cap capacity
    */
   public void ensureTriangleCapacity(int cap) {
      writeLock();
      if (stateInfo.numTriangleGroups == 0) {
         createTriangleGroupInternal();
      }
      currentTriangleGroup.ensureCapacity(cap);
      writeUnlock();
   }
   
   /**
    * Creates a new group of triangles that can be rendered.  By default,
    * only one triangle group exists.
    * @return the index of the new group of triangles
    * @see #triangleGroup(int)
    */
   public int createTriangleGroup() {
      writeLock();
      int idx = createTriangleGroupInternal ();
      writeUnlock();
      return idx;
   }
   
   private int createTriangleGroupInternal() {
      VertexIndexArray newTriangles = new VertexIndexArray();
      triangles.add(newTriangles);
      currentTriangleGroup = newTriangles;
      int idx = stateInfo.numTriangleGroups;
      stateInfo.triangleGroupIdx = idx;
      stateInfo.numTriangleGroups++;
      notifyTrianglesModifiedInternal ();
      return idx;
   }

   /**
    * Sets the current active triangle group for rendering.
    * @param setIdx index of active triangle group.
    */
   public void triangleGroup(int setIdx) {
      writeLock();
      stateInfo.triangleGroupIdx = setIdx;
      if (triangles != null) {
         currentTriangleGroup = triangles.get(stateInfo.triangleGroupIdx);
      }
      writeUnlock();
   }

   /**
    * Returns the index of the currently active triangle group.
    */
   public int getTriangleGroupIdx() {
      return stateInfo.triangleGroupIdx;
   }

   /**
    * The number of triangle groups available.
    */
   public int numTriangleGroups() {
      return stateInfo.numTriangleGroups;
   }

   /**
    * Number of triangle primitives defined in a group.
    */
   public int numTriangles(int tgroup) {
      if (triangles == null || triangles.size() <= tgroup) {
         return 0;
      }
      int s = triangles.get(tgroup).size()/TRIANGLE_STRIDE;
      
      return s;
   }

   /**
    * Returns a list of vertex indices of all triangle primitives defined
    * in the requested group.
    * Triangles are separated by a stride that can be queried with
    * {@link #getTriangleStride()}.
    * 
    * @param tgroup triangle group index
    * @return an array of vertex indices defining the triangles
    */
   public int[] getTriangles(int tgroup) {
      int[] out = null;
      if (hasTriangles ()) {
         
         VertexIndexArray tris = triangles.get (tgroup);
         tris.trimToSize ();
         out = tris.getArray ();
         
      }
      return out;
   }

   /**
    * Returns the latest triangles version number,
    * for use in detecting if changes are present.
    */
   public int getTrianglesVersion() {
      if (trianglesModified) {
         
         versionInfo.trianglesVersion++;
         trianglesModified = false;
         
      }
      return versionInfo.trianglesVersion;
   }

   /**
    * Clears all primitive groups, allowing them to be recreated.  Vertex
    * information remains untouched.
    */
   public void clearPrimitives() {
      writeLock();
      
      // in most cases will only have one group
      points = new ArrayList<>(1);
      lines = new ArrayList<>(1);
      triangles = new ArrayList<>(1);

      stateInfo.pointGroupIdx = -1;
      stateInfo.lineGroupIdx = -1;
      stateInfo.triangleGroupIdx = -1;

      currentPointGroup = null;
      currentLineGroup = null;
      currentTriangleGroup = null;

      stateInfo.numPointGroups = 0;
      stateInfo.numLineGroups = 0;
      stateInfo.numTriangleGroups = 0;

      // should trigger version count increase
      pointsModified = true;
      linesModified = true;
      trianglesModified = true;
      totalModified = true;
      
      writeUnlock();
   }

   /**
    * Clears everything in the RenderObject, allowing it to be
    * recreated.  This can be used when the object becomes invalid,
    * in place of discarding and regenerating a new one.  The object
    * becomes a clean slate, with no vertex attributes or primitives.
    */
   public void clearAll() {
      
      writeLock();
      
      // in most cases, we will only have one set
      positions = new ArrayList<>(1);
      normals = new ArrayList<>(1);
      colors = new ArrayList<>(1);
      texcoords = new ArrayList<>(1);

      currentPositionIdx = -1;
      currentNormalIdx = -1;
      currentColorIdx = -1;
      currentTextureIdx = -1;

      stateInfo.numPositions = 0;
      stateInfo.numNormals = 0;
      stateInfo.numColors = 0;
      stateInfo.numTexcoords = 0;

      positions = new ArrayList<> ();
      normals = new ArrayList<> ();
      colors = new ArrayList<> ();
      texcoords = new ArrayList<> ();

      positionsModified = true;
      normalsModified = true;
      colorsModified = true;
      texturesModified = true;
      totalModified = true;

      vertices = new int[0];
      numVertices = 0;
      vertexBufferMask = 0;
      vertexCapacity = 0;
      vertexPositionOffset = -1;
      vertexNormalOffset = -1;
      vertexColorOffset = -1;
      vertexTexcoordOffset = -1;
      vertexStride = 0;
      
      verticesModified = true;

      clearPrimitives();
      
      buildMode = null;
      buildModeStart = -1;
      
      writeUnlock();
      
   }

   //=========================================================================
   // Usage flags
   //=========================================================================

   /**
    * Retrieves the version of the object, for use in detecting
    * whether any information has changed since last use.
    * @return the overall modification version
    */
   public int getVersion() {
      // update all other versions
      getPositionsVersion();
      getNormalsVersion();
      getColorsVersion();
      getTextureCoordsVersion();
      getVerticesVersion ();
      getPointsVersion();
      getLinesVersion();
      getTrianglesVersion();
      
      if (totalModified) {
         
         versionInfo.totalVersion++;
         totalModified = false;
         
      }
      return versionInfo.totalVersion;
   }

   /**
    * Returns whether or not this RenderObject is valid.  If valid,
    * this object can be safely passed to a renderer for drawing.  
    * If not, it needs to be discarded.
    * @return <code>true</code> if this RenderObject is valid
    */
   public boolean isValid() {
      return !idInfo.isDisposed();
   }
   
   /**
    * Sets or clears the transient state of the render object.  Transient
    * objects will not be cached by renderers, which may improve performance
    * for short-lived objects.
    * @param set if <code>true</code>, sets this object to be transient
    */
   public void setTransient(boolean set) {
      writeLock();
      istransient = set;
      writeUnlock();
   }
   
   /**
    * Checks if the render object is labelled as "transient".  Transient objects
    * will not be cached by renderers, which may improve performance for short-lived
    * objects.
    * @return whether or not this render object is considered transient.
    */
   public boolean isTransient() {
      return istransient;
   }

   /**
    * Signal a destruction of the object.
    */
   public void dispose() {
      writeLock();
      
      idInfo.dispose();
      
      // clear all memory
      positions = null;
      normals = null;
      colors = null;
      texcoords = null;

      positions = null;
      normals = null;
      colors = null;
      texcoords = null;

      vertices = null;
      
      points = null;
      lines = null;
      triangles = null;

      currentPointGroup = null;
      currentLineGroup = null;
      currentTriangleGroup = null;
      
      writeUnlock();
   }

   /**
    * Garbage collection, clear memory and dispose
    */
   @Override
   protected void finalize() throws Throwable {
      dispose();
   }
   
   /**
    * @return a new copy of the object
    */
   protected RenderObject copy()  {

      RenderObject r = new RenderObject();

      readLock();
      
      if (positions != null) {
         r.positions = new ArrayList<float[]>(positions.size ());
         r.positions.addAll (positions);
      } else {
         r.positions = null;
      }

      if (normals != null) {
         r.normals = new ArrayList<>(normals.size());
         r.normals.addAll (normals);
      } else {
         r.normals = null;
      }

      if (colors != null) {
         r.colors = new ArrayList<>(colors.size());
         r.colors.addAll (colors);
      } else {
         r.colors = null;
      }

      if (texcoords != null) {
         r.texcoords = new ArrayList<>(texcoords.size());
         r.texcoords.addAll (texcoords);
      } else {
         r.texcoords = null;
      }

      r.stateInfo = stateInfo.clone();

      r.currentPositionIdx = currentPositionIdx;
      r.currentNormalIdx = currentNormalIdx;
      r.currentColorIdx = currentColorIdx;
      r.currentTextureIdx = currentTextureIdx;

      // keep track separately to allow storage to be cleared
      r.stateInfo.numPositions = stateInfo.numPositions;
      r.stateInfo.numNormals = stateInfo.numNormals;
      r.stateInfo.numColors = stateInfo.numColors;
      r.stateInfo.numTexcoords = stateInfo.numTexcoords;

      // indicators that attributes have been modified
      r.positionsModified = positionsModified;
      r.normalsModified = normalsModified;
      r.colorsModified = colorsModified;
      r.texturesModified = texturesModified;

      r.versionInfo = versionInfo.clone();

      r.vertices = Arrays.copyOf (vertices, vertices.length);
      r.numVertices = numVertices;
      r.vertexBufferMask = vertexBufferMask;
      r.vertexCapacity = vertexCapacity;
      r.vertexPositionOffset = vertexPositionOffset;
      r.vertexNormalOffset = vertexNormalOffset;
      r.vertexColorOffset = vertexColorOffset;
      r.vertexTexcoordOffset = vertexTexcoordOffset;
      r.vertexStride = vertexStride;
      
      r.verticesModified = verticesModified;

      if (points != null) {
         r.points = new ArrayList<>(points.size());
         for (VertexIndexArray pnts : points) {
            VertexIndexArray npnts = pnts.clone();
            r.points.add(npnts);
         }
      } else {
         r.points = null;
      }

      if (lines != null) {
         r.lines = new ArrayList<>(lines.size());
         for (VertexIndexArray lns : lines) {
            r.lines.add(lns.clone ());
         }
      } else {
         r.lines = null;
      }

      if (triangles != null) {
         r.triangles = new ArrayList<>(triangles.size());
         for (VertexIndexArray tris : triangles) {
            r.triangles.add(tris.clone());
         }
      } else {
         r.triangles = null;
      }

      if (stateInfo.pointGroupIdx >= 0) {
         r.pointGroup(stateInfo.pointGroupIdx);
      }
      if (stateInfo.lineGroupIdx >= 0) {
         r.lineGroup(stateInfo.lineGroupIdx);
      }
      if (stateInfo.triangleGroupIdx >= 0) {
         r.triangleGroup(stateInfo.triangleGroupIdx);
      }

      r.pointsModified = pointsModified;
      r.linesModified = linesModified;
      r.trianglesModified = trianglesModified;

      r.totalModified = totalModified;

      r.buildMode = buildMode;
      r.buildModeStart = buildModeStart;
      
      r.istransient = istransient;
      
      readUnlock();
      
      return r;
   }

   @Override
   public boolean isDisposed() {
      return idInfo.isDisposed();
   }

   @Override
   public RenderObjectIdentifier getDisposeObserver() {
      return idInfo;
   }
   
  
}
