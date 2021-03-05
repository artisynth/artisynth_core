package artisynth.demos.test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.geometry.GeometryTransformer.UndoState;

import artisynth.core.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.femmodels.*;
import artisynth.core.driver.*;

import artisynth.demos.mech.*;
import artisynth.demos.fem.*;

public class TransformGeometryTest {

   boolean debug = false;

   ArrayList<AffineTransform3dBase> myRandomTransforms;

   public TransformGeometryTest() {

      myRandomTransforms = new ArrayList<AffineTransform3dBase>();
      myRandomTransforms.add (new RigidTransform3d());
      myRandomTransforms.add (new AffineTransform3d());
      for (AffineTransform3dBase X : myRandomTransforms) {
         X.setRandom();
      }
   }

   private static class CompAttr {
      ModelComponent myComp;
      String myAttr;

      CompAttr (ModelComponent c, String a) {
         myComp = c;
         myAttr = a;
      }

      CompAttr (CompAttr ca) {
         myComp = ca.myComp;
         myAttr = ca.myAttr;
      }

      public boolean equals (Object obj) {
         if (obj instanceof CompAttr) {
            CompAttr ca = (CompAttr)obj;
            return myComp == ca.myComp && myAttr.equals (ca.myAttr);
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return (myComp.hashCode() + myAttr.hashCode())/2;
      }

      public String toString() {
         return ComponentUtils.getPathName(myComp)+":"+myAttr;
      }
   }

   private static class AttributeSet {
      HashMap<CompAttr,Object> myMap = new LinkedHashMap<CompAttr,Object>();

      public void set (ModelComponent comp, String attr, Object obj) {
         set (new CompAttr (comp, attr), obj);
      }

      public void set (CompAttr ca, Object obj) {
         myMap.put (ca, copy (obj));
      }

      public Object get (ModelComponent comp, String attr) {
         return myMap.get (new CompAttr (comp, attr));
      }

      public void checkEquals (AttributeSet set, double eps) {
         
         if (myMap.size() != set.myMap.size()) {
            throw new TestException (
               "Attribute sets have unequal numbers of entries: " +
               myMap.size() + " vs. " + set.myMap.size());
         }
         for (Map.Entry<CompAttr,Object> e : myMap.entrySet()) {
            Object obj = set.myMap.get (e.getKey());
            StringHolder errMsg = new StringHolder();
            if (!equals (e.getValue(), obj, errMsg, eps)) {
               String msg = e.getKey()+": ";
               if (errMsg.value != null) {
                  msg += errMsg.value;
               }
               else if (obj instanceof Matrix) {
                  msg += " Expecting\n"+obj+"\ngot\n" + e.getValue();
               }
               else {
                  msg += " Expecting "+obj+"; got " + e.getValue();
               }
               throw new TestException (msg);
            }
         }
      }

      public void print() {
         for (Map.Entry<CompAttr,Object> e : myMap.entrySet()) {
            Object obj = e.getValue();
            System.out.print (" " + e.getKey() + ": ");
            if (obj instanceof Matrix) {
               System.out.println ("\n" + obj);
            }
            else {
               System.out.println (obj);
            }
         }
      }

      public Set<CompAttr> getAttributeKeys() {
         return myMap.keySet();
      }

      public AttributeSet copy() {
         AttributeSet newSet = new AttributeSet();
         for (Map.Entry<CompAttr,Object> e : myMap.entrySet()) {
            newSet.set (new CompAttr (e.getKey()), e.getValue());
         }
         return newSet;
      }

      public static Object copy (Object obj) {

         if (obj == null) {
            return null;
         }
         else if (obj instanceof Point3d) {
            return new Point3d ((Point3d)obj);
         }
         else if (obj instanceof Vector3d) {
            return new Vector3d ((Vector3d)obj);
         }
         else if (obj instanceof Matrix3d) {
            return new Matrix3d ((Matrix3d)obj);
         }
         else if (obj instanceof RigidTransform3d) {
            return new RigidTransform3d ((RigidTransform3d)obj);
         }
         else if (obj instanceof AffineTransform3d) {
            return new AffineTransform3d ((AffineTransform3d)obj);
         }
         else if (obj instanceof Vector) {
            return new VectorNd ((Vector)obj);
         }
         else if (obj instanceof Matrix) {
            return new MatrixNd ((Matrix)obj);
         }
         else if (obj instanceof Boolean) {
            return new Boolean ((Boolean)obj);
         }
         else if (obj instanceof Integer) {
            return new Integer ((Integer)obj);
         }
         else if (obj instanceof Double) {
            return new Double ((Double)obj);
         }
         else if (obj instanceof MeshBase) {
            return ((MeshBase)obj).copy();
         }
         else if (obj instanceof Vector[]) {
            Vector[] vecArray = (Vector[])obj;
            VectorNd[] newArray = new VectorNd[vecArray.length];
            for (int i=0; i<vecArray.length; i++) {
               newArray[i] = new VectorNd(vecArray[i]);
            }
            return newArray;
         }
         else {
            throw new UnsupportedOperationException (
               "Unsupported attribute type: " + obj.getClass());
         }
      }         

      private static boolean equals (
         Object obj1, Object obj2, StringHolder errMsg, double eps) {

         if ((obj1 == null) != (obj2 == null)) {
            return false;
         }
         else if (obj1 == null && obj2 == null) {
            return true;
         }
         else if (obj1.getClass().isAssignableFrom(obj2.getClass()) &&
                  obj2.getClass().isAssignableFrom(obj1.getClass())) {
            if (obj1 instanceof Matrix) {
               Matrix M1 = (Matrix)obj1;
               Matrix M2 = (Matrix)obj2;
               return (M1.epsilonEquals (M2, eps));
            }
            else if (obj1 instanceof Vector) {
               Vector v1 = (Vector)obj1;
               Vector v2 = (Vector)obj2;
               return (v1.epsilonEquals (v2, eps));
            }
            else if (obj1 instanceof Boolean) {
               Boolean b1 = (Boolean)obj1;
               Boolean b2 = (Boolean)obj2;
               return (b1.equals (b2));
            }
            else if (obj1 instanceof Double) {
               Double n1 = (Double)obj1;
               Double n2 = (Double)obj2;
               return (Math.abs (n1.doubleValue()-n2.doubleValue()) <= eps);
            }
            else if (obj1 instanceof Integer) {
               Integer n1 = (Integer)obj1;
               Integer n2 = (Integer)obj2;
               return (n1.intValue() == n2.intValue());
            }
            else if (obj1 instanceof MeshBase) {
               MeshBase m1 = (MeshBase)obj1;
               MeshBase m2 = (MeshBase)obj2;
               // just compare the vertices
               if (m1.numVertices() != m2.numVertices()) {
                  errMsg.value =
                     "Expected " + m2.numVertices() +
                     "vertices; got " + m1.numVertices();
                  return false;
               }
               else {
                  for (int i=0; i<m1.numVertices(); i++) { 
                     Vertex3d v1 = m1.getVertex(i);
                     Vertex3d v2 = m2.getVertex(i);
                     if (!v1.pnt.epsilonEquals (v2.pnt, eps)) {
                        errMsg.value =
                           "Vertex "+i+": expected "+v2.pnt + "; got "+v1.pnt;
                        return false;
                     }
                  }
               }
               return true;
            }
            else if (obj1 instanceof Vector3d[]) {         
               Vector3d[] va1 = (Vector3d[])obj1;
               Vector3d[] va2 = (Vector3d[])obj2;
               if (va1.length != va2.length) {
                  errMsg.value =
                     "Expected "+ va2.length + " vectors; got " + va1.length;
                  return false;
               }
               else {
                  for (int i=0; i<va1.length; i++) {
                     if (!va1[i].epsilonEquals (va2[1], eps)) {
                        errMsg.value =
                           "Vector "+i+": expected "+va2[i]+ "; got " + va1[i];
                        return false;
                     }
                  }
               }
               return true;
            }
            else {
               throw new UnsupportedOperationException (
                  "Unsupported object type: " + obj1.getClass());
            }
         }
         else {
            throw new InternalErrorException (
               "objects have differing types: " +
               obj1.getClass()+", "+obj2.getClass());
         }
      }         
   }

