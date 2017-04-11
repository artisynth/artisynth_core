package maspack.render;

import java.util.List;

import maspack.render.Renderer.DrawMode;

public interface RenderObjectInterface {

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
   public static interface RenderObjectIdentifier {
      
      /**
       * Unique identifying number
       */
      public int getId();
      
      /**
       * Whether the associated RenderObject is still valid.
       * A RenderObject becomes invalid if it runs out of
       * scope.
       */
      public boolean isValid();
   }
   
   /**
    * Keeps track of versions for detecting changes.  Can be
    * cloned so renderers can keep track of the latest versions
    * they have observed.
    */
   public static interface RenderObjectVersion {
     
      public int getPositionsVersion();
      public int getNormalsVersion();
      public int getColorsVersion();
      public int getTextureCoordsVersion();
      public int getVerticesVersion();
      
      public int getPointsVersion();
      public int getLinesVersion();
      public int getTrianglesVersion();
      
      public int getVersion();
      
      public RenderObjectVersion clone();
   }
   
   /**
    * Stores exposable state of the object, tracking the 
    * current attribute set indices and primitive group indices.
    * Can be cloned so renderers can keep track of the latest state
    * they have observed.
    * 
    */
   public static interface RenderObjectState {
     
      public boolean hasPositions();
      public boolean hasNormals();
      public boolean hasColors();
      public boolean hasTextureCoords();
      
      public int numPositionSets();
      public int numNormalSets();
      public int numColorSets();
      public int numTextureCoordSets();

      public int getPositionSetIdx();
      public int getNormalSetIdx();
      public int getColorSetIdx();
      public int getTextureCoordSetIdx();

      public int numPointGroups();
      public int numLineGroups();
      public int numTriangleGroups();
      
      public int getPointGroupIdx();
      public int getLineGroupIdx();
      public int getTriangleGroupIdx();

      public RenderObjectState clone();
      
   }
   
   /**
    * Class for storing position/normal/color/texture indices for a vertex
    */
   public static interface VertexIndexSet {

      public int getPositionIndex();
      public int getNormalIndex();
      public int getColorIndex();
      public int getTextureCoordIndex();

   }
   
   /**
    * Returns a special object to be used as a unique identifier for this
    * RenderObject.  It contains a unique ID number, as well as a flag
    * for determining whether the object still persists and is valid.
    * This should be as the key in HashMaps etc... so that the original
    * RenderObject can be cleared and garbage-collected when it runs out 
    * of scope. 
    */
   public abstract RenderObjectIdentifier getIdentifier();

   /**
    * Returns an immutable copy of all version information in this RenderObject,
    * safe for sharing between threads.  This can be used to detect whether
    * the RenderObject has been modified since last observed.
    */
   public abstract RenderObjectVersion getVersionInfo();

   public abstract RenderObjectState getStateInfo();

   //=========================================================================
   // Positions, Normals, Colors, Textures
   //=========================================================================
   /**
    * Hint for ensuring sufficient storage for positions
    * @param cap capacity
    */
   public abstract void ensurePositionCapacity(int cap);

   /**
    * Adds an indexable 3D position
    * @param px x coordinate
    * @param py y coordinate
    * @param pz z coordinate
    * @return the index of the position added
    */
   public abstract int addPosition(float px, float py, float pz);

   /**
    * Sets the current 3D position to be used in following vertices.
    * @param px x coordinate
    * @param py y coordinate
    * @param pz z coordinate
    * @return The index of the new position (valid only if a vertex
    * is added with the supplied position)
    */
   public abstract int position(float px, float py, float pz);

   /**
    * Sets the current position to be used in following vertices, 
    * based on position index. 
    * @param pidx index of a previously added position
    */
   public abstract void position(int pidx);

   /**
    * Updates the values of the position with index pidx.
    * @param pidx position to modify
    * @param px x coordinate
    * @param py y coordinate
    * @param pz z coordinate
    */
   public abstract void setPosition(int pidx, float px, float py, float pz);

   /**
    * Whether or not any positions have been defined.
    */
   public abstract boolean hasPositions();

   /**
    * Number of positions defined.
    */
   public abstract int numPositions();

   /**
    * Retrieves the position at the supplied index.  The returned position
    * should not be modified.
    * @param pidx position index
    * @return position {x,y,z}
    */
   public abstract float[] getPosition(int pidx);

   /**
    * Retrieves the full list of positions.  This list should not
    * be modified.
    * @return list of positions.
    */
   public abstract List<float[]> getPositions();

