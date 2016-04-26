/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.util.LinkedList;

import artisynth.core.modelbase.RenderableComponentList;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolygonalMeshRenderer;
import maspack.properties.PropertyList;
import maspack.render.PointRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.VertexIndexArray;

public class FaceList<P extends FaceComponent> extends RenderableComponentList<P> {
   
   protected static final long serialVersionUID = 1;

   private final int REG_GRP = 0;
   private final int SEL_GRP = 1;
   
   PolygonalMesh myMesh;
   private PolygonalMeshRenderer myMeshRenderer;
   private VertexIndexArray[] myTriangles;
   private int[][] myTriangleOffsets;
   private VertexIndexArray[] myLines;
   private int[][] myLineOffsets;

   public static PropertyList myProps =
      new PropertyList (FaceList.class, RenderableComponentList.class);

   static {
      myProps.get ("renderProps").setDefaultValue (new PointRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public FaceList (
      Class<P> type, String name, String shortName, PolygonalMesh mesh) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
      
      myMesh = mesh;
      myMeshRenderer = null;
      myTriangles = null;
      myTriangleOffsets = null;
      myLines = null;
      myLineOffsets = null;
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createMeshProps (this);
   }

   public void prerender (RenderList list) {
      
      // create stored copy of render information
      if (myMeshRenderer == null) {
         myMeshRenderer = new PolygonalMeshRenderer (myMesh);
      }
      myMeshRenderer.prerender (getRenderProps());
      
      if (myTriangles == null) {
         myTriangles = new VertexIndexArray[2];
         myTriangleOffsets = new int[2][size()];
         myTriangles[REG_GRP] = new VertexIndexArray (3*size());
         myTriangles[SEL_GRP] = new VertexIndexArray (3*size());
         myTriangleOffsets[REG_GRP] = new int[size()+1];
         myTriangleOffsets[SEL_GRP] = new int[size()+1];
         
         myLines = new VertexIndexArray[2];
         myLineOffsets = new int[2][size()];
         myLines[REG_GRP] = new VertexIndexArray (6*size());
         myLines[SEL_GRP] = new VertexIndexArray (6*size());
         myLineOffsets[REG_GRP] = new int[size()+1];
         myLineOffsets[SEL_GRP] = new int[size()+1];
      }
      
      int toff[] = {0, 0};
      int eoff[] = {0, 0};
      for (int i = 0; i < size(); i++) {
         FaceComponent p = get (i);
         
         myTriangleOffsets[REG_GRP][i] = toff[REG_GRP];
         myTriangleOffsets[SEL_GRP][i] = toff[SEL_GRP];
         myLineOffsets[REG_GRP][i] = eoff[REG_GRP];
         myLineOffsets[SEL_GRP][i] = eoff[SEL_GRP];
         
         if (p.getRenderProps() != null) {
            list.addIfVisible (p);
         }
         else {
            // p.prerender (list);
            int gidx = p.isSelected () ? SEL_GRP : REG_GRP;
            
            //            int nt = getFaceTriangles (p.getFace (), myMeshRenderer, myTriangles[gidx], toff[gidx]);
            //            int ne = getFaceLines (p.getFace (), myMeshRenderer, myLines[gidx], eoff[gidx]);
            //            
            //            toff[gidx] += nt;
            //            eoff[gidx] += ne;
         }
      }
      
      // resize arrays
      for (int i=0; i<2; ++i) {
         myTriangles[i].resize (toff[i]);
         myTriangleOffsets[i][size()] = toff[i];
         myLines[i].resize (eoff[i]);
         myLineOffsets[i][size()] = eoff[i];
      }
      
      
      
      
   }

   public boolean rendersSubComponents() {
      return true;
   }

   public void render (Renderer renderer, int flags) {
      
      RenderProps props = getRenderProps();

      //      // selected
      //      int hflags = flags | Renderer.HIGHLIGHT;
      //      PolygonalMeshRenderer.getInstance ().render (renderer, myMesh, props, hflags, myMeshRenderer,
      //         myTriangles[SEL_GRP], myTriangleOffsets[SEL_GRP], myLines[SEL_GRP], myLineOffsets[SEL_GRP]);
      //
      //      // non selected
      //      PolygonalMeshRenderer.getInstance ().render (renderer, myMesh, props, flags, myMeshRenderer,
      //         myTriangles[REG_GRP], myTriangleOffsets[REG_GRP], myLines[REG_GRP], myLineOffsets[REG_GRP]);
   }
   
   
   /**
    * Appends Face triangles to an index array associated with the 
    * provided rendering context.  This is most useful for rendering
    * a selection of faces from a PolygonalMesh.
    * @param face face of which to obtain appropriate rendering indices
    * @param rinfo rendering information to be used along with the faces
    * @param out list of indices to populate
    * @param offset offset into the list to put the value
    * @return number of indices added to <code>out</code>
    */
   public static int getFaceTriangles(Face face, PolygonalMesh mesh, 
      VertexIndexArray out, int offset) {
      
      int[] indexOffs = mesh.getFeatureIndexOffsets ();
      
      int nadd = 0;
      
      int idx = face.getIndex ();
      int foff = indexOffs[idx];
      int numv = indexOffs[idx+1] - foff;

      // triangle fan
      for (int j=0; j<numv-2; ++j) {
         for (int i=0; i<3; ++i) {
            out.set (offset+nadd, foff+i+j);
            ++nadd;
         }
      }
     
      return nadd;
   }
   
   /**
    * Appends Face lines to an index array associated with the 
    * provided rendering context.  This is most useful for rendering
    * a selection of faces from a PolygonalMesh.
    * @param face face of which to obtain appropriate rendering indices
    * @param rinfo rendering information to be used along with the faces
    * @param out list of indices to populate
    * @param offset starting index into out array to modify
    * @return number of indices added to <code>out</code>
    */
   private static int getFaceLines(Face face, PolygonalMesh mesh, VertexIndexArray out, 
      int offset) {
     
      int[] indexOffs = mesh.getFeatureIndexOffsets ();
      
      int nadd = 0;
 
      int idx = face.getIndex ();
      int foff = indexOffs[idx];
      int numv = indexOffs[idx+1] - foff;

      // line loop
      for (int j=0; j<numv-1; ++j) {
         out.set (offset+nadd, foff+j);
         out.set (offset+nadd+1, foff+j+1);
         nadd += 2;
      }
      // close the loop
      out.set (offset+nadd, foff+numv-1);
      out.set (offset+nadd+1, foff);
      nadd += 2;
      
      return nadd;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return 2*size ();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      // faces and edges
      int size = size();
      if (qid > size) {
         qid = qid-size;
      }
      if (qid >= 0 && qid < size) {
         list.addLast (get (qid));
      }
   }
   
}