   private Vector3d[] copy (Vector3d[] vecArray) {
      Vector3d[] newArray = null;
      if (vecArray != null) {
         newArray = new Vector3d[vecArray.length];
         for (int i=0; i<vecArray.length; i++) {
            newArray[i] = new Vector3d(vecArray[i]);
         }
      }
      return newArray;
   }


   public void getAttributes (ModelComponent c, AttributeSet attrs) {
      if (c instanceof Point) {
         Point p = (Point)c;
         attrs.set (p, "position", p.getPosition());
      }
      if (c instanceof Frame) {
         Frame f = (Frame)c;
         attrs.set (f, "pose", f.getPose());
      }
      if (c instanceof FemNode3d) {
         FemNode3d n = (FemNode3d)c;
         attrs.set (n, "restPos", n.getRestPosition());
      }
      if (c instanceof RigidCylinder) {
         RigidCylinder r = (RigidCylinder)c;
         attrs.set (r, "radius", r.getRadius());
      }
      if (c instanceof RigidSphere) {
         RigidSphere r = (RigidSphere)c;
         attrs.set (r, "radius", r.getRadius());
      }
      if (c instanceof RigidEllipsoid) {
         RigidEllipsoid r = (RigidEllipsoid)c;
         attrs.set (r, "axisLengths", r.getSemiAxisLengths());
      }
      if (c instanceof RigidTorus) {
         RigidTorus r = (RigidTorus)c;
         attrs.set (r, "innerRadius", r.getInnerRadius());
         attrs.set (r, "outerRadius", r.getOuterRadius());
      }
      if (c instanceof MeshComponent) {
         // this will capture the mesh info for rigid bodies as well
         MeshComponent m = (MeshComponent)c;
         attrs.set (m, "fileTransform", m.getFileTransform());
         attrs.set (m, "fileTransformRigid", m.isFileTransformRigid());
         attrs.set (m, "meshModified", m.isMeshModified());
         attrs.set (m, "mesh", m.getMesh());
      }
      if (c instanceof ParticlePlaneConstraint) {
         ParticlePlaneConstraint p = (ParticlePlaneConstraint)c;
         attrs.set (p, "normal", p.getNormal());
         attrs.set (p, "center", p.getCenter());
      }
      if (c instanceof PointForce) {
         PointForce p = (PointForce)c;
         attrs.set (c, "pose", p.getPose());
      }
      if (c instanceof BodyConnector) {
         BodyConnector b = (BodyConnector)c;
         attrs.set (b, "TCW", b.getCurrentTCW());
         attrs.set (b, "TDW", b.getCurrentTDW());
      }
      if (c instanceof PlanarConnector) {
         PlanarConnector p = (PlanarConnector)c;
         attrs.set (p, "normal", p.getNormal());
      }
      if (c instanceof MuscleElementDesc) {
         MuscleElementDesc m = (MuscleElementDesc)c;
         attrs.set (m, "dir", m.getDirection());
         attrs.set (m, "dirs", m.getDirections());
      }
      if (c instanceof FixedMeshBody) {
         FixedMeshBody f = (FixedMeshBody)c;
         attrs.set (f, "pose", f.getPose());
      }
   }   

   private void recursivelyGetAttributes (
      ModelComponent comp, AttributeSet set) {

      getAttributes (comp, set);
      if (comp instanceof CompositeComponent) {
         CompositeComponent ccomp = (CompositeComponent)comp;
         for (int i=0; i<ccomp.numComponents(); i++) {
            recursivelyGetAttributes (ccomp.get(i), set);
         }
      }
   }

   public AttributeSet getCurrentAttributes (MechSystemBase mech) {
      AttributeSet set = new AttributeSet();
      recursivelyGetAttributes (mech, set);
      return set;
   }

   public void checkCurrentAttributes (
      MechSystemBase mech, AttributeSet check, double eps) {
      AttributeSet current = getCurrentAttributes (mech);
      current.checkEquals (check, eps);
   }      

   GeometryTransformer transform (
      AffineTransform3dBase X, TransformableGeometry... tg) {

      GeometryTransformer gtr = GeometryTransformer.create (X);
      gtr.setUndoState (UndoState.SAVING);
      TransformGeometryContext.transform (tg, gtr, 0);
      return gtr;
   }

   void undo (GeometryTransformer gtr, TransformableGeometry... tg) {

      gtr.setUndoState (UndoState.RESTORING);
      TransformGeometryContext.transform (tg, gtr, 0);
   }

   void setPointChecks (
      AffineTransform3dBase X, AttributeSet check, Point... pnts) {

      Point3d pos = new Point3d();
      for (Point p : pnts) {
         pos.transform (X, p.getPosition());
         check.set (p, "position", pos);
      }
   }

   void setAttachedPointCheck (
      AttributeSet check, Point... pnts) {

      Point3d pos = new Point3d();
      for (Point p : pnts) {
         PointAttachment pa = (PointAttachment)p.getAttachment();
         pa.getCurrentPos (pos);
         check.set (p, "position", pos);
      }
   }

   void setFemNodeChecks (
      AffineTransform3dBase X, AttributeSet check, FemNode3d... nodes) {

      Point3d pos = new Point3d();
      for (FemNode3d n : nodes) {
         pos.transform (X, n.getPosition());
         check.set (n, "position", pos);
         pos.transform (X, n.getRestPosition());
         check.set (n, "restPos", pos);
      }
   }

   private FemNode3d[] getNodeArray (FemModel3d fem) {
      FemNode3d[] nodes = new FemNode3d[fem.numNodes()];
      for (int i=0; i<nodes.length; i++) {
         nodes[i] = fem.getNodes().get(i);
      }
      return nodes;
   }      

   void setFemChecks (
      AffineTransform3dBase X, AttributeSet check, FemModel3d... fems) {

      for (FemModel3d fem : fems) {
         FemNode3d[] nodes = getNodeArray (fem);
         setFemNodeChecks (X, check, nodes);
         setFemMeshCheck (X, check, fem.getSurfaceMeshComp());
      }
   }

