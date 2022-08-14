package artisynth.core.fields;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.*;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;
import maspack.render.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

public abstract class MeshFieldComp
   extends ModelComponentBase implements FieldComponent {
   
   protected MeshComponent myMeshComp;
   protected PolygonalMesh myMesh;
   protected RenderProps myRenderProps;

   /**
    * Check to ensure a particular vertex belongs to this field's mesh.
    */
   protected void checkVertexBelongsToMesh (Vertex3d vtx) {
      if (vtx.getMesh() != myMesh) {
         throw new IllegalArgumentException (
            "Vertex does not belong to this field's mesh");
      }
   }

   /**
    * Check to ensure a particular face belongs to this field's mesh.
    */
   protected void checkFaceBelongsToMesh (Face face) {
      if (face.getMesh() != myMesh) {
         throw new IllegalArgumentException (
            "Face does not belong to this field's mesh");
      }
   }

   /**
    * Checks that a face belongs to this field's mesh
    */

   protected class MeshFieldPointImpl implements MeshFieldPoint {
      
      Point3d myPos;
      Vertex3d[] myVtxs;
      double[] myWgts;

      MeshFieldPointImpl (
         Point3d pos, Vertex3d[] vtxs, double[] wgts) {
         myPos = new Point3d (pos);
         myVtxs = vtxs;
         myWgts = wgts;
      }

      public Point3d getPosition() {
         return myPos;
      }

      public Vertex3d[] getVertices() {
         return myVtxs;
      }

      public int numVertices() {
         return myVtxs.length;
      }

      public double[] getWeights() {
         return myWgts;
      }
   }

   protected void setMeshComp (MeshComponent mcomp) {
      MeshBase mesh = mcomp.getMesh();
      if (!(mesh instanceof PolygonalMesh) ||
          !((PolygonalMesh)mesh).isTriangular() ||
          ((PolygonalMesh)mesh).numFaces() == 0) {
         throw new IllegalArgumentException (
            "Mesh used for MeshField must be a triangular polygonal mesh");
      }
      myMeshComp = mcomp;
      myMesh = (PolygonalMesh)mcomp.getMesh();
   }

   public MeshComponent getMeshComp() {
      return myMeshComp;
   }

   public MeshBase getMesh() {
      return myMeshComp.getMesh();
   }

   protected void writeValues (
      PrintWriter pw, NumberFormat fmt, DynamicDoubleArray values, 
      DynamicBooleanArray valuesSet)
      throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int num=0; num<values.size(); num++) {
         if (!valuesSet.get(num)) {
            pw.println ("null");
         }
         else {
            pw.println (fmt.format (values.get(num)));
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected void writeScalarValueArrays (
      PrintWriter pw, NumberFormat fmt,
      ArrayList<double[]> valueArrays, WritableTest writableTest)
      throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int num=0; num<valueArrays.size(); num++) {
         double[] varray = valueArrays.get(num);
         if (varray == null) {
            pw.println ("null");
         }
         else {
            pw.print ("[ ");
            for (int k=0; k<varray.length; k++) {
               pw.print (fmt.format(varray[k])+" ");
            }
            pw.println ("]");
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected void scanValues (
      ReaderTokenizer rtok,
      DynamicDoubleArray values, DynamicBooleanArray valuesSet)
      throws IOException {

      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            values.add (0);
            valuesSet.add (false);
         }
         else if (rtok.tokenIsNumber()) {
            values.add (rtok.nval);
            valuesSet.add (true);
         }
         else {
            throw new IOException ("Expecting number or 'null', got "+rtok);
         }
      }
   }

   protected void scanScalarValueArrays (
      ReaderTokenizer rtok, ArrayList<double[]> valueArrays)
      throws IOException {

      ArrayList<Double> values = new ArrayList<>();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            valueArrays.add (null);
         }
         else {
            if (rtok.ttype != '[') {
               throw new IOException ("Expecting token '[', got "+rtok);
            }
            values.clear();
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               values.add (rtok.scanNumber());
            }
            valueArrays.add (ArraySupport.toDoubleArray(values));
         }
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "mesh", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println (
         "mesh=" + ComponentUtils.getWritePathName (ancestor, myMeshComp));
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "mesh")) {
         MeshComponent mcomp = 
            ScanWriteUtils.postscanReference (
               tokens, MeshComponent.class, ancestor);
         setMeshComp (mcomp);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected interface ReferenceTest {
      boolean isReferenced (int num);
   }

   protected interface WritableTest {
      boolean isWritable (int num);
   }

   protected static class NumDoublePair {
      int myNum;
      double myValue;

      NumDoublePair (int num, double value) {
         myNum = num;
         myValue = value;
      }
   }
   protected static class NumValuePair<T> {
      int myNum;
      T myValue;

      NumValuePair (int num, T value) {
         myNum = num;
         myValue = value;
      }
   }
 
   /**
    * {@inheritDoc}
    */
   public void clearCacheIfNecessary() {
   }

   protected void updateValueLists() {
   }

   protected <T> void resizeArrayList (
      ArrayList<T> list, int newsize) {
      while (list.size() < newsize) {
         list.add (null);
      }
      while (list.size() > newsize) {
         list.remove (list.size()-1);
      }
   }

   /**
    * Create a MeshFieldPoint corresponding to the nearest point on this
    * component's mesh to a specified position.
    */
   public MeshFieldPoint createFieldPoint (Point3d pnt) {
      MeshBase mesh = myMeshComp.getMesh();
      BVFeatureQuery query = new BVFeatureQuery();
      Point3d near = new Point3d();
      // assume here that the mesh is a triangular polygonal mesh with number
      // of faces > 0
      Vector2d uv = new Vector2d();
      Face face = query.nearestFaceToPoint (
         near, uv, (PolygonalMesh)mesh, pnt);
      Vertex3d[] vtxs = face.getVertices();
      double[] wgts = new double[] { 1-uv.x-uv.y, uv.x, uv.y };
      return new MeshFieldPointImpl (near, vtxs, wgts);
   }

   public void getHardReferences (List<ModelComponent> refs) {
      refs.add (myMeshComp);
   }

   /* --- Begin partial implemetation of Renderable --- */

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps = RenderableComponentBase.updateRenderProps (
         this, myRenderProps, props);
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= IsRenderable.TRANSPARENT;
      }
      return code;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   /* --- End partial implemetation of Renderable --- */

}
