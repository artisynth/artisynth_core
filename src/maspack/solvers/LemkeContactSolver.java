/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import maspack.matrix.LUDecomposition;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.ContactInfo;
import maspack.spatialmotion.SpatialInertia;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.util.InternalErrorException;

public class LemkeContactSolver extends LemkeSolverBase {
   private static final int NU = 0x1;
   private static final int SIGMA = 0x2;
   private static final int GAMMA = 0x3;

   private static final int THETA = (NU | Z_VAR);
   private static final int PHI = (SIGMA | Z_VAR);
   private static final int LAMBDA = (GAMMA | Z_VAR);

   private static int MAX_KSIZE = 8;

   private static final boolean wzVarNames = false;

   private boolean variableSize = false;
   private boolean reducibleSize = false;
   private boolean smartDirections = false;

   private int numFrictionDirs = 4;
   private boolean frictionEnabledP = true;
   private boolean checkPivotsP = true;

   private double conditionEstimate = 1;

   public boolean isFrictionEnabled() {
      return frictionEnabledP;
   }

   public void setFrictionEnabled (boolean enable) {
      frictionEnabledP = enable;
   }

   public int getNumFrictionDirections() {
      return numFrictionDirs;
   }

   public void setNumFrictionDirections (int num) {
      if (num < 0 || (num % 2 != 0)) {
         throw new IllegalArgumentException (
            "number of friction directions must be non-negative and even");
      }
      numFrictionDirs = num;
   }

   public void setEpsilon (double eps) {
      epsilon = eps;
   }

   public void setVariableSize (boolean enable) {
      variableSize = enable;
   }

   public boolean getVariableSize() {
      return variableSize;
   }

   public void setReducibleSize (boolean enable) {
      reducibleSize = enable;
   }

   public boolean getReducibleSize() {
      return reducibleSize;
   }

   public void setSmartDirections (boolean enable) {
      smartDirections = enable;
   }

   public boolean getSmartDirections() {
      return smartDirections;
   }

   // codes to describe the activity levels of the friction constraints.

   private static final int FRICTION_ACTIVE = 1;
   private static final int FRICTION_INACTIVE = 2;
   private static final int FRICTION_DORMANT = 3;

   protected static class ContactVariable extends Variable {
      ContactVariable next; // next variable in list
      int ci; // contact index
      int di; // first direction variable index

      Wrench nd; // constraint wrench (types THETA, PHI, NU, SIGMA)

      String getName() {
         if (wzVarNames) {
            if (type == Z0) {
               return "z0";
            }
            else if (isZ()) {
               return "z" + (idx + 1);
            }
            else {
               return "w" + (idx + 1);
            }
         }
         else {
            switch (type) {
               case THETA: {
                  return "t" + ci;
               }
               case NU: {
                  return "n" + ci;
               }
               case PHI: {
                  return "p" + ci + "." + (idx - di);
               }
               case SIGMA: {
                  return "s" + ci + "." + (idx - di);
               }
               case LAMBDA: {
                  return "l" + ci;
               }
               case GAMMA: {
                  return "g" + ci;
               }
               case Z0: {
                  return "z0";
               }
               default: {
                  return "???";
               }
            }
         }
      }

      void init (int type, int ci, int di, Variable complement, Wrench nd) {
         super.init (type, complement);
         this.ci = ci;
         this.di = di;
         this.nd = nd;
      }

      void set (ContactVariable var, int newdi, ContactVariable complement) {
         super.set (var);
         ci = var.ci;
         nd = var.nd;
         di = newdi;
         this.complement = complement;
      }

      void set (ContactVariable var) {
         super.set (var);
         ci = var.ci;
         nd = var.nd;
         di = var.di;
      }
   }

   private static class VariableList {
      ContactVariable head;
      ContactVariable tail;

      void clear() {
         head = tail = null;
      }

      void add (ContactVariable var) {
         if (tail == null) {
            head = var;
         }
         else {
            tail.next = var;
         }
         tail = var;
         var.next = null;
      }

      void close() {
         if (tail != null) {
            tail.next = null;
         }
      }

      int size() {
         int cnt = 0;
         for (ContactVariable var = head; var != null; var = var.next) {
            cnt++;
         }
         return cnt;
      }
   }

   // input parameters for the computation

   ContactInfo[] contactInfo;
   SpatialInertia myInertia;
   Wrench wapplyAdjusted = new Wrench();

   // items related to inverse mass or the equivalent

   private double vdamping = 1.0; // FIX XXX replace with mass matrix
   private double wdamping = 1.0; // FIX XXX
   // private double vdroot;
   // private double wdroot;

   // dimensions associated with the computation

   int numc; // number of contacts
   int numd; // number of friction directions
   int numVars; // number of variables in use (excluding z0)
   int numActiveVars; // number of active variables in use (excluding z0)
   int msize; // size of problem matrix (without cover vector)
   int ksize; // current size of kernel

   // items for debugging and testing

   private boolean cycleCheck = false;

   // variable lists and special variables

   Wrench[] NDBuffer = new Wrench[0];
   ContactVariable[] zVars = new ContactVariable[0];
   ContactVariable[] wVars = new ContactVariable[0];

   VariableList thetaAlpha = new VariableList();
   VariableList phiAlphaP = new VariableList();
   VariableList phiAlphaX = new VariableList();
   VariableList wBeta = new VariableList();
   VariableList wAlpha = new VariableList();

   ContactVariable wzVar = null;
   ContactVariable kernGamma = null;
   ContactVariable z0Var = null;

   ContactVariable[] phiXArray = new ContactVariable[0];
   int[] frictionStatus = new int[0];

   int[] varToActive = new int[0];
   int[] activeToVar = new int[0];

   // variables used in kernel computation

   LUDecomposition lud = new LUDecomposition (6);
   MatrixNd Mk = new MatrixNd (MAX_KSIZE, MAX_KSIZE);
   VectorNd bk = new VectorNd (MAX_KSIZE);
   VectorNd zk = new VectorNd (MAX_KSIZE);

   Wrench[] NDz = new Wrench[MAX_KSIZE];
   Wrench[] NDw = new Wrench[MAX_KSIZE];
   double[] ca = new double[MAX_KSIZE];
   double[] phiSum = new double[0];

   Wrench cstar = new Wrench();
   Wrench bstar = new Wrench();

   boolean z0InKernel;

   // variables used in pivot computations

   double[] qv = new double[0];
   double[] bq = new double[0];
   double[] mv = new double[0];
   double[] iv = new double[0];

   double[] b = new double[0];
   double[] c = new double[0];

   Wrench vc = new Wrench();
   Wrench vx = new Wrench();
   Wrench wtotalAdjusted = new Wrench();

   Twist currentVel = new Twist();

   int[] candidates = new int[0];
   int[] origCandidates = new int[0];

   // scratch variables

