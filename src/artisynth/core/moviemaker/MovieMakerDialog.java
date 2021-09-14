/**
 * Copyright (c) 2014, by the Authors: Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.moviemaker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.*;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.*;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import maspack.render.GL.FrameBufferObject;
import maspack.util.IntegerInterval;
import maspack.util.InternalErrorException;
import maspack.widgets.DoubleField;
import maspack.widgets.FileNameField;
import maspack.widgets.GuiUtils;
import maspack.widgets.IntegerField;
import maspack.widgets.LabeledComponentPanel;
import maspack.widgets.OptionPanel;
import maspack.widgets.StringField;
import maspack.widgets.StringSelector;
import maspack.widgets.EnumSelector;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.util.FolderFileFilter;
import maspack.properties.Property;
import maspack.render.Viewer;
import artisynth.core.driver.Main;
import artisynth.core.driver.MainFrame;
import artisynth.core.driver.Scheduler;
import artisynth.core.driver.ViewerManager;
import artisynth.core.modelbase.HasAudio;
import artisynth.core.moviemaker.MovieMaker.MethodInfo;
import artisynth.core.moviemaker.MovieMaker.Method;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ConvertRawToWav;
import artisynth.core.gui.widgets.ProgressFrame;
import artisynth.core.workspace.RootModel;

public class MovieMakerDialog extends JDialog
   implements ActionListener, ValueChangeListener {

   private static final long serialVersionUID = 1L;
   public static final String VIEW_WINDOW_CMD = "view window";
   public static final String FULL_WINDOW_CMD = "full window";
   public static final String CUSTOM_WINDOW_CMD = "custom window";
   public static final String CUSTOMIZE_CMD = "customize";
   public static final String START_CMD = "Start";
   public static final String STOP_CMD = "Stop";
   public static final String CLOSE_CMD = "Close";
   public static final String WAYPOINT_CMD = "Waypoint_Capture";
   public static final String FRAME_CMD = "Frame";
   public static final String REC_AUDIO_CMD = "Record audio";
   public static final String REC_AUDIO_TO_TXT_CMD = "Record audio to text";

   private Main myMain;
   private MainFrame myFrame;
   private MovieMaker myMovieMaker;

   private LabeledComponentPanel optionsPanel;

   private StringField myNameField;
   private DoubleField stopTime;
   private OptionPanel windowButtons;
   private JButton fullWindowButton;
   private JButton viewWindowButton;
   private JButton customWindowButton;

   private IntegerField windowXPosition;
   private IntegerField windowYPosition;
   private IntegerField windowWidth;
   private IntegerField windowHeight;
   private IntegerField resizeWidth;
   private IntegerField resizeHeight;
   private IntegerField resizeSamples;
   private FileNameField movieDirField;
   
   private JCheckBox showCaptureFrame;

   private DoubleField frameRateField;
   private DoubleField speedField;

   private StringSelector formatSelector;
   private EnumSelector methodSelector;
   private JButton customizeButton;

   private JCheckBox beginRecordOnStart;
   private JCheckBox endRecordOnStop;
   private JCheckBox automaticFrames;
   private JCheckBox removeImages;
   private JCheckBox saveMovieImage;
   private JCheckBox recordAudio;
   private JCheckBox normalizeAudio;
   private JCheckBox recordAudioTxt;
   private JCheckBox originalSize;
   private JCheckBox constrainSize;
   private JCheckBox alwaysOnTop;

   private boolean audioOptionsAdded = false;

   private OptionPanel controlPanel;
   private JButton startButton;
   private JButton stopButton;
   private JButton frameButton;

   private TransparentDialog areaSelectionFrame;   

   private String tmpFolder;
   private double savedFrameRate;

   private CaptureMode myCaptureMode = null;

   private enum CaptureMode {
      VIEWER,
      WINDOW,
      CUSTOM
   };
      

   private boolean areaHandlerMasked = false;
   private boolean isRecordingStarted = false;
   private boolean myStopRequested = false;

   private JTextArea messageArea;
   private JFrame myProgressFrame;   

   /**
    * Output stream that writes to the message area
    */
   class MessageOutputStream extends OutputStream {
      /**
       * TextAreaOutputStream which writes to the output text area.
       * 
       * Courtesy of EF5 at Stackoverflow
       */
      public void write (int b) throws IOException {
         messageArea.append (String.valueOf ((char)b));
         messageArea.setCaretPosition(messageArea.getDocument().getLength());
      }
      
      public void write (char[] cbuf, int off, int len) throws IOException {
         messageArea.append (new String(cbuf, off, len));
         messageArea.setCaretPosition (messageArea.getDocument().getLength());
      }
   }

   /**
    * Clear the message area
    */
   public void clearMessages () {
      messageArea.setText ("");
   } 

   /**
    * Print to the messages text area, followed by a newline
    */
   public void println (String msg) {
      messageArea.append (msg + "\n");
      messageArea.setCaretPosition(messageArea.getDocument().getLength());
   }

   /**
    * Print to the messages text area
    */
   public void print (String msg) {
      messageArea.append (msg);
      messageArea.setCaretPosition(messageArea.getDocument().getLength());
   }

   PrintStream createMessageStream() {
      return new PrintStream (new MessageOutputStream());
   }

   /**
    * Create a dialog box to set the movie making options.
    */
   public MovieMakerDialog (MovieMaker movieMaker, Main main) {
      super();

      myMain = main;
      myFrame = myMain.getMainFrame();
      myMovieMaker = movieMaker;

      // Initialize the contents of the movie dialog
      JPanel contentPane = new JPanel();
      contentPane.setLayout (new BorderLayout());
      setContentPane (contentPane);
      setTitle ("Movie Maker");

      JTabbedPane tabbedPane = new JTabbedPane();

      // Recorder tab

      Box recOptionsBox = Box.createVerticalBox();
      LabeledComponentPanel captureOptions = new LabeledComponentPanel();
      captureOptions.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(
            EtchedBorder.LOWERED), "Region To Capture"));  
      LabeledComponentPanel recOptions = new LabeledComponentPanel();
      recOptions.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(
            EtchedBorder.LOWERED), "Record Options"));

      windowButtons = new OptionPanel ("Viewer Window Custom", this);
      viewWindowButton = windowButtons.getButton ("Viewer");
      viewWindowButton.setActionCommand (VIEW_WINDOW_CMD);
      viewWindowButton.setToolTipText (
         "Set capture area to main ArtiSynth viewer");
      GuiUtils.setFixedSize (
         viewWindowButton, viewWindowButton.getPreferredSize());
      viewWindowButton.setBorder (new BevelBorder (BevelBorder.LOWERED));

      fullWindowButton = windowButtons.getButton ("Window");
      fullWindowButton.setToolTipText (
         "Set capture area to entire ArtiSynth frame");
      fullWindowButton.setActionCommand (FULL_WINDOW_CMD);
      GuiUtils.setFixedSize (
         fullWindowButton, fullWindowButton.getPreferredSize());
      fullWindowButton.setBorder (new BevelBorder (BevelBorder.RAISED));

      customWindowButton = windowButtons.getButton ("Custom");
      customWindowButton.setToolTipText (
         "Set capture area to custom area");
      customWindowButton.setActionCommand (CUSTOM_WINDOW_CMD);
      GuiUtils.setFixedSize (
         customWindowButton, customWindowButton.getPreferredSize());
      customWindowButton.setBorder (new BevelBorder (BevelBorder.RAISED));
      captureOptions.addWidget (windowButtons);

      windowWidth = new IntegerField ("Width", 0);
      windowWidth.addValueChangeListener (this);
      captureOptions.addWidget (windowWidth);
      windowHeight = new IntegerField ("Height", 0);
      windowHeight.addValueChangeListener (this);
      captureOptions.addWidget (windowHeight);
      windowXPosition = new IntegerField ("Left", 0);
      windowXPosition.addValueChangeListener (this);
      captureOptions.addWidget (windowXPosition);
      windowYPosition = new IntegerField ("Top", 0);
      windowYPosition.addValueChangeListener (this);
      captureOptions.addWidget (windowYPosition);

      showCaptureFrame = new JCheckBox ("Show capture frame", null, true);
      showCaptureFrame.addActionListener (this);
      showCaptureFrame.setEnabled (false);
      captureOptions.addWidget (showCaptureFrame);

      recOptionsBox.add(captureOptions);

      beginRecordOnStart = new JCheckBox ("Begin playing on start", null, true);
      recOptions.addWidget (beginRecordOnStart);
      endRecordOnStop = new JCheckBox ("End playing on stop", null, true);
      recOptions.addWidget (endRecordOnStop);
      automaticFrames = new JCheckBox("Automatic frame capture", null, true);
      automaticFrames.addChangeListener(
         new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
               JCheckBox box = (JCheckBox)e.getSource();
               if (isStarted()) {
                  if (box.isSelected()) {
                     myMovieMaker.setGrabbing(true);
                  } else {
                     myMovieMaker.setGrabbing(false);
                  }
               }
            }
         }
         
         );
      recOptions.addWidget(automaticFrames);
      removeImages = new JCheckBox ("Remove temporary files", null, true);
      recOptions.addWidget (removeImages);
      saveMovieImage = new JCheckBox ("Save first frame image", null, true);
      recOptions.addWidget (saveMovieImage);
      
      alwaysOnTop = new JCheckBox (
         "Window always on top", null, myMovieMaker.isAlwaysOnTop ());
      recOptions.add (alwaysOnTop);
      alwaysOnTop.addActionListener (this);
      
      recordAudio = new JCheckBox (REC_AUDIO_CMD, null, false);
      recordAudio.addActionListener (this);
      normalizeAudio = new JCheckBox ("Normalize audio", null, false);
      recordAudioTxt = new JCheckBox (REC_AUDIO_TO_TXT_CMD, null, false);
      recordAudioTxt.addActionListener (this);

      setAudioOptions (myMain.getRootModel() instanceof HasAudio);

      recOptionsBox.add (recOptions);  

      LabeledComponentPanel miscOptions = new LabeledComponentPanel();
      miscOptions.setBorder (
         BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

      myNameField = new StringField ("Movie name", "artisynth", 10);
      myNameField.setStretchable (true);
      myNameField.setBorder (BorderFactory.createEmptyBorder (5, 0, 5, 0));
      miscOptions.addWidget (myNameField);

      stopTime = new DoubleField ("Stop time");
      stopTime.setGUIVoidEnabled (true);
      stopTime.setVoidValueEnabled (true);
      stopTime.setValue (Property.VoidValue);
      miscOptions.addWidget (stopTime);

      recOptionsBox.add(miscOptions);

      // Encoder tab

      Box encOptionsBox = Box.createVerticalBox();
      LabeledComponentPanel outputOptions = new LabeledComponentPanel();
      outputOptions.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(
            EtchedBorder.LOWERED), "Encoding Options"));  
      LabeledComponentPanel sizeOptions = new LabeledComponentPanel();
      sizeOptions.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Output Size"));

      frameRateField = new DoubleField (
         "Frame rate", myMovieMaker.getFrameRate());
      frameRateField.addValueChangeListener (this);
      frameRateField.setRange (1, Integer.MAX_VALUE);
      outputOptions.addWidget (frameRateField);

      speedField = new DoubleField ("Speed", myMovieMaker.getSpeed());
      speedField.addValueChangeListener (this);
      speedField.setRange (1e-6, Double.POSITIVE_INFINITY);
      outputOptions.addWidget (speedField);

      formatSelector = new StringSelector ( 
         "Frame file", ImageIO.getWriterFormatNames());
      String imageFmt = movieMaker.getFormat ();
      if (imageFmt == null) {
         formatSelector.setValue ("png");
      } else {
         formatSelector.setValue (imageFmt);
      }
      formatSelector.addValueChangeListener (this);
      GuiUtils.setFixedSize (
         formatSelector.getComboBox(), new Dimension (125, 25));
      outputOptions.addWidget (formatSelector);

      methodSelector = new EnumSelector (
         "Method", movieMaker.getMethod(), Method.values());
      methodSelector.addValueChangeListener (this);
      GuiUtils.setFixedSize (
         methodSelector.getComboBox(), new Dimension (125, 25));

      customizeButton = new JButton ("Customize Method");
      customizeButton.setActionCommand (CUSTOMIZE_CMD);
      customizeButton.addActionListener (this);
      customizeButton.setMargin (new Insets (3, 3, 3, 3));
      GuiUtils.setFixedSize (
         customizeButton, customizeButton.getPreferredSize());
      outputOptions.addWidget (methodSelector);
      outputOptions.addWidget (customizeButton);

      encOptionsBox.add (outputOptions);

      originalSize = new JCheckBox ("Same as original", true);
      originalSize.addActionListener (this);
      sizeOptions.addWidget (originalSize);
      constrainSize = new JCheckBox ("Constrain proportions", true);
      constrainSize.addActionListener (this);
      constrainSize.setEnabled (false);
      sizeOptions.addWidget (constrainSize);

      resizeWidth = new IntegerField ("Width", 0);
      resizeWidth.getTextField().setEnabled (false);
      resizeWidth.addValueChangeListener (new ValueChangeListener() {
         public void valueChange (ValueChangeEvent e) {
            if (constrainSize.isSelected()) {
               double scale = 
                  resizeWidth.getDoubleValue() / windowWidth.getDoubleValue();
               resizeHeight.setValue (scale * windowHeight.getIntValue());
            }
            setMovieMakerCaptureAreaFromResize();
         }
      });
      sizeOptions.addWidget (resizeWidth);
      resizeHeight = new IntegerField ("Height", 0);
      resizeHeight.getTextField().setEnabled (false);
      resizeHeight.addValueChangeListener (new ValueChangeListener() {
         public void valueChange (ValueChangeEvent e) {
            if (constrainSize.isSelected()) {
               double scale = 
                  resizeHeight.getDoubleValue() / windowHeight.getDoubleValue();
               resizeWidth.setValue (scale * windowWidth.getIntValue());

            }
            setMovieMakerCaptureAreaFromResize();
         }
      });
      sizeOptions.addWidget (resizeHeight);
      int nDefaultSamples = FrameBufferObject.defaultMultiSamples;
      
      resizeSamples = new IntegerField("# samples", nDefaultSamples);
      resizeSamples.setToolTipText(
         "Number of samples to use for anti-aliasing. \n" + 
            "If output images are black, set this to 1 \n" + 
         "(may happen if graphics card has limited  \nOpenGL support)." );
      resizeSamples.getTextField().setEnabled(false);
      resizeSamples.setRange(new IntegerInterval(1, 16));
      resizeSamples.addValueChangeListener(this);
      sizeOptions.addWidget(resizeSamples);

      encOptionsBox.add (sizeOptions);

      // Advanced tab

      Box extraBox = Box.createVerticalBox();
      
      LabeledComponentPanel advancedOptions = new LabeledComponentPanel ();
      advancedOptions.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Advanced"));  
      advancedOptions.setName("advanced");
      
      movieDirField =
         new FileNameField (
            "Movie folder", myMovieMaker.getMovieFolder(), 10);
      JFileChooser chooser = movieDirField.getFileChooser();
      chooser.setFileSelectionMode (JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setAcceptAllFileFilterUsed (false);
      FolderFileFilter filter = new FolderFileFilter("Folders");
      chooser.addChoosableFileFilter (filter);
      chooser.setFileFilter (filter);
      // bit of a hack here to make chooser display current file properly
      chooser.setSelectedFile (new File(myMovieMaker.getMovieFolder().getName()));

      movieDirField.addValueChangeListener (this);

      advancedOptions.add (movieDirField);
      extraBox.add (advancedOptions);

      LabeledComponentPanel extraCommands = new LabeledComponentPanel();
      extraCommands.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Commands"));  
      extraCommands.setName("Extra commands");

      JButton waypointButton = new JButton("Movie from WayPoints");
      waypointButton.setActionCommand(WAYPOINT_CMD);
      waypointButton.addActionListener(this);
      extraCommands.addWidget(waypointButton);
      extraBox.add(extraCommands);

      // Messages tab

      Box messagesBox = Box.createVerticalBox();
      messageArea = new JTextArea(20, 80);
      messageArea.setMargin (new Insets(5,5,5,5));
      messageArea.setEditable(false);
      messageArea.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
      messagesBox.add (new JScrollPane(messageArea));

      // put everthing together in a tabbed pane

      tabbedPane.addTab ("Recorder", recOptionsBox);
      tabbedPane.addTab ("Encoder", encOptionsBox);
      tabbedPane.addTab ("Advanced", extraBox);
      tabbedPane.addTab ("Messages", messagesBox);

      if (myMain.getModelName() != null) {
         setMovieName (myMain.getModelName());
      }

      add (tabbedPane, BorderLayout.NORTH);

      updateMethodSelectors();

      controlPanel = new OptionPanel ("Screenshot " + 
         START_CMD + " " + FRAME_CMD + " " + STOP_CMD + " " + CLOSE_CMD, this);
      startButton = controlPanel.getButton (START_CMD);
      stopButton = controlPanel.getButton (STOP_CMD);
      stopButton.setEnabled (false);

      frameButton = controlPanel.getButton(FRAME_CMD);
      frameButton.setEnabled(false);

      add (controlPanel, BorderLayout.CENTER);

      setCaptureMode (CaptureMode.VIEWER);

      //final Component displayComponent = myFrame.getViewer().getComponent();
      updateCaptureArea ();

      myFrame.addComponentListener(
         new ComponentListener() {
            public void componentHidden (ComponentEvent c_evt) {}

            public void componentMoved (ComponentEvent c_evt) {
               if (myCaptureMode != CaptureMode.CUSTOM) {
                  updateCaptureArea ();
               }
            }

            public void componentResized (ComponentEvent c_evt) {
               System.out.println ("resize");
               if (myCaptureMode != CaptureMode.CUSTOM) {
                  updateCaptureArea ();               
               }
            }

            public void componentShown (ComponentEvent c_evt) {}
         });

      pack();
      setMinimumSize (getPreferredSize());
   }

   /**
    * Strips out white space and file separators before setting file name.
    */
   public void setMovieName (String name) {
      name = name.replaceAll ("\\s", "");
      name = name.replaceAll ("\\" + File.separator, ""); 
      // if name is a file name, remove trailing prefex
      int lastDot = name.lastIndexOf ('.');
      if (lastDot != -1) {
         name = name.substring (0, lastDot);
      }
      myNameField.setValue (name);
   }

   private void updateMethodSelectors() { 

      Method method = (Method) methodSelector.getValue();
      if (method == Method.INTERNAL) {

         MethodInfo methodInfo = myMovieMaker.getMethodInfo(method);
         if (myMovieMaker.getFormat() != methodInfo.frameFileFormat) {
            myMovieMaker.setFormat (methodInfo.frameFileFormat);
            formatSelector.setValue (methodInfo.frameFileFormat);
         }

         customizeButton.setEnabled (false);
         formatSelector.setEnabledAll (false);
      }
      else {
         customizeButton.setEnabled (true);
         formatSelector.setEnabledAll (true);
      }
   }

   private void setCaptureMode (CaptureMode mode) {
      if (myCaptureMode != mode) {
         if (mode == CaptureMode.CUSTOM) {
            windowXPosition.setEnabledAll (true);
            windowYPosition.setEnabledAll (true);
         }
         else {
            windowXPosition.setEnabledAll (false);
            windowYPosition.setEnabledAll (false);
         }
         myCaptureMode = mode;
      }
   }

   private Rectangle updateCaptureWindowWidgets() {

      Dimension size;
      Component locComp; // component used to find location

      switch (myCaptureMode) {
         case VIEWER: {
            Viewer v = myMain.getViewer();
            size = new Dimension (v.getScreenWidth(), v.getScreenHeight());
            locComp = v.getComponent();
            break;
         }
         case WINDOW: {
            size = myMain.getMainFrame().getSize();
            locComp = myMain.getMainFrame();
            break;
         }
         case CUSTOM:{
            size = areaSelectionFrame.background.getSize();
            locComp = areaSelectionFrame.background;
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented capture mode " + myCaptureMode);
         }
      }
      boolean widthChanged = (size.width != windowWidth.getIntValue());
      areaHandlerMasked = true;      
      windowWidth.setValue (size.width);
      windowHeight.setValue (size.height);
      Point loc = locComp.getLocationOnScreen();
      windowXPosition.setValue (loc.x);
      windowYPosition.setValue (loc.y);
      areaHandlerMasked = false;

      if (myCaptureMode == CaptureMode.VIEWER) {
         if (!originalSize.isSelected()) {
            if (constrainSize.isSelected()) {
               // resize width/height
               if (widthChanged) {
                  double scale = 
                     resizeHeight.getDoubleValue()/windowHeight.getDoubleValue();
                  resizeWidth.setValue (scale*windowWidth.getIntValue());
               }
               else {
                  double scale = 
                     resizeWidth.getDoubleValue()/windowWidth.getDoubleValue();
                  resizeHeight.setValue (scale*windowHeight.getIntValue());
               }
            }
         }
      }
      if (constrainSize.isSelected() && originalSize.isSelected()) {
         resizeWidth.setValue(size.width);
         resizeHeight.setValue(size.height);
      }
      return new Rectangle (loc.x, loc.y, size.width, size.height);
   }

   private void updateCaptureArea () {

      Rectangle area = updateCaptureWindowWidgets();

      Dimension viewerDim = null;
      if (myCaptureMode == CaptureMode.VIEWER) {
         if (!originalSize.isSelected()) {
            viewerDim = new Dimension (
               resizeWidth.getIntValue(), resizeHeight.getIntValue());
         }
         else {
            viewerDim = new Dimension (area.width, area.height);
         }
      }
      myMovieMaker.setCaptureArea (area, viewerDim);

      if (areaSelectionFrame != null && areaSelectionFrame.isVisible()) {
         areaSelectionFrame.setBoundsFromCaptureArea (area);
      }
   }

   private void showAreaSelectionFrame (boolean show) {
      if (show) {
         if (areaSelectionFrame == null) {
            TransparentDialog frame = new TransparentDialog (this);
            frame.pack();
            frame.setVisible (true);
            System.out.println ("bounmds=" + myMovieMaker.getCaptureArea());
            frame.setBoundsFromCaptureArea (
               myMovieMaker.getCaptureArea());
            areaSelectionFrame = frame;
         }
         else {
            areaSelectionFrame.updateBackground();
            areaSelectionFrame.setVisible (true);
         }
      }
      else {
         if (areaSelectionFrame != null) {
            areaSelectionFrame.setVisible (false);
         }
      }
   }

   public void setAudioOptions (boolean include) {
      if (include != audioOptionsAdded) {
         if (include) {
            optionsPanel.addWidget (recordAudio);
            optionsPanel.addWidget (recordAudioTxt);
            optionsPanel.addWidget (normalizeAudio);
            normalizeAudio.setEnabled (
               recordAudio.isSelected() || recordAudioTxt.isSelected());
         }
         else {
            optionsPanel.removeWidget (recordAudio);
            optionsPanel.removeWidget (recordAudioTxt);
            optionsPanel.removeWidget (normalizeAudio);
         }

         pack();
         audioOptionsAdded = include;
      }
   }

   public void updateForNewRootModel (
      String modelName, RootModel rootModel) {

      setAudioOptions (rootModel instanceof HasAudio);
      setMovieName (modelName);
      pack();
   }

   /**
    * Listens for value change events from the methodSelector and
    * the formatSelector.
    */
   public void valueChange (ValueChangeEvent event) { 
      Object source = event.getSource();

      if (source == methodSelector) {
         Method method = (Method)methodSelector.getValue();

         if (method != myMovieMaker.getMethod()) {
            MovieMaker.MethodInfo info = myMovieMaker.getMethodInfo (method);
            myMovieMaker.setMethod (method);

            if (!info.frameFileFormat.equals (myMovieMaker.getFormat())) {
               myMovieMaker.setFormat (info.frameFileFormat);
               formatSelector.setValue (info.frameFileFormat);
            }
         }
      }
      else if (source == formatSelector) {
         String format = (String) formatSelector.getValue();

         if (!format.equals (myMovieMaker.getFormat())) {
            myMovieMaker.setFormat (format);
            Method method = (Method) methodSelector.getValue();
            MovieMaker.MethodInfo methodInfo =
               myMovieMaker.getMethodInfo (method);
            methodInfo.frameFileFormat = format;
         }
      }
      else if (source == frameRateField) {
         myMovieMaker.setFrameRate (frameRateField.getDoubleValue()); 
      }
      else if (source == speedField) {
         myMovieMaker.setSpeed (speedField.getDoubleValue()); 
      }
      else if (source == resizeSamples) {
         myMovieMaker.setAntiAliasingSamples(resizeSamples.getIntValue());
      }
      else if (source == movieDirField) {
         File dir = new File ((String)movieDirField.getValue());
         if (validateFolder (this, dir)) {
            myMovieMaker.setMovieFolder (dir);
         }
         else {
            movieDirField.setValue (myMovieMaker.getMovieFolderPath());
         }
      }
      else if (source == windowWidth || source == windowHeight ||
               source == windowXPosition || source == windowYPosition) {
         if (!areaHandlerMasked) {
            int w = windowWidth.getIntValue();
            int h = windowHeight.getIntValue();
            switch (myCaptureMode) {
               case CUSTOM: {
                  int x = windowXPosition.getIntValue();
                  int y = windowYPosition.getIntValue();
                  Rectangle oldArea = myMovieMaker.getCaptureArea();
                  Rectangle newArea = new Rectangle (x, y, w, h);               
                  if (!oldArea.equals (newArea)) {
                     myMovieMaker.setCaptureArea (newArea, null);
                     if (areaSelectionFrame != null && 
                         areaSelectionFrame.isVisible()) { 
                        areaSelectionFrame.setBoundsFromCaptureArea (newArea);
                     }
                  }
                  break;
               }
               case VIEWER: {
                  Dimension size = myMain.getViewerSize();
                  if (size.width != w || size.height != h) {
                     myMain.setViewerSize (w, h);
                  }
                  break;
               }
               case WINDOW: {
                  Dimension size = myMain.getMainFrame().getSize();
                  if (size.width != w || size.height != h) {
                     myMain.getMainFrame().setSize (w, h);
                  }
                  break;
               }
               default: {
                  throw new InternalErrorException (
                     "Unimplemented capture mode "+myCaptureMode);
               }
            }
         }
      }
      else {
         throw new InternalErrorException (
            "valueChange event from unknown source " + source);
      }

      updateMethodSelectors();
   }

   public static boolean validateFolder (Component comp, File dir) {
      if (dir.isDirectory()) {
         return true;
      }
      else if (!dir.exists()) {
         boolean valid = false;
         boolean confirm = GuiUtils.confirmAction (
            comp, ""+dir+" does not exist. Create?");
         if (confirm) {
            String errMsg = null;
            try {
               valid = dir.mkdirs();
            }
            catch (Exception e) {
               errMsg = e.getMessage();
               valid = false;
            }
            if (!valid) {
               GuiUtils.showError (
                  comp, "Can't create "+dir+(errMsg != null ? ": "+errMsg : ""));
            }
         }
         return valid;
      }
      else {
         GuiUtils.showError (comp, ""+dir+" not a folder");
         return false;
      }
   }

   private void waypointCapture() {

      // disable play (otherwise destroys waypoints)
      boolean savedBegin = beginRecordOnStart.isEnabled();
      beginRecordOnStart.setEnabled(false);
      beginRecordOnStart.setSelected (false);

      // manual control of scheduler and rendering
      Scheduler scheduler = myMain.getScheduler();
      ViewerManager manager = myMain.getViewerManager();

      try {

         // reset to beginning
         scheduler.reset();
         manager.render();
         manager.repaint();

         // starts recording
         if (startMovie()) {
            myMovieMaker.setGrabbing (false);   // we're manually doing this

            myMovieMaker.resetFrameCounter();
            myMovieMaker.forceGrab();

            while (scheduler.fastForward()) {
               manager.render();
               manager.repaint();
               myMovieMaker.forceGrab();
            }
            stopMovie();
         }
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         println ("Failed to create movie");
      }

      beginRecordOnStart.setEnabled(savedBegin);
      beginRecordOnStart.setSelected (savedBegin);
   }

   private boolean needEvenCaptureArea() {
      Method method = myMovieMaker.getMethod();
      return (method == Method.AVCONV ||
              method == Method.FFMPEG || 
              method == Method.MENCODER);
   }

   private boolean captureAreaIsEven() {
      Rectangle dim = myMovieMaker.getCaptureArea();
      return (dim.width%2 == 0 && dim.height%2 == 0);
   }

   private boolean startMovie() {
      String movieFileName = myNameField.getText();
      myMovieMaker.setRenderingAudioToFile (recordAudio.isSelected());
      myMovieMaker.setRenderingAudioToText (recordAudioTxt.isSelected());
      myMovieMaker.setAudioNormalized (normalizeAudio.isSelected());

      clearMessages();

      MethodInfo methodInfo =
         myMovieMaker.getMethodInfo (myMovieMaker.getMethod());

      if (movieFileName.equals ("")) {
         GuiUtils.showError (this, "Please enter a movie name");
         myNameField.requestFocusInWindow();
         return false;
      }
      else if ("".equals(methodInfo.command)) {
         GuiUtils.showError (
            this,
            "Command for CUSTOM method is empty; " +
            "use 'Customize Method' to set it");
         return false;
      }
      else if (needEvenCaptureArea() && !captureAreaIsEven()) {
         GuiUtils.showError (
            this,
            "AVCONV, FFMPEG and MENCODER methods require the height and width "+
            "of the video capture are to be even");
         return false;
      }
      else {
         stopButton.setEnabled (true);
         startButton.setEnabled (false);
         frameButton.setEnabled(true);

         //  prevent resizing from affecting video
         // XXX: removed, since prevents zoom in orthographic projection
         // Main.getMain().getViewer().setResizeEnabled(false);   

         // Right now we expect the models to either produce a RAW audio
         // file, or a text file. The RAW file should be merged with the
         // movie, while the text is left alone as is.
         if (myMain.getRootModel() instanceof HasAudio) {
            if (recordAudio.isSelected()) { // Render a raw audio file
               myMovieMaker.setAudioFileName (
                  myMovieMaker.getMovieFolderPath() + "/aud.raw");
            }

            if (recordAudioTxt.isSelected()) { // Render a text file
               println ("Rendering audio to text...");
               myMovieMaker.setAudioFileName (
                  ArtisynthPath.getHomeDir() +
                  "/" + myNameField.getText() + ".txt");
            }

            boolean renderToFile =
               recordAudio.isSelected() || recordAudioTxt.isSelected();
            ((HasAudio) myMain.getRootModel()).setRenderAudioToFile (
               renderToFile);
         }
         
         println (myMovieMaker.getCaptureArea().toString());
         
         if (automaticFrames.isSelected()) {
            myMovieMaker.setGrabbing (true);
         }
         savedFrameRate = myMain.getFrameRate();
         myMain.setFrameRate (
            myMovieMaker.getFrameRate() / myMovieMaker.getSpeed());

         myFrame.setAlwaysOnTop (myMovieMaker.isAlwaysOnTop ());
         showAreaSelectionFrame (false);
         showCaptureFrame.setSelected (false);

         try {
            // Start playing when start button is clicked 
            // if not already playing
            if (myMain.getScheduler().isPlaying() == false &&
               beginRecordOnStart.isSelected()) {
               //myMain.getScheduler().playRequest();
               //myFrame.getMenuBarHandler().disableShowPlay();
               myMain.getScheduler().play();
            }
            isRecordingStarted = true;
         }
         catch (Exception e) {
            println ("ERROR: Can't create movie writer: " + e);
            myMain.setFrameRate (savedFrameRate);
            myMovieMaker.setGrabbing (false);

            myFrame.setAlwaysOnTop (false);
            return false;
         }
         return true;
      }
   }
   
   public boolean isStarted() {
      return isRecordingStarted;
   }

   /**
    * Called when movie making is stopped. Responsible for creating the movie
    * from the grabbed frames. This is spun off as a task because the movie
    * creation process can take a while and we don't want to freeze up the GUI.
    */
   class StopTask extends SwingWorker<Void, Void> {

      int myFrameCount = 0;
      boolean mySuccess = false;

      public boolean createMovieFromFrames() throws Exception {

         boolean success = false;
         String movieFileName = myNameField.getText();
         File mfile = new File(myMovieMaker.getMovieFolder(), movieFileName); 
         
         // make sure directories exist
         mfile.getParentFile ().mkdirs ();
         myFrame.getViewer().cleanupScreenShots();

         myMain.setFrameRate (savedFrameRate);
         myMovieMaker.setGrabbing (false);
         myFrame.getViewer().cleanupScreenShots();

         stopButton.setEnabled (false);
         frameButton.setEnabled (false);
         
         myFrame.setAlwaysOnTop (false);

         // Make the movie out of the frames
         println("Stopping movie...");

         myFrameCount = myMovieMaker.close();
         if (myFrameCount == 0) {
            return false;
         }
         // No merging required if outputting silent movie, or writing
         // audio to text file
         if (!myMovieMaker.isRenderingAudioToFile()) {
            success = myMovieMaker.render (movieFileName);
         }
         else {
            ((HasAudio) myMain.getRootModel()).onStop();
            String tmpMovieFn = 
               "file:" + myMovieMaker.getMovieFolderPath() + "/noAudio.mov";
            String finalMovieFn =
               "file: "+ArtisynthPath.getHomeDir()+"/" + movieFileName;
               
            println("Rendering frames...");
            // Render the movie's visual frames only
            success = myMovieMaker.render (tmpMovieFn);
               
            //convert raw audio to wav
            String waveFn = "file:" + 
               ConvertRawToWav.convert (
                  myMovieMaker.getAudioSampleRate(), 
                  myMovieMaker.getAudioFileName());
            String[] args = {tmpMovieFn, 
                             waveFn, 
                             "-o",
                             finalMovieFn};
            println ("final file name:" + finalMovieFn);
            new Merge (args);
            //TODO: delete tmp files!
         }
         if (saveMovieImage.isSelected()) {
            myMovieMaker.saveFirstFrame (movieFileName);
         }
         if (removeImages.isSelected()) {
            println ("Removing image files");
            myMovieMaker.clean();
         }
         myStopRequested = false;
         return success;
      }

      /*
       * Main task. Executed in background thread.
       */
      @Override
      public Void doInBackground() {
         try {
            mySuccess = createMovieFromFrames();
         }
         catch (Exception e) {
            e.printStackTrace (createMessageStream());
            mySuccess = false;
         }
         // change the audio source back to outputting to sound card
         if ((myMain.getRootModel() instanceof HasAudio) &&
             ((recordAudio.isSelected()) || recordAudioTxt.isSelected()) ) {
            println("Going back to soundcard output...");

            myMovieMaker.setRenderingAudioToFile (false);
            myMovieMaker.setRenderingAudioToText (false);

            recordAudio.setSelected (false);
            recordAudioTxt.setSelected (false);

            normalizeAudio.setSelected (false);
            normalizeAudio.setEnabled (false);

            ((HasAudio) myMain.getRootModel()).setRenderAudioToFile (false);
         }
         return null;
      }

      /*
       * Executed in event dispatching thread
       */
      @Override
      public void done() {    

         myProgressFrame.dispose();
         if (myFrameCount == 0) {
            println ("\nNo frames grabbed; movie not created"); 
            GuiUtils.showError (
               MovieMakerDialog.this, "No frames grabbed; movie not created");
         }
         else if (!mySuccess) {
            println ("\nMovie could not be created"); 
            GuiUtils.showError (
               MovieMakerDialog.this,
               "Movie could not be created. See 'Messages' tab for details");
         }
         else {
            println ("\nMovie successfully created"); 
            GuiUtils.showNotice (
               MovieMakerDialog.this, "Movie successfully created");
         }
         startButton.setEnabled (true);
      }
   }

   private void createAndShowProgressFrame() {
       // create the window
      myProgressFrame = new ProgressFrame("Making movie");
      myProgressFrame.setVisible(true);
      GuiUtils.locateCenter (myProgressFrame, this);
   }

   protected synchronized void stopMovie() {  
      if (!myStopRequested) {
         myStopRequested = true;
         // Stop playing when stop movie maker is pressed
         if (myMain.getScheduler().isPlaying() == true &&
             endRecordOnStop.isSelected()) {
            myMain.getScheduler().stopRequest();  
         }
         doStopMovie();
      }
   }

   private void doStopMovie() {
      StopTask stopTask = new StopTask();
      createAndShowProgressFrame();
      stopTask.execute();
      isRecordingStarted = false;
   }

   public void actionPerformed (ActionEvent event) {
      String cmd = event.getActionCommand();
      Object source = event.getSource();

      if (cmd.equals (FULL_WINDOW_CMD)) {
         setCaptureMode (CaptureMode.WINDOW);

         showCaptureFrame.setEnabled (false);
         showAreaSelectionFrame (false);

         updateCaptureArea ();
         viewWindowButton.setBorder (new BevelBorder (BevelBorder.RAISED));
         fullWindowButton.setBorder (new BevelBorder (BevelBorder.LOWERED));
         customWindowButton.setBorder (new BevelBorder (BevelBorder.RAISED));

         originalSize.setEnabled (false);
         constrainSize.setEnabled (false);
         resizeWidth.getTextField().setEnabled (false);
         resizeHeight.getTextField().setEnabled (false);
         resizeSamples.getTextField().setEnabled(false);
      }
      else if (cmd.equals (VIEW_WINDOW_CMD)) {
         setCaptureMode (CaptureMode.VIEWER);

         showCaptureFrame.setEnabled (false);
         showAreaSelectionFrame (false);

         updateCaptureArea ();
         viewWindowButton.setBorder (new BevelBorder(BevelBorder.LOWERED));
         fullWindowButton.setBorder (new BevelBorder(BevelBorder.RAISED));
         customWindowButton.setBorder (new BevelBorder (BevelBorder.RAISED));

         originalSize.setEnabled (true);
         if (!originalSize.isSelected()) {
            originalSize.doClick();
         }

         constrainSize.setEnabled (false);
         resizeWidth.getTextField().setEnabled (false);
         resizeHeight.getTextField().setEnabled (false);  
         resizeSamples.getTextField().setEnabled(false);
      }
      else if (cmd.equals (CUSTOM_WINDOW_CMD)) {
         setCaptureMode (CaptureMode.CUSTOM);

         if (!showCaptureFrame.isSelected() || 
            !showCaptureFrame.isEnabled ()) {            
            showCaptureFrame.setEnabled (true);
            showCaptureFrame.setSelected (true);
         }
         showAreaSelectionFrame (true);

         viewWindowButton.setBorder (new BevelBorder(BevelBorder.RAISED));
         fullWindowButton.setBorder (new BevelBorder(BevelBorder.RAISED));
         customWindowButton.setBorder (new BevelBorder (BevelBorder.LOWERED));

         originalSize.setEnabled (false);
         constrainSize.setEnabled (false);
         resizeWidth.getTextField().setEnabled (false);
         resizeHeight.getTextField().setEnabled (false);
         resizeSamples.getTextField().setEnabled(false);
      }
      else if (cmd.equals (REC_AUDIO_CMD)) {
         if (recordAudio.isSelected()) {
            println ("rec. audio selected");
            recordAudioTxt.setSelected (false);
         }

         normalizeAudio.setEnabled (
            recordAudio.isSelected() || recordAudioTxt.isSelected());
      }
      else if (cmd.equals (REC_AUDIO_TO_TXT_CMD)) {
         if (recordAudioTxt.isSelected()) {
            println ("rec. audio txt selected");
            recordAudio.setSelected (false);
         }

         normalizeAudio.setEnabled (
            recordAudio.isSelected() || recordAudioTxt.isSelected());
      }
      else if (cmd.equals (START_CMD)) {
         startMovie();
      }
      else if (cmd.equals (STOP_CMD)) {
         stopMovie();
      }
      else if (cmd.equals (CLOSE_CMD)) {
         setVisible (false); 
      } 
      else if (cmd.equals(WAYPOINT_CMD)) {
         waypointCapture();
      }
      else if (cmd.equals(FRAME_CMD)) {
         try {
            myMovieMaker.forceGrab();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      else if (cmd.equals (CUSTOMIZE_CMD)) { 
         Method method = (Method)methodSelector.getValue();
         MovieMaker.MethodInfo methodInfo = 
            myMovieMaker.getMethodInfo (method);
         MethodDialog dialog = new MethodDialog (this, methodInfo);
         dialog.setLocationRelativeTo (this);
         dialog.setVisible (true);
      }
      else if (source == showCaptureFrame) { 
         showAreaSelectionFrame (showCaptureFrame.isSelected());
      }
      else if (source == originalSize) {
         if (originalSize.isSelected()) { 
            constrainSize.setEnabled (false);
            resizeWidth.setValue(windowWidth.getIntValue());
            resizeHeight.setValue(windowHeight.getIntValue());
            resizeWidth.getTextField().setEnabled (false);
            resizeHeight.getTextField().setEnabled (false);
            resizeSamples.getTextField().setEnabled(false);
         }
         else {
            constrainSize.setEnabled (true);
            resizeWidth.getTextField().setEnabled (true);
            resizeHeight.getTextField().setEnabled (true);
            resizeSamples.getTextField().setEnabled(true);
         }
      }
      else if (cmd.equals ("Screenshot")) {
         takeScreenshot();
      }
      else if (source == constrainSize && constrainSize.isSelected()) {
         double scale = 
            resizeWidth.getDoubleValue() / windowWidth.getDoubleValue();
         resizeHeight.setValue (scale * windowHeight.getIntValue());
         setMovieMakerCaptureAreaFromResize();
      }
      else if (source == alwaysOnTop) {
         boolean onTop = alwaysOnTop.isSelected ();
         myMovieMaker.setAlwaysOnTop (onTop);
         if (isRecordingStarted) {
            myFrame.setAlwaysOnTop (onTop);
         }
      }
   }

   private void setMovieMakerCaptureAreaFromResize() {
      Dimension grabResize = null;
      if (myCaptureMode == CaptureMode.VIEWER) {
         grabResize = new Dimension (
            (Integer) resizeWidth.getIntValue(), 
            (Integer) resizeHeight.getIntValue());
      }
      myMovieMaker.setCaptureArea (
         myMovieMaker.getCaptureArea(), grabResize);

   }

   /**
    * Updates the widgets to reflect any changes in their underlying values
    */
   void updateWidgetValues() {
      // Note: if more options are made adjustable from interfaces outside this
      // dialog, the number of widgets that require updating will increase.

      // Recoder Tab: nothing currently needed

      // Encoder Tab:
      frameRateField.setValue (myMovieMaker.getFrameRate());
      methodSelector.setValue (myMovieMaker.getMethod());
      customizeButton.setEnabled (
         myMovieMaker.getMethod() != Method.INTERNAL);

      // Advanced Tab:
      movieDirField.setValue (myMovieMaker.getMovieFolderPath());
   }

   private void takeScreenshot() {
      String movieFileName = myNameField.getText();

      if (movieFileName.equals ("")) {
         GuiUtils.showError (this, "Please enter a movie name");
         myNameField.requestFocusInWindow();
      }
      else {

         boolean stopEnabled = stopButton.isEnabled();
         boolean startEnabled = startButton.isEnabled();
         stopButton.setEnabled (false);
         startButton.setEnabled (false);

         showAreaSelectionFrame (false);
         showCaptureFrame.setSelected (false);

         try {
            myMovieMaker.grab (movieFileName );
         }
         catch (Exception e) {
            e.printStackTrace();
         }

         stopButton.setEnabled (stopEnabled);
         startButton.setEnabled (startEnabled);
      }
   }

   /**
    * A dialog box with a transparent background and a single button to select
    * the transparent area.
    *
    */
   private class TransparentDialog
      extends JDialog implements ComponentListener, KeyListener {

      private static final long serialVersionUID = 1L;
      private TransparentBackground background;
      private MovieMakerDialog parent;

      public TransparentDialog (MovieMakerDialog parent) {
         super (parent);
         this.parent = parent;

         background = new TransparentBackground();
         getContentPane().add (background);

         setTitle ("Capture area is outlined in red");
         setModal (false);

         addComponentListener (this);
         addKeyListener(this);
         addWindowListener (new WindowAdapter() {
            public void windowClosing (WindowEvent e) {
               showCaptureFrame.setSelected (false);
            }
         });

         if (myCaptureMode != CaptureMode.CUSTOM) {
            // XXX finish
            setUndecorated (true);
         }
      }

      private void setCaptureArea() {
         parent.updateCaptureArea ();
      }

      public void setBoundsFromCaptureArea (Rectangle rect) {
         // Get the current bounds and adjust 
         Point origin = background.getLocationOnScreen();
         Rectangle bounds = getBounds();
         bounds.x += (rect.x - origin.x);
         bounds.y += (rect.y - origin.y);
         bounds.width += (rect.width - background.getWidth());
         bounds.height += (rect.height - background.getHeight());
         println ("setting bounds to bounds=" + bounds);
         setBounds (bounds);
      }

      /**
       * when the dialog moves repaint the background to be the new
       * part of the screen it covers
       * 
       */
      public void componentMoved (ComponentEvent e) {
         setCaptureArea();
         background.repaint();
      }

      public void componentResized (ComponentEvent e) {
         setCaptureArea();
      }

      public void componentHidden (ComponentEvent e) {};
      public void componentShown (ComponentEvent e) {};


      @Override
      public void keyTyped(KeyEvent e) {}

      @Override
      public void keyPressed(KeyEvent e) {
         // F5
         if (e.getKeyCode() == KeyEvent.VK_F5) {
            background.recapture();
         }
      }

      void updateBackground() {
         background.captureScreen();
      }
      
      @Override
      public void keyReleased(KeyEvent e) {}

      /**
       * Create a transparent background for a JPanel, which is really
       * a screen shot of the area covered by the JPanel because Swing does not
       * support full transparency of windows
       */
      private class TransparentBackground
         extends JPanel implements MouseListener { 
         private static final long serialVersionUID = 1L;
         private Image background;
         private Robot robot;

         public TransparentBackground() {
            setBorder (BorderFactory.createLineBorder (Color.RED, 2));
            addMouseListener(this);

            try {
               robot = new Robot();
               captureScreen();
            }
            catch (Exception e) {
               e.printStackTrace();
            }

         }

         /**
          * Capture an image of the screen
          */
         public void captureScreen() {	 
            Dimension screenSize = 
               Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRectangle =
               new Rectangle( 0, 0,
                  (int) screenSize.getWidth(),
                  (int) screenSize.getHeight());
            background = robot.createScreenCapture (screenRectangle);
         }

         /**
          * Paint the background as the part of the screen it covers
          */         
         public void paintComponent (Graphics g) {
            Point position = this.getLocationOnScreen();
            g.drawImage (background, -position.x, -position.y, null);
         }

         /**
          * Moves window out of the way, then re-captures background
          */
         public void recapture() {
            TransparentDialog.this.setVisible(false);
            captureScreen();
            TransparentDialog.this.setVisible(true);

         }

         @Override
         public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
               recapture();
               repaint();
            }
         }

         @Override
         public void mousePressed(MouseEvent e) {}

         @Override
         public void mouseReleased(MouseEvent e) {}

         @Override
         public void mouseEntered(MouseEvent e) {}

         @Override
         public void mouseExited(MouseEvent e) {}

      }
   }

   boolean isStopRequested() {
      return myStopRequested;
   }

   synchronized void requestStopMovie() {
      if (!myStopRequested) {
         myStopRequested = true;
         // Stop playing when stop movie maker is pressed
         if (myMain.getScheduler().isPlaying() == true &&
             endRecordOnStop.isSelected()) {
            myMain.getScheduler().stopRequest();  
         }
         SwingUtilities.invokeLater (
            new Runnable() {
               public void run() {
                  doStopMovie();
               }
            });
      }
   }

   double getRequestedStopTime() {
      if (stopTime.getValue() == Property.VoidValue) {
         return -1;
      }
      else {
         return stopTime.getDoubleValue();
      }
   }

   


}
