package maspack.util;

/**
 * Can pack integer-valued rectangles into a 2D area, essentially implementing a bin-packing algorithm
 */
public interface RectanglePacker {
   
   /**
    * Simple immutable class for storing integer-dimensioned rectangles
    * @author Antonio
    *
    */
   public static class Rectangle {
      int x;
      int y;
      int w;
      int h;

      public Rectangle(int x, int y, int w, int h) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
      }

      public int x() {
         return x;
      }

      public int y() {
         return y;
      }

      public int width() {
         return w;
      }

      public int height() {
         return h;
      }
      
      public int area() {
         return w*h;
      }
   }
   
   /**
    * Checks whether a rectangle with given size will
    * fit in the pack.  The search is cached so that
    * a following call to {@link #pack(int, int)} is
    * an O(1) operation.
    * @param w width
    * @param h height
    * @return true if fits, false otherwise
    */
   public boolean fits(int w, int h);
   
   /**
    * Packs a rectangle with given dimensions, returning
    * the destination rectangle.
    * @param w width
    * @param h height
    * @return fit location, null if it doesn't fit
    */
   public Rectangle pack(int w, int h);
   
   /**
    * Clears all rectangles, to start over
    */
   public void clear();
   
   /**
    * @return width of bounding rectangle
    */
   public int getWidth();
   
   /**
    * @return height of bounding rectangle
    */
   public int getHeight();
   
}
