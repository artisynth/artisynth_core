/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.io.*;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewerPanel;
import artisynth.core.gui.navpanel.NavigationPanel;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;

/**
 * the main frame to initialize all the panels and viewers. This class also
 * creates the main menu.
 * 
 * revision and total modifications: andreio removed unnecessary reallocations
 * in code, obsolete functions and improved code style, made global static
 * constraints out of magic numbers
 */

public class MainFrame extends JFrame {
   private static final long serialVersionUID = 1L;
   protected JMenuItem quitItem;
   private MenuBarHandler myMenuBar;
   private NavigationPanel myNavPanel;
   private GLViewerPanel GLPanel;
   private JSplitPane splitPane;
   protected Main myMain;
   protected String myErrMsg;
   private String baseName;
   boolean splitterExpanded = false;

   /** set the main frame constants */

   private static String myArtiSynthVersion = null;
   private static final String ABOUT_MODEL_NO_DESC = "No description available";
   private static final String ABOUT_MODEL_NOT_LOADED = "No model loaded.";
   private static final String ABOUT_MODEL_TITLE = "About model";
   private static final String ABOUT_ARTISYNTH_TITLE = "About Artisynth";
   private static final String KEYBINDINGS_TITLE = "Keybindings";

   /** set the default GL viewer panel and nav panel sizes */

   //private static final int GL_PANEL_WIDTH = 450;
   //private static final int GL_PANEL_HEIGHT = 450;

   private static final int NAV_PANEL_WIDTH = 250;
   //private static final int NAV_PANEL_HEIGHT = 400;

   private SelectComponentPanelHandler selectCompPanelHandler;

   /**
    * set the error message
    * 
    * @param msg error message
    */
   public void setErrorMessage (String msg) {
      myErrMsg = msg;
   }

   /**
    * get error message
    * 
    * @return error message string
    */

   public String getErrorMessage() {
      return myErrMsg;
   }

   /**
    * get main class instance
    * 
    * @return main instance
    */

   public Main getMain() {
      return myMain;
   }

   /**
    * set the main frame title
    */

   public void setBaseTitle (String name) {
      baseName = name;
      updateWorkingDirDisplay();
   }

   private static final int maxDirDisplayChars = 48;

   public void updateWorkingDirDisplay() {
      String path = ArtisynthPath.getWorkingDirPath();
      path = ArtisynthPath.convertToUnixSeparators (path);
      if (path.length() > maxDirDisplayChars-2) {
         int cutIdx = path.length() - maxDirDisplayChars + 3;
         path = "..." + path.substring (cutIdx);
      }
      setTitle (baseName + " [ " + path + " ] ");
   }

   /**
    * create main frame
    * 
    * @param name -
    * frame name
    * @param main -
    * main program instance
    * @param width -
    * viewer width
    * @param height -
    * viewer height
    */

