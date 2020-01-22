/**
 * Copyright (c) 2014, by the Authors: Johnty Wang (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.probeEditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.BevelBorder;

import maspack.properties.CompositeProperty;
import maspack.properties.NumericConverter;
import maspack.properties.Property;
import maspack.widgets.DoubleField;
import maspack.widgets.DoubleIntervalField;
import maspack.widgets.GuiUtils;
import maspack.widgets.LabeledComponentPanel;
import maspack.widgets.StringField;
import maspack.util.StringHolder;
import artisynth.core.driver.Main;
import artisynth.core.gui.timeline.GuiStorage;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.NumericProbeDriver;
import artisynth.core.probes.NumericProbeVariable;
import artisynth.core.probes.Probe;
import artisynth.core.util.ArtisynthPath;

/**
 * @author Johnty
 * @version 0.1 Class for representing a dialog that allows the addition of new
 * probes
 */

public class NumericProbeEditor extends JFrame implements ActionListener {
   static final long serialVersionUID = 0;

   public static final Color activeColor = null;

   public static final Color inactiveColor = Color.LIGHT_GRAY;
   public static final Color completedColor = new Color (150, 255, 150);

   protected static final int DEFAULT_DIM = 1;
   protected static final String INPUT_PREFIX = "V";
   protected static final String OUTPUT_PREFIX = "P";

   private int FRAME_WIDTH = 720;
   private int FRAME_HEIGHT = 250;
   private static final int minFrameW = 720;
   private static final int minFrameH = 250;

   private static int BORDER_WIDTH = 10;
   private static int INSET_H = 3;
   private static int INSET_W = 10;
   private static final int LINE_HEIGHT = 35; // the amount of space allocated
   // per line;

   // private static boolean isInputProbe;

   protected String originalFilePath;

   protected JPanel mainPane; // the main panel that contains everything else
   // in this dialog

   protected JPanel infoPaneA; //
   protected JPanel infoPaneB;
   protected JPanel infoPaneC;
   protected JLabel nameLabel;

   protected JSplitPane splitPane;

   protected JPanel middlePane; //
   protected JPanel vectorPane;
   protected JPanel equationPane;
   protected JPanel propPane;

   protected JPanel bottomPane;
   protected JPanel optionsPane;
   protected LabeledComponentPanel leftPropertyPanel;
   protected LabeledComponentPanel rightPropertyPanel;

   // protected JPanel filePane;

   protected JButton addVectorButton, addPropertyButton;
   protected JButton browseButton, addProbeButton, closeButton;

   protected StringField attachedFileField;

   protected DoubleField startTimeField;
   protected DoubleField endTimeField;
   protected DoubleField scaleField;
   protected StringField probeNameField;
   protected DoubleField intervalField;
   protected DoubleField dispUpperField;
   protected DoubleField dispLowerField;
   protected DoubleIntervalField rangeField;

   protected File attachedFile;

   protected ArrayList<AddPropertyPane> propList;
   protected ArrayList<AddEquationPane> eqList;
   protected ArrayList<AddVectorPane> probeChannels;

   protected ArrayList<Integer> usedVectorIndicies;
   protected ArrayList<Integer> usedPropIndicies;

   // these are the components that contain all the probe information
   protected ArrayList<NumericProbeDriver> myDrivers;
   protected LinkedHashMap<String,NumericProbeVariable> myVariables;
   protected ArrayList<Property> myProperties;

   protected boolean isReady;
   protected Main myMain;

   public NumericProbeEditor () {
      isReady = false;
      usedVectorIndicies = new ArrayList<Integer>();
      usedPropIndicies = new ArrayList<Integer>();
      setTitle ("Probe Editor");

      // the following items should be in sync with each other:
      // (GUI object) (Probe object)
      // probeChannels <=> myVariables
      // eqList <=> myDrivers
      // propList <=> myProperties

      probeChannels = new ArrayList<AddVectorPane>();
      eqList = new ArrayList<AddEquationPane>();
      propList = new ArrayList<AddPropertyPane>();

      myDrivers = new ArrayList<NumericProbeDriver>();
      myVariables = new LinkedHashMap<String,NumericProbeVariable>();
      myProperties = new ArrayList<Property>();

      setDefaultCloseOperation (DISPOSE_ON_CLOSE);

      addComponentListener (new java.awt.event.ComponentAdapter() {
         public void componentResized (ComponentEvent event) {
         // If we ever want to restrict the resizing, uncomment these lines:

         // FRAME_WIDTH = Math.max(minFrameW, frame.getWidth());
         // FRAME_HEIGHT = Math.max(minFrameH, frame.getHeight());
         // frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
         }
      });

      myMain = Main.getMain();

      mainPane = new JPanel();
      mainPane.setSize (FRAME_WIDTH, FRAME_HEIGHT);
      mainPane.setLayout (new BoxLayout (mainPane, BoxLayout.Y_AXIS));
      add (mainPane);
      createSubPanes();

      fillPanes();
      pack();

   }

