/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC) and ArtiSynth
 * Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.workspace;

import java.util.*;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import maspack.render.GLViewer;
import maspack.util.*;
import maspack.widgets.PropertyWindow;
import artisynth.core.modelbase.*;
import artisynth.core.probes.Probe;
import artisynth.core.probes.WayPoint;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.util.*;
import artisynth.core.driver.Main;
import artisynth.core.driver.Scheduler;
import artisynth.core.driver.ViewerManager;
import artisynth.core.gui.*;

import java.io.*;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowAdapter;

/**
 * the purpose of this class is to create a workspace for artisynth to include
 * all the input / output probes / waypoints and render driver for the currently
 * loaded model.
 * 
 * @author andreio
 * 
 */

public class Workspace {
   // protected ArrayList<Probe> myInputProbes = null;
   // protected ArrayList<Probe> myOutputProbes = null;
   // protected WayPointProbe myWayPoints = null;
   protected ViewerManager myViewerManager = null;
   protected RootModel myRoot = null;
   private LinkedList<PropertyWindow> myPropertyWindows =
      new LinkedList<PropertyWindow>();
   private LinkedList<Disposable> myDisposables = new LinkedList<Disposable>();

   private long myLastRenderTime = -1; // time of last render request, msec

   private boolean myRenderRequested = false;
   private boolean myWidgetUpdateRequested = false;
   private UpdateAction myRequestedUpdateAction = null;
   private boolean useNewProbeFileFormat = false;
   private RerenderListener myRerenderListener = new RerenderListener();

   public Workspace() {
      this (null);
   }

   // private void setClosures (DependencyClosure[] closures)
   // {
   // myRoot.setClosures(closures);
   // }

   /**
    * create workspace by creating input and output probes, creating waypoints
    * and creating root model object which will store the currently loaded root
    * model
    * 
    * @param name
    */

   public Workspace (String name) {
      myRoot = new RootModel (name);
   }

   // WS ???
   public void setViewerManager (ViewerManager driver) {
      myViewerManager = driver;
   }

   // WS ???
   public ViewerManager getViewerManager() {
      return myViewerManager;
   }

   private class UpdateAction implements Runnable {
      private boolean myCancel = false;

      private Throwable myStack;

      UpdateAction () {
         // myStack = new Throwable();  was used for debugging 
      }

      private void updateWidgets() {
         for (PropertyWindow w : myPropertyWindows) {
            if (w.isLiveUpdatingEnabled()) {
               w.updateWidgetValues();
            }
         }
         if (myRoot != null) {
            for (ControlPanel p : myRoot.getControlPanels()) {
               if (p.isLiveUpdatingEnabled()) {
                  p.updateWidgetValues();
               }
               p.removeStalePropertyWidgets();
            }
         }
         Main.getTimeline().requestUpdateWidgets();
         Main.getMainFrame().updateWidgets();
      }

      public void run() {
         if (myCancel) {
            return;
         }
         try {
            if (myRenderRequested) {
               //Main.getMain().updateDragger();
               if (myViewerManager != null) {
                  myViewerManager.render();
               }
               myLastRenderTime = System.currentTimeMillis();
            }
            updateWidgets(); // update panels regardless ...
            synchronized (this) {
               myRenderRequested = false;
               myWidgetUpdateRequested = false;
               myRequestedUpdateAction = null;
            }
         }
         catch (Exception e) {
            System.out.println ("Crash in update");
            if (myStack != null) {
               System.out.println ("Stack at time:");
               myStack.printStackTrace(); 
            }
            e.printStackTrace(); 
         }
      }

      public void cancel() {
         myCancel = true;
      }
       
   }

   public long getLastRenderTime() {
      return myLastRenderTime;
   }

     
   private void requestUpdateAction () {
      if (myRequestedUpdateAction == null) {
         myRequestedUpdateAction = new UpdateAction();
         Scheduler scheduler = Main.getScheduler();
         //JythonFrame jframe = Main.getMain().getJythonFrame();
         if (scheduler.requestAction (myRequestedUpdateAction)) {
            //System.out.println ("$sched");
         }
         // The need for this was removed now that all Jython actions
         // execute in a separate thread, leaving Swing free ...
         //
         // else if (jframe != null &&
         //          jframe.requestAction(myRequestedUpdateAction)) {
         //    (new Throwable()).printStackTrace(); 
         //    System.out.println ("$jframe");
         // }
         else {
            SwingUtilities.invokeLater (myRequestedUpdateAction);
            //System.out.println ("$swing");
         }
      }
   }

   public synchronized void rerender() {
      myRenderRequested = true;
      requestUpdateAction ();
   }

   public synchronized void rewidgetUpdate() {
      myWidgetUpdateRequested = true;
      requestUpdateAction ();
   }

   public synchronized void cancelRenderRequests () {
      if (myRequestedUpdateAction != null) {
         myRequestedUpdateAction.cancel();
         myWidgetUpdateRequested = false;
         myRenderRequested = false;
         myRequestedUpdateAction = null;
      }
   }

