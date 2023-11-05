package artisynth.core.fields; 

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;

import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.MeshFieldPoint;
import artisynth.core.util.ScanToken;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.util.DynamicBooleanArray;
import maspack.util.DynamicDoubleArray;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.*;
import maspack.properties.*;
import maspack.render.Renderer.*;
import maspack.render.*;
import maspack.matrix.*;

/**
 * A scalar field defined over a triangular polygonal mesh, using values set at
 * the mesh's faces. Values at other points are obtained by finding the faces
 * nearest to those points. Values at faces for which no explicit value has
 * been set are given by the field's <i>default value</i>. Since values
 * are assumed to be constant over a given face, this field is not continuous.
 */
public class ScalarFaceField extends ScalarMeshField {

   protected DynamicDoubleArray myValues;
   protected DynamicBooleanArray myValuesSet;
   
   public Range getVisualizationRange() {
      return new EnumRange<Visualization>(
         Visualization.class, new Visualization[] {
            Visualization.FACE,
            Visualization.POINT,
            Visualization.OFF
         });
   }

   protected void initValues () {
      myValues = new DynamicDoubleArray();
      myValuesSet = new DynamicBooleanArray();
      updateValueLists();
   }

   protected void updateValueLists() {
      int maxFaces = ((PolygonalMesh)myMeshComp.getMesh()).numFaces();
      myValues.resize (maxFaces);
      myValuesSet.resize (maxFaces);
   }

