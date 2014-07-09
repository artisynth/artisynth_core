/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;
import java.io.*;
import maspack.util.*;
import java.awt.Color;

public class EditTest extends TestHierarchy {
   static final PropertyMode Explicit = PropertyMode.Explicit;
   static final PropertyMode Inherited = PropertyMode.Inherited;
   static final PropertyMode VoidMode = PropertyMode.Void;
   static final PropertyMode Inactive = PropertyMode.Inactive;
   static final Object Composite = CompositeProperty.class;

   void checkEditorContents (LiveEditor editor, Object[] contents) {
      int numProps = editor.numProperties();
      if (contents.length / 3 != numProps) {
         throw new TestException ("editor has " + numProps + " properties but "
         + contents.length / 3 + " are specified");
      }
      for (int i = 0; i < contents.length; i += 3) {
         if (!(contents[i] instanceof String)) {
            throw new IllegalArgumentException ("property name expected, got "
            + contents[i]);
         }
         String name = (String)contents[i];
         EditingProperty prop = editor.getProperty (name);
         if (prop == null) {
            throw new TestException ("Property '" + name + "' not found");
         }
         Object value = prop.get();
         Object expectedValue = contents[i + 1];
         if (CompositeProperty.class.isAssignableFrom (
                prop.getInfo().getValueClass())) {
            if (expectedValue == Composite && value == null) {
               throw new TestException ("Property '" + name
               + "': value is null, composite expected");
            }
            else if (expectedValue == Property.VoidValue &&
                     value != Property.VoidValue) {
               throw new TestException ("Property '" + name + "': value is "
               + value + ", void expected");
            }
         }
         else {
            if (!PropertyUtils.equalValues (prop.get(), expectedValue)) {
               throw new TestException ("Property '" + name + "': value is "
               + prop.get() + ", expected " + contents[i + 1]);
            }
         }
         if (!(contents[i + 2] instanceof PropertyMode)) {
            throw new IllegalArgumentException ("property mode expected, got "
            + contents[i + 2]);
         }
         PropertyMode mode = prop.getMode();
         PropertyMode expectedMode = (PropertyMode)contents[i + 2];
         if (mode != expectedMode) {
            throw new TestException ("Property '" + name + "': mode is " + mode
            + ", expected " + expectedMode);
         }
      }
   }

   public EditTest() {
      super();
      S2.setStyle (3);
   }

