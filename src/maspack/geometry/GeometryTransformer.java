package maspack.geometry;

import java.util.*;

import maspack.matrix.*;

/**
 * Implements geometric transformations associated with a generalized
 * deformation mapping, under which points <code>p</code> are mapped
 * to deformed points <code>p'</code> using a function
 * <pre>
 * p' = f (p)
 * </pre>
 * The mapping is assumed to be piece-wise differentiable, so
 * that each point <code>p</code> is also associated with a
 * deformation gradient <code>F</code>.
 *
 * The transformations can be applied to a variety of primitives, including
 * points, vectors, planes, meshes, and transformation matrices. Sub-classes
 * provide different types of transformations, include rigid, affine, and
 * non-linear.
 *
 * <p>The transformations themselves are effected with a set of
 * <code>transform()</code> methods, one for each primitive type.  Primitives
 * of type {@link Point3d} and {@link Vector3d} are transformed using
 * primitives named <code>transformVec()</code> and
 * <code>transformPnt()</code>, to help resolve ambiguities between point and
 * vector transformations.
 *
 * <p>The transformer is able to save and restore the previous values associated
 * with each transformation, so that its transform actions can later be
 * "undone". Storing of save information is activated by calling
 * <code>setUndoState(UndoState.SAVING)</code>, after which each transform
 * operation will save the original value of each primitive. Restoring of this
 * information is then activated by calling
 * <code>setUndoState(UndoState.RESTORING)</code>, after which each transform
 * operation will restore the original primitive value instead of performing
 * the actual transformation. For example:
 * <pre>
 *   GeometryTransformer gtr;
 *   Vector3d v1, v2;
 *   Plane plane;
 *   
 *   ... initialize variables ...
 *   
 *   // start saving undo information
 *   gtr.setUndoState (UndoState.SAVING); 
 *   gtr.transformVec (v1);  
 *   gtr.transformVec (v2);  // transform v1, v2, and plane
 *   gtr.transform (plane)
 *
 *   // restore undo information
 *   gtr.setUndoState (UndoState.RESTORING); 
 *   gtr.transformVec (v1);                
 *   gtr.transformVec (v2);   // restore v1, v2, and plane to their
 *   gtr.transform (plane)    // original values
 * </pre>
 * When restoring undo information, operations must be called on the same
 * primitives in the same order as was used during the original transform. It
 * is the responsibility of the calling application to ensure this. The caller
 * may also save and restore additional application-specific information using
 * the primitives {@link #saveObject} and {@link #restoreObject}.
 * 
 * @author John E Lloyd
 */
public abstract class GeometryTransformer {
   
   private static double MACH_PREC = 1e-16;
   
   /**
    * Describes the current undo state of this transformer.
    */
   public enum UndoState { 

      /**
       * No undo information is being saved or restored.
       */
      OFF, 

      /**
       * Undo information is being saved.
       */

      SAVING, 

      /**
       * Undo information is being restored.
       */
      RESTORING };
   
   private UndoState myUndoState = UndoState.OFF;
   private ArrayDeque<Object> myUndoData;

   /**
    * Implements constraint operations on rigid or affine transform objects.
    */
   public interface Constrainer {

      /**
       * Modfies the value of <code>X</code>, if necessary, to satisfy some
       * constraint.
       */
      public void apply (AffineTransform3dBase X); 
      
      /**
       * Returns true if the constrainer transform is reflecting. In
       * most cases this method will likely return false.
       */
      public boolean isReflecting();
   }

   /**
    * Constrains an affine transform so that it results in
    * uniform scaling with no translation
    */
   public static class UniformScalingConstrainer implements Constrainer {
      
      public UniformScalingConstrainer() {
      }
      
      public void apply (AffineTransform3dBase X) {

         if (X instanceof AffineTransform3d) {
            if (!X.equals (AffineTransform3d.IDENTITY)) {
            
               AffineTransform3d XA = (AffineTransform3d)X;
               Matrix3d A = new Matrix3d (XA.A);
            
               // factor A into the polar decomposition A = Q P,
               // then constrain P to be a uniform scaling transform
               // with the same volume change
               PolarDecomposition3d pd = new PolarDecomposition3d(A);
               Matrix3d P = pd.getP();
               double s = Math.pow (Math.abs(P.determinant()), 1/3.0);
               A.setDiagonal (s, s, s);
               A.mul (pd.getQ(), A);
               
               XA.A.set(A);
               XA.p.setZero();               
            }
         }
         else if (X instanceof RigidTransform3d) {
            ((RigidTransform3d)X).set (RigidTransform3d.IDENTITY);
         }
      }
      
      public boolean isReflecting() {
         return false;
      }    
   }

   /**
    * Sets the undo state of this transformer so as to enable it save or
    * restore the original data form transformation operations.
    * 
    * @param state new undo state
    */
   public void setUndoState (UndoState state) {
      if (state != myUndoState) {
         switch (state) {
            case OFF: {
               myUndoData = null;
               break;
            }
            case SAVING: {
               myUndoData = new ArrayDeque<Object>();
               break;
            }
            case RESTORING: {
               if (myUndoData == null) {
                  throw new IllegalStateException (
                     "No undo state has been saved");
               }
               break;
            }
         }
         myUndoState = state;
      }
   }
   
