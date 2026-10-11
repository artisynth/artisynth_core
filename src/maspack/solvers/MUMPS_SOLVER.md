# The MUMPS solver

`maspack.solvers.MumpsSolver` is a `DirectSolver` based on MUMPS 5.9.1. It
is built MPI-free but OpenMP-threaded, with the native side in
`lib/MumpsJNI.cc`, `lib/mumps.cc` and `lib/hybridSolve.cc`. It is the
alternative to Pardiso on Linux and Windows. On Arm-based Macs, which have
no Pardiso, it is the alternative to the Accelerate solver
(`ACCELERATE_SOLVER.md`). Select it with `SparseSolverId.Mumps` or
`artisynth -matrixSolver Mumps`.

Building the native library is described in:

| Platform | Document |
|---|---|
| Linux | `lib/MUMPS_COMPILATION.md` |
| Windows | `lib/MUMPS_COMPILATION_WINDOWS.md` |
| macOS (Apple Silicon) | `lib/MUMPS_COMPILATION_MACOS.md` |

This document covers how the solver behaves:

- how `MumpsSolver` and `mumps.cc` set the MUMPS controls for the systems
  ArtiSynth solves, and why;
- where this departs from the MUMPS 5.9.1 user guide;
- the adaptive strategy that settles the one control which cannot be fixed
  in advance;
- how thread counts are chosen.

The controls were worked out on 2026-09-28/29, after diagnosing MUMPS-only
failures on two matrices: the jaw-tongue correction and the incompressible
slab (see "Why ICNTL(8) cannot be fixed in advance").

## The matrix class and how it is used

Every system of interest is a symmetric indefinite *augmented* (saddle point)
system, passed as an upper triangle with SYM=2:

```
[ M   G' ]        M  symmetric, usually positive definite
[ G  -R  ]        G  constraint rows, R a (often zero) regularization diagonal
```

Two properties drive every choice below.

- **A large zero diagonal block.** R is frequently zero, so the trailing block
  contributes explicitly stored zeros on the diagonal. The zeros are stored
  because Pardiso requires a full diagonal; MUMPS does not need them. The
  constraint rows are a substantial fraction of the matrix: 23% for the
  jaw-tongue correction, 19% for the incompressible slab.
- **One analysis, many factorizations.** ArtiSynth analyzes when the structure
  changes and then re-factors with new values at every time step — 100 or more
  factorizations per analysis for a typical model. Anything MUMPS computes
  during the analysis phase from the *values* therefore ages. (Models using
  bilateral vertex contact are the opposite extreme: the constraint set changes
  constantly, so analyses are frequent — FemBowlContact does 88 in one second.)

## Default controls

`Mumps::applySettings()` applies these on every phase, since JOB=-1 resets
ICNTL and CNTL. A negative cached value means "leave MUMPS alone".

| Control | Our default | MUMPS default | Notes |
|---|---|---|---|
| ICNTL(1-4) output | silent (-1/-1/-1/0), or 6/0/6/2 with `MUMPS_JNI_VERBOSE` | verbose | keeps library chatter out of ArtiSynth's console |
| ICNTL(7) ordering | 7 (automatic) | 7 | set explicitly, same value |
| ICNTL(6) matching | left automatic (7) | 7 | guide suggests 5 for augmented systems; measured to be a no-op, see below |
| ICNTL(8) scaling | **adaptive** (see below) | 77 (automatic) | cannot be fixed in advance; the reason for the adaptive policy |
| ICNTL(12) sym. ordering | left automatic (0) | 0 | driven by ICNTL(8); see below |
| ICNTL(10) refinement | 0 | 0 | accuracy is handled by the caller; refinement would hide bad solves and costs solve time |
| ICNTL(13) root node | **1** (sequential root) | 0 (ScaLAPACK) | differs: needed for exact inertia and root null-pivot detection |
| ICNTL(14) workspace | left at default, raised on retry | 20-35% | retries are sized from INFO(2); see below |
| ICNTL(23) max memory | left alone unless set | 0 | exposed for callers with memory limits |
| ICNTL(24) null pivots | **1** (detect) | 0 | differs: gives Pardiso-like behaviour on singular matrices |
| ICNTL(48) tree parallelism | left on (1) | 1 | requires every phase to use the analysis's thread count; see "Threads" |
| CNTL(3) null threshold | 0.0 (automatic) | 0.0 | |
| CNTL(4) static pivoting | -1.0 (off) | -1.0 | perturbs pivots and loses accuracy (1e-12 residuals observed); real pivoting preferred |

