package maspack.render.GL;
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.JFrame;
 
import com.jogamp.common.nio.Buffers;
 
/**
 * inspired from http://www.lighthouse3d.com/cg-topics/code-samples/opengl-3-3-glsl-1-5-sample/
 * 
 */
public class GL3Sample implements GLEventListener {
 
   enum ShaderType{ VertexShader, FragmentShader}
 
   // Data for drawing Axis
   float verticesAxis[] = { 
      -1.0f,  0.0f, 0.0f, 1.0f, 
      1.0f,  0.0f, 0.0f, 1.0f,
      0.0f, -1.0f, 0.0f, 1.0f, 
      0.0f,  1.0f, 0.0f, 1.0f};
 
   float colorAxis[] = { 
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f, 
   };
 
   // Data for triangle  x,y,z,w
   float vertices[] = {	0.8f, 0.2f, .0f, 1.0f,  
                        0.2f, 0.2f, .0f, 1.0f,
                        0.5f, 0.8f, .0f, 1.0f };
 
   float colorArray[] = {  // RGBA
      0.0f, 0.0f, 1.0f, 1.0f, 
      1.0f, 0.0f, 0.0f, 1.0f, 
      0.0f, 0.0f, 1.0f, 1.0f };
 
   // Program
   int programID;
 
   // Vertex Attribute Locations
   int vertexLoc, colorLoc;
 
   // Uniform variable Locations
   int projMatrixLoc, viewMatrixLoc;
 
   // storage for Matrices
   float projMatrix[] = new float[16];
   float viewMatrix[] = new float[16];
 
   protected int triangleVAO;
   protected int axisVAO;
 
   // ------------------
   // VECTOR STUFF
   //
 
   // res = a cross b;
   void crossProduct(float a[], float b[], float res[]) {
 
      res[0] = a[1] * b[2] - b[1] * a[2];
      res[1] = a[2] * b[0] - b[2] * a[0];
      res[2] = a[0] * b[1] - b[0] * a[1];
   }
 
   // Normalize a vec3
   void normalize(float a[]) {
 
      float mag = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
 
      a[0] /= mag;
      a[1] /= mag;
      a[2] /= mag;
   }
 
   // ----------------
   // MATRIX STUFF
   //
 
   // sets the square matrix mat to the identity matrix,
   // size refers to the number of rows (or columns)
   void setIdentityMatrix(float[] mat, int size) {
 
      // fill matrix with 0s
      for (int i = 0; i < size * size; ++i)
         mat[i] = 0.0f;
 
      // fill diagonal with 1s
      for (int i = 0; i < size; ++i)
         mat[i + i * size] = 1.0f;
   }
 
   //
   // a = a * b;
   //
   void multMatrix(float[] a, float[] b) {
 
      float[] res = new float[16];
 
      for (int i = 0; i < 4; ++i) {
         for (int j = 0; j < 4; ++j) {
            res[j * 4 + i] = 0.0f;
            for (int k = 0; k < 4; ++k) {
               res[j * 4 + i] += a[k * 4 + i] * b[j * 4 + k];
            }
         }
      }
      System.arraycopy(res, 0, a, 0, 16);
   }
 
   // Defines a transformation matrix mat with a translation
   void setTranslationMatrix(float[] mat, float x, float y, float z) {
 
      setIdentityMatrix(mat, 4);
      mat[12] = x;
      mat[13] = y;
      mat[14] = z;
   }
 
   // ------------------
   // Projection Matrix
   //
 
   float[] buildProjectionMatrix(float fov, float ratio, float nearP, float farP, float[] projMatrix) {
 
      float f = 1.0f / (float) Math.tan(fov * (Math.PI / 360.0));
 
      setIdentityMatrix(projMatrix, 4);
 
      projMatrix[0] = f / ratio;
      projMatrix[1 * 4 + 1] = f;
      projMatrix[2 * 4 + 2] = (farP + nearP) / (nearP - farP);
      projMatrix[3 * 4 + 2] = (2.0f * farP * nearP) / (nearP - farP);
      projMatrix[2 * 4 + 3] = -1.0f;
      projMatrix[3 * 4 + 3] = 0.0f;
 
      return projMatrix;
   }
 