   /**
    * to read the probe data from a tokenizer
    * 
    * @param rtok
    * @throws IOException
    */

   public void scanProbes (ReaderTokenizer rtok) throws IOException {
      scanProbes (rtok, getRootModel());
   }

   /**
    * to read the probe data from a tokenizer into the root model
    * 
    * @param rtok
    * @param rootModel
    * @throws IOException
    */
   public void scanProbes (ReaderTokenizer rtok, RootModel rootModel)
      throws IOException {
      if (useNewProbeFileFormat) {
         newScanProbes (rtok, rootModel);
      }
      else {
         oldScanProbes (rtok, rootModel);
      }
   }

   private void newScanProbes (ReaderTokenizer rtok, RootModel rootModel)
      throws IOException {
         
      rtok.scanToken ('[');
      while (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
         if (rtok.sval.equals ("inputProbes")) {
            rtok.scanToken ('=');
            ScanWriteUtils.scanfull (rtok, rootModel.getInputProbes(), rootModel);
         }
         else if (rtok.sval.equals ("outputProbes")) {
            rtok.scanToken ('=');
            ScanWriteUtils.scanfull (rtok, rootModel.getOutputProbes(), rootModel);
         }
         else if (rtok.sval.equals ("waypoints")) {
            rtok.scanToken ('=');
            ScanWriteUtils.scanfull (rtok, rootModel.getWayPoints(), rootModel);
         }
         else {
            rtok.scanToken ('=');
            rtok.scanToken ('[');
            // scan and ignore everything between [ ] 
            int level = 1;
            do {
               rtok.nextToken();
               if (rtok.ttype == '[') {
                  level++;
               }
               else if (rtok.ttype == ']') {
                  level--;
               }
            }
            while (level > 0);
         }
      }
      rtok.pushBack();
      rtok.scanToken (']');
   }

   private void oldScanProbes (ReaderTokenizer rtok, RootModel rootModel)
      throws IOException {
      // myInputProbes.clear();
      // myOutputProbes.clear()
      myRoot.removeAllInputProbes();
      myRoot.removeAllOutputProbes();
      myRoot.removeAllWayPoints();
      // myOutputProbes.add(myWayPoints); will be read from file
      // boolean isInvalid = false;

      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsWord()) {
            throw new IOException ("Expecting type identifier for probe, got "
            + rtok);
         }

         Class probeClass = null;
         if ((probeClass = ClassAliases.resolveClass (rtok.sval)) == null) {
            throw new IOException ("Unknown resolve probe class: " + rtok.sval);
         }

         Object probeObj = null;
         if (probeClass == WayPointProbe.class) {
            probeObj = myRoot.getWayPoints();
         }
         else {
            try {
               probeObj = probeClass.newInstance();
            }
            catch (Exception e) {
               throw new IOException ("Can't instantiate probe class "
               + probeClass.getName() + ", line " + rtok.lineno());
            }
         }

