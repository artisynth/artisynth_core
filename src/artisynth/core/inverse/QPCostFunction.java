package artisynth.core.inverse;

import java.util.ArrayList;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.solvers.DantzigQPSolver;
import maspack.solvers.DantzigQPSolver.Status;

public class QPCostFunction {
   /*
    * Terms for the quadratic program to solve:
    * min { x^T*Q*x + x^T*L }, subject to A*x <= b, Aeq*x = beq
    */
   protected int mySize;
   protected VectorNd x = new VectorNd();
   
   protected ArrayList<QPTerm> myCostTerms = new ArrayList<QPTerm>();
   protected MatrixNd Q = new MatrixNd();
   protected VectorNd P = new VectorNd();
   
   protected ArrayList<LeastSquaresTerm> 
      myInequalityTerms = new ArrayList<LeastSquaresTerm>();
   protected MatrixNd A = new MatrixNd();
   protected VectorNd b = new VectorNd();
   
   protected ArrayList<LeastSquaresTerm> 
      myEqualityTerms = new ArrayList<LeastSquaresTerm>();
   protected MatrixNd Aeq = new MatrixNd();
   protected VectorNd beq = new VectorNd();
   
   /*
    * Default solver
    */
   DantzigQPSolver mySolver = new DantzigQPSolver();
   
   /*
    * Constructors
    */
   public QPCostFunction () {
   }
   
   public QPCostFunction (int size) {
      setSize(size);
   }
   
   public void addCostTerm(QPTerm term) {
      term.setSize (mySize);
      myCostTerms.add (term);
   }
   
   public void addInequalityConstraint(LeastSquaresTerm term) {
      myInequalityTerms.add (term);
      int rows = A.rowSize() + term.getRowSize();
      A.setSize (rows,mySize);
      b.setSize (rows);
   }
   
   public void addEqualityConstraint(LeastSquaresTerm term) {
      myEqualityTerms.add (term);
      int rows = Aeq.rowSize() + term.getRowSize();
      Aeq.setSize (rows,mySize);
      beq.setSize (rows);
   }
   
   public void dispose () {
      for (QPTerm term : myCostTerms) {
         term.dispose();
      }
      myCostTerms.clear ();
      for (QPTerm term : myInequalityTerms) {
         term.dispose();
      }
      myInequalityTerms.clear ();
      for (QPTerm term : myEqualityTerms) {
         term.dispose();
      }
      myEqualityTerms.clear ();
   }
   
   /**
    * Solves the Quadratic Program of the form:
    * min { x^T*Q*x + x^T*L }, subject to A*x &lt;= b, Aeq*x = beq
    * @return x
    */
   public VectorNd solve(double t0, double t1) {
      /*
       * Collect all cost terms
       */
      Q.setZero ();
      P.setZero ();
      for (QPTerm term : myCostTerms) {
         term.getQP (Q,P,t0,t1);
      }
      int rowoff = 0;
      for (LeastSquaresTerm term : myInequalityTerms) {
         if (term.isEnabled ())
            term.getTerm (A,b,rowoff,t0,t1);
      }
      rowoff = 0;
      for (LeastSquaresTerm term : myEqualityTerms) {
         if (term.isEnabled ())
            term.getTerm (Aeq,beq,rowoff,t0,t1);
      }    
      
      /*
       * Solve QP problem
       */
      try {
         if (Aeq.rowSize () == 0 || beq.size () == 0) {
            mySolver.solve (x,Q,P,A,b);
         }
         else {
            Status qpStatus = mySolver.solve (x, Q, P, A, b, Aeq, beq);
            if (qpStatus != Status.SOLVED) {
               System.err.println("InverseSolve failed: solver status = "+qpStatus.toString ());
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return x;
   }
   
   /**
    * Sets the size of the quadratic program
    * Note that constraints terms will be removed.
    * @param size
    */
   public void setSize(int size) {
      if (size != mySize) {
         resize(size);
         mySize = size;
      }
   }
   
   /**
    * Resizes the quadratic program and removes all constraints
    * @param size
    */
   private void resize (int size) {
      x.setSize(size);
      
      for (QPTerm term : myCostTerms) {
         term.setSize(size);
      }
      Q.setSize(size, size);
      P.setSize(size);
      
      int rowSize = 0;     
      for (LeastSquaresTerm term : myEqualityTerms) {
         term.setSize(size);
         rowSize += term.getRowSize ();
      }
      Aeq.setSize (rowSize,size);
      beq.setSize (rowSize);
      
      rowSize = 0;
      for (LeastSquaresTerm term : myInequalityTerms) {
         term.setSize(size);
         rowSize += term.getRowSize ();
      }
      A.setSize (rowSize, size);
      b.setSize (rowSize);
   }
    
   public ArrayList<QPTerm> getCostTerms() {
      return myCostTerms;
   }
   
   public ArrayList<LeastSquaresTerm> getEqualityConstraints() {
      return myEqualityTerms;
   }
   
   public ArrayList<LeastSquaresTerm> getInequalityConstraints() {
      return myInequalityTerms;
   }
}