   private AffineTransform3dBase createScalingTransform (
      Vector3d scale, AffineTransform3dBase X, RigidTransform3d TBW) {

      AffineTransform3dBase Xmod = X;
      scale.set (1, 1, 1);
      if (X instanceof AffineTransform3d) {
         RigidTransform3d TBWnew = new RigidTransform3d(TBW);
         TBWnew.mulAffineLeft (X, (Matrix3d)null);
         AffineTransform3d Xnew = new AffineTransform3d(X);
         Xnew.mul (X, TBW);
         Xnew.mulInverseLeft (TBWnew, Xnew);

         double s = Math.pow(Math.abs(Xnew.A.determinant()), 1/3.0);
         scale.set (s, s, s);

         Xnew.A.setDiagonal (scale);
         Xnew.mulInverseRight (Xnew, TBW);
         Xnew.mul (TBWnew, Xnew);
         Xmod = Xnew;
      }
      return Xmod;
   }

   private AffineTransform3dBase createXYScalingTransform (
      Vector3d scale, AffineTransform3dBase X, RigidTransform3d TBW) {

      AffineTransform3dBase Xmod = X;
      scale.set (1, 1, 1);
      if (X instanceof AffineTransform3d) {
         RigidTransform3d TBWnew = new RigidTransform3d(TBW);
         TBWnew.mulAffineLeft (X, (Matrix3d)null);
         AffineTransform3d Xnew = new AffineTransform3d(X);
         Xnew.mul (X, TBW);
         Xnew.mulInverseLeft (TBWnew, Xnew);

         Matrix3d A = Xnew.A;
         double sxy = Math.sqrt (Math.abs(A.m00*A.m11 - A.m01*A.m10));
         scale.set (sxy, sxy, A.m22);
         
         Xnew.A.setDiagonal (scale);
         Xnew.mulInverseRight (Xnew, TBW);
         Xnew.mul (TBWnew, Xnew);
         Xmod = Xnew;
      }
      return Xmod;
   }

   private AffineTransform3dBase createXYZScalingTransform (
      Vector3d scale, AffineTransform3dBase X, RigidTransform3d TBW) {

      AffineTransform3dBase Xmod = X.copy();
      scale.set (1, 1, 1);
      if (X instanceof AffineTransform3d) {
         RigidTransform3d TBWnew = new RigidTransform3d(TBW);
         TBWnew.mulAffineLeft (X, (Matrix3d)null);
         AffineTransform3d Xnew = new AffineTransform3d(X);
         Xnew.mul (X, TBW);
         Xnew.mulInverseLeft (TBWnew, Xnew);

         Matrix3d A = Xnew.A;
         scale.set (A.m00, A.m11, A.m22);
 
         Xnew.A.setDiagonal (scale);
         Xnew.mulInverseRight (Xnew, TBW);
         Xnew.mul (TBWnew, Xnew);
         Xmod = Xnew;
      }
      return Xmod;
   }

   void setRigidSphereChecks (
      AffineTransform3dBase X, AttributeSet check, RigidSphere sphere) {

      Vector3d scale = new Vector3d();
      AffineTransform3dBase Xmod =
         createScalingTransform (scale, X, sphere.getPose());

      check.set (sphere, "radius", scale.x*sphere.getRadius());
      setRigidBodyChecks (X, check, sphere);
      setRigidBodyMeshChecks (Xmod, check, sphere);
   }

   void setRigidTorusChecks (
      AffineTransform3dBase X, AttributeSet check, RigidTorus torus) {

      Vector3d scale = new Vector3d();
      AffineTransform3dBase Xmod =
         createScalingTransform (scale, X, torus.getPose());

      check.set (torus, "innerRadius", scale.x*torus.getInnerRadius());
      check.set (torus, "outerRadius", scale.x*torus.getOuterRadius());
      setRigidBodyChecks (X, check, torus);
      setRigidBodyMeshChecks (Xmod, check, torus);
   }

   void setRigidCylinderChecks (
      AffineTransform3dBase X, AttributeSet check, RigidCylinder cylinder) {

      Vector3d scale = new Vector3d();
      AffineTransform3dBase Xmod =
         createXYScalingTransform (scale, X, cylinder.getPose());

      check.set (cylinder, "radius", scale.x*cylinder.getRadius());
      setRigidBodyChecks (X, check, cylinder);
      setRigidBodyMeshChecks (Xmod, check, cylinder);
   }

   void setRigidEllipsoidChecks (
      AffineTransform3dBase X, AttributeSet check, RigidEllipsoid ellipsoid) {

      Vector3d scale = new Vector3d();
      AffineTransform3dBase Xmod =
         createXYZScalingTransform (scale, X, ellipsoid.getPose());

      Vector3d axisLengths = new Vector3d (ellipsoid.getSemiAxisLengths());
      axisLengths.x *= scale.x;
      axisLengths.y *= scale.y;
      axisLengths.z *= scale.z;
      check.set (ellipsoid, "axisLengths", axisLengths);
      setRigidBodyChecks (X, check, ellipsoid);
      setRigidBodyMeshChecks (Xmod, check, ellipsoid);
   }

   final int SET_TCW = 0x01;
   final int SET_TDW = 0x02;

   void transformPose (RigidTransform3d TPW, AffineTransform3dBase X) {
      if (X instanceof RigidTransform3d) {
         RigidTransform3d T = (RigidTransform3d)X;
         TPW.mul (T, TPW);
      }
      else {
         AffineTransform3d A = (AffineTransform3d)X;
         TPW.mulAffineLeft (A, (Matrix3d)null);
      }
   }

   void setJointChecks (
      AffineTransform3dBase X, AttributeSet check, BodyConnector joint) {
      
      setJointChecks (X, check, joint, SET_TCW|SET_TDW);
   }

   void setJointChecks (
      AffineTransform3dBase X,
      AttributeSet check, BodyConnector joint, int flags) {

      if ((flags & SET_TCW) != 0) {
         RigidTransform3d TCW = new RigidTransform3d();
         joint.getCurrentTCW (TCW);
         transformPose (TCW, X);
         check.set (joint, "TCW", TCW);
      }
      if ((flags & SET_TDW) != 0) {
         RigidTransform3d TDW = new RigidTransform3d();
         joint.getCurrentTDW (TDW);
         transformPose (TDW, X);
         check.set (joint, "TDW", TDW);

         if (joint instanceof PlanarConnector) {
            PlanarConnector pc = (PlanarConnector)joint;
            Vector3d nrm = new Vector3d (pc.getNormal());

            Matrix3d A = new Matrix3d();
            joint.getCurrentTDW (TDW);
            RigidTransform3d TDWnew = new RigidTransform3d(TDW);
            TDWnew.mulAffineLeft (X, A);

            if (!A.equals (Matrix3d.IDENTITY)) {
               nrm.transform (TDWnew.R);
               A.mulInverseTranspose (nrm, nrm);
               nrm.normalize();
               nrm.inverseTransform (TDWnew.R);
            }
            check.set (joint, "normal", nrm);         
         }
      }
   }

   void setPoseCheck (
      AffineTransform3dBase X, AttributeSet check, ModelComponent comp,
      RigidTransform3d pose) {

      RigidTransform3d TFW = new RigidTransform3d(pose);
      transformPose (TFW, X);
      check.set (comp, "pose", TFW);
   }

   void setFrameChecks (
      AffineTransform3dBase X, AttributeSet check, Frame frame) {

      setPoseCheck (X, check, frame, frame.getPose());
   }

