package artisynth.demos.test;

import java.io.IOException;

import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Particle;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;

public class MultiSpringTest extends RootModel {

   @Override
   public void build (String[] args) throws IOException {
      super.build (args);

      MechModel mech = new MechModel("mech");
      addModel (mech);

      // addSprings(mech);
      // addMixedUpSprings(mech);
      // addSeparatedSprings(mech);
      // addSpringMesh(mech);
      addVerticalSprings(mech);

      // clear render props
      for (AxialSpring s : mech.axialSprings ()) {
         s.setRenderProps (null);
      }

      RenderProps.setLineStyle (mech, LineStyle.CYLINDER);
      RenderProps.setLineRadius (mech, 0.02);
   }

   protected void addSprings(MechModel mech) {
      Particle p = new Particle (1, new Point3d(0,0,0));
      mech.addParticle (p);
      Particle lastp = p;
      for (int i=0; i<20; ++i) {
         p = new Particle(1, new Point3d((i+1)*0.1, 2*(i%2-0.5)*0.1, 0));
         mech.addParticle (p);
         AxialSpring ax = new AxialSpring ("spring " + i);
         ax.setPoints (lastp, p);
         mech.addAxialSpring (ax);
         lastp = p;
      }
   }

   protected void addMixedUpSprings(MechModel mech) {

      Particle p = new Particle (1, new Point3d(0,0,0.1));
      mech.addParticle (p);
      Particle lastp = p;
      for (int i=0; i<20; ++i) {
         p = new Particle(1, new Point3d((i+1)*0.1, 2*(i%2-0.5)*0.1, 0.1));
         mech.addParticle (p);
         AxialSpring ax = new AxialSpring ("spring mixed " + i);
         if (i % 2 == 0) {
            ax.setPoints (lastp, p);
         } else {
            ax.setPoints (p, lastp);
         }
         mech.addAxialSpring (ax);
         lastp = p;
      }
   }

   protected void addSeparatedSprings(MechModel mech) {
      for (int i=0; i<10; ++i) {
         Particle p1 = new Particle(1, new Point3d((2*i)*0.1, 2*(i%2-0.5)*0.1, -0.1));
         Particle p2 = new Particle(1, new Point3d((2*i+1)*0.1, -2*(i%2-0.5)*0.1, -0.1));

         mech.addParticle (p1);
         mech.addParticle (p2);
         AxialSpring ax = new AxialSpring ("spring detached " + i);
         if (i % 2 == 0) {
            ax.setPoints (p1, p2);
         } else {
            ax.setPoints (p2, p1);
         }
         mech.addAxialSpring (ax);
      }
   }

   protected void addSpringMesh(MechModel mech) {
      Particle p0 = new Particle (0.5, -0.10, 0, 0.20);
      p0.setDynamic (false);

      Particle p1 = new Particle (0.5, 0, 0, 0.25);
      Particle p2 = new Particle (0.5, 0, 0, 0.15);
      Particle p3 = new Particle (0.5, 0.10, 0, 0.20);

      AxialSpring[] springs = new AxialSpring[10];
      for (int i = 0; i < springs.length; i++) {
         springs[i] = new AxialSpring (0.50, 0.20, 0.10);
      }

      //    mech.particles().addNumbered (p0, 5);
      mech.particles().addNumbered (p1, 4);
      mech.particles().addNumbered (p2, 0);
      //    mech.particles().addNumbered (p3, 1);

      //      mech.attachAxialSpring (p0, p1, springs[0]);
      //      mech.attachAxialSpring (p0, p2, springs[1]);
      mech.attachAxialSpring (p2, p1, springs[2]);
      //      mech.attachAxialSpring (p1, p3, springs[3]);
      //      mech.attachAxialSpring (p2, p3, springs[4]);

      //      Particle p10 = new Particle (0.5, 0.10, 0, 0.20);
      //      Particle p11 = new Particle (0.5, 0.05, 0, 0.10);
      //      Particle p12 = new Particle (0.5, 0.15, 0, 0.10);
      //      Particle p13 = new Particle (0.5, 0.10, 0, 0);
      //
      //      mech.addParticle (p10);
      //      mech.addParticle (p11);
      //      mech.addParticle (p12);
      //      mech.addParticle (p13);
      //
      //      mech.attachAxialSpring (p10, p11, springs[5]);
      //      mech.attachAxialSpring (p10, p12, springs[6]);
      //      mech.attachAxialSpring (p11, p12, springs[7]);
      //      mech.attachAxialSpring (p11, p13, springs[8]);
      //      mech.attachAxialSpring (p12, p13, springs[9]);
   }
   
   protected void addVerticalSprings(MechModel mech) {
      
      for (int i=0; i<5; ++i) {
         Particle p1 = new Particle (0.1, i*0.1, 0, 0.1);
         Particle p2 = new Particle (0.1, i*0.1+0.00001*i, 0, 0.0);
         mech.addParticle (p1);
         mech.addParticle (p2);
         AxialSpring as = new AxialSpring ("vertical " + i);
         if (i%2 == 0) {
            as.setPoints (p1, p2);
         } else {
            as.setPoints (p2, p1);
         }
         mech.addAxialSpring (as);
      }
      
   }

}
