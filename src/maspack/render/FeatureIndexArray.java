package maspack.render;

import maspack.util.DynamicIntArray;

/**
 * Container for keep tracking of feature indices, vertices, and offsets
 * @author Antonio
 */
public class FeatureIndexArray {

   VertexIndexArray vertices;
   DynamicIntArray features;
   DynamicIntArray offsets;
   
   boolean building;
   
   public FeatureIndexArray() {
      this(DynamicIntArray.DEFAULT_INITIAL_CAPACITY, 
         DynamicIntArray.DEFAULT_INITIAL_CAPACITY*10);
   }
   
   public FeatureIndexArray(int featureCap, int vertexCap) {
      vertices = new VertexIndexArray (vertexCap);
      features = new DynamicIntArray (featureCap);
      offsets = new DynamicIntArray (featureCap+1);
      offsets.add (0); // first offset
      building = false;
   }
   
   protected FeatureIndexArray(VertexIndexArray v, DynamicIntArray f, DynamicIntArray o) {
      this.vertices = v;
      this.features = f;
      this.offsets = o;
      building = false;
   }
   
   /**
    * Starts building a feature with the provided feature number
    * (such as face index or line index)
    * @param number tracked feature number
    */
   public void beginFeature(int number) {
      if (building) {
         throw new IllegalStateException ("Current feature must first be ended");
      }
      features.add (number);
      building = true;
   }
   
   /**
    * Adds a vertex index to the current feature (see {@link #beginFeature(int)}
    * @param vidx vertex index
    */
   public void addVertex(int vidx) {
      if (!building) {
         throw new IllegalStateException ("Not currently building a feature");
      }
      vertices.add (vidx);
   }
   
   /**
    * Ends building a feature
    * @see FeatureIndexArray#beginFeature(int)
    */
   public void endFeature() {
      offsets.add (vertices.size ());
      building = false;
   }
   
   /**
    * @return number of features
    */
   public int numFeatures() {
      return features.size ();
   }
   
   /**
    * Returns a subset of features
    * @param fidx starting feature index
    * @param nfeatures number of features
    */
   public FeatureIndexArray slice(int fidx, int nfeatures) {
      
      DynamicIntArray f = features.slice (fidx, nfeatures);
      DynamicIntArray o = offsets.slice (fidx, nfeatures+1);
      int vstart = getFeatureOffset (fidx);
      int vend = getFeatureOffset (fidx+nfeatures);
      VertexIndexArray v = vertices.slice (vstart, vend-vstart);
      
      return new FeatureIndexArray(v, f, o);
   }
   
   /**
    * Performs an in-place {@link #slice(int, int)}, modifying this
    * feature array.
    * @param fidx starting feature index
    * @param nfeatures number of features
    */
   public void chop(int fidx, int nfeatures) {
      features.chop (fidx, nfeatures);
      offsets.chop (fidx, nfeatures+1);
      int vstart = getFeatureOffset (fidx);
      int vend = getFeatureOffset (fidx+nfeatures);
      vertices.chop (vstart, vend-vstart);
   }
   
   /**
    * Returns the offset into the vertex index array for the start
    * of the ith feature
    * @param idx index of feature
    * @return offset
    */
   public int getFeatureOffset(int idx) {
      return offsets.get (idx);
   }
   
   /**
    * Returns the feature number at the provided index
    * @param idx index of feature
    * @return feature number
    */
   public int getFeature(int idx) {
      return features.get (idx);
   }
   
   /**
    * The number of vertex indices that define a given feature
    * @param idx feature index
    * @return number of vertices
    */
   public int getFeatureLength(int idx) {
      return offsets.get (idx+1)-offsets.get (idx);
   }
   
   /**
    * Vertices associated with each feature are concatenated into a single
    * array (see {@link #getVertices()}).  This provides the offsets into
    * that array.  The number of vertices can be obtained by taking the difference
    * <code>offset.get(i+1)-offset.get(i)</code>.  The last element in the
    * offset array is the total number of vertices.
    * @return offsets in the vertex array for each feature
    */
   public DynamicIntArray getVertexOffsets() {
      return offsets;
   }
   
   /**
    * Concatenated list of vertex indices for all contained features.
    * @return index array of vertices
    */
   public VertexIndexArray getVertices() {
      return vertices;
   }
   
}
