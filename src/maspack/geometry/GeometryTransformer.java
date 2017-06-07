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
   }

   /**
    * Constrains an affine transform so that its matrix component results in
    * uniform scaling.
    */
   public static class UniformScalingConstrainer implements Constrainer {
      
      public UniformScalingConstrainer() {
      }
      
      public void apply (AffineTransform3dBase X) {

         if (X instanceof AffineTransform3d && 
             !X.equals (AffineTransform3d.IDENTITY)) {
            
            AffineTransform3d XA = (AffineTransform3d)X;

            PolarDecomposition3d pd = null;
            Matrix3d P = XA.A;
            if (!P.isSymmetric (1000*MACH_PREC*P.oneNorm())) {
               pd = new PolarDecomposition3d();
               pd.factor (P);
               P = pd.getP();
            }
            double s = Math.pow (Math.abs(P.determinant()), 1/3.0);            
            XA.A.setDiagonal (s, s, s);
            if (pd != null) {
               XA.A.mul (pd.getR(), XA.A);
            }
         }
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
    * Transforms a point <code>p1</code> and returns the result in
    * <code>pr</code>.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>pr</code> is instead set to the value of
    * <code>p1</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param pr transformed point
    * @param p1 point to be transformed
    */
   public void transformPnt (Point3d pr, Point3d p1) {
      if (isRestoring()) {
         Point3d p = restore (pr);
         pr.set (p);
         return;
      }
      if (isSaving()) {
         save (new Point3d(p1));
      }
      computeTransformPnt (pr, p1);
   }
   
   /**
    * Transforms a point <code>p1</code> and returns the result in
    * <code>pr</code>. This provides the low level implementation for point
    * transformations and does not do any saving or restoring of data.
    * 
    * @param pr transformed point
    * @param p1 point to be transformed
    */
   public abstract void computeTransformPnt (Point3d pr, Point3d p1);

   /**
    * Transforms a vector <code>v</code>, located at reference position
    * <code>r</code>, in place.
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
    * Transforms a vector <code>v1</code>, located at reference position
    * <code>r</code>, and returns the result in <code>vr</code>.
    * Generally, this transformation will take the form
    * <pre>
    * vr = F v1
    * </pre>
    * where <code>F</code> is the deformation gradient at the reference
    * position.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>vr</code> is instead set to the value of
    * <code>v1</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param vr transformed vector
    * @param v1 vector to be transformed
    * @param r reference position of the vector, in original coordinates
    */
   public void transformVec (Vector3d vr, Vector3d v1, Vector3d r) {
      if (isRestoring()) {
         vr.set (restore (vr));
         return;
      }
      if (isSaving()) {
         save (new Vector3d(v1));
      }
      computeTransformVec (vr, v1, r);
   }
   
   
   /**
    * Transforms a vector <code>v1</code>, located at reference position
    * <code>r</code>, and returns the result in <code>vr</code>. This
    * provides the low level implementation for vector transformations and does
    * not do any saving or restoring of data.
    *
    * @param vr transformed vector
    * @param v1 vector to be transformed
    * @param r reference position of the vector, in original coordinates
    */
   public abstract void computeTransformVec (
      Vector3d vr, Vector3d v1, Vector3d r);

   /**
    * Transforms a rigid transform <code>T</code>, in place.
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
    * Transforms a rigid transform <code>T1</code> and returns the
    * result in <code>TR</code>. If
    * <pre>
    *      [  R1   p1  ]
    * T1 = [           ]
    *      [   0    1  ]
    * </pre>
    * then generally, this transform takes the form
    * <pre>
    *      [ RF R1   f(p1) ]
    * TR = [               ]
    *      [   0       1   ]
    * </pre>
    * where <code>RF</code> is the right-handed rotational component
    * of the deformation gradient <code>F</code> 
    * at <code>p1</code>, and <code>f(p1)</code> is the deformed
    * position of <code>p1</code>.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>TR</code> is instead set to the value of
    * <code>T1</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param TR transformed transform
    * @param T1 transform to be transformed
    */
   public void transform (RigidTransform3d TR, RigidTransform3d T1) {
      if (isRestoring()) {
         TR.set (restore (TR));
         return;
      }
      if (isSaving()) {
         save (new RigidTransform3d(T1));
      }
      computeTransform (TR, T1);
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
    * Transforms a rigid transform <code>T1</code> and returns the result in
    * <code>TR</code>. This provides the low level implementation for the
    * transformation of rigid transforms and does not do any saving or
    * restoring of data.
    *
    * @param TR transformed transform
    * @param T1 transform to be transformed
    */
   public abstract void computeTransform (
      RigidTransform3d TR, RigidTransform3d T1);
   
   /**
    * Transforms an affine transform <code>X</code>, in place.
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
    * Transforms an affine transform <code>X1</code> and returns the
    * result in <code>XR</code>. If
    * <pre>
    *      [  A1   p1  ]
    * X1 = [           ]
    *      [   0    1  ]
    * </pre>
    * then generally, this transform takes the form
    * <pre>
    *      [  F A1   f(p1) ]
    * XR = [               ]
    *      [   0       1   ]
    * </pre>
    * where <code>F</code> is the deformation gradient
    * at <code>p1</code>, and <code>f(p1)</code> is the deformed
    * position of <code>p1</code>.
    *
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>XR</code> is instead set to the value of
    * <code>X1</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param XR transformed transform
    * @param X1 transform to be transformed
    */
   public void transform (
      AffineTransform3d XR, AffineTransform3d X1) {
      if (isRestoring()) {
         XR.set (restore (XR));
         return;
      }
      if (isSaving()) {
         save (new AffineTransform3d(X1));
      }
      computeTransform (XR, X1);
   }

   /**
    * Transforms an affine transform <code>X1</code> and returns the result in
    * <code>XR</code>. This provides the low level implementation for the
    * transformation of affine transforms and does not do any saving or
    * restoring of data.
    * 
    * @param XR transformed transform
    * @param X1 transform to be transformed
    */
   public abstract void computeTransform (
      AffineTransform3d XR, AffineTransform3d X1);

   /**
    * Transforms a rotation matrix <code>R</code>, located at reference
    * position <code>r</code>, in place.
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
    * Transforms a rotation matrix <code>R1</code>, located at reference
    * position <code>r</code>, and returns the result in <code>RR</code>.
    * Generally, this transform takes the form
    * <pre>
    * RR = RF R1
    * </pre>
    * where <code>RF</code> is the right-handed rotational component
    * of the polar decomposition of the deformation gradient <code>F</code>
    * at the reference position.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>RR</code> is instead set to the value of
    * <code>R1</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param RR transformed rotation
    * @param R1 rotation to be transformed
    * @param r reference position of the rotation, in original coordinates
    */
   public void transform (
      RotationMatrix3d RR, RotationMatrix3d R1, Vector3d r) {
      if (isRestoring()) {
         RR.set (restore (RR));
         return;
      }
      if (isSaving()) {
         save (new RotationMatrix3d(R1));
      }
      computeTransform (RR, R1, r);      
   }

   /**
    * Transforms a rotation matrix <code>R1</code>, located at reference
    * position <code>r</code>, and returns the result in <code>RR</code>.
    * This provides the low level implementation for the transformation of
    * rotation matrices and does not do any saving or restoring of data.
    *
    * @param RR transformed rotation
    * @param R1 rotation to be transformed
    * @param r reference position of the rotation, in original coordinates
    */
   public abstract void computeTransform (
      RotationMatrix3d RR, RotationMatrix3d R1, Vector3d r);

   /**
    * Transforms a general 3 X 3 matrix <code>M</code>, located at reference
    * position <code>ref</code>, in place.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>M</code> is instead set to its original
    * value that was previously saved when the undo state was set to {@link
    * UndoState#SAVING}.
    *
    * @param M matrix to be transformed
    * @param ref reference position of the matrix, in original coordinates
    */
   public void transform (Matrix3d M, Vector3d ref) {
      transform (M, M, ref);
   }

   /**
    * Transforms a general 3 X 3 matrix <code>M1</code>, located at reference
    * position <code>ref</code>, and returns the result in <code>MR</code>.
    * Generally, this transform takes the form
    * <pre>
    * MR = F M1
    * </pre>
    * where <code>F</code> is the deformation gradient at the reference
    * position.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>MR</code> is instead set to the value of
    * <code>M1</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param MR transformed matrix
    * @param M1 matrix to be transformed
    * @param ref reference position of the matrix, in original coordinates
    */
   public void transform (Matrix3d MR, Matrix3d M1, Vector3d ref) {
      if (isRestoring()) {
         MR.set (restore (MR));
         return;
      }
      if (isSaving()) {
         save (new Matrix3d(M1));
      }
      computeTransform (MR, M1, ref);            
   }
   
   /**
    * Transforms a general 3 X 3 matrix <code>M1</code>, located at reference
    * position <code>ref</code>, and returns the result in <code>MR</code>.
    * This provides the low level implementation for the transformation of 3 X
    * 3 matrices and does not do any saving or restoring of data.
    *
    * @param MR transformed matrix
    * @param M1 matrix to be transformed
    * @param r reference position of the matrix, in original coordinates
    */
   public abstract void computeTransform (Matrix3d MR, Matrix3d M1, Vector3d r);

   /**
    * Transforms a plane <code>p</code>, located at reference
    * position <code>r</code>, in place.
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
    * Transforms a plane <code>p1</code>, located at reference position
    * <code>ref</code>, and returns the result in <code>pr</code>.
    * 
    * <p>If this transformer's undo state is set to {@link
    * UndoState#RESTORING}, then <code>pr</code> is instead set to the value of
    * <code>p1</code> that was previously saved when the undo state was set to
    * {@link UndoState#SAVING}.
    *
    * @param pr transformed plane
    * @param p1 plane to be transformed
    * @param r reference position of the plane, in original coordinates
    */
   public void transform (Plane pr, Plane p1, Vector3d r) {
      if (isRestoring()) {
         pr.set (restore (pr));
         return;
      }
      if (isSaving()) {
         save (new Plane(p1));
      }
      computeTransform (pr, p1, r);            
   }

   /**
    * Transforms a plane <code>p1</code>, located at reference position
    * <code>ref</code>, and returns the result in <code>pr</code>. This
    * provides the low level implementation for the transformation of planes
    * and does not do any saving or restoring of data.
    *
    * @param pr transformed plane
    * @param p1 plane to be transformed
    * @param r reference position of the plane, in original coordinates
    */
   public abstract void computeTransform (Plane pr, Plane p1, Vector3d r);

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
      if (isRestoring()) {
         restoreVertices (mesh);
         mesh.notifyVertexPositionsModified();
         return;
      }
      if (isSaving()) {
         saveVertices (mesh);
      }
      for (Vertex3d v : mesh.getVertices()) {
         transformPnt (v.pnt);
      }
      mesh.notifyVertexPositionsModified();
   }

   /**
    * Applies a geometric transformation to both the vertex positions of a mesh
    * and its mesh-to-world transform TMW, in world coordinates. The local
    * vertex positions <code>p</code> are modified to accomodate that of part
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
    * vertex positions <code>p</code> are modified to accomodate that part
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
         }
         save (new RigidTransform3d(TMW));
      }
      if (!isRigid()) {
         if (isAffine() || constrainer != null) {
            
            // can compress the transform into one operation
            //           -1 
            // Y = TMWnew   X TMW
            AffineTransform3d XL = computeRightAffineTransform (TMW);
            if (constrainer != null) {
               constrainer.apply (XL);
            }
            for (Vertex3d v : mesh.getVertices()) {
               v.pnt.transform (XL);
            }
         }
         else {
            Point3d p = new Point3d();
            for (Vertex3d v : mesh.getVertices()) {
               p.transform (TMW, v.pnt);
               computeTransformPnt (p, p);
               p.inverseTransform (TMWnew);
               v.pnt.set (p);
            }
         }
         mesh.notifyVertexPositionsModified();
      }
      mesh.setMeshToWorld (TMWnew);
      return s;
   }
   
   /**
    * Computes and returns the right affine transform for the general
    * transformation of a rigid transform T. This is the transform X such that
    * if T' is the transformed value of T, and T" is the general transformed
    * value of T (treating T as an affine transform), then
    * 
    * <pre>
    * T" = T' X
    * </pre>
    * X hence contains the non-rigid parts of T", expressed as a transform
    * that is applied before T'. If
    * <pre>
    *     [  R   p  ]
    * T = [         ]
    *     [  0   1  ]
    * </pre>
    * and PF RF = F is the left polar decomposition of the deformation gradient
    * F at p, then
    * <pre>
    *      [  RF R   f(p)  ]         [  F R   f(p)  ]
    * T' = [               ],  T'' = [              ]
    *      [    0      1   ]         [   0     1    ]
    * </pre>
    * and X takes the form
    * <pre>
    *     [  A   0  ]
    * X = [         ]  with A = R^T RF^T PF RF R 
    *     [  0   1  ]
    * </pre>
    * @param T rigid transform for which the right affine transform is computed.
    * @return right affine transform for <code>T</code>.
    */
   public AffineTransform3d computeRightAffineTransform (RigidTransform3d T) {
   
      AffineTransform3d XL = new AffineTransform3d();
      if (!isRigid()) {
         RigidTransform3d Tnew = new RigidTransform3d(T);
         computeTransform (Tnew);
         XL.set (T);
         computeTransform (XL);
         XL.mulInverseLeft (Tnew, XL);
      }
      return XL;
   }
   
   /**
    * Computes and returns the local affine transform at the specified
    * reference position. In the general case, this is the local linearization
    * of the deformation field f(p) around <code>ref</code>,
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
   public AffineTransform3dBase computeLocalTransform (Vector3d r) {
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
      else 
         throw new IllegalArgumentException (
            "Unknown argument type: " + X.getClass());
   }

}
