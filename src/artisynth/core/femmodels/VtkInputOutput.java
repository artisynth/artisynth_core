package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;

import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.femmodels.PyramidElement;
import artisynth.core.femmodels.TetElement;
import artisynth.core.femmodels.WedgeElement;

import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;

public class VtkInputOutput
{   
    ArrayList<String>    pointData_scalar_names; 
    ArrayList<double[]> pointData_scalar_data;
    ArrayList<String>               pointData_vector_names;
    ArrayList<ArrayList<double[]>> pointData_vector_data;
    ArrayList<String>    cellData_scalar_names;
    ArrayList<double[]> cellData_scalar_data;
    ArrayList<String>               cellData_vector_names;
    ArrayList<ArrayList<double[]>> cellData_vector_data;

    public void addPointDataScalars(String fieldName, double[] data)
    {
        if (pointData_scalar_names == null)
        {
            pointData_scalar_names = new ArrayList<String>();
            pointData_scalar_data = new ArrayList<double[]>();
        }
        pointData_scalar_names.add(fieldName);
        pointData_scalar_data.add(data);
    }

    public void addPointDataVectors(String fieldName, ArrayList<double[]> data)
    {
        if (pointData_vector_names == null)
        {
            pointData_vector_names = new ArrayList<String>();
            pointData_vector_data = new ArrayList<ArrayList<double[]>>();
        }
        pointData_vector_names.add(fieldName);
        pointData_vector_data.add(data);
    }

    // --- writing PolygonalMesh to VTK Polydata --- //

    // This non-static class can be used with functions to add scalar, vector data
    public void write(String filename, PolygonalMesh mesh)
    {
        writeVTK(filename, mesh, 
            pointData_scalar_names, pointData_scalar_data, 
            pointData_vector_names, pointData_vector_data, 
            cellData_scalar_names, cellData_scalar_data,
            cellData_vector_names, cellData_vector_data);
    }
    public void write(String filename, FemModel3d fem)
    {
        writeVTK(filename, fem, 
            pointData_scalar_names, pointData_scalar_data, 
            pointData_vector_names, pointData_vector_data, 
            cellData_scalar_names, cellData_scalar_data,
            cellData_vector_names, cellData_vector_data);
    }

    // --- below are static methods --- //

    public static void writeVTK(String filename, PolygonalMesh mesh)
    {
        writeVTK (filename, mesh, null, null, null, null, null, null, null, null);
    }

    public static void writeVTK(String filename, PolygonalMesh mesh, 
        ArrayList<String> pointData_scalar_names, ArrayList<double[]> pointData_scalar_data,
        ArrayList<String> pointData_vector_names, ArrayList<ArrayList<double[]>> pointData_vector_data,
        ArrayList<String> cellData_scalar_names, ArrayList<double[]> cellData_scalar_data,
        ArrayList<String> cellData_vector_names, ArrayList<ArrayList<double[]>> cellData_vector_data)
    {
        // this writes a surface geometry
        int nPoints = mesh.numVertices();
        int nCells = mesh.numFaces();

        try
        {
            PrintWriter file = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
            file.println("# vtk DataFile Version 3.0");	// header
            file.println("SurfaceMesh");			// title
            file.println("ASCII");				// data type: ASCII or BINARY
            file.println("DATASET POLYDATA");		// dataset type: STRUCTURED_POINTS, STRUCTURED_GRID, UNSTRUCTURED_GRID, POLYDATA, RECTILINEAR_GRID,  FIELD

            // write points
            file.println();
            file.printf("POINTS %d float\n", nPoints);
            for (Vertex3d v : mesh.getVertices())
            {
                Point3d p = v.getPosition();
                file.printf("%f %f %f\n", p.x, p.y, p.z);
            }

            // write cells
            int nCellPoints = 0;
            for (Face f : mesh.getFaces())
                nCellPoints = nCellPoints + f.getVertexIndices().length;

            file.println();
            file.printf("POLYGONS %d %d\n", nCells, nCellPoints + nCells);
            for (Face f : mesh.getFaces())
            {
                int[] indices = f.getVertexIndices();
                int nInd = indices.length;
                file.printf("%d", nInd);
                for (int a=0; a<nInd; a++)
                    file.printf(" %d", indices[a]);
                file.println();
            }

            // write point data
            writePointData(file, nPoints, pointData_scalar_names, pointData_scalar_data, pointData_vector_names, pointData_vector_data);

            // write cell data
            writeCellData(file, nCells, cellData_scalar_names, cellData_scalar_data, cellData_vector_names, cellData_vector_data);

            //file.flush();
            file.close();
        }
        catch(Exception e)
        {
        }
    }