   void updateValueRange (DoubleInterval range) {
      for (int i=0; i<myValuesSet.size(); i++) {
         if (myValuesSet.get(i)) {
            range.updateBounds (myValues.get(i));
         }
      }
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public ScalarFaceField () {
   }

   /**
    * Constructs a field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param mcomp component containing the mesh associated with the field
    */
   public ScalarFaceField (MeshComponent mcomp) {
      super (mcomp, 0);
      initValues ();
   }

   /**
    * Constructs a field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for faces which don't have
    * explicitly set values
    */
   public ScalarFaceField (MeshComponent mcomp, double defaultValue) {
      super (mcomp, defaultValue);
      initValues ();
   }

   /**
    * Constructs a named field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param name name of the field
    * @param mcomp component containing the mesh associated with the field
    */
   public ScalarFaceField (String name, MeshComponent mcomp) {
      this (mcomp);
      setName (name);
   }

   /**
    * Constructs a named field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param name name of the field
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for faces which don't have
    * explicitly set values
    */
   public ScalarFaceField (
      String name, MeshComponent mcomp, double defaultValue) {
      this (mcomp, defaultValue);
      setName (name);
   }

   /**
    * Returns the value at the face specified by a given index. The default
    * value is returned if a value has not been explicitly set for that face.
    * 
    * @param fidx face index
    * @return value at the face
    */
   public double getValue (int fidx) {
      if (fidx >= myMesh.numFaces()) {
         throw new IllegalArgumentException (
            "face index "+fidx+" exceeds number of faces "+
            myMesh.numFaces());
      }
      else if (fidx < myValues.size() && myValuesSet.get(fidx)) {
         return myValues.get(fidx);
      }
      else {
         return myDefaultValue;
      }
   }

   /**
    * Returns the value at a face. The default value is returned if a value
    * has not been explicitly set for that face.
    *
    * @param face face for which the value is requested
    * @return value at the face
    */
   public double getValue (Face face) {
      checkFaceBelongsToMesh (face);
      return getValue (face.getIndex());
   }

   /**
    * {@inheritDoc}
    */
   public double getValue (Point3d pos) {
      return getValue (createFieldPoint (pos));
   }
   
   private double computeTriangularFaceArea (HalfEdge he) {
      Point3d p0 = he.getTail().getPosition();
      Point3d p1 = he.getHead().getPosition();
      Point3d p2 = he.getNext().getHead().getPosition();
      return Face.computeTriangleArea (p0, p1, p2);
   }

   /**
    * {@inheritDoc}
    */
   public double getValue (MeshFieldPoint fp) {
      Vertex3d[] vtxs = fp.getVertices();
      switch (vtxs.length) {
         case 1: {
            double value = 0;
            double wsum = 0;
            Iterator<HalfEdge> hedges = vtxs[0].getIncidentHalfEdges();
            while (hedges.hasNext()) {
               HalfEdge he = hedges.next();
               double w = computeTriangularFaceArea (he);
               value += w*getValue(he.getFace());
               wsum += w;
            }
            return value/wsum;
         }
         case 2: {
            // edge: average face values
            HalfEdge he = vtxs[1].findIncidentHalfEdge (vtxs[0]);
            if (he != null) {
               return (getValue(he.getFace())+getValue(he.getOppositeFace()))/2;
            }
            break;
         }
         case 3: {
            // single face
            HalfEdge he = vtxs[1].findIncidentHalfEdge (vtxs[0]);
            if (he != null) {
               return getValue(he.getFace());
            }
            // try the other way around, although it shouldn't happen
            he = vtxs[0].findIncidentHalfEdge (vtxs[1]);
            if (he != null) {
               return getValue(he.getFace());
            }
            break;
         }
      }
      return myDefaultValue;
   }

   /**
    * Sets the value at a face.
    * 
    * @param face face for which the value is to be set
    * @param value new value for the face
    */
   public void setValue (Face face, double value) {
      checkFaceBelongsToMesh (face);
      int fidx = face.getIndex();
      if (fidx >= myValues.size()) {
         updateValueLists();
      }
      if (fidx < myValues.size()) {
         myValues.set (fidx, value);
         myValuesSet.set (fidx, true);
      }
   }

   /**
    * Queries whether a value has been seen at a given face.
    * 
    * @param face face being queried
    * @return {@code true} if a value has been set at the face
    */
   public boolean isValueSet (Face face) {
      checkFaceBelongsToMesh (face);
      int fidx = face.getIndex();
      if (fidx < myValuesSet.size()) {
         return myValuesSet.get (fidx);
      }
      else {
         return false;
      }
   }

   /**
    * Clears the value at a given face. After this call, the face will be
    * associated with the default value.
    * 
    * @param face face whose value is to be cleared
    */
   public void clearValue (Face face) {
      checkFaceBelongsToMesh (face);
      int fidx = face.getIndex();
      if (fidx < myValuesSet.size()) {
         myValuesSet.set (fidx, false);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void clearAllValues() {
      for (int i=0; i<myValuesSet.size(); i++) {
         myValuesSet.set (i, false);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("values=");
      writeValues (pw, fmt, myValues, myValuesSet);
   }

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new DynamicDoubleArray();
         myValuesSet = new DynamicBooleanArray();
         scanValues (rtok, myValues, myValuesSet);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   /**
    * {@inheritDoc}
    */
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      updateValueLists();
   }

   // --- rendering interface ----

   protected RenderObject buildFaceRenderObject (DoubleInterval range) {
      PolygonalMesh mesh = (PolygonalMesh)getMesh();
      RenderObject rob = new RenderObject();
      rob.createTriangleGroup();
      ScalarFieldUtils.addColors (rob, myColorMap);
      // add the positions
      Point3d wpnt = new Point3d();
      int i = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.getWorldPoint (wpnt);
         rob.addPosition (wpnt);
      }

      // add faces to render object
      Vector3d nrm = new Vector3d();
      Point3d cent = new Point3d();
      Point3d wpos = new Point3d(); // widget position
      Point3d ppos = new Point3d(); // point position
      for (Face face : mesh.getFaces()) {
         int[] pidxs = face.getVertexIndices();
         int cidx = ScalarFieldUtils.getColorIndex (getValue(face), range);
         face.getWorldNormal (nrm);
         int nidx = rob.addNormal ((float)nrm.x, (float)nrm.y, (float)nrm.z);
         // add triangles. 
         for (i=1; i<pidxs.length-1; i++) {
            int v0idx = rob.addVertex (pidxs[0], nidx, cidx, -1);
            int v1idx = rob.addVertex (pidxs[i], nidx, cidx, -1);
            int v2idx = rob.addVertex (pidxs[i+1], nidx, cidx, -1);
            rob.addTriangle (v0idx, v1idx, v2idx);     
         }
      }
      return rob;
   }

   protected RenderObject buildPointRenderObject (DoubleInterval range) {
      PolygonalMesh mesh = (PolygonalMesh)getMesh();
      RenderObject rob = new RenderObject();
      rob.createPointGroup();
      float[] rgb = new float[3];
      for (int i=0; i<256; i++) {
         myColorMap.getRGB (i/255.0, rgb);
         rob.addColor (rgb);
      }
      Point3d pos = new Point3d();
      int vidx = 0;
      for (Face face : mesh.getFaces()) {
         face.computeWorldCentroid (pos);
         rob.addPosition (pos);
         int cidx = ScalarFieldUtils.getColorIndex (getValue(face), range);
         rob.addVertex (vidx, -1, cidx, -1);
         rob.addPoint (vidx);
         vidx++;
      }     
      return rob;
   }

//    public void prerender (RenderList list) {
//       switch (myVisualization) {
//          case FACE: {
//             DoubleInterval range = updateRenderRange();
//             myRenderObj = buildFaceRenderObject (range);
//             break;
//          }
//          case POINT: {
//             DoubleInterval range = updateRenderRange();
//             myRenderObj = buildPointRenderObject (range);
//             break;
//          }
//          default:{
//             myRenderObj = null;
//             break;
//          }
//       }
//    }
   
//    public void render (Renderer renderer, int flags) {
//       RenderObject robj = myRenderObj;
//       RenderProps props = myRenderProps;
      
//       if (robj != null) {
//          switch (myVisualization) {
//             case FACE: {
//                if (robj.numTriangles() > 0) {
//                   renderer.setShading (props.getShading());
//                   renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
//                   renderer.drawTriangles (robj, /*group=*/0);
//                }
//                break;
//             }
//             case POINT: {
//                RenderableUtils.drawPoints (
//                   renderer, robj, 0, props, isSelected());
//                break;
//             }
//          }
//       }
//    }


}
