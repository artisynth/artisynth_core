package maspack.render;

import java.awt.Font;
import java.awt.geom.Rectangle2D;

public interface TextRenderer {

   /**
    * Bounding rectangle.  Includes ascent, descent, leading, advance.
    * Some glyphs may extend beyond these bounds in some fonts.
    * Bounds are base-line relative, with the position being
    * the bottom-left corner.
    * @param font font used for sizing
    * @param text text to bound
    * @param emsize 'em' unit size
    * @return bounding rectangle
    */
   public Rectangle2D getTextBounds(Font font, String text, float emsize);
   
   /**
    * Renders text with a given font at a provided baseline location.
    * @param font font to use
    * @param text text to draw
    * @param loc baseline location to start drawing
    * @param emsize size of an 'em' unit
    * @return the advance distance in the x-direction (width of text)
    */
   public float drawText (Font font, String text, float[] loc, float emsize);
   
}
