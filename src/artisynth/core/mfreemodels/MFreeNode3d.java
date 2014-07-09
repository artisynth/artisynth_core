/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import maspack.geometry.BSPTree;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Boundable;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.FemNodeNeighbor;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointState;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;

public class MFreeNode3d extends FemNode3d implements MFreePoint3d, Boundable {
   
   public static boolean DEFAULT_RENDER_BOUNDARY = false;
   
   ArrayList<MFreeNode3d> myDependentNodes;
   LinkedList<MFreeElement3d> myElementDependencies;
   VectorNd myCoords;
   
   
   protected Vector3d[] tmpF;
   
   protected boolean renderBoundary = DEFAULT_RENDER_BOUNDARY;
   protected PolygonalMesh myBoundaryMesh = null;
   protected boolean renderMeshValid = false;

   // used for computing shape functions, embeds region of
   // influence
   private MFreeWeightFunction myWeightFunction;
   private MFreeShapeFunction myShapeFunction;
   
   PointState trueState = new PointState();
   
   // volumes for incompressibility
   protected double myRestVolume = 0;
   protected double myVolume = 0;
   protected double myPartitionVolume = 0;
   protected double myPartitionRestVolume = 0;
   
   public static PropertyList myProps =
      new PropertyList (MFreeNode3d.class, FemNode3d.class);

   static {
      myProps.addReadOnly (
         "falsePosition",
         "false position");
      myProps.addReadOnly (
         "falseDisplacement",
         "false displacement");
      myProps.addReadOnly("nodalPressure getPressure", "nodal pressure");
      myProps.add("renderBoundary", "Render boundary of influence region", DEFAULT_RENDER_BOUNDARY);
   }
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public MFreeNode3d () {
      this(0,0,0);
   }

   public MFreeNode3d (Point3d p) {
      this(p.x,p.y,p.z);
   }

   public MFreeNode3d (double x, double y, double z) {
      super(x,y,z);
      setRestPosition(new Point3d(x,y,z));
      setFalsePosition(myRest);
      myElementDependencies = new LinkedList<MFreeElement3d>();
      
   }
   
//   double[] pdisp = new double[3];
//   public int setTargetFalseDisplacement(double[] post, int idx) {
//      Point3d tpos = getTruePosition();
//      for (int i=0; i<getPosStateSize(); i++) {
//         pdisp[i] = post[i]-getTruePosition().x;
//      }
//      
//      return idx+getPosStateSize();
//   }
   
   public void setRestPosition (Point3d pos) {
      super.setRestPosition(pos);
   }
   
   public int setTargetPos(double[] post, int idx) {
      return super.setTargetPos(post, idx);
   }
   
   public int getTargetPos(double [] post, double s, double h, int idx) {
      return super.getTargetPos(post, s, h, idx);
   }
   
   public Point3d getTargetPosition() {
      return super.getTargetPosition();
   }
   
   public Vector3d getTargetDisplacement() {
      return super.getTargetDisplacement();
   }
   
   public int setTargetVel(double[] velt, int idx) {
      return super.setTargetVel(velt, idx);
   }
   
   public int getTargetVel(double velt[], double s, double h, int idx) {
      return super.getTargetVel(velt, s, h, idx);
   }
   
   public Vector3d getTargetVelocity() {
      return super.getTargetVelocity();
   }
   
   public void registerNodeNeighbor (MFreeNode3d nbrNode) {
      FemNodeNeighbor nbr = getNodeNeighbor (nbrNode);
      if (nbr == null) {
         nbr = new MFreeNodeNeighbor (nbrNode);
         myNodeNeighbors.add (nbr);
      }
      else {
         nbr.increaseRefCount();
      }
   }
   
   public void addElementDependency(MFreeElement3d e) {
      myElementDependencies.add(e);
   }
   
   public void removeElementDependency(MFreeElement3d e) {
      myElementDependencies.remove(e);
   }
   
   public LinkedList<MFreeElement3d> getMFreeElementDependencies() {
      return myElementDependencies;
   }
   
   @Override
   public int numAdjacentElements() {
      return myElementDependencies.size();
   }

   public void setWeightFunction(RadialWeightFunction f) {
      myWeightFunction = f;
   }

   public MFreeWeightFunction getWeightFunction() {
      return myWeightFunction;
   }
   
   public void setShapeFunction(MFreeShapeFunction f) {
      myShapeFunction = f;
   }
   
   public MFreeShapeFunction getShapeFunction() {
      return myShapeFunction;
   }

   public boolean isInDomain(Point3d pos, double tol) {
      
      if (myBoundaryMesh != null) {
         BVFeatureQuery query = new BVFeatureQuery();
         return query.isInsideOrientedMesh (myBoundaryMesh, pos, tol);
      } else if (myWeightFunction != null){
         return myWeightFunction.isInDomain(pos, tol);
      }
      return (myWeightFunction.eval(pos) > 0);
   }
   