### Where we follow the guide

- **Matching is left on.** The guide advises ICNTL(6)=5 for augmented systems,
  because the matching selects the 1x1 and 2x2 pivots that make symmetric
  indefinite factorization work (Duff & Pralet). We leave ICNTL(6) at MUMPS's
  automatic choice, which already enables matching for SYM=2. This was verified
  rather than assumed: across the slab, bowl and jaw-tongue matrices, under
  both scaling strategies, ICNTL(6)=5 gives bit-identical reorder method,
  ordering strategy, delayed pivots, factor nonzeros and residuals to the
  automatic choice, differing only within timing noise. Disabling matching
  (ICNTL(6)=0) is what breaks these systems: jaw-tongue then needs 9246 delayed
  pivots or fails outright. So setting 5 explicitly would add a control we do
  not need to own, and forbidding it would be a serious regression.
- **Ordering is left automatic.** ICNTL(12)=2 (compressed graph) and 3
  (constrained, AMF only) are the augmented-system strategies, and ICNTL(12)=3
  additionally forces ICNTL(6)=5 and ICNTL(8)=-2. MUMPS selects among these
  itself, and its choice is *better informed than ours*: for the jaw-tongue
  matrix it picks AMF with ICNTL(12)=3 and delays no pivots at all, while for
  the incompressible slab it picks METIS with ICNTL(12)=2 and again delays
  none. Pinning either value would lose one of those cases.
- **No static pivoting.** CNTL(4) stays off, as MUMPS has it. Static pivoting
  does rescue one of our failure cases, but only by perturbing pivots, giving
  residuals near 1e-12 instead of 1e-15, with no way for the caller to tell.

### Where we differ, and why

- **ICNTL(24)=1, null pivot detection on** (MUMPS default: off). ArtiSynth
  systems can be genuinely singular — redundant constraints, unconstrained
  rigid bodies — and Pardiso's behaviour there is to perturb the pivot and
  carry on, which callers such as `KKTSolver` and `MurtyMechSolver` are written
  around. With ICNTL(24)=0 MUMPS instead fails the factorization, which would
  make MUMPS unusable as a drop-in alternative.

  The cost of this choice is that a *false* null pivot is silently "fixed"
  instead of being pivoted around, so the solution is quietly wrong. That is
  exactly what the first failure below turned out to be, which is why the
  adaptive policy treats null pivots under analysis scaling as a signal rather
  than a result.
- **ICNTL(13)=1, sequential root node** (MUMPS default: parallel via
  ScaLAPACK). The inertia reported in INFOG(12) — used by `KKTSolver` to
  check that a KKT matrix has the expected number of negative eigenvalues — is
  only exact when the root is processed on one process, and null pivots on the
  root are otherwise not detected. Since MUMPS is built sequentially here
  (libmpiseq), nothing is lost in parallelism.
- **ICNTL(14) grown from INFO(2) on failure.** MUMPS preallocates its real
  workspace from analysis-phase estimates, and numerical pivoting can exceed
  it. The guide's remedy is to raise ICNTL(14) and factor again; it does not
  say by how much. Error -9 reports the missing entries in INFO(2) (in millions
  if negative) and INFO(20) holds the estimate, so `factorOnce()` adds the
  missing fraction plus a 20% margin, up to 20000% over 6 attempts. The
  previous scheme, doubling from 60% and abandoning above 1000%, could not
  reach the roughly 1600% the jaw-tongue matrix needs under factorization
  scaling, and turned a recoverable condition into a crash.