   /**
    * Sets whether or not positions should be considered dynamic.  If true,
    * positions can be updated.  Otherwise, positions are 
    * considered fixed for all time.  The dynamic property can only be 
    * modified before vertices are Committed.
    * @see #commit()
    */
   public abstract void setPositionsDynamic(boolean set);

   /**
    * Returns whether or not positions are considered dynamic.
    */
   public abstract boolean isPositionsDynamic();

   /**
    * Creates a new, alternative set of positions that can be used by vertices.  
    * This allows sharing of other resources, such as normals, colors and 
    * textures.  The new set of positions immediately becomes active, and
    * is initialized from the previously active position set.
    * @return the index of the newly created position set
    * @see #positionSet(int)
    */
   public abstract int createPositionSet();

   /**
    * Creates a new, alternative set of positions that can be used by vertices.  
    * This allows sharing of other resources, such as normals, colors and 
    * textures.  The new set of positions immediately becomes active, and 
    * is initialized from the position set identified by the supplied index.
    * @param copyIdx index of position set to duplicate
    * @return the index of the newly created position set
    * @see #positionSet(int)
    */
   public abstract int createPositionSetFrom(int copyIdx);

   /**
    * Sets the current active position set to be used/modified.  Vertices
    * will use the current active set of positions when rendering.
    * @param setIdx index of active position set
    */
   public abstract void positionSet(int setIdx);

   /**
    * Returns the index of the currently active position set.
    */
   public abstract int getPositionSetIdx();

   /**
    * The number of position sets available.
    */
   public abstract int numPositionSets();

   /**
    * Retrieves the position at the supplied index in a given set.  
    * The returned position should not be modified.
    * @param pset position set index
    * @param pidx position index
    * @return position {x,y,z}
    */
   public abstract float[] getPosition(int pset, int pidx);

   /**
    * Retrieves the full list of positions in a given set.  This list should 
    * not be modified.
    * @param pset position set index
    * @return list of positions.
    */
   public abstract List<float[]> getPositions(int pset);

   /**
    * Returns the latest committed positions version number,
    * for use in detecting if changes are present.
    */
   public abstract int getPositionsVersion();

   /**
    * Hint for ensuring sufficient storage for normals
    * @param cap capacity
    */
   public abstract void ensureNormalCapacity(int cap);

   /**
    * Adds an indexable 3D normal.
    * @param nx x component
    * @param ny y component
    * @param nz z component
    * @return the index of the normal added
    */
   public abstract int addNormal(float nx, float ny, float nz);

   /**
    * Sets the current 3D normal to be used in following
    * vertices.
    * @param nx x component
    * @param ny y component
    * @param nz z component
    * @return The index of the new normal (valid only if a vertex
    * is added with the supplied normal)
    */
   public abstract int normal(float nx, float ny, float nz);

   /**
    * Sets the current normal to be used in following vertices, 
    * based on normal index. 
    * @param nidx index of a previously added normal
    */
   public abstract void normal(int nidx);

   /**
    * Updates the values of the normal with index nidx.
    * @param nidx normal to modify
    * @param nx x component
    * @param ny y component
    * @param nz z component
    */
   public abstract void setNormal(int nidx, float nx, float ny, float nz);

   /**
    * Whether or not any normals have been defined.
    */
   public abstract boolean hasNormals();

   /**
    * Number of normals defined.
    */
   public abstract int numNormals();

   /**
    * Retrieves the normal at the supplied index.  The returned normal
    * should not be modified.
    * @param nidx normal index
    * @return normal {x,y,z}
    */
   public abstract float[] getNormal(int nidx);

   /**
    * Retrieves the full list of normals.  This list should not
    * be modified.
    * @return list of normals.
    */
   public abstract List<float[]> getNormals();

   /**
    * Sets whether or not normals should be considered dynamic.  If true,
    * normals can be updated.  Otherwise, normals are considered fixed for 
    * all time.  The dynamic property can only be modified 
    * before vertices are Committed.
    * @see #commit()
    */
   public abstract void setNormalsDynamic(boolean set);

   /**
    * Returns whether or not normals are considered dynamic.
    */
   public abstract boolean isNormalsDynamic();

   /**
    * Creates a new, alternative set of normals that can be used by vertices.  
    * This allows sharing of other resources, such as positions, colors and 
    * textures.  The new set of normals immediately becomes active, and is
    * initialized from the previously active normal set.
    * @return the index of the newly created normal set
    * @see #normalSet(int)
    */
   public abstract int createNormalSet();

