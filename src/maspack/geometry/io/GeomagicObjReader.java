package maspack.geometry.io;

import java.io.IOException;

import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.util.ReaderTokenizer;

public class GeomagicObjReader extends WavefrontReader {

   // Vector4d offset = new Vector4d();
   boolean firstVertex = true;

   public GeomagicObjReader (java.io.Reader reader) {
      super (reader);
   }
   
   protected boolean processLine (ReaderTokenizer rtok) throws IOException {

      if (curve != null || surface != null) {
         if (!rtok.sval.equals ("parm") && !rtok.sval.equals ("end")) {
            throw new IOException (
               "unexpected keyword '" + rtok.sval +
               "' between curv/surf and end, line " + rtok.lineno());
         }
      }
      int lineno = rtok.lineno();
      if (rtok.sval.equals ("v")) {
         Vector4d pnt = new Vector4d();
         Vector3d dummy = new Vector3d();
         if ((pnt.x = scanDouble (rtok)) != pnt.x ||
             (pnt.y = scanDouble (rtok)) != pnt.y ||
             (pnt.z = scanDouble (rtok)) != pnt.z ||
             (dummy.x = scanDouble (rtok)) != dummy.x ||
             (dummy.y = scanDouble (rtok)) != dummy.y ||
             (dummy.z = scanDouble (rtok)) != dummy.z) {
            throw new IOException ("vertex coordinate expected, line " + lineno);
         }
         pnt.w = scanOptionalNumber (rtok, "vertex w coordinate", 1);
         // if (firstVertex)
         // {
         // offset.set(pnt);
         // offset.w = 0.0;
         // firstVertex = false;
         // }
         // pnt.sub(offset);
         vertexList.add (pnt);
         return true;
      }
      else {
         return super.processLine (rtok);
      }
   }

}
