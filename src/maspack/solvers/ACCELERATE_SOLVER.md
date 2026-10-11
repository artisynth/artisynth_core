# The Accelerate solver (Arm-based Macs)

`maspack.solvers.AccelerateSolver` is a `DirectSolver` based on the sparse
direct solvers in Apple's Accelerate framework (`Sparse/Solve.h`). It is
available only on Apple Silicon, under an arm64 JVM, where there is no
Pardiso (Pardiso needs MKL, which is x86 only). It is the default direct
solver there, with MUMPS as the alternative.

Building the native library (`libAccelerateJNI.1.0.dylib`) is described
in `lib/ACCELERATE_COMPILATION.md`.

## Selecting it

On arm64 Macs, `SparseSolverId.getAvailableDirectSolvers()` returns
Accelerate first and then MUMPS. `getDefaultDirectSolver()` returns the
first entry, so Accelerate is the default there. To choose a solver
explicitly:

- `SparseSolverId.Accelerate` or `SparseSolverId.Mumps`, e.g. in
  `MechSystemSolver.setMatrixSolver`.
- `artisynth -matrixSolver Accelerate` (or `Mumps`).
- `new AccelerateSolver()` directly. Use `AccelerateSolver.isAvailable()`
  to check first; the constructor throws
  `UnsupportedOperationException` if the native library can't be loaded.

## Factorizations

| Matrix | Factorization | Notes |
|---|---|---|
| symmetric (SPD or indefinite, including KKT) | LDL^T with threshold partial pivoting (`SparseFactorizationLDLTTPP`) | |
| unsymmetric | LU with threshold partial pivoting (`SparseFactorizationLUTPP`) | **macOS 15.5 or later** |

Only the TPP variant of LDL^T is reliable on saddle-point (KKT) systems:

- **Bunch-Kaufman (`LDLTSBK`):** uses static pivoting, and returns
  *wrong solutions with status OK* on them (relative residuals up to
  1e39).
- **Unpivoted:** fails on the zero diagonal block.

The default pivot and zero tolerances (0.01 and 1e-4*eps) and the default
equilibration scaling are used.

The matrices are passed without copying or reordering:

- **Symmetric:** ArtiSynth's 1-based CRS upper triangle is the 0-based CSC
  lower triangle.
- **Unsymmetric:** a full CRS matrix is the CSC storage of its transpose,
  and is passed with `attributes.transpose = true`.

### LU availability

LU was added to Accelerate in macOS 15.5. The library still targets macOS
13.0, so LU is checked for at run time:

- `AccelerateSolver.isLUAvailable()` reports whether LU can be used.
- `SparseSolverId.Accelerate.getMatrixType()` returns
  `Matrix.INDEFINITE` (any matrix) when LU is available, and
  `Matrix.SYMMETRIC` otherwise. `isCompatible()` uses this.
- Without LU, `analyze()` of an unsymmetric matrix fails with "unsymmetric
  matrices are not supported".
- `KKTSolver` checks compatibility when it analyzes the matrix. If the
  chosen solver can't handle an unsymmetric system, it switches to the
  default direct solver, or to MUMPS if the default can't handle it
  either, with a one-time warning. On arm64 that means MUMPS.

The environment variable `ACCELERATE_NO_LU` (any value) simulates a
pre-15.5 system.

## Ordering

The fill-reducing ordering defaults to **METIS**. Accelerate's own default,
AMD, is unusable on KKT systems: it was 15x slower (38 s vs 2.4 s on a
259k system). The environment variable `ACCELERATE_ORDER` selects another
ordering:

- `amd`.
- `metis`, the default.
- `mtmetis`, which needs macOS 26 and otherwise falls back to METIS. Its
  analysis is 3-5x faster, but its factorization of KKT systems is slower.

## Solves and iterative refinement

Accelerate provides no iterative refinement, so the wrapper does it. After
each solve, it computes r = b - A x from the factored values, solves
A dx = r, and sets x += dx.

- The default is 1 step, as for Pardiso with symmetric matrices.
- The step count can be set with `setMaxRefinementSteps()` or the
  environment variable `ACCELERATE_REFINE`.
- `getNumRefinementSteps()` reports the last solve's count.

On a synthetic KKT test, one step reduced the relative residual from
1.3e-14 to 1.1e-15. A second step gained nothing, and in the ArtiSynth
test suites refinement made no measurable difference.

Multiple right-hand sides are supported (`hasMultipleRhsSolves()`).

## Hybrid (iterative) solves

`iterativeSolve()` uses GMRES or CGS, preconditioned by the most recent
factorization, through the same `HybridSolver` code (`lib/hybridSolve.cc`)
as MUMPS. Each preconditioner application is a single `SparseSolve`, with
no refinement. `getLastIterativeTimes()` breaks down the time of the last
iterative solve.

## Inertia and singularity