   RotationMatrix3d R = new RotationMatrix3d();
   Vector3d xdir = new Vector3d();
   Vector3d ydir = new Vector3d();
   Vector3d vtmp = new Vector3d();

   public LemkeContactSolver() {
      for (int i = 0; i < NDz.length; i++) {
         NDz[i] = new Wrench();
      }
      // XXX compute this
      epsilon = 2.7194799110210365E-13;
   }

   private void setSize (int npnts, int ndirs) {
      int NDsize = npnts * (1 + ndirs);

      numc = npnts;
      numd = frictionEnabledP ? ndirs : 0;
      numVars = npnts;
      numActiveVars = npnts;
      msize = npnts * (2 + ndirs);

      if (NDBuffer.length < NDsize) {
         NDBuffer =
            (Wrench[])growObjectArray (
               NDBuffer, NDsize, new Wrench().getClass());
      }

      super.allocateSpace (msize);

      if (qv.length < msize) {
         Class varClass = new ContactVariable().getClass();
         int oldLength;

         oldLength = wVars.length;
         wVars = (ContactVariable[])growObjectArray (wVars, msize, varClass);
         for (int i = oldLength; i < wVars.length; i++) {
            wVars[i].setIndex(i);
         }
         oldLength = zVars.length;
         zVars =
            (ContactVariable[])growObjectArray (zVars, msize + 1, varClass);
         for (int i = oldLength; i < zVars.length; i++) {
            zVars[i].setIndex(i);
         }

         qv = new double[msize];
         bq = new double[msize];
         mv = new double[msize];
         iv = new double[msize];

         c = new double[msize];
         b = new double[msize];

         candidates = new int[msize];
         origCandidates = new int[msize];

         varToActive = new int[msize];
         activeToVar = new int[msize];
      }

      if (phiXArray.length < numc) {
         phiXArray = new ContactVariable[numc];
         phiSum = new double[numc];
         frictionStatus = new int[numc];
      }
   }

   private void initialize (
      ContactInfo[] contacts, int ncontacts, SpatialInertia inertia,
      Wrench wapplied, int ndirs) {
      Point3d pnt = new Point3d();

      setSize (ncontacts, ndirs);
      myInertia = inertia;

      // vdroot = Math.sqrt(vdamping);
      // wdroot = Math.sqrt(wdamping);

      contactInfo = contacts;

      if (myInertia != null) {
         myInertia.mulLeftFactorInverse (wapplyAdjusted, wapplied);
      }

      // wapplyAdjusted.f.scale (1/vdroot, wapplied.f);
      // wapplyAdjusted.m.scale (1/wdroot, wapplied.m);
      // System.out.println ("wapp=" + wapplyAdjusted.toString("%8.3f"));
      wtotalAdjusted.set (wapplyAdjusted);

      // initialize points, contact normals, and first variables
      for (int i = 0; i < numc; i++) {
         Wrench N = NDBuffer[i];
         N.f.set (contactInfo[i].nrm);
         N.m.cross (contactInfo[i].pnt, contactInfo[i].nrm);

         bq[i] = contactInfo[i].normalVelocityLimit;

         if (myInertia != null) {
            myInertia.mulLeftFactorInverse (N, N);
         }

         zVars[i].init (THETA, i, -1, wVars[i], N);
         wVars[i].init (NU, i, -1, zVars[i], N);
         frictionStatus[i] = FRICTION_INACTIVE;

         // qv[i] = N.dot(wapplyAdjusted);
         c[i] = 1;

         varToActive[i] = i;
         activeToVar[i] = i;
      }
      zVars[numc].init (Z0, -1, -1, null, null);
      z0Var = zVars[numc];
   }

   private void activateFriction (int ci) {
      // boolean z0isBasic = z0Var.isBasic;

      // System.out.println ("activating friction " + ci);

      zVars[numVars + numd + 1].set (z0Var);
      z0Var = zVars[numVars + numd + 1];

      int di = numVars;

      if (frictionStatus[ci] == FRICTION_INACTIVE) { // first time friction
                                                      // activated, so compute
                                                      // friction directions
         Point3d pnt = contactInfo[ci].pnt;

         // set x direction to be parallel to current tangent velocity
         computeVelocity (currentVel, wtotalAdjusted); // current body
                                                         // velocity
         // compute tangent velocity
         vtmp.cross (currentVel.w, pnt);
         vtmp.add (currentVel.v);

         ydir.cross (vtmp, contactInfo[ci].nrm);
         double mag = ydir.norm();
         if (smartDirections && mag >= WORKING_PREC) {
            ydir.scale (1 / mag);
            xdir.cross (ydir, contactInfo[ci].nrm);
            xdir.normalize();
         }
         // else
         {
            R.setIdentity();
            R.setZDirection (contactInfo[ci].nrm);
            R.getColumn (0, xdir);
            R.getColumn (1, ydir);
         }
         for (int j = 0; j < numd; j++) {
            double cos = Math.cos (2 * Math.PI * j / (double)numd);
            double sin = Math.sin (2 * Math.PI * j / (double)numd);

            Wrench D = NDBuffer[numc + ci * numd + j];
            D.f.combine (cos, xdir, sin, ydir);
            D.m.cross (pnt, D.f);
            bq[di + j] = D.dot (contactInfo[ci].otherBodyVelocity);

            if (myInertia != null) {
               myInertia.mulLeftFactorInverse (D, D);
            }
         }
      }

      for (int j = 0; j < numd; j++) {
         Wrench D = NDBuffer[numc + ci * numd + j];
         int idx = di + j;
         zVars[idx].init (PHI, ci, di, wVars[idx], D);
         wVars[idx].init (SIGMA, ci, di, zVars[idx], D);
         c[idx] = 1;
      }

      int idx = di + numd;

      zVars[idx].init (LAMBDA, ci, di, wVars[idx], null);
      wVars[idx].init (GAMMA, ci, di, zVars[idx], null);
      c[idx] = 1;
      bq[idx] = 0;

      zVars[ci].di = di;
      wVars[ci].di = di;

      for (int j = 0; j < numd + 1; j++) {
         varToActive[numVars + j] = numActiveVars + j;
         activeToVar[numActiveVars + j] = numVars + j;
      }

      frictionStatus[ci] = FRICTION_ACTIVE;

      numVars += (numd + 1);
      numActiveVars += (numd + 1);
   }

   ContactVariable getGamma (ContactVariable var) {
      return var.di != -1 ? wVars[var.di + numd] : null;
   }

   boolean frictionActive (ContactVariable var) {
      return frictionStatus[var.ci] == FRICTION_ACTIVE;
   }

   ContactVariable getBasic (int idx) {
      if (numVars != numActiveVars) {
         idx = activeToVar[idx];
      }
      if (zVars[idx].isBasic) {
         return zVars[idx];
      }
      else {
         return wVars[idx];
      }
   }