   /**
    * Returns <code>true</code> if the undo state of this transformer is set to
    * restore primitive values.
    * 
    * @return true if this transformer is restoring values.
    */
   public boolean isRestoring() {
      return myUndoState == UndoState.RESTORING;
   }
   
   /**
    * Returns <code>true</code> if the undo state of this transformer is set to
    * save primitive values.
    * 
    * @return true if this transformer is saving values.
    */
   public boolean isSaving() {
      return myUndoState == UndoState.SAVING;
   }

   protected <T> T restore (T obj) {
      if (myUndoState != UndoState.RESTORING) {
         throw new ImproperStateException ("Undo state is not set to RESTORING");
      }
      else if (myUndoData.size() == 0) {
         throw new ImproperStateException ("No undo information remaining");
      }
      else {
         try {
            return (T)myUndoData.removeFirst();
         }
         catch (ClassCastException e) {
            throw new ImproperStateException (
               "Undo information not of type " + obj.getClass());             
         }
      }
   }

   protected void save (Object obj){
      if (myUndoState != UndoState.SAVING) {
         throw new ImproperStateException ("Undo state is not set to SAVING");
      }
      else {
         myUndoData.addLast (obj);
      }
   }

   /**
    * Returns application-specific data (that was previously set using {@link
    * #saveObject}) if the undo state of this transformer is set to {@link
    * UndoState#RESTORING}. Otherwise, an exception is thrown.
    * 
    * @param obj any object with the same data type as the object to be
    * restored. Used only for compile-time type checking and will not be
    * modified.
    * @return original data value
    */
   public <T> T restoreObject (T obj) {
      return restore (obj);
   }
   
   /**
    * Saves application-specific data if the undo state of this transformer is
    * set to {@link UndoState#SAVING}. Otherwise, an exception is thrown.
    * 
    * @param obj object to be saved.
    */
   public void saveObject (Object obj) {
      save (obj);
   }

   /**
    * Removes <code>cnt</code> items of restore data if the undo state of this
    * transformer is set to {@link UndoState#RESTORING}. Otherwise, an
    * exception is thrown.
    * 
    * @param cnt number of restore items to remove.
    */
   public void popRestoreData (int cnt) {
      if (myUndoState != UndoState.RESTORING) {
         throw new ImproperStateException ("Undo state is not set to RESTORING");
      }
      if (myUndoData.size() < cnt) {
         throw new ImproperStateException ("Not enough undo information");
      }
      while (cnt-- > 0) {
         myUndoData.removeFirst();
      }      
   }
   
   /**
    * Returns <code>true</code> if this transformer implements a linear
    * rigid transform.
    */
   public abstract boolean isRigid();

   /**
    * Returns <code>true</code> if this transformer implements a linear affine
    * transform.
    */
   public abstract boolean isAffine();

   /**
    * Returns <code>true</code> if this transformer globally implements a 
    * reflection (i.e., the determinant of the deformation gradient
    * at all transformation points is negative). The default declaration
    * of this method returns <code>false</code>. Subclasses should
    * override this if necessary.
    */
   public boolean isReflecting() {
      return false;
   }

   /**
    * Returns <code>true</code> if this transformer is invertible. If it is,
    * then an inverse transformer can be obtained using {@link #getInverse}.
    */
   public abstract boolean isInvertible();
   
   /**
    * Returns a transformer that implements the inverse operation of
    * this transformer, if {@link #isInvertible} returns <code>true</code>.
    * 
    * @return inverse transformer
    */
   public abstract GeometryTransformer getInverse();

   /**
    * Transforms a point <code>p</code>, in place.
    * This is equivalent to <code>transformPnt(p,p)</code>.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>p</code> is instead set to its original
    * value that was previously saved when the undo state was set to {@link
    * UndoState#SAVING}.
    *
    * @param p point to be transformed
    */
   public void transformPnt (Point3d p) {
      transformPnt (p, p);
   }

   /**
    * Transforms a point <code>p</code> and returns the result in
    * <code>pr</code>.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>pr</code> is instead set to the value of
    * <code>p</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param pr transformed point
    * @param p point to be transformed
    */
   public void transformPnt (Point3d pr, Point3d p) {
      if (isRestoring()) {
         Point3d px = restore (pr);
         pr.set (px);
         return;
      }
      if (isSaving()) {
         save (new Point3d(p));
      }
      computeTransformPnt (pr, p);
   }
   
   /**
    * Transforms a point <code>p</code> and returns the result in
    * <code>pr</code>. This provides the low level implementation for point
    * transformations and does not do any saving or restoring of data.
    * 
    * @param pr transformed point
    * @param p point to be transformed
    */
   public abstract void computeTransformPnt (Point3d pr, Point3d p);

