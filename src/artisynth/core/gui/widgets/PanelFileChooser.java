package artisynth.core.gui.widgets;

import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;

import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ExtensionFileFilter;
import artisynth.core.probes.*;
import artisynth.core.probes.Probe.*;
import maspack.widgets.*;
import maspack.properties.*;

/**
 * An extension to JFileChooser that adds a PropertyPanel between the chooser
 * window and the Select/Cancel buttons, which can be used to contain custom
 * widgets.
 *
 * <p>This class depends on the component implementaton of JFileChooser and
 * could break if that implementation changes. If that happens, it will be
 * detected and {@link #panelIsSupported} will return {@code false}.
 */
public class PanelFileChooser extends JFileChooser {

   // last JPanel in the dialog, which is where we place the property panel
   protected JPanel myLastPanel;
   // index where we want to place the panel inside myLastPanel. If there is no
   // property panel, this is the index of the checkbox panel.
   protected int myPropPanelIndex = -1;
   protected PropertyPanel myPropPanel; // property panel

   /**
    * Returns the index of the last JPanel inside a container, or -1 if there
    * are no JPanels.
    */
   private int getLastJPanelIndex (Container comp) {
      for (int i=comp.getComponentCount()-1; i >= 0; i--) {
         if (comp.getComponent(i) instanceof JPanel) {
            return i;
         }
      }
      return -1;
   }   

   /**
    * Returns the property panel, or {@code null} if the panel has not been
    * created or has been removed.
    *
    * @return the property panel
    */
   public PropertyPanel getPropertyPanel() {
      return myPropPanel;
   }

   /**
    * Queries whether the property panel currently exists. Will return {@code
    * false} if the panel has not yet been created or has been removed.
    *
    * @return {@code true} if the propety panel exists
    */
   public boolean hasPropertyPanel() {
      return myPropPanel != null;
   }

   /**
    * Removes the property panel from this dialog. If panel does not currently
    * exists, this method does nothing.
    */
   public void removePropertyPanel() {
      if (myPropPanel != null) {
         myLastPanel.remove (myPropPanelIndex);
         myLastPanel.remove (myPropPanelIndex);
         myPropPanel = null;
      }
   }

   /**
    * Creates and returns the property panel for this dialog. If the panel
    * already exists, this method does nothing and returns {@code null}.
    */
   public PropertyPanel createPropertyPanel() {
      if (myPropPanel == null && myPropPanelIndex != -1) {
         myPropPanel = new PropertyPanel();
         myPropPanel.setAutoRepackEnabled (false); // will repack ourselves
         myPropPanel.setBorder (
            BorderFactory.createEtchedBorder (EtchedBorder.LOWERED));  
         myLastPanel.add (myPropPanel, myPropPanelIndex);
         myLastPanel.add (new Box.Filler(
                             new Dimension(1,10),
                             new Dimension(1,10),
                             new Dimension(1,10)), myPropPanelIndex);        
         return myPropPanel;
      }
      else {
         return null;
      }
   }

   /**
    * Queries whether the property panel is actually supported. If this method
    * returns false, it means that the component of implementation of
    * JFileChooser has changed in a way that is incompatible with the
    * assumptions of this class. In that case, no property panel will be
    * created.
    *
    * @return {@code true} if the property panel is supported.
    */
   public boolean panelIsSupported() {
      return myPropPanelIndex != -1;
   }

   public PanelFileChooser() {
      super();
      // find where to add the panel 
      int lastPanelIndex = getLastJPanelIndex (this);
      if (lastPanelIndex != -1) {
         myLastPanel = (JPanel)getComponent(lastPanelIndex);
         myPropPanelIndex = getLastJPanelIndex (myLastPanel);
      }
   }
}