   // ------------------
   // View Matrix
   //
   // note: it assumes the camera is not tilted,
   // i.e. a vertical up vector (remmeber gluLookAt?)
   //
 
   float[] setCamera(float posX, float posY, float posZ, float lookAtX,
                     float lookAtY, float lookAtZ, float[] viewMatrix) {
 
      float[] dir = new float[3];
      float[] right = new float[3];
      float[] up = new float[3];
 
      up[0] = 0.0f;
      up[1] = 1.0f;
      up[2] = 0.0f;
 
      dir[0] = (lookAtX - posX);
      dir[1] = (lookAtY - posY);
      dir[2] = (lookAtZ - posZ);
      normalize(dir);
 
      crossProduct(dir, up, right);
      normalize(right);
 
      crossProduct(right, dir, up);
      normalize(up);
 
      float[] aux = new float[16];
 
      viewMatrix[0] = right[0];
      viewMatrix[4] = right[1];
      viewMatrix[8] = right[2];
      viewMatrix[12] = 0.0f;
 
      viewMatrix[1] = up[0];
      viewMatrix[5] = up[1];
      viewMatrix[9] = up[2];
      viewMatrix[13] = 0.0f;
 
      viewMatrix[2] = -dir[0];
      viewMatrix[6] = -dir[1];
      viewMatrix[10] = -dir[2];
      viewMatrix[14] = 0.0f;
 
      viewMatrix[3] = 0.0f;
      viewMatrix[7] = 0.0f;
      viewMatrix[11] = 0.0f;
      viewMatrix[15] = 1.0f;
 
      setTranslationMatrix(aux, -posX, -posY, -posZ);
 
      multMatrix(viewMatrix, aux);
 
      return viewMatrix;
   }
 
   // ------------------
 
   void changeSize(GL3 gl, int w, int h) {
 
      float ratio;
      // Prevent a divide by zero, when window is too short
      // (you cant make a window of zero width).
      if (h == 0)
         h = 1;
 
      // Set the viewport to be the entire window
      //gl.glViewport(0, 0, w, h);
 
      ratio = (1.0f * w) / h;
      this.projMatrix = buildProjectionMatrix(53.13f, ratio, 1.0f, 30.0f, this.projMatrix);
   }
 
   void setupBuffers(GL3 gl) {
      // generate the IDs
      this.triangleVAO = this.generateVAOId(gl);
      this.axisVAO = this.generateVAOId(gl);
 
      // create the buffer and link the data with the location inside the vertex shader
      this.newFloatVertexAndColorBuffers(gl, this.triangleVAO, 
                                         this.vertices, this.colorArray, this.vertexLoc, this.colorLoc);
      this.newFloatVertexAndColorBuffers(gl, this.axisVAO,
                                         this.verticesAxis, this.colorAxis, this.vertexLoc, this.colorLoc);
   }
 
   void newFloatVertexAndColorBuffers(GL3 gl, int vaoId, float[] verticesArray, float[] colorArray, int verticeLoc, int colorLoc){
      // bind the correct VAO id
      gl.glBindVertexArray( vaoId);
      // Generate two slots for the vertex and color buffers
      int vertexBufferId = this.generateBufferId(gl);
      int colorBufferId = this.generateBufferId(gl);
 
      // bind the two buffer
      this.bindBuffer(gl, vertexBufferId, verticesArray, verticeLoc);
      this.bindBuffer(gl, colorBufferId, colorArray, colorLoc);
   }
 
   void bindBuffer(GL3 gl, int bufferId, float[] dataArray, int dataLoc){
      // bind buffer for vertices and copy data into buffer
      gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
      gl.glBufferData(GL.GL_ARRAY_BUFFER, dataArray.length * Float.SIZE / 8,
                      Buffers.newDirectFloatBuffer(dataArray), GL.GL_STATIC_DRAW);
      gl.glEnableVertexAttribArray(dataLoc);
      gl.glVertexAttribPointer(dataLoc, 4, GL.GL_FLOAT, false, 0, 0);
 
   }
 