   /**
    * Transforms a vector <code>v</code>, located at reference position
    * <code>r</code>, in place.
    * This is equivalent to <code>transformVec(v,v,r)</code>.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>v</code> is instead set to its original
    * value that was previously saved when the undo state was set to {@link
    * UndoState#SAVING}.
    *
    * @param v vector to be transformed
    * @param r reference position of the vector, in original coordinates
    */
   public void transformVec (Vector3d v, Vector3d r) {
      transformVec (v, v, r);
   }

   /**
    * Transforms a vector <code>v</code>, located at reference position
    * <code>r</code>, and returns the result in <code>vr</code>.
    * Generally, this transformation will take the form
    * <pre>
    * vr = F v
    * </pre>
    * where <code>F</code> is the deformation gradient at the reference
    * position.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>vr</code> is instead set to the value of
    * <code>v</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param vr transformed vector
    * @param v vector to be transformed
    * @param r reference position of the vector, in original coordinates
    */
   public void transformVec (Vector3d vr, Vector3d v, Vector3d r) {
      if (isRestoring()) {
         vr.set (restore (vr));
         return;
      }
      if (isSaving()) {
         save (new Vector3d(v));
      }
      computeTransformVec (vr, v, r);
   }
   
   /**
    * Transforms a vector <code>v</code>, located at reference position
    * <code>r</code>, and returns the result in <code>vr</code>. This
    * provides the low level implementation for vector transformations and does
    * not do any saving or restoring of data.
    *
    * @param vr transformed vector
    * @param v vector to be transformed
    * @param r reference position of the vector, in original coordinates
    */
   public abstract void computeTransformVec (
      Vector3d vr, Vector3d v, Vector3d r);

   /**
    * Transforms a normal vector <code>n</code>, located at reference position
    * <code>r</code>, in place. This is equivalent to <code>transformNormal(n,
    * n)</code>.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>n</code> is instead set to its original
    * value that was previously saved when the undo state was set to {@link
    * UndoState#SAVING}.
    *
    * @param n normal to be transformed
    * @param r reference position of the normal, in original coordinates
    */
   public void transformNormal (Vector3d n, Vector3d r) {
      transformNormal (n, n, r);
   }

   /**
    * Transforms a normal vector <code>n</code>, located at reference position
    * <code>r</code>, and returns the result in <code>nr</code>.
    * Generally, this transformation will take the form
    * <pre>
    *       -1 T
    * nr = F     n
    * </pre>
    * where <code>F</code> is the deformation gradient at the reference
    * position. The result is <i>not</i> normalized since
    * the unnormalized form could be useful in some contexts.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>nr</code> is instead set to the value of
    * <code>n</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param nr transformed normal
    * @param n normal to be transformed
    * @param r reference position of the normal, in original coordinates
    */
   public void transformNormal (Vector3d nr, Vector3d n, Vector3d r) {
      if (isRestoring()) {
         nr.set (restore (nr));
         return;
      }
      if (isSaving()) {
         save (new Vector3d(n));
      }
      computeTransformNormal (nr, n, r);
   }
   
   /**
    * Transforms a normal vector <code>n</code>, located at reference position
    * <code>r</code>, and returns the result in <code>nr</code>. This
    * provides the low level implementation for normal transformations and does
    * not do any saving or restoring of data.
    *
    * @param nr transformed normal
    * @param n normal to be transformed
    * @param r reference position of the normal, in original coordinates
    */
   public abstract void computeTransformNormal (
      Vector3d nr, Vector3d n, Vector3d r);

   /**
    * Transforms a rigid transform <code>T</code>, in place.
    * This is equivalent to <code>transform(T,T)</code>.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>T</code> is instead set to its original
    * value that was previously saved when the undo state was set to {@link
    * UndoState#SAVING}.
    *
    * @param T rigid transform to be transformed.
    */
   public void transform (RigidTransform3d T) {
      transform (T, T);
   }

   /**
    * Transforms a rigid transform <code>T</code> and returns the
    * result in <code>TR</code>. If
    * <pre>
    *      [  R   p  ]
    * T =  [         ]
    *      [  0   1  ]
    * </pre>
    * and <code>f(p)</code> and <code>F</code> are the deformed position
    * and deformation gradient at <code>p</code>, then 
    * generally this transform takes the form
    * <pre>
    *      [  Q R N   f(p) ]
    * TR = [               ]
    *      [    0      1   ]
    * </pre>
    * where <code>Q</code> is the orthogonal matrix from the left polar
    * decomposition <code>F = P Q</code>, and <code>N</code> is matrix that
    * flips an axis to ensure that <code>Q R N</code> remains right-handed.
    * If <code>det(Q) = 1</code>, then <code>N</code> is the identity, while if
    * <code>det(Q) = -1</code>, then <code>N</code> is typically choosen to
    * correspond to the axis least affected by <code>Q</code>, which is the one
    * corresponding to the diagonal of <code>Q</code> nearest to 1.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>TR</code> is instead set to the value of
    * <code>T</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param TR transformed transform
    * @param T transform to be transformed
    */
   public void transform (RigidTransform3d TR, RigidTransform3d T) {
      if (isRestoring()) {
         TR.set (restore (TR));
         return;
      }
      if (isSaving()) {
         save (new RigidTransform3d(T));
      }
      computeTransform (TR, T);
      //computeTransform (TR, T);
   }
   
