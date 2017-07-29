package maspack.render.GL;

import com.jogamp.opengl.GL;

/**
 * Allows pipeline rendering of primitives.  Once a certain
 * number of vertices are reached, these are automatically
 * drawn to the active context.
 * 
 * @author Antonio
 *
 */
public interface GLPipelineRenderer {
   
   /**
    * Set up properties of pipeline.  Properties cannot be changed
    * while rendering.
    */
   public void setup(boolean hasNormals, boolean hasColors, boolean hasTexcoords);

   /**
    * Enables/disables per-vertex normals.  Cannot be changed
    * while rendering.
    */
   public void enableNormals(boolean set);
   
   public boolean isNormalsEnabled();
   
   /**
    * Enables/disables per-vertex colors.  Cannot be changed
    * while rendering.
    */
   public void enableColors(boolean set);
   
   public boolean isColorsEnabled();
   
   /**
    * Enables/disables per-vertex texture coordinates.
    * Cannot be changed while rendering.
    */
   public void enableTexCoords(boolean set);
   
   public boolean isTexCoordsEnabled();
   
   /**
    * Begins rendering of primitives
    * @param gl context to use
    * @param glMode draw mode
    * @param maxVertices maximum number of vertices before content
    * is flushed.  This should be a valid multiple of the vertices
    * required to complete primitives in the given draw mode.
    */
   public void begin(GL gl, int glMode, int maxVertices);
   
   /**
    * Cues up a normal to use for following vertices
    */
   public void normal(float x, float y, float z);
   
   /**
    * Cues up a color to use for following vertices
    */
   public void color(int r, int g, int b, int a);
   
   /**
    * Cues up a texture coordinate to use for following
    * vertices
    */
   public void texcoord(float x, float y);
   
   /**
    * Adds a vertex to the pipeline with the currently active
    * attributes.
    */
   public void vertex(float x, float y, float z);

   /**
    * Flushes the pipeline
    */
   public void flush();
   
   /**
    * Ends drawing, flushing the pipeline if necessary
    */
   public void end();
   
   /**
    * Checks whether or not pipeline is empty
    * @return true if there is content that can be flushed
    */
   public boolean isEmpty();
   
   /**
    * Disposes of any resources.  The renderer can no longer
    * be used after it is disposed.
    */
   public void dispose(GL gl);
   
   /**
    * The currently-active GL context if rendering.  Null if not between
    * {@link #begin(GL, int, int)} and {@link #end()}.
    * @return the active GL context
    */
   public GL getGL();
   
}
