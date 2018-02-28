/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.Color;
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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
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

   /** frame help instance allocated only once */
   private FrameHelp frameHelp = null;

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

   private static int maxDirDisplayChars = 48;

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
      GLPanel = new GLViewerPanel (width, height, myMain.getGLVersion());

      myNavPanel = new NavigationPanel();
      myNavPanel.setLayout (new FlowLayout (FlowLayout.LEFT));

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

      JScrollPane navScrollPane = new JScrollPane (myNavPanel);
      navScrollPane.setVerticalScrollBarPolicy (
         JScrollPane.VERTICAL_SCROLLBAR_NEVER);
      navScrollPane.setHorizontalScrollBarPolicy (
         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      myNavPanel.setParentScrollBar (navScrollPane);

      navScrollPane.setOpaque (true);

      // JPanel panel = new JPanel();
      // panel.setSize (400,200);
      // panel.setBackground (Color.BLUE);

      splitPane =
         new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, navScrollPane, GLPanel);
      
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

      // create one instance of help frame
      frameHelp = new FrameHelp();

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
      GLPanel.setSize (new Dimension(w, h));
      pack();
      if (isVisible()) {
         // Sometimes the layout manager doesn't give us the size
         // we want, so we do a marginal adjustmentto compensate
         Dimension dim = GLPanel.getSize();
         if (dim.width != w || dim.height != h) {
            Dimension windim = getSize();
            windim.width -= (dim.width - w);
            windim.height -= (dim.height - h);
            setSize (windim);
            // probably should call pack() here, but that may mung the size yet
            // again.
         }
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

   public void displayKeybindings() {
      frameHelp.setFrameTitleAndText (
         KEYBINDINGS_TITLE, myMain.getKeyBindings());
      frameHelp.setVisible (true);
   }

   public String getArtiSynthVersion() {
      if (myArtiSynthVersion == null) {
         try {
            BufferedReader reader = 
               new BufferedReader (new FileReader (
                  ArtisynthPath.getHomeRelativeFile ("VERSION", ".")));
            myArtiSynthVersion = reader.readLine();
            reader.close();
         }
         catch (IOException e) {
            myArtiSynthVersion = 
               "Version unknown: " + 
               "can't find or read VERSION file in install directory";
         }
      }
      return myArtiSynthVersion;
   }
   
   /**
    * display about artisynth dialog andreio: fixed memory allocation issues
    * 
    */
   public void displayAboutArtisynth() {
      
      frameHelp.setFrameTitleAndText (
         ABOUT_ARTISYNTH_TITLE, getArtiSynthVersion());
      frameHelp.setVisible (true);
   }

   /**
    * display about model dialog
    * 
    * @param rootModel root model for which dialog is to be displayed
    */
   public void displayAboutModel (RootModel rootModel) {
      String rootModelDescription;
      if (rootModel != null) {
         rootModelDescription = rootModel.getAbout();
         if (rootModelDescription == null)
            rootModelDescription = ABOUT_MODEL_NO_DESC;
      }
      else
         rootModelDescription = ABOUT_MODEL_NOT_LOADED;

      frameHelp.setFrameTitleAndText (ABOUT_MODEL_TITLE, rootModelDescription);
      frameHelp.setVisible (true);
   }

   /**
    * update navigation bar
    */

   public void updateNavBar() {
      myNavPanel.unloadModel();
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
      splitPane.setDividerLocation ((myNavPanel.getStatus()) ? NAV_PANEL_WIDTH
         : 0);
   }

   /**
    * send notification root model loaded in order to update probes and buttons
    * 
    */
   void notifyRootModelLoaded() {
      updateWorkingDirDisplay();
   }

   /**
    * 
    * @author andreio display frame help refactored by andrei on may 24 / 2006
    * 
    */

   private class FrameHelp extends JFrame {
      private static final long serialVersionUID = 1L;

      private static final int BOUND_X = 250, BOUND_Y = 250, BOUND_WIDTH = 500,
      BOUND_HEIGHT = 400;

      /** text area with information contents */
      JTextArea textAreaHelp;
      JScrollPane scrollPane;

      /**
       * default constructor instance
       * 
       */

      public FrameHelp() {
         this ("", "");
      }

      /**
       * create the information dialog andreio: reduced number of needless
       * function calls andreio: removed number of memory allocations
       * 
       * @param title -
       * title of the frame
       * @param helpText -
       * text to fill in the frame text
       */

      public FrameHelp (String title, String helpText) {
         addNotify();
         setTitle (title);
         setBounds (BOUND_X, BOUND_Y, BOUND_WIDTH, BOUND_HEIGHT);
         textAreaHelp = new JTextArea();
         textAreaHelp.setEditable (false);
         textAreaHelp.setText (helpText);
         scrollPane = new JScrollPane (textAreaHelp);
         getContentPane().add (scrollPane);
         textAreaHelp.setLineWrap (true);
         textAreaHelp.setWrapStyleWord (true);
         pack();
         setBounds (BOUND_X, BOUND_Y, BOUND_WIDTH, BOUND_HEIGHT);
         this.setAlwaysOnTop (true);
      }

      /**
       * set the frame title and text in one function call, to make it more
       * efficient
       * 
       * @param title -
       * title of the frame
       * @param helpText -
       * the help text of the frame
       */

      public void setFrameTitleAndText (String title, String helpText) {
         setFrameTitle (title);
         setFrameText (helpText);
      }

      /**
       * set the frame title
       */
      public void setFrameTitle (String title) {
         this.setTitle (title);
      }

      /**
       * Set the help text
       * 
       * @param helpText help text
       */
      public void setFrameText (String helpText) {
         textAreaHelp.setText (helpText);
      }
   }
}
