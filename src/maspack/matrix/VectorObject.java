package maspack.matrix;

import maspack.util.Scannable;

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
public interface VectorObject<T> extends Scannable {

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

}