   public MainFrame (String name, Main main, int width, int height) {
      
      super (name);

      JPopupMenu.setDefaultLightWeightPopupEnabled (false);

      // this requires the split pane to be repainted on the resize
      // of the window, so NavBar never appears when its not supposed
      // to appear, bug fix by andreio

      addComponentListener (new java.awt.event.ComponentAdapter() {
         public void componentResized (java.awt.event.ComponentEvent evt) {
            refreshSplitPane();
            getViewer().rerender();
         }
      });

      // end of the repaint on resize bug fix

      myMain = main;
      baseName = name;
      GLPanel = new GLViewerPanel (width, height, myMain.getGraphics());

      myNavPanel = new NavigationPanel();
      //myNavPanel.setLayout (new FlowLayout (FlowLayout.LEFT));

      // content panes must be opaque
      myNavPanel.setOpaque (true);

      addWindowListener (new WindowAdapter() {
            public void windowClosing (WindowEvent e) {
               GLPanel.dispose();  // cleanly close JOGL context
               Main.exit (0);
            }

            public void windowClosed (WindowEvent e) {
            }
         });                  

      setDefaultCloseOperation (JFrame.DISPOSE_ON_CLOSE);

      // Provide minimum sizes for the two components in the split pane
      GLPanel.setMinimumSize (new Dimension (0, 0));

      splitPane =
         new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, myNavPanel, GLPanel);
      // rerender if split pane moved
      splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, 
         new PropertyChangeListener() {
             @Override
             public void propertyChange(PropertyChangeEvent pce) {
                getViewer().rerender();
             }
      });
      
      splitPane.setOneTouchExpandable (true);
      refreshSplitPane();

      selectCompPanelHandler = new SelectComponentPanelHandler (main, this);
      selectCompPanelHandler.createPanel();

      // create menu bar handler to handle the main menu
      myMenuBar = new MenuBarHandler (main, this);
      getContentPane().add (splitPane);

      myMenuBar.createMenus();

      // this fixes the bug when the splitter clicked the first time the
      // navigation bar would disappear on resize, not any longer fixed by
      // andreio

      BasicSplitPaneDivider divider =
         ((BasicSplitPaneUI)splitPane.getUI()).getDivider();
      MainFrameMouseListener mainFrameMouseListener =
         new MainFrameMouseListener();
      mainFrameMouseListener.setNavPanel (myNavPanel);
      mainFrameMouseListener.setMainFrame (this);

      divider.addMouseListener (mainFrameMouseListener);
      // end of the splitter on resize bug fix
      setBaseTitle ("Artisynth");

      // set icon
      URL iconUrl = ArtisynthPath.findResource("artisynth/core/gui/icon/artisynth.gif");
      try {
         ImageIcon icon = new ImageIcon(iconUrl);
         setIconImage(icon.getImage());
      } catch (Exception e) {
      }
   }

   public void setViewerSize (int w, int h) {
      // need to set preferred size for both the viewer and the splitPane
      // in order for the viewer to ultimately end up with the right size
      int splitw = w + splitPane.getDividerSize();
      int splith = h;
      if (splitPane.getBorder() != null) {
         Insets insets = splitPane.getBorder().getBorderInsets(splitPane);
         splitw += insets.left + insets.right;
         splith += insets.top + insets.bottom;
      }
      boolean closedHack = false;
      if (!myNavPanel.isExpanded()) {
         closedHack = true;
         //myNavPanel.setMinimumSize (new Dimension(0,0));
      }
      else {
         splitw += myNavPanel.getSize().width;
      }
      splitPane.setPreferredSize (new Dimension (splitw, splith));
      GLPanel.setPreferredSize (new Dimension(w, h));
      pack();
      if (closedHack) {
         // keep divided closed in case it moved
         refreshSplitPane();
      }
   }

   /**
    * purpose:
    * 
    * @return SelectComponentPanelHandler instance
    */
   public SelectComponentPanelHandler getSelectCompPanelHandler() {
      return selectCompPanelHandler;
   }

   /**
    * get the GLviewer instance
    * 
    * @return GLViewer instance
    */

   public GLViewer getViewer() {
      return GLPanel.getViewer();
   }

   /**
    * get the GL panel
    * 
    * @return GLViewerPanel
    */

   public GLViewerPanel getGLPanel() {
      return GLPanel;
   }

   // public JSplitPane getSplitPane()
   // {
   // return splitPane;
   // }

   /**
    * get the menu bar handler
    * 
    * @return menu bar handler
    */

   public MenuBarHandler getMenuBarHandler() {
      return myMenuBar;
   }

   /**
    * get navigation panel instance
    * 
    * @return navigation panel
    */

   public NavigationPanel getNavPanel() {
      return myNavPanel;
   }

   /**
    * display key bindings
    * 
    */

   public String getArtiSynthVersion() {
      if (myArtiSynthVersion == null) {
         try {
            BufferedReader reader = 
               new BufferedReader (new FileReader (
                  ArtisynthPath.getHomeRelativeFile ("VERSION", ".")));
            myArtiSynthVersion = reader.readLine();
            int uidx = myArtiSynthVersion.lastIndexOf ("_");
            if (uidx != -1) {
               myArtiSynthVersion = myArtiSynthVersion.substring (uidx+1);
            }
            reader.close();
         }
         catch (IOException e) {
            myArtiSynthVersion = null;
         }
      }
      return myArtiSynthVersion;
   }
   
   /**
    * Create the ArtiSynth information frame.
    */
   public JFrame createArtisynthInfo() {
      JFrame frame = new JFrame (ABOUT_ARTISYNTH_TITLE);

      String bodyText;
      File gitDir = ArtisynthPath.getHomeRelativeFile (".git", ".");
      String version = getArtiSynthVersion();
      if (gitDir != null && gitDir.isDirectory()) {
         bodyText = "ArtiSynth, Git-based development version";
         if (version != null) {
            bodyText += " evolved from release " + version;
         }
      }
      else {
         bodyText = "ArtiSynth, precompiled release";
         if (version != null) {
            bodyText += " " + version;
         }
      }
      JTextArea textArea = new JTextArea();
      textArea.setFont (new Font ("Arial", Font.PLAIN, 18));
      textArea.setText (bodyText);

      // JEditorPane textPane = new JEditorPane();
      // textPane.setEditable (false);
      // textPane.setContentType ("text/html");
      // textPane.setText (
      //    "<html>\n" +
      //    "<body style=\"font-family:ariel;font-size:120%\">\n" +
      //    bodyText +
      //    "</body>\n" +
      //    "</html>");

      textArea.setBorder (BorderFactory.createEmptyBorder (20,20,20,20));
      frame.setContentPane (textArea);
      frame.pack();
      return frame;
   }

   /**
    * Create the model information frame.
    * 
    * @param rootModel root model for which info is to be displayed
    */
   public JFrame createModelInfo (RootModel rootModel) {

      JFrame frame = new JFrame (ABOUT_MODEL_TITLE);

      String bodyText;
      if (rootModel != null) {
         bodyText = rootModel.getAbout();
         if (bodyText == null) {
            bodyText = ABOUT_MODEL_NO_DESC;
         }
      }
      else {
         bodyText = ABOUT_MODEL_NOT_LOADED;
      }

      JTextArea textArea = new JTextArea();
      textArea.setFont (new Font ("Arial", Font.PLAIN, 18));
      //textArea.setColumns (80);
      //textArea.setLineWrap (true);
      textArea.setText (bodyText);

      textArea.setBorder (BorderFactory.createEmptyBorder (20,20,20,20));
      frame.setContentPane (new JScrollPane (textArea));
      frame.pack();
      return frame;
   }

   public JFrame createKeyBindingInfo() {

      JFrame frame = new JFrame (KEYBINDINGS_TITLE);

      JTextArea textArea = new JTextArea();
      textArea.setFont (new Font ("Arial", Font.PLAIN, 16));
      //textArea.setColumns (80);
      //textArea.setLineWrap (true);
      textArea.setText (myMain.getKeyBindings());

      textArea.setBorder (BorderFactory.createEmptyBorder (20,20,20,20));
      frame.setContentPane (new JScrollPane (textArea));
      frame.pack();
      return frame;
   }

   /**
    * update navigation bar
    */

   public void updateNavBar() {
      myNavPanel.loadModel(myMain.getRootModel());

      // content panes must be opaque
      myNavPanel.setOpaque (true);
   }

   public void updateWidgets() {
      myMenuBar.updateTimeDisplay();
   }

   /**
    * update the split pane andreio: updated to reduce number of lines of code
    * 
    */
   public void refreshSplitPane() {
      int loc;
      if (myNavPanel.isExpanded()) {
         loc = NAV_PANEL_WIDTH;
      }
      else {
         loc = myNavPanel.getLeftBorderWidth();
      }
      if (loc != splitPane.getDividerLocation()) {
         splitPane.setDividerLocation (loc);
      }
   }

   /**
    * send notification root model loaded in order to update probes and buttons
    * 
    */
   void notifyRootModelLoaded() {
      updateWorkingDirDisplay();
   }

}
