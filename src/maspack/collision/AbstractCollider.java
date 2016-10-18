package maspack.collision;

import maspack.geometry.PolygonalMesh;

public interface AbstractCollider {
   
   public ContactInfo getContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1);

//   public double getEpsilon();
//
//   public double getPointTolerance();
//
//   public double getRegionTolerance();
//
//   public void setPointTolerance (double d);
//
//   public void setRegionTolerance (double d);

}
// mesh0=