         if (probeObj instanceof Probe) {
            if (((Probe)probeObj).isInput()) {
               Probe iprobe = (Probe)probeObj;
               try {
                  ScanWriteUtils.scanfull (rtok, iprobe, rootModel);
                  myRoot.addInputProbe (iprobe);
               }
               catch (IOException e) {
                  String errMsg = "scan probe failed, ";
                  if (rtok.getResourceName() != null) {
                     errMsg += "file=" + rtok.getResourceName() + ", ";
                  }
                  System.err.println (errMsg + e.getMessage());
               }
            }
            else {
               Probe oprobe = (Probe)probeObj;
               try {
                  ScanWriteUtils.scanfull (rtok, oprobe, rootModel);
                  if (!(oprobe instanceof WayPointProbe)) { // waypoint probe is
                                                            // already added
                     myRoot.addOutputProbe (oprobe);
                  }
               }
               catch (IOException e) {
                  String errMsg = "scan probe failed, ";
                  if (rtok.getResourceName() != null) {
                     errMsg += "file=" + rtok.getResourceName() + ", ";
                  }
                  System.err.println (errMsg + e.getMessage());
               }
            }
         }
         else {
            // throw new IOException("Probe object " + probeClass.getName()
            // + " is not a probe, line " + rtok.lineno());
            // isInvalid = true;
         }
      }
   }

   // WS
   public void writeProbes (PrintWriter pw, NumberFormat fmt)
      throws IOException {
      writeProbes (pw, fmt, getRootModel());
   }

   public void writeProbes (
      PrintWriter pw, NumberFormat fmt, RootModel rootModel)
      throws IOException {
      if (useNewProbeFileFormat) {
         newWriteProbes (pw, fmt, rootModel);
      }
      else {
         oldWriteProbes (pw, fmt, rootModel);
      }
   }

   private void newWriteProbes (
      PrintWriter pw, NumberFormat fmt, RootModel rootModel)
      throws IOException {

      if (fmt == null)
         fmt = new NumberFormat ("%g");

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.print ("inputProbes=");
      rootModel.getInputProbes().write (pw, fmt, rootModel);
      pw.print ("outputProbes=");
      rootModel.getOutputProbes().write (pw, fmt, rootModel);
      pw.println ("waypoints=");
      rootModel.getWayPoints().write (pw, fmt, rootModel);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");      
      pw.flush();
   }


   /**
    * write out the probes -- currently written out in a flat list -- should be
    * rewritten to match model file format, i.e. inputProbes = [ ... ]
    * outputProbes = [ ... ] waypoints = [ ... ]
    * 
    * @param pw
    * @param fmt
    * @throws IOException
    */
   // WS
   private void oldWriteProbes (
      PrintWriter pw, NumberFormat fmt, RootModel rootModel) throws IOException {
      if (fmt == null)
         fmt = new NumberFormat ("%g");

      if (myRoot.getInputProbes().size() > 0 ||
          myRoot.getOutputProbes().size() > 0 || 
          myRoot.getWayPoints ().size () > 0) {
         pw.println ("[");
         IndentingPrintWriter.addIndentation (pw, 2);

         // write out the input probes
         for (Probe iprobe : myRoot.getInputProbes()) {
            pw.println (ClassAliases.getAliasOrName (iprobe.getClass()));
            iprobe.write (pw, fmt, rootModel);
         }

         // write out the output probes
         for (Probe oprobe : myRoot.getOutputProbes()) {
            pw.println (ClassAliases.getAliasOrName (oprobe.getClass()));
            oprobe.write (pw, fmt, rootModel);
         }

         // write way points
         pw.println (ClassAliases.getAliasOrName (
                      myRoot.getWayPoints().getClass()));
         myRoot.getWayPoints().write (pw, fmt, myRoot);
         IndentingPrintWriter.removeIndentation (pw, 2);
         pw.println ("]");
      }
      else {
         pw.println ("[ ]");
      }
      pw.flush();
   }

   // WS
   public ComponentList<Probe> getInputProbeList() {
      return myRoot.getInputProbes();
   }

   // WS
   public ComponentList<Probe> getOutputProbeList() {
      return myRoot.getOutputProbes();
   }

   public RootModel getRootModel() {
      return myRoot;
   }

   public void setRootModel (RootModel myNewRoot) {
      myRoot = myNewRoot;
      // getWayPoints().setModel(myNewRoot);
   }

   public ComponentListView<Model> models() {
      return myRoot.models();
   }

   public void advance (double t0, double t1, int flags) {
      myRoot.advance (t0, t1, flags);
   }

   public void initialize (double t) {
      myRoot.initialize (t);
   }

   public boolean rootModelHasState() {
      return myRoot.hasState();
   }

   private class CloseHandler extends WindowAdapter {
      PropertyWindow myWindow;

      public CloseHandler (PropertyWindow w) {
         super();
         myWindow = w;
      }
   };

   private void addCloseHandler (PropertyWindow w) {
      ((Window)w).addWindowListener (new CloseHandler (w) {
         public void windowClosed (WindowEvent e) {
            deregisterWindow (myWindow);
         }
      });
   }

   private void removeCloseHandler (PropertyWindow w) {
      WindowListener[] listeners = ((Window)w).getWindowListeners();
      for (int i = 0; i < listeners.length; i++) {
         if (listeners[i] instanceof CloseHandler) {
            ((Window)w).removeWindowListener (listeners[i]);
         }
      }
   }

   public void registerWindow (PropertyWindow w) {
      myPropertyWindows.add (w);
      w.addGlobalValueChangeListener (myRerenderListener);
      if (myRoot != null) {
         w.setSynchronizeObject (myRoot);
      }
      addCloseHandler (w);
   }

   public boolean deregisterWindow (PropertyWindow w) {
      removeCloseHandler (w);
      w.removeGlobalValueChangeListener (myRerenderListener);
      w.setSynchronizeObject (null);
      return myPropertyWindows.remove (w);
   }

   public void registerDisposable (Disposable w) {
      myDisposables.add (w);
   }

   public boolean deregisterDisposable (Disposable w) {
      return myDisposables.remove (w);
   }

   public void removeDisposables() {
      for (PropertyWindow w : myPropertyWindows) {
         removeCloseHandler (w);
         w.dispose();
      }
      myPropertyWindows.clear();
      // create a tmp list because dispose might want to call
      // deregisterDisposable().
      LinkedList<Disposable> tmpList =
         (LinkedList<Disposable>)myDisposables.clone();
      for (Disposable d : tmpList) {
         d.dispose();
      }
      myDisposables.clear();
      if (myRoot != null) {
         myRoot.dispose();
      }
   }

   public boolean windowIsRegistered (PropertyWindow w) {
      return myPropertyWindows.contains (w);
   }

   /** 
    * For diagnostic purposes.
    */
   public LinkedList<PropertyWindow> getPropertyWindows() {
      LinkedList<PropertyWindow> list = new LinkedList<PropertyWindow>();
      list.addAll (myPropertyWindows);
      return list;
   }

}
