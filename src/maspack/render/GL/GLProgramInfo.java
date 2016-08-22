package maspack.render.GL;

import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;

public class GLProgramInfo {

   public enum RenderingMode {
      DEFAULT,
      POINTS,
      INSTANCED_POINTS,
      INSTANCED_LINES,
      INSTANCED_FRAMES,
      INSTANCED_AFFINES
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
   public static final byte DEFAULT_VERTEX_COLOR_MIX_MASK = MIX_DIFFUSE_FLAG;
   public static final byte DEFAULT_TEXTURE_COLOR_MIX_MASK = MIX_DIFFUSE_FLAG;
   
   private int numLights;
   private int numClipPlanes;
   
   private RenderingMode mode;
   
   private boolean hasVertexNormals;
   private boolean hasVertexColors;
   private boolean hasVertexTextures;
   
   private boolean selecting;
   
   private boolean useRoundPoints;
   
   private boolean hasInstanceColors;
   private boolean hasLineScaleOffset;
   private boolean hasLineColors;
   
   private Shading shading;
   private ColorInterpolation colorInterp;
   
   private ColorMixing vertexColorMixing;
   private byte vertexColorMixMask;
   
   private ColorMixing textureMixing;
   private byte textureColorMixMask;
   
   private short textureMapMask;
   
   public GLProgramInfo() {
      numLights = 0;
      numClipPlanes = 0;
      
      shading = Shading.NONE;
      colorInterp = ColorInterpolation.RGB;
      
      hasVertexNormals = false;
      hasVertexColors = false;
      hasVertexTextures = false;
      
      useRoundPoints = false;
      
      selecting = false;
      
      mode = RenderingMode.DEFAULT;

      hasInstanceColors = false;      
      hasLineScaleOffset = false;
      hasLineColors = false;
      
      vertexColorMixing = DEFAULT_TEXTURE_COLOR_MIXING;
      textureMixing = DEFAULT_TEXTURE_COLOR_MIXING;
      textureMapMask = DEFAULT_MAP_MASK;
      vertexColorMixMask = DEFAULT_VERTEX_COLOR_MIX_MASK;
      textureColorMixMask = DEFAULT_TEXTURE_COLOR_MIX_MASK;
   }
   
