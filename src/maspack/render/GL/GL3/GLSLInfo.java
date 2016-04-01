package maspack.render.GL.GL3;

import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;

public class GLSLInfo {

   public enum InstancedRendering {
      NONE,
      POINTS,
      LINES,
      FRAMES,
      AFFINES
   }
   
   public static final short COLOR_MAP_FLAG = 0x01;
   public static final short NORMAL_MAP_FLAG = 0x10;
   public static final short BUMP_MAP_FLAG = 0x20;
   public static final short DEFAULT_MAP_MASK = 0;
   
   public static final ColorMixing DEFAULT_VERTEX_COLOR_MIXING = ColorMixing.REPLACE;
   public static final ColorMixing DEFAULT_TEXTURE_COLOR_MIXING = ColorMixing.MODULATE;
   
   public static final byte MIX_DIFFUSE_FLAG = 0x2;
   public static final byte MIX_SPECULAR_FLAG = 0x4;
   public static final byte MIX_EMISSION_FLAG = 0x8;
   public static final byte DEFAULT_VERTEX_COLOR_MIX_MASK = MIX_DIFFUSE_FLAG | MIX_EMISSION_FLAG;
   public static final byte DEFAULT_TEXTURE_COLOR_MIX_MASK = MIX_DIFFUSE_FLAG | MIX_EMISSION_FLAG;
   
   private int numLights;
   private int numClipPlanes;
   
   private boolean hasVertexNormals;
   private boolean hasVertexColors;
   private boolean hasVertexTextures;
   
   private boolean useRoundPoints;
   
   private boolean hasInstanceOrientations;
   private boolean hasInstanceAffines;
   private boolean hasInstanceColors;
  
   private boolean hasLineScaleOffset;
   private boolean hasLineColors;
   
   private Shading shading;
   private ColorInterpolation colorInterp;
   private InstancedRendering instanced;
   
   private ColorMixing vertexColorMixing;
   private byte vertexColorMixMask;
   
   private ColorMixing textureMixing;
   private byte textureColorMixMask;
   
   private short textureMapMask;
   
   public static class GLSLInfoBuilder {
      GLSLInfo info;
      
      public GLSLInfo build() {
         return info.clone();
      }
      
      public GLSLInfoBuilder() {
         info = new GLSLInfo();
      }
      
      public void setNumLights(int numLights) {
         info.numLights = numLights;
      }

      public void setNumClipPlanes(int numClipPlanes) {
         info.numClipPlanes = numClipPlanes;
      }

      public void setVertexNormals(boolean set) {
         info.hasVertexNormals=set;
      }

      public void setVertexColors(boolean set) {
         info.hasVertexColors=set;
      }

      public void setVertexTextures(boolean set) {
         info.hasVertexTextures=set;
      }

      public void setRoundPoints(boolean enable) {
         info.useRoundPoints=enable;
      }
      
      public void setInstanceOrientations(boolean enable) {
         info.hasInstanceOrientations=enable;
      }

      public void setInstanceAffines(boolean enable) {
         info.hasInstanceAffines=enable;
      }

      public void setInstanceColors(boolean enable) {
         info.hasInstanceColors=enable;
      }

      public void setLineScaleOffset(boolean enable) {
         info.hasLineScaleOffset=enable;
      }

      public void setLineColors(boolean enable) {
         info.hasLineColors=enable;
      }

      public void setLighting(Shading shading) {
         info.shading=shading;
      }

      public void setColorInterpolation(ColorInterpolation colorInterp) {
         info.colorInterp=colorInterp;
      }

      public void setInstancedRendering(InstancedRendering instanced) {
         info.instanced=instanced;
      }
      
      public void setVertexColorMixing(ColorMixing cmix) {
         if (cmix == null) {
            cmix=DEFAULT_VERTEX_COLOR_MIXING;
         }
         info.vertexColorMixing=cmix;
      }
      
      public void setTextureColorMixing(ColorMixing tmix) {
         if (tmix == null) {
            tmix=DEFAULT_TEXTURE_COLOR_MIXING;
         }
         info.textureMixing=tmix;
      }
      
