package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.util.ReaderTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads a polygonal mesh from the VTK vtp format
 * 
 * @author antonio
 * 
 */

public class VtkXmlReader extends MeshReaderBase {

   public VtkXmlReader (InputStream is) throws IOException {
      super (is);
   }

   public VtkXmlReader (File file) throws IOException {
      super (file);
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

      private StringBuilder xmlContent = new StringBuilder();
      private DATA_TYPE currentData;
      private int nFaces = 0;
      private int nVertices = 0;
      private ArrayList<Integer> faceNodes;
      private int[] offsets;
      private double[][] coords;

      enum DATA_TYPE {
         NORMALS, POINTS, FACES, OFFSETS
      }

      enum TAG_TYPE {
         VTK_FILE, POLY_DATA, PIECE, POINT_DATA, POINTS, POLYS, DATA_ARRAY
      }

      private Stack<TAG_TYPE> tagStack = new Stack<TAG_TYPE>();

      public VtkSaxHandler (PolygonalMesh mesh) {
         setMesh(mesh);
      }

      public void setMesh(PolygonalMesh mesh) {
         if (mesh == null) {
            mesh = new PolygonalMesh();
         }
         this.mesh = mesh;
      }
      
      public PolygonalMesh getMesh() {
         return mesh;
      }

      // SAX parser stuff
      public void startDocument() throws SAXException {
         mesh.clear();
      }

      public void startElement(String namespaceURI,
         String localName,
         String qName,
         Attributes atts) throws SAXException {

         if (localName.equals("VTKFile")) {
            tagStack.push(TAG_TYPE.VTK_FILE);
         } else if (localName.equals("PolyData")) {
            tagStack.push(TAG_TYPE.POLY_DATA);
         } else if (localName.equals("Piece")) {
            tagStack.push(TAG_TYPE.PIECE);

            nVertices = Integer.parseInt(atts.getValue("NumberOfPoints"));
            nFaces = Integer.parseInt(atts.getValue("NumberOfPolys"));
            faceNodes = new ArrayList<Integer>();
            coords = new double[nVertices][3];
            offsets = new int[nFaces];

         } else if (localName.equals("PointData")) {
            tagStack.push(TAG_TYPE.POINT_DATA);

         } else if (localName.equals("Points")) {
            tagStack.push(TAG_TYPE.POINTS);

         } else if (localName.equals("Polys")) {
            tagStack.push(TAG_TYPE.POLYS);

         } else if (localName.equals("DataArray")) {

            if (tagStack.peek() == TAG_TYPE.POINTS) {
               currentData = DATA_TYPE.POINTS;
            } else if (tagStack.peek() == TAG_TYPE.POLYS) {
               String type = atts.getValue("Name");
               if (type.equals("connectivity")) {
                  currentData = DATA_TYPE.FACES;
               } else if (type.equals("offsets")) {
                  currentData = DATA_TYPE.OFFSETS;
               }
            }

            xmlContent = new StringBuilder();
            tagStack.push(TAG_TYPE.DATA_ARRAY);
         }

      }

      public void endElement(String uri, String localName, String qName)
         throws SAXException {

         if (tagStack.peek() == TAG_TYPE.POINTS) {
            // assign vertices

            for (int i = 0; i < nVertices; i++) {
               mesh.addVertex(coords[i][0], coords[i][1], coords[i][2]);
            }

         } else if (tagStack.peek() == TAG_TYPE.POLY_DATA) {
            // assign faces

            int idx = 0;
            for (int i = 0; i < nFaces; i++) {
               int nV = offsets[i] - idx;
               int[] face = new int[nV];

               for (int j = 0; j < nV; j++) {
                  face[j] = faceNodes.get(j + idx);
               }
               mesh.addFace(face);
               idx = offsets[i];
            }

         } else if (tagStack.peek() == TAG_TYPE.DATA_ARRAY) {

            ReaderTokenizer rtok =
               new ReaderTokenizer(new StringReader(xmlContent.toString()));

            if (currentData == DATA_TYPE.POINTS) {
               // parse points from xmlContent
               for (int i = 0; i < nVertices; i++) {
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

            } else if (currentData == DATA_TYPE.FACES) {

               try {
                  scanIntegers(rtok, faceNodes, Integer.MAX_VALUE);
               } catch (IOException e) {
                  e.printStackTrace();
               }

               // parse face node indices
            } else if (currentData == DATA_TYPE.OFFSETS) {
               // parse offsets
               int nRead = 0;
               try {
                  nRead = scanIntegers(rtok, offsets, nFaces);
               } catch (IOException e) {
                  e.printStackTrace();
               }
               if (nRead != nFaces) {
                  throw new SAXException("Invalid number of offsets");
               }

            }

         }
         tagStack.pop();
      }

      public void endDocument() throws SAXException {
      }

      public void characters(char ch[], int start, int length)
         throws SAXException {

         if (tagStack.peek() == TAG_TYPE.DATA_ARRAY) {
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

   public MeshBase readMesh (MeshBase mesh) throws IOException {
      if (mesh instanceof PolygonalMesh) {
         return readMesh ((PolygonalMesh)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported by this reader");
      }
   }

   public PolygonalMesh readMesh (PolygonalMesh mesh)
      throws IOException {

      // create a SAX handler
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setNamespaceAware(true);
      try {
         SAXParser saxParser = spf.newSAXParser();
         XMLReader xmlReader = saxParser.getXMLReader();

         VtkSaxHandler sax = new VtkSaxHandler(mesh);
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

   public static PolygonalMesh read (File file) throws IOException {
      VtkXmlReader reader = new VtkXmlReader (file);
      return (PolygonalMesh)reader.readMesh (new PolygonalMesh());
    }
   
   public static PolygonalMesh read (String fileName) throws IOException {
      return read (new File(fileName));
    }
   

}