   protected void resizeFrame() {
      FRAME_WIDTH = Math.max (minFrameW, getWidth());
      FRAME_HEIGHT = Math.max (minFrameH, getHeight());
      setSize (FRAME_WIDTH, FRAME_HEIGHT);
   }

   private void createSubPanes() {

      mainPane.add (Box.createRigidArea (new Dimension (0, 4)));

      middlePane = new JPanel();
      middlePane.setLayout (new BoxLayout (middlePane, BoxLayout.X_AXIS));
      vectorPane = new JPanel();
      vectorPane.setLayout (new BoxLayout (vectorPane, BoxLayout.Y_AXIS));
      vectorPane.setMaximumSize (new Dimension (85, Integer.MAX_VALUE));
      vectorPane.setPreferredSize (new Dimension (85, 50));
      vectorPane.setOpaque (false);
      vectorPane.setBorder (
         BorderFactory.createBevelBorder (BevelBorder.LOWERED));
      vectorPane.setAlignmentY (Component.TOP_ALIGNMENT);

      splitPane = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT);
      splitPane.setAlignmentY (TOP_ALIGNMENT);
      splitPane.setMaximumSize (new Dimension (
         Integer.MAX_VALUE, Integer.MAX_VALUE));
      splitPane.setDividerSize (2);
      splitPane.setResizeWeight (0.5);
      splitPane.setBorder (
         BorderFactory.createBevelBorder (BevelBorder.LOWERED));

      propPane = new JPanel();
      propPane.setLayout (new BoxLayout (propPane, BoxLayout.Y_AXIS));
      propPane.setMaximumSize (new Dimension (
         Integer.MAX_VALUE, Integer.MAX_VALUE));
      propPane.setOpaque (false);
      // propPane.setBorder(new LineBorder(Color.black));

      equationPane = new JPanel();
      equationPane.setLayout (new BoxLayout (equationPane, BoxLayout.Y_AXIS));
      equationPane.setMaximumSize (new Dimension (
         Integer.MAX_VALUE, Integer.MAX_VALUE));
      equationPane.setOpaque (false);
      equationPane.setAlignmentY (Component.TOP_ALIGNMENT);
      // equationPane.setBorder(new LineBorder(Color.black));

      mainPane.add (middlePane);

      // filePane = new JPanel();
      // filePane.setLayout(new BoxLayout(filePane, BoxLayout.X_AXIS));
      // filePane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
      // filePane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
      // mainPane.add(filePane);

      optionsPane = new JPanel();
      optionsPane.setLayout (new BoxLayout (optionsPane, BoxLayout.X_AXIS));
      // optionsPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
      // mainPane.add(optionsPane);

      leftPropertyPanel = new LabeledComponentPanel();
      rightPropertyPanel = new LabeledComponentPanel();
      leftPropertyPanel.setBorder (BorderFactory.createEtchedBorder());
      rightPropertyPanel.setBorder (BorderFactory.createEtchedBorder());
      optionsPane.add (leftPropertyPanel);
      optionsPane.add (rightPropertyPanel);
      // propertyPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
      mainPane.add (optionsPane);

      bottomPane = new JPanel();
      bottomPane.setLayout (new BoxLayout (bottomPane, BoxLayout.X_AXIS));
      bottomPane.setAlignmentX (CENTER_ALIGNMENT);
      bottomPane.setBorder (BorderFactory.createEmptyBorder (5, 10, 5, 10));
      mainPane.add (bottomPane);