   public void computeTransform (AffineTransform3dBase X) {
      if (X instanceof RigidTransform3d) {
         computeTransform ((RigidTransform3d)X, (RigidTransform3d)X);         
      }
      else if (X instanceof AffineTransform3d) {
         computeTransform ((AffineTransform3d)X, (AffineTransform3d)X);         
      }
      else {
         throw new UnsupportedOperationException (
            "doTransform() not implemented for type "+X.getClass());
      }
   }
   
   /**
    * Transforms a rigid transform <code>T</code> and returns the result in
    * <code>TR</code>. This provides the low level implementation for the
    * transformation of rigid transforms and does not do any saving or
    * restoring of data.
    *
    * @param TR transformed transform
    * @param T transform to be transformed
    */
   protected void computeTransform (
      RigidTransform3d TR, RigidTransform3d T) {
      computeTransform (TR.R, null, T.R, T.p);
      Point3d p = new Point3d(T.p);
      computeTransformPnt (p, p);
      TR.p.set (p);
   }
   
   /**
    * Transforms an affine transform <code>X</code>, in place.
    * This is equivalent to <code>transform(X,X)</code>.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>X</code> is instead set to its original
    * value that was previously saved when the undo state was set to {@link
    * UndoState#SAVING}.
    *
    * @param X affine transform to be transformed.
    */
   public void transform (AffineTransform3d X) {
      transform (X, X);
   }

   /**
    * Transforms an affine transform <code>X</code> and returns the
    * result in <code>XR</code>. If
    * <pre>
    *     [  A   p  ]
    * X = [         ]
    *     [  0   1  ]
    * </pre>
    * then generally, this transform takes the form
    * <pre>
    *      [  F A   f(p) ]
    * XR = [             ]
    *      [   0     1   ]
    * </pre>
    * where <code>F</code> is the deformation gradient
    * at <code>p</code>, and <code>f(p)</code> is the deformed
    * position of <code>p</code>.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>XR</code> is instead set to the value of
    * <code>X</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param XR transformed transform
    * @param X transform to be transformed
    */
   public void transform (
      AffineTransform3d XR, AffineTransform3d X) {
      if (isRestoring()) {
         XR.set (restore (XR));
         return;
      }
      if (isSaving()) {
         save (new AffineTransform3d(X));
      }
      computeTransform (XR, X);
   }

   /**
    * Transforms an affine transform <code>X</code> and returns the result in
    * <code>XR</code>. This provides the low level implementation for the
    * transformation of affine transforms and does not do any saving or
    * restoring of data.
    * 
    * @param XR transformed transform
    * @param X transform to be transformed
    */
   public abstract void computeTransform (
      AffineTransform3d XR, AffineTransform3d X);

   /**
    * Transforms a rotation matrix <code>R</code>, located at reference
    * position <code>r</code>, in place.
    * This is equivalent to <code>transform(R,R,r)</code>.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>R</code> is instead set to its original
    * value that was previously saved when the undo state was set to {@link
    * UndoState#SAVING}.
    *
    * @param R rotation to be transformed
    * @param r reference position of the rotation, in original coordinates
    */
   public void transform (RotationMatrix3d R, Vector3d r) {
      transform (R, R, r);
   }

   /**
    * Transforms a rotation matrix <code>R</code>, located at reference
    * position <code>r</code>, and returns the result in <code>RR</code>.
    * If <code>F</code> is the deformation gradient at <code>r</code>,
    * this transform takes the form
    * <pre>
    * RR = Q R N 
    * </pre>
    * where <code>Q</code> is the orthogonal matrix from the left polar
    * decomposition <code>F = P Q</code>, and <code>N</code> is matrix that
    * flips an axis to ensure that <code>Q R N</code> remains right-handed.
    * If <code>det(Q) = 1</code>, then <code>N</code> is the identity, while if
    * <code>det(Q) = -1</code>, then <code>N</code> is typically choosen to
    * correspond to the axis least affected by <code>Q</code>, which is the one
    * corresponding to the diagonal of <code>Q</code> nearest to 1.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>RR</code> is instead set to the value of
    * <code>R</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param RR transformed rotation
    * @param R rotation to be transformed
    * @param r reference position of the rotation, in original coordinates
    */
   public void transform (
      RotationMatrix3d RR, RotationMatrix3d R, Vector3d r) {
      if (isRestoring()) {
         RR.set (restore (RR));
         return;
      }
      if (isSaving()) {
         save (new RotationMatrix3d(R));
      }
      computeTransform (RR, null, R, r);      
   }

