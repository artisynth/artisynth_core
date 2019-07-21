package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

public abstract class ScalarFemField
   extends FemFieldComp implements ScalarField {

   double myDefaultValue = 0;   

   public ScalarFemField () {
   }

   public ScalarFemField (FemModel3d fem) {
      myDefaultValue = 0;
      setFem (fem);
   }

   public ScalarFemField (FemModel3d fem, double defaultValue) {
      myDefaultValue = defaultValue;
      setFem (fem);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "defaultValue")) {
         myDefaultValue = rtok.scanNumber();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("defaultValue=" + fmt.format(myDefaultValue));
   }

   public abstract double getValue (Point3d pos);
}
