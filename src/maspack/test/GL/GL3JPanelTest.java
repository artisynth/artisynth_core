package maspack.test.GL;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.FPSAnimator;

import maspack.render.Light;
import maspack.render.GL.GLLightManager;
import maspack.render.GL.GLProgramInfo;
import maspack.render.GL.GLShaderProgram;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GL3.GL3ProgramManager;
import maspack.render.GL.GL3.GL3SharedResources;
import maspack.render.GL.GL3.GL3VertexAttributeInfo;
import maspack.render.GL.GL3.GL3VertexAttributeMap;
import maspack.render.GL.GL3.GLSLGenerator;
import maspack.render.GL.GL3.GLSLGenerator.StringIntPair;
import maspack.render.GL.jogl.GLJPanel;
import maspack.util.Logger;

public class GL3JPanelTest implements GLEventListener {

    private double theta = 0;
    private double s = 0;
    private double c = 0;

    static int width = 300;
    static int height = 300;

    int VAO = 0;
    int VBO = 0;
    
    static GL3ProgramManager progManager;
    static GLLightManager lightManager;
    static GL3SharedResources resources;
    
    public static void main(String[] args) {
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers (true);
        caps.setNumSamples (8);
        caps.setPBuffer(false);
        
        //GLCanvas canvas = new GLCanvas(caps);
        
        progManager = new GL3ProgramManager();
        if (resources == null) {
           // get attribute map from GLSL generator
           StringIntPair[] attributes = GLSLGenerator.ATTRIBUTES;
           GL3VertexAttributeMap attributeMap = new GL3VertexAttributeMap (
              new GL3VertexAttributeInfo (attributes[0].getString (), attributes[0].getInt ()), 
              new GL3VertexAttributeInfo (attributes[1].getString (), attributes[1].getInt ()),
              new GL3VertexAttributeInfo (attributes[2].getString (), attributes[2].getInt ()),
              new GL3VertexAttributeInfo (attributes[3].getString (), attributes[3].getInt ()));
           for (int i=4; i<attributes.length; ++i) {
              attributeMap.add (new GL3VertexAttributeInfo (attributes[i].getString (), attributes[i].getInt ()));
           }
           resources = new GL3SharedResources(caps, attributeMap);
        }
        
        lightManager = new GLLightManager();
        setDefaultLights();
        
        final GLJPanel canvas = resources.createPanel();
        
        final Frame frame = new Frame("AWT Window Test");
        frame.setSize(width, height);
        frame.setLocation (30, 30);
        frame.add(canvas);
        frame.setVisible(true);
        
        resources.registerViewer(frame);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
               frame.remove(canvas);
               resources.deregisterViewer(frame);
               frame.dispose ();
                System.exit(0);
            }
        });

        canvas.addGLEventListener(new GL3JPanelTest());

        FPSAnimator animator = new FPSAnimator(canvas, 60);
        animator.start();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        update();
        render(drawable);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
       
       GL3 gl = drawable.getGL().getGL3();
       
       int[] buff = new int[2];
       buff[0] = VAO;
       buff[1] = VBO;
       gl.glDeleteVertexArrays(1, buff, 0);
       gl.glDeleteBuffers(1, buff, 1);
       VAO = 0;
       VBO = 0;
       
       progManager.dispose(gl);
    }
    
    private void initPrograms(GL3 gl) {
       
       // Shaders
       String vertexShaderSource = "#version 330 core\n"
           + "layout (location = 0) in vec3 position;\n"
           + "void main()\n"
           + "{\n"
           + "   gl_Position = vec4(position.x, position.y, position.z, 1.0);\n"
           + "}";
       String fragmentShaderSource = "#version 330 core\n"
           + "out vec4 color;\n"
           + "void main()\n"
           + "{\n"
           + "   color = vec4(1.0f, 0.5f, 0.2f, 1.0f);\n"
           + "}\n";
       
       
       // default program that has matrices, etc...
       progManager.getProgram(gl, new GLProgramInfo());
       
       // basic debug program
       progManager.getProgram(gl, this, 
          new String[]{vertexShaderSource, fragmentShaderSource});
       
    }
    
    public void initBuffers(GL3 gl) {
       

       // Set up vertex data (and buffer(s)) and attribute pointers
       float vertices[] = {
           -0.5f, -0.5f, 0.0f, // Left  
            0.5f, -0.5f, 0.0f, // Right 
            0.0f,  0.5f, 0.0f  // Top   
       };
       
       
       // VAO/VBO
       int[] buff = new int[2];
       gl.glGenVertexArrays(1, buff, 0);
       VAO = buff[0];
       gl.glGenBuffers(1, buff, 1);
       VBO = buff[1];
       
       // Bind the Vertex Array Object first, then bind and set vertex buffer(s) and attribute pointer(s).
       gl.glBindVertexArray(VAO);
       gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBO);
       
       // fill buffer
       FloatBuffer fbuff = Buffers.newDirectFloatBuffer (vertices.length);
       for (float f : vertices) {
          fbuff.put(f);
       }
       fbuff.flip();
       
       gl.glBufferData(GL.GL_ARRAY_BUFFER, fbuff.capacity()*4, fbuff, GL.GL_STATIC_DRAW);

       gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 3*4, 0);
       gl.glEnableVertexAttribArray(0);

       gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0); // Note that this is allowed, the call to glVertexAttribPointer registered VBO as the currently bound vertex buffer object so afterwards we can safely unbind
       gl.glBindVertexArray(0); // Unbind VAO (it's always a good thing to unbind any buffer/array to prevent strange bugs)
       
    }

    @Override
    public void init(GLAutoDrawable drawable) {
       GL3 gl = drawable.getGL().getGL3();
       
      gl.glViewport(0, 0, width, height);
      
      resources.init(drawable);
      progManager.init(gl, lightManager.numLights(), 0);
     
      // initialize buffers
      initPrograms(gl);
      initBuffers(gl);
      
      String glslVersion = gl.glGetString(GL3.GL_SHADING_LANGUAGE_VERSION);
      Logger logger = Logger.getSystemLogger();
      logger.info("GLSL Version: " + glslVersion);
      
      gl.glEnable (GL.GL_BLEND);
      gl.glBlendFunc (GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
      
    }
    
    public static void setDefaultLights() {

       //      // For debugging lights, set to R-G-B
       //      float light0_ambient[] = { 0.2f, 0.2f, 0.2f, 1.0f };
       //      float light0_diffuse[] = { 0.8f, 0.0f, 0.0f, 1.0f };
       //      float light0_specular[] = { 0, 0, 0, 1 };
       //      float light0_position[] = { 1, 0, 0, 0 };
       //      
       //      float light1_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
       //      float light1_diffuse[] = { 0.0f, 0.8f, 0.0f, 1.0f };
       //      float light1_specular[] = { 0.0f, 0.0f, 0.0f, 1.0f };
       //      float light1_position[] = { 0, 1, 0, 0 };
       //
       //      float light2_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
       //      float light2_diffuse[] = { 0.0f, 0.0f, 0.8f, 1.0f };
       //      float light2_specular[] = { 0.0f, 0.0f, 0.0f, 1.0f };
       //      float light2_position[] = { 0, 0, 1, 0 };
       
       float light0_ambient[] = { 0.1f, 0.1f, 0.1f, 1f };
       float light0_diffuse[] = { 0.8f, 0.8f, 0.8f, 1.0f };
       float light0_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };
       float light0_position[] = { -0.8660254f, 0.5f, 1f, 0f };

       float light1_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
       float light1_diffuse[] = { 0.5f, 0.5f, 0.5f, 1.0f };
       float light1_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };
       float light1_position[] = { 0.8660254f, 0.5f, 1f, 0f };

       float light2_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
       float light2_diffuse[] = { 0.5f, 0.5f, 0.5f, 1.0f };
       float light2_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };
       float light2_position[] = { 0f, -10f, 1f, 0f };

       lightManager.clearLights();
       lightManager.addLight(new Light (
          light0_position, light0_ambient, light0_diffuse, light0_specular));
       lightManager.addLight (new Light (
          light1_position, light1_ambient, light1_diffuse, light1_specular));
       lightManager.addLight(new Light (
          light2_position, light2_ambient, light2_diffuse, light2_specular));
       lightManager.setMaxIntensity(1.0f);
       
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
    }

    private void update() {
        theta += 0.01;
        s = Math.sin(theta);
        c = Math.cos(theta);
    }

    private void render(GLAutoDrawable drawable) {
       //        GL2 gl = drawable.getGL().getGL2();
       //
       //        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
       //
       //        // draw a triangle filling the window
       //        gl.glBegin(GL.GL_TRIANGLES);
       //        gl.glColor3f(1, 0, 0);
       //        gl.glVertex2d(-c, -c);
       //        gl.glColor3f(0, 1, 0);
       //        gl.glVertex2d(0, c);
       //        gl.glColor3f(0, 0, 1);
       //        gl.glVertex2d(s, -s);
       //        gl.glEnd();
       
       GL3 gl = drawable.getGL().getGL3();
       
       gl.glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
       gl.glClear(GL.GL_COLOR_BUFFER_BIT);
   
       // Draw our first triangle
       GLShaderProgram prog = progManager.getProgram(gl, this);
       prog.use(gl);
       
       gl.glBindVertexArray(VAO);
       gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
       gl.glBindVertexArray(0);
       
       prog.unuse(gl);
       
       GLSupport.checkAndPrintGLError(gl);
       
    }
}