    // --- writing FEM to unstructured grid --- //
    
    public static void writeVTK(String filename, FemModel3d fem)
    {
        writeVTK(filename, fem, null, null, null, null, null, null, null, null);
    }

    public static void writeVTK(String filename, FemModel3d fem, 
        ArrayList<String> pointData_scalar_names, ArrayList<double[]> pointData_scalar_data,
        ArrayList<String> pointData_vector_names, ArrayList<ArrayList<double[]>> pointData_vector_data,
        ArrayList<String> cellData_scalar_names, ArrayList<double[]> cellData_scalar_data,
        ArrayList<String> cellData_vector_names, ArrayList<ArrayList<double[]>> cellData_vector_data)
    {
        // this writes a surface geometry
        int nPoints = fem.getNodes().size();
        int nCells  = fem.getElements().size();

        try
        {
            PrintWriter file = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
            file.println("# vtk DataFile Version 3.0");	// header
            file.println("VolumeMesh");			// title
            file.println("ASCII");				// data type: ASCII or BINARY
            file.println("DATASET UNSTRUCTURED_GRID");		// dataset type: STRUCTURED_POINTS, STRUCTURED_GRID, UNSTRUCTURED_GRID, POLYDATA, RECTILINEAR_GRID,  FIELD

            // write points
            file.println();
            file.printf("POINTS %d float\n", nPoints);
            for (FemNode3d n : fem.getNodes())
            {
                Point3d p = n.getPosition();
                file.printf("%f %f %f\n", p.x, p.y, p.z);
            }

            // write cells
            int nCellPoints = 0;
            int[] numNodes = new int[nCells];
            for (int a=0; a<nCells; a++)
            {
                int nNodes = fem.getElement(a).getNodes().length;
                nCellPoints = nCellPoints + nNodes;
                numNodes[a] = nNodes;
            }
            
            // correction in case indices start at 1 rather than 0
            int iAdj = 0;
            if (fem.getNodes().getByNumber(0) == null)
            {
                iAdj = 1;
                System.out.println("VTK Writer warning: node indices start at 0 rather than 1");
            }
            
            file.println();
            file.printf("CELLS %d %d\n", nCells, nCellPoints + nCells);
            for (int a=0; a<nCells; a++)
            {
                FemNode3d[] femNodes = fem.getElement(a).getNodes();
                //System.out.printf ("Elem number %d,\t nNodes=%d, type=%s\n", a, numNodes[a], fem.getElement(a).toString() );
                // XXX: quad elements are reduced to linear with this output.
                
                if      ( (fem.getElement(a) instanceof TetElement) || (fem.getElement(a) instanceof QuadtetElement) )
                {    
                    // VTK and Artisynth agree in node order
                    file.printf("%d %d %d %d %d\n", numNodes[a],
                        fem.getNodes().indexOf(femNodes[0]), 
                        fem.getNodes().indexOf(femNodes[1]),
                        fem.getNodes().indexOf(femNodes[2]), 
                        fem.getNodes().indexOf(femNodes[3]) );
                        //femNodes[0].myNumber-iAdj, femNodes[1].myNumber-iAdj, femNodes[2].myNumber-iAdj, femNodes[3].myNumber-iAdj);
                    
                }
                else if ( (fem.getElement(a) instanceof HexElement) || (fem.getElement(a) instanceof QuadhexElement) )
                {
                    // VTK and Artisynth differ in node order
                    file.printf("%d %d %d %d %d %d %d %d %d\n", numNodes[a], 
                        fem.getNodes().indexOf(femNodes[0]), 
                        fem.getNodes().indexOf(femNodes[3]),
                        fem.getNodes().indexOf(femNodes[2]), 
                        fem.getNodes().indexOf(femNodes[1]),
                        fem.getNodes().indexOf(femNodes[4]), 
                        fem.getNodes().indexOf(femNodes[7]),
                        fem.getNodes().indexOf(femNodes[6]), 
                        fem.getNodes().indexOf(femNodes[5]) );
//                        femNodes[0].myNumber-iAdj, femNodes[3].myNumber-iAdj, femNodes[2].myNumber-iAdj, femNodes[1].myNumber-iAdj, 
//                        femNodes[4].myNumber-iAdj, femNodes[7].myNumber-iAdj, femNodes[6].myNumber-iAdj, femNodes[5].myNumber-iAdj);
                }
                else if ( (fem.getElement(a) instanceof WedgeElement) || (fem.getElement(a) instanceof QuadwedgeElement) )
                {
                    // VTK and Artisynth differ in node order
                    file.printf("%d %d %d %d %d %d %d\n", numNodes[a], 
                        fem.getNodes().indexOf(femNodes[0]), 
                        fem.getNodes().indexOf(femNodes[2]), 
                        fem.getNodes().indexOf(femNodes[1]),
                        fem.getNodes().indexOf(femNodes[3]), 
                        fem.getNodes().indexOf(femNodes[5]), 
                        fem.getNodes().indexOf(femNodes[4]) );
//                        femNodes[0].myNumber-iAdj, femNodes[2].myNumber-iAdj, femNodes[1].myNumber-iAdj, 
//                        femNodes[3].myNumber-iAdj, femNodes[5].myNumber-iAdj, femNodes[4].myNumber-iAdj);
                }
                else if ( (fem.getElement(a) instanceof PyramidElement) || (fem.getElement(a) instanceof QuadpyramidElement) )
                {
                    // VTK and Artisynth agree in node order
                    file.printf("%d %d %d %d %d %d\n", numNodes[a],
                        fem.getNodes().indexOf(femNodes[0]), 
                        fem.getNodes().indexOf(femNodes[1]), 
                        fem.getNodes().indexOf(femNodes[2]),
                        fem.getNodes().indexOf(femNodes[3]),  
                        fem.getNodes().indexOf(femNodes[4]) );
//                        femNodes[0].myNumber-iAdj, femNodes[1].myNumber-iAdj, femNodes[2].myNumber-iAdj, femNodes[3].myNumber-iAdj, 
//                        femNodes[4].myNumber-iAdj);
                }
                else
                {
                    file.printf("%d", numNodes[a]);
                    for (int b=0; b<numNodes[a]; b++)
                        file.printf(" %d", femNodes[b].myNumber-iAdj);
                    file.println();
                }
            }
            
            file.println();
            file.printf("CELL_TYPES %d\n", nCells);
            for (int a=0; a<nCells; a++)
            {
                if       (numNodes[a] == 4)
                    file.printf("%d\n", 10);	// tetrahedron == 10
                else if (numNodes[a] == 8)
                    file.printf("%d\n", 12);	// hexahedron == 12
                else if (numNodes[a] == 6)
                    file.printf("%d\n", 13);	// wedge == 13
                else if (numNodes[a] == 5)
                    file.printf("%d\n", 14);	// pyramid == 14
            }

            // write point data
            writePointData(file, nPoints, pointData_scalar_names, pointData_scalar_data, pointData_vector_names, pointData_vector_data);

            // write cell data
            writeCellData(file, nCells, cellData_scalar_names, cellData_scalar_data, cellData_vector_names, cellData_vector_data);

            //file.flush();
            file.close();
        }
        catch(Exception e)
        {
            System.out.println("Warning: exception caught in VTK writer.");
        }
    }

