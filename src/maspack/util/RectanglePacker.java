package maspack.util;

/**
 * Can pack integer-valued rectangles into a 2D area, essentially implementing a bin-packing algorithm
 */
public interface RectanglePacker {
   
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
