package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;

import artisynth.core.femmodels.FemElement;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.materials.FemMaterial;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ScanToken;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.geometry.Boundable;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.InternalErrorException;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Base class for a shell element. Compared to traditional elements, 
 * shell elements are infinitely-thin elements that can better model surfaces.
 * Examples include water surfaces, living tissue, clothing, and aluminium sheet.
 */
public abstract class ShellElement3d extends FemElement3dBase
   implements Boundable, FrameAttachable {
   
   protected static double DEFAULT_THICKNESS = 0.01;
   protected double myDefaultThickness = DEFAULT_THICKNESS;
   
   protected static FemElementRenderer myRenderer;
   
   public static PropertyList myProps =
      new PropertyList (ShellElement3d.class, FemElement3dBase.class);

   static {
      myProps.add (
         "defaultThickness",
         "default rest thickness associated with this element",
         DEFAULT_THICKNESS);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ShellElement3d() {
   }

   /* --- Integration points and data --- */

   // protected IntegrationPoint3d[] createMembraneIntegrationPoints (
   //    IntegrationPoint3d[] ipnts) {
   //    // assume that the membrane integration points are simply the first nump
   //    // integration points, where nump = numPlanarIntegrationPoints()
   //    int nump = numPlanarIntegrationPoints();
   //    IntegrationPoint3d[] mpnts = new IntegrationPoint3d[nump];
   //    for (int i=0; i<nump; i++) {
   //       mpnts[i] = ipnts[i];
   //    }
   //    return mpnts;
   // }
   
   
   /**
    * Number of integration points in the shell plane.
    */
   public abstract int numPlanarIntegrationPoints();
   
   /* --- Volume --- */

   @Override
   public double computeVolumes () {
      return doComputeVolume (/* isRest= */false);
   }

   @Override
   public double computeRestVolumes () {
      return doComputeVolume (/* isRest= */true);
   }
   
   public double doComputeVolume (boolean isRest) {
      double vol = 0;

      double minDetJ = Double.MAX_VALUE;

      // For each integration point...
      IntegrationPoint3d[] ipnts = getIntegrationPoints ();
      IntegrationData3d[] idata= getIntegrationData ();
      int nump = numIntegrationPoints();
      for (int i = 0; i < nump; i++) {
         IntegrationPoint3d pt = ipnts[i];
         double detJ;
         double dv;
         if (isRest) {
            detJ = idata[i].getDetJ0();
            dv = detJ*pt.getWeight();
         }
         else {
            detJ = pt.computeJacobianDeterminant(getNodes());
            dv = detJ*pt.getWeight();
            // normalize detJ to get true value relative to rest position
            detJ /= idata[i].getDetJ0();           
         }
         if (detJ < minDetJ) {
            minDetJ = detJ;
         }        
         vol += dv;
      }
      if (myElementClass == ElementClass.MEMBRANE) {
         // for membrane elements, we need to explicitly scale the volume
         // by the thickness because the deformation gradient contains
         // no scaling in the normal direction.
         vol *= myDefaultThickness;
      }
      if (isRest) {
         myRestVolume = vol;
      }
      else {
         myVolume = vol;
      }
      return minDetJ;
   }

   /* --- Thickness and directors --- */
   
   public double getDefaultThickness() {
      return myDefaultThickness;
   }
   
   public void setDefaultThickness(double newThickness) {
      double prevThickness = myDefaultThickness;
      myDefaultThickness = newThickness;
      // Update rest data since it may depend on knowing the shell thickness.
      super.invalidateRestData();
      if (getFemModel() != null) {
         invalidateElementAndNodeMasses(); // masses depend on thickness
         for (FemNode3d n : myNodes) {
            n.rescaleDirectorsIfNecessary (this, prevThickness);
         }
      }
   }
   
//   /**
//    * Update the rest director of each node. There should be no need
//    * 
//    * This should be called whenever node.myAdjElements is updated or 
//    * shell thickness is modified, both which the rest director depends on.
//    */
//   public void computeRestDirectors() {
//      for (FemNode3d n : myNodes) {
//         if (n.hasDirector()) {
//            n.computeRestDirector ();
//         }
//      }
//   }
   
   void invalidateRestDirectorsIfNecessary() {
      for (FemNode3d n : myNodes) {
         // make sure n is not null: it might be if this method is called early
         // during a scan process before nodes have been initialized
         if (n != null) {
            n.invalidateRestDirectorIfNecessary();
         }
      }
   }
   
   public void invalidateRestData () {
      super.invalidateRestData();
      invalidateRestDirectorsIfNecessary();
   }

   /**
    * Computes the shell element normal, with respect to rest coordinates, at a
    * specific node, and returns the (cross product) area associated with that
    * normal. This is used for automatically initializing directors.
    */
   public abstract double computeRestNodeNormal (Vector3d nrm, FemNode3d node);

   /**
    * Computes the shell element normal, with respect to current coordinates,
    * at a specific node, and returns the (cross product) area associated with
    * that normal. This can be used for automatically initializing directors.
    */
   public abstract double computeNodeNormal (Vector3d nrm, FemNode3d node);
   
   /**
    * Computes a normal for three points oriented counter-clockwise
    * and returns the area of the associated triangle.
    */
   protected double computeNormal (
      Vector3d nrm, Point3d p0, Point3d p1, Point3d p2) {
      Vector3d d01 = new Vector3d();
      Vector3d d02 = new Vector3d();
      d01.sub (p1, p0);
      d02.sub (p2, p0);
      nrm.cross (d01, d02);
      double mag = nrm.norm();
      if (mag != 0) {
         nrm.scale (1/mag);
      }
      return mag/2;
   }         

   /* --- Shape functions and coordinates --- */

   /**
    * {@inheritDoc}
    */
   @Override   
   public void computeLocalPosition (Vector3d pnt, Vector3d ncoords) {
      pnt.setZero();
      Vector3d tmp = new Vector3d();
      switch (myElementClass) {
         case SHELL: {     
            for (int i=0; i<numNodes(); ++i) {
               myNodes[i].getDirector (tmp);
               tmp.scaledAdd (
                  0.5*(ncoords.z-1), tmp, myNodes[i].getLocalPosition());
               pnt.scaledAdd (getN(i,ncoords), tmp);
            }
            break;
         }
         case MEMBRANE: {
            for (int i=0; i<numNodes(); ++i) {
               pnt.scaledAdd (getN(i,ncoords), myNodes[i].getLocalPosition());
            }  
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Element class " + myElementClass + " not supported");
         }  
      }
   }   

   public void computeJacobian (Matrix3d J, Vector3d ncoords) {
      Vector3d dNds = new Vector3d();
      switch (myElementClass) {
         case SHELL: {
            Vector3d d = new Vector3d();
            Vector3d v = new Vector3d();

            double st = -0.5*(1-ncoords.z);
            for (int i=0; i<numNodes(); i++) {
               FemNode3d node = myNodes[i];
               d.sub (node.getLocalPosition(), node.getBackPosition());
               v.scaledAdd (st, d, node.getLocalPosition());
               
               getdNds (dNds, i, ncoords);
               double s0 = dNds.x;
               double s1 = dNds.y;
               double s2 = getN(i,ncoords)*0.5;
               
               J.m00 += s0*v.x; J.m01 += s1*v.x; J.m02 += s2*d.x;
               J.m10 += s0*v.y; J.m11 += s1*v.y; J.m12 += s2*d.y;
               J.m20 += s0*v.z; J.m21 += s1*v.z; J.m22 += s2*d.z;
            }            
            break;
         }
         case MEMBRANE: {
            Vector3d jc0 = new Vector3d();
            Vector3d jc1 = new Vector3d();
            Vector3d jc2 = new Vector3d();
            for (int i=0; i<numNodes(); i++) {
               Vector3d pos = myNodes[i].getLocalPosition();
               getdNds (dNds, i, ncoords);
               jc0.scaledAdd (dNds.x, pos);
               jc1.scaledAdd (dNds.y, pos);
            }            
            jc2.cross (jc0, jc1);
            jc2.normalize();

            J.m00 = jc0.x; J.m01 = jc1.x; J.m02 = jc2.x; 
            J.m10 = jc0.y; J.m11 = jc1.y; J.m12 = jc2.y; 
            J.m20 = jc0.z; J.m21 = jc1.z; J.m22 = jc2.z; 
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Element class " + myElementClass + " not supported");
         }  
      }
   }

   /* --- Hierarchy --- */
   
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      if (hcomp == getParent()) {
         invalidateRestDirectorsIfNecessary();
      }
   }
   
   public void disconnectFromHierarchy (CompositeComponent hcomp) {
      super.disconnectFromHierarchy (hcomp);
      if (hcomp == getParent()) {
         invalidateRestDirectorsIfNecessary();
      }
   }
   
   public abstract double nearestPoint (Point3d near, Point3d pnt);

   /* --- Scanning, writing and copying --- */
   
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "elementClass")) {
         myElementClass = rtok.scanEnum(ElementClass.class);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }      

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("elementClass=" + myElementClass);
   }   
   
   public ShellElement3d copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      return (ShellElement3d)super.copy (flags, copyMap);
   }

   /* --- Element creation --- */

   public static ShellElement3d createElement (
      FemNode3d[] nodes, double thickness, boolean membrane,
      boolean flipped) {
      
      switch(nodes.length) {
         case 3:
            if (flipped) {
               return new ShellTriElement(
                  nodes[0], nodes[2], nodes[1], thickness, membrane);
            }
            else {
               return new ShellTriElement(
                  nodes[0], nodes[1], nodes[2], thickness, membrane);
            }
         case 4:
            if (flipped) {
               return new ShellQuadElement(
                  nodes[0], nodes[3], nodes[2], nodes[1], thickness, membrane);
            }
            else {
               return new ShellQuadElement(
                  nodes[0], nodes[1], nodes[2], nodes[3], thickness, membrane);
            }          
         default:
            throw new IllegalArgumentException(
               "Unknown shell element type with " + nodes.length + " nodes");
      }
   }


}