   void setAllMeshChecks (
      AffineTransform3dBase X, AttributeSet check,
      ModelComponent comp, MeshBase mesh,
      RigidTransform3d TBW, AffineTransform3d XF) {

      AffineTransform3dBase XLocal =
         setMeshCheck (X, check, comp, mesh, TBW);

      if (XLocal instanceof AffineTransform3d) {
         AffineTransform3d XL = (AffineTransform3d)XLocal;
         if (!XL.equals (AffineTransform3d.IDENTITY)) {
            check.set (comp, "fileTransformRigid", false);
         }
      }
      if (XF != null) {
         AffineTransform3d XFNew = new AffineTransform3d();
         XFNew.mul (XLocal, XF);
         check.set (comp, "fileTransform", XFNew);
      }
   }

   AffineTransform3dBase setMeshCheck (
      AffineTransform3dBase X, AttributeSet check,
      ModelComponent comp, MeshBase mesh, RigidTransform3d TBW) {

      MeshBase newMesh = mesh.copy();
      AffineTransform3dBase XLocal = X;      
      if (TBW != null) {
         if (X instanceof AffineTransform3d) {
            AffineTransform3d XL = new AffineTransform3d ();
            RigidTransform3d TBWnew = new RigidTransform3d (TBW);
            TBWnew.mulAffineLeft (X, XL.A);
            XL.A.mul (TBWnew.R);
            XL.A.mulTransposeLeft (TBWnew.R, XL.A);
            XLocal = XL;
         }
         else {
            return new AffineTransform3d();
         }
      }
      newMesh.transform (XLocal);
      check.set (comp, "mesh", newMesh);
      return XLocal;
   }

   void setFemMeshCheck (
      AffineTransform3dBase X, AttributeSet check, FemMeshComp comp) {

      setMeshCheck (
         X, check, comp, comp.getMesh(), null);
   }

   void setPartialFemMeshCheck (
      AffineTransform3dBase X, AttributeSet check, FemMeshComp comp,
      FemNode3d... nodes) {

      MeshBase newMesh = comp.getMesh().copy();
      for (FemNode3d n : nodes) {
         Vertex3d vtx = comp.getVertexForNode (n);
         if (vtx != null) {
            Vertex3d vnew = newMesh.getVertex (vtx.getIndex());
            vnew.pnt.transform (X);
         }
      }
      check.set (comp, "mesh", newMesh);
   }

   void setRigidBodyMeshChecks (
      AffineTransform3dBase X, AttributeSet check, RigidBody rb) {

      for (RigidMeshComp mcomp : rb.getMeshComps()) {
         MeshBase mesh = mcomp.getMesh();
         if (mesh != null) {
            setAllMeshChecks (
               X, check, mcomp, mesh, rb.getPose(), mcomp.getFileTransform());
         }
      }

      // PolygonalMesh mesh = rb.getSurfaceMesh();
      // if (mesh != null) {
      //    MeshComponent mcomp = rb.getSurfaceMeshComp();
      //    setAllMeshChecks (
      //       X, check, rb, mesh, rb.getPose(), mcomp.getFileTransform());
      // }
   }

   void setSkinMeshBodyChecks (
      AffineTransform3dBase X, AttributeSet check, SkinMeshBody sb) {

      MeshBase mesh = sb.getMesh();
      if (mesh != null) {
         setMeshCheck (X, check, sb, mesh, null);
      }
   }

   void setRigidBodyChecks (
      AffineTransform3dBase X, AttributeSet check, RigidBody rb) {

      setFrameChecks (X, check, rb);
      setRigidBodyMeshChecks (X, check, rb);
   }

   void setFixedMeshBodyChecks (
      AffineTransform3dBase X, AttributeSet check, FixedMeshBody body) {

      setPoseCheck (X, check, body, body.getPose());

      RigidTransform3d TFW = new RigidTransform3d(body.getPose());
      transformPose (TFW, X);

      MeshBase mesh = body.getMesh();
      if (mesh != null) {
         setAllMeshChecks (
            X, check, body, mesh, body.getPose(), body.getFileTransform());
      }
   }

   void checkTransformAndUndo (
      AffineTransform3dBase X, AttributeSet check,
      MechModel mech, TransformableGeometry... comps) {
      
      checkTransformAndUndo (X, check, mech, 1e-10, comps);
   }

   void checkTransformAndUndo (
      AffineTransform3dBase X, AttributeSet check,
      MechModel mech, double eps, TransformableGeometry... comps) {

      AttributeSet original = getCurrentAttributes (mech);
      GeometryTransformer transformer = transform (X, comps);
      checkCurrentAttributes (mech, check, eps);
      undo (transformer, comps);
      checkCurrentAttributes (mech, original, eps);
   }

   GeometryTransformer checkTransform (
      AffineTransform3dBase X, AttributeSet check,
      MechModel mech, TransformableGeometry... comps) {

      GeometryTransformer transformer = transform (X, comps);
      checkCurrentAttributes (mech, check, 1e-10);
      return transformer;
   }

   void checkUndo (
      GeometryTransformer transformer, AttributeSet check,
      MechModel mech, TransformableGeometry... comps) {

      undo (transformer, comps);
      checkCurrentAttributes (mech, check, 1e-10);
   }

