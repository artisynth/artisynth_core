package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolylineMesh;
import maspack.util.ReaderTokenizer;

/**
 * Reads a polygonal mesh from the VTK vtp format
 * 
 * see http://www.cacr.caltech.edu/~slombey/asci/vtk/vtk_formats.simple.html
 */

public class VtkXmlReader extends MeshReaderBase {
   
   VtkSaxHandler sax;

   public VtkXmlReader (InputStream is) throws IOException {
      super (is);
      sax = null;
   }

   public VtkXmlReader (File file) throws IOException {
      super (file);
      sax = null;
   }

   public VtkXmlReader (String fileName) throws IOException {
      this (new File(fileName));
   }

   /**
    * SAX XML parser for constructing mesh from vtk file
    * 
    * @author Antonio
    *
    */
   private static class VtkSaxHandler extends DefaultHandler {

      private PolygonalMesh mesh = null;
      private PolylineMesh lmesh = null;
      private PointMesh pmesh = null;

      private StringBuilder xmlContent = new StringBuilder();
      private DATA_TYPE currentData;
      private int nPolys = 0;
      private int nPoints = 0;
      private int nVerts = 0;
      private int nLines = 0;
      private int nStrips = 0;
      private ArrayList<Integer> connectivity;
      private ArrayList<Integer> offsets;
      private double[][] coords;

      enum DATA_TYPE {
         NORMALS, POINTS, CONNECTIVITY, OFFSETS
      }

      enum TAG_TYPE {
         VTK_FILE, POLY_DATA, PIECE, POINT_DATA, POINTS, POLYS, DATA_ARRAY, CELL_DATA,
         VERTS, LINES, STRIPS
      }

      static class VTKElement {
         TAG_TYPE type;
         String name;
         Attributes attrs;
         public VTKElement(TAG_TYPE type, String name, Attributes attrs) {
            this.type = type;
            this.name = name;
            this.attrs = attrs;
         }
      }

      private ArrayDeque<VTKElement> elementStack = new ArrayDeque<VTKElement>();

      public VtkSaxHandler() {
      }
      
      public VtkSaxHandler (PolygonalMesh mesh) {
         setMesh(mesh);
      }
      
      public VtkSaxHandler (PolylineMesh mesh) {
         setMesh(mesh);
      }

      public void setMesh(PolygonalMesh mesh) {
         if (mesh == null) {
            mesh = new PolygonalMesh();
         }
         this.mesh = mesh;
      }

      public void setMesh(PolylineMesh mesh) {
         if (mesh == null) {
            mesh = new PolylineMesh();
         }
         this.lmesh = mesh;
      }
      
      public PolygonalMesh getMesh() {
         return mesh;
      }

      public PolylineMesh getPolylineMesh() {
         return lmesh;
      }

      public int numLines() {
         return nLines;
      }

      public int numFaces() {
         return nPolys;
      }

      // SAX parser stuff
      public void startDocument() throws SAXException {
         if (mesh != null) {
            mesh.clear ();
         }
         if (lmesh != null) {
            lmesh.clear ();
         }
      }

      private static int getIntegerAttribute(Attributes atts, String attribute, int defaultValue) {
         String attr = atts.getValue(attribute);
         if (attr == null) {
            return defaultValue;
         }
         return Integer.parseInt (attr);
      }

