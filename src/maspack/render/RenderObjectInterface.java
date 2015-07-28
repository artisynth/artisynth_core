package maspack.render;

import java.util.List;

/**
 * Generic cached renderable object
 */

public interface RenderObjectInterface {
   
   //=========================================================================
   // Positions, Normals, Colors, Textures
   //=========================================================================
   /**
    * Adds an indexable 3D position
    * @param x 
    * @param y
    * @param z
    * @return the index of the position added
    */
   public int addPosition(float x, float y, float z);

   /**
    * Sets the current 3D position to be used in following vertices.
    * @param x 
    * @param y
    * @param z
    * @return an index to be used for referring to the provided position
    */
   public int setCurrentPosition(float x, float y, float z);

   /**
    * Sets the current position to be used in following vertices, 
    * based on position index. 
    * @param pidx index of a previously added position
    */
   public void setCurrentPosition(int pidx);

   /**
    * Updates the values of the position with index pidx.  Following this call,
    * {@link #isModified()} will return true.
    * @param pidx position to modify
    * @param x
    * @param y
    * @param z
    */
   public void setPosition(int pidx, float x, float y, float z);
   
   /**
    * Whether or not any positions have been defined.
    */
   public boolean hasPositions();

   /**
    * Number of positions defined.
    */
   public int numPositions();

   /**
    * Retrieves the position at the supplied index.  The returned position
    * should not be modified.
    * @param pidx position index
    * @return position {x,y,z}
    */
   public float[] getPosition(int pidx);
   
   /**
    * Retrieves the full list of positions.  This list should not
    * be modified.
    * @return list of positions.
    */
   public List<float[]> getPositions();
   
   /**
    * Sets whether or not positions should be considered dynamic.  If true,
    * positions can be updated.  Otherwise, positions are 
    * considered fixed for all time.  The dynamic property can only be 
    * modified before the first position is added. 
    */
   public void setPositionsDynamic(boolean set);

   /**
    * Returns whether or not positions are considered dynamic.
    */
   public boolean isPositionsDynamic();

   /**
    * Creates a new set of positions that can be used by vertices.  
    * This allows sharing of other resources, such as normals, colors and 
    * textures.  The new set of positions immediately becomes active, and
    * is initialized from the previously active position set.
    * @return the index of the newly created position set
    * @see #setPositionSet(int)
    */
   public int createPositionSet();

   /**
    * Creates a new set of positions that can be used by vertices.  
    * This allows sharing of other resources, such as normals, colors and 
    * textures.  The new set of positions immediately becomes active, and 
    * is initialized from the position set identified by the supplied index.
    * @param copyIdx index of position set to duplicate
    * @return the index of the newly created position set
    * @see #setPositionSet(int)
    */
   public int createPositionSetFrom(int copyIdx);
   
   /**
    * Sets the current active position set to be used/modified.  Vertices
    * will use the current active set of positions when rendering.
    * @param setIdx index of active position set
    */
   public void setPositionSet(int setIdx);

   /**
    * Returns the index of the currently active position set.
    */
   public int getPositionSet();
   
   /**
    * The number of position sets available.
    */
   public int numPositionSets();

   /**
    * Adds an indexable 3D normal.
    * @param x
    * @param y
    * @param z
    * @return the index of the normal added
    */
   public int addNormal(float x, float y, float z);

   /**
    * Sets the current 3D normal to be used in following
    * vertices.
    * @param x 
    * @param y
    * @param z
    * @return an index to be used for referring to the provided normal
    */
   public int setCurrentNormal(float x, float y, float z);

   /**
    * Sets the current normal to be used in following vertices, 
    * based on normal index. 
    * @param nidx index of a previously added normal
    */
   public void setCurrentNormal(int nidx);

   /**
    * Updates the values of the normal with index nidx.  Following this call,
    * {@link #isModified()} will return true.
    * @param nidx normal to modify
    * @param x
    * @param y
    * @param z
    */
   public void setNormal(int nidx, float x, float y, float z);

   /**
    * Whether or not any normals have been defined.
    */
   public boolean hasNormals();

   /**
    * Number of normals defined.
    */
   public int numNormals();
   
   /**
    * Retrieves the normal at the supplied index.  The returned normal
    * should not be modified.
    * @param nidx normal index
    * @return normal {x,y,z}
    */
   public float[] getNormal(int nidx);
   