   public void testMechModelDemo() {

      MechModelDemo demo = new MechModelDemo();

      demo.build (new String[0]);
      
      MechModel mech = demo.myMech;
      AttributeSet original = getCurrentAttributes (mech);
      AttributeSet check = getCurrentAttributes (mech);

      HingeJoint joint =
         (HingeJoint)mech.findComponent ("bodyConnectors/joint2");
      PlanarConnector contact1 =
         (PlanarConnector)mech.findComponent ("bodyConnectors/contact1");
      PlanarConnector contact2 =
         (PlanarConnector)mech.findComponent ("bodyConnectors/contact2");
      RigidBody link1 = 
         (RigidBody)mech.findComponent ("rigidBodies/link1");
      RigidBody link2 = 
         (RigidBody)mech.findComponent ("rigidBodies/link2");
      RigidBody base = 
         (RigidBody)mech.findComponent ("rigidBodies/base");
      FrameMarker mkr0 =
         (FrameMarker)mech.findComponent ("frameMarkers/0");
      FrameMarker mkr1 =
         (FrameMarker)mech.findComponent ("frameMarkers/1");
      FrameMarker mkr2 =
         (FrameMarker)mech.findComponent ("frameMarkers/2");
      FrameMarker mkr3 =
         (FrameMarker)mech.findComponent ("frameMarkers/3");

      RigidTransform3d T = new RigidTransform3d(-3, 0, 0);

      for (AffineTransform3dBase X : myRandomTransforms) {

         // move joint1

         check = original.copy();
         setJointChecks (X, check, joint);

         checkTransformAndUndo (X, check, mech, joint);

         // move link1

         check = original.copy();

         setRigidBodyChecks (X, check, link1);
         setPointChecks (X, check, mkr2, mkr3);

         checkTransformAndUndo (X, check, mech, link1);

         // move joint and link1

         check = original.copy();

         setJointChecks (X, check, joint);
         setRigidBodyChecks (X, check, link1);
         setPointChecks (X, check, mkr2, mkr3);

         checkTransformAndUndo (X, check, mech, joint, link1);

         // move link2
         check = original.copy();

         setRigidBodyChecks (X, check, link2);
         setJointChecks (X, check, contact1, SET_TCW);
         setJointChecks (X, check, contact2, SET_TCW);
         checkTransformAndUndo (X, check, mech, link2);

         // move joint and link2
         check = original.copy();

         setRigidBodyChecks (X, check, link2);
         setJointChecks (X, check, joint);
         setJointChecks (X, check, contact1, SET_TCW);
         setJointChecks (X, check, contact2, SET_TCW);
         checkTransformAndUndo (X, check, mech, joint, link2);

         // move link1, link2, and joint
         check = original.copy();

         setRigidBodyChecks (X, check, link1);
         setPointChecks (X, check, mkr2, mkr3);
         setRigidBodyChecks (X, check, link2);
         setJointChecks (X, check, joint);
         setJointChecks (X, check, contact1, SET_TCW);
         setJointChecks (X, check, contact2, SET_TCW);
         checkTransformAndUndo (X, check, mech, joint, link2, link1);

         // move contact1

         check = original.copy();

         setJointChecks (X, check, contact1, SET_TDW);
         checkTransformAndUndo (X, check, mech, contact1);

         // move contact1 and contact2

         check = original.copy();

         setJointChecks (X, check, contact1, SET_TDW);
         setJointChecks (X, check, contact2, SET_TDW);
         checkTransformAndUndo (X, check, mech, contact1, contact2);

         // move contact1 and link2

         check = original.copy();

         setRigidBodyChecks (X, check, link2);
         setJointChecks (X, check, contact1);
         setJointChecks (X, check, contact2, SET_TCW);
         checkTransformAndUndo (X, check, mech, contact1, link2);

         // move everything except the markers 0 and 2

         check = original.copy();

         setRigidBodyChecks (X, check, base);
         setRigidBodyChecks (X, check, link1);
         setRigidBodyChecks (X, check, link2);
         setPointChecks (X, check, mkr0, mkr1, mkr2, mkr3);
         setJointChecks (X, check, joint);
         setJointChecks (X, check, contact1);
         setJointChecks (X, check, contact2);
         checkTransformAndUndo (
            X, check, mech, base, link2, link1,
            joint, contact1, contact2, mkr1, mkr3);

         // move the mech model

         check = original.copy();

         setRigidBodyChecks (X, check, base);
         setRigidBodyChecks (X, check, link1);
         setRigidBodyChecks (X, check, link2);
         setPointChecks (X, check, mkr0, mkr1, mkr2, mkr3);
         setJointChecks (X, check, joint);
         setJointChecks (X, check, contact1);
         setJointChecks (X, check, contact2);
         checkTransformAndUndo (X, check, mech, mech);

         // move marker 0

         check = original.copy();
         setPointChecks (X, check, mkr0);
         checkTransformAndUndo (X, check, mech, mkr0);

         // move marker 0 and the base

         check = original.copy();
         setRigidBodyChecks (X, check, base);
         setPointChecks (X, check, mkr0, mkr1);
         checkTransformAndUndo (X, check, mech, mkr0, base);
      }
   }

   public void testHex3dBlock() {

      Hex3dBlock demo = new Hex3dBlock();

      demo.build (new String[] { "-reduced" });
      demo.setConnected (true);
      
      MechModel mech = (MechModel)demo.findComponent ("models/0");

      HingeJoint joint =
         (HingeJoint)mech.findComponent ("bodyConnectors/0");
      RigidBody leftBody = 
         (RigidBody)mech.findComponent ("rigidBodies/leftBody");
      RigidBody rightBody = 
         (RigidBody)mech.findComponent ("rigidBodies/rightBody");
      FemModel3d fem = 
         (FemModel3d)mech.findComponent ("models/fem");
      FemNode3d[] nodes = getNodeArray (fem);

      FemMeshComp meshc = fem.getSurfaceMeshComp();      

      //FemMarker mkr = (FemMarker)fem.findComponent ("markers/0");
      //Particle part = (Particle)mech.findComponent ("particles/0");

      FemMarker mkr = fem.addMarker (new Point3d(.23, -0.02, -0.03));
      Particle part = new Particle ("part", 1, 0.03, 0.01, -0.02);
      mech.addParticle (part);
      mech.attachPoint (part, fem);
      //RenderProps.setSphericalPoints (part, 0.015, Color.RED);
      //RenderProps.setSphericalPoints (mkr, 0.015, Color.GREEN);

      AttributeSet original = getCurrentAttributes (mech);
      AttributeSet check = getCurrentAttributes (mech);

      for (int k=0; k<2; k++) {
         AffineTransform3dBase X;

         // don't make the test transform too large or the markers 
         // will move out of the FEM
         if (k==0) {
            RigidTransform3d XR =
               new RigidTransform3d(0.02, 0.01, 0.05, 0.01, 0.005, 0.015);
            X = XR;
         }
         else {
            AffineTransform3d XA = new AffineTransform3d();
            XA.applyScaling (1.2, 1.3, 1.1);
            X = XA;
         }

         // move joint1

         check = original.copy();
         setJointChecks (X, check, joint);

         checkTransformAndUndo (X, check, mech, joint);

         // move leftBody

         check = original.copy();

         setRigidBodyChecks (X, check, leftBody);
         setPointChecks (X, check, nodes[0], nodes[4], nodes[8], nodes[12]);
         setPartialFemMeshCheck (
             X, check, meshc, nodes[0], nodes[4], nodes[8], nodes[12]);

         checkTransformAndUndo (X, check, mech, leftBody);

         // move joint and leftBody

         check = original.copy();

         setJointChecks (X, check, joint);
         setRigidBodyChecks (X, check, leftBody);
         setPointChecks (X, check, nodes[0], nodes[4], nodes[8], nodes[12]);
         setPartialFemMeshCheck (
             X, check, meshc, nodes[0], nodes[4], nodes[8], nodes[12]);

         checkTransformAndUndo (X, check, mech, joint, leftBody);

         // move the fem

         check = original.copy();
         setFemNodeChecks (X, check, nodes);
         setPointChecks (X, check, mkr, part);
         setFemMeshCheck (X, check, meshc);
         checkTransformAndUndo (X, check, mech, fem);

         // move the rightBody. Marker should move because none
         // of its masters are being transformed

         check = original.copy();

         setRigidBodyChecks (X, check, rightBody);
         setPointChecks (X, check, nodes[3], nodes[7], nodes[11], nodes[15]);
         setPartialFemMeshCheck (
           X, check, meshc, nodes[3], nodes[7], nodes[11], nodes[15]);

         GeometryTransformer transformer = transform (X, rightBody);
         setAttachedPointCheck (check, mkr);

         checkCurrentAttributes (mech, check, 1e-10);
         undo (transformer, rightBody);
         checkCurrentAttributes (mech, original, 1e-10);

         // move the rightBody and the attached nodes.  Marker should move
         // because only some of its masters are being transformed.

         check = original.copy();

         setRigidBodyChecks (X, check, rightBody);
         setFemNodeChecks (X, check, nodes[3], nodes[7], nodes[11], nodes[15]);
         setPartialFemMeshCheck (
           X, check, meshc, nodes[3], nodes[7], nodes[11], nodes[15]);

         checkTransformAndUndo (
            X, check, mech, nodes[3], nodes[7], nodes[11], nodes[15], rightBody);

         // move the marker and the attached particle.

         check = original.copy();

         setPointChecks (X, check, part, mkr);

         checkTransformAndUndo (X, check, mech, part, mkr);

         // move the nodes 2, 6, 10, 14. Marker and the attached particle
         // should not change

         check = original.copy();

         setFemNodeChecks (X, check, nodes[2], nodes[6], nodes[10], nodes[14]);
         setPartialFemMeshCheck (
           X, check, meshc, nodes[2], nodes[6], nodes[10], nodes[14]);

         checkTransformAndUndo (
            X, check, mech, nodes[2], nodes[6], nodes[10], nodes[14]);

         // move all the nodes surrounding the particle. It should now change

         check = original.copy();

         setFemNodeChecks (X, check, 
            nodes[2], nodes[6], nodes[10], nodes[14],
            nodes[1], nodes[5], nodes[9], nodes[13]);
         setPointChecks (X, check, part);
         setPartialFemMeshCheck (
           X, check, meshc, 
            nodes[2], nodes[6], nodes[10], nodes[14],
            nodes[1], nodes[5], nodes[9], nodes[13]);

         checkTransformAndUndo (
            X, check, mech,
            nodes[2], nodes[6], nodes[10], nodes[14],
            nodes[1], nodes[5], nodes[9], nodes[13]);
      }
      
   }