      // vectorPane.setBackground(Color.green);
      // vectorPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
      // equationPane.setBackground(Color.green);
      // equationPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
   }

   private void fillPanes() {
      vectorPane.removeAll();
      propPane.removeAll();

      addVectorButton = new JButton (GuiStorage.getAddIcon());
      GuiUtils.setFixedSize (addVectorButton, 25, 25);
      addVectorButton.addActionListener (this);
      addVectorButton.setActionCommand ("Add");
      addVectorButton.setAlignmentX (LEFT_ALIGNMENT);
      addVectorButton.setOpaque (true);

      addPropertyButton = new JButton (GuiStorage.getAddIcon());
      GuiUtils.setFixedSize (addPropertyButton, 25, 25);
      addPropertyButton.setActionCommand ("Add Property");
      addPropertyButton.addActionListener (this);
      addPropertyButton.setAlignmentX (LEFT_ALIGNMENT);
      addPropertyButton.setOpaque (true);

      infoPaneA = new JPanel();
      infoPaneA.setLayout (new BoxLayout (infoPaneA, BoxLayout.X_AXIS));
      infoPaneA.setOpaque (false);

      infoPaneB = new JPanel();
      infoPaneB.setLayout (new BoxLayout (infoPaneB, BoxLayout.X_AXIS));
      infoPaneB.setOpaque (false);

      nameLabel = new JLabel ("Name");
      infoPaneA.setAlignmentX (LEFT_ALIGNMENT);
      infoPaneA.add (nameLabel);
      infoPaneA.add (Box.createRigidArea (new Dimension (15, 0)));
      infoPaneA.add (Box.createHorizontalGlue());
      infoPaneA.add (new JLabel ("Dim"));

      infoPaneC = new JPanel();
      infoPaneC.setLayout (new BoxLayout (infoPaneC, BoxLayout.X_AXIS));
      infoPaneC.add (Box.createHorizontalGlue());
      infoPaneC.add (new JLabel ("Formula"));
      infoPaneC.add (Box.createHorizontalGlue());
      infoPaneC.setOpaque (false);
      infoPaneC.setAlignmentX (LEFT_ALIGNMENT);

      infoPaneB.add (Box.createRigidArea (new Dimension (50, 0)));
      infoPaneB.add (Box.createHorizontalGlue());
      infoPaneB.add (new JLabel ("Component"));
      infoPaneB.add (Box.createHorizontalGlue());
      infoPaneB.add (new JLabel ("Property"));
      infoPaneB.setAlignmentX (LEFT_ALIGNMENT);
      infoPaneB.add (Box.createRigidArea (new Dimension (100, 0)));

      vectorPane.add (infoPaneA);
      propPane.add (infoPaneB);
      equationPane.add (infoPaneC);

      // filePane.add(new JLabel("File:"));
      // filePane.add(horizontalSpacer(10));
      attachedFileField = new StringField ("attached file", 24);
      // filePane.add(attachedFileField);
      // filePane.add(horizontalSpacer(10));
      browseButton = new JButton ("Browse");
      browseButton.addActionListener (this);
      attachedFileField.addMajorComponent (browseButton);
      // filePane.add(browseButton);

      // todo: retreive default values from Probe class
      startTimeField =
         new DoubleField (
            "start time", Probe.getDefaultStartTime());
      startTimeField.setColumns (8);
      startTimeField.setFormat ("%8.4f");
      endTimeField =
         new DoubleField (
            "end time", Probe.getDefaultStopTime());
      endTimeField.setColumns (8);
      endTimeField.setFormat ("%8.4f");
      scaleField = new DoubleField ("scale", Probe.getDefaultScale());
      scaleField.setColumns (8);
      intervalField = new DoubleField ("update interval", 0.01, "%8.4f");
      intervalField.setColumns (8);
      // intervalField.setFormat ("%8.4f");
      probeNameField = new StringField ("name", 8);
      probeNameField.setValue ("");
      probeNameField.setStretchable (true);

      dispUpperField = new DoubleField ("upper", 50);
      dispUpperField.setColumns (4);
      dispLowerField = new DoubleField ("lower", -50);
      dispLowerField.setColumns (4);

      rangeField = new DoubleIntervalField ("display range");
      rangeField.setGUIVoidEnabled (true);
      rangeField.setVoidValueEnabled (true);

      // optionsPane.add(probeNameField);
      // optionsPane.add(horizontalSpacer(5));
      // optionsPane.add(startTimeField);
      // optionsPane.add(horizontalSpacer(5));
      // optionsPane.add(endTimeField);
      // optionsPane.add(horizontalSpacer(5));

      leftPropertyPanel.addWidget (probeNameField);
      leftPropertyPanel.addWidget (startTimeField);
      leftPropertyPanel.addWidget (endTimeField);
      // optionsPane.add(scaleField);
      // optionsPane.add(Box.createRigidArea(new Dimension(5, 0)));

      addProbeButton = new JButton ("Done");
      addProbeButton.addActionListener (this);

      closeButton = new JButton ("Cancel");
      closeButton.addActionListener (this);

      bottomPane.add (addProbeButton);
      bottomPane.add (Box.createRigidArea (new Dimension (10, 0)));
      bottomPane.add (closeButton);
   }

   static public Color getHoverColor (Color inColor) {
      int red = Math.max (inColor.getRed() - 50, 0);
      int green = Math.max (inColor.getGreen() - 50, 0);
      int blue = Math.max (inColor.getBlue() - 50, 0);
      return new Color (red, green, blue);
   }

   protected void setupProbeProperties (Probe probe) {
      if (probe.isInput()) {
         scaleField.setValue (probe.getScale());
      }
      else {
         intervalField.setValue (probe.getUpdateInterval());
      }
      startTimeField.setValue (probe.getStartTime());
      endTimeField.setValue (probe.getStopTime());
      probeNameField.setValue (probe.getName());
   }

   static protected Component horizontalSpacer (int width) {
      return Box.createRigidArea (new Dimension (width, 0));
   }

   protected void invalidateDriverAndGUI (int id) {
      myDrivers.get (id).setInvalid();
      eqList.get (id).setEqText ("");
   }

   public void removeChannelGUI (int id) {
      vectorPane.remove (probeChannels.get (id));
      probeChannels.remove (id);
      vectorPane.revalidate();
      vectorPane.repaint();
   }

   public void removeChannelGUI (AddVectorPane pane) {
      removeFromListAndGUI (probeChannels, vectorPane, pane);
   }

   public void removePropertyGUI (int id) {
      // propList.get(id).detachCompFieldSelListener();
      AddPropertyPane app = propList.remove (id);
      propPane.remove (app);
      app.dispose();
      propPane.revalidate();
      propPane.repaint();
      if (propList.size() == 0) {
         addPropertyButton.setEnabled (true);
      }
   }

   // public void removePropertyGUI(AddPropertyPane pane)
   // {
   // removeFromListAndGUI(propList, propPane, pane);
   // }

   public void removeEquationGUI (int id) {
      equationPane.remove (eqList.get (id));
      eqList.remove (id);
      equationPane.revalidate();
      equationPane.repaint();
   }

   // public void removeEquationGUI(AddEquationPane pane)
   // {
   // removeFromListAndGUI(eqList, equationPane, pane);
   // }

   private void removeFromListAndGUI (
      ArrayList list, JPanel containerPane, Component elem) {
      containerPane.remove (elem);
      list.remove (elem);
      containerPane.revalidate();
      containerPane.repaint();
   }

   protected AddVectorPane getTrack (int id) {
      if (id < probeChannels.size()) {
         return probeChannels.get (id);
      }
      return null;
   }

   protected AddPropertyPane getPropPane (int id) {
      if (id < propList.size()) {
         return propList.get (id);
      }
      return null;
   }

   protected AddEquationPane getEqPane (int id) {
      if (id < eqList.size()) {
         return eqList.get (id);
      }
      return null;
   }

   /**
    * returns an unused index, and add it to the used indices list. if it turns
    * out to make more sense to update the used indices manually, then don't do
    * add(i) in this method
    * 
    * @return unused index
    */
   protected int getUnusedIndex (ArrayList<Integer> array) {
      int i = 0;
      while (array.contains (i)) {
         i++;
      }
      array.add (i);
      return i;
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      System.out.println ("Numeric Probe Editor received event: " + cmd);

      if (cmd.equals ("Cancel")) {
         dispose();
      }
      else if (cmd.equals ("Browse")) {
         JFileChooser fileDialog = new JFileChooser();
         if (attachedFile != null) {
            fileDialog.setSelectedFile (attachedFile);
         }
         else {
            System.out.println (ArtisynthPath.getWorkingDir().getPath());
            fileDialog.setCurrentDirectory (ArtisynthPath.getWorkingDir());
         }
         int retVal = fileDialog.showOpenDialog (this);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            attachedFile = fileDialog.getSelectedFile();
            attachedFileField.setValue (Probe.getPathFromFile (attachedFile));
         }
      }
      else if (cmd.equals ("Invalidate")) {
         AddPropertyPane pane = (AddPropertyPane)e.getSource();
         pane.updateAppearance();
         propPane.repaint();
         refreshAddBtn();
      }
      else if (cmd.equals ("Done")) {
         Property[] properties = new Property[propList.size()];
         for (int i = 0; i < propList.size(); i++) {
            String fullPropPath = propList.get (i).getPropPath();

            // String compPath = splitPath(fullPropPath, true);
            // String propPath = splitPath(fullPropPath, false);
            // System.out.println(compPath);
            // System.out.println(propPath);
            Property prop =
               ComponentUtils.findProperty (myMain.getRootModel(), fullPropPath);
            if (prop != null) {
               properties[i] = prop;
            }
         }
      }
      else if (cmd.equals ("Change Focus")) {
         AddPropertyPane focusedPane = (AddPropertyPane)e.getSource();
         for (AddPropertyPane pane : propList) // disable edit for all other
         // properties
         {
            if (pane != focusedPane) {
               pane.setEditable (false);
               pane.updateAppearance();
            }
         }
         // AddPropertyPane focusedPane = (AddPropertyPane)e.getSource();
         // focusedPane.setEditable(true);
         // focusedPane.refreshColor();
         propPane.repaint();
      }
   }

   // TODO: probably makes more sense to move the next few methods into
   // AddProeprtyPane class
   public static Property getProp (String fullPath) {
      return ComponentUtils.findProperty (
         Main.getMain().getRootModel(), fullPath);
      // String compPath = splitPath(fullPath, true);
      // String propPath = splitPath(fullPath, false);
      // ModelComponent comp = myMain.getRootModel().findComponent(
      // compPath);
      // Property prop = comp.getProperty(propPath);
      // return prop;
   }

   // public ModelComponent getTopComponent()
   // {
   // String fullPath = propList.get(0).getPropPath();
   // if (fullPath.startsWith (""))
   // { return myMain.getRootModel();
   // }
   // String compPath = fullPath.substring(0,fullPath.indexOf("/"));
   // fullPath = fullPath.substring(fullPath.indexOf("/")+1);
   // if (fullPath.indexOf("/") != -1)
   // compPath += "/" + fullPath.substring(0,fullPath.indexOf("/"));
   // System.out.println("Comp ROOT is "+compPath);

   // /*
   // * quick fix to bug when comp root is models -- get first model
   // */
   // if (compPath.compareTo("models")==0.0)
   // {
   // compPath = compPath + "/0";
   // System.out.println("Comp ROOT modified to be "+compPath);
   // }

   // ModelComponent comp =
   // myMain.getRootModel().findComponent(compPath);

   // return comp;
   // }

   /**
    * finds the number of dimensions of numeric property.
    * 
    * @return 0 if invalid
    */
   public static int GetPropDim (String fullPropPath) {
      Property prop = getProp (fullPropPath);
      if (prop != null) {
         if (NumericConverter.isNumeric (prop.get())) {
            try { // create a NumericPropertyInfo structure from the property in
               // order to find out its dimensions
               NumericConverter testNum = new NumericConverter (prop.get()); // test
               // to
               // see
               // if
               // property
               // is
               // numeric
               if (testNum != null) {
                  return testNum.getDimension();
               }
            }
            catch (Exception e) {
               System.out.println ("Caught exception:" + e.getMessage());
            }
         }
      }
      return 0;
   }

   public static int getPropDim (Property prop) {
      if (prop == null)
         return 0;
      NumericConverter conv = new NumericConverter (prop.get());
      return conv.getDimension();
   }

   // public static String splitPath(String fullPath, boolean isComp)
   // {
   // int index = fullPath.indexOf(".");
   // if (index < 0)
   // {
   // return null;
   // }
   // if (isComp)
   // {
   // return fullPath.substring(0, index);
   // }
   // else
   // {
   // return fullPath.substring(index + 1);
   // }
   // }

   /**
    * Adds the current track to the vector pane, and update the display.
    * 
    */
   // protected void addVectorGUI(int index)
   // {
   // AddVectorPane newTrack = new AddVectorPane(this, index);
   // probeChannels.add(newTrack);
   // vectorPane.add(newTrack.getPane());
   // vectorPane.revalidate();
   // }
   protected void addVectorGUI (AddVectorPane vec) {
      probeChannels.add (vec);
      vectorPane.add (vec, probeChannels.size());
      vectorPane.revalidate();
   }

   public void addPropertyGUI (AddPropertyPane newPane) {
      propList.add (newPane);
      for (AddPropertyPane pane : propList) // disable edit for all current
      // properties
      {
         pane.setEditable (false);
         pane.updateAppearance();
      }
      newPane.setEditable (true);
      propPane.add (newPane, propList.size());
      propPane.revalidate();
      newPane.updateAppearance();
      newPane.refreshContents();
   }

   public void addEquationGUI (AddEquationPane eqPane) {
      eqList.add (eqPane);
      equationPane.add (eqPane, eqList.size());
      equationPane.revalidate();
      equationPane.repaint();
   }

   public void load (NumericProbeBase probe) {
      // when opening a probe we create local copies of:
      //
      // 1) the properties
      // 2) the drivers
      // 3) the variables
      // 4) the variable dimensions (for input probes)

      NumericProbeDriver[] drivers = probe.getDrivers();
      myDrivers = new ArrayList<NumericProbeDriver>();
      for (int i = 0; i < drivers.length; i++) {
         myDrivers.add (new NumericProbeDriver (drivers[i]));
      }

      myProperties = new ArrayList<Property>();
      Property[] props = probe.getAttachedProperties();
      for (int i = 0; i < props.length; i++) {
         myProperties.add (props[i]);
      }

      myVariables = new LinkedHashMap<String,NumericProbeVariable>();
      for (Map.Entry<String,NumericProbeVariable> entry :
              probe.getVariables().entrySet()) {
         myVariables.put (entry.getKey(),
                          new NumericProbeVariable (entry.getValue()));
      }
   }

   protected String[] getDriverExpressions() {
      String[] drivers = new String[myDrivers.size()];
      for (int i = 0; i < myDrivers.size(); i++) {
         drivers[i] = myDrivers.get (i).getExpression();
         System.out.println ("driver expression "
         + myDrivers.get (i).getExpression() + " found.");
         System.out.println ("size of driver="
         + myDrivers.get (i).getOutputSize());
      }
      return drivers;
   }

   // 
   // Called when a user changes an expression
   // 
   public void changeExpression (String newexpr, int idx) {
      NumericProbeDriver driver = myDrivers.get (idx);
      try {
         driver.setExpression (newexpr, myVariables);
      }
      catch (Exception e) { // handle error
      }
      updateGUI();
   }

   public String getUniqueVariableName (String prefix) {
      // look through myVariables to find a unique name;
      if (prefix == NumericProbeEditor.INPUT_PREFIX) {
         for (int i = 0; i < myVariables.size(); i++) {
            if (myVariables.get (prefix + i) == null) {
               return prefix + i;
            }
         }
         return prefix + myVariables.size();
      }
      else {
         for (int i = 0; i > -1; i++) {
            if (!propNameExists (prefix + i)) {
               return prefix + i;
            }
         }
         return "";
      }
   }

   private boolean propNameExists (String name) {
      for (AddPropertyPane pane : propList) {
         if (pane.getPropNameFieldText().equals (name)) {
            return true;
         }
      }
      return false;
   }

   /**
    * if necessary, incase the frame size to fit newly added GUI components
    * 
    */
   protected void increaseFrameSize() {
      JFrame frame = this;
      int requiredHeight =
         Math.max ((propList.size() * 35), (probeChannels.size() * 35)) + 45;
      if ((requiredHeight > propPane.getHeight()) ||
          (requiredHeight > vectorPane.getHeight())) {
         setSize (frame.getWidth(), frame.getHeight() + 45);
      }
   }

   //
   // Stub routine for updating the GUI
   //

   public void updateGUI() {
      // addProbeButton.setEnabled(isReady);
   }

   protected void refreshAddBtn() {
      if (isReady()) {
         addProbeButton.setEnabled (true);
      }
      else {
         addProbeButton.setEnabled (false);
      }
   }

   private boolean isReady() {
      // System.out.println("checking if Ready...");
      // System.out.println("Props: "+ myProperties.size()+" Drivers: "+
      // myDrivers.size()+" Vars: "+myVariables.size());
      // System.out.println("# of gui propPanes: "+ propList.size()+" # of
      // selected properties: "+myProperties.size());
      if (myDrivers.isEmpty() ||
          myProperties.isEmpty() ||
          myVariables.isEmpty()) {
         // System.out.println("not ready: lists empty");
         return false;
      }
      for (AddPropertyPane pane : propList) {
         if (pane.isComplete() != true) {
            // implies some properties aren't ready.
            addPropertyButton.setEnabled (false);
            return false;
         }
      }
      addPropertyButton.setEnabled (true);
      for (NumericProbeDriver driver : myDrivers) {
         if (driver.isValid() == false) {
            // System.out.println("not ready: one or more drivers are invalid");
            return false;
         }
         // System.out.println("driver fine");
      }
      for (Property prop : myProperties) {
         if (prop == null) {
            // System.out.println("not ready: null prop in myProperties");
            return false;
         }
      }
      for (AddEquationPane eqPane : eqList) {
         if (eqPane.getEqText() == "") {
            // System.out.println("not ready: eq's incomplete");
            return false;
         }
      }
      // System.out.println("All Set!");
      return true;
   }

   //
   // goes through all drivers and see if the current string
   //
   protected void revalidateDrivers (String currentExpr) {
      // System.out.println("Revalidating Drivers... "+ currentExpr);
      // we need to go through the drivers and invalidate any that depends on
      // this variable
      for (NumericProbeDriver driver : myDrivers) {
         if (driver.usesVariable (currentExpr)) {
            driver.setInvalid();
            System.out.println ("invalidated driver with expr ["
            + driver.getExpression() + "]");
            int index = myDrivers.indexOf (driver);
            eqList.get (index).setEqText ("");
            System.out.println ("cleared equation box #" + index);
         }
      }
   }

   /**
    * returns the full string path of a property by going up the hierachy and
    * finding all its parent properties (if any) and component path.
    */
   protected String getFullPropPath (Property prop) {
      String compPath = prop.getName();

      ModelComponent root = myMain.getRootModel();
      Object host = prop.getHost();
      // System.out.println("Looking for parent path...");
      if (host instanceof CompositeProperty) {
         while (host instanceof CompositeProperty) {
            String name =
               ((CompositeProperty)host).getPropertyInfo().getName();
            // System.out.println(name);
            compPath = name + "." + compPath;
            host = ((CompositeProperty)host).getPropertyHost();
         }
         String baseComp =
            ComponentUtils.getPathName (
               (CompositeComponent)root, (ModelComponent)host);
         compPath =
            baseComp + ComponentUtils.componentPropertySeparator + compPath;
      }
      else {
         compPath =
            ComponentUtils.getPathName (
               (CompositeComponent)root, (ModelComponent)prop.getHost());
         compPath +=
            ComponentUtils.componentPropertySeparator + prop.getName();

      }
      // System.out.println("found component base path: "+compPath);
      return compPath;
   }

   @Override
   public void dispose() {
      for (AddPropertyPane pane : propList) {
         pane.dispose();
      }
      super.dispose();
   }

   protected static Color getBuildComponentColor (
      Component comp, boolean complete, boolean highlighted) {
      Color color = (complete ? activeColor : inactiveColor);
      if (highlighted) {
         if (color != null) {
            color = color.darker();
         }
         else if (comp.getParent() != null) {
            color = comp.getParent().getBackground().darker();
         }
         else {
            color = comp.getBackground().darker();
         }
      }
      return color;
   }

   protected boolean attachedFileValid (String path, StringHolder errMsg) {
      if (path != null && !path.equals ("")) {
         File file = Probe.getFileFromPath (path); 
         if (!file.canRead()) {
            if (errMsg != null) {
               errMsg.value = "File does not exist or is not readable";
            }
            return false;
         }
         else if (file.isDirectory()) {
            if (errMsg != null) {
               errMsg.value = "The provided file cannot be a directory";
            }
            return false;
         }
      }
      return true;
   }

  
}