   /**
    * Transforms a rotation matrix <code>R</code>, located at reference
    * position <code>r</code>, and returns the result in <code>RR</code>,
    * according to
    * <pre>
    * RR = Q R N
    * </pre>
    * where <code>P</code>, <code>Q</code> and <code>N</code> are described
    * in the documentation for {@link
    * #transform(RotationMatrix3d,RotationMatrix3d,Vector3d)}.
    *
    * <p>This provides the low level implementation for the transformation of
    * rotation matrices and does not do any saving or restoring of data.
    *
    * @param RR transformed rotation
    * @param R rotation to be transformed
    * @param r reference position of the rotation, in original coordinates
    * @param Ndiag if non-null, returns the diagonal elements of the
    * matrix <code>N</code>
    */
   public abstract void computeTransform (
      RotationMatrix3d RR, Vector3d Ndiag, RotationMatrix3d R, Vector3d r);

   /**
    * Transforms a general 3 X 3 matrix <code>M</code>, located at reference
    * position <code>r</code>, in place.
    * This is equivalent to <code>transform(M,M,r)</code>.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>M</code> is instead set to its original
    * value that was previously saved when the undo state was set to {@link
    * UndoState#SAVING}.
    *
    * @param M matrix to be transformed
    * @param r reference position of the matrix, in original coordinates
    */
   public void transform (Matrix3d M, Vector3d r) {
      transform (M, M, r);
   }

   /**
    * Transforms a general 3 X 3 matrix <code>M</code>, located at reference
    * position <code>r</code>, and returns the result in <code>MR</code>.
    * Generally, this transform takes the form
    * <pre>
    * MR = F M
    * </pre>
    * where <code>F</code> is the deformation gradient at the reference
    * position.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>MR</code> is instead set to the value of
    * <code>M</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param MR transformed matrix
    * @param M matrix to be transformed
    * @param r reference position of the matrix, in original coordinates
    */
   public void transform (Matrix3d MR, Matrix3d M, Vector3d r) {
      if (isRestoring()) {
         MR.set (restore (MR));
         return;
      }
      if (isSaving()) {
         save (new Matrix3d(M));
      }
      computeTransform (MR, M, r);            
   }
   
   /**
    * Transforms a general 3 X 3 matrix <code>M</code>, located at reference
    * position <code>r</code>, and returns the result in <code>MR</code>.
    * This provides the low level implementation for the transformation of 3 X
    * 3 matrices and does not do any saving or restoring of data.
    *
    * @param MR transformed matrix
    * @param M matrix to be transformed
    * @param r reference position of the matrix, in original coordinates
    */
   public abstract void computeTransform (Matrix3d MR, Matrix3d M, Vector3d r);

   /**
    * Transforms a plane <code>p</code>, located at reference
    * position <code>r</code>, in place.
    * This is equivalent to <code>transform(p,p,r)</code>.
    *  
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>p</code> is instead set to its original
    * value that was previously saved when the undo state was set to {@link
    * UndoState#SAVING}.
    *
    * @param p plane to be transformed
    * @param r reference position of the plane, in original coordinates
    */
   public void transform (Plane p, Vector3d r) {
      transform (p, p, r);
   }

   /**
    * Transforms a plane <code>p</code>, located at reference position
    * <code>r</code>, and returns the result in <code>pr</code>.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>pr</code> is instead set to the value of
    * <code>p</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param pr transformed plane
    * @param p plane to be transformed
    * @param r reference position of the plane, in original coordinates
    */
   public void transform (Plane pr, Plane p, Vector3d r) {
      if (isRestoring()) {
         pr.set (restore (pr));
         return;
      }
      if (isSaving()) {
         save (new Plane(p));
      }
      computeTransform (pr, p, r);            
   }

   /**
    * Transforms a plane <code>p</code>, located at reference position
    * <code>r</code>, and returns the result in <code>pr</code>. This
    * provides the low level implementation for the transformation of planes
    * and does not do any saving or restoring of data.
    *
    * @param pr transformed plane
    * @param p plane to be transformed
    * @param r reference position of the plane, in original coordinates
    */
   public abstract void computeTransform (Plane pr, Plane p, Vector3d r);

   private void restoreVertices (MeshBase mesh) {
      for (Vertex3d v : mesh.getVertices()) {
         v.pnt.set (restore (v.pnt));
      }
   }

   private void restoreMeshToWorld (MeshBase mesh) {
      RigidTransform3d TMW = new RigidTransform3d();
      TMW.set (restore (TMW));
      mesh.setMeshToWorld (TMW);
   }

   private void saveVertices (MeshBase mesh) {
      for (Vertex3d v : mesh.getVertices()) {
         save (new Point3d(v.pnt));
      }
   }

   private void saveNormals (MeshBase mesh) {
      // save all the normal info, indices and all, just in case
      int numNormals = mesh.numNormals();
      save (mesh.numNormals());
      if (numNormals > 0) {
         for (Vector3d n : mesh.getNormals()) {
            save (new Vector3d(n));
         }
         int[] indices = mesh.getNormalIndices();
         save (Arrays.copyOf(indices, indices.length));
      }
   }

