package maspack.util;

public class GridRectanglePacker implements RectanglePacker {

   int squares;
   int width;
   int height;
   int gwidth;
   int gheight;
   int swidth;
   int sheight;
   
   public GridRectanglePacker(int width, int height, int gridWidth, int gridHeight) {
      this.gwidth = gridWidth;
      this.gheight = gridHeight;
      init(width, height);
   }
   
   private void init (int width, int height) {
      swidth = width/gwidth;
      sheight = height/gheight;
      this.width = width;
      this.height = height;    
      squares = swidth*sheight;
   }
   
   /**
    * Width of a grid space
    * @return width
    */
   public int getGridWidth() {
      return gwidth;
   }
   
   /**
    * Height of a grid space
    * @return height
    */
   public int getGridHeight() {
      return gheight;
   }
   
   /**
    * Width of bounding box
    * @return width
    */
   public int getWidth() {
      return width;
   }

   /**
    * Height of bounding box
    * @return height
    */
   public int getHeight() {
      return height;
   }

   @Override
   public boolean fits (int w, int h) {
      if (w <= getGridWidth () && h <= getGridHeight ()) {
         return true;
      }
      return false;
   }
   
   /**
    * Number of remaining empty grid spaces
    * @return squares remaining
    */
   public int numRemaining() {
      return squares;
   }

   @Override
   public Rectangle pack (int w, int h) {
      if (squares == 0 || !fits(w, h)) {
         return null;
      }
      int id = swidth*sheight-squares; // start in zeroeth position
      int sy = id / swidth;
      int sx = id % swidth;
      --squares;
      return new Rectangle(sx*gwidth, sy*gheight, w, h);
   }
   
   @Override
   public void clear () {
      squares = swidth*sheight;
   }
}