- **ICNTL(8) is chosen adaptively** instead of left at 77. This is the
  substantive deviation, described next.

## Why ICNTL(8) cannot be fixed in advance

ICNTL(8) selects the scaling, but through MUMPS's automatic choices it also
determines the ordering strategy, and the two available strategies are each
badly wrong for some of our matrices:

| Matrix | ICNTL(8)=77 -> analysis scaling | ICNTL(8)=8 -> factorization scaling |
|---|---|---|
| jaw-tongue correction (n=3247) | AMF, ICNTL(12)=3: **0 delayed, 149k nnz, 14 ms** | AMF, ICNTL(12)=2: 2483 delayed, 731k nnz, 100 ms |
| BigSlab incompressible (n=13860) | AMF, ICNTL(12)=3: 747 delayed, 5.37M, 234 ms | METIS, ICNTL(12)=2: **0 delayed, 3.70M, 177 ms** |
| hex3d refactor sequences (n=482) | **wrong**: null pivots from step 2, residual 5.6e2 | correct, residual 3e-15 |
| FemBowlContact (n=7500/9998) | QAMD, ICNTL(12)=1: same fill either way | same fill; about 10% slower |

Three separate conclusions follow. Analysis scaling can be far better (5x less
fill on jaw-tongue) but goes stale under repeated factorizations, and a stale
scaling plus ICNTL(24)=1 corrupts solutions. Factorization scaling never goes
stale and is far better on other matrices (30% less fill on the slab). And no
static combination of ICNTL(6), ICNTL(12) or CNTL(4) rescues the loser: with
ICNTL(8)=8, jaw-tongue stays at 2483-3348 delayed pivots across every ICNTL(6)
x ICNTL(12) combination tried.

The guide's recommended strategy for augmented systems leans on analysis
scaling (ICNTL(12)=3 forces ICNTL(8)=-2), which is sound for its assumed usage
— one analysis per factorization — and unsafe for ours.

## The adaptive strategy

Implemented in `Mumps::adaptStrategy()`, active whenever the scaling has not
been pinned by `setScaling` (`myScaling < 0`, the default). Because ICNTL(8)
affects the analysis, the two candidates can only be compared by redoing it,
which `reanalyzeAndFactor()` does; the structure and values are unchanged.

1. **One trial per matrix.** After the first factorization following an
   analysis, re-analyze with the other ICNTL(8) and factor again. Keep the
   alternative only if it reduces the number of entries in the factors
   (INFOG(29)) to 0.9 or less of the incumbent's, so ties keep the incumbent.
2. **Remember the winner.** Later analyses of the same instance reuse it, and
   the choice is redone only if the matrix size or value count changes by more
   than 25% — which is what makes this affordable for contact models that
   re-analyze on almost every step.
3. **Treat null pivots as a staleness signal.** If a factorization under
   analysis scaling reports INFOG(28) > 0, the scaling has aged: re-analyze
   with factorization scaling and keep that until the next analysis. This is
   the guard that makes ICNTL(24)=1 safe.
4. **Then factor, with the workspace retry** described above.

**Why factor nonzeros, not delayed pivots, are the metric.** Delayed pivots
were the first choice and were wrong twice over: on FemBowlContact they
exceeded the trigger threshold on 82 of 88 analyses, provoking pointless
trials, and with incompressibility they selected the strategy that was 15%
slower. Factor nonzeros track the work the factorization will actually do, and
they separate the cases that matter (149k vs 731k; 5.37M vs 3.70M) while tying
on the cases where the strategies genuinely agree (FemBowlContact).