   private void restoreNormals (MeshBase mesh) {
      // restore all the normal info, indices and all, just in case
      int numNormals = restore (new Integer(0));
      if (numNormals == 0) {
         mesh.setNormals (null, null);
      }
      else {
         ArrayList<Vector3d> normals = new ArrayList<Vector3d>(numNormals);
         Vector3d dummyVec = new Vector3d();
         for (int i=0; i<numNormals; i++) {
            normals.add (restore (dummyVec));
         }
         int[] indices = restore (new int[0]);
         mesh.setNormals (normals, indices);
      }
   }

   /**
    * Return an array of points, one for each normal, that can be used to
    * provide a reference position for transforming that normal. If for some
    * reason a normal cannot be associated with a vertex, the corresponding
    * point will be set to the origin.
    *
    * <p>Each reference point is computed as the weighted sum of the vertex
    * points associated with each normal.
    */
   private Point3d[] getNormalPointRefs (MeshBase mesh) { 
      Point3d[] refs = new Point3d[mesh.numNormals()];
      int[] cnts = new int[mesh.numNormals()];
      for (int ni=0; ni<refs.length; ni++) {
         refs[ni] = new Point3d();
      }
      int[] indexOffs = mesh.getFeatureIndexOffsets();
      int[] vertexIdxs = mesh.createVertexIndices();
      int[] normalIdxs = mesh.getNormalIndices();
      for (int fi=0; fi<indexOffs.length-1; fi++) {
         for (int idx=indexOffs[fi]; idx<indexOffs[fi+1]; idx++) {
            int ni = normalIdxs[idx];
            cnts[ni]++;
            refs[ni].add (mesh.getVertex(vertexIdxs[idx]).pnt);
         }
      }
      for (int ni=0; ni<refs.length; ni++) {
         if (cnts[ni] != 0) {
            refs[ni].scale (1.0/cnts[ni]);
         }
      }
      return refs;
   }

   /**
    * Applies a geometric transformation to the vertex positions of a mesh, in
    * local mesh coordinates. The topology of the mesh remains unchanged.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then the vertex positions are instead set to their
    * original values that were previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param mesh Mesh to be transformed
    */
   public void transform (MeshBase mesh) {
      transform (mesh, /*constrainer=*/null);
   }

   public void transform (MeshBase mesh, Constrainer constrainer) {
      // if there is a constrainer, apply it to the linearization of this
      // transform at the origin, and use the resulting affine transform XC to
      // transform the mesh.
      AffineTransform3dBase XC = null;
      if (constrainer != null) {
         XC = computeLinearizedTransform (Vector3d.ZERO);
         constrainer.apply (XC);
      }
      if (isRestoring()) {
         restoreVertices (mesh);
         if (mesh.hasExplicitNormals()) {
            restoreNormals (mesh);
         }
         else {
            mesh.clearNormals(); // regenerate on demand
         }
         mesh.notifyVertexPositionsModified();
         return;
      }
      if (isSaving()) {
         saveVertices (mesh);
         if (mesh.hasExplicitNormals()) {
            saveNormals (mesh);
         }
      }
      // transform normals first because we need unmodified vertex points
      if (mesh.hasExplicitNormals()) {
         ArrayList<Vector3d> normals = mesh.getNormals();
         if (normals != null) {
            Point3d[] refs = null;
            Matrix3d invA = null;
            if (XC == null) {
               refs = getNormalPointRefs(mesh); // reference points for normals
            }
            else {
               // invA is needed to transform normals
               if (XC instanceof RigidTransform3d) {
                  invA.transpose (((RigidTransform3d)XC).R);
               }
               else {
                  invA.invert (((AffineTransform3d)XC).A);
               }
            }
            for (int ni=0; ni<normals.size(); ni++) {
               Vector3d nrm = normals.get(ni);
               if (XC == null) {
                  // transform normal using this transformer
                  computeTransformNormal (nrm, nrm, refs[ni]);
               }
               else {
                  // transform normal using the constrained XC
                  invA.mulTranspose (nrm, nrm);
               }
               nrm.normalize();
            }
         }
      }
      else {
         mesh.clearNormals(); // regenerate on demand
      }
      for (Vertex3d v : mesh.getVertices()) {
         if (XC == null) {
            // transform point using this transformer
            computeTransformPnt (v.pnt, v.pnt);
         }
         else {
            // transform point using the constrained XC
            v.pnt.transform (XC);
         }
      }
      mesh.notifyVertexPositionsModified();
   }

   /**
    * Applies a geometric transformation to both the vertex positions of a mesh
    * and its mesh-to-world transform TMW, in world coordinates. The local
    * vertex positions <code>p</code> are modified to accommodate that of part
    * of the transformation not provided by the change to TMW. Specifically,
    * <pre> p' = TMWnew X (TMW p) </pre> where <code>X( )</code> indicates the
    * transform applied by this transformer and TMWnew is the transformed value
    * of <code>TMW</code>.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then the vertex positions and TMW are instead set
    * to their original values that were previously saved when the undo state
    * was set to {@link UndoState#SAVING}.
    *
    * @param mesh Mesh to be transformed
    */
   public void transformWorld (MeshBase mesh) {
      transformWorld (mesh, /*constrainer=*/null);
   }

