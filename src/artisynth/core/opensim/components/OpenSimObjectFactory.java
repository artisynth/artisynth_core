package artisynth.core.opensim.components;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.util.Logger;
import maspack.util.ReaderTokenizer;

/**
 * Factory class that can parse DOM elements to create OpenSimComponents
 * 
 * @param <E> base class generated
 */
public abstract class OpenSimObjectFactory<E extends OpenSimObject> {

   OpenSimObjectFactoryStore factoryStore;

   Class<? extends E> instanceClass;
   E defaultInstance;

   @SuppressWarnings("unused")
   private OpenSimObjectFactory() {
      // disallow in subclasses
   }

   /**
    * Class for generating new components
    * @return
    */
   protected Class<? extends E> getComponentClass () {
      return instanceClass;
   }

   /**
    * Constructor, specifying class for instance creation
    * @param store store of factories that this factory belongs to
    * @param instanceClass class for instances from factory
    */
   protected OpenSimObjectFactory(Class<? extends E> instanceClass) {
      this.instanceClass = instanceClass;
      this.factoryStore = null;
   }

   /**
    * Assigns the factory storage from which to find other factories to parse
    * subcomponents
    * @param store factory store
    */
   protected void setFactoryStore(OpenSimObjectFactoryStore store) {
      this.factoryStore = store;
   }

   /**
    * Retrieves the factory storage to which this factory belongs, for finding
    * other factories to parse subcomponents
    */
   protected OpenSimObjectFactoryStore getFactoryStore() {
      return factoryStore;
   }
   
   /**
    * Retrieves the factory for parsing the given component type
    * @param objectClass items to parse
    * @return factory or null if not found
    */
   protected <F extends OpenSimObject> OpenSimObjectFactory<? extends F> getFactory(Class<F> objectClass) {
      OpenSimObjectFactory<? extends F> factory = null;
      if (factoryStore != null) {
         factory = factoryStore.getFactory (objectClass);
      }
      return factory;
   }
   
   /**
    * Finds a factory for parsing the given element that is assignable to the provided component class
    * @param objectClass assignable class
    * @param elem DOM element to parse
    * @return factory or null if none found
    */
   protected <F extends OpenSimObject> OpenSimObjectFactory<? extends F> findFactory(Class<F> objectClass, Element elem) {
      OpenSimObjectFactory<? extends F> factory = null;
      if (factoryStore != null) {
         factory = factoryStore.findFactory (objectClass, elem);
      }
      return factory;
   }

   /**
    * Sets default instance to use when creating new instances
    * @param def default instance
    */
   protected void setDefault(E def) {
      defaultInstance = def;
   }

   /**
    * Creates a default instance
    * @return new instance
    */
   public E createDefault() {
      if (defaultInstance == null) {
         // create new instance
         try {
            defaultInstance = instanceClass.newInstance ();
         } catch (Exception e) {
            Logger.getSystemLogger ().warn ("Failed to create new instance of class " 
            + instanceClass.getName ());
            Logger.getSystemLogger ().warn (e);

         }
      }
      @SuppressWarnings("unchecked")
      E out = (E)defaultInstance.clone();
      return out;
   }
   
   /**
    * Extracts the node name, first checking local name
    * @param node DOM node
    * @return name
    */
   protected static String getNodeName(Node node) {
      String lname = node.getLocalName ();
      if (lname == null) {
         lname = node.getNodeName ();
      }
      return lname;
   }

   /**
    * Returns true if the attribute can be parsed
    * @param comp component to set attribute on
    * @param attr DOM attribute
    * @return true if successful, false otherwise
    */
   protected boolean parseAttribute(E comp, Attr attr) {
      // parse name attribute
      if ("name".equals(getNodeName(attr))) {
         String name = attr.getValue ();
         if (name != null) {
            name = name.trim ();
         }
         comp.setName (name);
         return true;
      }
      return false;
   }

   /**
    * Returns true if the child element can be parsed
    * @param comp component to set child value
    * @param child DOM child element
    * @return true if successful, false otherwise
    */
   protected boolean parseChild(E comp, Element child) {
      return false;
   }

   /**
    * Attempts to parse all attributes of the provided element
    * @param comp component to populate
    * @param elem element to parse
    * @return true of all attributes successful, false if one
    * or more failed
    */
   protected boolean parseAttributes(E comp, Element elem) {
      if (!elem.hasAttributes ()) {
         return true;
      }

      boolean success = true;
      NamedNodeMap attributes = elem.getAttributes ();
      for (int i=0; i<attributes.getLength (); ++i) {
         Attr attribute = (Attr)(attributes.item (i));
         boolean asuccess = parseAttribute (comp, attribute);
         if (!asuccess) {
            Logger.getSystemLogger ().warn ("Failed to parse attribute '" + getNodeName(attribute) 
            + "' for " + comp.getClass ().getName ());
            success = false;
         }
      }
      return success;
   }

