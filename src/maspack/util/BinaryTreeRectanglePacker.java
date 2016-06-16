package maspack.util;

/**
 * Simple binary-tree based rectangle packer based on Jim Scott's Lightmap Packing 
 * http://www.blackpawn.com/texts/lightmaps/
 * 
 * Only keeps track of free space.  Cannot remove rectangles.
 * @author Antonio
 *
 */
public class BinaryTreeRectanglePacker implements RectanglePacker {

   private class Node {
      Rectangle rect;
      boolean leaf;
      Node left;
      Node right;

      public Node(Rectangle rect) {
         this.rect = rect;
         left = null;
         right = null;
         leaf = true;  // start as a leaf
      }

      public int width() {
         return rect.width ();
      }

      public int height() {
         return rect.height ();
      }

      public Node fit(int w, int h) {

         // dimension difference
         int dw = rect.width () - w;
         int dh = rect.height () - h;

         // exit early if we won't fit
         if (dw < 0 || dh < 0) {
            return null;
         }

         if (leaf) {
            return this;
         } 

         // not a leaf, choose a child to pack it into
         if (left != null) {
            Node out = left.fit (w, h);
            if (out != null) {
               return out;
            }
         }
         if (right != null) {
            return right.fit (w, h);
         }
         return null;  // didn't fit anywhere
      }

      public Rectangle pack(int w, int h) {

         // dimension difference
         int dw = rect.width () - w;
         int dh = rect.height () - h;

         // exit early if we won't fit
         if (dw < 0 || dh < 0) {
            return null;
         }

         if (leaf) {
            // we are a leaf on the wind, pack and split

            // split decision based on Jim's lightmap packing
            if (dw > dh) {
               left = new Node(
                  new Rectangle (rect.x(),rect.y()+h, w, dh));
               right = new Node (
                  new Rectangle (rect.x()+w, rect.y (), dw, rect.height ()));

            } else {
               left = new Node (
                  new Rectangle (rect.x()+w, rect.y (), dw, h));
               right = new Node(
                  new Rectangle (rect.x(),rect.y()+h, rect.width (), dh));

            }
            //
            // XXX always pack by smallest volume, leads to some odd looking packs
            // decide which way to split by cutting off
            // the smallest area
            //            int v00 = dw*h;
            //            int v01 = dh*rect.width ();
            //
            //            int v10 = dh*w;
            //            int v11 = dw*rect.height ();
            //
            //            int v0 = Math.min (v00, v01);
            //            int v1 = Math.min (v10, v11);
            //
            //            if (v0 < v1) {
            //               Node node0 = new Node (
            //                  new Rectangle (rect.x()+w, rect.y (), dw, h));
            //               Node node1 = new Node(
            //                  new Rectangle (rect.x(),rect.y()+h, rect.width (), dh));
            //
            //               if (v00 < v01) {
            //                  if (v00 == 0) {
            //                     left = node1;
            //                     right = null;
            //                  } else {
            //                     left = node0;
            //                     right = node1;  
            //                  }
            //               } else {
            //                  if (v00 == 0) {
            //                     left = null;
            //                     right = null;
            //                  } else if (v01 == 0) {
            //                     left = node0;
            //                     right = null;
            //                  } else {
            //                     left = node1;
            //                     right = node0;
            //                  }
            //               }
            //
            //            } else {
            //               Node node0 = new Node(
            //                  new Rectangle (rect.x(),rect.y()+h, w, dh));
            //               Node node1 = new Node (
            //                  new Rectangle (rect.x()+w, rect.y (), dw, rect.height ()));
            //               
            //               if (v10 < v11) {
            //                  if (v10 == 0) {
            //                     left = node1;
            //                     right = null;
            //                  } else {
            //                     left = node0;
            //                     right = node1;  
            //                  }
            //               } else {
            //                  if (v10 == 0) {
            //                     left = null;
            //                     right = null;
            //                  } else if (v11 == 0) {
            //                     left = node0;
            //                     right = null;
            //                  } else {
            //                     left = node1;
            //                     right = node0;
            //                  }
            //               }
            //            }
            //
            //  // minimum dimension first
            //            int v00 = Math.min (dw, h);
            //            int v01 = Math.min (dh, rect.width ());
            //
            //            int v10 = Math.min(dh,w);
            //            int v11 = Math.min (dw,rect.height ());
            //
            //            int v0 = Math.min (v00, v01);
            //            int v1 = Math.min (v10, v11);
            //
            //            if (v0 < v1) {
            //               Node node0 = new Node (
            //                  new Rectangle (rect.x()+w, rect.y (), dw, h));
            //               Node node1 = new Node(
            //                  new Rectangle (rect.x(),rect.y()+h, rect.width (), dh));
            //
            //               if (v00 < v01) {
            //                  if (v00 == 0) {
            //                     left = node1;
            //                     right = null;
            //                  } else {
            //                     left = node0;
            //                     right = node1;  
            //                  }
            //               } else {
            //                  if (v00 == 0) {
            //                     left = null;
            //                     right = null;
            //                  } else if (v01 == 0) {
            //                     left = node0;
            //                     right = null;
            //                  } else {
            //                     left = node1;
            //                     right = node0;
            //                  }
            //               }
            //
            //            } else {
            //               Node node0 = new Node(
            //                  new Rectangle (rect.x(),rect.y()+h, w, dh));
            //               Node node1 = new Node (
            //                  new Rectangle (rect.x()+w, rect.y (), dw, rect.height ()));
            //
            //               if (v10 < v11) {
            //                  if (v10 == 0) {
            //                     left = node1;
            //                     right = null;
            //                  } else {
            //                     left = node0;
            //                     right = node1;  
            //                  }
            //               } else {
            //                  if (v10 == 0) {
            //                     left = null;
            //                     right = null;
            //                  } else if (v11 == 0) {
            //                     left = node0;
            //                     right = null;
            //                  } else {
            //                     left = node1;
            //                     right = node0;
            //                  }
            //               }
            //            }

            leaf = false;  // no longer a leaf

            return new Rectangle (rect.x (), rect.y (), w, h);
         } 

         // not a leaf, choose a child to pack it into
         if (left != null) {
            Rectangle out = left.pack (w, h);
            if (out != null) {
               return out;
            }
         }
         if (right != null) {
            return right.pack (w, h);
         }
         return null;  // didn't fit anywhere
      }
   }