   /**
    * Applies a geometric transformation to both the vertices of a mesh and
    * its mesh-to-world transform TMW, in world coordinates. The local
    * vertex positions <code>p</code> are modified to accommodate that part
    * of the transformation not provided by the change to TMW. If
    * a <code>constrainer</code> is supplied, then this change is
    * constrained to that obtained by applying the constrainer to
    * the local affine transform obtained at the origin of the mesh
    * coordinate system. Otherwise, this method behaves identically to
    * {@link #transformWorld(MeshBase)}.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then the vertex positions and TMW are instead set
    * to their original values that were previously saved when the undo state
    * was set to {@link UndoState#SAVING}.
    *
    * @param mesh Mesh to be transformed
    */
   public double transformWorld (MeshBase mesh, Constrainer constrainer) {

      double s = 1.0;
      RigidTransform3d TMW = mesh.getMeshToWorld();

      if (isRestoring()) {
         if (!isRigid()) {
            restoreVertices (mesh);
            if (mesh.hasExplicitNormals()) {
               restoreNormals(mesh);
            }
            else {
               mesh.clearNormals();
            }
         }
         restoreMeshToWorld (mesh);
         mesh.notifyVertexPositionsModified();
         return s;
      }
      RigidTransform3d TMWnew = new RigidTransform3d();
      computeTransform (TMWnew, TMW);

      if (isSaving()) {
         if (!isRigid()) {
            saveVertices (mesh);
            if (mesh.hasExplicitNormals()) {
               saveNormals(mesh);
            }
         }
         save (new RigidTransform3d(TMW));
      }
      if (!isRigid()) {
         if (isAffine() || constrainer != null) {
            
            // Adjust local vertices and normals using the local affine
            // transform XL, which accounts for the non-rigid parts of the
            // transform that cannot be accommodated by the mesh-to-world 
            // transform
            AffineTransform3d XL = 
               computeLocalAffineTransform (TMW, constrainer);
            if (mesh.hasExplicitNormals()) {
               ArrayList<Vector3d> normals = mesh.getNormals();
               if (normals != null) {
                  Matrix3d Ainv = new Matrix3d();
                  Ainv.invert (XL.A);
                  for (Vector3d nrm : normals) {
                     Ainv.mulTranspose (nrm, nrm);
                     nrm.normalize();
                  }
               }
            }
            else {
               mesh.clearNormals(); // regenerate on demand
            }
            for (Vertex3d v : mesh.getVertices()) {
               v.pnt.transform (XL);
            }
         }
         else {
            Point3d pworld = new Point3d(); // vertex point in world coords
            // transform normals first because we need unmodified vertex points
            if (mesh.hasExplicitNormals()) {
               ArrayList<Vector3d> normals = mesh.getNormals();
               if (normals != null) {
                  Vector3d nworld = new Vector3d(); // normal in world coords
                  Point3d[] refs = getNormalPointRefs(mesh);
               
                  ArrayList<Vertex3d> vertices = mesh.getVertices();
                  for (int ni=0; ni<normals.size(); ni++) {
                     Vector3d nrm = normals.get(ni);
                     pworld.transform (TMW, refs[ni]);
                     nworld.transform (TMW, nrm);
                     transformNormal (nworld, pworld);
                     nrm.inverseTransform (TMWnew, nworld);
                     nrm.normalize();
                  }
               }
            }
            else {
               mesh.clearNormals(); // regenerate on demand
            }
            for (Vertex3d v : mesh.getVertices()) {
               pworld.transform (TMW, v.pnt);
               computeTransformPnt (pworld, pworld);
               v.pnt.inverseTransform (TMWnew, pworld);
            }
         }
         mesh.notifyVertexPositionsModified();
      }
      mesh.setMeshToWorld (TMWnew);
      return s;
   }
   
   /**
    * Computes the matrices <code>PL</code> and <code>N</code> that transform
    * points <code>xl</code> local to a coordinate frame <code>T</code> after
    * that frame is itself transformed.  The updated local coordinates are
    * given by
    * <pre>
    * xl' = N PL xl
    * </pre>
    * where <code>PL</code> is symmetric positive definite and
    * <code>N</code> is a diagonal matrix that is either the identity,
    * or a reflection that flips a single axis. This accounts
    * for the non-rigid aspects of the transformation
    * that cannot be absorbed into <code>T</code>.
    *
    * <p>See the documentation for {@link
    * #transform(RigidTransform3d,RigidTransform3d)}. If
    * <pre>
    *      [  R   p  ]
    * T =  [         ]
    *      [  0   1  ]
    * </pre>
    * then
    * <pre>
    *      [  Q R N   f(p) ]
    * TR = [               ].
    *      [    0      1   ]
    * </pre>
    * Meanwhile, the linearization of f(p) about <code>p</code> is given
    * by the affine transform
    * <pre>
    *     [  F    -F p + f(p) ]
    * X = [                   ].
    *     [  0         1      ]
    * </pre>
    * If <code>xw</code> and <code>xl</code> describe points with respect
    * to world and <code>T</code>, respectively,
    * we have
    * <pre>
    * xw' = X xw = X T xl
    * </pre>
    * and so the updated value of <code>xl</code> is given by
    * <pre>
    * xl' = inv(TR) xw' = inv(TR) X T xl
    * </pre>
    * which can be expressed as
    * <pre>
    *                       T  T
    * xl' = N PL xl,  PL = R  Q  P Q R
    * </pre>
    *
    * @param PL primary transformation matrix
    * @param Ndiag if non-null, returns the diagonal components of N
    * @param T rigid transform for which the local transforms are computed.
    */
   public abstract void computeLocalTransforms (
      Matrix3d PL, Vector3d Ndiag, RigidTransform3d T);
   