   /**
    * Creates a new, alternative set of normals that can be used by vertices.  
    * This allows sharing of other resources, such as positions, colors and 
    * textures.  The new set of normals will be initialized from the
    * normal set identified by the supplied index.
    * @param copyIdx index of normal set to duplicate
    * @return the index of the newly created normal set
    * @see #normalSet(int)
    */
   public abstract int createNormalSetFrom(int copyIdx);

   /**
    * Sets the current active normal set to be used/modified.  Vertices
    * will use the current active set of normals when rendering.
    * @param setIdx index of active normal set
    */
   public abstract void normalSet(int setIdx);

   /**
    * Returns the index of the currently active normal set.
    */
   public abstract int getNormalSetIdx();

   /**
    * The number of normal sets available.
    */
   public abstract int numNormalSets();

   /**
    * Retrieves the normal at the supplied index in a given set.  
    * The returned normal should not be modified.
    * @param nset normal set index
    * @param nidx normal index
    * @return normal {x,y,z}
    */
   public abstract float[] getNormal(int nset, int nidx);

   /**
    * Retrieves the full list of normals in a given set.  This list should 
    * not be modified.
    * @param nset normal set index
    * @return list of normals.
    */
   public abstract List<float[]> getNormals(int nset);

   /**
    * Returns the latest committed triangles version number,
    * for use in detecting if changes are present.
    */
   public abstract int getNormalsVersion();

   /**
    * Hint for ensuring sufficient storage for colors
    * @param cap capacity
    */
   public abstract void ensureColorCapacity(int cap);

   /**
    * Adds an indexable color
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    * @return the index of the color added
    */
   public abstract int addColor(byte r, byte g, byte b, byte a);

   /**
    * Adds an indexable color
    * @param r red [0-255]
    * @param g green [0-255]
    * @param b blue [0-255]
    * @param a alpha [0-255]
    * @return the index of the color added
    */
   public abstract int addColor(int r, int g, int b, int a);

   /**
    * Adds an indexable color
    * @param r red [0-1]
    * @param g green [0-1]
    * @param b blue  [0-1]
    * @param a alpha  [0-1]
    * @return the index of the color added
    */
   public abstract int addColor(float r, float g, float b, float a);

   /**
    * Adds an indexable color
    * @param rgba {red, green, blue, alpha}
    * @return the index of the color added
    */
   public abstract int addColor(byte[] rgba);

   /**
    * Sets the current color to be used in following vertices.
    * @param r red [0-255]
    * @param g green [0-255]
    * @param b blue [0-255]
    * @param a alpha [0-255]
    * @return The index of the new color (valid only if a vertex
    * is added with the supplied color)
    */
   public abstract int color(int r, int g, int b, int a);

   /**
    * Sets the current color to be used in following vertices.
    * @param r red [0-1]
    * @param g green [0-1]
    * @param b blue [0-1]
    * @param a alpha [0-1]
    * @return The index of the new color (valid only if a vertex
    * is added with the supplied color)
    */
   public abstract int color(float r, float g, float b, float a);

   /**
    * Sets the current color to be used in following vertices.
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    * @return The index of the new color (valid only if a vertex
    * is added with the supplied color)
    */
   public abstract int color(byte r, byte g, byte b, byte a);

   /**
    * Sets the current color to be used in following vertices.
    * @param rgba {red, green, blue, alpha}
    * @return The index of the new color (valid only if a vertex
    * is added with the supplied color)
    */
   public abstract int color(byte[] rgba);

   /**
    * Sets the current color to be used in following vertices
    * based on color index.
    * @param cidx index of a previously added color
    */
   public abstract void color(int cidx);

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public abstract void setColor(int cidx, byte r, byte g, byte b, byte a);

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public abstract void setColor(int cidx, int r, int g, int b, int a);

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public abstract void setColor(int cidx, float r, float g, float b, float a);

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param rgba {red, green, blue, alpha}
    */
   public abstract void setColor(int cidx, byte[] rgba);

   /**
    * Whether or not any colors have been defined.
    */
   public abstract boolean hasColors();

   /**
    * Number of positions defined
    */
   public abstract int numColors();

   /**
    * Retrieves the color at the supplied index.  The returned color
    * should not be modified.
    * @param cidx color index
    * @return color {red, green, blue, alpha}
    */
   public abstract byte[] getColor(int cidx);