   private void partitionVariables (ContactVariable drive) {
      thetaAlpha.clear();
      phiAlphaP.clear();
      phiAlphaX.clear();
      wBeta.clear();
      wAlpha.clear();

      kernGamma = null;

      ksize = 0;
      if (drive == null) {
         wzVar = null;
         z0InKernel = false;
      }
      else {
         wzVar = drive.isZ() ? (ContactVariable)drive.complement : drive;
         if (wzVar.type == GAMMA) {
            kernGamma = wzVar;
         }
         z0InKernel = true;
         ksize++;
      }

      // XXX diagnostic
      // int[] phiCnt = new int[numc];

      for (int i = 0; i < numc; i++) {
         ContactVariable theta = zVars[i];
         ContactVariable nu = wVars[i];

         phiXArray[i] = null;

         if (theta.isBasic) {
            thetaAlpha.add (theta);
            wAlpha.add (nu);
            ksize++;
         }
         else if (nu.isBasic) {
            wBeta.add (nu);
         }

         if (frictionStatus[i] == FRICTION_ACTIVE) {
            ContactVariable lambda = zVars[theta.di + numd];
            ContactVariable gamma = wVars[theta.di + numd];

            for (int j = 0; j < numd; j++) {
               ContactVariable phi = zVars[theta.di + j];
               ContactVariable sigma = wVars[theta.di + j];

               if (phi.isBasic) {
                  if (lambda.isBasic && phiXArray[i] == null) {
                     phiAlphaX.add (phi);
                     phiXArray[i] = phi;
                  }
                  else {
                     phiAlphaP.add (phi);
                     ksize++;
                  }
                  wAlpha.add (sigma);
                  // phiCnt[i]++;
               }
               else if (sigma.isBasic) {
                  wBeta.add (sigma);
               }
            }
            if (lambda.isBasic) {
               wAlpha.add (gamma);
               if (phiXArray[i] == null) { // sanity check
                  if (kernGamma != null) {
                     throw new InternalErrorException (
                        "zero col lambda not unique: contacts " + kernGamma.ci
                        + " and " + lambda.ci);
                  }
                  if (wzVar == null || lambda.ci != wzVar.ci ||
                      wzVar.type != SIGMA) {
                     throw new InternalErrorException (
                        "zero col lambda not associated with sigma, contact"
                        + lambda.ci);
                  }
                  kernGamma = (ContactVariable)lambda.complement;
               }
            }
            else if (gamma.isBasic) {
               wBeta.add (gamma);
            }
         }
      }

      if (wzVar != null) {
         wAlpha.add (wzVar);
      }

      String caseName = "PRINCIPAL";
      if (wzVar != null) {
         if (wzVar.type == NU) {
            caseName = "NU";
         }
         else if (wzVar.type == SIGMA && kernGamma == null) {
            caseName = "SIGMA non-zero";
         }
         else if (wzVar.type == SIGMA) {
            caseName = "SIGMA zero";
         }
         else if (wzVar.type == GAMMA) {
            caseName = "GAMMA";
         }
         else {
            throw new InternalErrorException (
               "bogus type: " + wzVar.getName());
         }
      }

      thetaAlpha.close();
      phiAlphaP.close();
      phiAlphaX.close();
      wBeta.close();
      wAlpha.close();

      // XXX diagnostic
      // for (int i=0; i<numc; i++)
      // { if (phiCnt[i] > 2)
      // { System.out.print ("XXX phiCnt=" + phiCnt[i]);
      // if (wzVar == null)
      // { System.out.print (" bbbqZZZ");
      // }
      // System.out.println ("");
      // }
      // }

      if (false) {
         System.out.println ("ta:" + thetaAlpha.size() + " pa:"
         + phiAlphaP.size() + " px:" + phiAlphaX.size() + " ksize:" + ksize
         + " " + caseName);
      }

      if (ksize == 8) {
         printBasis (drive);
      }
   }

   private void computeStarVector (
      Wrench xstar, double[] xa, double[] x, Wrench[] NDw) {
      if (xstar != null) {
         for (ContactVariable phi = phiAlphaX.head; phi != null; phi = phi.next) {
            xstar.scaledAdd (x[phi.di + numd], phi.nd, xstar);
         }
      }
      int kz = 0;
      for (ContactVariable the = thetaAlpha.head; the != null; the = the.next) {
         xa[kz] = x[the.idx];
         if (xstar != null) {
            xa[kz] += NDw[kz].dot (xstar);
         }
         kz++;
      }
      for (ContactVariable phi = phiAlphaP.head; phi != null; phi = phi.next) {
         xa[kz] = x[phi.idx];
         if (xstar != null) {
            xa[kz] += NDw[kz].dot (xstar);
         }
         if (phiXArray[phi.ci] != null) {
            xa[kz] -= x[phiXArray[phi.ci].idx];
         }
         kz++;
      }
      if (kernGamma != null) {
         xa[kz] = x[kernGamma.idx];
      }
      else if (wzVar != null) {
         xa[kz] = x[wzVar.idx];
         if (xstar != null) {
            xa[kz] += NDw[kz].dot (xstar);
         }
         if (wzVar.type == SIGMA && phiXArray[wzVar.ci] != null) {
            xa[kz] -= x[phiXArray[wzVar.ci].idx];
         }
      }
   }