   public void test() {
      HashMap<String,PropTreeCell> origPropMap = recordAllProperties (myRoot);
      HashMap<String,PropTreeCell> editOpenPropMap;
      newPropMap = recordAllProperties (myRoot);

      // basic panel check with one property:

      HostList hostList = new HostList (new HasProperties[] { M1 });
      LiveEditor editor = new LiveEditor (hostList);

      checkEditorContents (editor, new Object[] { "style", 3, Inherited,
                                                 "renderInfo", Composite,
                                                 Inactive, "material",
                                                 Composite, Inactive,
                                                 "modulus", 2.0, Inactive,
                                                 "order", 1, Inactive, "color",
                                                 red, Inherited });

      editor.set ("style", 5);
      editor.set ("color", green);
      editor.set ("modulus", 13.);

      checkChanges (new Object[] { "M1", "M style", 5,
      /* */"M color", green,
      /* */"M modulus", 13.0, END, "S2", "M color", green, END, "M3",
                                  "M color", green, END, "T4", "M style", 5,
                                  END, "M5", "M style", 5,
                                  /* */"M color", green, END, "S6", "M style",
                                  5,
                                  /* */"M color", green, END, });

      checkEditorContents (editor, new Object[] { "style", 5, Explicit,
                                                 "renderInfo", Composite,
                                                 Inactive, "material",
                                                 Composite, Inactive,
                                                 "modulus", 13.0, Inactive,
                                                 "order", 1, Inactive, "color",
                                                 green, Explicit });

      editor.setMode ("style", Inherited);

      checkChanges (new Object[] { "M1", "M style", 3, END, "T4", "M style", 3,
                                  END, "M5", "M style", 3, END, "S6",
                                  "M style", 3, END });

      editor.set ("style", 5);

      editor.cancel();

      newPropMap = origPropMap;
      checkChanges (new Object[] {});

      // now open a sub-panel

      hostList.set (new HasProperties[] { M1 });
      editor = new LiveEditor (hostList);
      LiveEditor matEditor = editor.edit ("material");

      checkEditorContents (matEditor, new Object[] { "density", 1.0, Explicit,
                                                    "stiffness", 10.0,
                                                    Inherited, "damping", 0.1,
                                                    Inherited, "renderInfo",
                                                    null, Inactive, "file",
                                                    null, Inactive, "activity",
                                                    0, Inactive });

      matEditor.set ("stiffness", 20.0);
      matEditor.set ("file", "foo");

      checkChanges (new Object[] { "M1", "M material.stiffness", 20.0,
      /* */"M material.file", "foo", END, "S2", "M material.stiffness", 20.0,
                                  END, "M3", "M material.stiffness", 20.0, END,
                                  "S6", "M material.stiffness", 20.0, END,
                                  "M5", "M material.stiffness", 20.0, END, });

      checkEditorContents (matEditor, new Object[] { "density", 1.0, Explicit,
                                                    "stiffness", 20.0,
                                                    Explicit, "damping", 0.1,
                                                    Inherited, "renderInfo",
                                                    null, Inactive, "file",
                                                    "foo", Inactive,
                                                    "activity", 0, Inactive });

      matEditor.dispose();
      editor.clear ("material");

      checkChanges (new Object[] { "M1", "A material", null,
      /* */"R material", null, END, "S2", "M material.stiffness", 10.0, END,
                                  "M3", "M material.stiffness", 10.0, END,
                                  "S6", "M material.stiffness", 10.0, END,
                                  "M5", "M material.stiffness", 10.0, END, });

      editor.cancel();

      newPropMap = origPropMap;
      checkChanges (new Object[] {});

      // open a sub-editor on renderPanel

      hostList.set (new HasProperties[] { M1 });
      editor = new LiveEditor (hostList);
      matEditor = editor.edit ("material");
      LiveEditor renderEditor = matEditor.edit ("renderInfo");

      checkEditorContents (renderEditor, new Object[] { "color", gray,
                                                       Inherited, "shine",
                                                       100.0, Inherited,
                                                       "width", 1, Explicit,
                                                       "textureFile", null,
                                                       Inactive });

      renderEditor.set ("color", blue);

      S2.getMaterial().setRenderInfo (new TestRenderInfo());

      checkChanges (new Object[] { "M1", "A material.renderInfo.color", blue,
      /* */"A material.renderInfo.shine", 100.0,
      /* */"A material.renderInfo.width", 1,
      /* */"A material.renderInfo.textureFile", null,
      /* */"R material.renderInfo", null, END, "S2",
                                  "A material.renderInfo.color", blue,
                                  /* */"A material.renderInfo.shine", 100.0,
                                  /* */"A material.renderInfo.width", 1,
                                  /* */"A material.renderInfo.textureFile",
                                  null,
                                  /* */"R material.renderInfo", null, END, });

      renderEditor.set ("shine", 200.0);

      checkChanges (new Object[] { "M1", "M material.renderInfo.shine", 200.0,
                                  END, "S2", "M material.renderInfo.shine",
                                  200.0, END });

      renderEditor.close();
      // reopen render editor panel; everything should be unchanged

      renderEditor = matEditor.edit ("renderInfo");
      checkEditorContents (renderEditor, new Object[] { "color", blue,
                                                       Explicit, "shine",
                                                       200.0, Explicit,
                                                       "width", 1, Explicit,
                                                       "textureFile", null,
                                                       Inactive });

      renderEditor.close();

      // now make some changes with the panel closed. Reopening
      // the panel should exhibit those changes

      M1.getMaterial().getRenderInfo().setColorMode (PropertyMode.Inherited);
      M1.getMaterial().getRenderInfo().setShine (666);

      checkChanges (new Object[] { "M1", "M material.renderInfo.shine", 666.0,
      /* */"M material.renderInfo.color", gray, END, "S2",
                                  "M material.renderInfo.shine", 666.0,
                                  /* */"M material.renderInfo.color", gray,
                                  END, });

      renderEditor = matEditor.edit ("renderInfo");
      checkEditorContents (renderEditor, new Object[] { "color", gray,
                                                       Inherited, "shine",
                                                       666.0, Explicit,
                                                       "width", 1, Explicit,
                                                       "textureFile", null,
                                                       Inactive });

      renderEditor.close();
      matEditor.clear ("renderInfo");

      checkChanges (new Object[] { "M1", "A material.renderInfo", null,
      /* */"R material.renderInfo", null, END, "S2",
                                  "M material.renderInfo.shine", 100.0, END, });

      // reopne the render editor, and what is installed should
      // be what was there before delete
      // ??? Do we want this, or do we want create() to be called???

      renderEditor = matEditor.edit ("renderInfo");
      checkEditorContents (renderEditor, new Object[] { "color", gray,
                                                       Inherited, "shine",
                                                       666.0, Explicit,
                                                       "width", 1, Explicit,
                                                       "textureFile", null,
                                                       Inactive });

      checkChanges (new Object[] { "M1", "A material.renderInfo.color", gray,
      /* */"A material.renderInfo.shine", 666.0,
      /* */"A material.renderInfo.width", 1,
      /* */"A material.renderInfo.textureFile", null,
      /* */"R material.renderInfo", null, END, "S2",
                                  "M material.renderInfo.shine", 666.0, END });

      editor.cancel();

      newPropMap = origPropMap;
      checkChanges (new Object[] { "S2", "A material.renderInfo.color", gray,
      /* */"A material.renderInfo.shine", 100.0,
      /* */"A material.renderInfo.width", 1,
      /* */"A material.renderInfo.textureFile", null,
      /* */"R material.renderInfo", null, END });

      // create an editor with S2, M1, and T4

      hostList.set (new HasProperties[] { M1, S2, T4 });
      editor = new LiveEditor (hostList);

      checkEditorContents (
         editor, new Object[] { "style", 3, VoidMode, "renderInfo",
                               Property.VoidValue, Inactive, });
      editor.close();

      S2.setStyle (7);

      editOpenPropMap = recordAllProperties (myRoot);

      editor = new LiveEditor (hostList);
      checkEditorContents (
         editor, new Object[] { "style", Property.VoidValue, VoidMode,
                               "renderInfo", Property.VoidValue, Inactive, });

      editor.set ("style", 10);
      checkEditorContents (
         editor, new Object[] { "style", 10, Explicit, "renderInfo",
                               Property.VoidValue, Inactive, });

      checkChanges (new Object[] { "M1", "M style", 10, END, "S2", "M style",
                                  10, END, "T4", "M style", 10, END, "M3",
                                  "M style", 10, END, "M5", "M style", 10, END,
                                  "S6", "M style", 10, END, });

      editor.setMode ("style", Inherited);

      checkEditorContents (
         editor, new Object[] { "style", 3, Inherited, "renderInfo",
                               Property.VoidValue, Inactive, });

      checkChanges (new Object[] { "M1", "M style", 3, END, "S2", "M style", 3,
                                  END, "T4", "M style", 3, END, "M3",
                                  "M style", 3, END, "M5", "M style", 3, END,
                                  "S6", "M style", 3, END, });

      renderEditor = editor.edit ("renderInfo");
      checkEditorContents (renderEditor, new Object[] { "color", gray,
                                                       Inherited, "shine",
                                                       100.0, Inherited,
                                                       "width", 1, Explicit,
                                                       "textureFile", null,
                                                       Inactive });

      renderEditor.close();
      T4.getRenderInfo().setColor (green);
      T4.getRenderInfo().setWidthMode (Inherited);
      T4.getRenderInfo().setShine (300);
      M1.getRenderInfo().setWidth (2);
      myRoot.getRenderInfo().setWidth (2);

      checkChanges (new Object[] { "root", "M renderInfo.width", 2, END, "M1",
                                  "M renderInfo.width", 2, END, "T4",
                                  "M renderInfo.color", green,
                                  /* */"M renderInfo.width", 2,
                                  /* */"M renderInfo.shine", 300.0, END, "M5",
                                  "M renderInfo.color", green,
                                  /* */"M renderInfo.shine", 300.0, END, "S6",
                                  "M renderInfo.color", green,
                                  /* */"M renderInfo.shine", 300.0, END, });

      renderEditor = editor.edit ("renderInfo");
      checkEditorContents (renderEditor, new Object[] { "color",
                                                       Property.VoidValue,
                                                       VoidMode, "shine",
                                                       Property.VoidValue,
                                                       VoidMode, "width",
                                                       Property.VoidValue,
                                                       VoidMode, "textureFile",
                                                       null, Inactive });

      renderEditor.set ("color", blue);
      renderEditor.set ("shine", 6.0);
      renderEditor.set ("width", 11);
      renderEditor.set ("textureFile", "bar");

      checkEditorContents (renderEditor, new Object[] { "color", blue,
                                                       Explicit, "shine", 6.0,
                                                       Explicit, "width", 11,
                                                       Explicit, "textureFile",
                                                       "bar", Inactive });

      checkChanges (new Object[] { "M1", "M renderInfo.color", blue,
      /* */"M renderInfo.shine", 6.0,
      /* */"M renderInfo.width", 11,
      /* */"M renderInfo.textureFile", "bar", END, "S2", "M renderInfo.color",
                                  blue,
                                  /* */"M renderInfo.shine", 6.0,
                                  /* */"M renderInfo.width", 11,
                                  /* */"M renderInfo.textureFile", "bar", END,
                                  "T4", "M renderInfo.color", blue,
                                  /* */"M renderInfo.shine", 6.0,
                                  /* */"M renderInfo.width", 11,
                                  /* */"M renderInfo.textureFile", "bar", END,
                                  "M3", "M renderInfo.color", blue,
                                  /* */"M renderInfo.shine", 6.0, END, "M5",
                                  "M renderInfo.color", blue,
                                  /* */"M renderInfo.shine", 6.0, END, "S6",
                                  "M renderInfo.color", blue,
                                  /* */"M renderInfo.shine", 6.0, END, });

      renderEditor.setMode ("color", Inherited);
      renderEditor.setMode ("shine", Inherited);
      renderEditor.setMode ("width", Inherited);

      checkEditorContents (renderEditor, new Object[] { "color", gray,
                                                       Inherited, "shine",
                                                       100.0, Inherited,
                                                       "width", 2, Inherited,
                                                       "textureFile", "bar",
                                                       Inactive });

      checkChanges (new Object[] { "M1", "M renderInfo.color", gray,
      /* */"M renderInfo.shine", 100.0,
      /* */"M renderInfo.width", 2, END, "S2", "M renderInfo.color", gray,
      /* */"M renderInfo.shine", 100.0,
      /* */"M renderInfo.width", 2, END, "T4", "M renderInfo.color", gray,
      /* */"M renderInfo.shine", 100.0,
      /* */"M renderInfo.width", 2, END, "M3", "M renderInfo.color", gray,
      /* */"M renderInfo.shine", 100.0, END, "M5", "M renderInfo.color", gray,
      /* */"M renderInfo.shine", 100.0, END, "S6", "M renderInfo.color", gray,
      /* */"M renderInfo.shine", 100.0, END, });

      renderEditor.set ("shine", 500.0);

      checkChanges (new Object[] { "M1", "M renderInfo.shine", 500.0, END,
                                  "S2", "M renderInfo.shine", 500.0, END, "T4",
                                  "M renderInfo.shine", 500.0, END, "M3",
                                  "M renderInfo.shine", 500.0, END, "M5",
                                  "M renderInfo.shine", 500.0, END, "S6",
                                  "M renderInfo.shine", 500.0, END, });

      renderEditor.close();
      editor.clear ("renderInfo");

      checkEditorContents (editor, new Object[] { "style", 3, Inherited,
                                                 "renderInfo", null, Inactive });

      checkChanges (new Object[] { "M1", "A renderInfo", null,
      /* */"R renderInfo", null, END, "S2", "A renderInfo", null,
      /* */"R renderInfo", null, END, "T4", "A renderInfo", null,
      /* */"R renderInfo", null, END, "M3", "M renderInfo.shine", 100.0, END,
                                  "M5", "M renderInfo.shine", 100.0, END, "S6",
                                  "M renderInfo.shine", 100.0, END, });

      editor.cancel();

      newPropMap = editOpenPropMap;
      checkChanges (new Object[] { "root", "M renderInfo.width", 2, END });

      T4.setRenderInfo (null);

      editOpenPropMap = recordAllProperties (myRoot);
      editor = new LiveEditor (hostList);
      editor.set ("style", 10);
      renderEditor = editor.edit ("renderInfo");
      renderEditor.setMode ("width", Inherited);
      renderEditor.set ("color", blue);

      editor.cancel();
      newPropMap = editOpenPropMap;
      checkChanges (new Object[] {});

      editOpenPropMap = recordAllProperties (myRoot);
      hostList.set (new HasProperties[] { S2, M8, S9 });
      editor = new LiveEditor (hostList);

      checkEditorContents (
         editor, new Object[] { "style", Property.VoidValue, VoidMode,
                               "renderInfo", Composite, Inactive, "material",
                               Composite, Inactive, "color",
                               Property.VoidValue, Inherited });

      editor.set ("style", 9);

      checkEditorContents (
         editor, new Object[] { "style", 9, Explicit, "renderInfo", Composite,
                               Inactive, "material", Composite, Inactive,
                               "color", Property.VoidValue, Inherited });

      checkChanges (new Object[] { "S2", "M style", 9, END, "M8", "M style", 9,
                                  END, "S9", "M style", 9, END, "M3",
                                  "M style", 9, END, "T10", "M style", 9, END, });

      editor.setMode ("style", Inherited);
      checkEditorContents (
         editor, new Object[] { "style", Property.VoidValue, Inherited,
                               "renderInfo", Composite, Inactive, "material",
                               Composite, Inactive, "color",
                               Property.VoidValue, Inherited });
      checkChanges (new Object[] { "S2", "M style", 3, END, "M8", "M style", 1,
                                  END, "S9", "M style", 1, END, "M3",
                                  "M style", 3, END, "T10", "M style", 1, END, });

      matEditor = editor.edit ("material");
      checkEditorContents (matEditor, new Object[] { "density", 1.0, Explicit,
                                                    "stiffness", 10.0,
                                                    Inherited, "damping", 0.1,
                                                    Inherited, "renderInfo",
                                                    Property.VoidValue,
                                                    Inactive, "file", null,
                                                    Inactive, "activity", 0,
                                                    Inactive });

      renderEditor = matEditor.edit ("renderInfo");

      checkEditorContents (renderEditor, new Object[] { "color", gray,
                                                       Inherited, "shine",
                                                       100.0, Inherited,
                                                       "width", 1, Explicit,
                                                       "textureFile", null,
                                                       Inactive });

      checkChanges (new Object[] { "M8", "A material.renderInfo.color", gray,
      /* */"A material.renderInfo.shine", 100.0,
      /* */"A material.renderInfo.width", 1,
      /* */"A material.renderInfo.textureFile", null,
      /* */"R material.renderInfo", null, END, "S9",
                                  "A material.renderInfo.color", gray,
                                  /* */"A material.renderInfo.shine", 100.0,
                                  /* */"A material.renderInfo.width", 1,
                                  /* */"A material.renderInfo.textureFile",
                                  null,
                                  /* */"R material.renderInfo", null, END, });

      renderEditor.set ("color", blue);
      renderEditor.set ("shine", 666.0);
      renderEditor.set ("textureFile", "bar");

      checkEditorContents (renderEditor, new Object[] { "color", blue,
                                                       Explicit, "shine",
                                                       666.0, Explicit,
                                                       "width", 1, Explicit,
                                                       "textureFile", "bar",
                                                       Inactive });

      checkChanges (new Object[] { "S2", "M material.renderInfo.color", blue,
      /* */"M material.renderInfo.shine", 666.0,
      /* */"M material.renderInfo.textureFile", "bar", END, "M8",
                                  "M material.renderInfo.color", blue,
                                  /* */"M material.renderInfo.shine", 666.0,
                                  /* */"M material.renderInfo.textureFile",
                                  "bar", END, "S9",
                                  "M material.renderInfo.color", blue,
                                  /* */"M material.renderInfo.shine", 666.0,
                                  /* */"M material.renderInfo.textureFile",
                                  "bar", END, });

      editor.cancel();
      newPropMap = editOpenPropMap;
      checkChanges (new Object[] {});

      S2.getMaterial().getRenderInfo().setColor (blue);
      S2.getMaterial().getRenderInfo().setShine (666.0);
      S2.getMaterial().getRenderInfo().setTextureFile ("foo");

      editOpenPropMap = recordAllProperties (myRoot);
      editor = new LiveEditor (hostList);
      matEditor = editor.edit ("material");
      renderEditor = matEditor.edit ("renderInfo");

      checkEditorContents (renderEditor, new Object[] { "color",
                                                       Property.VoidValue,
                                                       VoidMode, "shine",
                                                       Property.VoidValue,
                                                       VoidMode, "width", 1,
                                                       Explicit, "textureFile",
                                                       Property.VoidValue,
                                                       Inactive });

      renderEditor.set ("color", white);
      renderEditor.set ("shine", 333.0);
      renderEditor.set ("textureFile", "foo");

      checkEditorContents (renderEditor, new Object[] { "color", white,
                                                       Explicit, "shine",
                                                       333.0, Explicit,
                                                       "width", 1, Explicit,
                                                       "textureFile", "foo",
                                                       Inactive });

      checkChanges (new Object[] { "S2", "M material.renderInfo.color", white,
      /* */"M material.renderInfo.shine", 333.0,
      /* */"M material.renderInfo.textureFile", "foo", END, "M8",
                                  "A material.renderInfo.color", white,
                                  /* */"A material.renderInfo.shine", 333.0,
                                  /* */"A material.renderInfo.textureFile",
                                  "foo",
                                  /* */"A material.renderInfo.width", 1,
                                  /* */"R material.renderInfo", null, END,
                                  "S9", "A material.renderInfo.color", white,
                                  /* */"A material.renderInfo.shine", 333.0,
                                  /* */"A material.renderInfo.textureFile",
                                  "foo",
                                  /* */"A material.renderInfo.width", 1,
                                  /* */"R material.renderInfo", null, END, });

      editor.cancel();
      newPropMap = editOpenPropMap;
      checkChanges (new Object[] {});

      editOpenPropMap = recordAllProperties (myRoot);
      hostList.set (new HasProperties[] { S2, M1 });
      editor = new LiveEditor (hostList);

      checkEditorContents (
         editor, new Object[] { "style", Property.VoidValue, VoidMode,
                               "renderInfo", Composite, Inactive, "material",
                               Composite, Inactive, "color",
                               Property.VoidValue, Inherited });

      editor.setMode ("color", Explicit);

      checkEditorContents (editor, new Object[] { "style", Property.VoidValue,
                                                 VoidMode, "renderInfo",
                                                 Composite, Inactive,
                                                 "material", Composite,
                                                 Inactive, "color",
                                                 Property.VoidValue, Explicit });

      editor.setMode ("color", Inherited);

      checkEditorContents (
         editor, new Object[] { "style", Property.VoidValue, VoidMode,
                               "renderInfo", Composite, Inactive, "material",
                               Composite, Inactive, "color",
                               Property.VoidValue, Inherited });
   }

