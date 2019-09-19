/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.io.IOException;
import java.util.ArrayList;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.Vector;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import artisynth.core.modelbase.ModelComponent;

// allows for Affine transform of numeric input probes
// XXX DOESN'T WORK FOR FRAMES, ONLY POINTS
public class AffineNumericInputProbe extends NumericInputProbe {

   MatrixNd R = null; // affine matrix
   VectorNd t = null; // translation vector
   protected double timeOffset = 0;

   protected VectorNd myTmpVec; // private temporary vector

   /**
    * Interpolate data to specified time and set related properties values.
    * 
    * time in nano-seconds
    */
   public AffineNumericInputProbe () {
      super();
   }

   public AffineNumericInputProbe (ModelComponent e) {
      super(e);
   }

   public AffineNumericInputProbe (ModelComponent e, String propName,
      String fileName)
      throws IOException {
      super(e, propName, fileName);
      initTransform(myVsize);
   }

   public AffineNumericInputProbe (
      ModelComponent e, String propName, double startTime, double stopTime) {
      super(e, propName, startTime, stopTime);
      initTransform(myVsize);
   }

//   public AffineNumericInputProbe (Property prop, ModelComponent e) {
//      super(prop, e);
//      initTransform(myVsize);
//   }
//
//   public AffineNumericInputProbe (
//      Property prop, ModelComponent e, double ymin, double ymax) {
//      super(prop, e, ymin, ymax);
//      initTransform(myVsize);
//   }
//
   public AffineNumericInputProbe (Property[] props, ModelComponent e) {
      super(props, e);
      initTransform(myVsize);
   }


   protected VectorNd transform(VectorNd x) {
      VectorNd r = new VectorNd(x.size());
      r.mul(R, x);
      r.add(t);

      return r;
   }

   // initializes the identity transformation
   public void initTransform(int size) {
      R = new MatrixNd(size, size);
      t = new VectorNd(size);
      R.setIdentity();
      t.setZero();
   }

   // fills in a larger transform
   public void setTransform(AffineTransform3d A) {

      int nPoints = myVsize / 3;
      Matrix3dBase Ar = A.getMatrix();
      Vector3d At = A.getOffset();

      for (int i = 0; i < nPoints; i++) {
         R.setSubMatrix(i * 3, i * 3, Ar);
         t.setSubVector(i * 3, new VectorNd(At));
      }

   }

   public AffineTransform3d getTransform() {

      Matrix3d Ar = new Matrix3d();
      R.getSubMatrix(0, 0, Ar);
      Vector3d At = new Vector3d(t.get(0), t.get(1), t.get(2)); // not very
                                                                // elegant, but
                                                                // gets the job
                                                                // done

      return new AffineTransform3d(Ar, At);

   }

   public VectorNd getValues(double t) {
      VectorNd result = new VectorNd(myVsize);

      double tloc = (t - getStartTime()-timeOffset) / myScale;
      myNumericList.interpolate(result, tloc);
      result = transform(result);

      return result;
   }

   public ArrayList<Point3d> getPoints(double t) {
      ArrayList<Point3d> result = new ArrayList<Point3d>();

      VectorNd vec = getValues(t);
      double buff[] = vec.getBuffer();
      for (int i = 0; i < myVsize / 3; i++) {
         Point3d p = new Point3d(buff[i * 3], buff[i * 3 + 1], buff[i * 3 + 2]);
         result.add(p);
      }

      return result;

   }

   @Override
   public void set(
      Property[] props, String[] driverExpressions, String[] variableNames,
      int[] variableDimensions, PlotTraceInfo[] traceInfos) {
      super.set(
         props, driverExpressions, variableNames, variableDimensions,
         traceInfos);
      myTmpVec = new VectorNd(myVsize);
      initTransform(myVsize);
   }

   @Override
   public void createNumericList(int vsize) {
      super.createNumericList(vsize);
      myTmpVec = new VectorNd(vsize);
      initTransform(vsize);
   }
   
   public void setTimeOffset(double tOffset) {
      timeOffset = tOffset;
   }
   
   public double getTimeOffset() {
      return timeOffset;
   }

   @Override
   public void apply(double t) {
      double tloc = (t - getStartTime() - timeOffset) / myScale;
      myNumericList.interpolate(myTmpVec, tloc); // fills myTmpVec
      myTmpVec = transform(myTmpVec); // here is where I perform the transform
                                      // on the data

      int k = 0;
      double[] buf = myTmpVec.getBuffer(); // gets entire interpolated buffer
      // load all channels
      for (NumericProbeVariable var : myVariables.values()) {
         var.setValues(buf, k); // sets var variables, which will next be
                                // updated
         k += var.getDimension();
      }
      updateJythonVariables(myVariables, tloc);
      for (int i = 0; i < myDrivers.size(); i++) {
         NumericProbeDriver driver = myDrivers.get(i);
         double[] vals = driver.eval(myVariables, myJythonLocals);
         Object valObj = myConverters[i].arrayToObject(vals);
         myPropList.get(i).set(valObj);
      }
   }
}