   /**
    * Attempts to parse all child elements of the provided element
    * @param comp component to populate
    * @param elem element to parse
    * @return true if all children successfully parsed
    */
   protected boolean parseChildren(E comp, Element elem) {
      if (!elem.hasChildNodes ()) {
         return true;
      }

      boolean success = true;
      Node child = elem.getFirstChild ();
      while (child != null) {

         if (child.getNodeType () == Node.ELEMENT_NODE) {
            boolean csuccess = parseChild (comp, (Element)child);
            if (!csuccess) {
               Logger.getSystemLogger ().warn ("Failed to parse subelement '" + getNodeName(child) 
               + "' for " + comp.getClass ().getName ());
            }
         }

         child = child.getNextSibling ();
      }

      return success;
   }

   /**
    * Checks if this factory can parse the given DOM element.
    * 
    * By default, assumes we can parse the element if the element's local name matches the
    * component class name {@link #getComponentClass()}
    * 
    * @param elem DOM element
    * @return true if a valid E can be parsed by this factory, false otherwise
    */
   public boolean canParse(Element elem) {

      // check if element is of type Bone
      String name = getNodeName (elem);
      String cname = instanceClass.getSimpleName ();
      if (cname != null && cname.equals(name)) {
         return true;
      }
      
      return false;
   }

   /**
    * Tries to parse the provided element
    * @param elem DOM element
    * @return valid E if can be parsed by this factory, null otherwise
    */
   public E parse(Element elem) {
      E out = createDefault ();
      // parse attributes and children
      parseAttributes (out, elem);
      parseChildren (out, elem);
      return out;
   }

   /**
    * Extracts the first text value of an element
    * @param ele DOM element
    * @return text
    */
   protected static String parseTextValue(Element ele) {
      Node child = ele.getFirstChild ();
      while (child != null) {
         if (child.getNodeType () == Node.TEXT_NODE) {
            return child.getNodeValue ().trim ();
         }
         child = child.getNextSibling ();
      }
      return null;
   }

   /**
    * Parses a double value from an element
    * @param elem DOM element
    * @return double value (Default 0)
    */
   protected static double parseDoubleValue(Element elem) {
      String text = parseTextValue (elem);
      double value = 0;
      if (text != null) {
         value = Double.parseDouble(text);
      }
      return value;
   }

   /**
    * Parses an integer value from an element
    * @param elem DOM element
    * @return integer value (Default 0)
    */
   protected static int parseIntegerValue(Element elem) {
      String text = parseTextValue (elem);
      int value = 0;
      if (text != null) {
         value = Integer.parseInt(text);
      }
      return value;
   }

   /**
    * Parses a double array from a string, values separated by whitespace
    * @param str input string
    * @return parsed double array
    */
   protected static double[] parseDoubleArray(String str) {
      // split by spaces
      String[] strNum = str.trim().split("\\s+");
      double[] num = new double[strNum.length];

      for (int i = 0; i < strNum.length; i++) {
         num[i] = Double.parseDouble(strNum[i]);
      }
      return num;
   }

   /**
    * Parses a double array from an element
    * @param elem DOM element
    * @return parsed double array
    */
   protected static double[] parseDoubleArrayValue(Element elem) {
      String text = parseTextValue (elem);
      double[] values = null;
      if (text != null) {
         values = parseDoubleArray(text);
      }
      return values;
   }

   /**
    * Parses an integer array from a string, values separated by whitespace
    * @param str input string
    * @return parsed integer array
    */
   protected static int[] parseIntegerArray(String str) {
      // split by spaces
      String[] strNum = str.trim().split("\\s+");
      int[] num = new int[strNum.length];

      for (int i = 0; i < strNum.length; i++) {
         num[i] = Integer.parseInt(strNum[i]);
      }
      return num;
   }

   /**
    * Parses an integer array from an element
    * @param elem DOM element
    * @return parsed integer array
    */
   protected static int[] parseIntegerArrayValue(Element elem) {
      String text = parseTextValue (elem);
      int[] values = null;
      if (text != null) {
         values = parseIntegerArray(text);
      }
      return values;
   }

   /**
    * Parses a text array from a string, values separated by whitespace except if quoted
    * @param str input string
    * @return parsed text array
    */
   protected static String[] parseTextArray(String str) {
      // split by spaces
      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (str));
      
      ArrayList<String> words = new ArrayList<> ();
      try {
         while (rtok.nextToken () == ReaderTokenizer.TT_WORD) {
            words.add (rtok.sval);
         }
      }
      catch (IOException e) { }
      rtok.close ();
      
