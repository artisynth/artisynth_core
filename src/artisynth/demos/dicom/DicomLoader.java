package artisynth.demos.dicom;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.renderables.DicomViewer;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.render.GL.GLViewer.BlendFactor;
import maspack.widgets.BooleanSelector;
import maspack.widgets.FileNameField;
import maspack.widgets.StringField;

public class DicomLoader extends RootModel {
   
   DicomViewer viewer;
   FileNameField fnf;
   StringField fnp;
   BooleanSelector fns;
   ControlPanel panel;
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      // nothing, everything done in loader
      
   }
   
   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      driver.getViewer().setBackgroundColor(Color.WHITE);
      driver.getViewer().setBlendDestFactor(BlendFactor.GL_ONE_MINUS_SRC_ALPHA);
      
      createControlPanel();
   }
   
   void createControlPanel() {
      
      panel = new ControlPanel("DICOM controls");
      
      fnf = new FileNameField("File or folder:", 30);
      fnf.getFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fnf.setName("dicom_path");
      panel.addWidget(fnf);
      
      fnp = new StringField("File regex:", 30);
      fnp.setValue(".*");
      panel.addWidget(fnp);
      
      fns = new BooleanSelector("Subdirectories", true);
      panel.addWidget(fns);
      
      // full-width button
      JPanel jpanel = new JPanel();
      jpanel.setLayout(new GridLayout());
      JButton button = new JButton("Load");
      button.setActionCommand("load");
      button.addActionListener(this);
      jpanel.add(button);
      panel.addWidget(jpanel);
      
      
      addControlPanel(panel);
   }
   
   @Override
   public void actionPerformed(ActionEvent event) {
      
      String cmd = event.getActionCommand();
      
      if ("load".equals(cmd)) {
         load();
         return;
      } else {
         super.actionPerformed(event);
      }
   }   
   
   void load() {
      String folder = fnf.getText();
      String pattern = fnp.getText();
      boolean subdirs = fns.getBooleanValue();
      
      if (viewer != null) {
         removeRenderable(viewer);
      }
      
      File file = new File(folder);
      String name = file.getName();
      
      viewer = new DicomViewer(name, file.getAbsolutePath(), Pattern.compile(pattern), subdirs);
      addRenderable(viewer);
      Main.getMain().getViewer().autoFit();
      
   }

}