   /**
    * Retrieves the full list of normals.  This list should not
    * be modified.
    * @return list of normals.
    */
   public List<float[]> getNormals();

   /**
    * Sets whether or not normals should be considered dynamic.  If true,
    * normals can be updated.  Otherwise, normals are considered fixed for 
    * all time.  The dynamic property can only be modified 
    * before the first normal is added. 
    */
   public void setNormalsDynamic(boolean set);

   /**
    * Returns whether or not normals are considered dynamic.
    */
   public boolean isNormalsDynamic();

   /**
    * Creates a new set of normals that can be used by vertices.  
    * This allows sharing of other resources, such as positions, colors and 
    * textures.  The new set of normals immediately becomes active, and is
    * initialized from the previously active normal set.
    * @return the index of the newly created normal set
    * @see #setNormalSet(int)
    */
   public int createNormalSet();
   
   /**
    * Creates a new set of normals that can be used by vertices.  
    * This allows sharing of other resources, such as positions, colors and 
    * textures.  The new set of normals will be initialized from the
    * normal set identified by the supplied index.
    * @param copyIdx index of normal set to duplicate
    * @return the index of the newly created normal set
    * @see #setNormalSet(int)
    */
   public int createNormalSetFrom(int copyIdx);

   /**
    * Sets the current active normal set to be used/modified.  Vertices
    * will use the current active set of normals when rendering.
    * @param setIdx index of active normal set
    */
   public void setNormalSet(int setIdx);

   /**
    * Returns the index of the currently active normal set.
    */
   public int getNormalSet();
   
   /**
    * The number of normal sets available.
    */
   public int numNormalSets();

   /**
    * Adds an indexable color
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    * @return the index of the color added
    */
   public int addColor(byte r, byte g, byte b, byte a);

   /**
    * Adds an indexable color
    * @param rgba {red, green, blue, alpha}
    * @return the index of the color added
    */
   public int addColor(byte[] rgba);
   
   /**
    * Sets the current color to be used in following vertices.
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    * @return an index to be used for referring to the provided color
    */
   public int setCurrentColor(byte r, byte g, byte b, byte a);
   
   /**
    * Sets the current color to be used in following vertices.
    * @param rgba {red, green, blue, alpha}
    * @return an index to be used for referring to the provided color
    */
   public int setCurrentColor(byte[] rgba);

   /**
    * Sets the current color to be used in following vertices
    * based on color index.
    * @param cidx index of a previously added color
    */
   public void setCurrentColor(int cidx);

   /**
    * Updates the values of the color with index cidx. Following this call,
    * {@link #isModified()} will return true.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public void setColor(int cidx, byte r, byte g, byte b, byte a);
   
   /**
    * Updates the values of the color with index cidx. Following this call,
    * {@link #isModified()} will return true.
    * @param cidx color to modify
    * @param rgba {red, green, blue, alpha}
    */
   public void setColor(int cidx, byte[] rgba);

   /**
    * Whether or not any colors have been defined.
    */
   public boolean hasColors();

   /**
    * Number of positions defined
    */
   public int numColors();
   
   /**
    * Retrieves the color at the supplied index.  The returned color
    * should not be modified.
    * @param cidx color index
    * @return color {red, green, blue, alpha}
    */
   public byte[] getColor(int cidx);
   
   /**
    * Retrieves the full list of Colors.  This list should not
    * be modified.
    * @return list of colors.
    */
   public List<byte[]> getColors();

   /**
    * Sets whether or not colors should be considered dynamic.  If true,
    * colors can be updated.  Otherwise, colors are considered fixed for 
    * all time.  The dynamic property can only be modified before the first 
    * color is added. 
    */
   public void setColorsDynamic(boolean set);

   /**
    * Returns whether or not colors are considered dynamic.
    */
   public boolean isColorsDynamic();

   /**
    * Creates a new set of colors that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals, 
    * and textures.  The new set of colors immediately becomes active, and 
    * is initialized from the previously active color set.
    * @return the index of the newly created color set
    * @see #setColorSet(int)
    */
   public int createColorSet();
   
   /**
    * Creates a new set of colors that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals and 
    * textures.  The new set of colors immediately becomes active, and 
    * is initialized from the color set identified by the supplied index.
    * @param copyIdx index of color set to duplicate
    * @return the index of the newly created color set
    * @see #setColorSet(int)
    */
   public int createColorSetFrom(int copyIdx);