    // --- some generic methods --- //

    public static void writePointData(PrintWriter file, int nPoints,
        ArrayList<String> scalar_names, ArrayList<double[]> scalar_data,
        ArrayList<String> vector_names, ArrayList<ArrayList<double[]>> vector_data)
    {
        writeFieldData(file, nPoints, "POINT_DATA", scalar_names, scalar_data, vector_names, vector_data);
    }

    public static void writeCellData(PrintWriter file, int nCells,
        ArrayList<String> scalar_names, ArrayList<double[]> scalar_data,
        ArrayList<String> vector_names, ArrayList<ArrayList<double[]>> vector_data)
    {
        writeFieldData(file, nCells, "CELL_DATA", scalar_names, scalar_data, vector_names, vector_data);
    }

    public static void writeFieldData(PrintWriter file, int nComponents, String typeStr,
        ArrayList<String> scalar_names, ArrayList<double[]> scalar_data,
        ArrayList<String> vector_names, ArrayList<ArrayList<double[]>> vector_data)
    {
        //TODO: rather than writing as field data, I can write as SCALARS or VECTORS
        // or, I can generalize fieldData as a double[n] array...cut back on some code...
        int nPD_scalars = 0;
        int nPD_vectors = 0;
        if ( (scalar_names != null) && (scalar_data != null) )
            nPD_scalars = scalar_names.size();
        if ( (vector_names != null) && (vector_data != null) )
            nPD_vectors = vector_names.size();
        int nFields = nPD_scalars + nPD_vectors;
        if (nFields > 0)
        {
            file.println();
            file.printf("%s %d\n", typeStr, nComponents);
            file.printf("FIELD fieldData %d\n", nFields);

            for (int a=0; a<nPD_scalars; a++)
            {
                String name = scalar_names.get(a);
                double[] data = scalar_data.get(a);

                file.printf("%s %d %d float\n", name, 1, nComponents);	// name, num comp, num points, type
                for (int b=0; b<nComponents; b++)
                    file.printf("%f\n", data[b]);
            }

            for (int a=0; a<nPD_vectors; a++)
            {
                String name = vector_names.get(a);
                ArrayList<double[]> data = vector_data.get(a);

                file.printf("%s %d %d float\n", name, 3, nComponents);	// name, num comp, num points, type
                for (int b=0; b<nComponents; b++)
                    file.printf("%f %f %f\n", data.get(b)[0], data.get(b)[1], data.get(b)[2]);
            }

        }
    }