- `getNumNegEigenvalues()` and `getNumPosEigenvalues()` come from
  `SparseGetInertia`, which is exact for LDL^T, including SPD matrices.
- `getNumPerturbedPivots()` returns the number of zero pivots (below the
  zero tolerance). This is the only null-pivot information Accelerate
  provides.
- All three return -1 for unsymmetric (LU) factorizations.

## Threads and repeatability

Accelerate parallelizes internally, over all cores, and has no thread
control:

- The thread count methods only record a value.
- `getPhysicalCoreCount()` reports the performance-core count from the
  shared native code, but it has no effect on Accelerate.
- **Results are not bit-repeatable from run to run.** In one comparison,
  63 of 102 mechmodelTest sections were identical between two runs.
  MUMPS, in contrast, is bit-repeatable.

## Error handling

The wrapper installs a `reportError` callback. Without one, Accelerate
handles a parameter error (e.g. solving with a failed factorization) by
logging it and executing `__builtin_trap()`, which would kill the JVM
(SIGTRAP). Errors are instead returned as exceptions, with Accelerate's
message attached.

## Performance

IncompressDemo, 200 steps, average factor + solve time in msec:

| solver | direct | hybrid |
|---|---:|---:|
| x86_64 Pardiso (Rosetta) | 13.5 | 7.1 |
| arm64 MUMPS | 57 | 19.5 |
| arm64 Accelerate | 11 | 7.2 |

Synthetic saddle-point systems `[M G'; G 0]`, where M is a 7-point
Laplacian with 20% constraint rows. Factorization wall time in seconds,
with CPU time in parentheses:

| size | Accelerate (METIS) | MUMPS 1 thread | MUMPS 4 threads | MUMPS 8 threads |
|---|---|---|---|---|
| 32k | **0.04** (0.19) | 0.10 (0.10) | 0.06 (0.20) | 0.08 (0.58) |
| 77k | **0.21** (1.14) | 0.41 (0.41) | 0.34 (1.19) | 0.31 (1.91) |
| 259k | 2.42 (13.3) | 3.24 (3.23) | 2.03 (4.92) | **1.99** (7.74) |

So Accelerate is about 1.5x faster than MUMPS at 30-80k unknowns and
about 1.2x slower at 260k. It uses 5-6 CPU seconds per wall second, where
MUMPS at 8 threads uses about 4.

## Verification

- `AccelerateSolverTest` passes. It runs the common `DirectSolverTestBase`
  tests (SPD, indefinite, singular, refinement, hybrid) and, with LU, an
  exact 3x3 unsymmetric test and a large perturbed unsymmetric matrix:
  - direct solve error 1.1e-11;
  - GMRES converges in 9 iterations and CGS in 5.

  With `ACCELERATE_NO_LU=1`, it checks that unsymmetric matrices are
  rejected.
- **FrameSpringDemo with unsymmetric Jacobians**, made by calling
  `FrameSpring.setJacobianSymmetric(false)` on its springs: Accelerate's
  LU and MUMPS agree to 1.4e-14 over 2 s. With `ACCELERATE_NO_LU=1`, the
  `KKTSolver` warning is printed and the run is identical to MUMPS.
- **The core test suites** were run with x86_64 Pardiso, arm64 MUMPS and
  arm64 Accelerate, with and without refinement and hybrid solves. There
  were no crashes, exceptions or hangs.
  - Most sections agree to about 1e-14 across all solvers.
  - Contact-heavy or chaotic models (LaymanDemo, BeamBodyCollide,
    RigidCollisionTest, ...) differ for every solver pair, including
    Pardiso vs MUMPS.
- **Arm26FemElbow** sometimes goes 0.44 off Pardiso and MUMPS. This is a
  rounding-sensitive bifurcation, and it shows up in some Accelerate runs
  and not others.
- **JawLarynxDemo (Trapezoidal)** differs from Pardiso and MUMPS by
  7.8e-2.
  - The cause is a unilateral bite constraint that is released when its
    contact speed is >= `RigidBodyCoupling.myBreakSpeed`, which defaults
    to 0. That tests the *sign* of rounding noise.
  - Pardiso and MUMPS agree only by luck: both see a slightly positive
    speed, while Accelerate sees a slightly negative one.
  - With a break speed of 1e-8, all three agree to <1e-13.

## Limitations

- macOS on arm64 only; no Linux or Windows counterpart.
- No thread control, and results are not bit-repeatable.
- Fewer controls than MUMPS:
  - only the AMD, METIS and MTMetis orderings;
  - no null-pivot detection beyond the zero-pivot count;
  - nothing like MUMPS' adaptive scaling (`ICNTL(8)`), which mattered for
    two hard cases in `MUMPS_SOLVER.md`. Those cases have not been tried
    with Accelerate.
- Unsymmetric matrices need macOS 15.5 or later.
- Only tested on macOS 26 so far.