   public void computePivotKernel() {
      Mk.setSize (ksize, ksize);
      bk.setSize (ksize);
      zk.setSize (ksize);

      int kz = 0;
      for (ContactVariable the = thetaAlpha.head; the != null; the = the.next) {
         int ci = the.ci;
         double mu = contactInfo[ci].mu;
         NDz[kz].set (the.nd);
         if (phiXArray[ci] != null) {
            NDz[kz].scaledAdd (mu, phiXArray[ci].nd, NDz[kz]);
         }
         if (kernGamma != null) {
            Mk.set (ksize - 1, kz, kernGamma.ci == ci ? mu : 0);
         }
         NDw[kz++] = the.nd;
      }
      for (ContactVariable phi = phiAlphaP.head; phi != null; phi = phi.next) {
         int ci = phi.ci;
         NDz[kz].set (phi.nd);
         if (phiXArray[ci] != null) {
            NDz[kz].sub (phiXArray[ci].nd);
         }
         if (kernGamma != null) {
            Mk.set (ksize - 1, kz, kernGamma.ci == ci ? -1 : 0);
         }
         NDw[kz] = NDz[kz];
         kz++;
      }

      int kw = kz;
      if (wzVar != null && kernGamma == null) {
         NDz[kz].set (wzVar.nd);
         if (wzVar.type == SIGMA && phiXArray[wzVar.ci] != null) {
            NDz[kz].sub (phiXArray[wzVar.ci].nd);
         }
         NDw[kw++] = NDz[kz];
      }

      if (z0InKernel) {
         cstar.setZero();
         computeStarVector (cstar, ca, c, NDw);
      }

      for (int i = 0; i < kw; i++) {
         for (int j = 0; j < kz; j++) {
            Mk.set (i, j, NDw[i].dot (NDz[j]));
         }
         if (z0InKernel) {
            Mk.set (i, kz, ca[i]);
         }
      }
      if (kernGamma != null) {
         Mk.set (ksize - 1, ksize - 1, ca[ksize - 1]);
      }
      // System.out.println ("Mk=\n" + Mk.toString("%8.3f"));

      // for (ContactVariable the=thetaAlpha.head; the!=null; the=the.next)
      // { System.out.println ("N =" + the.nd.toString("%8.3f"));
      // }
      // for (ContactVariable phi=phiAlphaX.head; phi!=null; phi=phi.next)
      // { System.out.println ("DX =" + phi.nd.toString("%8.3f"));
      // }
      // int k = 0;
      // for (ContactVariable the=thetaAlpha.head; the!=null; the=the.next)
      // { System.out.println ("N* =" + NDz[k++].toString("%8.3f"));
      // }
      // for (ContactVariable phi=phiAlphaP.head; phi!=null; phi=phi.next)
      // { System.out.println ("D* =" + NDz[k++].toString("%8.3f"));
      // }
      // System.out.println ("Mk=\n" + Mk.toString("%9.6f"));
      lud.factor (Mk);
      conditionEstimate = lud.conditionEstimate (Mk);
      // System.out.println ("condEst=" + conditionEstimate);
   }

   protected boolean conditionEstimateAvailable() {
      return true;
   }

   protected double getConditionEstimate() {
      return conditionEstimate;
   }

   public void solveColumn (double[] y, Wrench vc, double[] b, Wrench bstar) {
      double z0 = 0;

      computeStarVector (bstar, bk.getBuffer(), b, NDw);

      lud.solve (zk, bk);

      // System.out.println ("bk=" + bk.toString("%8.3f"));
      // System.out.println ("zk=" + zk.toString("%8.3f"));

      if (z0InKernel) {
         z0 = zk.get (ksize - 1);
         y[wzVar.idx] = z0;
         vc.scale (z0, cstar);
      }
      else {
         vc.setZero();
      }

      for (int i = 0; i < numc; i++) // XXX is there a faster way?
      {
         phiSum[i] = 0;
      }

      if (bstar != null) {
         vc.sub (bstar);
      }

      int kz = 0;
      // set theta and phi values
      for (ContactVariable the = thetaAlpha.head; the != null; the = the.next) {
         double value = zk.get (kz);
         vc.scaledAdd (value, NDz[kz++], vc);
         y[the.idx] = value;
      }
      for (ContactVariable phi = phiAlphaP.head; phi != null; phi = phi.next) {
         double value = zk.get (kz);
         vc.scaledAdd (value, NDz[kz++], vc);
         phiSum[phi.ci] += value;
         y[phi.idx] = value;
      }
      for (ContactVariable phi = phiAlphaX.head; phi != null; phi = phi.next) {
         int lami = phi.di + numd;
         y[phi.idx] = -phiSum[phi.ci] - b[lami] + c[lami] * z0;
         ContactVariable the = zVars[phi.ci];
         if (the.isBasic) {
            y[phi.idx] += contactInfo[the.ci].mu * y[the.idx];
         }
         phiSum[phi.ci] += y[phi.idx]; // XXX
      }
      // compute lambda values
      for (ContactVariable phi = phiAlphaX.head; phi != null; phi = phi.next) {
         y[phi.di + numd] = -phi.nd.dot (vc) + b[phi.idx] - c[phi.idx] * z0;
      }
      if (kernGamma != null && wzVar.type == SIGMA) {
         ContactVariable phi = (ContactVariable)wzVar.complement;
         y[phi.di + numd] = -phi.nd.dot (vc) + b[phi.idx] - c[phi.idx] * z0;
      }

      for (ContactVariable wvar = wBeta.head; wvar != null; wvar = wvar.next) {
         if (wvar.type == NU) {
            y[wvar.idx] = wvar.nd.dot (vc) + c[wvar.idx] * z0;
         }
         else if (wvar.type == SIGMA) {
            y[wvar.idx] = wvar.nd.dot (vc) + c[wvar.idx] * z0;
            if (zVars[wvar.di + numd].isBasic) // lambda is basic
            {
               y[wvar.idx] += y[wvar.di + numd];
            }
         }
         else // wvar.type == GAMMA
         {
            y[wvar.idx] = c[wvar.idx] * z0 - phiSum[wvar.ci];
            if (zVars[wvar.ci].isBasic) // theta is basic
            {
               y[wvar.idx] += contactInfo[wvar.ci].mu * y[wvar.ci];
            }
         }
      }
   }

   private void compressColumn (double[] y) {
      // int idx0 = numc;
      // int idx1 = numc;
      // // System.out.println ("before:");
      // // for (int i=0; i<numVars; i++)
      // // { System.out.println (y[i]);
      // // }
      // for (int i=0; i<numc; i++)
      // { if (frictionStatus[i] == FRICTION_ACTIVE)
      // { for (int j=0; j<numd+1; j++)
      // { // System.out.println (idx0 + " <- " + idx1);
      // y[idx0++] = y[idx1++];
      // }
      // }
      // else if (frictionStatus[i] == FRICTION_DORMANT)
      // { idx1 += (numd+1);
      // }
      // }
      // // System.out.println ("after:");
      // // for (int i=0; i<numActiveVars; i++)
      // // { System.out.println (y[i]);
      // // }
      for (int i = 0; i < numActiveVars; i++) {
         y[i] = y[activeToVar[i]];
      }
   }