   public static void main (String[] args) {
      EditTest tester = new EditTest();
      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}

class LiveEditor {
   HostList myHostList;
   PropTreeCell myCell;
   LinkedHashMap<String,EditingProperty> myPropMap;

   public LiveEditor (HostList hostList) {
      this (hostList, null);
   }

   public LiveEditor (HostList hostList, PropTreeCell cell) {
      myHostList = hostList;
      if (cell == null) {
         cell = new PropTreeCell();
      }
      myCell = cell;

      if (!cell.hasChildren()) {
         cell.addChildren (hostList.commonProperties (cell, false));
         // System.out.println ("numHosts: " + hostList.numHosts());
         // System.out.println ("Common props:\n" + cell.treeString());
         hostList.saveBackupValues (cell);
         hostList.addSubHostsIfNecessary (cell);
      }
      else if (cell.getValue() == null) {
         hostList.addSubHostsIfNecessary (cell);
      }
      hostList.getCommonValues (cell, /* live= */true);
      cell.setValue (CompositeProperty.class);

      LinkedList<Property> propList =
         EditingProperty.createProperties (cell, hostList, /* isLive= */true);

      myPropMap = new LinkedHashMap<String,EditingProperty>();
      for (Property prop : propList) {
         myPropMap.put (prop.getName(), (EditingProperty)prop);
      }
   }

