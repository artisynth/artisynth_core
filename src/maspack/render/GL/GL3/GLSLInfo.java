package maspack.render.GL.GL3;

import maspack.render.RenderProps.Shading;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.ColorMixing;

public class GLSLInfo {

   public enum InstancedRendering {
      NONE,
      POINTS,
      LINES,
      FRAMES,
      AFFINES
   }
   
   public static final ColorMixing DEFAULT_COLOR_MIXING = ColorMixing.REPLACE;
   public static final ColorMixing DEFAULT_TEXTURE_MIXING = ColorMixing.MODULATE;
   
   private int numLights;
   private int numClipPlanes;
   
   private boolean hasVertexNormals;
   private boolean hasVertexColors;
   private boolean hasVertexTextures;
   
   private boolean hasInstanceOrientations;
   private boolean hasInstanceAffines;
   private boolean hasInstanceColors;
   private boolean hasInstanceTextures;
  
   private boolean hasLineLengthOffset;
   private boolean hasLineColors;
   private boolean hasLineTextures;
   
   private Shading shading;
   private ColorInterpolation colorInterp;
   private InstancedRendering instanced;
   
   private ColorMixing colorMixing;
   private ColorMixing textureMixing;
   
   public GLSLInfo() {
      numLights = 0;
      numClipPlanes = 0;
      
      shading = Shading.PHONG;
      colorInterp = ColorInterpolation.RGB;
      
      hasVertexNormals = false;
      hasVertexColors = false;
      hasVertexTextures = false;
      
      instanced = InstancedRendering.NONE;
      
      hasInstanceOrientations = false;
      hasInstanceAffines = false;
      hasInstanceColors = false;
      hasInstanceTextures = false;
      
      hasLineLengthOffset = false;
      hasLineColors = false;
      hasLineTextures = false;
      
      colorMixing = DEFAULT_COLOR_MIXING;
      textureMixing = DEFAULT_TEXTURE_MIXING;
      
   }
   
   public GLSLInfo(int numLights, int numClipPlanes, Shading shading,
      ColorInterpolation colorInterp, boolean hasVertexNormals,
      boolean hasVertexColors, boolean hasVertexTextures, InstancedRendering instanced,
      boolean hasInstanceColors, boolean hasInstanceTextures, boolean hasLineLengthOffset,
      boolean hasLineColors, boolean hasLineTextures,
      ColorMixing colorMixing, ColorMixing textureMixing) {
      
      this.numClipPlanes = numClipPlanes;
      
      this.shading = shading;
      if (shading == Shading.NONE) {
         this.numLights = 0;
      } else {
         this.numLights = numLights;   
      }
      
      this.colorInterp = colorInterp;
      
      this.hasVertexNormals = hasVertexNormals;
      this.hasVertexColors = hasVertexColors;
      this.hasVertexTextures = hasVertexTextures;
      
      this.instanced = instanced;
      switch (instanced) {
         case POINTS:
            this.hasInstanceOrientations = false;
            this.hasInstanceAffines = false;
            this.hasInstanceColors = hasInstanceColors;
            this.hasInstanceTextures = hasInstanceTextures;
            this.hasLineLengthOffset = false;
            this.hasLineColors = false;
            this.hasLineTextures = false;
            break;
         case FRAMES:
            this.hasInstanceOrientations = true;
            this.hasInstanceAffines = false;
            this.hasInstanceColors = hasInstanceColors;
            this.hasInstanceTextures = hasInstanceTextures;
            this.hasLineLengthOffset = false;
            this.hasLineColors = false;
            this.hasLineTextures = false;
            break;
         case AFFINES:
            this.hasInstanceOrientations = false;
            this.hasInstanceAffines = true;
            this.hasInstanceColors = hasInstanceColors;
            this.hasInstanceTextures = hasInstanceTextures;
            this.hasLineLengthOffset = false;
            this.hasLineColors = false;
            this.hasLineTextures = false;
            break;
         case LINES:
            this.hasInstanceOrientations = false;
            this.hasInstanceAffines = false;
            this.hasInstanceColors = false;
            this.hasInstanceTextures = false;
            this.hasLineLengthOffset = hasLineLengthOffset;
            this.hasLineColors = hasLineColors;
            this.hasLineTextures = hasLineTextures;
            break;
         case NONE:
            this.hasInstanceOrientations = false;
            this.hasInstanceAffines = false;
            this.hasInstanceColors = false;
            this.hasInstanceTextures = false;
            this.hasLineLengthOffset = false;
            this.hasLineColors = false;
            this.hasLineTextures = false;
            break;
      }
      
      this.colorMixing = colorMixing;
      this.textureMixing = textureMixing;
      
   }
   