      public void enableColorMap(boolean set) {
         if (set) {
            info.textureMapMask |= COLOR_MAP_FLAG;
         } else {
            info.textureMapMask &= ~COLOR_MAP_FLAG;
         }
      }
      
      public void enableNormalMap(boolean set) {
         if (set) {
            info.textureMapMask |= NORMAL_MAP_FLAG;
         } else {
            info.textureMapMask &= ~NORMAL_MAP_FLAG;
         }
      }
      
      public void enableBumpMap(boolean set) {
         if (set) {
            info.textureMapMask |= BUMP_MAP_FLAG;
         } else {
            info.textureMapMask &= ~BUMP_MAP_FLAG;
         }
      }
      
      public void mixTextureColorDiffuse(boolean enable) {
         if (enable) {
            info.textureColorMixMask |= MIX_DIFFUSE_FLAG;
         } else {
            info.textureColorMixMask &= ~MIX_DIFFUSE_FLAG;
         }
      }
      
      public void mixTextureColorSpecular(boolean enable) {
         if (enable) {
            info.textureColorMixMask |= MIX_SPECULAR_FLAG;
         } else {
            info.textureColorMixMask &= ~MIX_SPECULAR_FLAG;
         }
      }
      
      public void mixTextureColorEmission(boolean enable) {
         if (enable) {
            info.textureColorMixMask |= MIX_EMISSION_FLAG;
         } else {
            info.textureColorMixMask &= ~MIX_EMISSION_FLAG;
         }
      }
      
      public void mixVertexColorDiffuse(boolean enable) {
         if (enable) {
            info.vertexColorMixMask |= MIX_DIFFUSE_FLAG;
         } else {
            info.vertexColorMixMask &= ~MIX_DIFFUSE_FLAG;
         }
      }
      
      public void mixVertexColorSpecular(boolean enable) {
         if (enable) {
            info.vertexColorMixMask |= MIX_SPECULAR_FLAG;
         } else {
            info.vertexColorMixMask &= ~MIX_SPECULAR_FLAG;
         }
      }
      
      public void mixVertexColorEmission(boolean enable) {
         if (enable) {
            info.vertexColorMixMask |= MIX_EMISSION_FLAG;
         } else {
            info.vertexColorMixMask &= ~MIX_EMISSION_FLAG;
         }
      }
      
   }
   
   private GLSLInfo() {
      numLights = 0;
      numClipPlanes = 0;
      
      shading = Shading.PHONG;
      colorInterp = ColorInterpolation.RGB;
      
      hasVertexNormals = false;
      hasVertexColors = false;
      hasVertexTextures = false;
      
      useRoundPoints = false;
      
      instanced = InstancedRendering.NONE;
      
      hasInstanceOrientations = false;
      hasInstanceAffines = false;
      hasInstanceColors = false;
      
      hasLineScaleOffset = false;
      hasLineColors = false;
      
      vertexColorMixing = DEFAULT_TEXTURE_COLOR_MIXING;
      textureMixing = DEFAULT_TEXTURE_COLOR_MIXING;
      textureMapMask = DEFAULT_MAP_MASK;
      vertexColorMixMask = DEFAULT_VERTEX_COLOR_MIX_MASK;
      textureColorMixMask = DEFAULT_TEXTURE_COLOR_MIX_MASK;
   }
   
