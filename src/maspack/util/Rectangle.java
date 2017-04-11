package maspack.util;

/**
 * Simple immutable class for storing integer-dimensioned rectangles.  The
 * position of the rectangle is assumed to refer to the bottom-left coordinates.
 * @author Antonio
 *
 */
public class Rectangle {
   int x;
   int y;
   int w;
   int h;

   /**
    * Rectangle given a position (x,y) and dimensions (w,h).
    */
   public Rectangle(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
   }

   /**
    * @return x-coordinate
    */
   public int x() {
      return x;
   }

   /**
    * @return y coordinate
    */
   public int y() {
      return y;
   }

   /**
    * @return width
    */
   public int width() {
      return w;
   }

   /**
    * @return height
    */
   public int height() {
      return h;
   }
   
   /**
    * @return rectangular area (width*height)
    */
   public int area() {
      return w*h;
   }
}
