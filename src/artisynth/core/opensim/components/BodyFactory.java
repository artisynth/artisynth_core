package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class BodyFactory extends PhysicalFrameFactory<Body> {
   
   public BodyFactory() {
      super(Body.class);
   }
   
   protected BodyFactory(Class<? extends Body> instanceClass) {
      super(instanceClass);
   }
   
   @Override
   protected boolean parseChild (Body body, Element child) {
      boolean success = true;

      String cname = getNodeName(child);
      
      if ("mass".equals(cname)) {
         body.setMass (parseDoubleValue (child));
      } else if ("mass_center".equals(cname)) {
         body.setMassCenter (parsePoint3dValue(child));
      } else if ("inertia_xx".equals(cname)) {
         body.setInertiaXX (parseDoubleValue(child)); 
      } else if ("inertia_yy".equals(cname)) {
         body.setInertiaYY (parseDoubleValue(child));
      } else if ("inertia_zz".equals(cname)) {
         body.setInertiaZZ (parseDoubleValue(child));
      } else if ("inertia_xy".equals(cname)) {
         body.setInertiaXY (parseDoubleValue(child));
      } else if ("inertia_xz".equals(cname)) {
         body.setInertiaXZ (parseDoubleValue(child));
      } else if ("inertia_yz".equals(cname)) {
         body.setInertiaYZ (parseDoubleValue(child));
      } else if ("inertia".equals(cname)) {
         body.setInertia (parseDoubleArrayValue (child));
      } else if ("Joint".equals(cname)) {
         OpenSimObjectFactory<? extends Joint> jf = getFactory (Joint.class);
         if (jf != null) {
            Joint joint = jf.parse(child);
            body.setJoint(joint);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (body, child);
      }

      //
      //      //Bone Meshes
      //      if (bodyEl.getAttribute ("name").compareTo ("ground") != 0) {
      //         NodeList nlVisible = bodyEl.getElementsByTagName ("VisibleObject");
      //         Element elVisible = (Element)nlVisible.item(0);
      //         if(elVisible != null) {
      //            NodeList nlGeometry = elVisible.getElementsByTagName ("GeometrySet");
      //            if (nlGeometry.getLength ()>0) { // try <GeometrySet>
      //               Element elGeometry = (Element)nlGeometry.item (0);
      //               NodeList nlObj = elGeometry.getElementsByTagName ("objects");
      //               Element elObj = (Element)nlObj.item (0);
      //               NodeList nlDisp = elObj.getElementsByTagName ("DisplayGeometry");
      //
      //
      //               for(int i=0; i<nlDisp.getLength (); i++) {
      //                  Element elDisp = (Element)nlDisp.item (i);
      //                  attr = getTextValue(elDisp, "geometry_file");
      //                  attr = attr.trim ();
      //                  if (attr != null && !attr.isEmpty()) {
      //                     DisplayGeometry mesh = new DisplayGeometry (elDisp);
      //                     body.addBoneMesh (mesh);
      //                  } 
      //
      //               }
      //            } else { // try <geometry_files>
      //               attr = getTextValue(elVisible, "geometry_files");
      //               for (String str : attr.split ("\\s+")) {
      //                  if (!str.isEmpty ()) {
      //                     DisplayGeometry mesh = new DisplayGeometry(str);
      //                     body.addBoneMesh (mesh);
      //                  }
      //               }
      //            }
      //         }
      //      }
      //
      //      //Joint Names
      //      if (bodyEl.getAttribute ("name").compareTo ("ground") != 0) {
      //         NodeList nlJoint = bodyEl.getElementsByTagName ("Joint");
      //         Element elJoint = (Element)nlJoint.item (0);
      //
      //         // determine joint type
      //         NodeList children = elJoint.getChildNodes ();
      //         for (int j=0; j<children.getLength (); ++j) {
      //            Node child = children.item (j);
      //
      //            String name = child.getNodeName ();
      //            if ("CustomJoint".equals (name) || "WeldJoint".equals(name) || "PinJoint".equals(name)) {
      //               String jname = child.getAttributes ().getNamedItem ("name").getNodeValue ();
      //               body.addJointName (jname);
      //            }
      //         }
      //      }
      //
      //      // Wrap objects
      //      NodeList nl = bodyEl.getElementsByTagName("WrapObjectSet");
      //      Element el = (Element) nl.item(0);
      //      if(el != null) {
      //         nl = el.getElementsByTagName("objects");
      //         el = (Element) nl.item(0);
      //         if(el != null) {
      //            // Ellipsoids first.
      //            nl = el.getElementsByTagName("WrapEllipsoid");
      //            for(int i = 0; i < nl.getLength(); i++) {
      //               Element cel = (Element) nl.item(i);
      //               WrapObject w = new WrapObject();
      //               w.setName(cel.getAttribute("name"));
      //               w.setType(WrapObject.WrapObjectType.WRAP_ELLIPSOID);
      //
      //               attr = getTextValue(cel, "xyz_body_rotation");
      //               if(attr != null) {
      //                  w.setOrientation(new Point3d(parseDoubleArray(attr)));
      //               }
      //
      //               attr = getTextValue(cel, "translation");
      //               if(attr != null) {
      //                  w.setPosition(new Point3d(parseDoubleArray(attr)));
      //               }
      //
      //               attr = getTextValue(cel, "active");
      //               if(attr != null && attr.equals("true")) {
      //                  w.setActive(true);
      //               } else {
      //                  w.setActive(false);
      //               }
      //
      //               attr = getTextValue(cel, "dimensions");
      //               if(attr != null) {
      //                  w.setDimensions(new Point3d(parseDoubleArray(attr)));
      //               } else {
      //                  w.setDimensions(new Point3d(0, 0, 0));
      //               }
      //
      //               w.setParentBody(body.getName());
      //               body.addWrapObject(w.getName());
      //               myWrapObjects.add(w);
      //            }
      //
      //            // Cylinders next
      //            nl = el.getElementsByTagName("WrapCylinder");
      //            for(int i = 0; i < nl.getLength(); i++) {
      //               Element cel = (Element) nl.item(i);
      //               WrapObject w = new WrapObject();
      //               w.setName(cel.getAttribute("name"));
      //               w.setType(WrapObject.WrapObjectType.WRAP_CYLINDER);
      //
      //               attr = getTextValue(cel, "xyz_body_rotation");
      //               if(attr != null) {
      //                  w.setOrientation(new Point3d(parseDoubleArray(attr)));
      //               }
      //
      //               attr = getTextValue(cel, "translation");
      //               if(attr != null) {
      //                  w.setPosition(new Point3d(parseDoubleArray(attr)));
      //               }
      //
      //               attr = getTextValue(cel, "active");
      //               if(attr != null && attr.equals("true")) {
      //                  w.setActive(true);
      //               } else {
      //                  w.setActive(false);
      //               }
      //
      //               w.setQuadrant(getTextValue(cel, "quadrant"));
      //
      //               attr = getTextValue(cel, "radius");
      //               if(attr != null) {
      //                  w.setRadius(getDouble(attr));
      //               }
      //
      //               attr = getTextValue(cel, "length");
      //               if(attr != null) {
      //                  w.setLength(getDouble(attr));
      //               }
      //
      //               w.setParentBody(body.getName());
      //               body.addWrapObject(w.getName());
      //               myWrapObjects.add(w);
      //            }
      //
      //            // Torus next
      //            nl = el.getElementsByTagName("WrapTorus");
      //            for(int i = 0; i < nl.getLength(); i++) {
      //               Element cel = (Element) nl.item(i);
      //               WrapObject w = new WrapObject();
      //               w.setName(cel.getAttribute("name"));
      //               w.setType(WrapObject.WrapObjectType.WRAP_TORUS);
      //
      //               attr = getTextValue(cel, "xyz_body_rotation");
      //               if(attr != null) {
      //                  w.setOrientation(new Point3d(parseDoubleArray(attr)));
      //               }
      //
      //               attr = getTextValue(cel, "translation");
      //               if(attr != null) {
      //                  w.setPosition(new Point3d(parseDoubleArray(attr)));
      //               }
      //
      //               attr = getTextValue(cel, "active");
      //               if(attr != null && attr.equals("true")) {
      //                  w.setActive(true);
      //               } else {
      //                  w.setActive(false);
      //               }
      //
      //               w.setQuadrant(getTextValue(cel, "quadrant"));
      //
      //               attr = getTextValue(cel, "inner_radius");
      //               if(attr != null) {
      //                  w.setInnerRadius (getDouble(attr));
      //               }
      //
      //               attr = getTextValue(cel, "outer_radius");
      //               if(attr != null) {
      //                  w.setOuterRadius (getDouble(attr));
      //               }
      //
      //               w.setParentBody(body.getName());
      //               body.addWrapObject(w.getName());
      //               myWrapObjects.add(w);
      //            }
      //         }
      //      }
      
      return success;
   }
   
}
