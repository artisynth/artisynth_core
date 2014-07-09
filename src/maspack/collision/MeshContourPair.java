package maspack.collision;

public class MeshContourPair {
    MeshIntersectionContour contour1;
    MeshIntersectionContour contour2;
    
    public MeshContourPair (MeshIntersectionContour c1, MeshIntersectionContour c2) {
        contour1 = c1;
        contour2 = c2;
     }

    public boolean equals(MeshContourPair aPair) {
	return (
		(contour1 == aPair.contour1 && contour2 == aPair.contour2)
		|| (contour1 == aPair.contour2 && contour2 == aPair.contour1)
	);
    }

    public int hashCode() {
	return(contour1.hashCode() + contour2.hashCode());
    }
}