   /**
    * Sets the current active color set to be used/modified.  Vertices
    * will use the current active set of colors when rendering.
    * @param setIdx index of active color set
    */
   public void setColorSet(int setIdx);
   
   /**
    * Returns the index of the currently active color set.
    */
   public int getColorSet();

   /**
    * The number of color sets available.
    */
   public int numColorSets();

   /**
    * Adds an indexable 2D texture coordinate
    * @param x
    * @param y
    * @return the index of the texture coordinate added
    */
   public int addTextureCoord(float x, float y);

   /**
    * Sets the current 2D texture coordinate to be used in following vertices.
    * @param x
    * @param y
    * @return an index to be used for referring to the provided texture coordinate
    */
   public int setCurrentTextureCoord(float x, float y);

   /**
    * Sets the current texture coordinates to be used in following vertices,
    * based on texture coordinate index.
    * @param tidx index of a previously added texture coordinate
    */
   public void setCurrentTextureCoord(int tidx);

   /**
    * Updates the values of the texture coordinate with index tidx.  Following this call,
    * {@link #isModified()} will return true;
    * @param tidx
    * @param x
    * @param y
    */
   public void setTextureCoord(int tidx, float x, float y);
   
   /**
    * Whether or not any texture coordinates have been defined.
    */
   public boolean hasTextureCoords();

   /**
    * Number of texture coordinates defined.
    */
   public int numTextureCoords();
   
   /**
    * Retrieves the texture coordinate at the supplied index.  The returned 
    * coordinate should not be modified.
    * @param tidx position index
    * @return texture coordinate {x,y}
    */
   public float[] getTextureCoord(int tidx);
   
   /**
    * Retrieves the full list of texture coordinates.  This list should not
    * be modified.
    * @return list of texture coordinates.
    */
   public List<float[]> getTextureCoords();
   
   /**
    * Sets whether or not texture coordinates should be considered dynamic.  
    * If true, texture coordinates can be updated.  Otherwise, texture 
    * coordinates are considered fixed for all time.  The dynamic property 
    * can only be modified before the first texture coordinate is added. 
    */
   public void setTextureCoordsDynamic(boolean set);

   /**
    * Returns whether or not texture coordinates are considered dynamic.
    */
   public boolean isTextureCoordsDynamic();

   /**
    * Creates a new set of texture coordinates that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals, 
    * and colors.  The new set of texture coordinates immediately becomes active,
    * and is initialized from the previously active texture coordinate set.
    * @return the index of the newly create texture coordinate set
    * @see #setTextureCoordSet(int)
    */
   public int createTextureCoordSet();
   
   /**
    * Creates a new set of texture coordinates that can be used by vertices.  
    * This allows sharing of other resources, such as positions, normals, and
    * colors.  The new set of texture coordinates immediately becomes active,
    * and is initialized from the texture coordinate set identified by the 
    * supplied index.
    * @param copyIdx index of texture coordinate set to duplicate
    * @return the index of the newly created texture coordinate set
    * @see #setTextureCoordSet(int)
    */
   public int createTextureCoordSetFrom(int copyIdx);

   /**
    * Sets the current active texture coordinate set to be used/modified.  Vertices
    * will use the current active set of texture coordinates when rendering.
    * @param setIdx index of active position set
    */
   public void setTextureCoordSet(int setIdx);

   /**
    * Returns the index of the currently active texture coordinate set.
    */
   public int getTextureCoordSet();
   
   /**
    * The number of texture coordinate sets available.
    */
   public int numTextureCoordSets();
   
   
   //=========================================================================
   // Vertices
   //=========================================================================

   /**
    * Adds a vertex using the currently active position, normal, color,
    * and texture coordinate (if available).  If such a vertex already exists,
    * its index is returned.
    * @return the index of the existing or newly created vertex.
    */
   public int addVertex();

   /**
    * Adds a vertex at the supplied position, using the currently active 
    * normal, color and texture coordinate (if available).  A new position is
    * created to accommodate the vertex 
    * (see {@link #addPosition(float, float, float)})
    * @param x
    * @param y
    * @param z
    * @return the index of the newly created vertex.
    */
   public int addVertex(float x, float y, float z);