    /*
   public void writeVTK_centerline(String filename)
   {
      try
      {
	 PrintWriter file = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));

	 // write headers
	 file.println("# vtk DataFile Version 3.0");	// header
	 file.println("FluidSolution1D");		// title
	 file.println("ASCII");				// data type: ASCII or BINARY
	 file.println("DATASET POLYDATA");		// dataset type: STRUCTURED_POINTS, STRUCTURED_GRID, UNSTRUCTURED_GRID, POLYDATA, RECTILINEAR_GRID,  FIELD

	 int nPoints = geometry.getNumberOfPoints();
	 int nLines = nPoints - 1;

	 // write points --> //POINTS 19 float
	 file.println();
	 file.printf("POINTS %d float\n", nPoints);
	 for (int a=0; a<nPoints; a++)
	 {
	    Point3d p = geometry.centerline.getVertices().get(a);
	    file.printf("%f %f %f\n", p.x, p.y, p.z);
	 }

	 // write points --> //POINTS 19 float
	 file.println();
	 file.printf("LINES %d %d\n", nLines, nLines*3);
	 for (int a=0; a<nLines; a++)
	 {
	    file.printf("2 %d %d\n", a, a+1);
	 }

	 // write the point data
	 int nFields = fields.size() + 3; // fields + length, area and perimeter
	 file.println();
	 file.printf("POINT_DATA %d\n", nPoints);
	 file.printf("FIELD fieldData %d\n", nFields);

	 file.printf("%s %d %d float\n", "length", 1, nPoints);
	 for (int a=0; a<nPoints; a++)
	 {
	    file.printf("%f\n", geometry.getCenterline().getS()[a]);
	 }

	 file.printf("%s %d %d float\n", "area", 1, nPoints);
	 for (int a=0; a<nPoints; a++)
	 {
	    file.printf("%f\n", geometry.getArea()[a]);
	 }

	 file.printf("%s %d %d float\n", "perim", 1, nPoints);
	 for (int a=0; a<nPoints; a++)
	 {
	    file.printf("%f\n", geometry.getPerimeter()[a]);
	 }

	 for (Field f : fields)
	 {
	    file.printf("%s %d %d float\n", f.name, 1, nPoints);
	    for (int a=0; a<nPoints; a++)
	    {
	       file.printf("%f\n", f.values[a]);
	    }
	 }

	 file.close();
      }
      catch(Exception e)
      {
      }
   }
   //*/
    
    // Read VTK legacy file format for unstructured grid. Can specify a surface or volume mesh.
    
    /*//
    public static PolygonalMesh readUnstructuredMesh_surface(String filename)
    {
        return null;
    }
    //*/
    