   public static GLSLInfo create(int numClipPlanes) {
      
      GLSLInfo info = new GLSLInfo ();
      info.numClipPlanes = numClipPlanes;
      return info;
      
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
      result = prime * result + (hasLineScaleOffset ? 1231 : 1237);
      result = prime * result + (hasLineColors ? 1231 : 1237);
      result = prime * result + (useRoundPoints ? 1231 : 1237);
      result = prime * result + (hasVertexColors ? 1231 : 1237);
      result = prime * result + (hasVertexNormals ? 1231 : 1237);
      result = prime * result + (hasVertexTextures ? 1231 : 1237);
      result = prime * result + ((instanced == null) ? 0 : instanced.hashCode());
      result = prime * result + numClipPlanes;
      result = prime * result + numLights;
      result = prime * result + ((shading == null) ? 0 : shading.hashCode());
      result = prime * result + ((vertexColorMixing == null) ? 0 : vertexColorMixing.hashCode());
      result = prime * result + ((textureMixing == null) ? 0 : textureMixing.hashCode());
      result = prime * result + textureMapMask;
      result = prime * result + vertexColorMixMask;
      result = prime * result + textureColorMixMask;
      
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
      if (hasLineScaleOffset != other.hasLineScaleOffset) {
         return false;
      }
      if (hasLineColors != other.hasLineColors) {
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
      if (useRoundPoints != other.useRoundPoints) {
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
      if (vertexColorMixing != other.vertexColorMixing) {
         return false;
      }
      if (textureMixing != other.textureMixing) {
         return false;
      }
      if (textureMapMask != other.textureMapMask) {
         return false;
      }
      if (textureColorMixMask != other.textureColorMixMask) {
         return false;
      }
      if (vertexColorMixMask != other.vertexColorMixMask) {
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

   public boolean isUsingRoundPoints() {
      return useRoundPoints;
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
   
   public boolean hasLineScaleOffset() {
      return hasLineScaleOffset;
   }

   public boolean hasLineColors() {
      return hasLineColors;
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
   
   public ColorMixing getVertexColorMixing() {
      if (vertexColorMixing == null) {
         return DEFAULT_VERTEX_COLOR_MIXING;
      }
      return vertexColorMixing;
   }
   
   public boolean hasColorMap() {
      return (textureMapMask & COLOR_MAP_FLAG) != 0;
   }
   
   public boolean hasNormalMap() {
      return (textureMapMask & NORMAL_MAP_FLAG) != 0;
   }
   
   public boolean hasBumpMap() {
      return (textureMapMask & BUMP_MAP_FLAG) != 0;
   }
   
   public int getVertexColorMixMask() {
      return vertexColorMixMask;
   }
   
   public boolean mixVertexColorDiffuse() {
      return (vertexColorMixMask & MIX_DIFFUSE_FLAG) != 0;
   }
   
   public boolean mixVertexColorSpecular() {
      return (vertexColorMixMask & MIX_SPECULAR_FLAG) != 0;
   }
   
   public boolean mixVertexColorEmission() {
      return (vertexColorMixMask & MIX_EMISSION_FLAG) != 0;
   }
   
   public ColorMixing getTextureColorMixing() {
      if (textureMixing == null) {
         return DEFAULT_TEXTURE_COLOR_MIXING;
      }
      return textureMixing;
   }
   
   public int getTextureMapMask() {
      return textureMapMask;
   }
   
   public int getTextureColorMixMask() {
      return textureColorMixMask;
   }
   
   public boolean mixTextureColorDiffuse() {
      return (textureColorMixMask & MIX_DIFFUSE_FLAG) != 0;
   }
   
   public boolean mixTextureColorSpecular() {
      return (textureColorMixMask & MIX_SPECULAR_FLAG) != 0;
   }
   
   public boolean mixTextureColorEmission() {
      return (textureColorMixMask & MIX_EMISSION_FLAG) != 0;
   }
   
   public GLSLInfo clone() {
      GLSLInfo out = new GLSLInfo();
      
      out.numLights = this.numLights;
      out.numClipPlanes = this.numClipPlanes;
      
      out.hasVertexNormals = this.hasVertexNormals;
      out.hasVertexColors = this.hasVertexColors;
      out.hasVertexTextures = this.hasVertexTextures;
      
      out.useRoundPoints = this.useRoundPoints;
      
      out.hasInstanceOrientations = this.hasInstanceOrientations;
      out.hasInstanceAffines = this.hasInstanceAffines;
      out.hasInstanceColors = this.hasInstanceColors;
     
      out.hasLineScaleOffset = this.hasLineScaleOffset;
      out.hasLineColors = this.hasLineColors;
      
      out.shading = this.shading;
      out.colorInterp = this.colorInterp;
      out.instanced = this.instanced;
      
      out.vertexColorMixing = this.vertexColorMixing;
      out.vertexColorMixMask = this.vertexColorMixMask;
      
      out.textureMixing = this.textureMixing;
      out.textureColorMixMask = this.textureColorMixMask;
      out.textureMapMask = this.textureMapMask;
      
      return out;
   }
   
}
