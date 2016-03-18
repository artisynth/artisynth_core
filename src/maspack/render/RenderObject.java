package maspack.render;

import java.awt.Color;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import maspack.render.Renderer.DrawMode;

public class RenderObject {

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
   public static class RenderObjectIdentifier {
      private int id;
      private boolean valid;
      private RenderObjectIdentifier(int id, boolean valid) {
         this.id = id;
         this.valid = valid;
      }
      
      /**
       * Unique identifying number
       */
      public int getId() {
         return id;
      }
      
      /**
       * Whether the associated RenderObject is still valid.
       * A RenderObject becomes invalid if it runs out of
       * scope.
       */
      public boolean isValid() {
         return valid;
      }
      
      private void setValid(boolean valid) {
         this.valid = valid;
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
    * current attribute set indices and primitive group indices.
    * Can be cloned so renderers can keep track of the latest state
    * they have observed.
    * 
    */
   public static class RenderObjectState {
      private int numPositionSets;
      private int numNormalSets;
      private int numColorSets;
      private int numTextureSets;
      
      private int numPointGroups;
      private int numLineGroups;
      private int numTriangleGroups;
      
      private int positionSetIdx;
      private int normalSetIdx;
      private int colorSetIdx;
      private int textureSetIdx;
      private int pointGroupIdx;
      private int lineGroupIdx;
      private int triangleGroupIdx; 
      
      private RenderObjectState() {
         numPositionSets = 0;
         numNormalSets = 0;
         numColorSets = 0;
         numTextureSets = 0;
         numPointGroups = 0;
         numLineGroups = 0;
         numTriangleGroups = 0;
         
         positionSetIdx = -1;
         normalSetIdx = -1;
         colorSetIdx = -1;
         textureSetIdx = -1;
         pointGroupIdx = -1;
         lineGroupIdx = -1;
         triangleGroupIdx = -1;
      }

      public int numPositionSets() {
         return numPositionSets;
      }

      public int numNormalSets() {
         return numNormalSets;
      }

      public int numColorSets() {
         return numColorSets;
      }

      public int numTextureCoordSets() {
         return numTextureSets;
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

      public int getPositionSetIdx() {
         return positionSetIdx;
      }

      public int getNormalSetIdx() {
         return normalSetIdx;
      }

      public int getColorSetIdx() {
         return colorSetIdx;
      }

      public int getTextureCoordSetIdx() {
         return textureSetIdx;
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
         result = prime * result + colorSetIdx;
         result = prime * result + lineGroupIdx;
         result = prime * result + normalSetIdx;
         result = prime * result + numColorSets;
         result = prime * result + numLineGroups;
         result = prime * result + numNormalSets;
         result = prime * result + numPointGroups;
         result = prime * result + numPositionSets;
         result = prime * result + numTextureSets;
         result = prime * result + numTriangleGroups;
         result = prime * result + pointGroupIdx;
         result = prime * result + positionSetIdx;
         result = prime * result + textureSetIdx;
         result = prime * result + triangleGroupIdx;
         return result;
      }

      public boolean equals(RenderObjectState other) {
         if (  positionSetIdx != other.positionSetIdx 
            || normalSetIdx != other.normalSetIdx
            || colorSetIdx != other.colorSetIdx
            || textureSetIdx != other.textureSetIdx
            || pointGroupIdx != other.pointGroupIdx
            || lineGroupIdx != other.lineGroupIdx
            || triangleGroupIdx != other.triangleGroupIdx) {
            return false;
         }
         
         if (  numPositionSets != other.numPositionSets
            || numNormalSets != other.numNormalSets
            || numColorSets != other.numColorSets
            || numTextureSets != other.numTextureSets
            || numPointGroups != other.numPointGroups
            || numLineGroups != other.numLineGroups
            || numTriangleGroups != other.numTriangleGroups) {
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
         c.numPositionSets = numPositionSets;
         c.numNormalSets = numNormalSets;
         c.numColorSets = numColorSets;
         c.numTextureSets = numTextureSets;
         c.numPointGroups = numPointGroups;
         c.numLineGroups = numLineGroups;
         c.numTriangleGroups = numTriangleGroups;
         c.positionSetIdx = positionSetIdx;
         c.normalSetIdx = normalSetIdx;
         c.colorSetIdx = colorSetIdx;
         c.textureSetIdx = textureSetIdx;
         c.pointGroupIdx = pointGroupIdx;
         c.lineGroupIdx = lineGroupIdx;
         c.triangleGroupIdx = triangleGroupIdx;
         return c;
      }

      public boolean hasPositions() {
         return numPositionSets > 0;
      }
      
      public boolean hasNormals() {
         return numNormalSets > 0;
      }
      
      public boolean hasColors() {
         return numColorSets > 0;
      }
      
      public boolean hasTextureCoords() {
         return numTextureSets > 0;
      }
      
   }
   
   /**
    * Class for storing position/normal/color/texture indices for a vertex
    */
   public static class VertexIndexSet {
      final int pidx;
      final int nidx;
      final int cidx;
      final int tidx;
      public VertexIndexSet(int p, int n, int c, int t) {
         pidx = p;
         nidx = n;
         cidx = c;
         tidx = t;
      }

      public int getPositionIndex() {
         return pidx;
      }

      public int getNormalIndex() {
         return nidx;
      }

      public int getColorIndex() {
         return cidx;
      }

      public int getTextureCoordIndex() {
         return tidx;
      }

      @Override
      public int hashCode() {
         return ((pidx*31+nidx)*31+cidx)*31+tidx;
      }
      
      public boolean equals(VertexIndexSet other) {
         if ( (pidx != other.pidx)
            || (nidx != other.nidx)
            || (cidx != other.cidx)
            || (tidx != other.tidx) ) {
            return false;
         }
         return true;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null) {
            return false;
         }
         if (getClass() != obj.getClass()) {
            return false;
         }
         VertexIndexSet other = (VertexIndexSet)obj;
         return equals(other);
      }

      @Override
      protected VertexIndexSet clone() {
         return new VertexIndexSet(pidx, nidx, cidx, tidx);
      }

   }
   
   RenderObjectIdentifier idInfo;
   RenderObjectVersion versionInfo;
   RenderObjectState stateInfo;
   
   ArrayList<ArrayList<float[]>> positions;
   ArrayList<ArrayList<float[]>> normals;
   ArrayList<ArrayList<byte[]>> colors;
   ArrayList<ArrayList<float[]>> textures;
   
   // number of each attribute
   int numPositions;
   int numNormals;
   int numColors;
   int numTextures;

   int currentPositionIdx;
   int currentNormalIdx;
   int currentColorIdx;
   int currentTextureIdx;

   // whether or not attributes can be updated once the object is
   // committed
   boolean positionsDynamic;
   boolean normalsDynamic;
   boolean colorsDynamic;
   boolean texturesDynamic;

   // pointers to locations within positions/normals/colors
   ArrayList<float[]> currentPositionSet;
   ArrayList<float[]> currentNormalSet;
   ArrayList<byte[]> currentColorSet;
   ArrayList<float[]> currentTextureSet;

   // indicators that attributes have been modified
   boolean positionsModified;
   boolean normalsModified;
   boolean colorsModified;
   boolean texturesModified;
   
   DrawMode buildMode;
   int buildModeStart;  // starting number of vertices when build mode began

   ArrayList<VertexIndexSet> vertices;
   int numVertices;
   boolean verticesModified;

   ArrayList<ArrayList<int[]>> points;
   ArrayList<ArrayList<int[]>> lines;
   ArrayList<ArrayList<int[]>> triangles;

   // pointers to positions in points, lines, triangles
   ArrayList<int[]> currentPointGroup;
   ArrayList<int[]> currentLineGroup;
   ArrayList<int[]> currentTriangleGroup;

   boolean pointsModified;
   boolean linesModified;
   boolean trianglesModified;

   boolean verticesCommitted;
   boolean primitivesCommitted;
   
   // boolean storageValid;

   boolean totalModified;


   boolean isTransient;

   public RenderObject() {

      idInfo = new RenderObjectIdentifier(nextIdNumber++, true);
      versionInfo = new RenderObjectVersion();
      stateInfo = new RenderObjectState(); 
      
      reinitialize();

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
      return versionInfo.clone();
   }
   
   public RenderObjectState getStateInfo() {
      return stateInfo.clone();
   }

   //=========================================================================
   // Positions, Normals, Colors, Textures
   //=========================================================================
   /**
    * Hint for ensuring sufficient storage for positions
    * @param cap capacity
    */
   public void ensurePositionCapacity(int cap) {
      if (stateInfo.numPositionSets == 0) {
         createPositionSet();
      }
      for (ArrayList<float[]> pset : positions) {
         pset.ensureCapacity(cap);
      }
   }

   /**
    * Adds an indexable 3D position
    * @param px 
    * @param py
    * @param pz
    * @return the index of the position added
    */
   public int addPosition(float px, float py, float pz) {
      return addPosition (new float[]{px,py,pz});
   }
   
   /**
    * Adds a position by reference.  If the position is modified outside of this render
    * object, then you must manually flag the change using {@link #notifyPositionsModified()}.
    * Otherwise, renderers are free to assume the positions have not changed.
    * @param xyz position vector
    * @return an index referring to the added position
    */
   public int addPosition (float[] xyz) {
      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot add positions once vertices are committed");
      }
      // create first position set
      if (stateInfo.numPositionSets == 0) {
         createPositionSet();
      }
      int pidx = numPositions;
      for (List<float[]> pset : positions) {
         pset.add(xyz);
      }
      numPositions++;
      notifyPositionsModified ();
      currentPositionIdx = pidx;
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
   
//   /**
//    * Sets the current 3D position to be used in following vertices.
//    * @param pos position coordinates
//    * @return The index of the new position (valid only if a vertex
//    * is added with the supplied position)
//    */
//   public int position(Vector3d pos) {
//      return position(
//         new float[] {(float)pos.x, (float)pos.y, (float)pos.z});
//   }
//   
//   /**
//    * Sets the current 3D position to be used in following vertices.
//    * @param px 
//    * @param py
//    * @param pz
//    * @return The index of the new position (valid only if a vertex
//    * is added with the supplied position)
//    */
//   public int position(float px, float py, float pz) {
//      return position(new float[]{px,py,pz});
//   }
//   
//   /**
//    * Sets the current 3D position to be used in following vertices, BY REFERENCE.
//    * @see #addPosition(float[])
//    * @see #setCurrentPosition(int)
//    * @param pos
//    * @return The index of the new position (valid only if a vertex
//    * is added with the supplied position)
//    */
//   public int position(float[] pos) {
//      int pidx = addPosition(pos);
//      currentPositionIdx = pidx;
//      return pidx;
//   }

   /**
    * Sets the current position to be used in following vertices, 
    * based on position index. 
    * @param pidx index of a previously added position
    */
   public void setCurrentPosition(int pidx) {
      if (pidx >= 0) {
         if (pidx >= currentPositionSet.size()) {
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
    * @param px
    * @param py
    * @param pz
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
      if (verticesCommitted) {
         if (!positionsDynamic) {
            throw new IllegalStateException(
               "Cannot modify non-dynamic positions once vertices are committed");
         }
      }
      currentPositionSet.set(pidx, pos);
      notifyPositionsModified ();
   }

   /**
    * Whether or not any positions have been defined.
    */
   public boolean hasPositions() {
      return (numPositionSets() > 0);
   }

   /**
    * Number of positions defined.
    */
   public int numPositions() {
      return numPositions;
   }

   /**
    * Retrieves the position at the supplied index.  If the returned position
    * is modified, then {@link #notifyPositionsModified()} must be manually called.
    * @param pidx position index
    * @return position {x,y,z}
    */
   public float[] getPosition(int pidx) {
      if (pidx < 0) {
         return null;
      }
      return currentPositionSet.get(pidx);
   }

   /**
    * Retrieves the full list of positions.  This list should not
    * be modified.
    * @return list of positions.
    */
   public List<float[]> getPositions() {
      if (hasPositions()) {
         return Collections.unmodifiableList(currentPositionSet);
      }
      return null;
   }

   /**
    * Sets whether or not positions should be considered dynamic.  If true,
    * positions can be updated.  Otherwise, positions are 
    * considered fixed for all time.  The dynamic property can only be 
    * modified before the vertices are committed.
    * @see #commit()
    */
   public void setPositionsDynamic(boolean set) {
      if (set != positionsDynamic) {
         if (verticesCommitted) {
            throw new IllegalStateException(
               "Cannot modify dynamic property once vertices are committed");
         }
         else if (set && isTransient()) {
            throw new IllegalStateException(
               "Cannot make transient object dynamic");
         }
         positionsDynamic = set;
      }
   }

   /**
    * Returns whether or not positions are considered dynamic.
    */
   public boolean isPositionsDynamic() {
      return positionsDynamic;
   }

   /**
    * Indicate that the positions have been modified.
    */
   public void notifyPositionsModified() {
      positionsModified = true;
      totalModified = true;
   }
   
   /**
    * Creates a new, alternative set of positions that can be used by vertices.  
    * This allows sharing of other resources, such as normals, colors and 
    * textures.  The new set of positions immediately becomes active, and
    * is initialized from the previously active position set.
    * @return the index of the newly created position set
    * @see #positionSet(int)
    */
   public int createPositionSet() {
      return createPositionSetFrom(stateInfo.positionSetIdx);
   }

   /**
    * Creates a new, alternative set of positions that can be used by vertices.  
    * This allows sharing of other resources, such as normals, colors and 
    * textures.  The new set of positions immediately becomes active, and 
    * is initialized from the position set identified by the supplied index.
    * @param copyIdx index of position set to duplicate
    * @return the index of the newly created position set
    * @see #positionSet(int)
    */
   public int createPositionSetFrom(int copyIdx) {
      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot add a new set of positions once vertices are committed");
      }
      ArrayList<float[]> newPositions;
      if (stateInfo.numPositionSets == 0) {
         newPositions = new ArrayList<>();
      } else {
         if (copyIdx < 0) {
            copyIdx = 0;
         }
         newPositions = new ArrayList<>(positions.get(copyIdx));
      }
      int idx = stateInfo.numPositionSets;
      positions.add(newPositions);
      currentPositionSet = newPositions;
      stateInfo.positionSetIdx = idx;
      stateInfo.numPositionSets++;
      notifyPositionsModified ();
      return idx;
   }

   /**
    * Sets the current active position set to be used/modified.  Vertices
    * will use the current active set of positions when rendering.
    * @param setIdx index of active position set
    */
   public void positionSet(int setIdx) {
      stateInfo.positionSetIdx = setIdx;
      if (positions != null) {
         currentPositionSet = positions.get(stateInfo.positionSetIdx);
      }
   }

   /**
    * Returns the index of the currently active position set.
    */
   public int getPositionSetIdx() {
      return stateInfo.positionSetIdx;
   }

   /**
    * The number of position sets available.
    */
   public int numPositionSets() {
      return stateInfo.numPositionSets;
   }

   /**
    * Retrieves the position at the supplied index in a given set.  
    * If the returned positions are modified, then the render
    * object must be notified with {@link #notifyPositionsModified()}.
    * @param pset position set index
    * @param pidx position index
    * @return position {x,y,z}
    */
   public float[] getPosition(int pset, int pidx) {
      if (pidx < 0) {
         return null;
      }
      return positions.get(pset).get(pidx);
   }

   /**
    * Retrieves the full list of positions in a given set.  If entries
    * in this list are modified, the render object must be notified
    * with {@link #notifyPositionsModified()}.
    * @param pset position set index
    * @return list of positions.
    */
   public List<float[]> getPositions(int pset) {
      return Collections.unmodifiableList(positions.get(pset));
   }

   /**
    * Returns the latest committed positions version number,
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
      if (stateInfo.numNormalSets == 0) {
         createNormalSet();
      }
      for (ArrayList<float[]> nset : normals) {
         nset.ensureCapacity(cap);
      }
   }

   /**
    * Adds an indexable 3D normal.
    * @param nx
    * @param ny
    * @param nz
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
      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot add normals once vertices are committed");
      }
      // create first normal set
      if (stateInfo.numNormalSets == 0) {
         createNormalSet();
      }
      int nidx = numNormals;
      for (List<float[]> nset : normals) {
         nset.add(nrm);
      }
      numNormals++;
      notifyNormalsModified();
      currentNormalIdx = nidx;
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
   
//   /**
//    * Sets the current 3D normal to be used in following
//    * vertices.
//    * @param nx 
//    * @param ny
//    * @param nz
//    * @return The index of the new normal (valid only if a vertex
//    * is added with the supplied normal)
//    */
//   public int normal(float nx, float ny, float nz) {
//      return normal(new float[]{nx,ny,nz});
//   }
//
//   /**
//    * Sets the current 3D normal to be used in following vertices.
//    * @param  nrm normal coordinates
//    * @return The index of the new normal (valid only if a vertex
//    * is added with the supplied normal)
//    */
//   public int normal(Vector3d nrm) {
//      return normal(
//         new float[] {(float)nrm.x, (float)nrm.y, (float)nrm.z});
//   }
   
//    /**
//    * Sets the current 3D normal, by reference, to be used in following vertices.
//    * @see #addNormal(float[])
//    * @see #normal(int)
//    * @param nrm
//    * @return The index of the new normal (valid only if a vertex is added
//    * with the supplied normal).
//    */
//   public int normal(float[] nrm) {
//      int nidx = addNormal(nrm);
//      //currentNormalIdx = nidx;
//      return nidx;
//   }
   
   /**
    * Sets the current normal to be used in following vertices, 
    * based on normal index. 
    * @param nidx index of a previously added normal
    */
   public void setCurrentNormal(int nidx) {
      if (nidx >= 0) {
         if (nidx >= currentNormalSet.size()) {
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
    * @param nx
    * @param ny
    * @param nz
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
      if (verticesCommitted) {
         if (!normalsDynamic) {
            throw new IllegalStateException(
               "Cannot modify non-dynamic normals once vertices are committed");
         }
      }
      currentNormalSet.set(nidx, nrm);
      notifyNormalsModified();
   }

   /**
    * Whether or not any normals have been defined.
    */
   public boolean hasNormals() {
      return (numNormalSets() > 0);
   }

   /**
    * Number of normals defined.
    */
   public int numNormals() {
      return numNormals;
   }

   /**
    * Retrieves the normal at the supplied index.  If the returned
    * normal is modified, then {@link #notifyNormalsModified()} must
    * be called.
    * @param nidx normal index
    * @return normal {x,y,z}
    */
   public float[] getNormal(int nidx) {
      if (nidx < 0) {
         return null;
      }
      return currentNormalSet.get(nidx);
   }

   /**
    * Retrieves the full list of normals.  If the contents
    * of this list are modified, the method {@link #notifyNormalsModified()}
    * must be called.
    * @return list of normals.
    */
   public List<float[]> getNormals() {
      if (hasNormals()) {
         return Collections.unmodifiableList(currentNormalSet);
      }
      return null;
   }

   /**
    * Sets whether or not normals should be considered dynamic.  If true,
    * normals can be updated.  Otherwise, normals are considered fixed for 
    * all time.  The dynamic property can only be modified 
    * before the vertices are committed.
    * @see #commit()
    */
   public void setNormalsDynamic(boolean set) {
      if (set != normalsDynamic) {
         if (verticesCommitted) {
            throw new IllegalStateException(
               "Cannot modify dynamic property once vertices are committed");
         }
         else if (set && isTransient()) {
            throw new IllegalStateException(
               "Cannot make transient object dynamic");
         }
         normalsDynamic = set;
      }
   }

   /**
    * Returns whether or not normals are considered dynamic.
    */
   public boolean isNormalsDynamic() {
      return normalsDynamic;
   }
   
   /**
    * Indicate that the normals have been modified.
    */
   public void notifyNormalsModified() {
      normalsModified = true;
      totalModified = true;
   }

   /**
    * Creates a new, alternative set of normals that can be used by vertices.  
    * This allows sharing of other resources, such as positions, colors and 
    * textures.  The new set of normals immediately becomes active, and is
    * initialized from the previously active normal set.
    * @return the index of the newly created normal set
    * @see #normalSet(int)
    */
   public int createNormalSet() {
      return createNormalSetFrom(stateInfo.normalSetIdx);
   }

   /**
    * Creates a new, alternative set of normals that can be used by vertices.  
    * This allows sharing of other resources, such as positions, colors and 
    * textures.  The new set of normals will be initialized from the
    * normal set identified by the supplied index.
    * @param copyIdx index of normal set to duplicate
    * @return the index of the newly created normal set
    * @see #normalSet(int)
    */
   public int createNormalSetFrom(int copyIdx) {
      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot add a new set of normals once vertices are committed");
      }
      ArrayList<float[]> newNormals;
      if (stateInfo.numNormalSets == 0) {
         newNormals = new ArrayList<>();
      } else {
         if (copyIdx < 0) {
            copyIdx = 0;
         }
         newNormals = new ArrayList<>(normals.get(copyIdx));
      }
      int idx = stateInfo.numNormalSets;
      normals.add(newNormals);
      currentNormalSet = newNormals;
      stateInfo.normalSetIdx = idx;
      stateInfo.numNormalSets++;
      notifyNormalsModified ();
      return idx;
   }

   /**
    * Sets the current active normal set to be used/modified.  Vertices
    * will use the current active set of normals when rendering.
    * @param setIdx index of active normal set
    */
   public void normalSet(int setIdx) {
      stateInfo.normalSetIdx = setIdx;
      if (normals != null) {
         currentNormalSet = normals.get(stateInfo.normalSetIdx);
      }
   }

   /**
    * Returns the index of the currently active normal set.
    */
   public int getNormalSetIdx() {
      return stateInfo.normalSetIdx;
   }

   /**
    * The number of normal sets available.
    */
   public int numNormalSets() {
      return stateInfo.numNormalSets;
   }

   /**
    * Retrieves the normal at the supplied index in a given set.  
    * If the returned normal is modified, then {@link #notifyNormalsModified()} must be manually called.
    * @param nset normal set index
    * @param nidx normal index
    * @return normal {x,y,z}
    */
   public float[] getNormal(int nset, int nidx) {
      if (nidx < 0) {
         return null;
      }
      return normals.get(nset).get(nidx);
   }

   /**
    * Retrieves the full list of normals in a given set.  This list should 
    * not be modified.
    * @param nset normal set index
    * @return list of normals.
    */
   public List<float[]> getNormals(int nset) {
      return Collections.unmodifiableList(positions.get(nset));
   }

   /**
    * Returns the latest committed triangles version number,
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
      if (stateInfo.numColorSets == 0) {
         createColorSet();
      }
      for (ArrayList<byte[]> cset : colors) {
         cset.ensureCapacity(cap);
      }
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
      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot add colors once vertices are committed");
      }
      // create first color set
      if (stateInfo.numColorSets == 0) {
         createColorSet();
      }
      int cidx = numColors;
      for (List<byte[]> cset : colors) {
         cset.add(rgba);
      }
      numColors++;
      notifyColorsModified ();
      currentColorIdx = cidx;
      return cidx;
   }
   
   public void notifyColorsModified() {
      colorsModified = true;
      totalModified = true;
   }
   
//   /**
//    * Sets the current color to be used in following vertices.
//    * @param r red [0-255]
//    * @param g green [0-255]
//    * @param b blue [0-255]
//    * @param a alpha [0-255]
//    * @return The index of the new color (valid only if a vertex
//    * is added with the supplied color)
//    */
//   public int color(int r, int g, int b, int a) {
//      int cidx = addColor(r, g, b, a);
//      currentColorIdx = cidx;
//      return cidx;
//   }
//   
//   /**
//    * Sets the current color to be used in following vertices.
//    * @param r red [0-1]
//    * @param g green [0-1]
//    * @param b blue [0-1]
//    * @param a alpha [0-1]
//    * @return The index of the new color (valid only if a vertex
//    * is added with the supplied color)
//    */
//   public int color(float r, float g, float b, float a) {
//      int cidx = addColor(r, g, b, a);
//      currentColorIdx = cidx;
//      return cidx;
//   }

//   /**
//    * Sets the current color to be used in following vertices.
//    * @param color color from which RGBA values are determined
//    * @return The index of the new color (valid only if a vertex
//    * is added with the supplied color)
//    */
//   public int color(Color color) {
//      int cidx = addColor(color);
//      currentColorIdx = cidx;
//      return cidx;
//   }
//   
//   /**
//    * Sets the current color, by reference, to be used in following vertices.
//    * @see #addColor(byte[])
//    * @see #setCurrentColor(int)
//    * @param rgba {red, green, blue, alpha}
//    * @return The index of the new color (valid only if a vertex
//    * is added with the supplied color)
//    */
//   public int color(byte[] rgba) {
//      int cidx = addColor(rgba);
//      currentColorIdx = cidx;
//      return cidx;
//   }
//
   /**
    * Sets the current color to be used in following vertices
    * based on color index.
    * @param cidx index of a previously added color
    */
   public void setCurrentColor(int cidx) {
      if (cidx >= 0) {
         if (cidx >= currentColorSet.size()) {
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
      if (verticesCommitted) {
         if (!colorsDynamic) {
            throw new IllegalStateException(
               "Cannot modify non-dynamic colors once vertices are committed");
         }
      }
      currentColorSet.set(cidx, rgba);
      notifyColorsModified();
   }

   /**
    * Whether or not any colors have been defined.
    */
   public boolean hasColors() {
      return (numColorSets() > 0);
   }

   /**
    * Number of colors defined
    */
   public int numColors() {
      return numColors;
   }

   /**
    * Retrieves the color at the supplied index.  If the returned color
    * is modified, then {@link #notifyColorsModified()} must be manually called.
    * @param cidx color index
    * @return color {red, green, blue, alpha}
    */
   public byte[] getColor(int cidx) {
      if (cidx < 0) {
         return null;
      }
      return currentColorSet.get(cidx);
   }

   /**
    * Retrieves the full list of Colors.  This list should not
    * be modified.
    * @return list of colors.
    */
   public List<byte[]> getColors() {
      return Collections.unmodifiableList(currentColorSet);
   }

   /**
    * Sets whether or not colors should be considered dynamic.  If true,
    * colors can be updated.  Otherwise, colors are considered fixed for 
    * all time.  The dynamic property can only be modified before the
    * vertices are committed.
    * @see #commit()
    */
   public void setColorsDynamic(boolean set) {
      if (set != colorsDynamic) {
         if (verticesCommitted) {
            throw new IllegalStateException(
               "Cannot modify dynamic property once vertices are committed");
         }
         else if (set && isTransient()) {
            throw new IllegalStateException(
               "Cannot make transient object dynamic");
         }
         colorsDynamic = set;
      }
   }

   /**
    * Returns whether or not colors are considered dynamic.
    */
   public boolean isColorsDynamic() {
      return colorsDynamic;
   }

   /**
    * Creates a new, alternative set of colors that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals, 
    * and textures.  The new set of colors immediately becomes active, and 
    * is initialized from the previously active color set.
    * @return the index of the newly created color set
    * @see #colorSet(int)
    */
   public int createColorSet() {
      return createColorSetFrom(stateInfo.colorSetIdx);
   }

   /**
    * Creates a new, alternative set of colors that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals and 
    * textures.  The new set of colors immediately becomes active, and 
    * is initialized from the color set identified by the supplied index.
    * @param copyIdx index of color set to duplicate
    * @return the index of the newly created color set
    * @see #colorSet(int)
    */
   public int createColorSetFrom(int copyIdx) {
      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot add a new set of colors once vertices are committed");
      }
      ArrayList<byte[]> newColors;
      if (stateInfo.numColorSets == 0) {
         newColors = new ArrayList<>();
         stateInfo.colorSetIdx = 0;
      } else {
         if (copyIdx < 0) {
            copyIdx = 0;
         }
         newColors = new ArrayList<>(colors.get(copyIdx));
      }

      int idx = stateInfo.numColorSets;
      colors.add(newColors);
      currentColorSet = newColors;
      stateInfo.colorSetIdx = idx;
      stateInfo.numColorSets++;
      notifyColorsModified();
      return idx;
   }

   /**
    * Sets the current active color set to be used/modified.  Vertices
    * will use the current active set of colors when rendering.
    * @param setIdx index of active color set
    */
   public void colorSet(int setIdx) {
      stateInfo.colorSetIdx = setIdx;
      if (colors != null) {
         currentColorSet = colors.get(stateInfo.colorSetIdx);
      }
   }

   /**
    * Returns the index of the currently active color set.
    */
   public int getColorSetIdx() {
      return stateInfo.colorSetIdx;
   }

   /**
    * The number of color sets available.
    */
   public int numColorSets() {
      return stateInfo.numColorSets;
   }

   /**
    * Retrieves the color at the supplied index in a given set.  
    * If the returned color is modified, then {@link #notifyColorsModified()} must be manually called.
    * @param cset color set index
    * @param cidx color index
    * @return color {r,g,b,a}
    */
   public byte[] getColor(int cset, int cidx) {
      if (cidx < 0) {
         return null;
      }
      return colors.get(cset).get(cidx);
   }

   /**
    * Retrieves the full list of colors in a given set.  This list should 
    * not be modified.
    * @param cset color set index
    * @return list of colors.
    */
   public List<byte[]> getColors(int cset) {
      return Collections.unmodifiableList(colors.get(cset));
   }

   /**
    * Returns the latest committed colors version number,
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
      if (stateInfo.numTextureSets == 0) {
         createTextureCoordSet();
      }
      for (ArrayList<float[]> tset : textures) {
         tset.ensureCapacity(cap);
      }
   }

   /**
    * Adds an indexable 2D texture coordinate
    * @param tx
    * @param ty
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
    * @param xy
    * @return the index of the texture coordinate added
    */
   public int addTextureCoord(float[] xy) {
      
      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot add texture coordinates once vertices are committed");
      }
      // create first texture set
      if (stateInfo.numTextureSets == 0) {
         createTextureCoordSet();
      }
      int tidx = numTextures;
      for (List<float[]> tset : textures) {
         tset.add(xy);
      }
      numTextures++;
      notifyTextureCoordsModified();
      currentTextureIdx = tidx;
      return tidx;
   }
   
   public void notifyTextureCoordsModified() {
      texturesModified = true;
      totalModified = true;
   }

//   /**
//    * Sets the current 2D texture coordinate to be used in following vertices.
//    * @param x
//    * @param y
//    * @return The index of the new texture coordinate (valid only if a vertex
//    * is added with the supplied texture coordinate)
//    */
//   public int textureCoord(float x, float y) {
//     return textureCoord(new float[] {x,y});
//   }
//   
//   /**
//    * Sets the current 2D texture coordinate to be used in following vertices.
//    * @param xy the 2D coordinate
//    * @return The index of the new texture coordinate (valid only if a vertex
//    * is added with the supplied texture coordinate)
//    */
//   public int textureCoord(Vector2d xy) {
//     return textureCoord(new float[] {(float)xy.x,(float)xy.y});
//   }
//   
///**
//    * Sets the current 2D texture coordinate, by reference, to be used in following vertices.
//    * @see #addTextureCoord(float[])
//    * @see #setCurrentTextureCoord(int)
//    * @param xy the 2D coordinate
//    * @return The index of the new texture coordinate (valid only if a vertex
//    * is added with the supplied texture coordinate)
//    */
//   public int textureCoord(float[] xy) {
//      int tidx = addTextureCoord(xy);
//      currentTextureIdx = tidx;
//      return tidx;
//   }
//
   /**
    * Sets the current texture coordinates to be used in following vertices,
    * based on texture coordinate index.
    * @param tidx index of a previously added texture coordinate
    */
   public void setCurrentTextureCoord(int tidx) {
      if (tidx >= 0) {
         if (tidx >= currentTextureSet.size()) {
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
    * @param tidx
    * @param tx
    * @param ty
    */
   public void setTextureCoord(int tidx, float tx, float ty) {
      setTextureCoord (tidx, new float[] {tx,ty});
   }
   
   /**
    * Updates the values of the texture coordinate with index tidx.
    * @param tidx
    * @param xy new texture coordinates
    */
   public void setTextureCoord(int tidx, Vector2d xy) {
      setTextureCoord (tidx, new float[] {(float)xy.x,(float)xy.y});
   }
   
   /**
    * Updates the values of the texture coordinate with index tidx by reference.
    * @param tidx
    * @param xy
    */
   public void setTextureCoord(int tidx, float[] xy) {
      
      if (verticesCommitted) {
         if (!texturesDynamic) {
            throw new IllegalStateException(
               "Cannot modify non-dynamic texture coordinates once vertices are committed");
         }
      }
      currentTextureSet.set(tidx, xy);
      notifyTextureCoordsModified();
   }

   /**
    * Whether or not any texture coordinates have been defined.
    */
   public boolean hasTextureCoords() {
      return (numTextureCoordSets() > 0);
   }

   /**
    * Number of texture coordinates defined.
    */
   public int numTextureCoords() {
      return numTextures;
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
      return currentTextureSet.get(tidx);
   }

   /**
    * Retrieves the full list of texture coordinates.  This list should not
    * be modified.
    * @return list of texture coordinates.
    */
   public List<float[]> getTextureCoords() {
      if (hasTextureCoords()) {
         return Collections.unmodifiableList(currentTextureSet);
      }
      return null;
   }

   /**
    * Sets whether or not texture coordinates should be considered dynamic.  
    * If true, texture coordinates can be updated.  Otherwise, texture 
    * coordinates are considered fixed for all time.  The dynamic property 
    * can only be modified before the vertices are committed.
    * @see #commit() 
    */
   public void setTextureCoordsDynamic(boolean set) {
      if (set != texturesDynamic) {
         if (verticesCommitted) {
            throw new IllegalStateException(
               "Cannot modify dynamic property once vertices are committed");
         }
         else if (set && isTransient()) {
            throw new IllegalStateException(
               "Cannot make transient object dynamic");
         }
         texturesDynamic = set;
      }
   }

   /**
    * Returns whether or not texture coordinates are considered dynamic.
    */
   public boolean isTextureCoordsDynamic() {
      return texturesDynamic;
   }

   /**
    * Creates a new, alternative set of texture coordinates that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals, 
    * and colors.  The new set of texture coordinates immediately becomes active,
    * and is initialized from the previously active texture coordinate set.
    * @return the index of the newly create texture coordinate set
    * @see #textureCoordSet(int)
    */
   public int createTextureCoordSet() {
      return createTextureCoordSetFrom(stateInfo.textureSetIdx);
   }

   /**
    * Creates a new, alternative set of texture coordinates that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals, and
    * colors.  The new set of texture coordinates immediately becomes active,
    * and is initialized from the texture coordinate set identified by the 
    * supplied index.
    * @param copyIdx index of texture coordinate set to duplicate
    * @return the index of the newly created texture coordinate set
    * @see #textureCoordSet(int)
    */
   public int createTextureCoordSetFrom(int copyIdx) {
      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot add a new set of texture coordinates once vertices are committed");
      }
      // create first texture set

      ArrayList<float[]> newTextures;
      if (stateInfo.numTextureSets == 0) {
         newTextures = new ArrayList<>();
      } else {
         if (copyIdx < 0) {
            copyIdx = 0;
         }
         newTextures = new ArrayList<>(textures.get(copyIdx));
      }
      int idx = stateInfo.numTextureSets;
      textures.add(newTextures);
      currentTextureSet = newTextures;
      stateInfo.textureSetIdx = idx;
      stateInfo.numTextureSets++;
      notifyTextureCoordsModified();
      return idx;
   }

   /**
    * Sets the current active texture coordinate set to be used/modified.  Vertices
    * will use the current active set of texture coordinates when rendering.
    * @param setIdx index of active position set
    */
   public void textureCoordSet(int setIdx) {
      stateInfo.textureSetIdx = setIdx;
      if (textures != null) {
         currentTextureSet = textures.get(stateInfo.textureSetIdx);
      }
   }

   /**
    * Returns the index of the currently active texture coordinate set.
    */
   public int getTextureCoordSetIdx() {
      return stateInfo.textureSetIdx;
   }

   /**
    * The number of texture coordinate sets available.
    * @return number of texture coordinate sets
    */
   public int numTextureCoordSets() {
      return stateInfo.numTextureSets;
   }

   /**
    * Retrieves the texture coordinate at the supplied index in a given set.  
    * If the returned texture coordindate
    * is modified, then {@link #notifyTextureCoordsModified()} must be manually called.
    * @param tset texture coordinate set index
    * @param tidx texture coordinate index
    * @return texture coordinate {x,y}
    */
   public float[] getTextureCoord(int tset, int tidx) {
      if (tidx < 0) {
         return null;
      }
      return textures.get(tset).get(tidx);
   }

   /**
    * Retrieves the full list of texture coordinates in a given set.  This list should 
    * not be modified.
    * @param tset texture coordinate set index
    * @return list of texture coordinates.
    */
   public List<float[]> getTextureCoords(int tset) {
      return Collections.unmodifiableList(textures.get(tset));
   }

   /**
    * Returns the latest committed texture coordinates version number,
    * for use in detecting if changes are present.
    */
   public int getTextureCoordsVersion() {
      if (texturesModified) {
         versionInfo.texturesVersion++;
         texturesModified = false;
      }
      return versionInfo.texturesVersion;
   }

   /**
    * Checks whether this render object has any dynamic components.
    */
   public boolean isDynamic() {
      if (hasPositions() && isPositionsDynamic()) {
         return true;
      }
      if (hasNormals() && isNormalsDynamic()) {
         return true;
      }
      if (hasColors() && isColorsDynamic()) {
         return true;
      }
      if (hasTextureCoords() && isTextureCoordsDynamic()) {
         return true;
      }
      return false;
   }

   //=========================================================================
   // Vertices
   //=========================================================================

   /**
    * Hint for ensuring sufficient storage for vertices
    * @param cap capacity
    */
   public void ensureVertexCapacity(int cap) {
      vertices.ensureCapacity(cap);
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

      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot create a new vertex once vertices are committed");
      }
      VertexIndexSet idxs = new VertexIndexSet(pidx,nidx,cidx,tidx);
      int vidx = numVertices;
      vertices.add(idxs);
      numVertices++;
      verticesModified = true;
      totalModified = true;
      return vidx;
   }

   /**
    * Add a vertex at the supplied position, using the currently active
    * normal, color and texture coordinate (if available).  A new position
    * is created to accommodate the vertex.
    * @param px
    * @param py
    * @param pz
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
   
   /**
    * Add a vertex at the supplied position using the currently active
    * normal, color and texture coordinate (if available).  A new position
    * is created, by reference, to accommodate the vertex.
    * @see #addPosition(float[])
    * @see #addVertex(int)
    * @param xyz
    * @return vertex index
    */
   public int vertex(float[] xyz) {
      int pIdx = addPosition(xyz);
      return addVertex(pIdx);
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
      if (verticesCommitted) {
         throw new IllegalStateException(
            "Cannot create a new vertex once vertices are committed");
      }
      VertexIndexSet v = new VertexIndexSet(pidx, nidx, cidx, tidx);
      vertices.set(vidx, v);
      verticesModified = true;
      totalModified = true;
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
      VertexIndexSet idxs = vertices.get(vidx);
      return getPosition(idxs.pidx);
   }

   /**
    * Returns the normal of the supplied vertex
    */
   public float[] getVertexNormal(int vidx) {
      VertexIndexSet idxs = vertices.get(vidx);
      return getNormal(idxs.nidx);
   }

   /**
    * Returns the color of the supplied vertex
    */
   public byte[] getVertexColor(int vidx) {
      VertexIndexSet idxs = vertices.get(vidx);
      return getColor(idxs.cidx);
   }

   /**
    * Returns the texture coordinate of the supplied vertex
    */
   public float[] getVertexTextureCoord(int vidx) {
      VertexIndexSet idxs = vertices.get(vidx);
      return getTextureCoord(idxs.tidx);
   }

   /**
    * Retrieves the index set that identifies a vertex
    * @param vidx vertex index
    * @return the IndexSet which contains the position, normal, color and
    * texture coordinate indices
    */
   public VertexIndexSet getVertex(int vidx) {
      return vertices.get(vidx);
   }

   /**
    * Returns the full set of vertex identifiers
    */
   public List<VertexIndexSet> getVertices() {
      return Collections.unmodifiableList(vertices);
   }

   /**
    * Prevent further modification of vertex information.
    */
   private void commitVertices() {

      // maybe end building
      if (buildMode != null) {
         endBuild();
      }
      
      // compact memory
      positions.trimToSize();
      normals.trimToSize();
      colors.trimToSize();
      textures.trimToSize();

      for (ArrayList<float[]> pset : positions) {
         pset.trimToSize();
      }
      for (ArrayList<float[]> nset : normals) {
         nset.trimToSize();
      }
      for (ArrayList<byte[]> cset : colors) {
         cset.trimToSize();
      }
      for (ArrayList<float[]> tset : textures) {
         tset.trimToSize();
      }
      vertices.trimToSize();

      verticesCommitted = true;

   }

   /**
    * Checks whether vertex information has been committed
    */
   public boolean isVerticesCommitted() {
      return verticesCommitted;
   }

   /**
    * Returns the latest committed vertices version number,
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
    * using a specified mode.  Primitives are only gauranteed to be
    * complete after a call to {@link #endBuild()}.
    * @param mode mode for adding consecutive primitives
    */
   public void beginBuild(DrawMode mode) {
      if (buildMode != null) {
         endBuild();
      }
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
      
      int vStart = buildModeStart;
      int vEnd = numVertices()-1;
      buildModeStart = -1;
      
      switch (buildMode) {
         case POINTS:
            addPoints(vStart, vEnd);
            break;
         case LINES:
            addLines(vStart,vEnd);
            break;
         case LINE_LOOP:
            addLineLoop(vStart,vEnd);
            break;
         case LINE_STRIP:
            addLineStrip(vStart,vEnd);
            break;
         case TRIANGLES:
            addTriangles(vStart,vEnd);
            break;
         case TRIANGLE_FAN:
            addTriangleFan(vStart,vEnd);
            break;
         case TRIANGLE_STRIP:
            addTriangleStrip(vStart,vEnd);
            break;
         default:
            // nothing
            break;
         
      }
      buildMode = null;
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
      pointsModified = true;
      totalModified = true;
   }
   
   /**
    * Creates a point primitive at the supplied vertex.
    * @param vidx vertex index for point
    */
   public void addPoint(int vidx) {
      if (primitivesCommitted) {
         throw new IllegalStateException(
            "Cannot add a new primitive once primitives are committed");
      }
      // start first point group
      if (stateInfo.numPointGroups == 0) {
         createPointGroup();
      }
      currentPointGroup.add(new int[]{vidx});
      notifyPointsModified();
   }

   /**
    * Creates point primitives at the supplied vertex locations.
    * @param vidxs array of vertex indices at which to create point primitives.
    */
   public void addPoints(int... vidxs) {
      for (int v : vidxs) {
         addPoint(v);
      }
   }

   /**
    * Creates a point primitive at the supplied position.  A vertex is first
    * created using the currently active normal, color and texture coordinate
    * (if available).  
    * @param v array of length 3 (x,y,z)
    */
   public void addPoint(float[] v) {
      int vidx = vertex(v);
      addPoint(vidx);
   }

   /**
    * Creates a set of vertices and point primitives at the supplied positions.
    * @see #addPoint(float[])
    * @param pnts
    */
   public void addPoints(Iterable<float[]> pnts) {
      for (float[] v : pnts) {
         addPoint(v);
      }
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
      return currentPointGroup.size();
   }

   /**
    * Returns a list of vertex indices of all point primitives defined.
    */
   public List<int[]> getPoints() {
      if (hasPoints()) {
         return Collections.unmodifiableList(currentPointGroup);
      }
      return null;
   }

   /**
    * Hint for ensuring sufficient storage for points
    * @param cap capacity
    */
   public void ensurePointCapacity(int cap) {
      if (stateInfo.numPointGroups == 0) {
         createPointGroup();
      }
      currentPointGroup.ensureCapacity(cap);
   }
   
   /**
    * Creates a new group of points that can be rendered.  By default,
    * only one point group exists.
    * @return the index of the new group of points
    * @see #pointGroup(int)
    */
   public int createPointGroup() {
      if (primitivesCommitted) {
         throw new IllegalStateException(
            "Cannot create a new point group once primitives are committed");
      }
      ArrayList<int[]> newPoints = new ArrayList<>();
      points.add(newPoints);
      currentPointGroup = newPoints;
      int idx = stateInfo.numPointGroups;
      stateInfo.pointGroupIdx = idx;
      stateInfo.numPointGroups++;
      notifyPointsModified ();
      return idx;
   }

   /**
    * Sets the current active point group for rendering.
    * @param setIdx index of active point group.
    */
   public void pointGroup(int setIdx) {
      stateInfo.pointGroupIdx = setIdx;
      if (points != null) {
         currentPointGroup = points.get(stateInfo.pointGroupIdx);
      }
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
      return points.get(pgroup).size();
   }

   /**
    * Retrieves the point at the supplied index in a given group.  
    * The returned point should not be modified.
    * @param pgroup point group index
    * @param pidx point index
    * @return vertex index of point
    */
   public int[] getPoint(int pgroup, int pidx) {
      if (pidx < 0) {
         return null;
      }
      return points.get(pgroup).get(pidx);
   }

   /**
    * Returns the list of points (vertex indices) for a given group
    * @param pgroup point group index
    * @return vertex indices for point group
    */
   public List<int[]> getPoints(int pgroup) {
      return Collections.unmodifiableList(points.get(pgroup));
   }

   /**
    * Returns the latest committed points version number,
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
    * Informs the render object that lines have been modified outside of its control.
    */
   public void notifyLinesModified() {
      linesModified = true;
      totalModified = true;
   }

   protected void addLinePair(int[] vidxs) {
      if (primitivesCommitted) {
         throw new IllegalStateException(
            "Cannot add a new primitive once primitives are committed");
      }
      if (stateInfo.numLineGroups == 0)  {
         createLineGroup();
      }
      currentLineGroup.add(vidxs);
      notifyLinesModified ();
   }

   /**
    * Creates a line primitive between the supplied vertices.
    * @param v0idx vertex index for start of line
    * @param v1idx vertex index for end of line
    */
   public void addLine(int v0idx, int v1idx) {
      addLinePair(new int[]{v0idx, v1idx});
   }

   /**
    * Creates a set of line primitives between pairs of vertices as specified
    * by the supplied set of vertex indices.  For example, supplying 
    * {1,2,3,4} will create two line segments: 1-2, 3-4.
    * @param vidxs vertex indices, in pairs, defining line segments.  
    */
   public void addLines(int... vidxs) {
      for (int i=0; i<vidxs.length-1; i+=2) {
         addLinePair(new int[]{vidxs[i], vidxs[i+1]});
      }
   }
   
   /**
    * Creates a set of line primitives between pairs of vertices as specified
    * by the supplied vertex range.  For example, supplying 
    * {1,4} will create two line segments: 1-2, 3-4.
    * @param vStart starting vertex
    * @param vEnd ending vertex  
    */
   public void addLines(int vStart, int vEnd) {
      for (int i=vStart; i<vEnd; i+=2) {
         addLine(i, i+1);
      }
   }

   /**
    * Creates a set of line primitives between the supplied pairs of vertices.
    * @param lines int pairs of vertex indices defining line segments.
    */
   public void addLines(Iterable<int[]> lines) {
      for (int[] line : lines) {
         addLinePair(line);
      }
   }
   
   /**
    * Creates a set of connected line segments between neighboring vertices
    * in the supplied list of vertex indices.  For example, supplying {1,2,3,4}
    * will create three line segments: 1-2, 2-3, 3-4.
    * @param vidxs vertex indices specifying connectivity
    */
   public void addLineStrip(int... vidxs) {
      for (int i=0; i<vidxs.length-1; ++i) {
         addLinePair(new int[]{vidxs[i], vidxs[i+1]});
      }
   }
   
   /**
    * Creates a set of connected line primitives between neighboring vertices 
    * as specified by the supplied vertex range.  For example, supplying 
    * {1,4} will create three line segments: 1-2, 2-3, 3-4.
    * @param vStart starting vertex
    * @param vEnd ending vertex  
    */
   public void addLineStrip(int vStart, int vEnd) {
      for (int i=vStart; i<vEnd; ++i) {
         addLine(i, i+1);
      }
   }

   /**
    * Creates a set of connected line segments between neighboring vertices
    * in the supplied list of vertex indices.  For example, supplying {1,2,3,4}
    * will create four line segments: 1-2, 2-3, 3-4, 4-1.
    * @param vidxs vertex indices specifying connectivity
    */
   public void addLineLoop(int... vidxs) {
      for (int i=0; i<vidxs.length-1; ++i) {
         addLinePair(new int[]{vidxs[i], vidxs[i+1]});
      }
      if (vidxs.length > 1) {
         addLinePair(new int[]{vidxs[vidxs.length-1], vidxs[0]});
      }
   }
   
   /**
    * Creates a set of line primitives between neighboring vertices as specified
    * by the supplied vertex range.  For example, supplying 
    * {1,4} will create four line segments: 1-2, 2-3, 3-4, 4-1
    * @param vStart starting vertex
    * @param vEnd ending vertex  
    */
   public void addLineLoop(int vStart, int vEnd) {
      for (int i=vStart; i<vEnd; ++i) {
         addLine(i, i+1);
      }
      if (vEnd != vStart) {
         addLine(vEnd, vStart);
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
      int v0idx = vertex(v0[0], v0[1], v0[2]);
      int v1idx = vertex(v1[0], v1[1], v1[2]);
      addLine(v0idx, v1idx);
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
      return currentLineGroup.size();
   }

   /**
    * Returns a list of line primitives, identified by vertex index pairs.
    */
   public List<int[]> getLines() {
      if (hasLines()) {
         return Collections.unmodifiableList(currentLineGroup);
      }
      return null;
   }
   
   /**
    * Hint for ensuring sufficient storage for lines
    * @param cap capacity
    */
   public void ensureLineCapacity(int cap) {
      if (stateInfo.numLineGroups == 0) {
         createLineGroup();
      }
      currentLineGroup.ensureCapacity(cap);
   }

   /**
    * Creates a new group of lines that can be rendered.  By default,
    * only one line group exists.
    * @return the index of the new group of lines
    * @see #lineGroup(int)
    */
   public int createLineGroup() {
      if (primitivesCommitted) {
         throw new IllegalStateException(
            "Cannot create a new line group once primitives are committed");
      }
      ArrayList<int[]> newLines = new ArrayList<>();
      lines.add(newLines);
      currentLineGroup = newLines;
      int idx = stateInfo.numLineGroups;
      stateInfo.lineGroupIdx = idx;
      stateInfo.numLineGroups++;
      notifyLinesModified ();
      return idx;
   }

   /**
    * Sets the current active line group for rendering.
    * @param setIdx index of active line group.
    */
   public void lineGroup(int setIdx) {
      stateInfo.lineGroupIdx = setIdx;
      if (lines != null) {
         currentLineGroup = lines.get(stateInfo.lineGroupIdx);
      }
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
      return lines.get(lgroup).size();
   }

   /**
    * Retrieves the line at the supplied index in a given group.  
    * The returned line should not be modified.
    * @param lgroup line group index
    * @param lidx line index
    * @return vertex indices making up line
    */
   public int[] getLine(int lgroup, int lidx) {
      if (lidx < 0) {
         return null;
      }
      return lines.get(lgroup).get(lidx);
   }

   /**
    * Returns the list of lines (vertex indices) for a given group
    * @param lgroup line group index
    * @return vertex indices for line group
    */
   public List<int[]> getLines(int lgroup) {
      return Collections.unmodifiableList(lines.get(lgroup));
   }

   /**
    * Returns the latest committed lines version number,
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
      trianglesModified = true;
      totalModified = true;
   }
   
   protected void addTriangleTriple(int[] vidxs) {
      if (primitivesCommitted) {
         throw new IllegalStateException(
            "Cannot create a new primitive once primitives are committed");
      }
      if (stateInfo.numTriangleGroups == 0) {
         createTriangleGroup();
      }
      currentTriangleGroup.add(vidxs);
      notifyTrianglesModified ();
   }

   /**
    * Creates a triangle primitive between supplied vertices (CCW order).
    * @param v0idx first vertex
    * @param v1idx second vertex
    * @param v2idx third vertex
    */
   public void addTriangle(int v0idx, int v1idx, int v2idx) {
      addTriangleTriple(new int[] {v0idx, v1idx, v2idx});
   }

   /**
    * Creates a set of triangle primitives between triples of vertices as 
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create two triangles: 1-2-3, 4-5-6.
    * @param vidxs vertex indices, in triples, defining triangles.
    */
   public void addTriangles(int... vidxs) {
      for (int i=0; i<vidxs.length-2; i+=3) {
         addTriangleTriple(new int[]{vidxs[i], vidxs[i+1], vidxs[i+2]});
      }
   }
   
   /**
    * Creates a set of triangle primitives between triples of vertices as 
    * supplied by the given vertex range.  For example supplying {1, 6} will
    * create two triangles: 1-2-3, 4-5-6.
    * @param vStart starting vertex index
    * @param vEnd ending vertex index
    */
   public void addTriangles(int vStart, int vEnd) {
      for (int i=vStart; i<vEnd-1; i+=3) {
         addTriangleTriple(new int[]{i, i+1, i+2});
      }
   }

   /**
    * Creates a set of triangle primitives forming a fan using the
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create four triangles: 1-2-3, 1-3-4, 1-4-5, 1-5-6.
    * @param vidxs vertex indices defining triangles.
    */
   public void addTriangleFan(int... vidxs) {
      for (int i=2; i<vidxs.length; ++i) {
         addTriangleTriple(new int[]{vidxs[0], vidxs[i-1], vidxs[i]});
      }
   }
   
   /**
    * Creates a set of triangle primitives forming a fan using the
    * supplied vertex range.  For example supplying {1, 6} will
    * create four triangles: 1-2-3, 1-3-4, 1-4-5, 1-5-6.
    * @param vStart starting vertex index
    * @param vEnd ending vertex index
    */
   public void addTriangleFan(int vStart, int vEnd) {
      for (int i=vStart+2; i<=vEnd; ++i) {
         addTriangleTriple(new int[]{vStart, i-1, i});
      }
   }

   /**
    * Creates a set of triangle primitives forming a strip using the
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create four triangles: 1-2-3, 3-2-4, 3-4-5, 5-4-6
    * @param vidxs vertex indices defining triangles.
    */
   public void addTriangleStrip(int... vidxs) {

      // add pairs of triangles
      for (int i=2; i<vidxs.length-1; i+=2) {
         addTriangleTriple(new int[]{vidxs[i-2], vidxs[i-1], vidxs[i]});
         addTriangleTriple(new int[]{vidxs[i], vidxs[i-1], vidxs[i+1]});
      }
      // add last triangle
      int i = vidxs.length-1;
      if (i > 2 && i % 2 == 0) {
         addTriangleTriple(new int[]{vidxs[i-2], vidxs[i-1], vidxs[i]});
      }
   }
   
   /**
    * Creates a set of triangle primitives forming a strip using the
    * supplied by vertex range.  For example supplying {1,6} will
    * create four triangles: 1-2-3, 3-2-4, 3-4-5, 5-4-6
    * @param vStart starting vertex index
    * @param vEnd ending vertex index
    */
   public void addTriangleStrip(int vStart, int vEnd) {
      
      // add pairs of triangles
      for (int i=vStart+2; i<vEnd; i+=2) {
         addTriangleTriple(new int[]{i-2, i-1, i});
         addTriangleTriple(new int[]{i, i-1, i+1});
      }
      // add last triangle
      int i = vEnd-vStart;
      if (i > 2 && i % 2 == 0) {
         addTriangleTriple(new int[]{vEnd-2, vEnd-1, vEnd});
      }
   }

   /**
    * Creates a set of triangle primitives between supplied triples of 
    * vertices.
    * @param tris int triples of vertex indices defining triangles (CCW)
    */
   public void addTriangles(Iterable<int[]> tris) {
      for (int[] tri : tris) {
         addTriangleTriple(tri);
      }
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
      int v0idx = vertex(v0[0], v0[1], v0[2]);
      int v1idx = vertex(v1[0], v1[1], v1[2]);
      int v2idx = vertex(v2[0], v2[1], v2[2]);
      addTriangle(v0idx, v1idx, v2idx);
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
      return currentTriangleGroup.size();
   }

   /**
    * Returns a list of triangle primitives, identified by vertex index triples.
    */
   public List<int[]> getTriangles() {
      if (hasTriangles()) {
         return Collections.unmodifiableList(currentTriangleGroup);
      } 
      return null;
   }

   /**
    * Hint for ensuring sufficient storage for triangles
    * @param cap capacity
    */
   public void ensureTriangleCapacity(int cap) {
      if (stateInfo.numTriangleGroups == 0) {
         createTriangleGroup();
      }
      currentTriangleGroup.ensureCapacity(cap);
   }
   
   /**
    * Creates a new group of triangles that can be rendered.  By default,
    * only one triangle group exists.
    * @return the index of the new group of triangles
    * @see #triangleGroup(int)
    */
   public int createTriangleGroup() {
      if (primitivesCommitted) {
         throw new IllegalStateException(
            "Cannot create a new triangle group once primitives are committed");
      }
      ArrayList<int[]> newTriangles = new ArrayList<>();
      triangles.add(newTriangles);
      currentTriangleGroup = newTriangles;
      int idx = stateInfo.numTriangleGroups;
      stateInfo.triangleGroupIdx = idx;
      stateInfo.numTriangleGroups++;
      notifyTrianglesModified ();
      return idx;
   }

   /**
    * Sets the current active triangle group for rendering.
    * @param setIdx index of active triangle group.
    */
   public void triangleGroup(int setIdx) {
      stateInfo.triangleGroupIdx = setIdx;
      if (triangles != null) {
         currentTriangleGroup = triangles.get(stateInfo.triangleGroupIdx);
      }
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
      return triangles.get(tgroup).size();
   }

   /**
    * Retrieves the triangle at the supplied index in a given group.  
    * The returned triangle should not be modified.
    * @param tgroup triangle group index
    * @param tidx triangle index
    * @return vertex indices of triangle
    */
   public int[] getTriangle(int tgroup, int tidx) {
      if (tidx < 0) {
         return null;
      }
      return triangles.get(tgroup).get(tidx);
   }

   /**
    * Returns the list of triangles (vertex indices) for a given group
    * @param tgroup point group index
    * @return vertex indices for triangle group
    */
   public List<int[]> getTriangles(int tgroup) {
      return Collections.unmodifiableList(triangles.get(tgroup));
   }

   /**
    * Returns the latest committed triangles version number,
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
    * Prevent modification of primitives until all primitives have been 
    * cleared.
    */
   private void commitPrimitives() {
      points.trimToSize();
      lines.trimToSize();
      triangles.trimToSize();
      for (ArrayList<int[]> pnts : points) {
         pnts.trimToSize();
      }
      for (ArrayList<int[]> lns : lines) {
         lns.trimToSize();
      }
      for (ArrayList<int[]> tris : triangles) {
         tris.trimToSize();
      }
      primitivesCommitted = true;
   }

   /**
    * Clears all primitive groups, allowing them to be recreated.  Vertex
    * information remains untouched.
    */
   public void clearPrimitives() {

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

      primitivesCommitted = false;


   }

   /**
    * Clears everything in the RenderObject, allowing it to be
    * recreated.  This can be used when the object becomes invalid,
    * in place of discarding and regenerating a new one.  The object
    * becomes a clean slate, with no vertex attributes or primitives.
    */
   public void reinitialize() {
      // in most cases, we will only have one set
      positions = new ArrayList<>(1);
      normals = new ArrayList<>(1);
      colors = new ArrayList<>(1);
      textures = new ArrayList<>(1);

      stateInfo.positionSetIdx = -1;
      stateInfo.normalSetIdx = -1;
      stateInfo.colorSetIdx = -1;
      stateInfo.textureSetIdx = -1;
      stateInfo.numPositionSets = 0;
      stateInfo.numNormalSets = 0;
      stateInfo.numColorSets = 0;
      stateInfo.numTextureSets = 0;

      currentPositionIdx = -1;
      currentNormalIdx = -1;
      currentColorIdx = -1;
      currentTextureIdx = -1;

      numPositions = 0;
      numNormals = 0;
      numColors = 0;
      numTextures = 0;

      positionsDynamic = false;
      normalsDynamic = false;
      colorsDynamic = false;
      texturesDynamic = false;
      isTransient = false;

      currentPositionSet = null;
      currentNormalSet = null;
      currentColorSet = null;
      currentTextureSet = null;

      positionsModified = true;
      normalsModified = true;
      colorsModified = true;
      texturesModified = true;
      totalModified = true;

      vertices = new ArrayList<>();
      numVertices = 0;
      verticesModified = true;

      verticesCommitted = false;

      clearPrimitives();
      
      buildMode = null;
      buildModeStart = -1;
      
   }

   //=========================================================================
   // Usage flags
   //=========================================================================

   /**
    * Signal that the object is complete and ready for rendering.
    * No more data can be added to the renderable until it is cleared.  Only 
    * dynamic components can be modified.  This function should be called
    * before first use (either manually, or by a renderer)
    */
   public void commit() {
      if (!verticesCommitted) {
         commitVertices();
      }
      if (!primitivesCommitted) {
         commitPrimitives();
      }
   }

   /**
    * Indicates whether or not the object is complete and ready
    * for rendering.
    */
   public boolean isCommitted() {
      return verticesCommitted && primitivesCommitted;
   }

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
    * Invalidates the object.  An invalid object cannot be drawn by the renderer.
    * @see #isValid()
    */
   public void invalidate() {
      idInfo.setValid(false);
      
      // clear all memory
      positions = null;
      normals = null;
      colors = null;
      textures = null;

      currentPositionSet = null;
      currentNormalSet = null;
      currentColorSet = null;
      currentTextureSet = null;

      points = null;
      lines = null;
      triangles = null;

      currentPointGroup = null;
      currentLineGroup = null;
      currentTriangleGroup = null;
   }

   /**
    * Returns whether or not this RenderObject is valid.  If valid,
    * this object can be safely passed to a renderer for drawing.  
    * If not, it needs to be discarded.
    * @return <code>true</code> if this RenderObject is valid
    */
   public boolean isValid() {
      return idInfo.isValid();
   }

   /**
    * Signal a destruction of the object.
    */
   public void dispose() {
      invalidate();
   }

   /**
    * A transient object has a short life-span, and cannot be modified once
    * committed.
    */
   public void setTransient(boolean set) {
      if (set != isTransient) {
         if (verticesCommitted || primitivesCommitted) {
            throw new IllegalStateException(
               "Cannot modify transient property once vertices are committed.");
         }
         else if (set && isDynamic()) {
            throw new IllegalStateException (
               "Cannot make a dynamic object transient");
         }
         isTransient = set;
      }
   }

   /**
    * A transient object has a short life-span, and cannot be 
    * modified once committed.
    */
   public boolean isTransient() {
      return isTransient;
   }

   /**
    * Garbage collection, clear memory and dispose
    */
   @Override
   protected void finalize() throws Throwable {
      dispose();
   }

   @Override
   protected RenderObject clone()  {

      RenderObject r = new RenderObject();

      if (positions != null) {
         r.positions = new ArrayList<>(positions.size());
         for (ArrayList<float[]> poss : positions) {
            ArrayList<float[]> nposs = new ArrayList<float[]>(poss.size());
            for (float[] pos : poss) {
               nposs.add(Arrays.copyOf(pos, pos.length));
            }
            r.positions.add(nposs);
         }
      } else {
         r.positions = null;
      }

      if (normals != null) {
         r.normals = new ArrayList<>(normals.size());
         for (ArrayList<float[]> nrms : normals) {
            ArrayList<float[]> npos = new ArrayList<float[]>(nrms.size());
            for (float[] nrm : nrms) {
               npos.add(Arrays.copyOf(nrm, nrm.length));
            }
            r.normals.add(npos);
         }
      } else {
         r.normals = null;
      }

      if (colors != null) {
         r.colors = new ArrayList<>(colors.size());
         for (ArrayList<byte[]> cols : colors) {
            ArrayList<byte[]> ncols = new ArrayList<byte[]>(cols.size());
            for (byte[] col : cols) {
               ncols.add(Arrays.copyOf(col, col.length));
            }
            r.colors.add(ncols);
         }
      } else {
         r.colors = null;
      }

      if (textures != null) {
         r.textures = new ArrayList<>(textures.size());
         for (ArrayList<float[]> texs : textures) {
            ArrayList<float[]> ntexs = new ArrayList<float[]>(texs.size());
            for (float[] tex : texs) {
               ntexs.add(Arrays.copyOf(tex, tex.length));
            }
            r.textures.add(ntexs);
         }
      } else {
         r.textures = null;
      }

      r.stateInfo = stateInfo.clone();

      r.currentPositionIdx = currentPositionIdx;
      r.currentNormalIdx = currentNormalIdx;
      r.currentColorIdx = currentColorIdx;
      r.currentTextureIdx = currentTextureIdx;

      // keep track separately to allow storage to be cleared
      r.numPositions = numPositions;
      r.numNormals = numNormals;
      r.numColors = numColors;
      r.numTextures = numTextures;

      // whether or not attributes can be updated once the object is
      // Committed
      r.positionsDynamic = positionsDynamic;
      r.normalsDynamic = normalsDynamic;
      r.colorsDynamic = colorsDynamic;
      r.texturesDynamic = texturesDynamic;

      if (stateInfo.positionSetIdx >= 0) {
         r.positionSet(stateInfo.positionSetIdx);
      }
      if (stateInfo.normalSetIdx >= 0) {
         r.normalSet(stateInfo.normalSetIdx);
      }
      if (stateInfo.colorSetIdx >= 0) {
         r.colorSet(stateInfo.colorSetIdx);
      }
      if (stateInfo.textureSetIdx >= 0) {
         r.textureCoordSet(stateInfo.textureSetIdx);
      }

      // indicators that attributes have been modified
      r.positionsModified = positionsModified;
      r.normalsModified = normalsModified;
      r.colorsModified = colorsModified;
      r.texturesModified = texturesModified;

      r.versionInfo = versionInfo.clone();

      r.vertices = new ArrayList<>(vertices.size());
      for (VertexIndexSet v : vertices) {
         r.vertices.add(v.clone());
      }
      r.numVertices = numVertices;
      r.verticesModified = verticesModified;

      if (points != null) {
         r.points = new ArrayList<>(points.size());
         for (ArrayList<int[]> pnts : points) {
            ArrayList<int[]> npnts = new ArrayList<>(pnts.size());
            for (int[] pnt : pnts) {
               npnts.add(Arrays.copyOf(pnt, pnt.length));
            }
            r.points.add(npnts);
         }
      } else {
         r.points = null;
      }

      if (lines != null) {
         r.lines = new ArrayList<>(lines.size());
         for (ArrayList<int[]> lns : lines) {
            ArrayList<int[]> nlns = new ArrayList<>(lns.size());
            for (int[] ln : lns) {
               nlns.add(Arrays.copyOf(ln, ln.length));
            }
            r.lines.add(nlns);
         }
      } else {
         r.lines = null;
      }

      if (triangles != null) {
         r.triangles = new ArrayList<>(triangles.size());
         for (ArrayList<int[]> tris : triangles) {
            ArrayList<int[]> ntris = new ArrayList<>(tris.size());
            for (int[] tri : tris) {
               ntris.add(Arrays.copyOf(tri, tri.length));
            }
            r.triangles.add(ntris);
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

      r.verticesCommitted = verticesCommitted;
      r.primitivesCommitted = primitivesCommitted;
      
      r.idInfo.setValid(idInfo.isValid());

      r.totalModified = totalModified;

      r.isTransient = isTransient;
      
      r.buildMode = buildMode;
      r.buildModeStart = buildModeStart;

      return r;
   }

   // XXX I don't think we can ever actually clear the internal storage and have
   // the object remain valid.  For GL2 renderers, whenever a dynamic attribute
   // is updated, we need to reconstruct all display lists completely.  For
   // GL3 renderers, if ever a new renderer is added, it needs access to the complete
   // set of data as well.  If renderers start making their own copies of the data,
   // then we've lost all the advantage of clearing storage in the first place.  We
   // may as well keep a valid copy of internal storage here always.
   
   //   /**
   //    * Clears internal storage.  Once called, it is assumed all data
   //    * has been properly consumed by all renderers.  Clearing the storage 
   //    * DOES NOT invalidate the object.  It is assumed the renderers still 
   //    * contain the information required for drawing.
   //    */
   //   private void freeStorage() {
   //      // clear all static memory
   //      if (!positionsDynamic) {
   //         positions = null;
   //         currentPositionSet = null;
   //      }
   //      if (!normalsDynamic) {
   //         normals = null;   
   //         currentNormalSet = null;
   //      }
   //
   //      if (!colorsDynamic) {
   //         colors = null;
   //         currentColorSet = null;
   //      }
   //
   //      if (!texturesDynamic) {
   //         textures = null;
   //         currentTextureSet = null;
   //      }
   //
   //      if (!positionsDynamic && !normalsDynamic && !colorsDynamic 
   //      && !texturesDynamic) {
   //         vertices = null;
   //      }
   //
   //      points = null;
   //      lines = null;
   //      triangles = null;
   //
   //      currentPointGroup = null;
   //      currentLineGroup = null;
   //      currentTriangleGroup = null;
   //
   //      storageValid = false;
   //   }
   //
   //   /**
   //    * Returns whether or not the object still contains a valid copy of all
   //    * data defined.
   //    */
   //   private boolean isStorageValid() {
   //      return storageValid;
   //   }
   //
   //   /**
   //    * Checks whether primitives are still stored in the object
   //    */
   //   private boolean isPrimitiveStorageValid() {
   //      if ((numPointGroups + numLineGroups + numTriangleGroups) == 0) {
   //         return true;
   //      }
   //      if (points == null || lines == null || triangles == null) {
   //         return false;
   //      }
   //      return true;
   //   }
   
}