      return words.toArray (new String[words.size ()]);
   }

   /**
    * Parses a text array from an element, values separated by whitespace
    * @param elem input element
    * @return parsed text array
    */
   protected static String[] parseTextArrayValue(Element elem) {
      String text = parseTextValue (elem);
      String[] strs = null;
      if (text != null) {
         strs = parseTextArray(text); 
      }
      return strs;
   }

   /**
    * Creates a 3D point from a string
    * @param str input string
    * @return Point3d
    */
   protected static Point3d parsePoint3d (String str) {
      Point3d pnt = new Point3d();
      double values[] = parseDoubleArray(str);
      pnt.set (values);

      return pnt;
   }

   /**
    * Creates a 3D vector from a string
    * @param str input string
    * @return Vector3d
    */
   protected static Vector3d parseVector3d (String str) {
      Vector3d v = new Vector3d();
      double values[] = parseDoubleArray(str);
      v.set (values);

      return v;
   }

   /**
    * Creates a 3D point from an element
    * @param elem DOM element
    * @return parsed point
    */
   protected static Point3d parsePoint3dValue(Element elem) {
      String text = parseTextValue (elem);
      if (text != null) {
         return parsePoint3d(text);
      }
      return null;
   }

   /**
    * Creates a 3D vector from an element
    * @param elem DOM element
    * @return parsed vector
    */
   protected static Vector3d parseVector3dValue(Element elem) {
      String text = parseTextValue (elem);
      if (text != null) {
         return parseVector3d(text);
      }
      return null;
   }


   /**
    * Creates a boolean from an element
    * @param elem DOM element
    * @return parsed boolean
    */
   protected static boolean parseBooleanValue(Element elem) {
      String text = parseTextValue (elem);
      boolean value = false;
      if (text != null) {
         if ("true".equalsIgnoreCase (text)) {
            value = true;
         }
      }
      return value;
   }
   
   /**
    * Parses the first function-type child element of the provided element
    * @param elem container of function
    * @return parsed function or null if not successful
    */
   protected FunctionBase parseFunctionValue(Element elem) {
      
      FunctionBase func = null;
      
      // try to parse function, could be empty
      Node grandChild = elem.getFirstChild ();
      while (grandChild != null && func == null) {
         if (grandChild.getNodeType () == Node.ELEMENT_NODE) {
            OpenSimObjectFactory<? extends FunctionBase> factory = findFactory (FunctionBase.class, (Element)grandChild);
            if (factory != null) {
               func = factory.parse ((Element)grandChild);
            }
         }
         grandChild = grandChild.getNextSibling ();
      }
      
      return func;
   }
   

   
   /**
    * @see OpenSimObjectFactory#setOrientationXYZ(RotationMatrix3d, double, double, double)
    */
   public static void setOrientationXYZ(RotationMatrix3d R, Point3d rot) {
      setOrientationXYZ (R, rot.x, rot.y, rot.z);
   }

   /**
    *  Set this rotations to represent a body-fixed rotation of X-Y-Z. 
    *  first rotation is x about the body frame's X axis, followed by a rotation of y
    *  about the body frame's NEW Y axis, followed by a rotation of z about the body 
    *  frame's NEW Z axis.  See Kane, Spacecraft Dynamics, pg. 423, body-three: 1-2-3.
    * 
    * @param R rotation matrix to populate
    * @param x
    * first angle (radians) rotation about body's x-axis
    * @param y
    * second angle (radians) rotation about body's new y-axis
    * @param z
    * third angle (radians) rotation about body's new y-axis
    */
   public static void setOrientationXYZ(RotationMatrix3d R, double x, double y, double z) {
      double sx = Math.sin (x);
      double cx = Math.cos (x);
      double sy = Math.sin (y);
      double cy = Math.cos (y);
      double sz = Math.sin (z);
      double cz = Math.cos (z);

      double sxsy = sx * sy;
      double szcx = sz * cx;
      double cxcz = cx * cz;

      R.m00 = cy * cz;
      R.m01 = sz * -cy;
      R.m02 = sy;
      R.m10 = szcx + sxsy * cz;
      R.m11 = cxcz - sxsy * sz;
      R.m12 = sx * -cy;
      R.m20 = sx * sz - sy * cxcz;
      R.m21 = sx * cz + sy * szcx;
      R.m22 = cx * cy;
   }

   /**
    * Creates a rigid transform from a string
    * @param str string
    * @return transform
    */
   protected static RigidTransform3d parseTransform(String str) {
      double[] dvals = parseDoubleArray(str);

      RigidTransform3d trans = new RigidTransform3d();
      setOrientationXYZ (trans.R, dvals[0], dvals[1], dvals[2]);
      trans.p.set(dvals[3], dvals[4], dvals[5]);

      return trans;
   }

   /**
    * Creates a rigid transform from an element
    * @param Element DOM element
    * @return transform
    */
   protected static RigidTransform3d parseTransformValue(Element elem) {
      String text = parseTextValue (elem);
      RigidTransform3d trans = null;
      if (text != null) {
         trans = parseTransform(text);
      }
      return trans;
   }

   /**
    * Creates an orientation from a string
    * @param str string
    * @return orientation
    */
   protected static AxisAngle parseOrientation(String str) {
      double[] dvals = parseDoubleArray(str);

      RotationMatrix3d R = new RotationMatrix3d();
      setOrientationXYZ (R, dvals[0], dvals[1], dvals[2]);

      return new AxisAngle(R);
   }

   /**
    * Creates an orientation from an element
    * @param Element DOM element
    * @return orientation
    */
   protected static AxisAngle parseOrientationValue(Element elem) {
      String text = parseTextValue (elem);
      AxisAngle trans = null;
      if (text != null) {
         trans = parseOrientation(text);
      }
      return trans;
   }
   





}
