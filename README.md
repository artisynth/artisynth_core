ArtiSynth: Combined MultiBody/FEM Simulation
--------------------------------------------

This is the core distribution of ArtiSynth, a 3D mechanical modeling
system that supports the combined simulation of multibody and finite
element (FEM) models, together with contact and constraints.  It is
targeted predominantly at biomechanical and biomedical applications,
but can also be used for general purpose mechanical simulation. It is
freely available under a two-clause BSD-style open source license.

The system is implemented in Java, and provides a rich set of modeling
components, including particles, rigid bodies, finite elements with
both linear and nonlinear materials, point-to-point muscles, and
various bilateral and unilateral constraints including contact. A
graphical interface allows interactive component navigation, model
editing, and simulation control.

Full details are provided on the ArtiSynth website at
[www.artisynth.org](https://www.artisynth.org).

Installation instructions, including what to do after
you clone from Github, are available at
[www.artisynth.org/installGuides](https://www.artisynth.org/installGuides).

Other documentation on how to use the system and build models is
available at [www.artisynth.org/doc](https://www.artisynth.org/doc).

--------------------------------------------------------------------

### Files in the top directory:

<dl>

<dt>.classpath</dt>
<dd>Classpath information used by the Eclipse IDE</dd>

<dt>.git*</dt>
<dd>Repository and configuration information used by the Git SCM</dd>

<dt>.project</dt>
<dd>Project information used by the Eclipse IDE</dd>

<dt>ArtiSynth.launch</dt>
<dd>Default "launch" configuration for used by the Eclipse IDE</dd>

<dt>ArtiSynth_launch</dt>
<dd>Backup copy of the default version of ArtiSynth.launch</dd>

<dt>.settings/</dt>
<dd>Settings information used by the Eclipse IDE</dd>

<dt>DISTRO_EXCLUDE</dt>
<dd>Files that should be excluded from the current precompiled distribution</dd>

<dt>EXTCLASSPATH.sample</dt>
<dd>Example EXTCLASSPATH file. An EXTCLASSPATH files indicates to the
ArtiSynth Launcher additional classpaths that should be used when
searching for models.</dd>

<dt>LICENSE</dt>
<dd>Licensing and terms of use</dd>

<dt>Makefile</dt>
<dd>Makefile for compiling and doing certain maintenance operations in
a shell environment</dd>

<dt>Makefile.base</dt>
<dd>Base definitions for Makefile and Makefile in subdirectories</dd>

<dt>README.md</dt>
<dd>This file</dd>

<dt>VERSION</dt>
<dd>Current distribution version</dd>

<dt>bin/</dt>
<dd>Stand-alone programs, mostly implemented as scripts.
The program 'artisynth' starts up the ArtiSynth system.</dd>

<dt>classes/</dt>
<dd>Root directory for compiled classes</dd>

<dt>demoModels.txt</dt>
<dd>List of "banner" demo models available when ArtiSynth starts up</dd>

<dt>doc/</dt>
<dd>System documenation</dd>

<dt>eclipseSettings.zip</dt>
<dd>Default project settings for the Eclipse IDE (obsolete since these
are now checked directly into the repository)</dd>

<dt>lib/</dt>
<dd>Java libraries, plus architecture-specific libraries for native
code support, mostly involving linear solvers, Java OpenGL (JOGL), and
collision detection</dd>

<dt>mainModels.txt</dt>
<dd>List of "banner" anatomical models in the ArtiSynth Models package
(which must be installed)</dd>

<dt>matlab/</dt>
<dd>Matlab scripts for running ArtiSynth from matlab</dd>

<dt>modelMenu.xml</dt>
<dd>Default configuraion for the model menu</dd>
    
<dt>scriptMenu.xml</dt>
<dd>Default configuraion for the model menu</dd>
    
<dt>scripts/</dt>
<dd>Jython scripts for basic testing, etc. Some of these may assume
the installation of additional projects.</dd>

<dt>setup.bash</dt>
<dd>Example ArtiSynth setup script for bash</dd>

<dt>setup.csh</dt>
<dd>Example ArtiSynth setup script for chs/tcsh</dd>

<dt>src/</dt>
<dd>ArtiSynth source code</dd>

<dt>support/</dt>
<dd>Configuration information for external IDEs and support software,
including default settings for the Eclipse IDE</dd>

<dt>tmp/</dt>
<dd>Temp directory</dd>

</dl>
