# Hybrid direct/iterative solves — current state

A hybrid solve uses the last factorization as a preconditioner for an
iterative solve on the current matrix values, avoiding a refactor while the
values stay close. Implemented for `PardisoSolver` and `MumpsSolver`. All
changes are uncommitted.

## How ArtiSynth uses them

- Off by default (`MechSystemSolver.myDefaultHybridSolveP = false`); enabled
  by `-enableHybridSolves` or the simulation preferences.
- Callers: `KKTSolver.factorAndSolve` and `MurtyMechSolver.hybridSolveA`,
  both with tolerance 1e-10 (from `tolExp` 10). Used only when the structure
  is unchanged (no analyze) and there are no friction/unilateral rows.
  Changes in contact force analyze + direct.
- FEM systems are passed as `Matrix.SYMMETRIC` (Pardiso mtype −2, MUMPS
  SYM=2), i.e. symmetric indefinite.
- The position-correction solve uses the velocity matrix (see
  `mechmodels/POS_CORRECTION_MATRIX.md`), so the two solves in each step are
  similar and share one factorization.

## Refactor policy: `HybridSolvePolicy`

Shared by `KKTSolver` and `MurtyMechSolver`; tested by `HybridSolvePolicyTest`.
- A cycle starts with a direct solve (time t_d), followed by iterative solves
  t_1…t_n. Refactor when t_n ≥ tavg_(n−1), where tavg = (t_d + Σt_i)/(n+1),
  or when an iterative solve fails. Each solve counts separately, not each
  step.
- Backoff: if a cycle ends at its first iterative solve, skip iterative
  solves for k solves (k = 2, doubling up to 32; reset when a first iterative
  solve succeeds).
- The time of a failed solve is not recorded. Nothing caps max solves per
  solve; it's bounded by the solver setting (50) plus the GMRES stagnation
  abort. An earlier cap of ≈ t_direct/t_precond was removed because it caused
  all the GMRES failures at 8 threads.
- Resets: KKT `analyze()` and Murty `analyzeA()` call `invalidateFactor()`;
  `initialize()` calls `reset()`. KKT `factor()` calls made outside
  `factorAndSolve()` aren't recorded.

## API

- `DirectSolver`: `enum IterativeMethod {GMRES, CGS}`,
  `set/getIterativeMethod`, `set/getIterativeMaxSolves` (default 50),
  `set/getIterativeTolerance` (relative residual ‖b−Mx‖/‖b‖, default 1e-10),
  `iterativeSolve(vals, x, b)` (> 0 = success, ≤ 0 = failure; the caller then
  refactors), and `getLastIterativeSolves/Residual` (−1 if unavailable). The
  `tolExp` forms are deprecated.
- `DirectSolverBase`: `set/getGmresRestart` (default 20), static
  `setUseCriticalArrayAccess` (default true).
- `PardisoSolver`: `setUseNativeIterativeSolve` (instance) and
  `setDefaultUseNativeIterativeSolve` (static), default false (= wrapper
  GMRES). Native = Pardiso's own `iparm[3]` CG/CGS: tolerance rounded to
  10^−L, Pardiso's own stopping test, 150-iteration limit, no stats.
  `getLastIterativeTimes()` returns ms {total, precond, matvec}.
  `setIParam/getIParam/clearIParams` exist for the harnesses only (see
  `ThreadCountSweep.md`).
- `MumpsSolver`: GMRES/CGS only; also `getLastIterativeTimes()`.

## Native implementation (`src/maspack/solvers/lib/`)

- `hybridSolve.{h,cc}`: abstract `HybridSolver` containing the loops, a MKL
  inspector-executor matvec (CSR handle over the wrapper's own arrays, no
  `mkl_sparse_optimize`, so in-place value updates are seen), workspace,
  stats and `<chrono>` timers. Subclasses implement `precondSolve()`, call
  `setIterativeStructure()` after analyze and `clearIterativeStructure()`
  before reallocating, and may override `beginIterations/endIterations`.
  - GMRES: right-preconditioned restarted GMRES(m), storing Z (flexible),
    true-residual stopping, x0 = 0. Fails if a restart cycle reduces the
    residual by < 10%.
  - CGS: preconditioned CGS (Templates / Sonneveld). Fails on breakdown,
    non-finite values, or residual growth > 1e8.
  - Returned iterations: GMRES = solves; CGS = solves/2.
- Two value arrays: `myVals` (factored values) and `myCurVals` (current
  values).
- `Pardiso4 : HybridSolver`: `precondSolve` = phase 33 on `myVals`, with
  `iparm[7]=0` and `iparm[3]=0` forced after the `setIParam` overrides. With
  perturbed pivots MKL still does 2 refinement steps, which can't be disabled.
  None were seen in any test or profiling run.
- `Mumps : HybridSolver`: `precondSolve` = JOB=3; `beginIterations` sets
  ICNTL(10)=ICNTL(11)=0.
- `hybridSolveJNI.h`: shared JNI glue for `doHybridSolve` (Pardiso) and
  `doIterativeSolve` (MUMPS). With critical access on, vals/b are copied into
  native buffers and x is copied out, each by a single memcpy inside a short
  `GetPrimitiveArrayCritical` region. The region is never held during the
  solve (GC-locker safe on JDK ≤ 21). Tested with `-Xcheck:jni` on JDK
  21/22.
- `libPardisoJNI` doesn't hide symbols, so it exports the `HybridSolver`
  symbols. This is harmless.

## Native libraries / platforms

