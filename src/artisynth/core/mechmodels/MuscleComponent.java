package artisynth.core.mechmodels;

import java.awt.Color;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.materials.AxialMaterial;

public interface MuscleComponent
   extends ExcitationComponent, ForceEffector, RenderableComponent {

   public Color getExcitationColor();

   public void setExcitationColor (Color color);
   
   public AxialMaterial getMaterial();
   
   public <T extends AxialMaterial> void setMaterial (T mat);
   
   public double getRestLength();
   
   public void setRestLength (double rlen);
   
   public int numPoints();
   
   public Point getPoint (int idx);
   
   public double getLength();
   
   public double getLengthDot();

}
