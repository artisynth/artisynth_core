# MUMPS controls for ArtiSynth saddle point systems

How `MumpsSolver`/`mumps.cc` set the MUMPS controls for the systems ArtiSynth
solves, why, where this departs from the MUMPS 5.9.1 user guide, and the
adaptive strategy that settles the one control which cannot be fixed in
advance. Written after diagnosing two MUMPS-only failures (see "The two
failures" below), 2026-09-28/29.

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
| ICNTL(48) tree parallelism | left on (1) | 1 | requires every phase to use the analysis's thread count; see "Thread counts" |
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

## Thread counts must be consistent across a solver's phases

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