- Only Linux is rebuilt: `libPardisoJNI.so.2026.1.1`, `libMumpsJNI.so.5.9.1`
  (MKL 2026.1). Windows/Mac aren't rebuilt; `NMakefile` is untested.
- Older libraries lack the hybrid entry points, which then raise
  `UnsatisfiedLinkError`. Pardiso falls back permanently to its native
  solve; MUMPS disables iterative solves. `setIParam` etc. are not guarded.
- `PardisoSolver.nativeLibrary` currently loads `PardisoJNI.2021.1.1`, with
  `2026.1.1` commented out, so Pardiso uses its native iterative solve.
- **Open:** `MurtyMechSolverTest.testBasisSolves` fails with the 2026 Pardiso
  library and passes with 2021.1.1 (phi off by ~4e-12 against a tolerance of
  2.2e-12). Either the tolerance is too tight for MKL 2026 or there's a real
  numerical difference.
- MUMPS build: `~/packages/mumps_build_all` / `mumps_clean_all`, then
  `make -f Makefile.mumps clean.mumps check` in `lib/` (see
  `lib/MUMPS_COMPILATION.md`).

## Tools and tests

- `HybridSolveTiming`: synthetic `slab`/`slabC` (constrained) at res N.
  Factors at V0, perturbs V1 = V0·(1+eps·r), and times the iterative solve
  against direct factor+solve. Always use `-symmetric` (without it, the type
  is SPD). Options include `-solvers Mumps,Pardiso`, `-mumpsMethods GMRES,CGS`,
  `-pardisoMethods CG,CGS,GMRES,HCGS` (CG/CGS = native; GMRES/HCGS = wrapper),
  `-criticalArrays/-noCriticalArrays`.
- `HybridReplayTiming`: replays solves; method NATIVE selects Pardiso's
  native solve.
- Real models: `bin/artisynth -noGui -model artisynth.demos.test.{BigSlab,
  ConstrainedTubeFem} [-small|-medium|-large] [-murtySolve] -playFor 1
  -exitOnBreak -numSolverThreads N -enableHybridSolves`. Sum the
  `factor/solve time (DIRECT|iterative[ FAILED])` lines. `FunctionTimer`
  prints values below 1 ms in µs.
- Tests: `DirectSolverTestBase.testLargeMatrix` (every method, critical
  access on and off, tolerance 1e-11), `PardisoSolverTest`, `MumpsSolverTest`,
  `KKTSolverTest`, `HybridSolvePolicyTest`,
  `MurtyMechSolverTest.testHybridSolves`.
- Benchmarking pitfalls on the Linux laptop (i7-11800H): thermal throttling
  (wait for package ≤ 60°C, `sensors`; use ABBA ordering); unset
  `OMP_NUM_THREADS`, or pass `-numSolverThreads`; run-to-run noise is about ±10
  points on Δsolve; JVM crashes leave `hs_err_pid*.log` in the repo root.

## Key results

- Preconditioner solves are 84–94% of the iterative time; matvec 3–6%,
  vector ops 2–6%, JNI < 1%. The only real levers are fewer solves and
  cheaper solves.
- Synthetic res 48, nt 8, Pardiso, GMRES vs native CG (solves, ms; direct
  factor ≈ 890 ms):

  | case | eps | native CG | GMRES |
  |---|---|---|---|
  | slabC | 1e-4 | 4, 184 | 3, 145 |
  | slabC | 1e-2 | 7, 311 | 7, 319 |
  | slab | 1e-2 | 18, 592 | 18, 622 |
  | slab | 1e-1 | 182, 5961 (misses tol) | fail at 40, 1390 |

  GMRES needs about one solve fewer than CG and always meets the true
  residual when it succeeds. CGS never beats GMRES. It pays off when it
  converges in ≲ 15 solves (nt 8) or ≲ 30–40 (nt 1).
- MUMPS: similar solve counts; its JOB=3 is faster than Pardiso phase 33
  (slabC 48 nt 8: 38.5 vs 48.6 ms), but its factorization is often slower.
- Real ArtiSynth (BigSlab/ConstrainedTubeFem, hybrid on vs off, factor/solve
  time):
  - nt 1: −26% to −47%, growing with model size (wall −5% to −36%).
  - nt 8: −18% to +7%. 8 threads speed up factorization 4–6× but the
    triangular solves only 1.5–2×, so iterative/direct ≈ 1.
- GMRES vs Pardiso native in ArtiSynth: parity at nt 1; about +2–3% at nt 8,
  from the wrapper loop's per-solve overhead.

## Open work / ideas

- Resolve the `MurtyMechSolverTest.testBasisSolves` / MKL 2026 issue, then
  pick the shipped Pardiso library.
- Rebuild the Windows/Mac Pardiso and MUMPS libraries (`hybridSolve.o` is
  already in `Makefile`, `NMakefile`, `Makefile.mumps`).
- Use separate factorizations for the velocity and position-correction solves
  (untested; less relevant now that both use the velocity matrix).
- Cheaper preconditioner: MUMPS ICNTL(47) (single-precision factors) or BLR.
- A loose time-budget abort (several × direct cost) for pathological cases.
- The wrapper's default `maxRefinementSteps` (1 for symmetric) adds 19–36% to
  every Pardiso direct solve with no residual gain in the tests. Consider
  refining only when `getNumPerturbedPivots() > 0`.
- On Windows, threaded MUMPS solves had a 15–20 ms per-call floor, so
  in-loop solves may be faster single-threaded there.
