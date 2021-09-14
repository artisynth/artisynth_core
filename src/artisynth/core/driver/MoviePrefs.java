package artisynth.core.driver;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import javax.swing.*;

import artisynth.core.util.ArtisynthIO;
import artisynth.core.moviemaker.MovieMaker;
import artisynth.core.moviemaker.MovieMakerDialog;
import artisynth.core.moviemaker.MethodDialog;
import artisynth.core.moviemaker.MovieMaker.Method;
import artisynth.core.moviemaker.MovieMaker.MethodInfo;
import maspack.properties.PropertyList;
import maspack.widgets.*;
import maspack.util.*;

/**
 * Preferences related to movie generation
 */
public class MoviePrefs extends Preferences
   implements ActionListener, ValueChangeListener {
   
   static PropertyList myProps = new PropertyList (MoviePrefs.class);

   private MovieMaker myMovieMaker = null;

   public static double DEFAULT_FRAME_RATE = 50.0;
   private double myFrameRate = DEFAULT_FRAME_RATE;

   public static Method DEFAULT_METHOD = Method.INTERNAL;
   private Method myMethod = DEFAULT_METHOD;

   // Note: true default working dir path will be set from the movie maker
   public static String DEFAULT_MOVIE_FOLDER = null;
   private String myMovieFolderPath = DEFAULT_MOVIE_FOLDER;

   LinkedHashMap<Method,MethodInfo> myMethodMap;

   // special widgets that we need to remember because they don't correspond to
   // properties
   JButton myCustomizeMethodButton;
   FileNameField myMovieFolderField;

   static {
      myProps.add (
         "frameRate",
         "movie making frame rate (Hz)", DEFAULT_FRAME_RATE, "NS");
      myProps.add (
         "method",
         "movie making method", DEFAULT_METHOD);
   }

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public MoviePrefs (MovieMaker maker) {
      myMethodMap = new LinkedHashMap<Method, MethodInfo>();
      setDefaultMethodInfo();
      myMovieMaker = maker;
      setMovieFolderPath (maker.getDefaultMovieFolderPath());
   }

   private void setDefaultMethodInfo() {
      for (Method method : Method.values()) {
         myMethodMap.put (
            method, new MethodInfo (MovieMaker.getDefaultMethodInfo (method)));
      }
   }

   public double getFrameRate () {
      return myFrameRate;
   }

   public void setFrameRate (double rate) {
      myFrameRate = rate;
   }

   public Method getMethod () {
      return myMethod;
   }

   public void setMethod (Method method) {
      myMethod = method;
   }

   public String getMovieFolderPath() {
      return myMovieFolderPath;
   }

   public void setMovieFolderPath (String path) {
      myMovieFolderPath = path;
   }

   public MovieMaker getMovieMaker() {
      return myMovieMaker;
   }

   public void setDefaults() {
      setDefaultMethodInfo();
      if (myMovieMaker != null) { // paranoid - movieMaker should not be null
         setMovieFolderPath (myMovieMaker.getDefaultMovieFolderPath());
      }
      super.setDefaults();
   }

   public void setFromCurrent() {
      if (myMovieMaker != null) { // paranoid - movieMaker should not be null
         setFrameRate (myMovieMaker.getFrameRate());
         setMethod (myMovieMaker.getMethod());
         for (Method method : Method.values()) {
            myMethodMap.put (
               method, new MethodInfo (myMovieMaker.getMethodInfo (method)));
         }
         setMovieFolderPath (myMovieMaker.getMovieFolderPath());
      }
   }

   public void applyToCurrent() {
      if (myMovieMaker != null) { // paranoid - movieMaker should not be null
         myMovieMaker.setFrameRate (getFrameRate());
         myMovieMaker.setMethod (getMethod());

         for (Method method : Method.values()) {
            myMovieMaker.setMethodInfo (
               method, myMethodMap.get (method));
         }
         myMovieMaker.setMovieFolder (new File (myMovieFolderPath));
         myMovieMaker.updateDialogWidgets();
      }
   }

   protected PropertyPanel createEditingPanel() {
      PropertyPanel panel = createDefaultEditingPanel();

      // add extra widgets not covered by properties:

      // customize button
      JButton customizeButton = new JButton ("Customize Method");
      customizeButton.setActionCommand ("Customize Method");
      customizeButton.addActionListener (this);
      customizeButton.setMargin (new Insets (3, 3, 3, 3));
      customizeButton.setEnabled (getMethod() != Method.INTERNAL);

      GuiUtils.setFixedSize (
         customizeButton, customizeButton.getPreferredSize());
      panel.addWidget (customizeButton);
      myCustomizeMethodButton = customizeButton;

      // callback to enable/disable customize button based on method
      EnumSelector methodWidget = (EnumSelector)panel.getWidget ("method");
      methodWidget.addValueChangeListener (this);

      // working folder
      myMovieFolderField =
         new FileNameField (
            "movie folder", getMovieFolderPath(), 10);
      JFileChooser chooser = myMovieFolderField.getFileChooser();
      chooser.setFileSelectionMode (JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setAcceptAllFileFilterUsed (false);
      FolderFileFilter filter = new FolderFileFilter("Folders");
      chooser.addChoosableFileFilter (filter);
      chooser.setFileFilter (filter);
      // bit of a hack here to make chooser display current file properly
      File curdir = new File(getMovieFolderPath());
      chooser.setSelectedFile (new File(curdir.getName()));

      myMovieFolderField.addValueChangeListener (this);

      panel.addWidget (myMovieFolderField);

      addLoadApplyButtons (panel);
      return panel;
   }

   protected void updateEditingPanelWidgets() {
      if (myEditingPanel != null) {
         super.updateEditingPanelWidgets();
         myCustomizeMethodButton.setEnabled (getMethod() != Method.INTERNAL);
         myMovieFolderField.setValue (getMovieFolderPath());
      }
   }

   public void valueChange (ValueChangeEvent e) {
      // enable or disable customize button depending on which method is
      // selected
      LabeledControl control = (LabeledControl)e.getSource();
      if (control.getLabelText().equals ("method")) {
         myCustomizeMethodButton.setEnabled (getMethod() != Method.INTERNAL);
      }
      else if (control == myMovieFolderField) {
         File dir = new File ((String)myMovieFolderField.getValue());
         Window win = SwingUtilities.windowForComponent (myMovieFolderField);
         if (MovieMakerDialog.validateFolder (win, dir)) {
            setMovieFolderPath (dir.getAbsolutePath());
         }
         else {
            myMovieFolderField.setValue (getMovieFolderPath());
         }        
      }
   }

   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("Customize Method")) {
         Window win = SwingUtilities.windowForComponent (
            (Component)e.getSource());
         MethodInfo methodInfo = myMethodMap.get (getMethod());
         MethodDialog dialog = new MethodDialog (win, methodInfo);
         dialog.setLocationRelativeTo (win);
         dialog.setVisible (true);
      }
      else {
         super.actionPerformed (e);
      }
   }

   public boolean scanItem (ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      if (rtok.tokenIsWord ("commands")) {
         rtok.scanToken ('=');
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            String methodName = rtok.scanWord();
            String command = rtok.scanQuotedString('"');
            Method method = Method.valueOf (methodName);
            if (method == null) {
               throw new IOException ("Unknown method " + methodName);
            }
            MethodInfo info = myMethodMap.get (method);
            info.command = command;
         }
         return true;
      }
      else if (rtok.tokenIsWord ("moviefolder")) {
         rtok.scanToken ('=');
         setMovieFolderPath (rtok.scanQuotedString('"'));
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok);
   }

   public void writeItems (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      super.writeItems (pw, fmt, ref);
      pw.println ("commands=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (Method method : Method.values()) {
         MethodInfo info = myMethodMap.get (method);
         MethodInfo defaultInfo = MovieMaker.getDefaultMethodInfo (method);
         if (!info.command.equals (defaultInfo.command)) {
            pw.println (method + " \"" + info.command + "\"");
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
      if (myMovieFolderPath != null &&
          !myMovieMaker.getDefaultMovieFolderPath().equals (myMovieFolderPath)) {
         String folderStr =
            ReaderTokenizer.getQuotedString (myMovieFolderPath, '"');
         pw.println ("moviefolder="+folderStr);
      }
   }
}