   /**
    * Adds a vertex using the position, normal, color and texture
    * coordinates identified by index number.  Negative index values are 
    * ignored.  If such a vertex already exists, then its index is returned.
    * 
    * @param pidx position index
    * @param nidx normal index
    * @param cidx color index
    * @param tidx texture coordinate index
    * @return the index of the existing or newly created vertex.
    */
   public int addVertex(int pidx, int nidx, int cidx, int tidx);
   
   /**
    * Returns the number of vertices, as defined by unique sets of position,
    * normal, color, and texture indices.
    */
   public int numVertices();

   /**
    * Returns the position of the supplied vertex
    */
   public float[] getVertexPosition(int vidx);
   
   /**
    * Returns the normal of the supplied vertex
    */
   public float[] getVertexNormal(int vidx);
   
   /**
    * Returns the color of the supplied vertex
    */
   public byte[] getVertexColor(int vidx);
   
   /**
    * Returns the texture coordinate of the supplied vertex
    */
   public float[] getVertexTextureCoord(int vidx);
   
   //=========================================================================
   // Points, lines, triangles
   //=========================================================================
   
   /**
    * Creates a point primitive at the supplied vertex.
    * @param vidx vertex index for point
    */
   public void addPoint(int vidx);
   
   /**
    * Creates point primitives at the supplied vertex locations.
    * @param vidxs array of vertex indices at which to create point primitives.
    */
   public void addPoints(int[] vidxs);

   /**
    * Creates a point primitive at the supplied position.  A vertex is first
    * created using the currently active normal, color and texture coordinate
    * (if available).  
    * @param v array of length 3 (x,y,z)
    */
   public void addPoint(float[] v);
   
   /**
    * Creates a set of vertices and point primitives at the supplied positions.
    * @see #addPoint(float[])
    * @param pnts
    */
   public void addPoints(Iterable<float[]> pnts);

   /**
    * Indicates whether any point primitives have been defined.
    */
   public boolean hasPoints();

   /**
    * Number of point primitives defined.
    */
   public int numPoints();

   /**
    * Returns a list of vertex indices of all point primitives defined.
    */
   public List<Integer> getPoints();
   
   /**
    * Creates a new group of points that can be rendered.  By default,
    * only one point group exists.
    * @return the index of the new group of points
    * @see #setPointGroup(int)
    */
   public int createPointGroup();
   
   /**
    * Sets the current active point group for rendering.
    * @param setIdx index of active point group.
    */
   public void setPointGroup(int setIdx);

   /**
    * Returns the index of the currently active point group.
    */
   public int getPointGroup();
   
   /**
    * The number of point groups available.
    */
   public int numPointGroups();
   
   /**
    * Creates a line primitive between the supplied vertices.
    * @param v0idx vertex index for start of line
    * @param v1idx vertex index for end of line
    */
   public void addLine(int v0idx, int v1idx);
   
   /**
    * Creates a set of line primitives between pairs of vertices as specified
    * by the supplied set of vertex indices.  For example, supplying 
    * {1,2,3,4} will create two line segments: 1-2, 3-4.
    * @param vidxs vertex indices, in pairs, defining line segments.  
    */
   public void addLines(int[] vidxs);
   
   /**
    * Creates a set of line primitives between the supplied pairs of vertices.
    * @param lines int pairs of vertex indices defining line segments.
    */
   public void addLines(Iterable<int[]> lines);
   
   /**
    * Creates a set of connected line segments between neighboring vertices
    * in the supplied list of vertex indices.  For example, supplying {1,2,3,4}
    * will create three line segments: 1-2, 2-3, 3-4.
    * @param vidxs vertex indices specifying connectivity
    */
   public void addLineStrip(int[] vidxs);
   
   /**
    * Creates a set of connected line segments between neighboring vertices
    * in the supplied list of vertex indices.  For example, supplying {1,2,3,4}
    * will create four line segments: 1-2, 2-3, 3-4, 4-1.
    * @param vidxs vertex indices specifying connectivity
    */
   public void addLineLoop(int[] vidxs);

   /**
    * Creates a line primitive between two new vertices defined
    * at the supplied locations.  The currently active normal, color and texture
    * coordinates will be used when creating the vertices.
    * @param v0 length 3 array for position of starting vertex (x,y,z)
    * @param v1 length 3 array for position of ending vertex (x,y,z)
    */
   public void addLine(float[] v0, float[] v1);
   