   public boolean intersects(MFreeNode3d node) {
      
      MFreeWeightFunction m1 = getWeightFunction();
      MFreeWeightFunction m2 = node.getWeightFunction();
      
      if (m1 != null && m2 != null) {
         if (m1 instanceof RadialWeightFunction && m2 instanceof RadialWeightFunction) {
            return ((RadialWeightFunction)m1).intersects((RadialWeightFunction)m2);
         }
      }
      
      // fall back to using meshes
      PolygonalMesh mesh2 = node.getBoundaryMesh();
      PolygonalMesh mesh1 = getBoundaryMesh();
      if (mesh1 != null && mesh2 != null) {
         BSPTree mesh1Tree = new BSPTree(mesh1);
         BSPTree mesh2Tree = new BSPTree(mesh2);
         PolygonalMesh meshInt = mesh1Tree.intersect(mesh2Tree).generateMesh();
         if (meshInt.getNumFaces() > 0) {
            return true;
         }
         return false;
      }
      
      throw new RuntimeException("No idea how to determine if nodes intersect");
      
   }

   public double getWeight(Point3d pos) {
      return myWeightFunction.eval(pos);
   }

   public MFreeNode3d copy(
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      MFreeNode3d node = (MFreeNode3d)super.copy(flags, copyMap);
      node.myWeightFunction = myWeightFunction.clone();
      return node;
   }
   
   public void getFalseDisplacement(Vector3d disp) {
      disp.set(getFalsePosition());
      disp.sub(getRestPosition());
   }
   
   public Vector3d getFalseDisplacement() {
      Vector3d del = new Vector3d();
      del.sub (getFalsePosition(), myRest);
      return del;
   }
   
   public Vector3d getDisplacement() {
      Vector3d del = new Vector3d();
      del.sub (getTruePosition(), myRest);
      return del;
   }
   
   public Point3d getFalsePosition() {
      return super.getPosition();
   }
   
   public void setFalsePosition(Point3d pos) {
      super.setPosition(pos);
   }
    
   public Vector3d getFalseVelocity() {
      return super.getVelocity();
   }
   
   public void setFalseVelocity(Vector3d vel) {
      super.setVelocity(vel);
   }
   public ArrayList<MFreeNode3d> getDependentNodes() {
      return myDependentNodes;
   }

   public void setDependentNodes(List<MFreeNode3d> nodes, VectorNd coords) {
      myDependentNodes = new ArrayList<MFreeNode3d>();
      myDependentNodes.addAll(nodes);
      setNodeCoordinates(coords);
      
      
   }
   
   public boolean reduceDependencies(double tol) {
            
      boolean changed = false;
      for (int i=0; i<myDependentNodes.size(); i++) {
         if (Math.abs(myCoords.get(i)) <= tol) {
            changed = true;
            myCoords.set(i, 0);
         }
      }   
      myCoords.scale(1.0/myCoords.sum()); // re-sum to one
      return changed;
   }

   public VectorNd getNodeCoordinates() {
      return myCoords;
   }

   public void setNodeCoordinates(VectorNd coords) {
      myCoords = new VectorNd(coords);
      updatePosAndVelState();
   }
   
   public void updatePosAndVelState() {
      updatePosState();
      updateVelState();
   }

   public void updatePosState() {
      trueState.setPos(Point3d.ZERO);
      for (int i=0; i<myDependentNodes.size(); i++) {
         trueState.scaledAddPos(myCoords.get(i),  myDependentNodes.get(i).getFalsePosition());
      }
   }

   public void updateVelState() {
      trueState.setVel(Vector3d.ZERO);
      for (int i=0; i<myDependentNodes.size(); i++) {
         trueState.scaledAddVel(myCoords.get(i),  myDependentNodes.get(i).getFalseVelocity());
      }
   }

   public Point3d getTruePosition() {
      return trueState.getPos();
   }
   
   public Vector3d getTrueVelocity() {
      return trueState.getVel();
   }
   
   public boolean isRadial() {
      return (myWeightFunction instanceof RadialWeightFunction);
   }
   
   public double getInfluenceRadius() {
      if (isRadial()) {
         return ((RadialWeightFunction)myWeightFunction).getRadius();
      }
      return -1;
   }
   
   @Override 
   public Point3d getPosition() {
      return getFalsePosition();
   }
   
   @Override 
   public Vector3d getVelocity() {
      return getFalseVelocity();
   }
   
   @Override
   public double distance (Point pnt) {
      return pnt.distance(getTruePosition());
   }

   @Override
   public double distance (Point3d pos) {
      return pos.distance(getTruePosition());
   }
   
