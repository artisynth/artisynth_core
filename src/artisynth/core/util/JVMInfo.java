/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.awt.Frame;
import java.io.File;
import java.lang.reflect.Field;

import javax.media.opengl.*;
import javax.media.opengl.awt.GLCanvas;

import java.io.*;

public class JVMInfo {

   private static String osname, ostype, osarch;
   private static boolean isMac, isWin, isJava2, isJava14, isJava15, isLinux;

   static {
      osname = System.getProperty ("os.name");
      osarch = System.getProperty ("os.arch");
      isWin = osname.startsWith ("Windows");
      isMac = !isWin && osname.startsWith ("Mac");
      isLinux = osname.startsWith ("Linux");
      // System.out.println("osname = " + osname);
      // System.out.println("osarch = " + osarch);
      String version = System.getProperty ("java.version").substring (0, 3);
      // JVM on Sharp Zaurus PDA claims to be "3.1"!
      isJava2 = version.compareTo ("1.1") > 0 && version.compareTo ("2.9") <= 0;
      isJava14 =
         version.compareTo ("1.3") > 0 && version.compareTo ("2.9") <= 0;
      isJava15 =
         version.compareTo ("1.4") > 0 && version.compareTo ("2.9") <= 0;

      // see http://lopica.sourceforge.net/os.html
      if (isWindows()) {
         if (osarch.contains ("64")) {
            ostype = "Windows64";
         }
         else {
            ostype = "Windows";
         }
      }
      else if (isLinux()) {
         if (osarch.endsWith ("amd64")) {
            ostype = "Linux64";
         }
         else {
            ostype = "Linux";
         }
      }
      else if (isMacOSX()) {
         ostype = "Darwin-" + osarch;
      }
      else {
         ostype = "";
      }
      // System.out.println("ostype = " + ostype);
   }

   /**
    * Add Default to library-path
    * 
    * @throws NoSuchFieldException
    * @throws SecurityException
    * @throws IllegalAccessException
    * @throws IllegalArgumentException
    */
   public static void appendDefaultLibraryPath()
      throws SecurityException, NoSuchFieldException, IllegalArgumentException,
      IllegalAccessException {
      if (getOSType() != "") {
         String addedPath =
            ArtisynthPath.getHomeDir() + System.getProperty ("file.separator")
            + "lib" + System.getProperty ("file.separator") + getOSType();
         boolean exists = (new File (addedPath)).exists();
         if (exists) {

            addJavaLibraryPath (addedPath);

         }
         else {
            System.err.println ("Default Library Path does not exist:"
            + addedPath);

         }
      }
      else {
         System.err.println ("OS:" + System.getProperty ("os.name")
         + "not supported");
      }
   }

   /**
    * Adds location to current java.library.path.
    * 
    * Original idea from http://forum.java.sun.com/thread.jspa?threadID=627890
    * 
    * Approach uses Java reflection and knowledge of internal classloader
    * mechanism and thus it is a bit dirty solution. Future versions can change
    * behaviour and this may not work. However it's probably the only way how to
    * do it now.
    * 
    * Here is way it is working:
    * 
    * ClassLoader.loadLibrary(Class fromClass, String name, boolean isAbsolute) {
    * ... if (sys_paths == null) { usr_paths =
    * initializePath("java.library.path"); sys_paths =
    * initializePath("sun.boot.library.path"); } ... }
    * 
    * @param newPath
    */
   public static void addJavaLibraryPath (String newPath) {
      // System.out.println("java.library.path=" +
      // System.getProperty("java.library.path"));

      // Reset the "sys_paths" field of the ClassLoader to null.
      Class<ClassLoader> clazz = ClassLoader.class;
      Field field = null;
      try {
         field = clazz.getDeclaredField ("sys_paths");
      }
      catch (SecurityException e) {
         e.printStackTrace();
      }
      catch (NoSuchFieldException e) {
         e.printStackTrace();
      }
      boolean accessible = field.isAccessible();
      if (!accessible)
         field.setAccessible (true);

      try {
         // Reset it to null so that whenever "System.loadLibrary" is
         // called, it will be reconstructed with the changed value.
         field.set (clazz, null);
      }
      catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
      catch (IllegalAccessException e) {
         e.printStackTrace();
      }

      try {
         String current = System.getProperty ("java.library.path");
         // Change the value.
         System.setProperty ("java.library.path", current + File.pathSeparator
         + newPath);
      }
      catch (SecurityException e) {
         e.printStackTrace();
      }
      finally {
         field.setAccessible (accessible);
      }

      // System.out.println("java.library.path=" +
      // System.getProperty("java.library.path"));
   }

   /**
    * Prints content of ClassLoader.usr_path field.
    * 
    * usr_path field is created from java.library.path property, when
    * ClassLoader.sys_path == null.
    * 
    * usr_path field contains array of paths, where ClassLoader is searching for
    * native libraries.
    * 
    */
   public static void printUsrPathField() {
      Class<ClassLoader> clazz = ClassLoader.class;
      Field field = null;
      boolean accessible = false;
      try {
         try {
            field = clazz.getDeclaredField ("usr_paths");
            accessible = field.isAccessible();
         }
         catch (SecurityException e) {
            e.printStackTrace();
         }
         catch (NoSuchFieldException e) {
            e.printStackTrace();
         }

         if (!accessible) {
            field.setAccessible (true);
         }
         String[] sys_paths = (String[])field.get (clazz);
         for (int i = 0; i < sys_paths.length; i++) {
            System.out.println (sys_paths[i]);
         }
      }
      catch (IllegalArgumentException e1) {
         e1.printStackTrace();
      }
      catch (IllegalAccessException e1) {
         e1.printStackTrace();
      }
      finally {
         field.setAccessible (accessible);
      }
   }