**Cost.** A trial is two extra analyze+factor pairs, once per matrix. In a
simulation this is invisible — FemBowlContact does one trial in 88 analyses and
comes out marginally faster than the best pinned setting — but it is plain in a
single-solve benchmark: the jaw-tongue system's first factorization goes from
6.8 to 98.5 ms. Across `allruntests` the suite made 401 decisions, of which 394
kept the incumbent, so most trials are fruitless. Gating them on delayed pivots
would skip 374 of the 401 but would also lose 3 of the 7 real switches, so the
trial is left unconditional.

Diagnostics: setting `MUMPS_STRATEGY_TRACE` prints every decision with
ICNTL(8)/ICNTL(12), delayed and null pivots, factor nonzeros, and running
counts of trials, stale switches and re-analyses. These prints are temporary
and can be removed.

## Threads

### Thread counts must be consistent across a solver's phases

MUMPS 5.9 enables multithreaded tree parallelism by default (ICNTL(48)=1), and
then **requires the factorization and solve phases to run with the same number
of threads as the analysis**, failing with error -58 (INFOG(2) reporting the
analysis count) otherwise. The OpenMP thread count is a property of the
process, not of a solver, so in ArtiSynth two things routinely break that
requirement:

- creating another solver instance, whose constructor calls
  `DirectSolverBase.initThreadState()`;
- another solver's analysis being throttled: `applyThreadThrottle` runs in
  `setMatrix` before the native analysis and, per
  `MumpsSolver.maxThreadsNnz`, caps Linux matrices with nnz <= 64306 to one
  thread. The sparse position-correction matrix is below that threshold while
  the velocity matrix is not, so the correction solver's analysis dropped the
  process-wide count to 1 every step.

This produced the third failure: `artisynth.demos.opensim.Arm26FemElbow` and
`Arm26FemHumerus` died with "MUMPS error -58 (INFOG(2)=8)" on their second
velocity solve, only at 8 threads, while MurtyMechSolver was unaffected
(it uses a single solver instance, so nothing changes the count between its
phases).

`Mumps` now records the thread count in effect at analysis
(`myPhaseNumThreads`) and re-asserts it, for OpenMP and MKL, at the start of
every factorization, solve and preconditioner solve (`applyPhaseThreads()`).
Each instance's phases are therefore self-consistent no matter what else in
the process changes the count, and the per-nnz throttling still works, since
each instance keeps the count it analyzed with.

Two consequences:

- `setNumThreads()` called between an analysis and a factorization no longer
  takes effect for that factorization; it applies from the next analysis. This
  matches the documented contract, which already said the count should not be
  changed between phases.
- It also fixes accidental under-threading, which was the more expensive bug:
  the correction solver's 1-thread throttle used to leak into the velocity
  solver's factorizations. FemBowlContact per-step cost fell from 48.2 to
  42.9-43.8 ms with incompressibility, and 27.6 to 26.6 ms without (MUMPS, 8
  threads).

Decided (2026-09-29): the per-nnz throttling stays as it is, so different
solvers may use different thread counts while each solver's own phases remain
consistent. Per-phase consistency removes the correctness problem on its own,
and the throttle exists because MUMPS is slower with 8 threads on small
matrices.

### Default thread count

A new solver's nominal thread count (`DirectSolverBase.initThreadState()`)
is set as follows:

1. `DirectSolverBase.setDefaultNumThreads()`, if positive.
2. Otherwise, the OpenMP default: `OMP_NUM_THREADS` if it is set, or else
   the number of logical cores.
3. In either case, capped at `getPhysicalCoreCount()` when that is known.

**`OMP_NUM_THREADS` is set to 1 on some systems**, for binary
repeatability, and that silently makes MUMPS single-threaded. Override
it, or call `setDefaultNumThreads()`, when timing or running threaded.

For comparison across platforms, factorization of a 216k-unknown 3D
Laplacian:

| Platform | 1 thread | 8 threads |
|---|---|---|
| Linux, MKL (i7-11800H) | 7.15 s | 2.78 s |
| macOS, Accelerate (M5 Pro) | 1.70 s | 0.99 s |

### Thread throttling