   // create an FEM beam  with specified size and stiffness, and add it 
   // to a mech model
   private FemModel3d addFem (
      MechModel mech, double wx, double wy, double wz, double stiffness) {
      FemModel3d fem = FemFactory.createHexGrid (null, wx, wy, wz, 10, 1, 1);
      fem.setMaterial (new LinearMaterial (stiffness, 0.3));
      fem.setDensity (1.0);
      fem.setMassDamping (1.0);
      fem.setSurfaceRendering (FemModel3d.SurfaceRender.Shaded);
      mech.addModel (fem);
      return fem;
   }

   void setScalingAboutPoint (
      AffineTransform3d X, 
      double sx, double sy, double sz, 
      double px, double py, double pz) {

      X.A.setDiagonal (sx, sy, sz);
      X.p.set (px-sx*px, py-sy*py, py-sy*py);
   }

   public void testJointedFems() {
      
      MechModel mech = new MechModel ("mechMod");
      
      double stiffness = 5000;
      // create first fem beam and fix the leftmost nodes      
      FemModel3d fem1 = addFem (mech, 2.4, 0.6, 0.4, stiffness);
      for (FemNode3d n : fem1.getNodes()) {
         if (n.getPosition().x <= -1.2) {
            n.setDynamic(false);
         }
      }
      // create the second fem beam and shift it 1.5 to the right
      FemModel3d fem2 = addFem (mech, 2.4, 0.4, 0.4, 0.1*stiffness);
      fem2.transformGeometry (new RigidTransform3d (1.5, 0, 0));

      // create a slotted revolute joint that connects the two fem beams
      RigidTransform3d TDW = new RigidTransform3d(0.5, 0, 0, 0, 0, Math.PI/2);
      HingeJoint joint = new HingeJoint (fem2, fem1, TDW);
      mech.addBodyConnector (joint);

      AttributeSet original = getCurrentAttributes (mech);
      AttributeSet check = getCurrentAttributes (mech);

      RigidTransform3d TCW = new RigidTransform3d(0, 0, 0, 0, 0, Math.PI/2);
      RigidTransform3d XT;
      GeometryTransformer transformer;

      // move joint

      XT = new RigidTransform3d (0.5, 0, 0);
      check = original.copy();
      setJointChecks (XT, check, joint);
      checkTransformAndUndo (XT, check, mech, joint);

      // move joint so far it hits the end of fem1

      XT = new RigidTransform3d (1.0, 0, 0);
      check = original.copy();
      TDW.p.set (1.2, 0, 0);
      check.set (joint, "TDW", TDW);
      TCW.p.set (1.5, 0, 0);
      check.set (joint, "TCW", TCW);
      checkTransformAndUndo (XT, check, mech, joint);

      // move fem2

      check = original.copy();
      XT = new RigidTransform3d (-1.0, 0, 0);

      setFemChecks (XT, check, fem2);
      checkTransformAndUndo (XT, check, mech, fem2);

      // move fem2 so that the joint hits the end of fem1

      check = original.copy();
      XT = new RigidTransform3d (1.0, 0, 0);
      setFemChecks (XT, check, fem2);
      TCW.p.set (1.3, 0, 0);
      check.set (joint, "TCW", TCW);

      transformer = checkTransform (XT, check, mech, fem2);

      // then move fem2 back

      XT = new RigidTransform3d (-1.1, 0, 0);
      setFemChecks (XT, check, fem2);
      TCW.p.set (0.5, 0, 0);
      check.set (joint, "TCW", TCW);
      checkTransform (XT, check, mech, fem2);

      checkUndo (transformer, original, mech, fem2);

      // move fem1

      check = original.copy();
      XT = new RigidTransform3d (-0.5, 0, 0);
      setFemChecks (XT, check, fem1);
      checkTransformAndUndo (XT, check, mech, fem1);

      // move fem1 so that the joint hits the limit

      check = original.copy();
      XT = new RigidTransform3d (-1.0, 0, 0);
      setFemChecks (XT, check, fem1);
      TDW.p.set (0.2, 0, 0);
      check.set (joint, "TDW", TDW);

      transformer = checkTransform (XT, check, mech, fem1);

      // and then move fem1 back ...

      XT = new RigidTransform3d (0.2, 0, 0);
      setFemChecks (XT, check, fem1);
      TDW.p.set (0.4, 0, 0);
      check.set (joint, "TDW", TDW);
      checkTransform (XT, check, mech, fem1);

      setFemChecks (XT, check, fem1);
      TDW.p.set (0.5, 0, 0);
      check.set (joint, "TDW", TDW);
      checkTransform (XT, check, mech, fem1);

      checkUndo (transformer, original, mech, fem1);

      // now move fem1 and the joint
      check = original.copy();

      XT = new RigidTransform3d (1.0, 0, 0);
      setJointChecks (XT, check, joint);
      setFemChecks (XT, check, fem1);

      checkTransformAndUndo (XT, check, mech, joint, fem1);

      // now move fem1 and the joint far enough to take the joint away from fem2
      
      check = original.copy();

      XT = new RigidTransform3d (-0.5, 0, 0);
      setJointChecks (XT, check, joint);
      setFemChecks (XT, check, fem1);
      TCW.p.set (0.3, 0, 0);
      check.set (joint, "TCW", TCW);
      transformer = checkTransform (XT, check, mech, joint, fem1);

      // and move partly back
      
      XT = new RigidTransform3d (0.4, 0, 0);
      setJointChecks (XT, check, joint);
      setFemChecks (XT, check, fem1);
      TCW.p.set (0.4, 0, 0);
      check.set (joint, "TCW", TCW);
      checkTransform (XT, check, mech, joint, fem1);

      checkUndo (transformer, original, mech, joint, fem1);

      // move fem2 and the joint

      check = original.copy();

      XT = new RigidTransform3d (-1.0, 0, 0);
      setJointChecks (XT, check, joint);
      setFemChecks (XT, check, fem2);

      checkTransformAndUndo (XT, check, mech, joint, fem2);

      // move fem2 and the joint far enough to move the joint out of fem1

      check = original.copy();

      XT = new RigidTransform3d (1.0, 0, 0);
      setFemChecks (XT, check, fem2);
      setJointChecks (XT, check, joint);
      TDW.p.set (1.2, 0, 0);
      check.set (joint, "TDW", TDW);

      transformer = checkTransform (XT, check, mech, joint, fem2);

      // and move back. TCW will move to the edge of fem2 to try
      // to reduce the error

      XT = new RigidTransform3d (-0.5, 0, 0);
      setFemChecks (XT, check, fem2);
      setJointChecks (XT, check, joint);
      TCW.p.set (0.8, 0, 0);
      check.set (joint, "TCW", TCW);

      checkTransform (XT, check, mech, joint, fem2);

      checkUndo (transformer, original, mech, joint, fem2);

      // now do a random transform on everything

      check = original.copy();
      XT.setRandom();

      setFemChecks (XT, check, fem2);
      setFemChecks (XT, check, fem1);
      setJointChecks (XT, check, joint);

      checkTransformAndUndo (XT, check, mech, joint, fem1, fem2);
      checkTransformAndUndo (XT, check, mech, mech);

      // now do affine transforms

      AffineTransform3d XA = new AffineTransform3d();

      // scale fem2

      setScalingAboutPoint (XA, 1.2, 0.8, 1.3, 1.5, 0, 0);
      check = original.copy();
      setFemChecks (XA, check, fem2);
      checkTransformAndUndo (XA, check, mech, fem2);

      // scale fem2 so the joint hits the end of fem1

      setScalingAboutPoint (XA, 0.5, 0.8, 1.3, 1.5, 0, 0);
      check = original.copy();
      setFemChecks (XA, check, fem2);
      TCW.p.set (0.9, 0, 0);
      check.set (joint, "TCW", TCW);
      checkTransformAndUndo (XA, check, mech, fem2);

      // scale fem1

      check = original.copy();
      setScalingAboutPoint (XA, 1.2, 0.8, 1.3, 0, 0, 0);
      setFemChecks (XA, check, fem1);
      checkTransformAndUndo (XA, check, mech, fem1);

      // scale fem1 so the joint hits the end of fem2

      check = original.copy();
      setScalingAboutPoint (XA, 0.5, 0.8, 1.3, -1.5, 0, 0);
      setFemChecks (XA, check, fem1);
      TDW.p.set (-0.15, 0, 0);
      check.set (joint, "TDW", TDW);
      checkTransformAndUndo (XA, check, mech, fem1);

      // scale fem1 and the joint

      check = original.copy();
      setScalingAboutPoint (XA, 1.2, 0.8, 1.3, 0, 0, 0);
      setFemChecks (XA, check, fem1);
      setJointChecks (XA, check, joint);
      checkTransformAndUndo (XA, check, mech, joint, fem1);

      // scale fem1 and the joint so the joint hits the end of fem2

      check = original.copy();
      setScalingAboutPoint (XA, 0.5, 0.8, 1.3, -1.5, 0, 0);
      setFemChecks (XA, check, fem1);
      setJointChecks (XA, check, joint);
      TCW.p.set (0.3, 0, 0);
      check.set (joint, "TCW", TCW);
      checkTransformAndUndo (XA, check, mech, joint, fem1);

      // scale fem2 and the joint

      check = original.copy();
      setScalingAboutPoint (XA, 1.2, 0.8, 1.3, 1.5, 0, 0);
      setFemChecks (XA, check, fem2);
      setJointChecks (XA, check, joint);
      checkTransformAndUndo (XA, check, mech, joint, fem2);

      // scale fem2 and the joint so the joint hits the end of fem1

      check = original.copy();
      setScalingAboutPoint (XA, 0.5, 0.8, 1.3, 2.5, 0, 0);
      setFemChecks (XA, check, fem2);
      setJointChecks (XA, check, joint);
      TDW.p.set (1.2, 0, 0);
      check.set (joint, "TDW", TDW);
      checkTransformAndUndo (XA, check, mech, joint, fem2);

      // do a random affine transform on everything

      check = original.copy();

      XA.setRandom();

      setFemChecks (XA, check, fem2);
      setFemChecks (XA, check, fem1);
      setJointChecks (XA, check, joint);

      checkTransformAndUndo (XA, check, mech, joint, fem1, fem2);
      checkTransformAndUndo (XA, check, mech, mech);
   }