   /** Returns machine type of the System: Windows, Linux, Darwin. */
   public static String getOSType() {
      return ostype;
   }

   /** Returns true if this machine is a Macintosh. */
   public static boolean isMacintosh() {
      return isMac;
   }

   /** Returns true if this machine is a Macintosh running OS X. */
   public static boolean isMacOSX() {
      return isMacintosh() && isJava2();
   }

   /** Returns true if this machine is running Windows. */
   public static boolean isWindows() {
      return isWin;
   }

   /** Returns true if ImageJ is running on Java 2. */
   public static boolean isJava2() {
      return isJava2;
   }

   /** Returns true if ImageJ is running on a Java 1.4 or greater JVM. */
   public static boolean isJava14() {
      return isJava14;
   }

   /** Returns true if ImageJ is running on a Java 1.5 or greater JVM. */
   public static boolean isJava15() {
      return isJava15;
   }

   /** Returns true if ImageJ is running on Linux. */
   public static boolean isLinux() {
      return isLinux;
   }

   /** Prints system info */
   public static void main (String[] args) {
      Runtime rt = Runtime.getRuntime();
      System.out.print ("OS " + System.getProperty ("os.name") + ", ");
      System.out.print ("Version " + System.getProperty ("os.version") + ", ");
      System.out.print ("Architecture " + System.getProperty ("os.arch") + ", ");
      System.out.println ("Processors " + rt.availableProcessors());

      // System.out.print("Vendor " + System.getProperty("java.vendor")+", ");
      // System.out.println("Version " + System.getProperty("java.version"));
      System.out.print ("JVM " + System.getProperty ("java.vm.name") + ", ");
      System.out.print ("Version " + System.getProperty ("java.vm.version")
      + ", ");
      System.out.println ("Vendor " + System.getProperty ("java.vm.vendor"));

      System.out.print ("Memory total: " + rt.totalMemory() + ", ");
      System.out.print ("max: " + rt.maxMemory() + ", ");
      System.out.println ("free: " + rt.freeMemory());

      if (isWindows()) {
         System.out.println ("Cpuspeed: " + getCPUSpeed());
      }

      // printglinfo();
   }

   // ** Query Cpu speed for windows NT or XP */
   private static final String REGQUERY_UTIL = "reg query ";
   // private static final String REGSTR_TOKEN = "REG_SZ";
   private static final String REGDWORD_TOKEN = "REG_DWORD";
   private static final String CPU_SPEED_CMD =
      REGQUERY_UTIL
      + "\"HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0\""
      + " /v ~MHz";

   public static String getCPUSpeed() {
      try {
         Process process = Runtime.getRuntime().exec (CPU_SPEED_CMD);
         StreamReader reader = new StreamReader (process.getInputStream());

         reader.start();
         process.waitFor();
         reader.join();

         String result = reader.getResult();
         int p = result.indexOf (REGDWORD_TOKEN);

         if (p == -1)
            return null;

         // CPU speed in Mhz (minus 1) in HEX notation, convert it to DEC
         String temp = result.substring (p + REGDWORD_TOKEN.length()).trim();
         return Integer.toString ((Integer.parseInt (temp.substring ("0x"
            .length()), 16) + 1));
      }
      catch (Exception e) {
         return null;
      }
   }

   static class StreamReader extends Thread {
      private InputStream is;
      private StringWriter sw;

      StreamReader (InputStream is) {
         this.is = is;
         sw = new StringWriter();
      }

      public void run() {
         try {
            int c;
            while ((c = is.read()) != -1)
               sw.write (c);
         }
         catch (IOException e) {
            ;
         }
      }

      String getResult() {
         return sw.toString();
      }
   }

   /** Prints opengl info and exists */
   public static void printglinfo() {
      Frame frame = new Frame();
      GLProfile glp = GLProfile.get(GLProfile.GL2);
      GLCanvas canvas = new GLCanvas (new GLCapabilities(glp));
      canvas.addGLEventListener (new Listener());
      frame.setUndecorated (true);
      frame.add (canvas);
      frame.setSize (1, 1);
      frame.setVisible (true);
   }

   static class Listener implements GLEventListener {
      public void init (GLAutoDrawable drawable) {
         GL2 gl = drawable.getGL().getGL2();
         System.out.println ("GL vendor: " + gl.glGetString (GL2.GL_VENDOR));
         System.out.println ("GL version: " + gl.glGetString (GL2.GL_VERSION));
         System.out.println ("GL renderer: " + gl.glGetString (GL2.GL_RENDERER));
         // System.out.println("GL extensions:");
         // String[] extensions = gl.glGetString(GL2.GL_EXTENSIONS).split(" ");
         // int i = 0;
         // while (i < extensions.length) {
         // System.out.println(extensions[i++]);
         // }
         runExit();
      }

      public void display (GLAutoDrawable drawable) {
      }

      public void reshape (GLAutoDrawable drawable, int x, int y, int w, int h) {
      }

      public void displayChanged (
         GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
      }

      @Override
      public void dispose(GLAutoDrawable arg0) {
         // XXX nothing?
      }
   }

   private static void runExit() {
      // Note: calling System.exit() synchronously inside the draw,
      // reshape or init callbacks can lead to deadlocks on certain
      // platforms (in particular, X11) because the JAWT's locking
      // routines cause a global AWT lock to be grabbed. Run the
      // exit routine in another thread.
      new Thread (new Runnable() {
         public void run() {
            System.exit (0);
         }
      }).start();

   }
}