   protected int generateVAOId(GL3 gl) {
      // allocate an array of one element in order to strore 
      // the generated id
      int[] idArray = new int[1];
      // let's generate
      gl.glGenVertexArrays(1, idArray, 0);
      // return the id
      return idArray[0];
   }
 
   protected int generateBufferId(GL3 gl) {
      // allocate an array of one element in order to strore 
      // the generated id
      int[] idArray = new int[1];
      // let's generate
      gl.glGenBuffers( 1, idArray, 0);
 
      // return the id
      return idArray[0];
   }
 
   protected void renderScene(GL3 gl) {
 
      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
 
      setCamera(	0.5f, 0.5f,  2, 
                        0.5f, 0.5f, -1,
                        this.viewMatrix);
 
      gl.glUseProgram(this.programID);
 
      // must be called after glUseProgram
      // set the view and the projection matrix 
      gl.glUniformMatrix4fv( this.projMatrixLoc, 1, false, this.projMatrix, 0);
      gl.glUniformMatrix4fv( this.viewMatrixLoc, 1, false, this.viewMatrix, 0);
 
      gl.glBindVertexArray(this.triangleVAO);
      gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
 
      gl.glBindVertexArray(this.axisVAO);
      gl.glDrawArrays(GL.GL_LINES, 0, 4);
 
      // Check out error
      int error = gl.glGetError();
      if(error!=0){
         System.err.println("ERROR on render : " + error);}
   }
 
   /** Retrieves the info log for the shader */
   public String getShaderInfoLog(GL3 gl, int obj) {
      // Otherwise, we'll get the GL info log
      final int logLen = getShaderParameter(gl, obj, GL3.GL_INFO_LOG_LENGTH);
      if (logLen <= 0)
         return "";
 
      // Get the log
      final int[] retLength = new int[1];
      final byte[] bytes = new byte[logLen + 1];
      gl.glGetShaderInfoLog(obj, logLen, retLength, 0, bytes, 0);
      final String logMessage = new String(bytes);
 
      return String.format("ShaderLog: %s", logMessage);
   }
 
   /** Get a shader parameter value. See 'glGetShaderiv' */
   private int getShaderParameter(GL3 gl, int obj, int paramName) {
      final int params[] = new int[1];
      gl.glGetShaderiv(obj, paramName, params, 0);
      return params[0];
   }
 
   /** Retrieves the info log for the program */
   public String printProgramInfoLog(GL3 gl, int obj) {
      // get the GL info log
      final int logLen = getProgramParameter(gl, obj, GL3.GL_INFO_LOG_LENGTH);
      if (logLen <= 0)
         return "";
 
      // Get the log
      final int[] retLength = new int[1];
      final byte[] bytes = new byte[logLen + 1];
      gl.glGetProgramInfoLog(obj, logLen, retLength, 0, bytes, 0);
      final String logMessage = new String(bytes);
 
      return logMessage;
   }
 
   /** Gets a program parameter value */
   public int getProgramParameter(GL3 gl, int obj, int paramName) {
      final int params[] = new int[1];
      gl.glGetProgramiv(obj, paramName, params, 0);
      return params[0];
   }
 
