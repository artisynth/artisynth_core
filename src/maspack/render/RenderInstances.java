package maspack.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.util.Clonable;
import maspack.util.Disposable;
import maspack.util.DisposeObservable;
import maspack.util.DisposeObserver.DisposeObserverImpl;
import maspack.util.DynamicIntArray;
import maspack.util.Versioned;

/**
 * Object containing information for repetitively drawing instances of {@link RenderObject}s, with
 * varying transforms:
 * <ul>
 * <li><em>point</em>, a simple 3 degree-of-freedom translation
 * <li><em>frame</em>, a rigid 6 degree-of-freedom transform 
 * <li><em>affine</em>, a non-rigid 12 degree-of-freedom transform
 * </ul>
 *
 */
public class RenderInstances implements Versioned, DisposeObservable, Disposable {

   private static int nextIdNumber = 0;
   
   public static enum InstanceTransformType {
      POINT, // scale, translation
      FRAME,  // scale, rigid transform (translation + rotation)
      AFFINE  // scale, affine (translation + affine stretch/skew) 
   }
   
   public static class RenderInstancesIdentifier extends DisposeObserverImpl {
      private int id;
      
      public RenderInstancesIdentifier(int id) {
         this.id = id;
      }
      
      /**
       * Unique identifying number
       */
      public int getId() {
         return id;
      }
      
      @Override
      protected void dispose() {
         super.dispose();
      }

      public boolean isValid() {
         return !isDisposed();
      }
      
   }
   
   /**
    * Keeps track of versions for detecting changes.  Can be
    * cloned so renderers can keep track of the latest versions
    * they have observed.
    */
   public static class RenderInstancesVersion implements Clonable {
      
      private int pointsVersion;
      private int framesVersion;
      private int affinesVersion;
      private int instancesVersion;
      
      private int colorsVersion;
      private int scalesVersion;
      
      private int totalVersion;
      
      protected RenderInstancesVersion() {
         pointsVersion = 0;
         framesVersion = 0;
         affinesVersion = 0;
         instancesVersion = 0;
         colorsVersion = 0;
         scalesVersion = 0;         
         totalVersion = 0;
      }
      
      public int getPointsVersion() {
         return pointsVersion;
      }
      
      public int getColorsVersion() {
         return colorsVersion;
      }
      
      public int getScalesVersion() {
         return scalesVersion;
      }
      
      public int getFramesVersion() {
         return framesVersion;
      }
      
      public int getAffinesVersion() {
         return affinesVersion;
      }
      
      public int getInstancesVersion() {
         return instancesVersion;
      }
      
      public int getVersion() {
         return totalVersion;
      }
      
      @Override
      public RenderInstancesVersion clone() {
         RenderInstancesVersion c;
         try {
            c = (RenderInstancesVersion)super.clone();
         } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
         }
         return c;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + pointsVersion;
         result = prime * result + framesVersion;
         result = prime * result + affinesVersion;
         result = prime * result + instancesVersion;
         result = prime * result + colorsVersion;
         result = prime * result + scalesVersion;
         result = prime * result + totalVersion;
         return result;
      }
      