      public void startElement(String namespaceURI,
         String localName,
         String qName,
         Attributes atts) throws SAXException {

         if (localName.equals("VTKFile")) {
            elementStack.addLast(new VTKElement (TAG_TYPE.VTK_FILE, localName, atts));
         } else if (localName.equals("PolyData")) {
            elementStack.addLast(new VTKElement (TAG_TYPE.POLY_DATA, localName, atts));
         } else if (localName.equals("Piece")) {
            elementStack.addLast(new VTKElement (TAG_TYPE.PIECE, localName, atts));

            nPoints = getIntegerAttribute(atts, "NumberOfPoints", 0);
            nPolys = getIntegerAttribute(atts, "NumberOfPolys", 0);
            nVerts = getIntegerAttribute(atts, "NumberOfVerts", 0);
            nLines = getIntegerAttribute(atts, "NumberOfLines", 0);
            nStrips = getIntegerAttribute(atts, "NumberOfStrips", 0);

         } else if (localName.equals("PointData")) {
            elementStack.addLast(new VTKElement (TAG_TYPE.POINT_DATA, localName, atts));
         } else if (localName.equals("Points")) {
            coords = new double[nPoints][3];
            elementStack.addLast(new VTKElement (TAG_TYPE.POINTS, localName, atts));
         } else if (localName.equals ("Lines")) {
            offsets = new ArrayList<Integer>(nLines);
            connectivity = new ArrayList<Integer>();
            elementStack.addLast (new VTKElement (TAG_TYPE.LINES, localName, atts));
         } else if (localName.equals("Verts")) {
            elementStack.addLast (new VTKElement (TAG_TYPE.VERTS, localName, atts));
         } else if (localName.equals("Polys")) {
            offsets = new ArrayList<Integer>(nPolys);
            connectivity = new ArrayList<Integer>();
            elementStack.addLast(new VTKElement (TAG_TYPE.POLYS, localName, atts));
         } else if (localName.equals("DataArray")) {

            VTKElement parent = elementStack.peekLast ();

            if (parent.type == TAG_TYPE.POINTS) {
               currentData = DATA_TYPE.POINTS;
            } else if (parent.type == TAG_TYPE.POLYS) {
               String type = atts.getValue("Name");
               if (type.equals("connectivity")) {
                  currentData = DATA_TYPE.CONNECTIVITY;
               } else if (type.equals("offsets")) {
                  currentData = DATA_TYPE.OFFSETS;
               }
            } else if (parent.type == TAG_TYPE.LINES) {
               String type = atts.getValue("Name");
               if (type.equals("connectivity")) {
                  currentData = DATA_TYPE.CONNECTIVITY;
               } else if (type.equals("offsets")) {
                  currentData = DATA_TYPE.OFFSETS;
               }
            }

            xmlContent = new StringBuilder();
            elementStack.addLast(new VTKElement (TAG_TYPE.DATA_ARRAY, localName, atts));
         } else {
            elementStack.addLast (new VTKElement(null, localName, atts));
         }

      }

      public void endElement(String uri, String localName, String qName)
      throws SAXException {

         TAG_TYPE type = elementStack.peekLast ().type;

         if (type == TAG_TYPE.POINTS) {

         } else if (type == TAG_TYPE.POLY_DATA) {
            // assign vertices
            if (mesh == null) {
               mesh = new PolygonalMesh();
            }
            for (int i = 0; i < nPoints; i++) {
               mesh.addVertex(coords[i][0], coords[i][1], coords[i][2]);
            }

            // assign faces
            int idx = 0;
            for (int i = 0; i < nPolys; i++) {
               int nV = offsets.get (i) - idx;
               int[] face = new int[nV];

               for (int j = 0; j < nV; j++) {
                  face[j] = connectivity.get(j + idx);
               }
               mesh.addFace(face);
               idx = offsets.get (i);
            }
         } else if (type == TAG_TYPE.LINES) {
            // assign vertices
            if (lmesh == null) {
               lmesh = new PolylineMesh();
            }
            for (int i = 0; i < nPoints; i++) {
               lmesh.addVertex(coords[i][0], coords[i][1], coords[i][2]);
            }

            // assign faces
            int idx = 0;
            for (int i = 0; i < nLines; i++) {
               int nV = offsets.get (i) - idx;
               int[] line = new int[nV];

               for (int j = 0; j < nV; j++) {
                  line[j] = connectivity.get(j + idx);
               }
               lmesh.addLine (line);
               idx = offsets.get (i);
            }
         } else if (type == TAG_TYPE.DATA_ARRAY) {

            ReaderTokenizer rtok =
            new ReaderTokenizer(new StringReader(xmlContent.toString()));

            if (currentData == DATA_TYPE.POINTS) {
               // parse points from xmlContent
               for (int i = 0; i < nPoints; i++) {
                  int nRead = 0;
                  try {
                     nRead = rtok.scanNumbers(coords[i], 3);
                  } catch (IOException e) {
                     e.printStackTrace();
                  }
                  if (nRead != 3) {
                     throw new SAXException("Coordinate " + i + " is invalid.");
                  }
               }

            } else if (currentData == DATA_TYPE.CONNECTIVITY) {

               try {
                  scanIntegers(rtok, connectivity, Integer.MAX_VALUE);
               } catch (IOException e) {
                  e.printStackTrace();
               }

               // parse face node indices
            } else if (currentData == DATA_TYPE.OFFSETS) {
               // parse offsets
               int nRead = 0;
               try {
                  nRead = scanIntegers(rtok, offsets, Integer.MAX_VALUE);
               } catch (IOException e) {
                  e.printStackTrace();
               }
               if (nRead != offsets.size ()) {
                  throw new SAXException("Invalid number of offsets");
               }

            }

            currentData = null;

         }

         elementStack.removeLast();
      }

