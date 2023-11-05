package maspack.matrix;

import maspack.util.Scannable;
import maspack.util.Clonable;

/**
 * Defines basic vector operations for a type T.
 *
 * The <i>scale</i>, <i>add</i>, and <i>scaledAdd</i> operations are currently
 * named {@code scaleObj()}, {@code addObj()} and {@code scaledAddObj()}, since
 * the corresponding {@code scale()}, {@code add()} and {@code scaledAdd()}
 * methods in the {@code Vector} and {@code Matrix} subclasses sometimes may
 * incompatitable return types (some return a reference to their object, while
 * others return {@code void}).
 */
public interface VectorObject<T> extends Scannable, Clonable {

   /**
    * Scales this vector by {@code s}.
    */
   public void scaleObj (double s);

   /**
    * Adds {@code v1} to this vector.
    */
   public void addObj (T v1);

   /**
    * Scales {@code v1} by {@code s} and adds it to this vector.
    */
   public void scaledAddObj (double s, T v1);

   /**
    * Sets this vector to {@code v1}
    */
   public void set (T v1);

   /**
    * Sets this vector to zero.
    */
   public void setZero();

   /**
    * Returns true if the components of this vector are equal to those of
    * {@code v1}, within the tolerance {@code tol}.  If {@code tol} = 0, then
    * exact equality is required.
    */
   public boolean epsilonEquals (T v1, double tol);

   /**
    * Where possible, maps or projects this VectorObject onto a 3-vector.
    * Currently used for rendering vector fields.
    */
   public default boolean getThreeVectorValue (Vector3d vec) {
      if (this instanceof Vector3d) {
         vec.set ((Vector3d)this);
         return true;
      }
      else if (this instanceof Vector2d) {
         Vector2d vec2 = (Vector2d)this;
         vec.set (vec2.x, vec2.y, 0);
         return true;
      }
      else if (this instanceof Vector) {
         Vector vecx = (Vector)this;         
         int size = Math.min (3, vecx.size());
         vec.setZero();
         for (int i=0; i<size; i++) {
            vec.set (i, vecx.get(i));
         }
         return true;
      }
      else {
         return false;
      }
   }


}