   public void testMuscleFem() {

      MechModel mech = new MechModel ("mech");
      FemMuscleModel fem = new FemMuscleModel ("fem");
      FemFactory.createHexGrid (
         fem, 1.0, 0.25, 0.25, 4, 2, 2);

      for (FemNode3d n : fem.getNodes()) {
         if (Math.abs (n.getPosition().x + 0.5) < 1e-10) {
            n.setDynamic (false);
         }
      }

      MuscleBundle all = new MuscleBundle ("all");
      ArrayList<MuscleElementDesc> descs = new ArrayList<MuscleElementDesc>();
      for (FemElement3d e : fem.getElements()) {
         MuscleElementDesc d = new MuscleElementDesc();
         d.setElement (e);
         d.setDirection (Vector3d.X_UNIT);
         all.addElement (d);
         descs.add(d);
      }
      fem.addMuscleBundle (all);
      
      fem.setDirectionRenderLen (0.5);

      mech.addModel (fem);


      AttributeSet original = getCurrentAttributes (mech);
      AttributeSet check = getCurrentAttributes (mech);

      for (AffineTransform3dBase X : myRandomTransforms) {

         // move a single element desc

         Vector3d dir = new Vector3d();
         X.getMatrix().getColumn (0, dir);
         dir.normalize();

         MuscleElementDesc desc = descs.get(0);

         check = original.copy();
         check.set (desc, "dir", dir);
         checkTransformAndUndo (X, check, mech, desc);

         // move a whole muscle bundle

         check = original.copy();
         for (MuscleElementDesc d : descs) {
            check.set (d, "dir", dir);
         }
         checkTransformAndUndo (X, check, mech, all);

         // move the whole fem

         check = original.copy();
         for (MuscleElementDesc d : descs) {
            check.set (d, "dir", dir);
         }
         setFemChecks (X, check, fem);
         checkTransformAndUndo (X, check, mech, fem);
      }
   }