      public void endDocument() throws SAXException {
      }

      public void characters(char ch[], int start, int length)
      throws SAXException {

         if (elementStack.peekLast().type == TAG_TYPE.DATA_ARRAY) {
            xmlContent.append(new String(ch, start, length));
         }

      }

      private static int scanIntegers(ReaderTokenizer rt,
         ArrayList<Integer> vals, int max) throws IOException {
         for (int i = 0; i < max; i++) {
            rt.nextToken();
            if (rt.ttype == ReaderTokenizer.TT_NUMBER) {
               vals.add((int)rt.nval);
            }
            else {
               return i;
            }
         }
         return max;
      }

      private static int scanIntegers(ReaderTokenizer rt, int[] vals, int max)
      throws IOException {
         for (int i = 0; i < max; i++) {
            rt.nextToken();
            if (rt.ttype == ReaderTokenizer.TT_NUMBER) {
               vals[i] = (int)rt.nval;
            }
            else {
               return i;
            }
         }
         return max;
      }

   }

   @Override
   public MeshBase readMesh() throws IOException {
      parse();
      
      // try polygonal mesh
      PolygonalMesh pmesh = getPolygonalMesh ();
      if (pmesh != null && pmesh.numFaces () > 0) {
         return pmesh;
      }
      
      // try polyline mesh
      PolylineMesh lmesh = getPolylineMesh ();
      if (lmesh != null && lmesh.numLines () > 0) {
         return lmesh;
      }
      
      return null;
   }

   public MeshBase readMesh (MeshBase mesh) throws IOException {
      if (mesh == null) {
         // generic mesh
         return readMesh();
      }
      // detect from type
      if (mesh instanceof PolygonalMesh) {
         return readMesh((PolygonalMesh)mesh);
      } else if (mesh instanceof PolylineMesh) {
         return readMesh((PolylineMesh)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.vtp' files");
      }
   }

   public void parse() throws IOException {
      // create a SAX handler
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setNamespaceAware(true);
      try {
         SAXParser saxParser = spf.newSAXParser();
         XMLReader xmlReader = saxParser.getXMLReader();

         this.sax = new VtkSaxHandler();
         xmlReader.setContentHandler(sax);

         InputSource isource = new InputSource(myIstream);
         xmlReader.parse(isource);

      } catch (SAXException e) {
         throw new IOException(e.getMessage(), e);
      } catch (ParserConfigurationException e) {
         throw new IOException(e.getMessage(), e);
      }
   }
   
   public PolygonalMesh getPolygonalMesh() {
      if (sax != null) {
         return sax.getMesh ();
      }
      return null;
   }
   
   public PolylineMesh getPolylineMesh() {
      if (sax != null) {
         return sax.getPolylineMesh ();
      }
      return null;
   }
   
   
   public PolygonalMesh readMesh (PolygonalMesh mesh)
   throws IOException {

      // create a SAX handler
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setNamespaceAware(true);
      try {
         SAXParser saxParser = spf.newSAXParser();
         XMLReader xmlReader = saxParser.getXMLReader();

         this.sax = new VtkSaxHandler(mesh);
         xmlReader.setContentHandler(sax);

         InputSource isource = new InputSource(myIstream);
         xmlReader.parse(isource);

         return sax.getMesh();

      } catch (SAXException e) {
         throw new IOException(e.getMessage(), e);
      } catch (ParserConfigurationException e) {
         throw new IOException(e.getMessage(), e);
      }

   }

   public PolylineMesh readMesh (PolylineMesh mesh) throws IOException {

      // create a SAX handler
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setNamespaceAware(true);
      try {
         SAXParser saxParser = spf.newSAXParser();
         XMLReader xmlReader = saxParser.getXMLReader();

         this.sax = new VtkSaxHandler(mesh);
         xmlReader.setContentHandler(sax);

         InputSource isource = new InputSource(myIstream);
         xmlReader.parse(isource);

         return sax.getPolylineMesh();

      } catch (SAXException e) {
         throw new IOException(e.getMessage(), e);
      } catch (ParserConfigurationException e) {
         throw new IOException(e.getMessage(), e);
      }

   }

   public static MeshBase read (File file) throws IOException {
      VtkXmlReader reader = new VtkXmlReader (file);
      return reader.readMesh ();
   }

   public static MeshBase read (String fileName) throws IOException {
      return read (new File(fileName));
   }


}