   /**
    * Retrieves the full list of Colors.  This list should not
    * be modified.
    * @return list of colors.
    */
   public abstract List<byte[]> getColors();

   /**
    * Sets whether or not colors should be considered dynamic.  If true,
    * colors can be updated.  Otherwise, colors are considered fixed for 
    * all time.  The dynamic property can only be modified before vertices 
    * are Committed.
    * @see #commit()
    */
   public abstract void setColorsDynamic(boolean set);

   /**
    * Returns whether or not colors are considered dynamic.
    */
   public abstract boolean isColorsDynamic();

   /**
    * Creates a new, alternative set of colors that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals, 
    * and textures.  The new set of colors immediately becomes active, and 
    * is initialized from the previously active color set.
    * @return the index of the newly created color set
    * @see #colorSet(int)
    */
   public abstract int createColorSet();

   /**
    * Creates a new, alternative set of colors that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals and 
    * textures.  The new set of colors immediately becomes active, and 
    * is initialized from the color set identified by the supplied index.
    * @param copyIdx index of color set to duplicate
    * @return the index of the newly created color set
    * @see #colorSet(int)
    */
   public abstract int createColorSetFrom(int copyIdx);

   /**
    * Sets the current active color set to be used/modified.  Vertices
    * will use the current active set of colors when rendering.
    * @param setIdx index of active color set
    */
   public abstract void colorSet(int setIdx);

   /**
    * Returns the index of the currently active color set.
    */
   public abstract int getColorSetIdx();

   /**
    * The number of color sets available.
    */
   public abstract int numColorSets();

   /**
    * Retrieves the color at the supplied index in a given set.  
    * The returned color should not be modified.
    * @param cset color set index
    * @param cidx color index
    * @return color {r,g,b,a}
    */
   public abstract byte[] getColor(int cset, int cidx);

   /**
    * Retrieves the full list of colors in a given set.  This list should 
    * not be modified.
    * @param cset color set index
    * @return list of colors.
    */
   public abstract List<byte[]> getColors(int cset);

   /**
    * Returns the latest committed colors version number,
    * for use in detecting if changes are present.
    */
   public abstract int getColorsVersion();

   /**
    * Hint for ensuring sufficient storage for texture coordinates
    * @param cap capacity
    */
   public abstract void ensureTextureCoordCapacity(int cap);

   /**
    * Adds an indexable 2D texture coordinate
    * @param x x coordinate
    * @param y y coordinate
    * @return the index of the texture coordinate added
    */
   public abstract int addTextureCoord(float x, float y);

   /**
    * Sets the current 2D texture coordinate to be used in following vertices.
    * @param x x coordinate
    * @param y y coordinate
    * @return The index of the new texture coordinate (valid only if a vertex
    * is added with the supplied texture coordinate)
    */
   public abstract int textureCoord(float x, float y);

   /**
    * Sets the current texture coordinates to be used in following vertices,
    * based on texture coordinate index.
    * @param tidx index of a previously added texture coordinate
    */
   public abstract void textureCoord(int tidx);

   /**
    * Updates the values of the texture coordinate with index tidx.
    * @param tidx coordinate index
    * @param x x coordinate
    * @param y y coordinate
    */
   public abstract void setTextureCoord(int tidx, float x, float y);

   /**
    * Whether or not any texture coordinates have been defined.
    */
   public abstract boolean hasTextureCoords();

   /**
    * Number of texture coordinates defined.
    */
   public abstract int numTextureCoords();

   /**
    * Retrieves the texture coordinate at the supplied index.  The returned 
    * coordinate should not be modified.
    * @param tidx position index
    * @return texture coordinate {x,y}
    */
   public abstract float[] getTextureCoord(int tidx);

   /**
    * Retrieves the full list of texture coordinates.  This list should not
    * be modified.
    * @return list of texture coordinates.
    */
   public abstract List<float[]> getTextureCoords();

   /**
    * Sets whether or not texture coordinates should be considered dynamic.  
    * If true, texture coordinates can be updated.  Otherwise, texture 
    * coordinates are considered fixed for all time.  The dynamic property 
    * can only be modified before vertices are Committed.
    * @see #commit() 
    */
   public abstract void setTextureCoordsDynamic(boolean set);

   /**
    * Returns whether or not texture coordinates are considered dynamic.
    */
   public abstract boolean isTextureCoordsDynamic();