   @Override
   public void prerender (RenderList list) {
      super.prerender(list);
      Point3d pos = trueState.getPos();
      myRenderCoords[0] = (float)pos.x;
      myRenderCoords[1] = (float)pos.y;
      myRenderCoords[2] = (float)pos.z;
      renderMeshValid = false;
   }

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps(this);
   }
   
   @Override
   public void render(GLRenderer renderer, int flags) {
      RenderProps myProps = getRenderProps();
      if (myProps.isVisible()) {
         super.render(renderer, flags);
         renderWidget(renderer, myProps, 0);
      }
   }
   
   public void renderWidget(GLRenderer renderer, RenderProps props, int flags) {

      if (myBoundaryMesh != null && renderBoundary) {
         if (!renderMeshValid) {
            if (myNumber == 0) {
               System.out.println("rebuilding node 0");
            }
            for (Vertex3d vtx : myBoundaryMesh.getVertices()) {
               if (vtx instanceof MFreeVertex3d) {
                  ((MFreeVertex3d)vtx).updatePosAndVelState();
               }
            }
            renderMeshValid = true;
         }
         //renderer.drawMesh(props, myBoundaryMesh, 0); 
         myBoundaryMesh.render (renderer, props, 0);
      }
   }
   
   
   public void setBoundaryMesh(PolygonalMesh mesh) {
      myBoundaryMesh = mesh;
      if (!mesh.isClosed()) {
         System.out.println("mesh is not closed");
      }
   }
   
   public PolygonalMesh getBoundaryMesh() {
      return myBoundaryMesh;
   }
   
   @Override
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      // paranoid; do this in both connect and disconnect
      myNodeNeighbors.clear();
      clearIndirectNeighbors();
   }

   @Override
   public void disconnectFromHierarchy() {
      super.disconnectFromHierarchy();
      myNodeNeighbors.clear();
      clearIndirectNeighbors();
   }

   public void computeCentroid(Vector3d centroid) {
      centroid.set(getTruePosition());
   }

   public double computeCovariance (Matrix3d C) {
      Point3d pos = getTruePosition();
      C.outerProduct (pos, pos);
      return 1;
   }
   
   public void updateBoundaryMesh() {
      if (myBoundaryMesh != null) {
         for (Vertex3d vtx : myBoundaryMesh.getVertices()) {
            if (vtx instanceof MFreeVertex3d) {
               ((MFreeVertex3d)vtx).updatePosAndVelState();
            }
         }
      }
      
   }
   
   @Override 
   public void updateBounds(Point3d pmin, Point3d pmax) {
      
      if (myBoundaryMesh != null) {
         updateBoundaryMesh();
         myBoundaryMesh.updateBounds(pmin, pmax);
         return;
      } else {
         // assume at rest
         // XXX
         if (myWeightFunction != null) {
            myWeightFunction.updateBounds(pmin, pmax);
         } else {
            getTruePosition().updateBounds(pmin, pmax);
         }
      }
      
   }
      
   public boolean getRenderBoundary() {
      return renderBoundary;
   }
   
   public void setRenderBoundary(boolean render) {
      renderBoundary = render;
   }
   
   public void setRestVolume(double vol) {
      myRestVolume = vol;
   }
   
   public void setPartitionRestVolume(double vol) {
      myPartitionRestVolume = vol;
   }
   
   public void setVolume(double vol) {
      myVolume = vol;
   }
   
   public void setPartitionVolume(double vol) {
      myPartitionVolume = vol;
   }
   
   public double getRestVolume() {
      return myRestVolume;
   }
   
   public double getPartitionVolume(double vol) {
      return myPartitionVolume;
   }
   
   public double getPartitionRestVolume() {
      return myPartitionRestVolume;
   }
   
   public double getVolume() {
      return myVolume;
   }
   
   public void addRestVolume(double pvol) {
      myRestVolume += pvol;
   }
   
   public void addVolume(double pvol) {
      myVolume += pvol;
   }
   
   public void addPartitionVolume(double pvol) {
      myPartitionVolume += pvol;
   }
   
   public void addPartitionRestVolume(double pvol) {
      myPartitionRestVolume += pvol;
   }
   
   public double getPressure() {
      return myPressure;
   }
   
   SymmetricMatrix3d getAvgStressP() {
      return myAvgStress;
   }
   
   SymmetricMatrix3d getAvgStrainP() {
      return myAvgStrain;
   }
   
   void setAvgStressP(SymmetricMatrix3d sig) {
      myAvgStress = sig;
   }
   
   void setAvgStrainP(SymmetricMatrix3d eps) {
      myAvgStrain = eps;
   }
   
   // implementation of IndexedPointSet

   public int numPoints() {
      return 1;
   }

   public Point3d getPoint (int idx) {
      if (idx == 0) {
         return getPosition();
      }
      else {
         throw new ArrayIndexOutOfBoundsException ("idx=" + idx);
      }
   }
   
   
   
}