`MumpsSolver.maxThreadsNnz()` limits the thread count by matrix size,
using a per-platform table of (nnz upper bound, maximum threads) entries.
Above the last entry, the solver uses its nominal thread count. The nnz
is that of the full matrix: for symmetric matrices, which are passed to
MUMPS as an upper triangle, it is doubled before the lookup.

| Platform | Single thread up to nnz | Measured on |
|---|---|---|
| Linux | 64306 | |
| Windows | 146100 | |
| macOS (Apple Silicon) | 146100 | M5 Pro, with 5 threads (2026-10-10, three runs agreed) |

Windows' per-factorization thread-team overhead is roughly an order of
magnitude higher than Linux's, so small matrices stay single-threaded up
to a larger size there. The tables are measured with
`maspack.solvers.FindSolverThreadThreshold -solver Mumps`. By default it
compares a single thread against the solver's `getPhysicalCoreCount()`
(the `-maxThreads` option overrides this).

### Apple Silicon

Two things differ on Arm-based Macs, both because of how MUMPS uses
Accelerate's BLAS and libomp there.

**OpenMP blocktime.** On macOS, MUMPS is linked against LLVM's libomp,
since with gfortran's libgomp it got *slower* with more threads. On hybrid
(P+E core) CPUs, libomp defaults to `KMP_BLOCKTIME=0`
(`openmp/runtime/src/kmp.h`), so idle threads sleep after each of MUMPS'
many short parallel regions. That caused about 500k involuntary context
switches per factorization, against 252 at one thread. `mumps.cc`
(compiled with `-DLIBOMP`) therefore calls `kmp_set_blocktime(1)` before
each MUMPS call, unless `KMP_BLOCKTIME` is set in the environment.

Factorization of a 216k-unknown 7-point Laplacian (METIS ordering), in
seconds:

| OpenMP runtime / setting | 1 thread | 4 threads | 8 threads | 15 threads |
|---|---|---|---|---|
| libgomp | 1.71 | 2.25 | 2.64 | 2.57 |
| libomp, default (`KMP_BLOCKTIME=0`) | 1.70 | 1.85 | 2.12 | 2.03 |
| **libomp, `KMP_BLOCKTIME=1`** | 1.70 | **1.04** | **0.99** | 1.03 |
| libomp, `KMP_BLOCKTIME=200` | 1.73 | 1.06 | 1.01 | 1.58 |

Longer blocktimes don't help and burn CPU. Turning off tree parallelism
(`ICNTL(48)=0`) was worse in every case.

**Performance cores only.** Accelerate's DGEMM runs on the chip's shared
AMX/SME matrix units. A single thread already gets 200-470 GFLOP/s, so
extra MUMPS threads mostly compete for those units, and scaling saturates
at about 4-8 threads. On the M5 Pro (5 performance + 10 efficiency cores),
the efficiency cores slow factorization down.

`hybridGetPhysicalCoreCount()` (hybridSolve.cc), and hence
`MumpsSolver.getPhysicalCoreCount()`, therefore returns the
performance-core count (`hw.perflevel0.physicalcpu`) when
`hw.nperflevels > 1`. Intel Macs, Linux and Windows are unchanged.

ThreadCountSweep compared this with other counts on the gridC models
(nnz 0.37M-9.3M):

- A fixed 5 threads was 4% (mean) and 8.7% (max) slower than the best
  count for each size.
- 15 threads was 28% (mean) and 74% (max) slower.
- 8 threads was best above about 1.5M nnz, by 5-10%.

The cap is tuned for MUMPS with Accelerate's BLAS, and is not true of
solvers in general. Pardiso under Rosetta is fastest with all 15 cores.
Revisit the cap for any native arm64 solver that threads well.

## Licensing

MUMPS and SCOTCH are licensed under CeCILL-C. It carries a
source-availability obligation, which static linking does not remove.
METIS is Apache-2.0. MKL (Linux and Windows) is redistributable under
Intel's license. Accelerate (macOS) is a system framework and is not
shipped.