   /**
    * Computes the local affine transform <code>XL</code> that transforms
    * points <code>xl</code> local to a coordinate frame <code>T</code> after
    * that frame is itself transformed. The updated local coordinates are
    * given by
    * <pre>
    * [ xl' ]      [ xl ]
    * [     ] = XL [    ]
    * [  1  ]      [ 0  ]
    * </pre>
    * and <code>XL</code> is computed from the <code>PL</code> and
    * <code>N</code> matrices returned by
    * {@link #computeLocalTransforms(Matrix3d,Vector3d,RigidTransform3d)
    * computeLocalTransform(PL,Ndiag,T)}, according to 
    * <pre>
    *      [  N PL  0 ]
    * XL = [          ].
    *      [  0     1 ]
    * </pre>
    * An optional <code>constrainer</code> can be provided to
    * further constrain the resulting value of <code>XL</code>.
    *
    * @param T rigid transform for which the local affine transform is computed
    * @param constrainer if non-null, specifies a constrainer that should be
    * applied to constrain the value of the transform
    * @return local affine transform
    */
   public AffineTransform3d computeLocalAffineTransform (
      RigidTransform3d T, Constrainer constrainer) {
      
      Matrix3d PL = new Matrix3d();
      Vector3d Ndiag = new Vector3d();
      computeLocalTransforms (PL, Ndiag, T);

      AffineTransform3d XL = new AffineTransform3d (PL, Vector3d.ZERO);
      XL.A.mulRows (Ndiag);
      if (constrainer != null) {
         constrainer.apply (XL);
      }
      return XL;
   }
   
   /**
    * Computes and returns the linearized affine transform at the specified
    * reference position. In the general case, this is the local linearization
    * of the deformation field f(p) around <code>r</code>,
    * given by
    * <pre>
    *     [  F   f(dp) ]
    * X = [            ]
    *     [  0     1   ]
    * </pre>
    * so that for a small change in position <code>dp</code>, we have
    * <pre>
    *             [ dp ]
    * f(p+dp) = X [    ]
    *             [ 1  ]
    * </pre>
    * In the case where f(p) is itself an affine or rigid transform
    * given by 
    * <pre>
    *     [  F     px  ]
    * A = [            ]
    *     [  0     1   ]
    * </pre>
    * the local transform becomes
    * <pre>
    *     [  F     F p + px  ]
    * X = [                  ]
    *     [  0         1     ]
    * </pre>
    * and if p = 0, then X is the simply the transform itself.
    *
    * @param r reference position at which the transformation is calculated
    * @return local affine transform
    */
   public AffineTransform3dBase computeLinearizedTransform (Vector3d r) {
      if (isRigid()) {
         RigidTransform3d XR = new RigidTransform3d();
         computeTransform (XR);
         return XR;
      }
      else {
         AffineTransform3d XA = new AffineTransform3d();
         computeTransform (XA);
         return XA;
      }      
   }
   
   /**
    * Return an appropriate transformer for a given affine transform.
    * 
    * @param X Affine transform (either rigid or strictly affine) for which
    * the transformer should be created 
    * @return transformer for the indicated affine transform
    */
   public static GeometryTransformer create (AffineTransform3dBase X) {
      if (X instanceof RigidTransform3d) {
         return new RigidTransformer ((RigidTransform3d)X);         
      }
      else if (X instanceof AffineTransform3d) {
         return new AffineTransformer ((AffineTransform3d)X);
      }
      else if (X instanceof ScaledRigidTransform3d) {
         
         ScaledRigidTransform3d srt = (ScaledRigidTransform3d)X;
         AffineTransform3d trans = new AffineTransform3d();
         trans.setRotation (srt.R);
         trans.setTranslation (srt.p);
         trans.setScaling (srt.s, srt.s, srt.s);
         return new AffineTransformer(trans);
      }
      else 
         throw new IllegalArgumentException (
            "Unknown argument type: " + X.getClass());
   }

   /**
    * Return the current size of the undo data buffer, or -1 if the buffer is
    * null. Used for debugging only.
    */ 
   public int getUndoDataSize() {
      if (myUndoData != null) {
         return myUndoData.size();
      }
      else {
         return -1;
      }
   }

}