   /**
    * Creates a new, alternative set of texture coordinates that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals, 
    * and colors.  The new set of texture coordinates immediately becomes active,
    * and is initialized from the previously active texture coordinate set.
    * @return the index of the newly create texture coordinate set
    * @see #textureCoordSet(int)
    */
   public abstract int createTextureCoordSet();

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
   public abstract int createTextureCoordSetFrom(int copyIdx);

   /**
    * Sets the current active texture coordinate set to be used/modified.  Vertices
    * will use the current active set of texture coordinates when rendering.
    * @param setIdx index of active position set
    */
   public abstract void textureCoordSet(int setIdx);

   /**
    * Returns the index of the currently active texture coordinate set.
    */
   public abstract int getTextureCoordSetIdx();

   /**
    * The number of texture coordinate sets available.
    * @return number of available texture coordinate sets
    */
   public abstract int numTextureCoordSets();

   /**
    * Retrieves the texture coordinate at the supplied index in a given set.  
    * The returned texture coordinate should not be modified.
    * @param tset texture coordinate set index
    * @param tidx texture coordinate index
    * @return texture coordinate {x,y}
    */
   public abstract float[] getTextureCoord(int tset, int tidx);

   /**
    * Retrieves the full list of texture coordinates in a given set.  This list should 
    * not be modified.
    * @param tset texture coordinate set index
    * @return list of texture coordinates.
    */
   public abstract List<float[]> getTextureCoords(int tset);

   /**
    * Returns the latest committed texture coordinates version number,
    * for use in detecting if changes are present.
    */
   public abstract int getTextureCoordsVersion();

   /**
    * Checks whether this render object has any dynamic components.
    */
   public abstract boolean isDynamic();

   /**
    * Hint for ensuring sufficient storage for vertices
    * @param cap capacity
    */
   public abstract void ensureVertexCapacity(int cap);

   /**
    * Adds a vertex using the currently active position, normal, color,
    * and texture coordinate (if available).  
    * @return the index of the newly created vertex.
    */
   public abstract int addVertex();

   /**
    * Adds a vertex using the supplied position index.
    * @return the index of the newly created vertex.
    */
   public abstract int addVertex(int pidx);

   /**
    * Adds a vertex using the supplied position and normal indices.
    * @return the index of the newly created vertex.
    */
   public abstract int addVertex(int pidx, int nidx);

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
   public abstract int addVertex(int pidx, int nidx, int cidx, int tidx);

   /**
    * Add a vertex at the supplied position, using the currently active
    * normal, color and texture coordinate (if available).  A new position
    * is created to accommodate the vertex.
    * @param x x coordinate
    * @param y y coordinate
    * @param z z coordinate
    * @return vertex index
    */
   public abstract int vertex(float x, float y, float z);

   /**
    * Modify the attribute indices of a particular vertex
    * @param vidx index of vertex to modify
    * @param pidx new position index
    * @param nidx new normal index
    * @param cidx new color index
    * @param tidx new texture coordinate index
    */
   public abstract void setVertex(
      int vidx, int pidx, int nidx, int cidx, int tidx);

   /**
    * Returns the number of vertices, as defined by unique sets of position,
    * normal, color, and texture indices.
    */
   public abstract int numVertices();

   /**
    * Returns the position of the supplied vertex
    */
   public abstract float[] getVertexPosition(int vidx);

   /**
    * Returns the normal of the supplied vertex
    */
   public abstract float[] getVertexNormal(int vidx);

   /**
    * Returns the color of the supplied vertex
    */
   public abstract byte[] getVertexColor(int vidx);

   /**
    * Returns the texture coordinate of the supplied vertex
    */
   public abstract float[] getVertexTextureCoord(int vidx);

   /**
    * Retrieves the index set that identifies a vertex
    * @param vidx vertex index
    * @return the IndexSet which contains the position, normal, color and
    * texture coordinate indices
    */
   public abstract VertexIndexSet getVertex(int vidx);

   /**
    * Returns the full set of vertex identifiers
    */
   public abstract List<VertexIndexSet> getVertices();

   /**
    * Checks whether vertex information has been committed
    */
   public abstract boolean isVerticesCommitted();

   /**
    * Returns the latest committed vertices version number,
    * for use in detecting if changes are present.
    */
   public abstract int getVerticesVersion();

   /**
    * Start automatically building primitives for every added vertex,
    * using a specified mode.  Primitives are only gauranteed to be
    * complete after a call to {@link #endBuild()}.
    * @param mode mode for adding consecutive primitives
    */
   public abstract void beginBuild(DrawMode mode);