      public boolean equals(RenderInstancesVersion v) {
         if (  totalVersion != v.totalVersion
            || pointsVersion != v.pointsVersion
            || framesVersion != v.framesVersion
            || affinesVersion != v.affinesVersion
            || instancesVersion != v.instancesVersion 
            || colorsVersion != v.colorsVersion
            || scalesVersion != v.scalesVersion
            ) {
            return false;
         }
         return true;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null || getClass() != obj.getClass()) {
            return false;
         }
         RenderInstancesVersion other = (RenderInstancesVersion)obj;
         return equals(other);
      }      
   }
   
   /**
    * Stores exposable state of the object, tracking the 
    * current primitive group indices.
    * 
    */
   public static class RenderInstancesState implements Clonable {     
      int numPoints;
      int numFrames;
      int numAffines;
      int numInstances;
      int numColors;
      int numScales;
      int numPointInstances;
      int numFrameInstances;
      int numAffineInstances;
      
      protected RenderInstancesState() {
         clear();
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + numPoints;
         result = prime * result + numFrames;
         result = prime * result + numAffines;
         result = prime * result + numInstances;
         result = prime * result + numColors;
         result = prime * result + numScales;
         result = prime * result + numPointInstances;
         result = prime * result + numFrameInstances;
         result = prime * result + numAffineInstances;
         return result;
      }

      public boolean equals(RenderInstancesState other) {
         if ( numPoints != other.numPoints
            || numFrames != other.numFrames
            || numAffines != other.numAffines
            || numInstances != other.numInstances
            || numColors != other.numColors
            || numScales != other.numScales
            || numPointInstances != other.numPointInstances
            || numFrameInstances != other.numFrameInstances
            || numAffineInstances != other.numAffineInstances
            ) {
            return false;
         }
         return true;
      }
      
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null || getClass() != obj.getClass()) {
            return false;
         }
         RenderInstancesState other = (RenderInstancesState)obj;
         return equals(other);
      }
      
      public RenderInstancesState clone() {
         RenderInstancesState c;
         try {
            c = (RenderInstancesState)super.clone();
         } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
         }
         return c;
      }

      public boolean hasPoints() {
         return numPoints > 0;
      }
    
      public boolean hasFrames() {
         return numFrames > 0;
      }
      
      public boolean hasAffines() {
         return numAffines > 0;
      }
      
      public boolean hasInstances() {
         return numInstances > 0;
      }
      
      public boolean hasPointInstances() {
         return numPointInstances > 0;
      }
      
      public boolean hasFrameInstances() {
         return numFrameInstances > 0;
      }
      
      public boolean hasAffineInstances() {
         return numAffineInstances > 0;
      }
      
      public boolean hasColors() {
         return numColors > 0;
      }
      
      public boolean hasScales() {
         return numScales > 0;
      }

      protected void clear() {
         numPoints = 0;
         numFrames = 0;         
         numAffines = 0;
         numInstances = 0;
         numColors = 0;
         numScales = 0;
         numPointInstances = 0;
         numFrameInstances = 0;
         numAffineInstances = 0;
      }
      
   }
   
   RenderInstancesVersion versionInfo;
   RenderInstancesState stateInfo;
     
   ArrayList<float[]> points;
   ArrayList<RigidTransform3d> frames;
   ArrayList<AffineTransform3d> affines;   
   ArrayList<byte[]> colors;
   int currentColorIdx = -1;
   ArrayList<Double> scales;
   int currentScaleIdx = -1;
   
   DynamicIntArray instances; // type, transformIdx, scale index, color index

   // indicators that attributes have been modified
   volatile boolean pointsModified;
   volatile boolean framesModified;
   volatile boolean affinesModified;
   volatile boolean colorsModified;
   volatile boolean scalesModified;
   volatile boolean instancesModified;
   volatile boolean totalModified;
   
   ReentrantReadWriteLock lock;
   
   RenderInstancesIdentifier rid;
      
   public RenderInstances() {
      rid = new RenderInstancesIdentifier(nextIdNumber++);
      versionInfo = createVersionInfo();
      stateInfo = createStateInfo();   
      lock = new ReentrantReadWriteLock();
      clearAll();
   }
   
   protected RenderInstancesVersion createVersionInfo() {
      return new RenderInstancesVersion();
   }
   
   protected RenderInstancesState createStateInfo() {
      return new RenderInstancesState();
   }
   
   /**
    * Prevents modifications until object is unlocked
    */
   public void readLock() {
      lock.readLock().lock();
   }
   
   /**
    * Release a read lock on the object, allowing modifications.
    */
   public void readUnlock() {
      lock.readLock().unlock();
   }
   
   /**
    * Prevents reading until object is unlocked
    */
   public void writeLock() {
      lock.writeLock().lock();
   }
   
   /**
    * Release a write lock on the object, allowing reads
    */
   public void writeUnlock() {
      lock.writeLock().unlock();
   }
   
   /**
    * Returns a special identifier for this
    * RenderInstances object.  It contains a unique ID number, as well as a flag
    * for determining whether the object still persists and is valid.
    * This should be as the key in HashMaps etc... so that the original
    * RenderObject can be cleared and garbage-collected when it runs out 
    * of scope. 
    */
   public RenderInstancesIdentifier getIdentifier() {
      return rid;
   }
   
   /**
    * Returns an immutable copy of all version information in this RenderInstances,
    * safe for sharing between threads.  This can be used to detect whether
    * the RenderObject has been modified since last observed.
    */
   public RenderInstancesVersion getVersionInfo() {
      getVersion(); // trigger update of all version numbers
      RenderInstancesVersion v = versionInfo.clone();
      return v;
   }
   
   public RenderInstancesState getStateInfo() {
      RenderInstancesState s = stateInfo.clone();
      return s;
   }

   //=========================================================================
   // Transforms, Colors, Scales
   //=========================================================================
   /**
    * Hint for ensuring sufficient storage
    * @param cap capacity
    */
   public void ensurePointCapacity(int cap) {
      writeLock();
      if (points == null) {
         points = new ArrayList<>();
      }
      points.ensureCapacity (cap);
      writeUnlock();
   }
   
   /**
    * Adds a new point that can be referenced to create
    * an instance.  Note that this does not create a
    * new instance.
    * @param pos position of point added, by reference
    * @return index of the new point added
    */
   public int addPoint(float[] pos) {
      writeLock();
      int pidx = addPointInternal(pos);
      writeUnlock();
      return pidx;
   }
   
   /**
    * Adds a new point that can be referenced to create
    * an instance.  Note that this does not create a
    * new instance.
    * @param pos position of point added
    * @return index of the new point added
    */
   public int addPoint(Vector3d pos) {
      return addPoint(pos.x, pos.y, pos.z);
   }
   
   private int addPointInternal(float[] pos) {
      int pidx = stateInfo.numPoints;
      if (points == null) {
         points = new ArrayList<>();
      }
      points.add(pos);
      ++stateInfo.numPoints;
      notifyPointsModifiedInternal();
      return pidx;
   }
   
   /**
    * Adds a new point that can be referenced to create
    * an instance.  Note that this does not create a
    * new instance.
    * 
    * @param x x-coordinate
    * @param y y-coordinate
    * @param z z-coordinate
    * @return index of new point
    */
   public int addPoint(double x, double y, double z) {
      return addPoint(new float[] {(float)x, (float)y, (float)z});
   }
   
   /**
    * Sets the point position
    * @param pidx index of point to modify
    * @param x x-coordinate
    * @param y y-coordinate
    * @param z z-coordinate
    */
   public void setPoint(int pidx, double x, double y, double z) {
      writeLock();
      float[] pos = points.get(pidx);
      pos[0] = (float)x;
      pos[1] = (float)y;
      pos[2] = (float)z;
      notifyPointsModified();
      writeUnlock();
   }
   
   /**
    * Sets the point position by reference
    */
   public void setPoint(int pidx, float[] pos) {
      writeLock();
      points.set(pidx, pos);
      notifyPointsModified();
      writeUnlock();
   }
   
   /**
    * Sets the point position
    */
   public void setPoint(int pidx, Vector3d pos) {
      setPoint(pidx, pos.x, pos.y, pos.z);
   }
   
   /**
    * Retrieves the point stored with supplied index
    * @param pidx index of point
    * @return point, by reference
    */
   public float[] getPoint(int pidx) {
      if (pidx < 0) {
         return null;
      }
      return points.get(pidx);
   }
   
   private void notifyPointsModifiedInternal() {
      pointsModified = true;
      totalModified = true;
   }
   
   /**
    * Indicate that the positions have been modified.
    */
   public void notifyPointsModified() {
      writeLock();
      notifyPointsModifiedInternal ();
      writeUnlock();
   }
   
   /**
    * Whether or not any points have been defined.
    */
   public boolean hasPoints() {
      if (points == null) {
         return false;
      }
      return (points.size () > 0);
   }

   /**
    * Number of points defined
    */
   public int numPoints() {
      return stateInfo.numPoints;
   }

   
   /**
    * Retrieves the full list of Points.  This list should not
    * be modified.  
    * 
    * @return list of points.
    */
   public List<float[]> getPoints() {
      return Collections.unmodifiableList(points);
   }

   /**
    * Returns the latest points version number,
    * for use in detecting if changes are present.
    */
   public int getPointsVersion() {
      if (pointsModified) {
         versionInfo.pointsVersion++;
         pointsModified = false;
      }
      return versionInfo.pointsVersion;
   }
   
   
   /**
    * Hint for ensuring sufficient storage
    * @param cap capacity
    */
   public void ensureFrameCapacity(int cap) {
      writeLock();
      if (frames == null) {
         frames = new ArrayList<>();
      }
      frames.ensureCapacity (cap);
      writeUnlock();
   }
   
   /**
    * Adds a new frame that can be referenced to create
    * an instance.  Note that this does not create a
    * new instance.
    * @param frame frame to be added, by reference
    * @return index of the new frame added
    */
   public int addFrame(RigidTransform3d frame) {
      writeLock();
      int pidx = addFrameInternal(frame);
      writeUnlock();
      return pidx;
   }
   
   private int addFrameInternal(RigidTransform3d frame) {
      int pidx = stateInfo.numFrames;
      if (frames == null) {
         frames = new ArrayList<>();
      }
      frames.add(frame);
      ++stateInfo.numFrames;
      notifyFramesModifiedInternal();
      return pidx;
   }
   
   /**
    * Sets the frame position by reference
    * @param fidx frame index
    * @param frame frame pose
    */
   public void setFrame(int fidx, RigidTransform3d frame) {
      writeLock();
      frames.set(fidx, frame);
      notifyFramesModified();
      writeUnlock();
   }
   
   /**
    * Retrieves the frame stored with supplied index
    * @param fidx index of frame
    * @return frame, by reference
    */
   public RigidTransform3d getFrame(int fidx) {
      if (fidx < 0) {
         return null;
      }
      return frames.get(fidx);
   }
   
   private void notifyFramesModifiedInternal() {
      framesModified = true;
      totalModified = true;
   }
   
   /**
    * Indicate that the positions have been modified.
    */
   public void notifyFramesModified() {
      writeLock();
      notifyFramesModifiedInternal ();
      writeUnlock();
   }
   
   /**
    * Whether or not any frames have been defined.
    */
   public boolean hasFrames() {
      if (frames == null) {
         return false;
      }
      return (frames.size () > 0);
   }

   /**
    * Number of frames defined
    */
   public int numFrames() {
      return stateInfo.numFrames;
   }

   
   /**
    * Retrieves the full list of Frames.  This list should not
    * be modified.  
    * 
    * @return list of frames.
    */
   public List<RigidTransform3d> getFrames() {
      return Collections.unmodifiableList(frames);
   }

   /**
    * Returns the latest frames version number,
    * for use in detecting if changes are present.
    */
   public int getFramesVersion() {
      if (framesModified) {
         versionInfo.framesVersion++;
         framesModified = false;
      }
      return versionInfo.framesVersion;
   }
   
   /**
    * Hint for ensuring sufficient storage
    * @param cap capacity
    */
   public void ensureAffineCapacity(int cap) {
      writeLock();
      if (affines == null) {
         affines = new ArrayList<>();
      }
      affines.ensureCapacity (cap);
      writeUnlock();
   }
   
   /**
    * Adds a new affine transform that can be referenced to create
    * an instance.  Note that this does not create a
    * new instance.
    * @param affine affine transform to add, by reference
    * @return index of the new affine added
    */
   public int addAffine(AffineTransform3d affine) {
      writeLock();
      int pidx = addAffineInternal(affine);
      writeUnlock();
      return pidx;
   }
   
   private int addAffineInternal(AffineTransform3d affine) {
      int pidx = stateInfo.numAffines;
      if (affines == null) {
         affines = new ArrayList<>();
      }
      affines.add(affine);
      ++stateInfo.numAffines;
      notifyAffinesModifiedInternal();
      return pidx;
   }
   
   /**
    * Sets the affine position by reference
    * @param aidx affine index
    * @param affine affine transform
    */
   public void setAffine(int aidx, AffineTransform3d affine) {
      writeLock();
      affines.set(aidx, affine);
      notifyAffinesModified();
      writeUnlock();
   }
   
   /**
    * Retrieves the affine stored with supplied index
    * @param pidx index of affine
    * @return affine, by reference
    */
   public AffineTransform3d getAffine(int pidx) {
      if (pidx < 0) {
         return null;
      }
      return affines.get(pidx);
   }
   
   private void notifyAffinesModifiedInternal() {
      affinesModified = true;
      totalModified = true;
   }
   
   /**
    * Indicate that the positions have been modified.
    */
   public void notifyAffinesModified() {
      writeLock();
      notifyAffinesModifiedInternal ();
      writeUnlock();
   }
   
   /**
    * Whether or not any affines have been defined.
    */
   public boolean hasAffines() {
      if (affines == null) {
         return false;
      }
      return (affines.size () > 0);
   }

   /**
    * Number of affines defined
    */
   public int numAffines() {
      return stateInfo.numAffines;
   }

   
   /**
    * Retrieves the full list of Affines.  This list should not
    * be modified.  
    * 
    * @return list of affines.
    */
   public List<AffineTransform3d> getAffines() {
      return Collections.unmodifiableList(affines);
   }

   /**
    * Returns the latest affines version number,
    * for use in detecting if changes are present.
    */
   public int getAffinesVersion() {
      if (affinesModified) {
         versionInfo.affinesVersion++;
         affinesModified = false;
      }
      return versionInfo.affinesVersion;
   }
   
   /**
    * Hint for ensuring sufficient storage
    * @param cap capacity
    */
   public void ensureColorCapacity(int  cap) {
      writeLock();
      if (colors == null) {
         colors = new ArrayList<>();
      }
      colors.ensureCapacity (cap);
      writeUnlock();
   }
   
   /**
    * Adds an indexable color
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    * @return the index of the color added
    */
   public int addColor(byte r, byte g, byte b, byte a) {
      return addColor(new byte[]{r,g,b,a});
   }
   
   /**
    * Adds an indexable color
    * @param r red [0-255]
    * @param g green [0-255]
    * @param b blue [0-255]
    * @param a alpha [0-255]
    * @return the index of the color added
    */
   public int addColor(int r, int g, int b, int a) {
      return addColor(new byte[]{(byte)r,(byte)g,(byte)b,(byte)a});
   }
   
   /**
    * Adds an indexable color
    * @param r red [0-1]
    * @param g green [0-1]
    * @param b blue  [0-1]
    * @param a alpha  [0-1]
    * @return the index of the color added
    */
   public int addColor(float r, float g, float b, float a) {
      return addColor(new byte[]{
         (byte)(255*r),(byte)(255*g),(byte)(255*b),(byte)(255*a)});
   }

   /**
    * Adds an indexable color.
    * @param rgba 4-float vector
    * @return the index of the color added
    */
   public int addColor(float[] rgba) {
      float alpha = 1f;
      if (rgba.length > 3) {
         alpha = rgba[3];
      }
      return addColor(new byte[]{
         (byte)(255*rgba[0]),(byte)(255*rgba[1]),(byte)(255*rgba[2]),(byte)(255*alpha)});
   }
   
   /**
    * Adds an indexable color
    * @param color color from which RGBA values are determines
    */
   public int addColor(Color color) {
      return addColor(
         new byte[]{
                    (byte)color.getRed(), (byte)color.getGreen(), 
                    (byte)color.getBlue(), (byte)color.getAlpha()});
   }
   
   /**
    * Adds an indexable color by reference.  If the color is modified
    * outside of this object, then {@link #notifyColorsModified()} must
    * be called.  Otherwise, renderers are free to assume the render object
    * has not changed.
    * @param rgba {red, green, blue, alpha}
    * @return the index of the color added
    */
   public int addColor(byte[] rgba) {
      writeLock();
      int cidx = stateInfo.numColors;
      if (colors == null) {
         colors = new ArrayList<>();
      }
      colors.add (rgba);
      stateInfo.numColors++;
      currentColorIdx = cidx;
      notifyColorsModifiedInternal ();
      writeUnlock();
      return cidx;
   }
   
   private void notifyColorsModifiedInternal() {
      colorsModified = true;
      totalModified = true;
   }
   
   public void notifyColorsModified() {
      writeLock();
      notifyColorsModifiedInternal ();
      writeUnlock();
   }
   
   /**
    * Sets the current color to be used in following instances
    * based on color index.
    * @param cidx index of a previously added color
    */
   public void setCurrentColor(int cidx) {
      if (cidx >= 0) {
         if (cidx >= colors.size()) {
            throw new IllegalArgumentException (
               "Color "+cidx+" is not defined");
         }
         currentColorIdx = cidx;
      }
      else {
         currentColorIdx = -1;
      }
   }
   
   /**
    * Returns the index associated with the current color, or -1
    * if there is no current color.
    * 
    * @return current color index
    */
   public int getCurrentColor() {
      return currentColorIdx;
   }

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public void setColor(int cidx, byte r, byte g, byte b, byte a) {
      setColor(cidx, new byte[]{r,g,b,a});
   }

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public void setColor(int cidx, int r, int g, int b, int a) {
      setColor(cidx, (byte)r, (byte)g, (byte)b, (byte)a);
   }

   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public void setColor(int cidx, float r, float g, float b, float a) {
      setColor(cidx, (byte)(255*r), (byte)(255*g), (byte)(255*b), (byte)(255*a));
   }
   
   /**
    * Updates the values of the color with index cidx.
    * @param cidx color to modify
    * @param color new color values
    */
   public void setColor(int cidx, Color color) {
      setColor(cidx, 
         new byte[]{
                    (byte)color.getRed(), (byte)color.getGreen(), 
                    (byte)color.getBlue(), (byte)color.getAlpha()});
   }
   
   /**
    * Updates the values of the color, by reference, with index cidx.
    * @param cidx color to modify
    * @param rgba {red, green, blue, alpha}
    */
   public void setColor(int cidx, byte[] rgba) {
      writeLock();
      colors.set(cidx, rgba);
      notifyColorsModifiedInternal();
      writeUnlock();
   }

   /**
    * Retrieves the color at the supplied index.  If the returned color
    * is modified, then {@link #notifyColorsModified()} must be manually called.
    * @param cidx color index
    * @return color {red, green, blue, alpha}
    */
   public byte[] getColor(int cidx) {
      byte[] c = getColorInternal (cidx);
      return c;
   }
   
   private byte[] getColorInternal(int cidx) {
      if (cidx < 0) {
         return null;
      }
      return colors.get(cidx);
   }

   /**
    * Whether or not any colors have been defined.
    */
   public boolean hasColors() {
      if (colors == null) {
         return false;
      }
      return (colors.size () > 0);
   }

   /**
    * Number of colors defined
    */
   public int numColors() {
      return stateInfo.numColors;
   }

   
   /**
    * Retrieves the full list of Colors.  This list should not
    * be modified.  
    * 
    * @return list of colors.
    */
   public List<byte[]> getColors() {
      return Collections.unmodifiableList(colors);
   }

   /**
    * Returns the latest colors version number,
    * for use in detecting if changes are present.
    */
   public int getColorsVersion() {
      if (colorsModified) {
         versionInfo.colorsVersion++;
         colorsModified = false;
      }
      return versionInfo.colorsVersion;
   }
   
   
   
   /**
    * Hint for ensuring sufficient storage
    * @param cap capacity
    */
   public void ensureScaleCapacity(int  cap) {
      writeLock();
      if (scales == null) {
         scales = new ArrayList<>();
      }
      scales.ensureCapacity (cap);
      writeUnlock();
   }
   
   /**
    * Adds a new indexable scale.
    * @param s scale, by reference
    * @return index of the new scale added
    */
   public int addScale(Double s) {
      writeLock();
      int sidx = stateInfo.numScales;
      if (scales == null) {
         scales = new ArrayList<>();
      }
      scales.add(s);
      ++stateInfo.numScales;
      notifyScalesModifiedInternal();
      currentScaleIdx = sidx;
      writeUnlock();
      return sidx;
   }
   
   /**
    * Sets the current scale to be used in following instances
    * based on scale index.
    * @param sidx index of a previously added scale
    */
   public void setCurrentScale(int sidx) {
      if (sidx >= 0) {
         if (sidx >= scales.size()) {
            throw new IllegalArgumentException (
               "Scale "+sidx+" is not defined");
         }
         currentScaleIdx = sidx;
      }
      else {
         currentScaleIdx = -1;
      }
   }
   
   /**
    * Returns the index associated with the current scale, or -1
    * if there is no current scale.
    * 
    * @return current scale index
    */
   public int getCurrentScale() {
      return currentScaleIdx;
   }
   
   /**
    * Sets the scale by reference
    * @param sidx index of scale to modify
    * @param s scale
    */
   public void setScale(int sidx, Double s) {
      writeLock();
      scales.set(sidx, s);
      notifyScalesModified();
      writeUnlock();
   }
   
   /**
    * Retrieves the scale stored with supplied index
    * @param sidx index of scale
    * @return scale, by reference
    */
   public Double getScale(int sidx) {
      if (sidx < 0) {
         return null;
      }
      return scales.get(sidx);
   }
   
   private void notifyScalesModifiedInternal() {
      scalesModified = true;
      totalModified = true;
   }
   
   /**
    * Indicate that the scales have been modified.
    */
   public void notifyScalesModified() {
      writeLock();
      notifyScalesModifiedInternal ();
      writeUnlock();
   }
   
   /**
    * Whether or not any scales have been defined.
    */
   public boolean hasScales() {
      if (scales == null) {
         return false;
      }
      return (scales.size () > 0);
   }

   /**
    * Number of scales defined
    */
   public int numScales() {
      return stateInfo.numScales;
   }

   
   /**
    * Retrieves the full list of Scales.  This list should not
    * be modified.  
    * 
    * @return list of scales.
    */
   public List<Double> getScales() {
      return Collections.unmodifiableList(scales);
   }

   /**
    * Returns the latest scales version number,
    * for use in detecting if changes are present.
    */
   public int getScalesVersion() {
      if (scalesModified) {
         versionInfo.scalesVersion++;
         scalesModified = false;
      }
      return versionInfo.scalesVersion;
   }
   
   //=======================================================================
   // Instances
   //=======================================================================
   
   /**
    * Hint for ensuring sufficient storage
    * @param cap capacity
    */
   public void ensureInstanceCapacity(int cap) {
      writeLock();
      if (instances == null) {
         instances = new DynamicIntArray();
      }
      instances.ensureCapacity (4*cap);
      writeUnlock();
   }
   
   /**
    * Adds an instance with supplied transform index, scale index and color index
    * @param type type of instance
    * @param tidx transform index (either point, frame, or affine index)
    * @param sidx scale index
    * @param cidx color index
    * @return index of instance
    */
   public int addInstance(InstanceTransformType type, int tidx, int sidx, int cidx) {
      writeLock();
      int idx = addInstanceInternal(type, tidx, sidx, cidx);
      writeUnlock();
      return idx;
   }
   
   public void setInstance(int idx, InstanceTransformType type, int tidx, int sidx, int cidx) {
      writeLock();
      int off = idx*4;
      InstanceTransformType oldType = InstanceTransformType.values()[instances.get(off)];
      switch (oldType) {
         case AFFINE:
            --stateInfo.numAffineInstances;
            break;
         case FRAME:
            --stateInfo.numFrameInstances;
            break;
         case POINT:
            --stateInfo.numPointInstances;
            break;
      }
      instances.set(off, type.ordinal());
      instances.set(off+1, tidx);
      instances.set(off+2, sidx);
      instances.set(off+3, cidx);
      switch(type) {
         case AFFINE:
            ++stateInfo.numAffineInstances;
            break;
         case FRAME:
            ++stateInfo.numFrameInstances;
            break;
         case POINT:
            ++stateInfo.numPointInstances;
      }
      notifyInstancesModifiedInternal();
      writeUnlock();
   }
   
   public static InstanceTransformType[] getTransformTypes() {
      return InstanceTransformType.values();
   }
   
   public InstanceTransformType getInstanceType(int idx) {
      int off = getInstanceStride()*idx+getInstanceTypeOffset();
      return InstanceTransformType.values()[instances.get(off)];
   }
   
   public Double getInstanceScale(int idx) {
      int off = getInstanceStride()*idx+getInstanceScaleOffset();
      return getScale(instances.get(off));
   }
   
   public int[] getInstances() {
      if (instances == null) {
         instances = new DynamicIntArray();
      }
      return instances.getArray();
   }
   
   public int getInstanceTypeOffset() {
      return 0;
   }
   
   public int getInstanceTransformOffset() {
      return 1;
   }
   
   public int getInstanceScaleOffset() {
      return 2;
   }
   
   public int getInstanceColorOffset() {
      return 3;
   }
   
   public int getInstanceStride() {
      return 4;
   }
   
   /**
    * Apply a function to each instance, separating
    * points, frames, and affines.  Each instance type
    * is assigned an index corresponding to its position
    * within its own group in order of appearance 
    * (i.e. point index 0 is the first point,
    * 1 the second point, ..., 
    * frame index 0 is the first frame, ...).
    * order.
    * @param consumer consumer object for processing instances
    */
   public void forEachInstance(RenderInstanceConsumer consumer) {
      int idx = 0;
      int[] buff = getInstances();
      InstanceTransformType[] types = InstanceTransformType.values();
      int pidx = 0;
      int fidx = 0;
      int aidx = 0;
      
      for (int i=0; i<stateInfo.numInstances; ++i) {
         int type = buff[idx++];
         int tidx = buff[idx++];
         int sidx = buff[idx++];
         int cidx = buff[idx++];
         InstanceTransformType tt = types[type];
         switch(tt) {
            case POINT:
               consumer.point(pidx, getPoint(tidx), getScale(sidx), getColor(cidx));
               ++pidx;
               break;
            case FRAME:
               consumer.frame(fidx, getFrame(tidx), getScale(sidx), getColor(cidx));
               ++fidx;
               break;
            case AFFINE:
               consumer.affine(aidx, getAffine(tidx), getScale(sidx), getColor(cidx));
               ++aidx;
               break;
         }
      }
   }
      
   private void notifyInstancesModifiedInternal() {
      instancesModified = true;
      totalModified = true;
   }
   
   /**
    * Indicate that the instances have been modified.
    */
   public void notifyInstancesModified() {
      writeLock();
      notifyInstancesModifiedInternal ();
      writeUnlock();
   }
   
   /**
    * Returns the latest colors version number,
    * for use in detecting if changes are present.
    */
   public int getInstancesVersion() {
      if (instancesModified) {
         versionInfo.instancesVersion++;
         instancesModified = false;
      }
      return versionInfo.instancesVersion;
   }
   
   /**
    * Whether or not any instances have been defined.
    */
   public boolean hasInstances() {
      return (stateInfo.numInstances > 0);
   }

   /**
    * Number of instances defined
    */
   public int numInstances() {
      return stateInfo.numInstances;
   }
   
   public boolean hasPointInstances() {
      return (stateInfo.numPointInstances > 0);
   }
   
   public int numPointInstances() {
      return stateInfo.numPointInstances;
   }
   
   public boolean hasFrameInstances() {
      return (stateInfo.numFrameInstances > 0);
   }
   
   public int numFrameInstances() {
      return stateInfo.numFrameInstances;
   }
   
   public boolean hasAffineInstances() {
      return (stateInfo.numAffineInstances > 0);
   }
   
   public int numAffineInstances() {
      return stateInfo.numAffineInstances;
   }
   
   /**
    * Adds an instance with supplied transform index, scale index and current
    * color and scale
    * @param type type of instance
    * @param tidx transform index (either point, frame, or affine index)
    * @return index of the instance added
    */
   public int addInstance(InstanceTransformType type, int tidx) {
      return addInstance(type, tidx, currentScaleIdx, currentColorIdx);
   }
   
   /**
    * Adds an instance based on point index
    * @param pidx point index
    * @return index of new instance
    */
   public int addPointInstance(int pidx) {
      return addInstance(InstanceTransformType.POINT, pidx, currentScaleIdx, currentColorIdx);
   }
   
   /**
    * Adds an instance based on frame index
    * @param fidx frame index
    * @return index of new instance
    */
   public int addFrameInstance(int fidx) {
      return addInstance(InstanceTransformType.FRAME, fidx, currentScaleIdx, currentColorIdx);
   }
   
   /**
    * Adds an instance based on affine index
    * @param aidx affine index
    * @return index of new instance
    */
   public int addAffineInstance(int aidx) {
      return addInstance(InstanceTransformType.AFFINE, aidx, currentScaleIdx, currentColorIdx);
   }
   
   private int addInstanceInternal(InstanceTransformType type, int tidx, int sidx, int cidx) {
      int idx = stateInfo.numInstances;
      if (instances == null) {
         instances = new DynamicIntArray();
      }
      instances.add(type.ordinal());
      instances.add(tidx);
      instances.add(sidx);
      instances.add(cidx);
      ++stateInfo.numInstances;
      switch (type) {
         case AFFINE:
            ++stateInfo.numAffineInstances;
            break;
         case FRAME:
            ++stateInfo.numFrameInstances;
            break;
         case POINT:
            ++stateInfo.numPointInstances;
            break;
         
      }
      notifyInstancesModifiedInternal();
      return idx;
   }


   /**
    * Adds an instance by point
    * @param px x-coordinate of instance
    * @param py y-coordinate of instance
    * @param pz z-coordinate of instance
    * @return the index of the instance added
    */
   public int addInstance(float px, float py, float pz) {
      return addInstance (new float[]{px,py,pz});
   }
   
   /**
    * Adds an instance by point
    * @param xyz coordinates
    * @return the index of the instance added
    */
   public int addInstance (float[] xyz) {
      writeLock();
      int pidx = addPointInternal (xyz);
      int idx = addInstanceInternal (InstanceTransformType.POINT, pidx, currentScaleIdx, currentColorIdx);
      writeUnlock();
      return idx;      
   }
  
   /**
    * Adds an instance by point
    * @param pos position of instance
    * @return the index of the instance added
    */
   public int addInstance(Vector3d pos) {
      return addInstance (
         new float[]{(float)pos.x, (float)pos.y, (float)pos.z});
   }
   
   /**
    * Adds an instance by frame
    * @param frame frame transform for instance
    * @return the index of the instance added
    */
   public int addInstance(RigidTransform3d frame) {
      writeLock();
      int pidx = addFrameInternal (frame);
      int idx = addInstanceInternal (InstanceTransformType.FRAME, pidx, currentScaleIdx, currentColorIdx);
      writeUnlock();
      return idx;      
   }
   
   /**
    * Adds an instance by affine
    * @param affine transform to be added, by reference
    * @return the index of the instance added
    */
   public int addInstance(AffineTransform3d affine) {
      writeLock();
      int pidx = addAffineInternal (affine);
      int idx = addInstanceInternal (InstanceTransformType.AFFINE, pidx, currentScaleIdx, currentColorIdx);
      writeUnlock();
      return idx;      
   }

   /**
    * Clears everything in the RenderInstances, allowing it to be
    * recreated.  This can be used when the object becomes invalid,
    * in place of discarding and regenerating a new one.  The object
    * becomes a clean slate, with no instance attributes.
    */
   public void clearAll() {
      
      writeLock();
      
      points = null;
      frames = null;
      affines = null;
      colors = null;
      scales = null;
      instances = null;

      stateInfo.clear();
      
      pointsModified = true;
      framesModified = true;
      affinesModified = true;
      colorsModified = true;
      scalesModified = true;
      instancesModified = true;
      totalModified = true;
      
      currentScaleIdx = -1;
      currentColorIdx = -1;
      
      writeUnlock();
   }

   //=========================================================================
   // Usage flags
   //=========================================================================

   /**
    * Retrieves the version of the object, for use in detecting
    * whether any information has changed since last use.
    * @return the overall modification version
    */
   public int getVersion() {
      // update all other versions
      getPointsVersion();
      getFramesVersion();
      getAffinesVersion();
      getScalesVersion();
      getColorsVersion();
      getInstancesVersion();
      if (totalModified) {
         versionInfo.totalVersion++;
         totalModified = false;
      }
      return versionInfo.totalVersion;
   }

   /**
    * Returns whether or not this RenderObject is valid.  If valid,
    * this object can be safely passed to a renderer for drawing.  
    * If not, it needs to be discarded.
    * @return <code>true</code> if this RenderObject is valid
    */
   public boolean isValid() {
      return !(rid.isDisposed());
   }

   /**
    * Signal a destruction of the object.
    */
   public void dispose() {
      clearAll();
      rid.dispose();
   }

   /**
    * Garbage collection, clear memory and dispose
    */
   @Override
   protected void finalize() throws Throwable {
      dispose();
   }
   
   /**
    * @return a new copy of the object
    */
   public RenderInstances copy()  {

      readLock();
      RenderInstances r;
      try {
         r = (RenderInstances)super.clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }

      if (points != null) {
         r.points = new ArrayList<float[]>(points);
      } else {
         r.points = null;
      }
      
      if (frames != null) {
         r.frames = new ArrayList<RigidTransform3d>(frames);
      } else {
         r.frames = null;
      }

      if (affines != null) {
         r.affines = new ArrayList<AffineTransform3d>(affines);
      } else {
         r.affines = null;
      }
      
      if (colors != null) {
         r.colors = new ArrayList<>(colors.size());
         r.colors.addAll (colors);
      } else {
         r.colors = null;
      }
      
      if (scales != null) {
         r.scales = new ArrayList<>(scales.size());
         r.scales.addAll (scales);
      } else {
         r.scales = null;
      }

      r.stateInfo = stateInfo.clone();

      // keep track separately to allow storage to be cleared
      r.stateInfo = stateInfo.clone();
      r.versionInfo = versionInfo.clone();
      
      readUnlock();
      
      return r;
   }
   
   @Override
   public boolean isDisposed() {
      return rid.isDisposed();
   }
   
   @Override
   public RenderInstancesIdentifier getDisposeObserver() {
      return rid;
   }
   
   /**
    * Interface to allow simplified iterations through all instance transforms
    *
    */
   public static interface RenderInstanceConsumer {
      public void point(int pidx, float[] pos, Double scale, byte[] color);
      public void frame(int fidx, RigidTransform3d trans, Double scale, byte[] color);
      public void affine(int aidx, AffineTransform3d trans, Double scale, byte[] color);
   }
   
  
}
