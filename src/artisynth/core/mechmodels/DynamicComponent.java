/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformableGeometry;
import maspack.util.DataBuffer;
import maspack.matrix.Matrix;
import maspack.matrix.MatrixBlock;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.Vector3d;

import java.util.*;

public interface DynamicComponent
   extends DynamicAgent, ModelComponent, ForceEffector, TransformableGeometry {
}