   public GLSLInfo(int numLights, int numClipPlanes, Shading shading,
      ColorInterpolation colorInterp, boolean hasVertexNormals,
      boolean hasVertexColors, boolean hasVertexTextures,
      ColorMixing colorMixing, ColorMixing textureMixing) {
      
      this.numClipPlanes = numClipPlanes;
      
      this.shading = shading;
      if (shading == Shading.NONE) {
         this.numLights = 0;
      } else {
         this.numLights = numLights;   
      }
      
      this.colorInterp = colorInterp;
      
      this.hasVertexNormals = hasVertexNormals;
      this.hasVertexColors = hasVertexColors;
      this.hasVertexTextures = hasVertexTextures;
      
      this.instanced = InstancedRendering.NONE;
      this.hasInstanceOrientations = false;
      this.hasInstanceAffines = false;
      this.hasInstanceColors = false;
      this.hasInstanceTextures = false;
      this.hasLineLengthOffset = false;
      this.hasLineColors = false;
      this.hasLineTextures = false;
      
      this.colorMixing = colorMixing;
      this.textureMixing = textureMixing;
   }
   
   public GLSLInfo create(int numClipPlanes) {
      return new GLSLInfo(0, numClipPlanes, Shading.NONE, ColorInterpolation.NONE, false, false, false,
         InstancedRendering.NONE, false, false, false, false, false, DEFAULT_COLOR_MIXING, DEFAULT_TEXTURE_MIXING);
   }
   

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result =
         prime * result + ((colorInterp == null) ? 0 : colorInterp.hashCode());
      result = prime * result + (hasInstanceAffines ? 1231 : 1237);
      result = prime * result + (hasInstanceColors ? 1231 : 1237);
      result = prime * result + (hasInstanceOrientations ? 1231 : 1237);
      result = prime * result + (hasInstanceTextures ? 1231 : 1237);
      result = prime * result + (hasLineLengthOffset ? 1231 : 1237);
      result = prime * result + (hasLineColors ? 1231 : 1237);
      result = prime * result + (hasLineTextures ? 1231 : 1237);
      result = prime * result + (hasVertexColors ? 1231 : 1237);
      result = prime * result + (hasVertexNormals ? 1231 : 1237);
      result = prime * result + (hasVertexTextures ? 1231 : 1237);
      result = prime * result + ((instanced == null) ? 0 : instanced.hashCode());
      result = prime * result + numClipPlanes;
      result = prime * result + numLights;
      result = prime * result + ((shading == null) ? 0 : shading.hashCode());
      result = prime * result + ((colorMixing == null) ? 0 : colorMixing.hashCode());
      result = prime * result + ((textureMixing == null) ? 0 : textureMixing.hashCode());
      
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      GLSLInfo other = (GLSLInfo)obj;
      if (colorInterp != other.colorInterp) {
         return false;
      }
      if (hasInstanceAffines != other.hasInstanceAffines) {
         return false;
      }
      if (hasInstanceColors != other.hasInstanceColors) {
         return false;
      }
      if (hasInstanceOrientations != other.hasInstanceOrientations) {
         return false;
      }
      if (hasInstanceTextures != other.hasInstanceTextures) {
         return false;
      }
      if (hasLineLengthOffset != other.hasLineLengthOffset) {
         return false;
      }
      if (hasLineColors != other.hasLineColors) {
         return false;
      }
      if (hasLineTextures != other.hasLineTextures) {
         return false;
      }
      if (hasVertexColors != other.hasVertexColors) {
         return false;
      }
      if (hasVertexNormals != other.hasVertexNormals) {
         return false;
      }
      if (hasVertexTextures != other.hasVertexTextures) {
         return false;
      }
      if (instanced != other.instanced) {
         return false;
      }
      if (numClipPlanes != other.numClipPlanes) {
         return false;
      }
      if (numLights != other.numLights) {
         return false;
      }
      if (shading != other.shading) {
         return false;
      }
      if (colorMixing != other.colorMixing) {
         return false;
      }
      if (textureMixing != other.textureMixing) {
         return false;
      }
      return true;
   }
   
   public int numLights() {
      return numLights;
   }

   public int numClipPlanes() {
      return numClipPlanes;
   }

   public boolean hasVertexNormals() {
      return hasVertexNormals;
   }

   public boolean hasVertexColors() {
      return hasVertexColors;
   }

   public boolean hasVertexTextures() {
      return hasVertexTextures;
   }

   public boolean hasInstanceOrientations() {
      return hasInstanceOrientations;
   }

   public boolean hasInstanceAffines() {
      return hasInstanceAffines;
   }

   public boolean hasInstanceColors() {
      return hasInstanceColors;
   }

   public boolean hasInstanceTextures() {
      return hasInstanceTextures;
   }
   
   public boolean hasLineLengthOffset() {
      return hasLineLengthOffset;
   }

   public boolean hasLineColors() {
      return hasLineColors;
   }

   public boolean hasLineTextures() {
      return hasLineTextures;
   }

   public Shading getShading() {
      return shading;
   }

   public ColorInterpolation getColorInterpolation() {
      return colorInterp;
   }

   public InstancedRendering getInstancedRendering() {
      return instanced;
   }
   
   public ColorMixing getColorMixing() {
      if (colorMixing == null) {
         return DEFAULT_COLOR_MIXING;
      }
      return colorMixing;
   }
   
   public ColorMixing getTextureMixing() {
      if (textureMixing == null) {
         return DEFAULT_TEXTURE_MIXING;
      }
      return textureMixing;
   }
   
}
