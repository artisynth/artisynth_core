/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import maspack.render.Renderer;


public interface GLRenderer extends Renderer {

   /**
    * Returns true if the renderer is currently performing a selection
    * render. When this is the case, the application should avoid calls
    * affecting color and lighting.
    *
    * @return true if the renderer is in selection mode.
    */
   public boolean isSelecting();
   
   public HighlightStyle getSelectionHighlightStyle();

   public void getSelectionColor(float[] rgba);
   
   /**
    * Begins a selection query with the {\it query identifier}
    * <code>qid</code>.  If the rendering that occurs between this call and the
    * subsequent call to {@link #endSelectionQuery} results in anything being
    * rendered within the selection window, a {\it selection hit} will be
    * generated for <code>qid</code>.
    *
    * If called within the <code>render</code> method for a {@link
    * maspack.render.GL.GLSelectable}, then <code>qid</code> must lie in the range
    * 0 to <code>numq</code>-1, where <code>numq</code> is the value returned
    * by {@link maspack.render.GL.GLSelectable#numSelectionQueriesNeeded
    * GLSelectable.numSelectionQueriesNeeded()}.  Selection queries cannot be
    * nested, and a given query identifier should be used only once.
    *
    * @param qid identifier for the selection query
    * @see #endSelectionQuery
    */
   public void beginSelectionQuery (int qid);

   /**
    * Ends a selection query that was initiated with a call to
    * {@link #beginSelectionQuery}.
    *
    * @see #beginSelectionQuery
    */
   public void endSelectionQuery ();

   /**
    * Begins selection for a {@link maspack.render.GL.GLSelectable}
    * <code>s</code>that
    * manages its own selection; this call should
    * be used in place of {@link #beginSelectionQuery} for such objects.
    * Selectables that manage their own selection are identified by
    * having a value <code>numq</code> >= 0, where <code>numq</code>
    * is the value returned by
    * {@link maspack.render.GL.GLSelectable#numSelectionQueriesNeeded
    * GLSelectable#numSelectionQueriesNeeded{}}.
    * The argument <code>qid</code> is the current selection query identifier,
    * which after the call should be increased by <code>numq</code>.
    *
    * @param s Selectable that is managing its own sub-selection
    * @param qid current selection query identifier
    */
   public void beginSubSelection (GLSelectable s, int qid);

   /**
    * Ends object sub-selection that was initiated with a call
    * to {@link #beginSubSelection}.
    *
    * @see #endSubSelection
    */
   public void endSubSelection ();

   /**
    * Sets a selection filter for the renderer. This restricts which objects
    * are actually rendered when a selection render is performed, and therefore
    * restricts which objects can actually be selected. This allows
    * selection of objects that might otherwise be occluded within a scene.
    *
    * @param filter Selection filter to be applied
    */
   public void setSelectionFilter (GLSelectionFilter filter);

   /**
    * Returns the current selection filter for the renderer, if any.
    *
    * @return current selection filter, or <code>null</code> if there is none.
    */
   public GLSelectionFilter getSelectionFilter ();

   /**
    * Returns true if <code>s</code> is selectable in the current selection
    * context. This will be the case if <code>s.isSelectable()</code> returns
    * <code>true</code>, and <code>s</code> also passes whatever selection
    * filter might currently be set in the renderer.
    */
   public boolean isSelectable (GLSelectable s);

   
   public GL getGL();
   
   @Deprecated
   public GL2 getGL2();
}