   public EditingProperty getProperty (String name) {
      EditingProperty prop = myPropMap.get (name);
      if (prop == null) {
         throw new IllegalArgumentException ("property '" + name
         + "' not found");
      }
      return prop;
   }

   public int numProperties() {
      return myPropMap.size();
   }

   public Iterable<? extends Property> getProperties() {
      return myPropMap.values();
   }

   public void set (String name, Object value) {
      getProperty (name).set (value);
   }

   public Object get (String name) {
      return getProperty (name).get();
   }

   public void setMode (String name, PropertyMode mode) {
      getProperty (name).setMode (mode);
   }

   public PropertyMode getMode (String name) {
      return getProperty (name).getMode();
   }

   public void clear (String name) {
      EditingProperty prop = getProperty (name);
      if (!CompositeProperty.class.isAssignableFrom (
             prop.getInfo().getValueClass())) {
         throw new IllegalArgumentException ("property '" + name
         + "' is not a CompositeProperty");
      }
      prop.set (null);
   }

   public LiveEditor edit (String name) {
      EditingProperty prop = getProperty (name);
      if (!CompositeProperty.class.isAssignableFrom (
             prop.getInfo().getValueClass())) {
         throw new IllegalArgumentException ("property '" + name
         + "' is not a CompositeProperty");
      }
      return new LiveEditor (myHostList, prop.getCell());
   }

   public void cancel() {
      if (myCell.getParent() != null) {
         throw new IllegalStateException (
            "cancel is only supported for top-level editors");
      }
      myHostList.restoreBackupValues();
      dispose();
   }

   public void close() {
      dispose();
   }

   public void dispose() {
      myPropMap = null;
      myHostList = null;
      myCell = null;
   }

}