   /**
    * End automatically building primitives.
    */
   public abstract void endBuild();

   public abstract DrawMode getBuildMode();

   /**
    * Creates a point primitive at the supplied vertex.
    * @param vidx vertex index for point
    */
   public abstract void addPoint(int vidx);

   /**
    * Creates point primitives at the supplied vertex locations.
    * @param vidxs array of vertex indices at which to create point primitives.
    */
   public abstract void addPoints(int... vidxs);

   /**
    * Creates a point primitive at the supplied position.  A vertex is first
    * created using the currently active normal, color and texture coordinate
    * (if available).  
    * @param v array of length 3 (x,y,z)
    */
   public abstract void addPoint(float[] v);

   /**
    * Creates a set of vertices and point primitives at the supplied positions.
    * @see #addPoint(float[])
    * @param pnts positions for which points and vertices should be defined
    */
   public abstract void addPoints(Iterable<float[]> pnts);

   /**
    * Indicates whether any point primitives have been defined.
    */
   public abstract boolean hasPoints();

   /**
    * Number of point primitives defined.
    */
   public abstract int numPoints();

   /**
    * Returns a list of vertex indices of all point primitives defined.
    */
   public abstract List<int[]> getPoints();

   /**
    * Hint for ensuring sufficient storage for points
    * @param cap capacity
    */
   public abstract void ensurePointCapacity(int cap);

   /**
    * Creates a new group of points that can be rendered.  By default,
    * only one point group exists.
    * @return the index of the new group of points
    * @see #pointGroup(int)
    */
   public abstract int createPointGroup();

   /**
    * Sets the current active point group for rendering.
    * @param setIdx index of active point group.
    */
   public abstract void pointGroup(int setIdx);

   /**
    * Returns the index of the currently active point group.
    */
   public abstract int getPointGroupIdx();

   /**
    * The number of point groups available.
    */
   public abstract int numPointGroups();

   /**
    * Number of point primitives defined in a point group.
    */
   public abstract int numPoints(int pgroup);

   /**
    * Retrieves the point at the supplied index in a given group.  
    * The returned point should not be modified.
    * @param pgroup point group index
    * @param pidx point index
    * @return vertex index of point
    */
   public abstract int[] getPoint(int pgroup, int pidx);

   /**
    * Returns the list of points (vertex indices) for a given group
    * @param pgroup point group index
    * @return vertex indices for point group
    */
   public abstract List<int[]> getPoints(int pgroup);

   /**
    * Returns the latest committed points version number,
    * for use in detecting if changes are present.
    */
   public abstract int getPointsVersion();

   /**
    * Creates a line primitive between the supplied vertices.
    * @param v0idx vertex index for start of line
    * @param v1idx vertex index for end of line
    */
   public abstract void addLine(int v0idx, int v1idx);

   /**
    * Creates a set of line primitives between pairs of vertices as specified
    * by the supplied set of vertex indices.  For example, supplying 
    * {1,2,3,4} will create two line segments: 1-2, 3-4.
    * @param vidxs vertex indices, in pairs, defining line segments.  
    */
   public abstract void addLines(int... vidxs);

   /**
    * Creates a set of line primitives between pairs of vertices as specified
    * by the supplied vertex range.  For example, supplying 
    * {1,4} will create two line segments: 1-2, 3-4.
    * @param vStart starting vertex
    * @param vEnd ending vertex  
    */
   public abstract void addLines(int vStart, int vEnd);

   /**
    * Creates a set of line primitives between the supplied pairs of vertices.
    * @param lines int pairs of vertex indices defining line segments.
    */
   public abstract void addLines(Iterable<int[]> lines);

   /**
    * Creates a set of connected line segments between neighboring vertices
    * in the supplied list of vertex indices.  For example, supplying {1,2,3,4}
    * will create three line segments: 1-2, 2-3, 3-4.
    * @param vidxs vertex indices specifying connectivity
    */
   public abstract void addLineStrip(int... vidxs);

   /**
    * Creates a set of connected line primitives between neighboring vertices 
    * as specified by the supplied vertex range.  For example, supplying 
    * {1,4} will create three line segments: 1-2, 2-3, 3-4.
    * @param vStart starting vertex
    * @param vEnd ending vertex  
    */
   public abstract void addLineStrip(int vStart, int vEnd);