   public void computeMvCol (double[] y, ContactVariable var) {
      // clear b
      for (ContactVariable wvar = wAlpha.head; wvar != null; wvar = wvar.next) {
         b[wvar.idx] = 0;
      }
      if (var.isW()) {
         b[var.idx] = 1;
         bstar.setZero();
         solveColumn (y, vx, b, var.type == GAMMA ? bstar : null);
      }
      else if (var.type == THETA) {
         if (frictionActive (var) && !getGamma (var).isBasic) {
            b[var.di + numd] = -contactInfo[var.ci].mu;
         }
         bstar.negate (var.nd);
         solveColumn (y, vx, b, bstar);
         if (frictionActive (var) && getGamma (var).isBasic) {
            y[var.di + numd] += contactInfo[var.ci].mu;
         }
      }
      else if (var.type == PHI) {
         if (frictionActive (var) && !getGamma (var).isBasic) {
            b[var.di + numd] = 1;
         }
         bstar.negate (var.nd);
         solveColumn (y, vx, b, bstar);
         if (frictionActive (var) && getGamma (var).isBasic) {
            y[var.di + numd] -= 1;
         }
      }
      else if (var.type == LAMBDA) {
         if (frictionActive (var)) {
            for (int j = 0; j < numd; j++) {
               if (!wVars[var.di + j].isBasic) {
                  b[var.di + j] = -1;
               }
            }
         }
         solveColumn (y, vx, b, null);
         if (frictionActive (var)) {
            for (int j = 0; j < numd; j++) {
               if (wVars[var.di + j].isBasic) {
                  y[var.di + j] += 1;
               }
            }
         }
      }
      else if (var.type == Z0) {
         bstar.setZero();
         for (ContactVariable wvar = wAlpha.head; wvar != null; wvar =
            wvar.next) {
            b[wvar.idx] = -c[wvar.idx];
         }
         solveColumn (y, vx, b, bstar);
         for (ContactVariable wvar = wBeta.head; wvar != null; wvar = wvar.next) {
            y[wvar.idx] += c[wvar.idx];
         }
      }
      else {
         throw new InternalErrorException ("Unknown variable type, var="
         + var.getName());
      }
      if (numVars != numActiveVars) {
         compressColumn (y);
      }
   }

   public boolean checkQv (double[] qv) {
      // double max = -1;
      // for (int i=0; i<numVars; i++)
      // { double abs = qv[i];
      // if (abs < 0)
      // { abs = -abs;
      // }
      // if (abs > max)
      // { max = abs;
      // }
      // }
      double worstQ = Double.POSITIVE_INFINITY;
      for (int i = 0; i < numVars; i++) {
         if (qv[i] < worstQ || qv[i] != qv[i]) {
            worstQ = qv[i];
         }
      }
      if (worstQ != worstQ) {
         if ((debug & (SHOW_STATS)) != 0) {
            System.out.println ("Complementarity loss: " + worstQ);
         }
         return false;
      }
      else if (worstQ < 0) {
         if ((debug & (SHOW_STATS)) != 0) {
            System.out.println ("Complementarity loss: " + worstQ);
         }
         if (worstQ < -epsilon) {
            return false;
         }
      }
      return true;
   }

   public void computeQv (double[] qv) {
      // clear b. Not used any more since we use bq instead
      // for (ContactVariable wvar=wAlpha.head; wvar!=null; wvar=wvar.next)
      // { b[wvar.idx] = 0;
      // }
      bstar.negate (wapplyAdjusted);
      solveColumn (qv, wtotalAdjusted, bq, bstar);
      for (ContactVariable wvar = wBeta.head; wvar != null; wvar = wvar.next) {
         qv[wvar.idx] -= bq[wvar.idx];
      }
      if (numVars != numActiveVars) {
         compressColumn (qv);
      }
   }

   private ContactVariable[] getZAlphaVars() {
      int numBasicZ = 0;
      for (int i = 0; i < numVars; i++) {
         if (zVars[i].isBasic) // && frictionActive(zVars[i]))
         {
            numBasicZ++;
         }
      }
      if (z0Var.isBasic) {
         numBasicZ++;
      }
      ContactVariable[] zAlpha = new ContactVariable[numBasicZ];
      int k = 0;

      // theta variables
      for (int i = 0; i < numc; i++) {
         if (zVars[i].isBasic) {
            zAlpha[k++] = zVars[i];
         }
      }
      // phi variables
      for (int i = 0; i < numc; i++) {
         if (frictionStatus[i] == FRICTION_ACTIVE) {
            int di = zVars[i].di;
            for (int j = 0; j < numd; j++) {
               if (zVars[di + j].isBasic) {
                  zAlpha[k++] = zVars[di + j];
               }
            }
         }
      }
      // lambda variables
      for (int i = 0; i < numc; i++) {
         if (frictionStatus[i] == FRICTION_ACTIVE) {
            int di = zVars[i].di;
            if (zVars[di + numd].isBasic) {
               zAlpha[k++] = zVars[di + numd];
            }
         }
      }
      if (z0Var.isBasic) {
         zAlpha[k++] = z0Var;
      }
      return zAlpha;
   }

   private int compressIndex (int idx) {
      int dec = 0;
      for (int i = numc; i < idx; i += (numd + 1)) {
         if (!frictionActive (wVars[i])) {
            dec += (numd + 1);
         }
      }
      return idx - dec;
   }

   private int uncompressIndex (int idx) {
      if (idx < numc) {
         return idx;
      }
      else {
         int fblkTarget = (idx - numc) / (numd + 1);
         int inc = 0;
         int fblk = 0;
         for (int i = numc; i < numVars; i += (numd + 1)) {
            if (!frictionActive (wVars[i])) {
               inc += (numd + 1);
            }
            else if (fblk++ == fblkTarget) {
               break;
            }
         }
         return idx + inc;
      }
   }

   // private void printStepInfo (
   // String msg, double[] mv, double[] qv, ContactVariable driveVar,
   // ContactVariable blockingVar, int[] candidates, int numCand)
   // {
   // int[] indices = new int[numActiveVars];
   // Variable[] vars = new Variable[numActiveVars];

   // // int dec = 0;
   // // for (int i=0; i<numVars; i++)
   // // { if (wVars[i].di == i && !frictionActive(wVars[i]))
   // // { dec += numd+1;
   // // i += numd;
   // // }
   // // else if (wVars[i].isBasic)
   // // { vars[i-dec] = wVars[i];
   // // }
   // // else if (zVars[i].isBasic)
   // // { vars[i-dec] = zVars[i];
   // // }
   // // else
   // // { vars[i-dec] = z0Var;
   // // }
   // // }
   // // System.out.println ("dec=" + dec);
   // for (int i=0; i<numActiveVars; i++)
   // { int vari = activeToVar[i];
   // if (wVars[vari].isBasic)
   // { vars[i] = wVars[vari];
   // }
   // else if (zVars[vari].isBasic)
   // { vars[i] = zVars[vari];
   // }
   // else
   // { vars[i] = z0Var;
   // }
   // }
   // int i = 0;

   // // ContactVariable[] zAlpha = getZAlphaVars();
   // // int i = 0;
   // // for (int k=0; k<zAlpha.length; k++)
   // // { indices[i++] = zAlpha[k] == z0Var ? wzVar.idx : zAlpha[k].idx;
   // // }
   // // for (ContactVariable wvar=wBeta.head; wvar!=null; wvar=wvar.next)
   // // { indices[i++] = wvar.idx;
   // // }
   // for (i=0; i<numActiveVars; i++)
   // { indices[i] = i;
   // }
   // // if (numVars != numActiveVars)
   // // { for (i=0; i<numActiveVars; i++)
   // // { indices[i] = compressIndex (indices[i]);
   // // System.out.println (indices[i]);
   // // }
   // // }
   // printStepInfo (msg, vars, mv, qv, numActiveVars, driveVar,
   // blockingVar, candidates, numCand, indices);
   // }