   protected String loadStringFileFromCurrentPackage( String fileName){
      InputStream stream = null;
      try {
         stream = new FileInputStream (fileName);
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
           
 
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      // allocate a string builder to add line per line 
      StringBuilder strBuilder = new StringBuilder();
 
      try {
         String line = reader.readLine();
         // get text from file, line per line
         while(line != null){
            strBuilder.append(line + "\n");
            line = reader.readLine();	
         }
         // close resources
         reader.close();
         stream.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
 
      return strBuilder.toString();
   }

   String vertexShader = new String (
      "#version 150\n" +
      "\n" +
      "uniform mat4 viewMatrix, projMatrix;\n" +
      "\n" +      
      "in vec4 position;\n" +
      "in vec3 color;\n" +
      "\n" + 
      "out vec3 Color;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   Color = color;\n" + 
      "   gl_Position = projMatrix * viewMatrix * position ;\n"+
      "}\n");

   String fragmentShader = new String (
      "#version 150\n" +
      "\n" +
      "in vec3 Color;\n" + 
      "out vec4 outColor;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   outColor = vec4(Color,1.0);\n" +
      "}\n"); 
 
   int newProgram(GL3 gl) {
      // create the two shader and compile them
      int v = this.newShaderFromString(gl, vertexShader, ShaderType.VertexShader);
      int f = this.newShaderFromString(gl, fragmentShader, ShaderType.FragmentShader);
 
      System.out.println(getShaderInfoLog(gl, v));
      System.out.println(getShaderInfoLog(gl, f));
 
      int p = this.createProgram(gl, v, f);
 
      gl.glBindFragDataLocation(p, 0, "outColor");
      printProgramInfoLog(gl, p);
 
      this.vertexLoc = gl.glGetAttribLocation( p, "position");
      this.colorLoc = gl.glGetAttribLocation( p, "color");
 
      this.projMatrixLoc = gl.glGetUniformLocation( p, "projMatrix");
      this.viewMatrixLoc = gl.glGetUniformLocation( p, "viewMatrix");
 
      return p;
   }
 
   private int createProgram(GL3 gl, int vertexShaderId, int fragmentShaderId) {
      // generate the id of the program
      int programId = gl.glCreateProgram();
      // attach the two shader
      gl.glAttachShader(programId, vertexShaderId);
      gl.glAttachShader(programId, fragmentShaderId);
      // link them
      gl.glLinkProgram(programId);
 
      return programId;
   }
 
   int newShaderFromString (GL3 gl, String source, ShaderType type){
      // define the shaper type from the enum
      int shaderType = type==ShaderType.VertexShader?GL3.GL_VERTEX_SHADER:GL3.GL_FRAGMENT_SHADER;
      // create the shader id
      int id = gl.glCreateShader(shaderType);
      //  link the id and the source
      gl.glShaderSource(id, 1, new String[] { source }, null);
      //compile the shader
      gl.glCompileShader(id);
 
      return id;
   }
 
   /** GL Init */
   @Override
   public void init(GLAutoDrawable drawable) {
      GL3 gl = drawable.getGL().getGL3();
      gl.glEnable(GL.GL_DEPTH_TEST);
      gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
 
      this.programID = this.newProgram(gl);
      this.setupBuffers(gl);
   }
 
   /** GL Window Reshape */
   @Override
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      float ratio;
      // Prevent a divide by zero, when window is too short
      // (you can't make a window of zero width).
      if (height == 0)
         height = 1;
 
      ratio = (1.0f * width) / height;
      this.projMatrix = buildProjectionMatrix(53.13f, ratio, 1.0f, 30.0f, this.projMatrix);
   }
 
   /** GL Render loop */
   @Override
   public void display(GLAutoDrawable drawable) {
      GL3 gl = drawable.getGL().getGL3();
      renderScene(gl);
   }
 
   /** GL Complete */
   @Override
   public void dispose(GLAutoDrawable drawable) {
   }
 
   public static JFrame newJFrame(String name, GLEventListener sample, int x,
                                  int y, int width, int height) {
      JFrame frame = new JFrame(name);
      frame.setBounds(x, y, width, height);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
      GLProfile glp = GLProfile.get(GLProfile.GL3);
      GLCapabilities glCapabilities = new GLCapabilities(glp);
      //GLCanvas canvas = new GLCanvas(glCapabilities);
      GLJPanel canvas = new GLJPanel(glCapabilities);
 
      canvas.addGLEventListener(sample);
      frame.add(canvas);
 
      return frame;
   }
 
   public static void main(String[] args) {
      // allocate the openGL application
      GL3Sample sample = new GL3Sample();
 
      // allocate a frame and display the openGL inside it
      JFrame frame = newJFrame("JOGL3 sample with Shader",
                               sample, 10, 10, 300, 200);
 
      // display it and let's go
      frame.setVisible(true);
   }
}