   /**
    * Creates a set of connected line segments between neighboring vertices
    * in the supplied list of vertex indices.  For example, supplying {1,2,3,4}
    * will create four line segments: 1-2, 2-3, 3-4, 4-1.
    * @param vidxs vertex indices specifying connectivity
    */
   public abstract void addLineLoop(int... vidxs);

   /**
    * Creates a set of line primitives between neighboring vertices as specified
    * by the supplied vertex range.  For example, supplying 
    * {1,4} will create four line segments: 1-2, 2-3, 3-4, 4-1
    * @param vStart starting vertex
    * @param vEnd ending vertex  
    */
   public abstract void addLineLoop(int vStart, int vEnd);

   /**
    * Creates a line primitive between two new vertices defined
    * at the supplied locations.  The currently active normal, color and texture
    * coordinates will be used when creating the vertices.
    * @param v0 length 3 array for position of starting vertex (x,y,z)
    * @param v1 length 3 array for position of ending vertex (x,y,z)
    */
   public abstract void addLine(float[] v0, float[] v1);

   /**
    * Indicates whether any line primitives have been defined
    */
   public abstract boolean hasLines();

   /**
    * Number of line primitives defined
    */
   public abstract int numLines();

   /**
    * Returns a list of line primitives, identified by vertex index pairs.
    */
   public abstract List<int[]> getLines();

   /**
    * Hint for ensuring sufficient storage for lines
    * @param cap capacity
    */
   public abstract void ensureLineCapacity(int cap);

   /**
    * Creates a new group of lines that can be rendered.  By default,
    * only one line group exists.
    * @return the index of the new group of lines
    * @see #lineGroup(int)
    */
   public abstract int createLineGroup();

   /**
    * Sets the current active line group for rendering.
    * @param setIdx index of active line group.
    */
   public abstract void lineGroup(int setIdx);

   /**
    * Returns the index of the currently active line group.
    */
   public abstract int getLineGroupIdx();

   /**
    * The number of line groups available.
    */
   public abstract int numLineGroups();

   /**
    * Number of line primitives defined in a group.
    */
   public abstract int numLines(int lgroup);

   /**
    * Retrieves the line at the supplied index in a given group.  
    * The returned line should not be modified.
    * @param lgroup line group index
    * @param lidx line index
    * @return vertex indices making up line
    */
   public abstract int[] getLine(int lgroup, int lidx);

   /**
    * Returns the list of lines (vertex indices) for a given group
    * @param lgroup line group index
    * @return vertex indices for line group
    */
   public abstract List<int[]> getLines(int lgroup);

   /**
    * Returns the latest committed lines version number,
    * for use in detecting if changes are present.
    */
   public abstract int getLinesVersion();

   /**
    * Creates a triangle primitive between supplied vertices (CCW order).
    * @param v0idx first vertex
    * @param v1idx second vertex
    * @param v2idx third vertex
    */
   public abstract void addTriangle(int v0idx, int v1idx, int v2idx);

   /**
    * Creates a set of triangle primitives between triples of vertices as 
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create two triangles: 1-2-3, 4-5-6.
    * @param vidxs vertex indices, in triples, defining triangles.
    */
   public abstract void addTriangles(int... vidxs);

   /**
    * Creates a set of triangle primitives between triples of vertices as 
    * supplied by the given vertex range.  For example supplying {1, 6} will
    * create two triangles: 1-2-3, 4-5-6.
    * @param vStart starting vertex index
    * @param vEnd ending vertex index
    */
   public abstract void addTriangles(int vStart, int vEnd);

   /**
    * Creates a set of triangle primitives forming a fan using the
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create four triangles: 1-2-3, 1-3-4, 1-4-5, 1-5-6.
    * @param vidxs vertex indices defining triangles.
    */
   public abstract void addTriangleFan(int... vidxs);

   /**
    * Creates a set of triangle primitives forming a fan using the
    * supplied vertex range.  For example supplying {1, 6} will
    * create four triangles: 1-2-3, 1-3-4, 1-4-5, 1-5-6.
    * @param vStart starting vertex index
    * @param vEnd ending vertex index
    */
   public abstract void addTriangleFan(int vStart, int vEnd);

   /**
    * Creates a set of triangle primitives forming a strip using the
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create four triangles: 1-2-3, 3-2-4, 3-4-5, 5-4-6
    * @param vidxs vertex indices defining triangles.
    */
   public abstract void addTriangleStrip(int... vidxs);

