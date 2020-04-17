import sys
class NonconformingSizeException(Exception):
   "Jython exception for non-conforming argument sizes"

from maspack.util import *
from maspack.matrix import *
from maspack.geometry import *
from maspack.collision import *
from maspack.render import *
from maspack.solvers import *
from artisynth.core.mechmodels import *
from artisynth.core.mechmodels.MechSystemSolver import Integrator
from artisynth.core.mechmodels.MechSystemSolver import PosStabilization
from artisynth.core.mechmodels import *
from artisynth.core.femmodels import *
# need to explicitly import FemModel3d; not sure why - maybe Jython
# can't grok lambdas?
from artisynth.core.femmodels import FemModel3d
from artisynth.core.materials import *
from artisynth.core.modelbase import *
from artisynth.core.driver import *
from artisynth.core.workspace import *
from artisynth.core.inverse import *
from java.lang import *
from java.io import *

def inv(m):
   try:
      return JythonMatrixSupport.mOInvert (m)
   except ImproperSizeException:
      raise NonconformingSizeException, "matrix must be square"

def tsp(m):
   return JythonMatrixSupport.mOTranspose (m)

def setFormat(s):
   try:
      JythonMatrixSupport.setFormat(s)
   except IllegalArgumentException:
      raise ValueError, "improper format string"

# Vector__str__ and Matrix__str__ aren't useful because there is no easy
# way to rebind the toString method used by the interpreter - the
# intepreter calls __str__ of PyJavaInstance, which in turn calls
# the original toString() method of the object
def Vector__str__(self):
   return JythonMatrixSupport.toStr(self)

def Matrix__str__(self):
   return JythonMatrixSupport.toStr(self)

def VectorNd__abs__(self):
   tmp = JythonMatrixSupport.vNCopy(self)
   tmp.abs();
   return tmp
def VectorNd__pos__(self):
   return JythonMatrixSupport.vNCopy(self)
def VectorNd__neg__(self, v):
   tmp = JythonMatrixSupport.vNCopy(self)
   tmp.negate();
   return tmp
def VectorNd__add__(self, v):
   try:
      return JythonMatrixSupport.vNAdd (self, v)
   except ImproperSizeException:
      raise NonconformingSizeException, "argument sizes do not conform"
def VectorNd__div__(self, s):
   return JythonMatrixSupport.vNScale (self, 1/float(s))
def VectorNd__sub__(self, v):
   try:
      return JythonMatrixSupport.vNSub (self, v)
   except ImproperSizeException:
      raise NonconformingSizeException, "argument sizes do not conform"
def VectorNd__rsub__(self, v):
   try:
      return JythonMatrixSupport.vNSubLeft (self, v)
   except ImproperSizeException:
      raise NonconformingSizeException, "argument sizes do not conform"
def VectorNd__mul__(self, v):
   return JythonMatrixSupport.vNScale (self, v)
def VectorNd__div__(self, v):
   return JythonMatrixSupport.vNScale (self, 1/float(v))
def VectorNd__iadd__(self, v):
   try:
      JythonMatrixSupport.add (self, v)
      return self
   except ImproperSizeException:
      raise NonconformingSizeException, "argument sizes do not conform"  
def VectorNd__isub__(self, v):
   try:
      JythonMatrixSupport.sub (v)
      return self
   except ImproperSizeException:
      raise NonconformingSizeException, "argument sizes do not conform"  
def VectorNd__imul__(self, s):
   self.scale(s);
   return self
def VectorNd__idiv__(self, s):
   self.scale(1/float(s));
   return self
def Vector3d__add__(self, v):
   tmp = Vector3d()
   tmp.add (self, v)
   return tmp
def Vector3d__mul__(self, s):
   tmp = Vector3d()
   tmp.scale (s, self)
   return tmp
def Vector3d__sub__(self, v):
   tmp = Vector3d()
   tmp.sub (self, v)
   return tmp
def Vector3d__rsub__(self, v):
   tmp = Vector3d()
   tmp.sub (v, self)
   return tmp
def Vector__len__(self):
   return self.size()

def Matrix__mul__(self, m):
   try:
      res = JythonMatrixSupport.mOMul (self, m)
      if isinstance(res,Double):
         return Double.doubleValue()
      else:
         return res
   except ImproperSizeException:
      raise NonconformingSizeException, "argument sizes do not conform" 

def Vector__getitem__(self, key):
   if isinstance(key,int):
      return self.get(key)
   else:
      tmp = VectorNd((key.stop-key.start)/key.step)
      k = key.start
      if k == None:
         k = 0
      inc = key.step
      if inc == None:
         inc = 1
      j = 0
      while k < key.stop:
         tmp.set(j,self.get(k))
         k += inc
         j += 1
      return tmp
def Vector__setitem__(self, key, src):
   if isinstance(key,int):
      self.set(key,src)
   else:
      srcSize = (key.stop-key.start)/key.step
      k = key.start
      if k == None:
         k = 0
      inc = key.step
      if inc == None:
         inc = 1
      j = 0
      if isinstance(src,list):
         while k < key.stop:
            self.set(k,src[j])
            k += inc
            j += 1
      else:
         while k < key.stop:
            self.set(k,src.get(j))
            k += inc
            j += 1
def Matrix__getitem__(self, ij):
   return self.get(ij[0],ij[1])
def Matrix__setitem__(self, ij, v):
   return self.set(ij[0],ij[1],v)

setattr(VectorNd, '__abs__', VectorNd__abs__)
setattr(VectorNd, '__neg__', VectorNd__neg__)
setattr(VectorNd, '__pos__', VectorNd__pos__)
setattr(VectorNd, '__add__', VectorNd__add__)
setattr(VectorNd, '__radd__', VectorNd__add__)
setattr(VectorNd, '__sub__', VectorNd__sub__)
setattr(VectorNd, '__rsub__', VectorNd__rsub__)
setattr(VectorNd, '__mul__', VectorNd__mul__)
setattr(VectorNd, '__rmul__', VectorNd__mul__)
setattr(VectorNd, '__div__', VectorNd__div__)
setattr(VectorNd, '__iadd__', VectorNd__iadd__)
setattr(VectorNd, '__isub__', VectorNd__isub__)
setattr(VectorNd, '__idiv__', VectorNd__idiv__)
setattr(VectorNd, '__imul__', VectorNd__imul__)

setattr(Vector3d, '__add__', Vector3d__add__)
setattr(Vector3d, '__radd__', Vector3d__add__)
setattr(Vector3d, '__sub__', Vector3d__sub__)
setattr(Vector3d, '__rsub__', Vector3d__rsub__)
setattr(Vector3d, '__mul__', Vector3d__mul__)
setattr(Vector3d, '__rmul__', Vector3d__mul__)

setattr(Matrix, '__mul__', Matrix__mul__)

setattr(Vector, '__getitem__', Vector__getitem__)
setattr(Vector, '__setitem__', Vector__setitem__)
setattr(Matrix, '__getitem__', Matrix__getitem__)
setattr(Matrix, '__setitem__', Matrix__setitem__)

# string methods not really useful -- see above
setattr(VectorBase, 'toString', Vector__str__)
setattr(MatrixBase, 'toString', Matrix__str__)

#setFormat ("%.5g") - don't set default numeric format by default
#VectorBase.setColumnVectorStringsVertical(1)
