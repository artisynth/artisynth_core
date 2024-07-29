package artisynth.models.testspace.template;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

/**
 * Template class for creating ArtiSymth models.
 *
 * <p>Caution: do not modify this class, as the ArtiSynth team may change it
 * from time to time. Instead, copy it.
 */
public class Template extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      RigidBody ball = RigidBody.createSphere (
         "ball", /*radius*/.1, /*density*/1000, /*nslices*/32);
      mech.addRigidBody (ball);
   }
}