   /**
    * Creates a set of triangle primitives forming a strip using the
    * supplied by vertex range.  For example supplying {1,6} will
    * create four triangles: 1-2-3, 3-2-4, 3-4-5, 5-4-6
    * @param vStart starting vertex index
    * @param vEnd ending vertex index
    */
   public abstract void addTriangleStrip(int vStart, int vEnd);

   /**
    * Creates a set of triangle primitives between supplied triples of 
    * vertices.
    * @param tris int triples of vertex indices defining triangles (CCW)
    */
   public abstract void addTriangles(Iterable<int[]> tris);

   /**
    * Creates a triangle primitive between three new vertices defined at the
    * supplied locations.  The currently active normal, color, and texture
    * coordinates will be used when creating the vertices.
    * @param v0 {x,y,z} position of first vertex
    * @param v1 {x,y,z} position of second vertex
    * @param v2 {x,y,z} position of third vertex
    */
   public abstract void addTriangle(float[] v0, float[] v1, float[] v2);

   /**
    * Indicates whether any triangle primitives have been defined.
    */
   public abstract boolean hasTriangles();

   /**
    * Number of triangle primitives defined.
    */
   public abstract int numTriangles();

   /**
    * Returns a list of triangle primitives, identified by vertex index triples.
    */
   public abstract List<int[]> getTriangles();

   /**
    * Hint for ensuring sufficient storage for triangles
    * @param cap capacity
    */
   public abstract void ensureTriangleCapacity(int cap);

   /**
    * Creates a new group of triangles that can be rendered.  By default,
    * only one triangle group exists.
    * @return the index of the new group of triangles
    * @see #triangleGroup(int)
    */
   public abstract int createTriangleGroup();

   /**
    * Sets the current active triangle group for rendering.
    * @param setIdx index of active triangle group.
    */
   public abstract void triangleGroup(int setIdx);

   /**
    * Returns the index of the currently active triangle group.
    */
   public abstract int getTriangleGroupIdx();

   /**
    * The number of triangle groups available.
    */
   public abstract int numTriangleGroups();

   /**
    * Number of triangle primitives defined in a group.
    */
   public abstract int numTriangles(int tgroup);

   /**
    * Retrieves the triangle at the supplied index in a given group.  
    * The returned triangle should not be modified.
    * @param tgroup triangle group index
    * @param tidx triangle index
    * @return vertex indices of triangle
    */
   public abstract int[] getTriangle(int tgroup, int tidx);

   /**
    * Returns the list of triangles (vertex indices) for a given group
    * @param tgroup point group index
    * @return vertex indices for triangle group
    */
   public abstract List<int[]> getTriangles(int tgroup);

   /**
    * Returns the latest committed triangles version number,
    * for use in detecting if changes are present.
    */
   public abstract int getTrianglesVersion();

   /**
    * Clears all primitive groups, allowing them to be recreated.  Vertex
    * information remains untouched.
    */
   public abstract void clearPrimitives();

   /**
    * Clears everything in the RenderObject, allowing it to be
    * recreated.  This can be used when the object becomes invalid,
    * in place of discarding and regenerating a new one.  The object
    * becomes a clean slate, with no vertex attributes or primitives.
    */
   public abstract void reinitialize();

   /**
    * Signal that the object is complete and ready for rendering.
    * No more data can be added to the renderable until it is cleared.  Only 
    * dynamic components can be modified.  This function should be called
    * before first use (either manually, or by a renderer)
    */
   public abstract void commit();

   /**
    * Indicates whether or not the object is complete and ready
    * for rendering.
    */
   public abstract boolean isCommitted();

   /**
    * Retrieves the version of the object, for use in detecting
    * whether any information has changed since last use.
    * @return the overall modification version
    */
   public abstract int getVersion();

   /**
    * Invalidates the object.  An invalid object cannot be drawn by the renderer.
    * @see #isValid()
    */
   public abstract void invalidate();

   /**
    * Returns whether or not this RenderObject is valid.  If valid,
    * this object can be safely passed to a renderer for drawing.  
    * If not, it needs to be discarded.
    * @return <code>true</code> if this RenderObject is valid
    */
   public abstract boolean isValid();

   /**
    * Signal a destruction of the object.
    */
   public abstract void dispose();

   /**
    * A streaming object has a short life-span, and cannot be modified once committed.
    */
   public abstract void setStreaming(boolean set);

   /**
    * A streaming object has a short life-span, and cannot be modified once committed.
    */
   public abstract boolean isStreaming();

}
