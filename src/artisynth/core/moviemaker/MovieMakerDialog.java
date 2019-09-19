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
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
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
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.driver.Main;
import artisynth.core.driver.MainFrame;
import artisynth.core.driver.Scheduler;
import artisynth.core.driver.ViewerManager;
import artisynth.core.modelbase.HasAudio;
import artisynth.core.moviemaker.MovieMaker.Method;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ConvertRawToWav;
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

   private StringField filename;
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
   private FileNameField workingDirField;
   
   private JCheckBox showCaptureFrame;

   private DoubleField frameRateField;
   private DoubleField speedField;

   private StringSelector formatSelector;
   private StringSelector methodSelector;
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

   private String tmpDirectory;
   private double savedFrameRate;

   private boolean viewerCapture;
   private boolean customCapture;

   private boolean areaHandlerMasked = false;
   private boolean isRecordingStarted = false;

   private class AreaHandler implements ValueChangeListener {
      public void valueChange (ValueChangeEvent e) {
         if (!areaHandlerMasked) {
            Rectangle oldArea = myMovieMaker.getCaptureArea();
            Rectangle newArea = new Rectangle (
               (Integer) windowXPosition.getIntValue(),
               (Integer) windowYPosition.getIntValue(), 
               (Integer) windowWidth.getIntValue(), 
               (Integer) windowHeight.getIntValue());

            if (!oldArea.equals (newArea)) {
               Dimension viewerDim = null;
               if (viewerCapture && !originalSize.isSelected()) {
                  viewerDim = new Dimension (
                     (Integer) resizeWidth.getIntValue(),
                     (Integer) resizeHeight.getIntValue());
               }
               else if (viewerCapture) {
                  viewerDim = new Dimension (newArea.width, newArea.height);
               }

               myMovieMaker.setCaptureArea (newArea, viewerDim, viewerCapture);

               if (areaSelectionFrame != null && 
                  areaSelectionFrame.isVisible()) { 
                  areaSelectionFrame.setBoundsFromCaptureArea (newArea);
               }
            }
         }
      }
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
      setTitle ("Set Movie Options");

      JTabbedPane tabbedPane = new JTabbedPane();

      Box recOptionsBox = Box.createVerticalBox();
      LabeledComponentPanel captureOptions = new LabeledComponentPanel();
      captureOptions.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Region To Capture"));  
      LabeledComponentPanel recOptions = new LabeledComponentPanel();
      recOptions.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Record Options"));

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

      windowXPosition = new IntegerField ("Left", 0);
      windowXPosition.addValueChangeListener (new AreaHandler());
      captureOptions.addWidget (windowXPosition);
      windowYPosition = new IntegerField ("Top", 0);
      windowYPosition.addValueChangeListener (new AreaHandler());
      captureOptions.addWidget (windowYPosition);
      windowWidth = new IntegerField ("Width", 0);
      windowWidth.addValueChangeListener (new AreaHandler());
      captureOptions.addWidget (windowWidth);
      windowHeight = new IntegerField ("Height", 0);
      windowHeight.addValueChangeListener (new AreaHandler());
      captureOptions.addWidget (windowHeight);

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
      showCaptureFrame = new JCheckBox ("Show capture frame", null, true);
      showCaptureFrame.addActionListener (this);
      showCaptureFrame.setEnabled (false);
      recOptions.addWidget (showCaptureFrame);
      
      alwaysOnTop = new JCheckBox ("Window always on top", null, myMovieMaker.isAlwaysOnTop ());
      recOptions.add (alwaysOnTop);
      alwaysOnTop.addActionListener (this);
      
      recordAudio = new JCheckBox (REC_AUDIO_CMD, null, false);
      recordAudio.addActionListener (this);
      normalizeAudio = new JCheckBox ("Normalize audio", null, false);
      recordAudioTxt = new JCheckBox (REC_AUDIO_TO_TXT_CMD, null, false);
      recordAudioTxt.addActionListener (this);

      setAudioOptions (myMain.getRootModel() instanceof HasAudio);

      recOptionsBox.add (recOptions);  

      filename = new StringField ("Movie name", "artisynth", 10);
      filename.setStretchable (true);
      filename.setBorder (BorderFactory.createEmptyBorder (5, 0, 5, 0));
      recOptionsBox.add (filename);

      Box encOptionsBox = Box.createVerticalBox();
      LabeledComponentPanel outputOptions = new LabeledComponentPanel();
      outputOptions.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Output Options"));  
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

      HashMap<String,MovieMaker.Method> methodMap = movieMaker.getMethodMap();
      methodSelector = new StringSelector (
         "Method", methodMap.keySet().toArray (new String[0]));
      String currentMethod = movieMaker.getMethod ();
      if (currentMethod == null) {
         methodSelector.setValue (MovieMaker.INTERNAL_METHOD);
      } else {
         methodSelector.setValue (currentMethod);
      }
      methodSelector.addValueChangeListener (this);
      GuiUtils.setFixedSize (
         methodSelector.getComboBox(), new Dimension (125, 25));

      customizeButton = new JButton ("Customize Method");
      customizeButton.setActionCommand (CUSTOMIZE_CMD);
      customizeButton.addActionListener (this);
      customizeButton.setMargin (new Insets (3, 3, 3, 3));
      GuiUtils.setFixedSize (customizeButton, new Dimension (125, 25));
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
            myMovieMaker.setCaptureArea (myMovieMaker.getCaptureArea(), 
               new Dimension ((Integer) resizeWidth.getIntValue(), 
                  (Integer) resizeHeight.getIntValue()), viewerCapture);
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
            myMovieMaker.setCaptureArea (myMovieMaker.getCaptureArea(), 
               new Dimension ((Integer) resizeWidth.getIntValue(), 
                  (Integer) resizeHeight.getIntValue()), viewerCapture);
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

      // advanced
      Box extraBox = Box.createVerticalBox();
      
      LabeledComponentPanel advancedOptions = new LabeledComponentPanel ();
      advancedOptions.setBorder (BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Advanced"));  
      advancedOptions.setName("advanced");
      
      workingDirField = new FileNameField ("Working directory", ArtisynthPath.getTempDir ().getAbsolutePath (), 10);
      workingDirField.getFileChooser ().setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY);
      workingDirField.getFileChooser ().setCurrentDirectory (ArtisynthPath.getTempDir ());
      advancedOptions.add (workingDirField);
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
      

      tabbedPane.addTab ("Recorder", recOptionsBox);
      tabbedPane.addTab ("Encoder", encOptionsBox);
      tabbedPane.addTab("Advanced", extraBox);

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

      viewerCapture = true;
      customCapture = false;

      final Component displayComponent = myFrame.getViewer().getCanvas().getComponent();
      setCaptureArea (displayComponent);

      displayComponent.addComponentListener(
         new ComponentListener() {
            public void componentHidden (ComponentEvent c_evt) {}

            public void componentMoved (ComponentEvent c_evt) {
               if (!customCapture && viewerCapture) {
                  setCaptureArea (displayComponent);
               }
               else if (!customCapture && !viewerCapture) {
                  setCaptureArea (myFrame);
               }
            }

            public void componentResized (ComponentEvent c_evt) {
               if (!customCapture && viewerCapture) {
                  setCaptureArea (displayComponent);               
               }
               else if (!customCapture && !viewerCapture) {
                  setCaptureArea (myFrame);
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
      filename.setValue (name);
   }

   private void updateMethodSelectors() { 

      String methodName = (String) methodSelector.getValue();
      if (MovieMaker.INTERNAL_METHOD.equals (methodName)) {

         Method method = myMovieMaker.getMethodMap().get (methodName);
         if (myMovieMaker.getFormat() != method.frameFileFormat) {
            myMovieMaker.setFormat (method.frameFileFormat);
            formatSelector.setValue (method.frameFileFormat);
         }

         customizeButton.setEnabled (false);
         formatSelector.setEnabledAll (false);
      }
      else {
         customizeButton.setEnabled (true);
         formatSelector.setEnabledAll (true);
      }
   }

   private void setCaptureArea (Component comp) {
      areaHandlerMasked = true;

      int width = comp.getWidth();
      int height = comp.getHeight();
      Point pos = comp.getLocationOnScreen();

      boolean widthChanged = false;
      if (width != windowWidth.getIntValue()) {
         widthChanged = true;
      }

      windowWidth.setValue (width);
      windowHeight.setValue (height);
      windowXPosition.setValue (pos.x);
      windowYPosition.setValue (pos.y);

      Rectangle area = new Rectangle(pos.x, pos.y, width, height);

      Dimension viewerDim = null;
      if (viewerCapture && !originalSize.isSelected()) {
         if (constrainSize.isSelected()) {
            // resize width/height
            if (widthChanged) {
               double scale = 
                  resizeHeight.getDoubleValue() / windowHeight.getDoubleValue();
               resizeWidth.setValue (scale * windowWidth.getIntValue());
            } else {
               double scale = 
                  resizeWidth.getDoubleValue() / windowWidth.getDoubleValue();
               resizeHeight.setValue (scale * windowHeight.getIntValue());
            }
         }
         viewerDim = new Dimension (
            (Integer) resizeWidth.getIntValue(),
            (Integer) resizeHeight.getIntValue());
      }
      else if (viewerCapture) {
         viewerDim = new Dimension (area.width, area.height);
      } 

      myMovieMaker.setCaptureArea (area, viewerDim, viewerCapture);

      if (customCapture) {
         windowXPosition.getTextField().setEnabled (true);
         windowYPosition.getTextField().setEnabled (true);
         windowWidth.getTextField().setEnabled (true);
         windowHeight.getTextField().setEnabled (true);
      }
      else {
         windowXPosition.getTextField().setEnabled (false);
         windowYPosition.getTextField().setEnabled (false);
         windowWidth.getTextField().setEnabled (false);
         windowHeight.getTextField().setEnabled (false);

      }

      if (constrainSize.isSelected() && originalSize.isSelected()) {
         resizeWidth.setValue(width);
         resizeHeight.setValue(height);
      }



      if (areaSelectionFrame != null && 
         areaSelectionFrame.isVisible()) {

         areaSelectionFrame.setBoundsFromCaptureArea (area);
      }

      areaHandlerMasked = false;
   }

   /**
    * Sets the file name for movie saving to the title of the main frame,
    * which is name of the currently loaded model.
    */
   public void resetFileName() {
      filename.setValue (myFrame.getTitle());
   }

   private void setAreaSelectionFrame (TransparentDialog frame) {
      TransparentDialog oldFrame = areaSelectionFrame;
      areaSelectionFrame = frame;      
      if (frame == null && oldFrame != null) {
         oldFrame.setVisible (false);
         oldFrame.dispose();
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
      if (event.getSource() == methodSelector) {
         String methodName = (String) methodSelector.getValue();

         if (!methodName.equals (myMovieMaker.getMethod())) {
            MovieMaker.Method method = 
               myMovieMaker.getMethodMap().get (methodName);
            myMovieMaker.setMethod (methodName);

            if (!method.frameFileFormat.equals (myMovieMaker.getFormat())) {
               myMovieMaker.setFormat (method.frameFileFormat);
               formatSelector.setValue (method.frameFileFormat);
            }
         }
      }
      else if (event.getSource() == formatSelector) {
         String format = (String) formatSelector.getValue();

         if (!format.equals (myMovieMaker.getFormat())) {
            myMovieMaker.setFormat (format);
            String methodName = (String) methodSelector.getValue();
            MovieMaker.Method method =
               myMovieMaker.getMethodMap().get (methodName);
            method.frameFileFormat = format;
         }
      }
      else if (event.getSource() == frameRateField) {
         myMovieMaker.setFrameRate (frameRateField.getDoubleValue()); 
      }
      else if (event.getSource() == speedField) {
         myMovieMaker.setSpeed (speedField.getDoubleValue()); 
      }
      else if (event.getSource() == resizeSamples) {
         myMovieMaker.setAntiAliasingSamples(resizeSamples.getIntValue());
      }
      else {
         throw new InternalErrorException (
            "valueChange event from unknown source " + event.getSource());
      }

      updateMethodSelectors();
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
         manager.paint();

         // starts recording
         startMovie();        
         myMovieMaker.setGrabbing (false);   // we're manually doing this

         myMovieMaker.resetFrameCounter();
         myMovieMaker.forceGrab();

         while (scheduler.fastForward()) {
            manager.render();
            manager.paint();
            myMovieMaker.forceGrab();
         }
         stopMovie();

      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         System.err.println("Failed to create movie");
      }

      beginRecordOnStart.setEnabled(savedBegin);
      beginRecordOnStart.setSelected (savedBegin);


   }

   private void startMovie() {
      String movieFileName = filename.getText();
      myMovieMaker.setRenderingAudioToFile (recordAudio.isSelected());
      myMovieMaker.setRenderingAudioToText (recordAudioTxt.isSelected());
      myMovieMaker.setAudioNormalized (normalizeAudio.isSelected());

      if (movieFileName.equals ("")) {
         GuiUtils.showError (this, "Please enter a movie name");
         filename.requestFocusInWindow();
      }
      else {
         stopButton.setEnabled (true);
         startButton.setEnabled (false);
         frameButton.setEnabled(true);

         //  prevent resizing from affecting video
         // XXX: removed, since prevents zoom in orthographic projection
         // Main.getMain().getViewer().setResizeEnabled(false);   

         tmpDirectory = workingDirField.getText ();

         // test if tmp directory exists
         File testdir = new File (tmpDirectory);
         if (!testdir.exists()) {
            System.out.println ("Creating:" + tmpDirectory);
            testdir.mkdirs();
         }
         else if (!testdir.isDirectory()) {
            System.err.println ("Error: " + tmpDirectory + " as file");
            filename.requestFocusInWindow();
            return;
         }

         // Right now we expect the models to either produce a RAW audio
         // file, or a text file. The RAW file should be merged with the
         // movie, while the text is left alone as is.
         if (myMain.getRootModel() instanceof HasAudio) {
            if (recordAudio.isSelected()) { // Render a raw audio file
               myMovieMaker.setAudioFileName (tmpDirectory + "/aud.raw");
            }

            if (recordAudioTxt.isSelected()) { // Render a text file
               System.out.println ("Rendering audio to text...");
               myMovieMaker.setAudioFileName (
                  ArtisynthPath.getHomeDir() + "/" + filename.getText() + ".txt");
            }

            boolean renderToFile =
               recordAudio.isSelected() || recordAudioTxt.isSelected();
            ((HasAudio) myMain.getRootModel()).setRenderAudioToFile (
               renderToFile);
         }
         
         System.out.println (myMovieMaker.getCaptureArea());
         
         if (automaticFrames.isSelected()) {
            myMovieMaker.setGrabbing (true);
         }
         savedFrameRate = myMain.getFrameRate();
         myMain.setFrameRate (
            myMovieMaker.getFrameRate() / myMovieMaker.getSpeed());

         myFrame.setAlwaysOnTop (myMovieMaker.isAlwaysOnTop ());
         setAreaSelectionFrame (null);
         showCaptureFrame.setSelected (false);

         try {
            myMovieMaker.setDataPath (tmpDirectory);
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
            System.out.println ("ERROR: Can't create movie writer: " + e);
            myMain.setFrameRate (savedFrameRate);
            myMovieMaker.setGrabbing (false);

            myFrame.setAlwaysOnTop (false);
         }
      }
   }
   
   public boolean isStarted() {
      return isRecordingStarted;
   }

   public void stopMovie() {  

      // re-enable viewer resize
      // myMain.getViewer().setResizeEnabled(true);
      // myMain.getViewer().rerender(); // trigger rerender

      Thread stopThread = new Thread() {
         public void run() {
            String movieFileName = filename.getText();
            
            // make sure directories exist
            File mfile = new File(myMovieMaker.getDataPath (), movieFileName); 
            mfile.getParentFile ().mkdirs ();
            
            myMain.setFrameRate (savedFrameRate);
            myMovieMaker.setGrabbing (false);
            myFrame.getViewer().cleanupScreenShots();

            stopButton.setEnabled (false);
            frameButton.setEnabled(false);
            startButton.setEnabled (true);

            myFrame.setAlwaysOnTop (false);

            // Stop playing when stop movie maker is pressed
            if (myMain.getScheduler().isPlaying() == true &&
               endRecordOnStop.isSelected()) {
               myMain.getScheduler().pause();  
            }

            // Make the movie out of the frames
            try {

               System.out.println("Stopping movie...");
               int frameCount = myMovieMaker.close();

               // No merging required if outputting silent movie, or writing audio 
               // to text file
               if (frameCount > 0) { 
                  if (!myMovieMaker.isRenderingAudioToFile()) {
                     myMovieMaker.render (movieFileName);
                  }
                  else {
                     ((HasAudio) myMain.getRootModel()).onStop();
                     String tmpMovieFn = "file:" + tmpDirectory + "/noAudio.mov";
                     String finalMovieFn =
                        "file:" + ArtisynthPath.getHomeDir() + "/" + movieFileName;

                     System.out.println("Rendering frames...");
                     // Render the movie's visual frames only
                     myMovieMaker.render (tmpMovieFn);

                     //convert raw audio to wav
                     String waveFn = "file:" + 
                        ConvertRawToWav.convert ( myMovieMaker.getAudioSampleRate(), 
                           myMovieMaker.getAudioFileName());
                     String[] args = {tmpMovieFn, 
                        waveFn, 
                        "-o",
                        finalMovieFn};
                     System.out.println ("final file name:" + finalMovieFn);
                     new Merge (args);
                     //TODO: delete tmp files!
                  }
                  if (saveMovieImage.isSelected()) {
                     myMovieMaker.saveFirstFrame (movieFileName);
                  }
               }
               else {
                  System.out.println ("No images grabbed, not making movie"); 
               }

               if (removeImages.isSelected()) {
                  System.out.println ("removing image files");
                  myMovieMaker.clean();
               }
            }

            catch (Exception e) {
               e.printStackTrace();
            }

            // change the audio source back to outputting to sound card
            if ((myMain.getRootModel() instanceof HasAudio) &&
               ((recordAudio.isSelected()) || recordAudioTxt.isSelected()) ) {
               System.out.println("Going back to soundcard output...");

               myMovieMaker.setRenderingAudioToFile (false);
               myMovieMaker.setRenderingAudioToText (false);

               recordAudio.setSelected (false);
               recordAudioTxt.setSelected (false);

               normalizeAudio.setSelected (false);
               normalizeAudio.setEnabled (false);

               ((HasAudio) myMain.getRootModel()).setRenderAudioToFile (false);
            }
            
            System.out.println("Done making movie");
            
         }
      };

      stopThread.start();
      isRecordingStarted = false;

   }

   public void actionPerformed (ActionEvent event) {
      String cmd = event.getActionCommand();
      Object source = event.getSource();

      if (cmd.equals (FULL_WINDOW_CMD)) {
         viewerCapture = false;
         customCapture = false;

         showCaptureFrame.setEnabled (false);
         setAreaSelectionFrame (null);

         setCaptureArea (myFrame);
         viewWindowButton.setBorder (new BevelBorder (BevelBorder.RAISED));
         fullWindowButton.setBorder (new BevelBorder (BevelBorder.LOWERED));
         customWindowButton.setBorder (new BevelBorder (BevelBorder.RAISED));

         originalSize.setEnabled (false);
         constrainSize.setEnabled (false);
         resizeWidth.getTextField().setEnabled (false);
         resizeHeight.getTextField().setEnabled (false);
         resizeSamples.getTextField().setEnabled(false);

         windowWidth.getTextField().setEnabled (false);
         windowHeight.getTextField().setEnabled (false);
         windowXPosition.getTextField().setEnabled (false);
         windowYPosition.getTextField().setEnabled (false);
      }
      else if (cmd.equals (VIEW_WINDOW_CMD)) {
         viewerCapture = true;
         customCapture = false;

         showCaptureFrame.setEnabled (false);
         setAreaSelectionFrame (null);

         setCaptureArea (myFrame.getViewer().getCanvas().getComponent());
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

         windowWidth.getTextField().setEnabled (false);
         windowHeight.getTextField().setEnabled (false);
         windowXPosition.getTextField().setEnabled (false);
         windowYPosition.getTextField().setEnabled (false);
      }
      else if (cmd.equals (CUSTOM_WINDOW_CMD)) {
         viewerCapture = false;
         customCapture = true;


         if (!showCaptureFrame.isSelected() || 
            !showCaptureFrame.isEnabled ()) {            
            showCaptureFrame.setEnabled (true);
            showCaptureFrame.setSelected (true);

            createSelectionFrame();
            showCaptureFrame.setSelected (true);
         }

         viewWindowButton.setBorder (new BevelBorder(BevelBorder.RAISED));
         fullWindowButton.setBorder (new BevelBorder(BevelBorder.RAISED));
         customWindowButton.setBorder (new BevelBorder (BevelBorder.LOWERED));

         originalSize.setEnabled (false);
         constrainSize.setEnabled (false);
         resizeWidth.getTextField().setEnabled (false);
         resizeHeight.getTextField().setEnabled (false);
         resizeSamples.getTextField().setEnabled(false);

         windowWidth.getTextField().setEnabled (true);
         windowHeight.getTextField().setEnabled (true);
         windowXPosition.getTextField().setEnabled (true);
         windowYPosition.getTextField().setEnabled (true);
      }
      else if (cmd.equals (REC_AUDIO_CMD)) {
         if (recordAudio.isSelected()) {
            System.out.println("rec. audio selected");
            recordAudioTxt.setSelected (false);
         }

         normalizeAudio.setEnabled (
            recordAudio.isSelected() || recordAudioTxt.isSelected());
      }
      else if (cmd.equals (REC_AUDIO_TO_TXT_CMD)) {
         if (recordAudioTxt.isSelected()) {
            System.out.println("rec. audio txt selected");
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
         String methodName = (String) methodSelector.getValue();
         MovieMaker.Method method = 
            myMovieMaker.getMethodMap().get (methodName);

         if (method == null) {
            throw new InternalErrorException (
               "MovieMaker does not know about method " + methodName);
         }

         MethodDialog dialog = new MethodDialog (this, method);
         dialog.setLocationRelativeTo (this);
         dialog.setVisible (true);
      }
      else if (source == showCaptureFrame) { 
         if (showCaptureFrame.isSelected()) { 
            createSelectionFrame ();
         }
         else {
            setAreaSelectionFrame (null);
         }
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

         myMovieMaker.setCaptureArea (myMovieMaker.getCaptureArea(), 
            new Dimension ((Integer) resizeWidth.getIntValue(), 
               (Integer) resizeHeight.getIntValue()), viewerCapture);
      } else if (source == alwaysOnTop) {
         boolean onTop = alwaysOnTop.isSelected ();
         myMovieMaker.setAlwaysOnTop (onTop);
         if (isRecordingStarted) {
            myFrame.setAlwaysOnTop (onTop);
         }
      }
   }

   private void createSelectionFrame () {
      // Create transparent dialog to select the screen area
      setAreaSelectionFrame (new TransparentDialog (this));

      areaSelectionFrame.pack();
      areaSelectionFrame.setVisible (true);
      areaSelectionFrame.setBoundsFromCaptureArea (
         myMovieMaker.getCaptureArea());
   }

   private void takeScreenshot() {
      String movieFileName = filename.getText();

      if (movieFileName.equals ("")) {
         GuiUtils.showError (this, "Please enter a movie name");
         filename.requestFocusInWindow();
      }
      else {

         boolean stopEnabled = stopButton.isEnabled();
         boolean startEnabled = startButton.isEnabled();
         stopButton.setEnabled (false);
         startButton.setEnabled (false);

         tmpDirectory = workingDirField.getText ();

         // test if tmp directory exists
         File testdir = new File (tmpDirectory);
         if (!testdir.exists()) {
            System.out.println ("Creating:" + tmpDirectory);
            testdir.mkdir();
         }
         else if (!testdir.isDirectory()) {
            System.err.println ("Error: " + tmpDirectory + " as file");
            filename.requestFocusInWindow();
            return;
         }

         setAreaSelectionFrame (null);
         showCaptureFrame.setSelected (false);

         try {
            myMovieMaker.grab (tmpDirectory + "/" + movieFileName );
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
   private class TransparentDialog extends JDialog implements ComponentListener, KeyListener {
      private static final long serialVersionUID = 1L;
      private TransparentBackground background;
      private MovieMakerDialog parent;

      public TransparentDialog (MovieMakerDialog parent) {
         super (parent);
         this.parent = parent;

         background = new TransparentBackground();
         getContentPane().add (background);

         setTitle ("Capture area is outlined in red");
         setDefaultCloseOperation (DISPOSE_ON_CLOSE);
         setModal (false);

         addComponentListener (this);
         addKeyListener(this);
         addWindowListener (new WindowAdapter() {
            public void windowClosing (WindowEvent e) {
               showCaptureFrame.setSelected (false);
            }
         });

         if (!customCapture) {
            setUndecorated (true);
         }
      }

      private void setCaptureArea() {
         parent.setCaptureArea (background);
      }

      public void setBoundsFromCaptureArea (Rectangle rect) {
         // Get the current bounds and adjust 
         Point origin = background.getLocationOnScreen();
         Rectangle bounds = getBounds();
         bounds.x += (rect.x - origin.x);
         bounds.y += (rect.y - origin.y);
         bounds.width += (rect.width - background.getWidth());
         bounds.height += (rect.height - background.getHeight());
         System.out.println ("bounds=" + bounds);
         setBounds (bounds);
      }

      public void dispose() {
         System.out.println ("disposing area selection frame");
         setAreaSelectionFrame (null);
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
         System.out.println(e.getKeyCode());
         if (e.getKeyCode() == KeyEvent.VK_F5) {
            background.recapture();
         }
      }

      @Override
      public void keyReleased(KeyEvent e) {}

      /**
       * Create a transparent background for a JPanel, which is really
       * a screen shot of the area covered by the JPanel because Swing does not
       * support full transparency of windows
       */
      private class TransparentBackground extends JPanel implements MouseListener { 
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
            Dimension screenSize = 
               Toolkit.getDefaultToolkit().getScreenSize();

            Point oldPos = TransparentDialog.this.getLocation();
            TransparentDialog.this.setLocation(screenSize.width, screenSize.height);
            captureScreen();
            TransparentDialog.this.setLocation(oldPos);
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


   private class MethodDialog extends JDialog implements ActionListener {
      private static final long serialVersionUID = 1L;
      private JTextField myTextField;
      private OptionPanel myOptionPanel;
      private MovieMaker.Method myMethod;

      MethodDialog (Window owner, MovieMaker.Method method) {
         /* explicit Dialog cast for compatibility with Java 1.5 */
         super ((Dialog) owner);
         myTextField = new JTextField();
         JPanel panel = new JPanel();
         panel.setLayout (new BoxLayout (panel, BoxLayout.Y_AXIS));
         setContentPane (panel);

         panel.setBorder (new EmptyBorder (4, 4, 4, 4));
         myTextField.setText (method.command);
         Dimension size = myTextField.getPreferredSize();
         size.width = 10000;
         myTextField.setMaximumSize (size);
         myOptionPanel = new OptionPanel ("OK Cancel", this);
         myMethod = method;
         panel.add (myTextField);
         panel.add (Box.createRigidArea (new Dimension (0,4)));
         System.out.println (myTextField.getPreferredSize());
         panel.add (myOptionPanel);
         setModal (true);
         pack();
      }

      public void actionPerformed (ActionEvent e) {
         String cmd = e.getActionCommand();
         if (cmd.equals ("OK")) {
            myMethod.command = myTextField.getText();
         }
         else if (cmd.equals ("Cancel")) {}

         dispose();
      }
   }
}