   public void testSkinMeshBody() {

      FemSkinDemo demo = new FemSkinDemo();

      demo.build (new String[0]);
      
      MechModel mech = (MechModel)demo.findComponent ("models/0");

      HingeJoint joint =
         (HingeJoint)mech.findComponent ("bodyConnectors/0");
      RigidBody leftBlock = 
         (RigidBody)mech.findComponent ("rigidBodies/leftBlock");
      RigidBody rightBlock = 
         (RigidBody)mech.findComponent ("rigidBodies/rightBlock");
      RigidBody middleBlock = 
         (RigidBody)mech.findComponent ("rigidBodies/middleBlock");
      FemModel3d fem1 = 
         (FemModel3d)mech.findComponent ("models/fem1");
      FemModel3d fem2 = 
         (FemModel3d)mech.findComponent ("models/fem2");
      SkinMeshBody skin = 
         (SkinMeshBody)mech.findComponent ("meshBodies/skin");
      HingeJoint joint0 = 
         (HingeJoint)mech.findComponent ("bodyConnectors/0");
      HingeJoint joint1 = 
         (HingeJoint)mech.findComponent ("bodyConnectors/1");

      AttributeSet original = getCurrentAttributes (mech);
      AttributeSet check = getCurrentAttributes (mech);

      for (AffineTransform3dBase X : myRandomTransforms) {
         // lower check tolerance for when skin recomputes weights, because
         // weights have only float precision
         double ftol = 1e-7;
         
         // just move the skin. Nothing else should move.

         check = original.copy();

         setSkinMeshBodyChecks (X, check, skin);
         checkTransformAndUndo (X, check, mech, skin);

         // move one of the fems. Nothing else should move.
         
         check = original.copy();
         setFemChecks (X, check, fem1);
         checkTransformAndUndo (X, check, mech, ftol, fem1);

         // move the middle block. Only the attached nodes should move

         check = original.copy();
         setRigidBodyChecks (X, check, middleBlock);
         ArrayList<FemNode3d> movedNodes1 = new ArrayList<FemNode3d>();
         ArrayList<FemNode3d> movedNodes2 = new ArrayList<FemNode3d>();
         for (DynamicAttachment a : middleBlock.getMasterAttachments()) {
            if (a.getSlave() instanceof FemNode3d) {
               FemNode3d n = (FemNode3d)a.getSlave();
               setPointChecks (X, check, n);
               if (n.getGrandParent() == fem1) {
                  movedNodes1.add (n);
               }
               else {
                  movedNodes2.add (n);
               }
            }
         }
         setPartialFemMeshCheck (
            X, check, fem1.getSurfaceMeshComp(),
            movedNodes1.toArray (new FemNode3d[0]));
         setPartialFemMeshCheck (
            X, check, fem2.getSurfaceMeshComp(),
            movedNodes2.toArray (new FemNode3d[0]));

         checkTransformAndUndo (X, check, mech, ftol, middleBlock);

         // move the whole mech model.

         check = original.copy();
         setRigidBodyChecks (X, check, rightBlock);
         setRigidBodyChecks (X, check, middleBlock);
         setRigidBodyChecks (X, check, leftBlock);
         setFemChecks (X, check, fem1);
         setFemChecks (X, check, fem2);
         setSkinMeshBodyChecks (X, check, skin);
         setJointChecks (X, check, joint0);
         setJointChecks (X, check, joint1);
         checkTransformAndUndo (X, check, mech, ftol, mech);

      }
   }

   public void testRigidAndMeshBodies() {

      double density = 1000;

      MechModel mech = new MechModel ("mech");

      PolygonalMesh mesh =
         MeshFactory.createIcosahedralSphere (2.0, /*ndiv=*/2);
      FixedMeshBody fixedBody = new FixedMeshBody (mesh);

      RigidSphere sphere =
         new RigidSphere (null, /*radius=*/2.0, density);
      
      RigidCylinder cylinder =
         new RigidCylinder (null, /*radius=*/2.0, /*h=*/3.0, density);

      RigidTorus torus = 
         new RigidTorus (null, /*router=*/3.0, /*rinner=*/1.0, density);

      RigidEllipsoid ellipsoid = 
         new RigidEllipsoid (null, 2.0, 4.0, 6.0, density);

      mech.addMeshBody (fixedBody);
      mech.addRigidBody (ellipsoid);
      mech.addRigidBody (torus);
      mech.addRigidBody (cylinder);
      mech.addRigidBody (sphere);

      RigidTransform3d TBW = new RigidTransform3d();
      TBW.setRandom();

      ellipsoid.setPose (TBW);
      torus.setPose (TBW);
      cylinder.setPose (TBW);
      sphere.setPose (TBW);
      fixedBody.setPose (TBW);

      AttributeSet original = getCurrentAttributes (mech);
      AttributeSet check = getCurrentAttributes (mech);

      for (AffineTransform3dBase X : myRandomTransforms) {

         // transform each of the bodies

         check = original.copy();
         setFixedMeshBodyChecks (X, check, fixedBody);
         checkTransformAndUndo (X, check, mech, fixedBody);

         check = original.copy();
         setRigidSphereChecks (X, check, sphere);
         checkTransformAndUndo (X, check, mech, sphere);

         check = original.copy();
         setRigidTorusChecks (X, check, torus);
         checkTransformAndUndo (X, check, mech, torus);

         check = original.copy();
         setRigidCylinderChecks (X, check, cylinder);
         checkTransformAndUndo (X, check, mech, cylinder);

         check = original.copy();
         setRigidEllipsoidChecks (X, check, ellipsoid);
         checkTransformAndUndo (X, check, mech, ellipsoid);
      }
   }

   public void testEmbeddedMesh() {

      MechModel mech = new MechModel();
      FemModel3d fem = FemFactory.createHexGrid (
         null, 1.0, 0.5, 0.5, 4, 2, 2);

      fem.transformGeometry (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
      FemFactory.setPlanarNodesFixed (
         fem, new Point3d(0, 0, 0.5), new Vector3d(0, 0, 1), true);

      mech.addModel (fem);
      mech.setGravity(0,0,1e-10);
         
      MeshBase mesh;
      mesh = MeshFactory.createSphere (0.2, 24, 24);
      mesh.scale (1, 1, 2);

      FemMeshComp mcomp = fem.addMesh (mesh);

      AttributeSet original = getCurrentAttributes (mech);
      AttributeSet check = getCurrentAttributes (mech);

      for (AffineTransform3dBase X : myRandomTransforms) {

         // move the ebedded mesh - nothing should happen

         check = original.copy();
         checkTransformAndUndo (X, check, mech, mcomp);

         // move the whole thing

         check = original.copy();
         setFemChecks (X, check, fem);
         setFemMeshCheck (X, check, mcomp);
         checkTransformAndUndo (X, check, mech, mech);
      }
   }

   public void testParticlePlaneConstraint() {

      MechModel mech = new MechModel ("mech");
      FemMuscleModel fem = new FemMuscleModel ("fem");
      FemFactory.createHexGrid (fem, 1.0, 0.25, 0.25, 4, 2, 2);

      Point3d center = new Point3d (0.5, 0, 0);
      Vector3d normal = new Vector3d (1, 0, 0);
      ParticlePlaneConstraint constraint =
         new ParticlePlaneConstraint (normal, center);
      mech.addConstrainer (constraint);

      for (FemNode3d n : fem.getNodes()) {
         if (Math.abs (n.getPosition().x - 0.5) > 1e-10) {
            constraint.addParticle (n);
         }
      }
      mech.addModel (fem);

      AttributeSet original = getCurrentAttributes (mech);
      AttributeSet check = getCurrentAttributes (mech);

      for (AffineTransform3dBase X : myRandomTransforms) {

         Point3d cen = new Point3d(0.5, 0, 0);
         Vector3d nrm = new Vector3d(1, 0, 0);
         X.getMatrix().mulInverseTranspose (nrm, nrm);
         nrm.normalize();
         cen.transform (X);

         // move the constrainer

         check = original.copy();
         check.set (constraint, "center", cen);
         check.set (constraint, "normal", nrm);
         checkTransformAndUndo (X, check, mech, constraint);

         // move the whole thing

         setFemChecks (X, check, fem);
         checkTransformAndUndo (X, check, mech, mech);
      }
   }

   public void testRigidCompositeBody() {
   }

   public void test() {
      
      testMechModelDemo();
      testHex3dBlock();
      testJointedFems();
      testMuscleFem();
      testParticlePlaneConstraint();
      testRigidAndMeshBodies();
      testEmbeddedMesh();
      testSkinMeshBody();

      System.out.println ("\nPassed\n");
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);
      TransformGeometryTest tester = new TransformGeometryTest();
      tester.test();
   }
      
}