   // public int solve (Twist vel, Point3d[] pnts, Vector3d[] nrms,
   // double[] mus, int ncontacts,
   // SpatialInertia inertia, Twist tw)
   // {
   // ContactInfo[] cinfo = new ContactInfo[ncontacts];
   // for (int i=0; i<ncontacts; i++)
   // { ContactInfo c = new ContactInfo();
   // c.mu = mus[i];
   // c.pnt = pnts[i];
   // c.nrm = nrms[i];
   // cinfo[i] = c;
   // }
   // Wrench wapplied = new Wrench();
   // inertia.mul (wapplied, tw);
   // return solve (vel, cinfo, ncontacts, inertia,
   // wapplied, numFrictionDirs);
   // }

   /**
    * Applies an impulse to a rigid body velocity to adjust for normal and
    * frictional contact forces.
    * 
    * @param tr
    * velocity with contact impulse added
    * @param contacts
    * provides information about the each contact
    * @param ncontacts
    * number of contacts
    * @param inertia
    * spatial inertia of the rigid body
    * @param tw
    * initial velocity of the rigid body
    * @param restitution
    * coefficent of restitution
    */
   public int solve (
      Twist tr, ContactInfo[] contacts, int ncontacts, SpatialInertia inertia,
      Twist tw, double restitution) {
      if (ncontacts == 0) {
         tr.set (tw);
         return SOLVED;
      }
      Wrench wapplied = new Wrench();
      inertia.mul (wapplied, tw);
      return solve (tr, contacts, ncontacts, inertia, wapplied, restitution);
   }

   // public int solve (Twist tr, Contact[] contacts, int ncontacts,
   // SpatialInertia inertia, Wrench wapplied, int ndirs)
   // {
   // return solve (tr, (ContactInfo[])contacts, ncontacts,
   // inertia, wapplied, ndirs);
   // }

   public int solve (
      Twist tr, ContactInfo[] contacts, int ncontacts, SpatialInertia inertia,
      Wrench wapplied, double restitution) {
      int numCand;
      int origNumCand = 0;
      ContactVariable driveVar;
      ContactVariable blockingVar;

      int ndirs = numFrictionDirs;

      if (ncontacts == 0) {
         if (inertia != null) {
            inertia.mulInverse (tr, wapplied);
         }
         else {
            tr.set (wapplied);
         }
         return SOLVED;
      }

      initialize (contacts, ncontacts, inertia, wapplied, ndirs);

      // checkVariables();

      int pivotCnt = 0;
      int maxPivotCnt =
         (pivotLimit == AUTOMATIC_PIVOT_LIMIT ? msize * msize : pivotLimit);

      if (!variableSize && numd > 0) {
         for (int i = 0; i < numc; i++) {
            if (contacts[i].mu != 0) {
               activateFriction(i); // do this now for starters
            }
         }
      }
      // initialize q

      for (int i = 0; i < numVars; i++) {
         if (wVars[i].type != GAMMA) {
            qv[i] = wVars[i].nd.dot (wapplyAdjusted);
         }
         else {
            qv[i] = 0;
         }
         qv[i] -= bq[i];
      }
      partitionVariables (null);

      for (int i = 0; i < numVars; i++) {
         mv[i] = -c[i];
      }

      int imin = lexicoMinRatioTest (mv, qv, numActiveVars, -1, true);
      if (imin == -1) { // then q >= 0 and so z = 0 is a solution
         computeVelocity (tr, wapplyAdjusted);
         return SOLVED;
      }

      if ((debug & SHOW_COLS) != 0) {
         System.out.println ("Initial:");
      }

      if (wVars[imin].isBasic) {
         driveVar = pivot (wVars[imin], z0Var);
      }
      else {
         driveVar = pivot (zVars[imin], z0Var);
      }

      // if (cycleCheck)
      // { currentBasis.init (nonBasicVars[r]);
      // }

      while (pivotCnt < maxPivotCnt) {
         // find the blocking variable from the r-th column
         computeMvCol (mv, driveVar);

         if ((debug & (SHOW_COLS | SHOW_BASIS)) != 0) {
            System.out.println ("Basis: " + basisString (driveVar));
         }
         if ((debug & SHOW_STATS) != 0) {
            System.out.println ("epsilon=" + epsilon);
            System.out.println ("driving variable=" + driveVar.getName());
            System.out.println ("numActiveVars=" + numActiveVars);
            System.out.println ("conditionEstimate=" + conditionEstimate);
         }
         boolean pivotOK = true;
         clearRejectedPivots (numVars);
         do {
            int s = lexicoMinRatioTest (mv, qv, numActiveVars, wzVar.idx,
            /* initial= */false);
            if (s == -1) { // then driving variable is unblocked, so we can't
               // find a solution
               System.out.println ("unbounded ray");
               tr.setZero();
               return UNBOUNDED_RAY;
            }
            else if (s == wzVar.idx) { // zx blocks the driving variable
               pivot (z0Var, driveVar);
               if ((debug & SHOW_COLS) != 0) {
                  printQv ("Final:", qv, numVars);
               }
               computeVelocity (tr, wtotalAdjusted);
               if (restitution != 0) {
                  addRestitution (tr, restitution);
               }
               return SOLVED;
            }
            else {
               ContactVariable leftPivot = getBasic (s);
               // printPredictedQv ("Predicted q:", mv, qv, s,
               // driveVar, numVars);
               ContactVariable newDriveVar = pivot (leftPivot, driveVar);
               pivotOK = checkPivotsP ? checkQv (qv) : true;
               if (!pivotOK) {
                  if (debug != 0) {
                     System.out.println ("REJECTED pivot "
                     + leftPivot.getName());
                  }
                  // checkPivotComputations (newDriveVar);
                  if ((debug & SHOW_COLS) != 0) {
                     printQv ("Bad Q:", qv, numVars);
                  }
                  rejectPivot (s);
                  pivot (driveVar, leftPivot);
                  cumulativePivotCnt -= 2;
               }
               else {
                  driveVar = newDriveVar;
               }
            }
         }
         while (!pivotOK);
         pivotCnt++;
      }

      tr.setZero();
      return PIVOT_LIMIT_EXCEEDED;
   }

   protected String basisString (Variable driveVar) {
      ContactVariable[] zAlpha = getZAlphaVars();

      String s = "";
      for (int i = 0; i < zAlpha.length; i++) {
         s += " " + zAlpha[i].getName();
      }
      if (z0Var.isBasic) {
         s += " (";
         if (driveVar != null) {
            s += driveVar.getName();
         }
         else {
            s += "null";
         }
         s += ")";
      }
      return s;
   }