   public static GLProgramInfo create(int numClipPlanes) {
      
      GLProgramInfo info = new GLProgramInfo ();
      info.numClipPlanes = numClipPlanes;
      return info;
      
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result =
         prime * result + ((colorInterp == null) ? 0 : colorInterp.hashCode());
      result = prime * result + (hasInstanceColors ? 1231 : 1237);
      result = prime * result + (hasLineScaleOffset ? 1231 : 1237);
      result = prime * result + (hasLineColors ? 1231 : 1237);
      result = prime * result + (useRoundPoints ? 1231 : 1237);
      result = prime * result + (selecting ? 1231 : 1237);
      result = prime * result + (hasVertexColors ? 1231 : 1237);
      result = prime * result + (hasVertexNormals ? 1231 : 1237);
      result = prime * result + (hasVertexTextures ? 1231 : 1237);
      result = prime * result + ((mode == null) ? 0 : mode.hashCode());
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
      GLProgramInfo other = (GLProgramInfo)obj;
      if (colorInterp != other.colorInterp) {
         return false;
      }
      if (hasInstanceColors != other.hasInstanceColors) {
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
      if (selecting != other.selecting) {
         return false;
      }
      if (mode != other.mode) {
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

   public boolean hasRoundPoints() {
      return useRoundPoints;
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

   public boolean isSelecting() {
     return selecting;
   }
   
   public ColorInterpolation getColorInterpolation() {
      return colorInterp;
   }

   public RenderingMode getMode() {
      return mode;
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
   
   public boolean isMixVertexColorDiffuse() {
      return (vertexColorMixMask & MIX_DIFFUSE_FLAG) != 0;
   }
   
   public boolean isMixVertexColorSpecular() {
      return (vertexColorMixMask & MIX_SPECULAR_FLAG) != 0;
   }
   
   public boolean isMixVertexColorEmission() {
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
   
   public boolean isMixTextureColorDiffuse() {
      return (textureColorMixMask & MIX_DIFFUSE_FLAG) != 0;
   }
   
   public boolean isMixTextureColorSpecular() {
      return (textureColorMixMask & MIX_SPECULAR_FLAG) != 0;
   }
   
   public boolean isMixTextureColorEmission() {
      return (textureColorMixMask & MIX_EMISSION_FLAG) != 0;
   }
   
   public GLProgramInfo clone() {
      GLProgramInfo out = new GLProgramInfo();
      
      out.numLights = this.numLights;
      out.numClipPlanes = this.numClipPlanes;
      
      out.hasVertexNormals = this.hasVertexNormals;
      out.hasVertexColors = this.hasVertexColors;
      out.hasVertexTextures = this.hasVertexTextures;
      
      out.useRoundPoints = this.useRoundPoints;

      out.selecting = this.selecting;
      
      out.hasInstanceColors = this.hasInstanceColors;     
      out.hasLineScaleOffset = this.hasLineScaleOffset;
      out.hasLineColors = this.hasLineColors;
      
      out.shading = this.shading;
      out.colorInterp = this.colorInterp;
      out.mode = this.mode;
      
      out.vertexColorMixing = this.vertexColorMixing;
      out.vertexColorMixMask = this.vertexColorMixMask;
      
      out.textureMixing = this.textureMixing;
      out.textureColorMixMask = this.textureColorMixMask;
      out.textureMapMask = this.textureMapMask;
      
      return out;
   }
   
   public void setNumLights(int numLights) {
      this.numLights = numLights;
   }

   public void setNumClipPlanes(int numClipPlanes) {
      this.numClipPlanes = numClipPlanes;
   }

   public void setVertexNormalsEnabled(boolean set) {
      this.hasVertexNormals=set;
   }

   public void setVertexColorsEnabled(boolean set) {
      this.hasVertexColors=set;
   }

   public void setVertexTexturesEnabled(boolean set) {
      this.hasVertexTextures=set;
   }

   public void setRoundPointsEnabled(boolean enable) {
      this.useRoundPoints=enable;
   }
   
   public void setInstanceColorsEnabled(boolean enable) {
      this.hasInstanceColors=enable;
   }

   public void setLineScaleOffsetEnabled(boolean enable) {
      this.hasLineScaleOffset=enable;
   }

   public void setLineColorsEnabled(boolean enable) {
      this.hasLineColors=enable;
   }

   public void setShading(Shading shading) {
      this.shading=shading;
   }

   public void setSelecting(boolean set) {
      this.selecting = set;
   }
   
   public void setColorInterpolation(ColorInterpolation colorInterp) {
      this.colorInterp=colorInterp;
   }

   public void setMode(RenderingMode mode) {
      this.mode=mode;
   }
   
   public void setVertexColorMixing(ColorMixing cmix) {
      if (cmix == null) {
         cmix=DEFAULT_VERTEX_COLOR_MIXING;
      }
      this.vertexColorMixing=cmix;
   }
   
   public void setTextureColorMixing(ColorMixing tmix) {
      if (tmix == null) {
         tmix=DEFAULT_TEXTURE_COLOR_MIXING;
      }
      this.textureMixing=tmix;
   }
   
   public void setColorMapEnabled(boolean set) {
      if (set) {
         this.textureMapMask |= COLOR_MAP_FLAG;
      } else {
         this.textureMapMask &= ~COLOR_MAP_FLAG;
      }
   }
   
   public void setNormalMapEnabled(boolean set) {
      if (set) {
         this.textureMapMask |= NORMAL_MAP_FLAG;
      } else {
         this.textureMapMask &= ~NORMAL_MAP_FLAG;
      }
   }
   
   public void setBumpMapEnabled(boolean set) {
      if (set) {
         this.textureMapMask |= BUMP_MAP_FLAG;
      } else {
         this.textureMapMask &= ~BUMP_MAP_FLAG;
      }
   }
   
   public void setMixTextureColorDiffuse(boolean enable) {
      if (enable) {
         this.textureColorMixMask |= MIX_DIFFUSE_FLAG;
      } else {
         this.textureColorMixMask &= ~MIX_DIFFUSE_FLAG;
      }
   }
   
   public void setMixTextureColorSpecular(boolean enable) {
      if (enable) {
         this.textureColorMixMask |= MIX_SPECULAR_FLAG;
      } else {
         this.textureColorMixMask &= ~MIX_SPECULAR_FLAG;
      }
   }
   
   public void setMixTextureColorEmission(boolean enable) {
      if (enable) {
         this.textureColorMixMask |= MIX_EMISSION_FLAG;
      } else {
         this.textureColorMixMask &= ~MIX_EMISSION_FLAG;
      }
   }
   
   public void setMixVertexColorDiffuse(boolean enable) {
      if (enable) {
         this.vertexColorMixMask |= MIX_DIFFUSE_FLAG;
      } else {
         this.vertexColorMixMask &= ~MIX_DIFFUSE_FLAG;
      }
   }
   
   public void setMixVertexColorSpecular(boolean enable) {
      if (enable) {
         this.vertexColorMixMask |= MIX_SPECULAR_FLAG;
      } else {
         this.vertexColorMixMask &= ~MIX_SPECULAR_FLAG;
      }
   }
   
   public void setMixVertexColorEmission(boolean enable) {
      if (enable) {
         this.vertexColorMixMask |= MIX_EMISSION_FLAG;
      } else {
         this.vertexColorMixMask &= ~MIX_EMISSION_FLAG;
      }
   }

   public boolean hasTextureMap () {
      return hasColorMap () || hasBumpMap () || hasNormalMap ();
   }
   
}