   /**
    * Indicates whether any line primitives have been defined
    */
   public boolean hasLines();

   /**
    * Number of line primitives defined
    */
   public int numLines();
   
   /**
    * Returns a list of line primitives, identified by vertex index pairs.
    */
   public List<int[]> getLines();
   
   /**
    * Creates a new group of lines that can be rendered.  By default,
    * only one line group exists.
    * @return the index of the new group of lines
    * @see #setLineGroup(int)
    */
   public int createLineGroup();
   
   /**
    * Sets the current active line group for rendering.
    * @param setIdx index of active line group.
    */
   public void setLineGroup(int setIdx);

   /**
    * Returns the index of the currently active line group.
    */
   public int getLineGroup();
   
   /**
    * The number of line groups available.
    */
   public int numLineGroups();

   /**
    * Creates a triangle primitive between supplied vertices (CCW order).
    * @param v0idx first vertex
    * @param v1idx second vertex
    * @param v2idx third vertex
    */
   public void addTriangle(int v0idx, int v1idx, int v2idx);
   
   /**
    * Creates a set of triangle primitives between triples of vertices as 
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create two triangles: 1-2-3, 4-5-6.
    * @param vidxs vertex indices, in triples, defining triangles.
    */
   public void addTriangles(int[] vidxs);
   
   /**
    * Creates a set of triangle primitives forming a fan using the
    * supplied by vertex indices.  For example supplying {1,2,3,4,5,6} will
    * create four triangles: 1-2-3, 1-3-4, 1-4-5, 1-5-6.
    * @param vidxs vertex indices, in triples, defining triangles.
    */
   public void addTriangleFan(int[] vidxs);
   
   /**
    * Creates a set of triangle primitives between supplied triples of 
    * vertices.
    * @param tris int triples of vertex indices defining triangles (CCW)
    */
   public void addTriangles(Iterable<int[]> tris);

   /**
    * Creates a triangle primitive between three new vertices defined at the
    * supplied locations.  The currently active normal, color, and texture
    * coordinates will be used when creating the vertices.
    * @param v0 {x,y,z} position of first vertex
    * @param v1 {x,y,z} position of second vertex
    * @param v2 {x,y,z} position of third vertex
    */
   public void addTriangle(float[] v0, float[] v1, float[] v2);

   /**
    * Indicates whether any triangle primitives have been defined.
    */
   public boolean hasTriangles();

   /**
    * Number of triangle primitives defined.
    */
   public int numTriangles();
   
   /**
    * Returns a list of triangle primitives, identified by vertex index triples.
    */
   public List<int[]> getTriangles();
   
   /**
    * Creates a new group of triangles that can be rendered.  By default,
    * only one triangle group exists.
    * @return the index of the new group of triangles
    * @see #setTriangleGroup(int)
    */
   public int createTriangleGroup();
   
   /**
    * Sets the current active triangle group for rendering.
    * @param setIdx index of active triangle group.
    */
   public void setTriangleGroup(int setIdx);

   /**
    * Returns the index of the currently active triangle group.
    */
   public int getTriangleGroup();
   
   /**
    * The number of triangle groups available.
    */
   public int numTriangleGroups();
   
   //=========================================================================
   // Usage flags
   //=========================================================================

   /**
    * Returns whether or not the dynamic components of the object have been
    * modified since last use.
    */
   public boolean isModified();

   /**
    * Invalidates the object.  An invalid object cannot be drawn by the renderer.
    * @see #isValid()
    */
   public void invalidate();

   /**
    * Returns whether or not the renderable object is valid.  If valid,
    * this object can be safely passed to a renderer for drawing.  
    * If not, it needs to be either repopulated or discarded.
    * @return 
    */
   public boolean isValid();

   /**
    * Clears internal storage.  Once called, it is assumed all data
    * has been properly consumed by the renderer.  Clearing the storage 
    * DOES NOT invalidate the object.  It is assumed the renderer still 
    * contains the information required for drawing.
    */
   // NOTE: if multiple renderers, or multiple contexts, we 
   //       might not be able to clear storage at all.  Doing so will
   //       cause one of the renderers to choke when trying to retrieve data.
   public void clearStorage();  

   /**
    * Returns whether or not the object still contains a valid copy of all
    * data defined.
    */
   public boolean isStorageValid();
   
}
