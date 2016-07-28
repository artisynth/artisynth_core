package artisynth.demos.test;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.HashMap;

import artisynth.core.renderables.TextComponent3d;
import artisynth.core.renderables.TextComponentBase.HorizontalAlignment;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ScanToken;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

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
      text.setFollowEye (false);
      RenderProps.setFaceStyle (text, FaceStyle.FRONT_AND_BACK);
      addRenderable (text);
      
      text = new TextComponent3d ("alphabet");
      text.setFont (new Font(Font.SANS_SERIF, 0, 54));
      text.setText ( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" );
      text.setPosition (new Point3d(-2f,0f,0f));
      text.setTextSize (0.25);
      text.setTextColor (Color.ORANGE);
      text.setFollowEye (false);
      RenderProps.setFaceStyle (text, FaceStyle.FRONT_AND_BACK);
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
      text.setFollowEye (false);
      RenderProps.setFaceStyle (text, FaceStyle.FRONT_AND_BACK);
      RenderProps.setShading (text, Shading.NONE);
      addRenderable (text);
      
      
      text = new TextComponent3d ("flip");
      text.setFont (new Font(Font.SERIF, 0, 64));
      text.setText ( "Flip me!" );
      text.setPosition (new Point3d(0,-0.44f,0f));
      text.setOrientation (new AxisAngle(0, 1, 0, Math.toRadians (30)));
      text.setTextSize (0.3);
      text.setTextColor (Color.MAGENTA);
      text.setHorizontalAlignment (HorizontalAlignment.CENTRE);
      text.setFollowEye (false);
      RenderProps.setFaceStyle (text, FaceStyle.FRONT);
      addRenderable (text);
      
      text = new TextComponent3d ("flip2");
      text.setFont (new Font(Font.SERIF, 0, 64));
      text.setText ( "Flip me!" );
      text.setPosition (new Point3d(0,-0.44f,0f));
      text.setOrientation (new AxisAngle(0, 1, 0, Math.toRadians (210)));
      text.setTextSize (0.3);
      text.setTextColor (Color.MAGENTA);
      text.setHorizontalAlignment (HorizontalAlignment.CENTRE);
      text.setFollowEye (false);
      RenderProps.setFaceStyle (text, FaceStyle.FRONT);
      addRenderable (text);
      
      PrintWriter writer = new PrintWriter (ArtisynthPath.getTempDir().getAbsolutePath() + "/texttest.txt");
      write (writer, new NumberFormat ("%g"), null);
      writer.close ();

      ArrayDeque<ScanToken> tokens = new ArrayDeque<> ();
      ReaderTokenizer rtok = new ReaderTokenizer (new FileReader (ArtisynthPath.getTempDir().getAbsolutePath() + "/texttest.txt"));
      scan (rtok, tokens);
      rtok.close ();
   }
   
   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);
      
      driver.getViewer ().setAxialView (AxisAlignedRotation.X_Y);
      
   }

}