    public static PolygonalMesh readUnstructuredMesh_surface(String filename)
    {
        try 
        {
            VtkData data = readVTKUnstructuredGrid(filename);
            return buildUnstructuredMesh_surface(data);
        }
        catch (IOException e) 
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static FemModel3d readUnstructuredMesh_volume(String filename)
    {
        try 
        {
            FemModel3d fem = new FemModel3d();
            VtkData data = readVTKUnstructuredGrid(filename);
            buildUnstructuredMesh_volume(fem, data);
            return fem;
        }
        catch (IOException e) 
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void readUnstructuredMesh_volume(FemModel3d fem, String filename)
    {
        try 
        {
            VtkData data = readVTKUnstructuredGrid(filename);
            buildUnstructuredMesh_volume(fem, data);
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public static void buildUnstructuredMesh_volume(FemModel3d fem, VtkData vtkData) throws IOException
    {
//        BufferedReader input = new BufferedReader(new FileReader(filename));
//        String inLine = null;
//        String[] inSplit = null;
//
//        int nPoints = 0;
//        int nCells = 0;
//        ArrayList<Point3d> points = null;
//        ArrayList<Cell> cells = null;
//
//        inLine = input.readLine();          // 1st line: version info, etc...
//        String title = input.readLine();    // 2nd line: title
//        fem.setName(title);
//        inLine = input.readLine();          // 3rd line: ASCII or Binary --> better be ASCII!
//        inLine = input.readLine();          // 4th line: DATASET <type> --> this only reads unstructured grid
//        inSplit = inLine.split(" ");
//        if (inSplit[1].equalsIgnoreCase("UNSTRUCTURED_GRID") == false)
//        {
//            System.out.println("Unrecognized VTK dataset: " + inSplit[1]);
//            return null;
//        }
//
//        while (input.ready() == true)
//        {
//            inLine = input.readLine();
//            inLine = inLine.trim();
//            if (inLine.isEmpty () == true)
//                continue;   // blank lines can be ignored...
//
//            inSplit = inLine.split(" ");
//
//            if       (inSplit[0].equalsIgnoreCase("POINTS") == true)
//            {
//                nPoints = Integer.parseInt (inSplit[1]);
//                points = new ArrayList<Point3d>(nPoints);
//
//                double[] nums = new double[nPoints*3];
//                int nNum = 0;
//                while (nNum < nPoints*3)
//                {
//                    inLine = input.readLine();
//                    inSplit = inLine.split(" ");
//                    for (int i=0; i<inSplit.length; i++)
//                    {
//                        nums[nNum] = Double.parseDouble( inSplit[i] );
//                        nNum++;
//                    }
//                }
//
//                for (int n=0; n<nPoints; n++)
//                {
//                    points.add( new Point3d(nums[n*3+0],nums[n*3+1],nums[n*3+2]) );
//                    //fem.addNode( new FemNode3d(nums[n*3+0],nums[n*3+1],nums[n*3+2]) );
//                }
//            }
//            else if (inSplit[0].equalsIgnoreCase("CELLS") == true)
//            {
//                nCells = Integer.parseInt (inSplit[1]);
//                cells = new ArrayList<Cell>(nCells);
//
//                for (int n=0; n<nCells; n++)
//                {
//                    Cell cell = new Cell();
//                    cells.add(cell);
//
//                    inLine = input.readLine();
//                    while (inLine.contains("  ") )
//                        inLine = inLine.replaceAll("  ", " "); // some writers use multiple spaces...
//                    inSplit = inLine.split(" ");
//                    cell.nPoints = Integer.parseInt(inSplit[0]);
//                    cell.indices = new int[cell.nPoints];
//                    for (int i=0; i<cell.nPoints; i++)
//                        cell.indices[i] = Integer.parseInt(inSplit[i+1]);
//                }
//            }
//            else if (inSplit[0].equalsIgnoreCase("CELL_TYPES") == true)
//            {
//                // I'm assuming "CELLS" comes before "CELL_TYPES"...
//                for (int n=0; n<nCells; n++)
//                {
//                    inLine = input.readLine();
//
//                    Cell cell = cells.get(n);
//                    cell.vtkType = Integer.parseInt(inLine);
//                }
//            }
//
//            // I do not see much point in implementing cell,point,field data reading (we are just getting the geometry)
//            else if (inSplit[0].equalsIgnoreCase("CELL_DATA") == true)
//            {
//            }
//            else if (inSplit[0].equalsIgnoreCase("POINT_DATA") == true)
//            {
//            }
//            else if (inSplit[0].equalsIgnoreCase("FIELD_DATA") == true)
//            {
//            }
//
//        }
//        input.close();
        
        ArrayList<Point3d> points = vtkData.points;
        ArrayList<Cell> cells = vtkData.cells;
        fem.setName(vtkData.name);

        // now, compile the FEM
        for (Point3d p : points)
            fem.addNode(new FemNode3d(p));
        //for (Cell c : cells)
        for (int a=0; a<cells.size(); a++)
        {
            Cell c = cells.get(a);
            //System.out.printf("index=%d,\t vtkType=%d, %d  %d  %d\n", a, c.vtkType, c.indices[0], c.indices[1], c.indices[2]);
            //if (a==3430)
                //System.out.println("bugger");

            if      (c.vtkType == 1)    // vertex
                ;
            else if (c.vtkType == 3)    // line
                ;
            else if (c.vtkType == 4)    // polyline
                ;
            else if (c.vtkType == 5)    // triangle
                ;
            else if (c.vtkType == 7)    // polygon
                ;
            else if (c.vtkType == 9)    // quad
                ;

            else if (c.vtkType == 10)    // tet (vtk and artisynth agree)
            {
                TetElement elem = new TetElement(
                    fem.getNode(c.indices[0]), 
                    fem.getNode(c.indices[1]),
                    fem.getNode(c.indices[2]),
                    fem.getNode(c.indices[3]) );
                fem.addElement(elem);

                elem.computeVolumes();
                if (elem.getVolume() < 0.0)
                    System.out.println("Warning: inverted tet element");

            }
            else if (c.vtkType == 12)    // hex  (vtk and artisynth disagree)
            {
                HexElement elem = new HexElement(
                    fem.getNode(c.indices[0]), 
                    fem.getNode(c.indices[3]), 
                    fem.getNode(c.indices[2]), 
                    fem.getNode(c.indices[1]),
                    fem.getNode(c.indices[4]),
                    fem.getNode(c.indices[7]),
                    fem.getNode(c.indices[6]),
                    fem.getNode(c.indices[5]) );
                fem.addElement(elem);

                elem.computeVolumes();
                if (elem.getVolume() < 0.0)
                    System.out.println("Warning: inverted hex element");

            }
            else if (c.vtkType == 13)    // wedge (vtk and artisynth disagree)
            {
                WedgeElement elem = new WedgeElement(
                    fem.getNode(c.indices[0]), 
                    fem.getNode(c.indices[2]),
                    fem.getNode(c.indices[1]),
                    fem.getNode(c.indices[3]),
                    fem.getNode(c.indices[5]),
                    fem.getNode(c.indices[4]) );
                fem.addElement(elem);

                elem.computeVolumes();
                if (elem.getVolume() < 0.0)
                    System.out.println("Warning: inverted wedge element");

            }
            else if (c.vtkType == 14)    // pyramid  (vtk and artisynth agree)
            {
                PyramidElement elem = new PyramidElement(
                    fem.getNode(c.indices[0]), 
                    fem.getNode(c.indices[1]),
                    fem.getNode(c.indices[2]),
                    fem.getNode(c.indices[3]),
                    fem.getNode(c.indices[4]) );
                fem.addElement(elem);

                elem.computeVolumes();
                if (elem.getVolume() < 0.0)
                    System.out.println("Warning: inverted pyramid element");

            }
        }
    }
    
    static PolygonalMesh buildUnstructuredMesh_surface(VtkData vtkData) throws IOException
    {        
        PolygonalMesh mesh = new PolygonalMesh();
        mesh.setName(vtkData.name);
        
        ArrayList<Point3d> points = vtkData.points;
        ArrayList<Cell> cells = vtkData.cells;

        // now, compile the FEM
        for (Point3d p : points)
            mesh.addVertex(p);
        //for (Cell c : cells)
        for (int a=0; a<cells.size(); a++)
        {
            Cell c = cells.get(a);

            if      (c.vtkType == 1)    // vertex
                ;
            else if (c.vtkType == 3)    // line
                ;
            else if (c.vtkType == 4)    // polyline
                ;
            else if (c.vtkType == 5)    // triangle
                mesh.addFace(c.indices);
            else if (c.vtkType == 7)    // polygon
                mesh.addFace(c.indices);
            else if (c.vtkType == 9)    // quad
                mesh.addFace(c.indices);

            else if (c.vtkType == 10)    // tet (vtk and artisynth agree)
                ;
            else if (c.vtkType == 12)    // hex  (vtk and artisynth disagree)
                ;
            else if (c.vtkType == 13)    // wedge (vtk and artisynth disagree)
                ;
            else if (c.vtkType == 14)    // pyramid  (vtk and artisynth agree)
                ;
        }
        return mesh;
    }
    
    static VtkData readVTKUnstructuredGrid(String filename) throws IOException
    {
        BufferedReader input = new BufferedReader(new FileReader(filename));
        String inLine = null;
        String[] inSplit = null;

        int nPoints = 0;
        int nCells = 0;
        ArrayList<Point3d> points = null;
        ArrayList<Cell> cells = null;

        inLine = input.readLine();          // 1st line: version info, etc...
        String title = input.readLine();    // 2nd line: title
        inLine = input.readLine();          // 3rd line: ASCII or Binary --> better be ASCII!
        inLine = input.readLine();          // 4th line: DATASET <type> --> this only reads unstructured grid
        inSplit = inLine.split(" ");
        if (inSplit[1].equalsIgnoreCase("UNSTRUCTURED_GRID") == false)
        {
            System.out.println("Unrecognized VTK dataset: " + inSplit[1]);
            return null;
        }

        while (input.ready() == true)
        {
            inLine = input.readLine();
            inLine = inLine.trim();
            if (inLine.isEmpty () == true)
                continue;   // blank lines can be ignored...

            inSplit = inLine.split(" ");

            if       (inSplit[0].equalsIgnoreCase("POINTS") == true)
            {
                nPoints = Integer.parseInt (inSplit[1]);
                points = new ArrayList<Point3d>(nPoints);

                double[] nums = new double[nPoints*3];
                int nNum = 0;
                while (nNum < nPoints*3)
                {
                    inLine = input.readLine();
                    inSplit = inLine.split(" ");
                    for (int i=0; i<inSplit.length; i++)
                    {
                        nums[nNum] = Double.parseDouble( inSplit[i] );
                        nNum++;
                    }
                }

                for (int n=0; n<nPoints; n++)
                {
                    points.add( new Point3d(nums[n*3+0],nums[n*3+1],nums[n*3+2]) );
                    //fem.addNode( new FemNode3d(nums[n*3+0],nums[n*3+1],nums[n*3+2]) );
                }
            }
            else if (inSplit[0].equalsIgnoreCase("CELLS") == true)
            {
                nCells = Integer.parseInt (inSplit[1]);
                cells = new ArrayList<Cell>(nCells);

                for (int n=0; n<nCells; n++)
                {
                    Cell cell = new Cell();
                    cells.add(cell);

                    inLine = input.readLine();
                    while (inLine.contains("  ") )
                        inLine = inLine.replaceAll("  ", " "); // some writers use multiple spaces...
                    inSplit = inLine.split(" ");
                    cell.nPoints = Integer.parseInt(inSplit[0]);
                    cell.indices = new int[cell.nPoints];
                    for (int i=0; i<cell.nPoints; i++)
                        cell.indices[i] = Integer.parseInt(inSplit[i+1]);
                }
            }
            else if (inSplit[0].equalsIgnoreCase("CELL_TYPES") == true)
            {
                // I'm assuming "CELLS" comes before "CELL_TYPES"...
                for (int n=0; n<nCells; n++)
                {
                    inLine = input.readLine();

                    Cell cell = cells.get(n);
                    cell.vtkType = Integer.parseInt(inLine);
                }
            }

            // I do not see much point in implementing cell,point,field data reading (we are just getting the geometry)
            else if (inSplit[0].equalsIgnoreCase("CELL_DATA") == true)
            {
            }
            else if (inSplit[0].equalsIgnoreCase("POINT_DATA") == true)
            {
            }
            else if (inSplit[0].equalsIgnoreCase("FIELD_DATA") == true)
            {
            }

        }
        input.close();
        
        VtkData vtkData = new VtkData();
        vtkData.points = points;
        vtkData.cells = cells;
        vtkData.name = title;
        return vtkData;
    }

}

class VtkData
{
    String name;
    ArrayList<Cell> cells;
    ArrayList<Point3d> points;
}

class Cell
{
    int nPoints;
    int[] indices;
    int vtkType;
}