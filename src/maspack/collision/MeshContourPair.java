package maspack.collision;

public class MeshContourPair {
    IntersectionContour contour1;
    IntersectionContour contour2;
    
    public MeshContourPair (IntersectionContour c1, IntersectionContour c2) {
        contour1 = c1;
        contour2 = c2;
     }

    public boolean equals(Object obj) {
       if (obj instanceof MeshContourPair) {
          MeshContourPair pair = (MeshContourPair)obj;
          return ((contour1 == pair.contour1 && contour2 == pair.contour2) ||
                  (contour1 == pair.contour2 && contour2 == pair.contour1));
       }
       else {
          return false;
       }
    }

    public int hashCode() {
	return(contour1.hashCode() + contour2.hashCode());
    }
}

