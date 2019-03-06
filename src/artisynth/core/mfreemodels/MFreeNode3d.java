/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.ShellElement3d;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointState;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.TransformGeometryContext;
import maspack.geometry.Boundable;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;

public class MFreeNode3d extends FemNode3d implements MFreePoint3d, Boundable {
 
   // node-dependency
   FemNode3d[] myDependentNodes;
   VectorNd myCoords;
   
   protected Vector3d[] tmpF;
   
   // used for computing shape functions, embeds region of influence
   private MFreeWeightFunction myWeightFunction;
   
   // "real" position
   PointState trueState = new PointState();
   
   public static PropertyList myProps =
      new PropertyList (MFreeNode3d.class, FemNode3d.class);

   static {
      myProps.addReadOnly (
         "falsePosition",
         "false position");
      myProps.addReadOnly (
         "falseDisplacement",
         "false displacement");
      myProps.addReadOnly (
         "truePosition",
         "true position");
      myProps.addReadOnly (
         "trueDisplacement",
         "true displacement");
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
   }
   
   public void setWeightFunction(RadialWeightFunction f) {
      myWeightFunction = f;
   }

   public MFreeWeightFunction getWeightFunction() {
      return myWeightFunction;
   }
   
   public boolean isInRestDomain(Vector3d coords) {
      Point3d c = new Point3d(coords);
      return (myWeightFunction.isInDomain(c, 0));
   }

   public boolean isInDomain(Point3d pos, double tol) {
      if (myWeightFunction != null){
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
      
      throw new RuntimeException("I have no idea how to determine if nodes intersect");
      
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
   
   public Point3d getTrueDisplacement() {
      return myState.getPos();
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
   
   public FemNode3d[] getDependentNodes() {
      return myDependentNodes;
   }

   public void setDependentNodes(FemNode3d[] nodes, VectorNd coords) {
      myDependentNodes = Arrays.copyOf(nodes, nodes.length);
      setNodeCoordinates(coords);
   }
   
   public boolean reduceDependencies(double tol) {
            
      int ndeps = 0;
      boolean changed = false;
      for (int i=0; i<myDependentNodes.length; i++) {
         if (Math.abs(myCoords.get(i)) <= tol) {
            changed = true;
            myCoords.set(i, 0);
         } else {
            if (changed) {
               myDependentNodes[ndeps] = myDependentNodes[i];
               myCoords.set(ndeps, myCoords.get(i));
            }
            ++ndeps;
         }
      }   
      if (changed) {
         myDependentNodes = Arrays.copyOf(myDependentNodes, ndeps);
         myCoords.setSize(ndeps);
         myCoords.scale(1.0/myCoords.sum()); // re-sum to one   
      }
      
      return changed;
   }

   public VectorNd getNodeCoordinates() {
      return myCoords;
   }

   public void setNodeCoordinates(VectorNd coords) {
      myCoords = new VectorNd(coords);
      updateSlavePos();
   }
   
   public void updateSlavePos() {
      updatePosState();
      updateVelState();
   }

   public void updatePosState() {
      trueState.setPos(Point3d.ZERO);
      double[] buff = myCoords.getBuffer();
      for (int i=0; i<myDependentNodes.length; i++) {
         trueState.scaledAddPos(buff[i],  myDependentNodes[i].getPosition());
      }
   }

   public void updateVelState() {
      trueState.setVel(Vector3d.ZERO);
      double[] buff = myCoords.getBuffer();
      for (int i=0; i<myDependentNodes.length; i++) {
         trueState.scaledAddVel(buff[i],  myDependentNodes[i].getVelocity());
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
     
   }

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps(this);
   }
   
   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      if (hcomp == getParent()) {
         // paranoid; do this in both connect and disconnect
         myNodeNeighbors.clear();
         clearIndirectNeighbors();
      }
   }

   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      super.disconnectFromHierarchy(hcomp);
      if (hcomp == getParent()) {
         myNodeNeighbors.clear();
         clearIndirectNeighbors();
      }
   }

   public void computeCentroid(Vector3d centroid) {
      centroid.set(getTruePosition());
   }

   public double computeCovariance (Matrix3d C) {
      Point3d pos = getTruePosition();
      C.outerProduct (pos, pos);
      return 1;
   }
   
   @Override 
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      
      if (myWeightFunction != null) {
         myWeightFunction.updateBounds(pmin, pmax);
      } else {
         getTruePosition().updateBounds(pmin, pmax);
      }
      
   }
      
   @Override
   /**
    * Center only
    */
   public int numPoints() {
      return 1;
   }

   @Override
   public Point3d getPoint (int idx) {
      if (idx == 0) {
         return getPosition();
      }
      else {
         throw new ArrayIndexOutOfBoundsException("idx="+idx);
      }
   }
   
   @Override
   public void transformGeometry(
      GeometryTransformer gt, TransformGeometryContext context, int flags) {
      super.transformGeometry(gt, context, flags);
      
      updateSlavePos();
   }
   
   public double computeMassFromDensity() {
      double mass = 0;
      for (FemElement3dBase e : getElementDependencies()) {
         if (e instanceof FemElement3d) {
            FemElement3d ee = (FemElement3d)e;
            mass += ee.getRestVolume()*ee.getDensity()/ee.numNodes();
         }
         else if (e instanceof ShellElement3d) {
            // XXX TODO use area
         }
      }
      return mass;
   }

   
}
