/**
 * Copyright (c) 2020, by the Authors: Fabien Pean
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import maspack.matrix.Matrix3d;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.render.RenderProps;
import maspack.render.Renderer;

public class BSpline3dElement extends FemElement3d {
   private static FemElementRenderer myRenderer;
   
   IntegrationPoint3d[] integrationPoints = null;
   IntegrationPoint3d warpingPoint = null;
   
   public SplineBasis[] uvw = new SplineBasis[3];

   @Override
   public double[] getNodeMassWeights () {
      double[] mass = new double[numNodes ()];
      for(int inode = 0; inode<numNodes ();++inode)
      for(IntegrationPoint3d ip : getIntegrationPoints()) {
         double Ni = getN(inode, ip.getCoords());
         VectorNd N = ip.getShapeWeights ();
         double tmp_mass = 0;
         for(int j = 0; j < N.size(); j++) {
            tmp_mass += Ni * N.get(j);
         }
         tmp_mass *= getIntegrationData ()[inode].myDetJ0 * ip.getWeight();
         mass[inode] += tmp_mass;
      }
      return mass;
   }

   @Override
   public int numIntegrationPoints() {
      if(integrationPoints == null) {
         getIntegrationPoints();
      }
      return integrationPoints.length;
   }

   @Override
   public double[] getIntegrationCoords() {
      SplineBasis u = uvw[0];
      SplineBasis v = uvw[1];
      SplineBasis w = uvw[2];

      double[] ku = u.getKnotVector();
      int pu = u.getDegree();

      double[] kv = v.getKnotVector();
      int pv = v.getDegree();

      double[] kw = w.getKnotVector();
      int pw = w.getDegree();

      int nu = u.getNumberNonZeroKnotSpan();
      int nv = v.getNumberNonZeroKnotSpan();
      int nw = w.getNumberNonZeroKnotSpan();

      int n = nu * nv * nw;

      List<QuadraturePoint> qps = new ArrayList<>(pu * pv * pw * n);
      for(int i = pu; i < ku.length - pu - 1; i++) {
         for(int j = pv; j < kv.length - pv - 1; j++) {
            for(int k = pw; k < kw.length - pw - 1; k++) {

               double du = ku[i + 1] - ku[i];
               double dv = kv[j + 1] - kv[j];
               double dw = kw[k + 1] - kw[k];

               if(du == 0 || dv == 0 || dw == 0) // not a valid span
                  continue;

               int ngp_u =  pu + 1;
               int ngp_v =  pv + 1;
               int ngp_w =  pw + 1;  
               
               ngp_u = u.getNumberZeroKnotSpanInRange(i) > 1 ? ngp_u : ngp_u-1; // Reduce number of Gauss points for regular elements
               ngp_v = v.getNumberZeroKnotSpanInRange(j) > 1 ? ngp_v : ngp_v-1;
               ngp_w = w.getNumberZeroKnotSpanInRange(k) > 1 ? ngp_w : ngp_w-1;
               
               ngp_u = pu == 1 ? 2 : ngp_u; // Special case when polynomial degree is 1, always return 2
               ngp_v = pv == 1 ? 2 : ngp_v;
               ngp_w = pw == 1 ? 2 : ngp_w;

               QuadraturePoint[] tmp = null;
               if((pu == 3 || pv == 3 || pw == 3)
                  && (ngp_u<4 && ngp_v<4 && ngp_w<4)) {
                  tmp = QuadratureGaussLegendre.useStroud13();
               } else {
                  tmp = QuadratureGaussLegendre.computePoints(ngp_u, ngp_v, ngp_w);
               }
               
               for(QuadraturePoint qp : tmp) {
                  qp.add(1, 1, 1);
                  qp.scale(0.5);
                  qp.scale(du, dv, dw);
                  qp.add(ku[i], kv[j], kw[k]);
                  qp.w *= 0.125 * du * dv * dw;
               }

               qps.addAll(Arrays.asList(tmp));

            }
         }
      }
      return qps.stream().flatMapToDouble(x-> Arrays.stream (QuadraturePoint.toArray(x))).toArray ();
   }
   @Override
   public IntegrationPoint3d[] getIntegrationPoints() {
      if(integrationPoints == null) {
         integrationPoints = createIntegrationPoints();
      }

      return integrationPoints;
   }

   @Override
   public IntegrationPoint3d getWarpingPoint() {
      if(warpingPoint == null) {
         double du = uvw[0].getLastKnotValid() - uvw[0].getFirstKnotValid();
         double dv = uvw[1].getLastKnotValid() - uvw[1].getFirstKnotValid();
         double dw = uvw[2].getLastKnotValid() - uvw[2].getFirstKnotValid();
         QuadraturePoint qp = new QuadraturePoint(0, 0, 0, 8);
         qp.add(1, 1, 1);
         qp.scale(0.5);
         qp.scale(du, dv, dw);
         qp.add(uvw[0].getFirstKnotValid(), uvw[1].getFirstKnotValid(), uvw[2].getFirstKnotValid());
         qp.w *= 0.125 * du * dv *dw;
         warpingPoint = IntegrationPoint3d.create(this, qp.x,qp.y,qp.z,qp.w);
      }
      return warpingPoint;
   }

   @Override
   public int[] getEdgeIndices() {
      int nu = uvw[0].getNumberBasis();
      int nv = uvw[1].getNumberBasis();
      int nw = uvw[2].getNumberBasis();

      int p = uvw[0].getDegree();
      int q = uvw[1].getDegree();
      int r = uvw[2].getDegree();

      class Edge {
         int a;
         int b;

         Edge(int a, int b) {
            this.a = a;
            this.b = b;
         }

         @Override
         public int hashCode() {
            int res = Math.max(a, b);
            res = (res << 16) | (res >>> 16); // exchange top and bottom 16
                                              // bits.
            res = res ^ Math.min(a, b);
            return res;
         }

         @Override
         public boolean equals(Object o) {
            if(o == this)
               return true;
            if(!(o instanceof Edge))
               return false;
            Edge e = (Edge)o;
            return (a == e.a && b == e.b) || (a == e.b && b == e.a);
         }

         @Override
         public String toString() {
            return a + "-" + b;
         }
      }
      
      Set<Edge> edges = new HashSet<>();
      for(int i = 0; i < nu - 1; i++) {
         for(int j = 0; j < nv - 1; j++) {
            for(int k = 0; k < nw - 1; k++) {
               edges.add(new Edge(nv * nw * i + nw * j + k, nv * nw * (i + 1) + nw * j + k));
               edges.add(new Edge(nv * nw * i + nw * j + k, nv * nw * i + nw * (j + 1) + k));
               edges.add(new Edge(nv * nw * (i + 1) + nw * (j + 1) + k, nv * nw * (i + 1) + nw * j + k));
               edges.add(new Edge(nv * nw * (i + 1) + nw * (j + 1) + k, nv * nw * i + nw * (j + 1) + k));

               edges.add(new Edge(nv * nw * i + nw * j + k + 1, nv * nw * (i + 1) + nw * j + k + 1));
               edges.add(new Edge(nv * nw * i + nw * j + k + 1, nv * nw * i + nw * (j + 1) + k + 1));
               edges.add(new Edge(nv * nw * (i + 1) + nw * (j + 1) + k + 1, nv * nw * (i + 1) + nw * j + k + 1));
               edges.add(new Edge(nv * nw * (i + 1) + nw * (j + 1) + k + 1, nv * nw * i + nw * (j + 1) + k + 1));

               edges.add(new Edge(nv * nw * i + nw * j + k, nv * nw * i + nw * j + k + 1));
               edges.add(new Edge(nv * nw * (i + 1) + nw * j + k, nv * nw * (i + 1) + nw * j + k + 1));
               edges.add(new Edge(nv * nw * (i + 1) + nw * (j + 1) + k, nv * nw * (i + 1) + nw * (j + 1) + k + 1));
               edges.add(new Edge(nv * nw * i + nw * (j + 1) + k, nv * nw * i + nw * (j + 1) + k + 1));
            }
         }
      }

      int[] out = new int[3 * edges.size()];
      int i = 0;
      for(Edge edge : edges) {
         out[3 * i] = 2;
         out[3 * i + 1] = edge.a;
         out[3 * i + 2] = edge.b;
         i++;
      }
      return out;
   }

   @Override
   public int[] getFaceIndices() {
      int nu = uvw[0].getNumberBasis();
      int nv = uvw[1].getNumberBasis();
      int nw = uvw[2].getNumberBasis();

      class Face {
         int a, b, c, d;

         Face(int aa, int bb, int cc, int dd) {
            a = aa;
            b = bb;
            c = cc;
            d = dd;
         }

         public void flip() {
            int tmp = b;
            b = d;
            d = tmp;
         }
      }
      List<Face> faces = new ArrayList<>();
      for(int i = 0; i < nu - 1; i++) {
         for(int j = 0; j < nv - 1; j++) {
            Face last = null;
            for(int k = nw - 1; k >= 0; k -= nw - 1) {
               last =
                  new Face(
                     nv * nw * i + nw * j + k, nv * nw * (i + 1) + nw * j + k, nv * nw * (i + 1) + nw * (j + 1) + k,
                     nv * nw * i + nw * (j + 1) + k);
               faces.add(last);
            }
            last.flip();
         }
      }
      for(int i = 0; i < nu - 1; i++) {

         for(int k = 0; k < nw - 1; k++) {
            Face last = null;
            for(int j = nv - 1; j >= 0; j -= nv - 1) {
               last =
                  new Face(
                     nv * nw * i + nw * j + k, nv * nw * i + nw * j + k + 1, nv * nw * (i + 1) + nw * j + k + 1,
                     nv * nw * (i + 1) + nw * j + k);
               faces.add(last);
            }
            last.flip();
         }
      }
      for(int j = 0; j < nv - 1; j++) {
         for(int k = 0; k < nw - 1; k++) {
            Face last = null;
            for(int i = nu - 1; i >= 0; i -= nu - 1) {
               last =
                  new Face(
                     nv * nw * i + nw * j + k, nv * nw * i + nw * (j + 1) + k, nv * nw * i + nw * (j + 1) + k + 1,
                     nv * nw * i + nw * j + k + 1);
               faces.add(last);
            }
            last.flip();

         }
      }

      int[] out = new int[5 * faces.size()];
      int i = 0;
      for(Face face : faces) {
         out[5 * i] = 4;
         out[5 * i + 1] = face.a;
         out[5 * i + 2] = face.b;
         out[5 * i + 3] = face.c;
         out[5 * i + 4] = face.d;

         i++;
      }
      return out;
   }

   public  Point3d[][] getFaceCoords(int resolution) {
      double usta = uvw[0].getFirstKnotValid();
      double uend = uvw[0].getLastKnotValid();
      double vsta = uvw[1].getFirstKnotValid();
      double vend = uvw[1].getLastKnotValid();
      double wsta = uvw[2].getFirstKnotValid();
      double wend = uvw[2].getLastKnotValid();

      int nku = uvw[0].getNumberNonZeroKnotSpan();
      int nkv = uvw[1].getNumberNonZeroKnotSpan();
      int nkw = uvw[2].getNumberNonZeroKnotSpan();

      int pu = uvw[0].getDegree();
      int pv = uvw[1].getDegree();
      int pw = uvw[2].getDegree();
      
      int nu = (pu * resolution * nku);
      int nv = (pv * resolution * nkv);
      int nw = (pw * resolution * nkw);
      
      double du = (uend - usta) / nu;
      double dv = (vend - vsta) / nv;
      double dw = (wend - wsta) / nw;

      List<Point3d[]> faces = new ArrayList<>();
      for(int i = 0; i < nu; i++) {
         for(int j = 0; j < nv; j++) {
            for(int k = 0; k <= 1; k++) {
               Point3d a = new Point3d(usta + du * i, vsta + dv * j, (1 - k) * wsta + k * wend);
               Point3d b = new Point3d(usta + du * (i + 1), vsta + dv * j, (1 - k) * wsta + k * wend);
               Point3d c = new Point3d(usta + du * (i + 1), vsta + dv * (j + 1), (1 - k) * wsta + k * wend);
               Point3d d = new Point3d(usta + du * i, vsta + dv * (j + 1), (1 - k) * wsta + k * wend);
               if(k == 1)
                  faces.add(new Point3d[] { a, b, c, d });
               else
                  faces.add(new Point3d[] { a, d, c, b });
            }
         }
      }
      for(int i = 0; i < nu; i++) {
         for(int k = 0; k < nw; k++) {
            for(int j = 0; j <= 1; j++) {
               Point3d a = new Point3d(usta + du * i, (1 - j) * vsta + j * vend, wsta + dw * k);
               Point3d b = new Point3d(usta + du * i, (1 - j) * vsta + j * vend, wsta + dw * (k + 1));
               Point3d c = new Point3d(usta + du * (i + 1), (1 - j) * vsta + j * vend, wsta + dw * (k + 1));
               Point3d d = new Point3d(usta + du * (i + 1), (1 - j) * vsta + j * vend, wsta + dw * k);
               if(j == 1)
                  faces.add(new Point3d[] { a, b, c, d });
               else
                  faces.add(new Point3d[] { a, d, c, b });
            }
         }
      }
      for(int k = 0; k < nw; k++) {
         for(int j = 0; j < nv; j++) {
            for(int i = 0; i <= 1; i++) {
               Point3d a = new Point3d((1 - i) * usta + i * uend, vsta + dv * j, wsta + dw * k);
               Point3d b = new Point3d((1 - i) * usta + i * uend, vsta + dv * (j + 1), wsta + dw * k);
               Point3d c = new Point3d((1 - i) * usta + i * uend, vsta + dv * (j + 1), wsta + dw * (k + 1));
               Point3d d = new Point3d((1 - i) * usta + i * uend, vsta + dv * j, wsta + dw * (k + 1));
               if(i == 1)
                  faces.add(new Point3d[] { a, b, c, d });
               else
                  faces.add(new Point3d[] { a, d, c, b });
            }
         }
      }
      return faces.toArray(new Point3d[][] {});
   }

   @Override
   public boolean coordsAreInside(Vector3d coords) {
      double eps = 1e-12;
      if(coords.x < uvw[0].getFirstKnotValid() - eps || coords.x > uvw[0].getLastKnotValid() + eps)
         return false;
      if(coords.y < uvw[1].getFirstKnotValid() - eps || coords.y > uvw[1].getLastKnotValid() + eps)
         return false;
      if(coords.z < uvw[2].getFirstKnotValid() - eps || coords.z > uvw[2].getLastKnotValid() + eps)
         return false;

      return true;
   }
   
   public void clamp(Vector3d coords) {
      coords.x=uvw[0].clamp(coords.x);
      coords.y=uvw[1].clamp(coords.y);
      coords.z=uvw[2].clamp(coords.z);
   }

   @Override
   public double[] getNodeCoords() {
      double[][] nodeCoords = new double[numNodes()][3];
      int nu = uvw[0].getNumberBasis();
      int nv = uvw[1].getNumberBasis();
      int nw = uvw[2].getNumberBasis();

      int idx = 0;
      for(int i = 0; i < nu; i++) {
         for(int j = 0; j < nv; j++) {
            for(int k = 0; k < nw; k++) {
               nodeCoords[idx][0] = uvw[0].clamp(uvw[0].computeGrevilleAbscissae(i));
               nodeCoords[idx][1] = uvw[1].clamp(uvw[1].computeGrevilleAbscissae(j));
               nodeCoords[idx][2] = uvw[2].clamp(uvw[2].computeGrevilleAbscissae(k));
               idx++;
            }
         }
      }
      return Stream.of(nodeCoords).flatMapToDouble (Arrays::stream).toArray ();
   }

   @Override
   public void render(Renderer renderer, RenderProps props, int flags) {
      if(myRenderer == null) {
         myRenderer = new FemElementRenderer(this);
      }
      myRenderer.render(renderer, this, props);
   }

   @Override
   public void renderWidget(Renderer renderer, double size, RenderProps props) {
      if(myRenderer == null) {
         myRenderer = new FemElementRenderer(this);
      }
      myRenderer.renderWidget(renderer, this, size, props);
   }

   private VectorNd computeBasisFunction(Vector3d parametricCoordinates) {
      double[][] basis = new double[3][];
      for(int i = 0; i < 3; i++)
         basis[i] = uvw[i].computeBasisFunction(parametricCoordinates.get(i));

      int numberNNZBasisU = basis[0].length;
      int numberNNZBasisV = basis[1].length;
      int numberNNZBasisW = basis[2].length;
      int numberNNZBasis = numberNNZBasisU * numberNNZBasisV * numberNNZBasisW;
      VectorNd output = new VectorNd(numberNNZBasis);

      double[] basisUVW = output.getBuffer();
      int count = 0;
      for(int itBasisU = 0; itBasisU != basis[0].length; itBasisU++) {
         for(int itBasisV = 0; itBasisV != basis[1].length; itBasisV++) {
            for(int itBasisW = 0; itBasisW != basis[2].length; itBasisW++) {
               basisUVW[count] = basis[0][itBasisU] * basis[1][itBasisV] * basis[2][itBasisW];
               count++;
            }
         }
      }
      return output;
   }

   private Vector3d[] computeBasisFunctionGradient(Vector3d parametricCoordinates) {
      double[][] basis = new double[3][];
      for(int i = 0; i < 3; i++)
         basis[i] = uvw[i].computeBasisFunction(parametricCoordinates.get(i));

      double[][] basisGradient = new double[3][];
      for(int i = 0; i < 3; i++)
         basisGradient[i] = uvw[i].computeBasisFunctionGradient(parametricCoordinates.get(i));

      int numberNNZBasisU = basis[0].length;
      int numberNNZBasisV = basis[1].length;
      int numberNNZBasisW = basis[2].length;
      int numberNNZBasis = numberNNZBasisU * numberNNZBasisV * numberNNZBasisW;
      Vector3d[] gradient = new Vector3d[numberNNZBasis];
      for(int i = 0; i < numberNNZBasis; i++)
         gradient[i] = new Vector3d();

      int count = 0;
      for(int itBasisU = 0, itBasisDU = 0; itBasisU != basis[0].length; itBasisU++, itBasisDU++) {
         for(int itBasisV = 0, itBasisDV = 0; itBasisV != basis[1].length; itBasisV++, itBasisDV++) {
            for(int itBasisW = 0, itBasisDW = 0; itBasisW != basis[2].length; itBasisW++, itBasisDW++) {
               gradient[count].x = basisGradient[0][itBasisDU] * basis[1][itBasisV] * basis[2][itBasisW];
               gradient[count].y = basis[0][itBasisU] * basisGradient[1][itBasisDV] * basis[2][itBasisW];
               gradient[count].z = basis[0][itBasisU] * basis[1][itBasisV] * basisGradient[2][itBasisDW];
               count++;
            }
         }
      }
      return gradient;
   }

   public double getN(int i, double x, SplineBasis u) {
      int ku = u.findKnotSpan(x);
      int p = u.getDegree();

      if(i < ku - p)
         return 0;
      if(i > ku)
         return 0;

      double[] Nu = u.computeBasisFunction(x);

      double bu = Nu[i + p - ku];

      return bu;
   }

   @Override
   public double getN(int n, Vector3d coords) {
      double x = coords.x;
      double y = coords.y;
      double z = coords.z;

      SplineBasis u = uvw[0];
      SplineBasis v = uvw[1];
      SplineBasis w = uvw[2];

      int nu = uvw[0].getNumberBasis();
      int nv = uvw[1].getNumberBasis();
      int nw = uvw[2].getNumberBasis();

      int i = n / nw / nv;
      int j = n / nw % nv;
      int k = n % nw;

      double bu = getN(i, x, u);
      double bv = getN(j, y, v);
      double bw = getN(k, z, w);

      return bu * bv * bw;
   }

   @Override
   public void getdNds(Vector3d dNds, int n, Vector3d coords) {
      Vector3d[] dN = computeBasisFunctionGradient(coords);

      double x = coords.x;
      double y = coords.y;
      double z = coords.z;

      SplineBasis u = uvw[0];
      SplineBasis v = uvw[1];
      SplineBasis w = uvw[2];

      int nu = uvw[0].getNumberBasis();
      int nv = uvw[1].getNumberBasis();
      int nw = uvw[2].getNumberBasis();

      int ku = u.findKnotSpan(x);
      int kv = v.findKnotSpan(y);
      int kw = w.findKnotSpan(z);

      int p = u.getDegree();
      int q = v.getDegree();
      int r = w.getDegree();

      int i = n / nw / nv;
      int j = n / nw % nv;
      int k = n % nw;

      if(i < ku - p || i >= ku + 1 || j < kv - q || j >= kv + 1 || k < kw - r || k >= kw + 1) {
         dNds.set(0, 0, 0);
         return;
      }

      int idx = (i - ku + p) * (q + 1) * (r + 1) + (j - kv + q) * (r + 1) + (k - kw + r);

      dNds.set(dN[idx]);
   }

   @Override
   public MatrixNd getNodalExtrapolationMatrix () {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public int[] getTriangulatedFaceIndices () {
      // TODO Auto-generated method stub
      return null;
   }
}