   Node root;
   Node lastfit;
   int lastfitw;
   int lastfith;
   
   public BinaryTreeRectanglePacker(int width, int height) {
      init(width, height);
   }
   
   private void init (int width, int height) {
      lastfitw = -1;
      lastfith = -1;
      lastfit = null;
      root = new Node (new Rectangle (0, 0, width, height));
   }
   
   public boolean fits(Rectangle r) {
      return fits(r.width(), r.height());
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
   public boolean fits(int w, int h) {
      lastfit = root.fit (w, h);
      lastfitw = w;
      lastfith = h;
      return (lastfit != null);
   }
   
   public Rectangle pack(Rectangle r) {
      return pack(r.width (), r.height ());
   }

   /**
    * Packs a rectangle with given dimensions, returning
    * the destination rectangle.
    * @param w width
    * @param h height
    * @return fit location, null if it doesn't fit
    */
   @Override
   public Rectangle pack(int w, int h) {
      
      Node packnode = root;
      if (w == lastfitw && h == lastfitw) {
         if (lastfit == null) {
            return null;
         }
         packnode = lastfit;
      }
      // clear history
      lastfit = null;
      lastfitw = -1;
      lastfith = -1;
      return packnode.pack (w, h);
   }

   /**
    * Width of bounding box
    * @return width
    */
   public int getWidth() {
      return root.width();
   }

   /**
    * Height of bounding box
    * @return height
    */
   public int getHeight() {
      return root.height();
   }
      
   @Override
   public void clear () {
      init(root.width (), root.height ());
   }

}
