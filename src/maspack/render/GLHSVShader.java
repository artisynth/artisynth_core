/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.nio.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;

// see http://www.lighthouse3d.com/opengl/glsl/index.php?intro

public class GLHSVShader {
   private static boolean myInitialized = false;
   private static int myProgram = -1;

   private static String[] testProg = new String[] {
      "void main()",
      "{",
      "    gl_FragColor = vec4(0.4,0.4,0.8,1.0);",
      "}"
   };

   // John Lloyd, Nov 2015 replaced 
   //
   //    hIndex = int(hRound) % 6;",
   //
   // with
   //
   //    hIndex = int(hRound);
   //    ...
   //    if (hIndex == 0 || hIndex == 6) 
   //
   // since default shader compiler on MacOS does not support '%"

   private static String[] hsvProg = new String[] {
      "vec4 HSVtoRGB(vec4 color)",
      "{",
      "    float f,p,q,t, hRound;",
      "    int hIndex;",
      "    float h, s, v;",
      "    vec3 result;",
      "",
      "    /* just for clarity */",
      "    h = color.r;",
      "    s = color.g;",
      "    v = color.b;",
      "",
      "    hRound = floor(h * 6.0);",
      "    hIndex = int(hRound);",
      "    f = (h*6.0) - hRound;",
      "    p = v*(1.0 - s);",
      "    q = v*(1.0 - f*s);",
      "    t = p + v*s*f;",
      "",
      "    if (hIndex == 0 || hIndex == 6) {",
      "        result = vec3(v,t,p);",
      "    }",
      "    else if (hIndex == 1) {",
      "        result = vec3(q,v,p);",
      "    }",
      "    else if (hIndex == 2) {",
      "        result = vec3(p,v,t);",
      "    }",
      "    else if (hIndex == 3) {",
      "        result = vec3(p,q,v);",
      "    }",
      "    else if (hIndex == 4) {",
      "        result = vec3(t,p,v);",
      "    }",
      "    else {",
      "        result = vec3(v,p,q);",
      "    }",
      "    return vec4(result.r, result.g, result.b, color.a);",
      "}",
      "void main()",
      "{",
      "    gl_FragColor = HSVtoRGB(gl_Color);",
      "}"
   };

   public static void printInfoLog (GL2 gl, int obj) {
      //int[] infologLength = new int[1];
      //int[] charsWritten = new int[1];
      IntBuffer ibuf = IntBuffer.allocate (100);

      gl.glGetObjectParameterivARB(
         obj, gl.GL_OBJECT_INFO_LOG_LENGTH_ARB, ibuf);
      int infologLength = ibuf.get(0);
      
      if (infologLength > 0) {
         //byte[] buffer = new byte[64000];
         ByteBuffer buffer = ByteBuffer.allocate (infologLength);
         gl.glGetInfoLogARB (obj, infologLength, ibuf, buffer);
         //System.out.println (new String(buffer, 0, charsWritten[0]));
         System.out.println (new String(buffer.array()));
      }
      
   }

   public static int getStatus (GL2 gl, int obj, int type) {
      IntBuffer ibuf = IntBuffer.allocate (100);

      gl.glGetObjectParameterivARB (obj, type, ibuf);
      return ibuf.get(0);      
   }

   public static int createShaderProgram(GL2 gl) {

      System.out.println ("Initializing HSV shader ...");
      int shader = gl.glCreateShaderObjectARB (GL2.GL_FRAGMENT_SHADER);

      gl.glShaderSourceARB (shader, hsvProg.length, hsvProg, null);
      gl.glCompileShaderARB (shader);
      int status = getStatus (gl, shader, GL2.GL_OBJECT_COMPILE_STATUS_ARB);
      if (status != 1) {
         System.out.println ("Error compiling HSV shader");
         printInfoLog (gl, shader);
         return -1;
      }
      int prog = gl.glCreateProgramObjectARB();
      gl.glAttachObjectARB(prog,shader);
      gl.glLinkProgramARB (prog);
      status = getStatus (gl, prog, GL2.GL_OBJECT_LINK_STATUS_ARB);
      if (status != 1) {
         System.out.println ("Error linking HSV shader");
         printInfoLog (gl, prog);
         return -1;
      }
      System.out.println ("HSV shader successfully created");
      return prog;
   }

   public static int getShaderProgram (GL2 gl) {
      if (!myInitialized) {
         myProgram = createShaderProgram (gl);
         myInitialized = true;
      }
      return myProgram;
   }

}