   private void adjustCoverVector (double[] qv, int newActiveContact, double z0) {
      int baseIdx = zVars[newActiveContact].di;
      int activeBase = baseIdx;
      if (numVars != numActiveVars) {
         activeBase = varToActive[baseIdx];
      }
      double delcMax = 0;

      for (int i = 0; i < numd + 1; i++) {
         if (qv[activeBase + i] < 0) {
            double delc = -2 * qv[activeBase + i] / z0;
            if (delc > delcMax) {
               delcMax = delc;
            }
         }
      }
      if (delcMax != 0) {
         for (int i = 0; i < numd + 1; i++) {
            c[baseIdx + i] += delcMax;
            qv[activeBase + i] += delcMax * z0;
         }
      }
   }

   private double Melem (int i, int j) {
      ContactVariable wvar = wVars[i];
      ContactVariable zvar = zVars[j];
      if (zvar.type == Z0) {
         return c[wvar.idx];
      }
      else if (wvar.nd != null && zvar.nd != null) {
         return wvar.nd.dot (zvar.nd);
      }
      else if (zvar.type == LAMBDA) {
         if (wvar.type == SIGMA && wvar.ci == zvar.ci) {
            return 1;
         }
         else {
            return 0;
         }
      }
      else if (wvar.type == GAMMA) {
         if (zvar.type == THETA && wvar.ci == zvar.ci) {
            return contactInfo[zvar.ci].mu;
         }
         else if (zvar.type == PHI && wvar.ci == zvar.ci) {
            return -1;
         }
         else {
            return 0;
         }
      }
      else {
         throw new InternalErrorException (
            "unknown element at i=" + i + ", j=" + j);
      }
   }

   private void exchangeRows (MatrixNd M, int i1, int i2) {
      double[] row1 = new double[M.colSize()];
      double[] row2 = new double[M.colSize()];

      M.getRow (i1, row1);
      M.getRow (i2, row2);
      M.setRow (i2, row1);
      M.setRow (i1, row2);
   }

   private void exchangeColumns (MatrixNd M, int j1, int j2) {
      double[] col1 = new double[M.rowSize()];
      double[] col2 = new double[M.rowSize()];

      M.getColumn (j1, col1);
      M.getColumn (j2, col2);
      M.setColumn (j2, col1);
      M.setColumn (j1, col2);
   }

   private void checkPivotComputations (ContactVariable drive) {
      MatrixNd Mv = new MatrixNd (numActiveVars, numActiveVars + 1);
      MatrixNd MvCheck = new MatrixNd (numActiveVars, numActiveVars + 1);

      // first, compute Mv using the normal routines of this class
      double[] mvcol = new double[numActiveVars];
      int asize = 0;
      for (int i = 0; i < numActiveVars; i++) {
         int vari = activeToVar[i];
         if (zVars[vari].isBasic) {
            computeMvCol (mvcol, wVars[vari]);
            System.out.println (zVars[vari].getName());
            asize++;
         }
         else {
            computeMvCol (mvcol, zVars[vari]);
         }
         Mv.setColumn (i, mvcol);
      }
      if (z0Var.isBasic) {
         computeMvCol (mvcol, wzVar);
         Mv.setColumn (numActiveVars, mvcol);
         asize++;
      }
      else {
         computeMvCol (mvcol, z0Var);
         Mv.setColumn (numActiveVars, mvcol);
      }

      if (asize == 0) {
         return;
      }

      // Next, compute Mv using a general method
      int bsize = numActiveVars - asize;
      MatrixNd Maa = new MatrixNd (asize, asize);
      MatrixNd Mba = new MatrixNd (bsize, asize);
      MatrixNd Mab = new MatrixNd (asize, bsize + 1);
      MatrixNd Mbb = new MatrixNd (bsize, bsize + 1);

      MatrixNd Mvaa = new MatrixNd (asize, asize);
      MatrixNd Mvba = new MatrixNd (bsize, asize);
      MatrixNd Mvab = new MatrixNd (asize, bsize + 1);
      MatrixNd Mvbb = new MatrixNd (bsize, bsize + 1);

      // System.out.println ("asize=" + asize + " bsize=" + bsize);

      int[] zaIndices = new int[asize];
      int[] waIndices = new int[asize];
      int[] zbIndices = new int[bsize + 1];
      int[] wbIndices = new int[bsize];

      int iza = 0;
      int iwa = 0;
      int izb = 0;
      int iwb = 0;
      int kwzi = -1;
      for (int j = 0; j < numActiveVars; j++) {
         int idx = activeToVar[j];
         if (zVars[idx].isBasic) {
            zaIndices[iza++] = j;
         }
         else {
            zbIndices[izb++] = j;
         }
      }
      if (z0Var.isBasic) {
         zaIndices[iza++] = numActiveVars;
      }
      else {
         zbIndices[izb++] = numActiveVars;
      }
      for (int i = 0; i < numActiveVars; i++) {
         int vari = activeToVar[i];
         if (!wVars[vari].isBasic) {
            if (wVars[vari] == wzVar) {
               kwzi = iwa;
            }
            waIndices[iwa++] = i;
         }
         else {
            wbIndices[iwb++] = i;
         }
      }

      MatrixNd M = new MatrixNd (numActiveVars, numActiveVars + 1);
      for (int i = 0; i < numActiveVars; i++) {
         for (int j = 0; j < numActiveVars; j++) {
            M.set (i, j, Melem (activeToVar[i], activeToVar[j]));
         }
         M.set (i, numActiveVars, Melem (activeToVar[i], numActiveVars));
      }

      M.getSubMatrix (waIndices, zaIndices, Maa);
      M.getSubMatrix (waIndices, zbIndices, Mab);
      M.getSubMatrix (wbIndices, zaIndices, Mba);
      M.getSubMatrix (wbIndices, zbIndices, Mbb);

      System.out.println ("Maa=\n" + Maa.toString ("%9.6f"));

      Mvaa.invert (Maa);
      Mvab.mul (Mvaa, Mab);
      Mvab.negate();
      if (bsize != 0) {
         Mvba.mul (Mba, Mvaa);
         Mvbb.mul (Mba, Mvab);
         Mvbb.add (Mbb);
      }

      // compute q values

      iwa = 0;
      iwb = 0;
      VectorNd qa = new VectorNd (asize);
      VectorNd qb = new VectorNd (bsize);
      VectorNd qva = new VectorNd (asize);
      VectorNd qvb = new VectorNd (bsize);
      double[] qvcol = new double[numActiveVars];
      for (int i = 0; i < numVars; i++) {
         if (wVars[i].type != GAMMA) {
            qvcol[i] = wVars[i].nd.dot (wapplyAdjusted) - bq[i];
         }
         else {
            qvcol[i] = -bq[i];
         }
      }

      for (iwa = 0; iwa < asize; iwa++) {
         qa.set (iwa, qvcol[waIndices[iwa]]);
      }
      for (iwb = 0; iwb < bsize; iwb++) {
         qb.set (iwb, qvcol[wbIndices[iwb]]);
      }

      Mvaa.mul (qva, qa);
      qva.negate();
      Mba.mul (qvb, qva);
      qvb.add (qb);

      if (wzVar != null) {
         zaIndices[asize - 1] = wzVar.idx;
         waIndices[kwzi] = numActiveVars;
      }
      MvCheck.setSubMatrix (zaIndices, waIndices, Mvaa);
      MvCheck.setSubMatrix (zaIndices, zbIndices, Mvab);
      MvCheck.setSubMatrix (wbIndices, waIndices, Mvba);
      MvCheck.setSubMatrix (wbIndices, zbIndices, Mvbb);

      VectorNd qvCheck = new VectorNd (numActiveVars);
      for (iza = 0; iza < asize; iza++) {
         qvCheck.set (zaIndices[iza], qva.get (iza));
      }
      for (iwb = 0; iwb < bsize; iwb++) {
         qvCheck.set (wbIndices[iwb], qvb.get (iwb));
      }

      double mtol = MvCheck.infinityNorm() * 1e-12;
      double qtol = MvCheck.infinityNorm() * 1e-12;

      VectorNd qvVec = new VectorNd (numActiveVars);
      qvVec.set (qv);
      VectorNd qInit = new VectorNd (numActiveVars);
      qInit.set (qvcol);

      // System.out.println ("cond=" +
      // (Mvaa.infinityNorm()*Maa.infinityNorm()));

      if (!Mv.epsilonEquals (MvCheck, mtol) |
          !qvVec.epsilonEquals (qvCheck, qtol)) {
         System.out.println (
            "Error, pivot check failed, see mvcheck.m for matrices");
         printBasis (drive);
         try {
            PrintStream fps =
               new PrintStream (new BufferedOutputStream (new FileOutputStream (
                  "mvcheck.m")));
            fps.println ("M=[\n" + M.toString ("%9.6f") + "];");
            fps.println ("Mv=[\n" + Mv.toString ("%9.6f") + "];");
            fps.println ("MvCheck=[\n" + MvCheck.toString ("%9.6f") + "];");
            fps.println ("Maa=[\n" + Maa.toString ("%18.12f") + "];");
            fps.println ("Mab=[\n" + Mab.toString ("%9.6f") + "];");
            fps.println ("Mba=[\n" + Mba.toString ("%9.6f") + "];");
            fps.println ("Mbb=[\n" + Mbb.toString ("%9.6f") + "];");
            fps.println ("q=[" + qInit.toString ("%9.6f") + "];");
            fps.println ("qa=[" + qa.toString ("%9.6f") + "];");
            fps.println ("qb=[" + qb.toString ("%9.6f") + "];");
            fps.println ("qv=[" + qvVec.toString ("%9.6f") + "];");
            fps.println ("qvCheck=[" + qvCheck.toString ("%9.6f") + "];");
            fps.close();
         }
         catch (Exception e) {
            e.printStackTrace();
         }
         MvCheck.sub (Mv);
         System.out.println ("M error: " + MvCheck.infinityNorm());
         qvCheck.sub (qvVec);
         System.out.println ("q error: " + qvCheck.infinityNorm());
      }
   }

