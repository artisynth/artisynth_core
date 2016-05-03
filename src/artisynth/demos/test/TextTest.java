package artisynth.demos.test;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;

import artisynth.core.renderables.TextComponent3d;
import artisynth.core.renderables.TextComponentBase.HorizontalAlignment;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;

public class TextTest extends RootModel {
   
   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      
      final HashMap<String,Font> fontMap = new HashMap<> ();
      for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment ().getAllFonts ()) {
         fontMap.put (font.getName (), font);
      }
      
      TextComponent3d text = new TextComponent3d ("hello world");
      text.setFont (new Font(Font.SANS_SERIF, 0, 54));
      text.setText ( "Hello world! And goodnight to all.");
      text.setPosition (new Point3d(0.4f,0.3f,0.3f));
      text.setTextSize (0.25);
      text.setTextColor (Color.WHITE);
      addRenderable (text);
      
      text = new TextComponent3d ("alphabet");
      text.setFont (new Font(Font.SANS_SERIF, 0, 54));
      text.setText ( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" );
      text.setPosition (new Point3d(-2f,0f,0f));
      text.setTextSize (0.25);
      text.setTextColor (Color.ORANGE);
      addRenderable (text);
      
      Font comic = fontMap.get ("Comic Sans MS");
      if (comic == null) {
         comic = new Font(Font.MONOSPACED, Font.BOLD, 32);
      } else {
         comic = comic.deriveFont (Font.BOLD, 32);
      }
      text = new TextComponent3d ("mikey");
      text.setFont (new Font(Font.SANS_SERIF, 0, 54));
      text.setText ( "Cowabunga" );
      text.setPosition (new Point3d(0,0.5f,0f));
      text.setTextSize (0.25);
      text.setTextColor (Color.CYAN);
      text.setHorizontalAlignment (HorizontalAlignment.CENTRE);
      addRenderable (text);
      
      
   }

}
