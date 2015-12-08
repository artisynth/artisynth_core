/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.render.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Class to attach a frame to a FEM and it's coordinate frame.
 */
public class FrameFem3dAttachmentTest extends UnitTest {

   FemModel3d myFem;
   FrameFem3dAttachment myAttachment;

   FrameFem3dAttachmentTest() {
      myFem = FemFactory.createHexGrid (null, 1.0, 0.25, 0.25, 1, 1, 1);
      myAttachment = new FrameFem3dAttachment();
      RigidTransform3d TSW = new RigidTransform3d(0.1, 0.1, 0);
      myAttachment.setFromElement (TSW, myFem.getElements().get(0));
   }

   public void test() {
   }   

   public static void main (String[] args) {
      FrameFem3dAttachmentTest tester = new FrameFem3dAttachmentTest();
      tester.runtest();
   }
}