   private ContactVariable pivot (
      ContactVariable dropping, ContactVariable entering) {
      ContactVariable newDrive = null;
      int newActiveContact = -1;
      entering.isBasic = true;
      dropping.isBasic = false;

      newDrive = (ContactVariable)dropping.complement;

      if (dropping.type == NU &&
          !frictionActive (dropping) &&
          contactInfo[dropping.ci].mu != 0 &&
          numd > 0) {
         newActiveContact = dropping.ci;
      }
      // if (dropping.type == THETA && allFrictionBasic(dropping.ci))
      // {
      // if (reducibleSize)
      // { // System.out.println ("deactivating friction " + dropping.ci);
      // deactivateFriction (dropping.ci);
      // }
      // }
      // if (entering.type == THETA && entering.di == -1)

      if (newActiveContact != -1) {
         if (frictionStatus[newActiveContact] == FRICTION_INACTIVE) {
            activateFriction (newActiveContact);
         }
         // else
         // { reactivateFriction (newActiveContact);
         // }
      }

      partitionVariables (newDrive);

      computePivotKernel();
      computeQv (qv);

      if (newActiveContact != -1 && wzVar != null) {
         int idx = wzVar.idx;
         if (numVars != numActiveVars) {
            idx = varToActive[idx];
         }
         double z0 = qv[idx];
         adjustCoverVector (qv, newActiveContact, z0);
      }

      // checkPivotComputations(newDrive);
      if (ksize == 8) { // checkPivotComputations(newDrive);
         System.out.println ("ksize=8");
      }

      cumulativePivotCnt++;
      return newDrive;
   }

   private void computeVelocity (Twist vel, Wrench wr) {
      vel.set (wr);
      if (myInertia != null) {
         myInertia.mulRightFactorInverse (vel, vel);
      }
   }

   private void addRestitution (Twist vel, double restitution) {
      // compute the velocity impulse that was applied along all the
      // normals
      Wrench fnorm = new Wrench();
      Twist vnorm = new Twist();
      int kz = 0;
      for (ContactVariable the = thetaAlpha.head; the != null; the = the.next) {
         double value = zk.get (kz++);
         fnorm.scaledAdd (value, the.nd, fnorm);
      }
      vnorm.set (fnorm);
      if (myInertia != null) {
         myInertia.mulRightFactorInverse (vnorm, vnorm);
      }
      vel.scaledAdd (restitution, vnorm, vel);
   }

   protected Variable getWzVar() {
      return wzVar;
   }

   protected Variable[] getBasicVars() {
      Variable[] vars = new Variable[numActiveVars];

      for (int i = 0; i < numActiveVars; i++) {
         int vari = activeToVar[i];
         if (wVars[vari].isBasic) {
            vars[i] = wVars[vari];
         }
         else if (zVars[vari].isBasic) {
            vars[i] = zVars[vari];
         }
         else {
            vars[i] = z0Var;
         }
      }
      return vars;
   }

   protected boolean wIsBasic (int j) {
      return wVars[activeToVar[j]].isBasic;
   }

   protected void getBasisColumn (double[] iv, int j) {
      computeMvCol (iv, wVars[activeToVar[j]]);
   }

   public String getConfigString() {
      return ("variableSize=" + variableSize + "\n" + "numFrictionDirs="
      + numFrictionDirs + "\n" + "frictionEnabled=" + frictionEnabledP + "\n"
      + "epsilon=" + epsilon + "\n" + "cycleCheckingEnabled="
      + cycleCheckingEnabled + "\n" + "maxCycleCheckSize=" + maxCycleCheckSize
      + "\n" + "maxBasisHistory=" + maxBasisHistory);
   }

}
