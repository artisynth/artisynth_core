# Solver thread-count analysis — how to rerun

Determines how many threads a direct sparse solver should use for a matrix of a
given size, by sweeping thread count against matrix size and fitting a
predictive rule. Written because MUMPS gets *slower* past a certain thread
count, and the count at which that happens depends on the problem size.

## Files

| file | role |
|---|---|
| `FemMatrixGenerator.java` | builds synthetic matrices with 2D/3D FE sparsity |
| `ThreadCountSweep.java` | runs the sweep, and fits/reports the rules |
| `DirectSolverTiming.java` | unrelated single-shot timing harness |

## Running it

Build first (`make` in the repo root, or in `src/maspack/solvers`). Then, from
the repo root — note Windows uses `;` as the classpath separator, not `:`:

```
java -Xmx14g -cp "classes;lib\*" maspack.solvers.ThreadCountSweep -out sweepWin.csv
```

That sweeps all four cases and prints the analysis when done. To re-analyze an
existing CSV without re-measuring (on any machine):

```
java -cp "classes;lib\*" maspack.solvers.ThreadCountSweep -analyze sweepWin.csv
```

Useful options:

- `-solver Pardiso` — MUMPS is the default. **If `libMumpsJNI` has not been
  built for Windows, use `-solver Pardiso`**, which is worth measuring in its
  own right since its threading behaviour differs from MUMPS.
- `-cases grid,sheet` — cases to run. The default is now `slab,slabC` (hex
  slab of `n x n x n/4` nodes, without and with constraints);
  `grid,gridC,sheet,sheetC` are the cases used for the results below.
- `-res 32,48` — run only these x/y resolutions (slab 32 and 48 give ~25k and
  ~85k dofs), for quick A/B tests.
- `-iparm 1=13,23=0` — Pardiso only: override `iparm[k]` (0-based C
  indexing), via `PardisoSolver.setIParam()`.
- `-symmetric` — declare every matrix symmetric indefinite (Pardiso mtype -2),
  as ArtiSynth does for FEM systems even without constraints.
- `-threads 1,2,4,6,8,12,16,24` — override the thread counts tested. The
  default is every value to 8, then even values, up to
  `Runtime.availableProcessors()` (i.e. *logical* processors).
- `-pardisoLib PardisoJNI.2021.1.1` — load a specific Pardiso native library
  instead of `PardisoSolver.nativeLibrary`'s default, to compare MKL versions
  without editing the solver.

Expected runtime: roughly 20–40 min for all four cases on 8 cores. The
constrained (`gridC`, `sheetC`) cases dominate. Peak memory is about 6 GB at
the largest constrained size, so keep `-Xmx` generous.

## Before you trust the numbers

1. **`OMP_NUM_THREADS`.** It was set to `1` on the Linux box, which silently
   forced every solver call single-threaded. The sweep overrides it with
   explicit `setNumThreads()` calls and prints a warning if it is set, but
   check that ArtiSynth proper is not being throttled by it in normal use.
2. **Physical vs logical cores.** On the Linux box (8 physical / 16 logical),
   the optimum never exceeded 8, and 16 threads cost 24–53% more than 8. The
   analysis *fits* the upper clamp rather than assuming it, so it will find the
   right ceiling on the Windows box by itself — but check that the reported
   clamp is plausible against the physical core count.
3. Close other applications; a single competing process distorts the large-size
   points badly. Watch for thermal or power throttling on a laptop — a run that
   slows monotonically over 30 min will bias the large sizes, which are
   measured last.

## What to compare against the Linux baseline

Reference machine: 11th Gen Core i7-11800H, 8 physical / 16 logical cores,
Linux, MUMPS 5.9.1, MKL 2025.3. 456 measurements = 4 cases × 9–10 sizes × 12
thread counts, cost = factor + solve, best of ≥2 reps.

**Fitted rules** (coefficients chosen to minimize measured *time*, not
thread-count error — the penalty surface is flat near the optimum, so a
least-squares fit on thread count spends accuracy where it is worth nothing):

```
T = 1 if nnz < 65536, 7 if nnz < 2097152, else 8   mean penalty 2.7%, max 36.9%
T = clamp (2.06*log2(nnz) - 31.25, 1, 8)           mean penalty 3.5%, max 29.2%
T = clamp (2.30*log2(size) - 22.00, 1, 8)          mean penalty 3.6%, max 36.9%
oracle (per-problem best)                          mean penalty 0.5%
```

**Constant-thread baselines**, for the "is a model worth it" question:

| T | mean penalty | max penalty |
|---|---|---|
| 1 | 158.2% | 364.8% |
| 4 | 47.3% | 273.6% |
| 6 | 32.1% | 279.5% |
| 8 | 32.8% | 273.3% |
| 16 | 66.3% | 303.2% |

**Key findings to check against Windows:**

- Optimal T saturated at the physical core count (8), never above. Max speedup
  was only 3.5–4.6×, not 8×.
- Raw polynomials in `size` or `nnz` are useless — 31–33% mean penalty at
  degree 1, 2 or 3, no better than a constant. The relation is a saturating
  step, which a polynomial in the raw variable cannot represent. Only log-space
  fits work.
- `nnz` and `size` are interchangeable predictors here, because `nnz/size` is
  nearly constant within a case (81 for 3D hex, ~54 for the sheet). Using both
  buys nothing.
- Leave-one-case-out cross validation gave 3.8–7.6% mean penalty, i.e. the rule
  transfers across grid/sheet and constrained/unconstrained.
- **Constant T=8 had a median penalty of only 4.5% but a mean of 32.8%.**
  Almost all the value of any model is in turning threading *off* below ~1500
  unknowns, where it costs 2–4×, not in choosing between 6 and 8 at the top
  end. On a machine with a stronger thread penalty, expect the upper clamp to
  drop and the high-T columns of the penalty surface to worsen; the low-size
  cutoff should move less.

### Baseline: measured optima

`T*` is the fastest thread count, `Tknee` the smallest within 3 percent of it,
and `speedup` is the single-threaded cost divided by the best.

```
=== measured optima (cost = factor + solve) ===
case        size        nnz         nnzL   T*  Tknee   best(ms)  speedup
grid         375      19773        33861    1      1       0.47     1.00
grid        1029      61731       150378    1      1       2.53     1.00
grid        2187     140625       524610    6      6       5.40     1.57
grid        5184     353736      1861389    5      5      14.31     2.27
grid       10125     715563      3768408    6      6      27.21     2.86
grid       20577    1497375     10516596    7      6      89.33     2.87
grid       41472    3087000     28691856    8      7     275.44     3.94
grid       73167    5527125     63459102    8      8     776.64     3.61
grid      117912    9000000    122545518    8      8    1847.96     3.81
gridC        413      20723        36768    1      1       1.48     1.00
gridC       1132      64306       200866    5      5       6.28     1.37
gridC       2406     146100       740976    8      8      12.77     2.17
gridC       5702     366686      2964679    8      7      37.98     2.94
gridC      11138     740888      4938724    8      6      49.62     3.70
gridC      22635    1548825     13675152    7      7     152.67     3.71
gridC      45619    3190675     36931514    8      8     477.48     4.14
gridC      80484    5710050     80914988    8      8    1272.61     4.65
gridC     129703    9294775    158355399    8      8    3032.82     4.24
sheet        294      12996        13053    1      1       0.39     1.00
sheet        600      28224        36840    1      1       0.83     1.00
sheet       1176      57600       100848    1      1       1.96     1.00
sheet       2400     121104       235776    7      7       3.44     1.48
sheet       4704     242064       533460    4      4       5.43     2.15
sheet       9600     501264      1302132    7      7      11.07     2.57
sheet      18816     992016      3089352    8      7      21.63     2.67
sheet      38400    2039184      7217688    8      7      43.38     3.62
sheet      75264    4016016     15796776    8      7      96.75     3.69
sheet     119286    6380676     26938305    8      8     182.86     3.46
sheetC       323      13721        17805    1      1       0.77     1.00
sheetC       660      29724        56206    1      1       1.89     1.00
sheetC      1294      60550       141086    1      1       5.07     1.00
sheetC      2640     127104       387820    5      4       9.01     1.54
sheetC      5174     253814       913428    6      5      15.31     2.01
sheetC     10560     525264      1778407    6      6      17.86     2.66
sheetC     20698    1039066      3902466    7      7      33.42     3.31
sheetC     42240    2135184      9165575   10     10      71.65     3.95
sheetC     82790    4204166     19968651    8      8     153.02     4.10
sheetC    131215    6678901     33469272    8      8     279.95     4.05
```

### Baseline: penalty surface

Each row is one problem, each column a thread count; entries are cost relative
to that problem's best. This is the table to eyeball when comparing machines.

```
=== penalty surface: cost/best at each thread count ===
case        size     1     2     3     4     5     6     7     8    10    12    14    16
grid         375  1.00  3.28  3.45  3.49  3.49  3.57  3.61  3.51  3.90  3.69  3.90  4.03
grid        1029  1.00  1.20  1.26  1.29  1.25  1.40  1.38  1.37  1.34  1.78  1.51  1.82
grid        2187  1.57  1.45  1.33  1.11  1.04  1.00  1.13  1.18  1.24  1.39  1.19  1.62
grid        5184  2.27  1.57  1.21  1.07  1.00  1.02  1.01  1.02  1.10  1.24  1.45  1.53
grid       10125  2.86  1.78  1.34  1.17  1.09  1.00  1.00  1.02  1.13  1.15  1.25  1.35
grid       20577  2.87  1.87  1.49  1.23  1.08  1.02  1.00  1.04  1.02  1.11  1.18  1.26
grid       41472  3.94  2.27  1.67  1.36  1.23  1.13  1.03  1.00  1.06  1.04  1.31  1.39
grid       73167  3.61  2.18  1.61  1.34  1.16  1.07  1.08  1.00  1.30  1.39  1.35  1.48
grid      117912  3.81  2.38  1.67  1.39  1.22  1.10  1.04  1.00  1.36  1.31  1.44  1.53
gridC        413  1.00  1.66  1.58  1.58  1.56  1.56  1.52  1.51  1.53  1.55  1.44  1.60
gridC       1132  1.37  1.29  1.31  1.29  1.00  1.06  1.10  1.09  1.11  1.13  1.31  1.34
gridC       2406  2.17  1.59  1.29  1.10  1.07  1.14  1.12  1.00  1.14  1.25  1.25  1.29
gridC       5702  2.94  2.84  2.30  1.29  1.13  1.05  1.02  1.00  1.26  1.27  1.31  1.53
gridC      11138  3.70  2.06  1.50  1.23  1.11  1.02  1.00  1.00  1.16  1.25  1.34  1.53
gridC      22635  3.71  2.14  1.68  1.30  1.12  1.08  1.00  1.04  1.22  1.21  1.25  1.34
gridC      45619  4.14  2.75  1.91  1.50  1.25  1.15  1.06  1.00  1.37  1.32  1.39  1.48
gridC      80484  4.65  2.53  1.77  1.46  1.28  1.14  1.05  1.00  1.30  1.35  1.34  1.37
gridC     129703  4.24  2.60  1.90  1.49  1.23  1.14  1.06  1.00  1.38  1.35  1.37  1.44
sheet        294  1.00  3.76  3.82  3.74  3.76  3.79  3.73  3.73  3.70  3.73  3.83  3.97
sheet        600  1.00  2.32  2.37  2.30  2.51  2.36  3.59  3.57  3.60  2.40  2.54  3.70
sheet       1176  1.00  1.50  1.46  1.33  1.40  1.27  1.95  1.53  1.52  1.73  2.40  1.74
sheet       2400  1.48  1.56  1.55  1.17  1.08  1.09  1.00  1.12  1.26  1.59  1.44  1.83
sheet       4704  2.15  1.53  1.61  1.00  1.17  1.05  1.17  1.24  1.31  1.48  1.56  1.49
sheet       9600  2.57  1.70  1.37  1.18  1.11  1.10  1.00  1.04  1.23  1.18  1.13  1.32
sheet      18816  2.67  1.82  1.43  1.24  1.12  1.09  1.00  1.00  1.12  1.18  1.23  1.25
sheet      38400  3.62  2.34  1.70  1.42  1.23  1.10  1.02  1.00  1.03  1.10  1.17  1.32
sheet      75264  3.69  2.36  1.72  1.41  1.28  1.12  1.03  1.00  1.06  1.20  1.18  1.20
sheet     119286  3.46  2.22  1.70  1.35  1.19  1.06  1.11  1.00  1.01  1.05  1.11  1.24
sheetC       323  1.00  2.37  2.41  2.34  2.33  2.35  2.32  2.30  2.53  2.37  2.66  2.39
sheetC       660  1.00  1.55  1.56  1.55  1.55  1.49  1.49  1.51  1.49  1.50  1.57  1.65
sheetC      1294  1.00  1.13  1.10  1.08  1.06  1.02  1.01  1.08  1.12  1.09  1.14  1.29
sheetC      2640  1.54  1.30  1.19  1.02  1.00  1.05  1.03  1.20  1.31  1.34  1.48  1.49
sheetC      5174  2.01  1.28  1.15  1.05  1.01  1.00  1.08  1.13  1.36  1.36  1.43  1.48
sheetC     10560  2.66  1.93  1.44  1.22  1.13  1.00  1.03  1.13  1.31  1.22  1.33  1.33
sheetC     20698  3.31  2.11  1.60  1.30  1.17  1.07  1.00  1.05  1.05  1.13  1.20  1.42
sheetC     42240  3.95  2.65  1.87  1.49  1.23  1.16  1.08  1.05  1.00  1.12  1.14  1.43
sheetC     82790  4.10  2.58  2.00  1.59  1.38  1.26  1.12  1.00  1.13  1.21  1.26  1.41
sheetC    131215  4.05  2.57  1.83  1.47  1.30  1.12  1.09  1.00  1.14  1.11  1.14  1.29
```

Values well above 1.0 on the right hand side are the thread penalty. On the
Linux box the 10/12/14/16 columns are worse than 8 for every problem large
enough to benefit from threading at all; on a machine with a stronger penalty
expect that boundary to move left.

## Windows results (MUMPS)

Same CPU as the Linux reference (11th Gen i7-11800H, 8P/16L — likely the same
box, dual-booted), MUMPS 5.9.1. `OMP_NUM_THREADS` unset. 456 measurements,
same 4 cases, default thread counts `1..8,10,12,14,16`. `grid`/`gridC` used the
script's auto-tuned rep count (2-6, targets ~400msec); `sheet`/`sheetC` were
re-run after raising that cap from 6 to 40 (still auto-tuned to ~400msec, just
no longer artificially truncated for fast problems) — see "more reps" below.

**Headline: per-factor() thread-team overhead is far larger on Windows than
Linux — roughly an order of magnitude.** Every factor() call with `nt>=2` pays
a large, variable floor here regardless of matrix size (e.g. size=375: nt=1
takes 0.7-0.8ms, nt=2..16 sit at roughly 15-20ms, sometimes higher). Linux
pays a similar-shaped but much smaller penalty at the same size (nt=8 there is
3.5x single-thread; here it's 18-39x, and varies run to run — see below). This
drags the constant-T baselines way up — Linux T=8 mean penalty was 32.8%,
Windows T=8 is **328.4%**, an order of magnitude worse — even though
large-matrix behavior (below) is comparable to Linux. Likely cause: Windows
thread-team creation/synchronization (Intel `libiomp5md.dll`) costs much more
per call than Linux's pthreads equivalent; not investigated further.

**More reps (6→40 cap) confirms the small-size floor is real overhead, not
just under-averaged jitter** — it persisted, at roughly the same order of
magnitude, across two independent higher-rep re-runs of `sheet`/`sheetC`. But
those same two re-runs **did not agree with each other on the optimal thread
count for the largest problems**: `sheet` size=119286 came back T*=16 on the
first re-run and T*=7 on the second; `sheet` size=9600 was 12/1.75x-speedup on
one run and 1/no-speedup on the other. So the earlier read of that scatter as
"thin problems may genuinely benefit from hyperthreading" doesn't hold up —
raising the rep count denoises a single run's estimate but doesn't make it
reproducible run-to-run, which points to something external and intermittent
(background load, thermal/turbo variation, scheduler placement) rather than a
stable property of the matrix. `grid`/`gridC` weren't re-run so this can't be
ruled in or out for them, but their curves already looked clean and
Linux-like on the first pass (optimal T never exceeds 10).

**Fitted rules (from the final merged data — `grid`/`gridC` from the original
run, `sheet`/`sheetC` from the second higher-rep re-run):**

```
T = 1 if nnz < 262144, 10 if nnz < 2097152, else 12  mean penalty 12.5%, max 104.1%
T = clamp (1.74*log2(nnz) - 32.00, 1, 6)             mean penalty 21.1%, max 118.4%
T = clamp (2.50*log2(size) - 29.00, 1, 7)            mean penalty 20.1%, max 128.8%
oracle (per-problem knee)                            mean penalty 0.1%
```

Worse than the Linux fit (12.5%/104.1% vs 3.5%/29.2%), and leave-one-case-out
cross validation is much worse and itself unstable across the two re-runs
(held-out `sheet` went from 37.7% mean penalty on the first re-run to **121.5%**
on the second, max 708.5%). Given the section above, treat that as further
evidence that `sheet`/`sheetC` timings are intermittently noisy on this
machine, not as a real transfer failure between cases.

**Caveat — the step rule's upper clamp (12) exceeds the physical core count
(8), and should not be trusted.** It's fit to exactly the `sheet`/`sheetC`
points shown above to be run-to-run irreproducible. The `grid`/`gridC` cases
(clean, stable, and matching Linux — optimal T never above 10) are the
trustworthy signal here; per the .md's own "before you trust the numbers"
checklist, that's also where nearly all of a rule's practical value lives
anyway (turning threading off below ~1500 unknowns).

### Windows: measured optima

```
=== measured optima (cost = factor + solve) ===
case        size        nnz         nnzL   T*  Tknee   best(ms)  speedup
grid         375      19773        33861    1      1       0.82     1.00
grid        1029      61731       150378    1      1       3.15     1.00
grid        2187     140625       524610    1      1      10.01     1.00
grid        5184     353736      1861389    7      7      23.92     1.74
grid       10125     715563      3768408    6      6      48.65     1.80
grid       20577    1497375     10516596   10     10     119.27     2.39
grid       41472    3087000     28799469    6      6     370.10     2.94
grid       73167    5527125     63414705    6      6     940.49     2.92
grid      117912    9000000    122066385    7      6    2265.35     3.02
gridC        413      20723        36768    1      1       1.72     1.00
gridC       1132      64306       200866    1      1       7.74     1.00
gridC       2406     146100       740976    8      8      14.76     2.04
gridC       5702     366686      2964679    6      6      50.82     2.18
gridC      11138     740888      5203690    7      7      75.21     2.69
gridC      22635    1548825     13919107   10     10     191.62     2.91
gridC      45619    3190675     36918158    6      6     558.85     3.40
gridC      80484    5710050     82023114    7      7    1626.04     3.38
gridC     129703    9294775    158839963    6      6    3699.94     3.34
sheet        294      12996        13053    1      1       0.37     1.00
sheet        600      28224        36840    1      1       0.84     1.00
sheet       1176      57600       100848    1      1       2.00     1.00
sheet       2400     121104       235776    1      1       4.00     1.00
sheet       4704     242064       533460    1      1       7.77     1.00
sheet       9600     501264      1302132   14     14      16.07     1.25
sheet      18816     992016      3107928    4      4      43.20     1.28
sheet      38400    2039184      7223628    4      4      80.16     1.54
sheet      75264    4016016     15789252    4      4     157.15     1.99
sheet     119286    6380676     26922285    7      7     245.86     2.17
sheetC       323      13721        17805    1      1       0.95     1.00
sheetC       660      29724        56206    1      1       2.23     1.00
sheetC      1294      60550       141086    1      1       4.60     1.00
sheetC      2640     127104       387820    5      5      10.43     1.07
sheetC      5174     253814       913428    7      4      15.57     1.53
sheetC     10560     525264      1732384   10     10      35.36     1.46
sheetC     20698    1039066      3905703   10     10      69.14     1.51
sheetC     42240    2135184      9201655   14     14     114.29     2.34
sheetC     82790    4204166     19661721   14     12     227.95     2.54
sheetC    131215    6678901     33797814   12     12     392.35     2.91
```

### Windows: penalty surface

```
=== penalty surface: cost/best at each thread count ===
case        size     1     2     3     4     5     6     7     8    10    12    14    16
grid         375  1.00 19.55 18.76 18.21 16.93 19.55 18.34 18.53 18.64 17.79 18.18 19.17
grid        1029  1.00  4.87  5.09  4.99  4.90  4.65  5.13  4.29  4.64  4.85  4.85  5.02
grid        2187  1.00  1.61  1.63  1.28  1.67  1.13  1.52  1.55  1.19  1.18  1.72  1.19
grid        5184  1.74  1.84  2.03  1.23  1.50  1.40  1.00  1.35  1.32  1.40  1.39  1.37
grid       10125  1.80  1.58  1.67  1.17  1.10  1.00  1.02  1.68  1.01  1.05  1.20  1.21
grid       20577  2.39  1.64  1.47  1.24  1.19  1.25  1.32  1.42  1.00  1.08  1.16  1.12
grid       41472  2.94  1.63  1.32  1.08  1.09  1.00  1.00  1.05  1.08  1.09  1.11  1.18
grid       73167  2.92  1.86  1.42  1.16  1.09  1.00  1.19  1.11  1.13  1.16  1.22  1.22
grid      117912  3.02  1.78  1.31  1.11  1.04  1.01  1.00  1.02  1.11  1.13  1.17  1.18
gridC        413  1.00  5.56 11.42 11.60 11.61 11.81 11.65  8.73 11.16 10.61 11.47 11.38
gridC       1132  1.00  2.53  2.57  2.04  1.94  2.44  2.35  2.52  2.05  2.38  2.54  2.43
gridC       2406  2.04  1.37  1.38  1.34  1.07  1.29  1.29  1.00  1.26  1.54  1.35  1.51
gridC       5702  2.18  2.29  1.68  1.14  1.04  1.00  1.04  1.15  1.22  1.33  1.27  1.20
gridC      11138  2.69  1.58  1.41  1.38  1.20  1.14  1.00  1.11  1.06  1.23  1.23  1.27
gridC      22635  2.91  1.81  1.44  1.36  1.10  1.11  1.13  1.10  1.00  1.08  1.23  1.30
gridC      45619  3.40  2.01  1.54  1.29  1.15  1.00  1.00  1.12  1.16  1.15  1.17  1.23
gridC      80484  3.38  1.91  1.40  1.26  1.07  1.03  1.00  1.09  1.12  1.11  1.15  1.20
gridC     129703  3.34  1.82  1.37  1.20  1.04  1.00  1.01  1.09  1.13  1.16  1.15  1.19
sheet        294  1.00 12.43 38.82 41.36 41.23 40.70 36.03 38.88 38.85 39.91 41.69 41.85
sheet        600  1.00 18.05 18.41 18.49 17.13 17.25 18.20 18.59 15.64 18.54 17.79 19.13
sheet       1176  1.00  1.56  7.26  7.63  7.52  6.34  7.11  7.18  7.73  7.99  6.96  8.47
sheet       2400  1.00  5.19  8.08  4.85  3.84  3.52  4.00  3.64  3.79  3.86  7.80  3.01
sheet       4704  1.00  5.22  7.31  4.12  2.98  1.99  1.97  1.90  3.89  3.11  4.08  2.10
sheet       9600  1.25  2.07  2.83  1.94  3.91  4.32  2.89  3.30  1.89  1.92  1.00  1.03
sheet      18816  1.28  1.43  1.41  1.00  2.12  1.26  1.71  1.57  1.09  1.08  1.04  1.24
sheet      38400  1.54  1.36  1.45  1.00  1.46  1.34  1.24  1.07  1.28  1.40  1.48  1.03
sheet      75264  1.99  1.45  1.22  1.00  1.22  1.22  1.05  1.11  1.28  1.19  1.15  1.19
sheet     119286  2.17  1.61  1.19  1.22  1.17  1.11  1.00  1.05  1.41  1.10  1.16  1.04
sheetC       323  1.00 12.71 15.47 20.54 16.34 16.21 17.52 14.86 15.92 20.52 15.88 19.59
sheetC       660  1.00  3.03  7.00  7.04  6.72  6.82  6.84  6.63  7.10  6.65  6.97  7.08
sheetC      1294  1.00  3.36  3.26  3.18  3.13  3.24  3.24  3.19  3.20  3.70  2.98  3.34
sheetC      2640  1.07  1.80  1.58  1.65  1.00  1.38  1.32  1.34  1.38  1.38  1.53  1.17
sheetC      5174  1.53  1.29  1.25  1.02  1.01  1.01  1.00  1.31  1.93  2.89  1.68  1.40
sheetC     10560  1.46  1.73  1.76  1.28  1.75  1.76  1.75  1.31  1.00  1.19  1.39  1.05
sheetC     20698  1.51  1.36  1.49  1.25  1.50  1.69  1.48  1.25  1.00  1.16  1.42  1.37
sheetC     42240  2.34  1.62  1.58  1.36  1.41  1.41  1.49  1.34  1.57  1.13  1.00  1.19
sheetC     82790  2.54  1.92  1.51  1.27  1.26  1.20  1.09  1.14  1.12  1.00  1.00  1.13
sheetC    131215  2.91  1.83  1.49  1.28  1.46  1.10  1.26  1.23  1.10  1.00  1.02  1.12
```

Raw timings: `sweepWin.csv` (repo root, untracked). The script now takes
`-reps n` to force a fixed rep count per (size,threads), and its auto-tuned
rep cap was raised from 6 to 40 — use it to re-run `sheet`/`sheetC` again if
you want a third data point on the run-to-run instability noted above.

## Pardiso results (Windows)

Same run as above (`-solver Pardiso`, all 4 cases, default thread counts,
auto-tuned reps). Full data: `sweepWinPardiso.csv`.

**Pardiso is far better behaved than MUMPS on this machine — close to (or
better than) the Linux MUMPS baseline.** Fitted step rule: mean penalty 4.4%,
max 44.1% (vs Windows MUMPS's 12.5%/104.1%, vs Linux MUMPS's 3.5%/29.2%).
Cross-validation is good and stable: 2.1-18.3% mean penalty, no case anywhere
near MUMPS's 121.5%. The fitted upper clamp lands on **8**, matching the
physical core count exactly — no implausible-clamp caveat needed here, unlike
either MUMPS run:

```
T = 1 if nnz < 32768, 6 if nnz < 131072, else 8   mean penalty 4.5%, max 44.1%
T = clamp (1.22*log2(nnz) - 13.25, 1, 8)          mean penalty 4.4%, max 44.1%
T = clamp (1.18*log2(size) - 6.25, 1, 8)          mean penalty 4.6%, max 44.1%
oracle (per-problem knee)                         mean penalty 0.7%
```

**On the "no degradation for T≤8" hypothesis: mostly true, with two real
exceptions, both visible in the T=1..8 columns of the penalty surface below.**

1. **Small matrices genuinely do get slower as threads increase, even ≤8** —
   just much less dramatically than MUMPS's floor. `sheet` size=294 rises
   monotonically 1.00→1.01→1.03→1.07→1.07→1.10→1.13→1.13 from T=1 to T=8 (13%
   worse at 8 threads than 1); `gridC` size=413 similarly creeps 1.00→1.09
   from T=2 to T=8. Same mechanism as MUMPS (thread start/sync cost exceeds
   the work available), just roughly two orders of magnitude smaller here.
2. **One large spike, confirmed reproducible**: `sheetC` size=2640 is smooth
   and improving through T=7 (1.00, the best) then jumps to **2.13x at T=8** —
   a genuine 113% regression from one more thread, specifically within the
   "should be safe" ≤8 range. Re-run independently (`sheetC` case only,
   `-reps 30` fixed instead of the auto-tuned budget): it reproduced almost
   exactly — T=7 best (1.92ms), T=8 jumps to 2.18x, staying elevated through
   T=16 (2.16-2.56x) — and no other `sheetC` size showed anything like it in
   either run. So this is a real, repeatable Pardiso/thread-count interaction
   at this specific problem size, not a one-off system hiccup, though *why*
   remains unexplained (a pivoting or scheduling decision that changes
   discretely at 8 threads for this particular sparsity pattern would be a
   reasonable guess, not confirmed). It's a concrete counter-example to "T≤8
   is always safe" — worth keeping in mind if a real model's matrix happens to
   land near this size/sparsity with Pardiso.

Aside from those, T=1→8 is close to monotonically non-increasing everywhere,
including the constrained (`gridC`/`sheetC`, indefinite KKT) cases — unlike
MUMPS, Pardiso doesn't show a distinct worse-behaved pattern for those.

**Solver choice depends on problem type, not just speed in general.** Comparing
best achieved times (MUMPS from the merged sweep above vs Pardiso, matched by
size):

| case | size | MUMPS best (ms) | Pardiso best (ms) | Pardiso vs MUMPS |
|---|---|---|---|---|
| grid | 117912 | 2265.4 | 1399.4 | 1.6x faster |
| grid | 73167 | 940.5 | 521.3 | 1.8x faster |
| grid | 41472 | 370.1 | 176.0 | 2.1x faster |
| sheet | 119286 | 245.9 | 113.3 | 2.2x faster |
| gridC | 129703 | 3699.9 | 5922.1 | 1.6x **slower** |
| gridC | 80484 | 1626.0 | 2124.2 | 1.3x **slower** |
| gridC | 45619 | 558.9 | 722.0 | 1.3x **slower** |
| sheetC | 131215 | 392.4 | 349.0 | 1.1x faster |
| sheetC | 82790 | 228.0 | 202.0 | 1.1x faster |

Pardiso is clearly faster (1.6-2.2x) on the unconstrained SPD cases (`grid`,
`sheet`). On the constrained indefinite KKT cases it's a mixed bag: `sheetC`
mildly favors Pardiso, but `gridC` — the *thickest*, most FE-realistic
constrained case — consistently favors MUMPS by 1.3-1.6x at the larger sizes.
Since ArtiSynth's real workload is contact-heavy (constrained/indefinite),
this cuts against just switching to Pardiso for the win seen on `grid`/`sheet`
alone; `gridC`'s behavior is the more representative one to weight.

### Pardiso: measured optima

```
=== measured optima (cost = factor + solve) ===
case        size        nnz         nnzL   T*  Tknee   best(ms)  speedup
grid         375      19773        34495    1      1       0.23     1.00
grid        1029      61731       131297   10      8       0.44     2.37
grid        2187     140625       407120   14     12       1.76     2.07
grid        5184     353736      1474451   10      8       5.62     3.14
grid       10125     715563      3711123    8      8      12.15     4.64
grid       20577    1497375     10250325   16     12      43.83     5.15
grid       41472    3087000     27554611   10      8     176.03     4.46
grid       73167    5527125     60006176   12      8     521.33     4.70
grid      117912    9000000    117138681   10     10    1399.38     4.25
gridC        413      20723        34387    2      2       0.54     1.07
gridC       1132      64306       156597   14      8       1.11     2.84
gridC       2406     146100       515764    6      6       3.92     3.00
gridC       5702     366686      1781175    8      8      15.90     3.85
gridC      11138     740888      4766586    8      8      43.35     4.45
gridC      22635    1548825     13161674   10     10     180.17     3.99
gridC      45619    3190675     36537993    8      8     722.03     4.12
gridC      80484    5710050     78912647   14      8    2124.16     4.02
gridC     129703    9294775    155821230    8      8    5922.05     3.69
sheet        294      12996        14095    1      1       0.12     1.00
sheet        600      28224        39781    4      4       0.28     1.18
sheet       1176      57600        91313   16      8       0.35     1.93
sheet       2400     121104       238001    6      6       0.67     2.55
sheet       4704     242064       574205   14     14       1.90     2.62
sheet       9600     501264      1383893    8      8       5.13     2.67
sheet      18816     992016      3168665   10      8      10.73     4.06
sheet      38400    2039184      7188405   16     12      26.44     3.48
sheet      75264    4016016     16125945   14      8      61.27     3.84
sheet     119286    6380676     26616213   14     14     113.29     3.52
sheetC       323      13721        19047    7      6       0.29     1.09
sheetC       660      29724        56997    4      4       0.89     1.08
sheetC      1294      60550       136403   12      8       0.94     2.67
sheetC      2640     127104       479454    7      7       2.04     2.81
sheetC      5174     253814       778818    8      8       4.29     3.59
sheetC     10560     525264      1870353   16      8      11.39     3.75
sheetC     20698    1039066      4234291   14      8      26.69     4.05
sheetC     42240    2135184     10067200   14      8      71.24     4.39
sheetC     82790    4204166     22509421    8      8     201.97     4.04
sheetC    131215    6678901     36633320   12      8     348.98     3.97
```

### Pardiso: penalty surface

```
=== penalty surface: cost/best at each thread count ===
case        size     1     2     3     4     5     6     7     8    10    12    14    16
grid         375  1.00  1.02  1.25  1.25  1.27  1.29  1.25  1.02  1.00  1.26  1.00  1.01
grid        1029  2.37  1.41  1.42  1.37  1.09  1.05  1.07  1.03  1.00  1.03  1.07  1.07
grid        2187  2.07  1.59  1.62  1.15  1.21  1.20  1.20  1.07  1.04  1.03  1.00  1.03
grid        5184  3.14  2.18  2.16  1.37  1.38  1.12  1.15  1.02  1.00  1.01  1.06  1.01
grid       10125  4.64  2.53  2.48  1.39  1.42  1.49  1.53  1.00  1.01  1.01  1.00  1.01
grid       20577  5.15  2.74  2.76  1.51  1.53  1.16  1.21  1.07  1.04  1.01  1.17  1.00
grid       41472  4.46  2.43  2.48  1.45  1.44  1.15  1.10  1.02  1.00  1.00  1.06  1.02
grid       73167  4.70  2.53  2.42  1.39  1.44  1.10  1.12  1.01  1.03  1.00  1.08  1.03
grid      117912  4.25  2.37  2.33  1.37  1.35  1.09  1.13  1.03  1.00  1.06  1.04  1.07
gridC        413  1.07  1.00  1.04  1.01  1.06  1.08  1.08  1.09  1.09  1.09  1.09  1.09
gridC       1132  2.84  1.73  1.69  1.32  1.23  1.03  1.06  1.01  1.01  1.01  1.00  1.22
gridC       2406  3.00  1.75  1.71  1.19  1.16  1.00  1.03  1.09  1.15  1.23  1.20  1.17
gridC       5702  3.85  1.93  1.96  1.25  1.25  1.03  1.04  1.00  1.04  1.01  1.05  1.06
gridC      11138  4.45  2.27  2.65  1.54  1.46  1.14  1.19  1.00  1.06  1.05  1.19  1.13
gridC      22635  3.99  2.31  2.30  1.42  1.50  1.10  1.10  1.03  1.00  1.03  1.02  1.01
gridC      45619  4.12  2.17  2.26  1.40  1.38  1.09  1.13  1.00  1.03  1.05  1.08  1.02
gridC      80484  4.02  2.07  2.11  1.31  1.40  1.07  1.07  1.01  1.03  1.06  1.00  1.02
gridC     129703  3.69  2.01  2.02  1.30  1.34  1.06  1.08  1.00  1.02  1.02  1.01  1.01
sheet        294  1.00  1.01  1.03  1.07  1.07  1.10  1.13  1.13  1.14  1.14  1.14  1.14
sheet        600  1.18  1.52  1.06  1.00  1.00  1.01  1.02  1.05  1.04  1.05  1.34  1.08
sheet       1176  1.93  1.52  1.49  1.10  1.11  1.18  1.08  1.01  1.04  1.02  1.02  1.00
sheet       2400  2.55  1.73  1.74  1.14  1.06  1.00  1.10  1.03  1.07  1.00  1.04  1.06
sheet       4704  2.62  1.97  1.95  1.58  1.60  1.23  1.22  1.04  1.04  1.08  1.00  1.12
sheet       9600  2.67  1.70  1.70  1.22  1.15  1.05  1.07  1.00  1.03  1.01  1.01  1.04
sheet      18816  4.06  2.39  2.40  1.36  1.31  1.20  1.19  1.01  1.00  1.03  1.07  1.02
sheet      38400  3.48  2.20  2.12  1.40  1.39  1.08  1.16  1.05  1.03  1.03  1.06  1.00
sheet      75264  3.84  2.53  2.12  1.34  1.46  1.20  1.31  1.01  1.19  1.07  1.00  1.17
sheet     119286  3.52  2.26  2.07  1.42  1.40  1.07  1.13  1.03  1.04  1.04  1.00  1.01
sheetC       323  1.09  1.15  1.13  1.05  1.05  1.01  1.00  1.02  1.09  1.15  1.20  1.11
sheetC       660  1.08  1.13  1.13  1.00  1.01  1.00  1.16  1.19  1.21  1.05  1.17  1.15
sheetC      1294  2.67  1.73  1.69  1.57  1.65  1.44  1.53  1.03  1.09  1.00  1.12  1.04
sheetC      2640  2.81  1.59  1.58  1.30  1.27  1.04  1.00  2.13  2.16  2.56  2.24  2.27
sheetC      5174  3.59  2.05  1.88  1.53  1.30  1.28  1.27  1.00  1.08  1.06  1.05  1.07
sheetC     10560  3.75  2.13  2.12  1.37  1.33  1.08  1.10  1.01  1.03  1.00  1.05  1.00
sheetC     20698  4.05  2.13  2.21  1.51  1.48  1.12  1.13  1.02  1.02  1.08  1.00  1.01
sheetC     42240  4.39  2.42  2.41  1.45  1.57  1.20  1.19  1.01  1.02  1.13  1.00  1.02
sheetC     82790  4.04  2.26  2.37  1.26  1.46  1.10  1.08  1.00  1.05  1.01  1.03  1.07
sheetC    131215  3.97  2.22  2.42  1.35  1.30  1.10  1.12  1.01  1.05  1.00  1.02  1.12
```

Raw timings: `sweepWinPardiso.csv` (repo root, untracked).

## MKL 2026.1 vs 2021.1.1 (Pardiso)

Re-ran the identical sweep after updating to MKL 2026.1
(`PardisoJNI.2026.1.dll`, confirmed loaded via ArtiSynth's own
"Loading native library" printout — `PardisoSolver.nativeLibrary` was updated
accordingly). Same machine, same matrices — `nnzL` is identical between the
two runs at every size, confirming the fill-reducing ordering didn't change,
so any timing difference is the library's compute, not a different ordering
choice. Full data: `sweepWinPardiso2026.csv`.

**Result: 2026.1 is not faster — it's consistently *slower*, by roughly
10-45%, worse at larger sizes.** Comparing best-achieved time at matched
sizes:

| case | size | 2021.1.1 best (ms) | 2026.1 best (ms) | change |
|---|---|---|---|---|
| grid | 117912 | 1399.4 | 1850.4 | 32% slower |
| grid | 73167 | 521.3 | 754.3 | 45% slower |
| grid | 41472 | 176.0 | 243.9 | 39% slower |
| gridC | 129703 | 5922.1 | 7001.9 | 18% slower |
| gridC | 80484 | 2124.2 | 2563.2 | 21% slower |
| gridC | 45619 | 722.0 | 885.1 | 23% slower |
| sheet | 119286 | 113.3 | 132.6 | 17% slower |
| sheet | 75264 | 61.3 | 73.6 | 20% slower |
| sheetC | 131215 | 349.0 | 408.8 | 17% slower |
| sheetC | 82790 | 202.0 | 243.0 | 20% slower |

Every case, every large size, in the same direction. Peak speedup from
threading also dropped a bit (e.g. `grid` size=41472: 4.46x → 3.77x), meaning
the regression isn't just an across-the-board constant — the newer library
also gets relatively less benefit from more threads on this problem set. The
fitted rule is correspondingly noisier too (mean/max penalty 8.7%/64.0% vs
2021.1.1's 4.4%/44.1%), though still clearly better-behaved than MUMPS on
this machine.

**The `sheetC` size=2640, T=7→8 spike reproduces under 2026.1 too** — 1.00 at
T=7, jumping to 2.26x at T=8 and staying elevated (2.13-2.37x) through T=16,
essentially the same shape as under 2021.1.1. Since the same `nnzL` rules out
a reordering difference, and the effect survives an MKL major-version change,
this looks like something structural to Pardiso's handling of this specific
sparsity pattern/size rather than a bug particular to either MKL build.

**Caveat:** this is one run per version, not independently repeated the way
`sheetC` was above — if this matters for a real decision (e.g. whether to
ship the MKL update), re-run at least one of the two versions again before
treating the exact percentages as more than "clearly slower, same shape."

## Testing iparm[23] as the cause

`pardisoMkl.cc` never explicitly set Pardiso's two-level parallel
factorization switch — it was left at whatever `pardisoinit()` defaults to
for the matrix type, and Intel's docs describe that default as version- and
structure-dependent. Hypothesis: MKL changed that default between 2021.1.1
and 2026.1, and that alone explains the regression.

**Correction: the first attempt at this test set the wrong array index.**
Intel's PARDISO docs number parameters 1-based (`iparm(1)`...`iparm(64)`);
the two-level switch is documented as `iparm(24)`, which is `myIParams[23]`
in the 0-based C array — confirmed by cross-checking the existing code's own
`myIParams[26] = getMatrixChecking()` against the doc's `iparm(27)` "matrix
checker" (consistent −1 shift both times). The first pass instead set
`myIParams[24]` (`iparm(25)`, a different, undocumented index) — a real
indexing bug, not a units/convention quibble. That run's numbers weren't
wrong measurements, just mislabeled as testing something they didn't; its
CSV (`sweepWinPardiso2026Iparm24.csv`) is kept for the record, but the earlier
"recovers 71-80% for grid/gridC" reading doesn't survive re-measurement at
the correct index, so the table below replaces it rather than sitting
alongside it.

Re-ran with `myIParams[23] = 1;` added correctly, rebuilding
`PardisoJNI.2026.1.dll` the same way. Data:
`sweepWinPardiso2026Iparm23fixed.csv`.

**Verdict: a real, substantial, and — this time — sensible effect: it tracks
matrix definiteness, not matrix thickness.**

| case | size | 2021.1.1 | 2026.1 default | 2026.1, iparm[23]=1 (correct) |
|---|---|---|---|---|
| gridC | 129703 | 5922.1 | 7001.9 | 6155.5 (78% of regression recovered) |
| gridC | 80484 | 2124.2 | 2563.2 | 2177.9 (88% recovered) |
| gridC | 22635 | 180.2 | 218.9 | 186.8 (83% recovered) |
| sheetC | 131215 | 349.0 | 408.8 | 347.1 (**faster than 2021.1.1**) |
| sheetC | 42240 | 71.2 | 83.3 | 66.7 (**faster than 2021.1.1**) |
| sheetC | 5174 | 4.29 | 4.80 | 4.04 (**faster than 2021.1.1**) |
| grid | 117912 | 1399.4 | 1850.4 | 1715.4 (30% recovered) |
| grid | 73167 | 521.3 | 754.3 | 696.3 (25% recovered) |
| grid | 20577 | 43.8 | 60.9 | 74.5 (**worse than default**) |
| sheet | 119286 | 113.3 | 132.6 | 141.1 (**worse than default**) |
| sheet | 75264 | 61.3 | 73.6 | 88.7 (**worse than default**) |
| sheet | every size tested | — | — | **worse than default at all 10 sizes** |

**The indefinite/KKT cases (`gridC`, `sheetC`) — the ones structurally closest
to ArtiSynth's real contact problems — improve strongly and consistently**,
several sizes even beating 2021.1.1 outright. **The unconstrained SPD cases
split**: `grid` gets a real but partial and less consistent recovery (one
size, 20577, came out worse); `sheet` gets uniformly worse at every single
size, cleanly and monotonically. So the earlier "thick 3D helps, thin sheet
hurts" read was accidentally in the right direction for `sheetC` vs `sheet`,
but for the wrong reason (thickness) — the real split is indefinite-vs-SPD,
and `grid` (thick, SPD) doesn't fit the old story at all.

Aggregate effect is a genuine net improvement over 2026.1's own default:
fitted rule mean/max penalty 6.3%/59.1%, vs default's 8.7%/64.0% and
2021.1.1's 4.4%/44.1% — closes roughly half the aggregate gap to 2021.1.1
while making `sheet` specifically worse.

**Given the mixed result, `myIParams[23]=1` was reverted again** (same reason
as before: not an unconditional win) — but this time the mechanism was clean
enough (indefinite-vs-SPD, not a per-size fit artifact) to implement properly
instead of just flagging it: `pardisoMkl.cc`'s `setMatrix()` now sets
`myIParams[23] = (myMatrixType == REAL_SYMMETRIC_INDEF) ? 1 : 0` — explicit
per-matrix-type, rather than leaving it at the library's own (apparently
version-dependent) default. This *is* a real production behavior change with
sign-off, unlike the two earlier reverts.

**Verification re-run of the gated build**, full sweep, `sweepWinPardiso2026Gated.csv`:

- `nnzL` confirms the gate engages correctly in both directions: `grid`/
  `sheet` (SPD) reproduce the 2026.1-default fill exactly; `gridC`/`sheetC`
  (indefinite) reproduce the forced-`iparm[23]=1` run's fill exactly.
- `grid`/`sheet` timings land within normal run-to-run noise of the 2026.1
  default (as expected — the gate leaves them untouched).
- **`gridC`/`sheetC` timings did *not* reproduce anywhere near the earlier
  70-100%-of-regression-recovered figures** — mostly small wins over default
  (a few percent), a couple of small losses (`gridC` 11138 5.5% worse,
  `sheetC` 2640 — the known-erratic spike case — 11% worse), nothing close to
  the single-run numbers quoted above. Net effect is still an improvement
  overall (fitted rule mean/max penalty 6.8%/59.8%, essentially matching the
  forced-run's 6.3%/59.1%, both clearly better than default's 8.7%/64.0%),
  but the size of the win for any *individual* indefinite problem is much
  noisier and less reliable than that first test suggested — consistent with
  the run-to-run instability this machine has shown throughout this
  investigation (see the MUMPS `sheet`/`sheetC` section above). The direction
  (indefinite benefits, SPD doesn't) held up; the magnitude didn't.
- Kept in the codebase regardless: it never showed a *consistent* downside
  across a full sweep the way the unconditional version did for `sheet`, and
  the aggregate fit quality held. But if this matters for a real decision,
  it's worth averaging a few more full-sweep runs before trusting a specific
  percentage-improvement claim for any one problem size.

**The `sheetC` size=2640 T≤8 spike is unrelated to this setting either way**
— it persists at essentially the same magnitude (1.00 best, jumping to
~2x at T=8) whether `iparm[23]` is forced, gated, or left at default, on both
MKL versions and at both the correct and incorrect index. Whatever causes it,
it isn't the two-level algorithm choice.

## MKL 2026.1 vs 2021.1.1 (Pardiso, Linux)

Same Linux box as the MUMPS baseline (i7-11800H, 8P/16L), `-solver Pardiso
-threads 1,8`, all 4 cases, auto-tuned reps, `OMP_NUM_THREADS` unset. 2021 via
`-pardisoLib PardisoJNI.2021.1.1`, 2026 via the default
(`libPardisoJNI.so.2026.1.1`); the loaded library was confirmed from the
"Loading native library" printout. **Each version was run twice** (a/b), to
measure the run-to-run noise. Each run takes ~2.5 min. Data:
`sweepLinPardiso{2021,2026}{,b}.csv`, under
`~/projects/artisynth/claude/pardisoTuning/threadSweep2021vs2026`.

**Result: the opposite of Windows. At T=8, 2026.1 is faster than 2021.1.1**,
modestly but reproducibly. Geomean time ratio 2026/2021 at T=8, sizes ≥ 10k:

| case | run a | run b | same-version noise (b/a, 2021 / 2026) |
|---|---|---|---|
| grid | 0.94 | 0.89 | 1.05 / 0.99 |
| gridC | 0.89 | 0.86 | 1.03 / 1.00 |
| sheet | 1.00 | 0.98 | 1.00 / 0.97 |
| sheetC | 0.95 | 0.92 | 1.05 / 1.02 |
| all | 0.94 | 0.91 | 1.03 / 1.00 |

The biggest wins are at the largest thick sizes, T=8 (ms, runs a/b):

| case | size | 2021.1.1 | 2026.1 | change |
|---|---|---|---|---|
| grid | 117912 | 1526 / 1643 | 1298 / 1303 | 15-21% faster |
| grid | 73167 | 557 / 597 | 512 / 510 | 8-15% faster |
| gridC | 129703 | 6881 / 6864 | 5552 / 5504 | 19-20% faster |
| gridC | 80484 | 2257 / 2353 | 1899 / 1904 | 16-19% faster |
| sheet | 119286 | 110 / 110 | 100 / 100 | 9% faster |
| sheetC | 131215 | 348 / 345 | 310 / 325 | 6-11% faster |
| sheetC | 82790 | 172 / 170 | 179 / 177 | 4% slower |

In both runs, 2026 was slower at T=8 for: `grid` 5184 (+16%), `sheet` 9600
(+18-23%), `sheet` 18816 (+15%), `sheetC` 2640 (+23%) and `sheetC` 82790
(+4%). For `sheetC` 2640 and 82790, 2026 also produces more fill at T=8
(`nnzL` +27% and +5%).

**T=1 is inconclusive.** The 2026/2021 geomean at ≥ 10k was 1.08 in run a and
0.96 in run b, and 2026's own T=1 run-to-run noise was 7-12%. T=1 is not the
production setting anyway.

**Cross-platform:** Linux 2021 at T=8 is roughly on par with Windows 2021
(e.g. `grid` 41472: 181/192 ms vs 180 ms on Windows). Linux 2026 is well
ahead of Windows 2026: `grid` 117912 takes 1298 ms at T=8 on Linux, vs a
Windows best over *all* T of 1850 ms. So the Windows 2026 regression looks
Windows-specific (build, `libiomp5md.dll`, or OS threading), not a change in
MKL's Pardiso itself.

**Unlike Windows, `nnzL` is not identical** between versions (usually within
~1.5%). It also varies with thread count within a single version: T=8 fill is
1-6% lower than T=1 for most problems, which suggests the ordering depends on
thread count. The exception is `sheetC` 2640, where T=8 fill is 1.14x T=1
under 2021 and 1.44x under 2026. That likely explains the known T=7→8 spike
at that size (see the Pardiso section above).

**Confounds, not isolated:**

- The 2021 `.so` predates the `iparm[23]` gating. It uses the library default,
  whereas the 2026 build gates it on (indefinite) or off (SPD). The SPD cases
  `grid`/`sheet` also improve, so gating is not the whole story. But it may
  contribute to the `gridC`/`sheetC` gains.
- Both libraries load `lib/Linux64/libiomp5.so`, which is the 2021-era OMP
  runtime (5.0.20201007). The matching 2026 runtime is at
  `/opt/intel/oneapi2026/compiler/2026.1/lib/libiomp5.so`, but was not used.
  2026 might do better still with its own runtime; untested.

## Pardiso 2026 tuning (Linux)

Motivated by Intel's claim that Pardiso 2026 can be much faster, together with
the parameter reference in `oneMKLManual2026.pdf` (pp. 1928-1965). Setup:
- **Cases:** `slab` (SPD), `slab -symmetric` (the same matrix as mtype -2) and
  `slabC` (with constraints). Constraint rows each couple 4 neighbouring
  nodes, so G consists of 1x3 blocks, like FE contact. Rigid bodies (6x6
  blocks) were left out, since ArtiSynth models rarely have more than about a
  dozen.
- **Sizes:** resolutions 32 and 48 (~25k and ~85k dofs), 3 reps, 8 threads
  unless noted.
- **Parameters:** set through the new `PardisoSolver.setIParam()`.
- **Code:** config runner and comparison scripts were in the session
  scratchpad.
- **Data:** `pardisoTuning/thermal` and `pardisoTuning/thermalU`, under
  `~/projects/artisynth/claude`.

**Thermal throttling invalidates naive A/B runs on this laptop.** Under
sustained load the package reaches 85-95°C and throttles. In one batch, the
same baseline config got 1.2-1.9x slower over 7 minutes. So:
- Every run was made to wait until the package had cooled to ≤60°C.
- Each config is compared against the mean of the baseline runs immediately
  before and after it.
- With these controls, repeated baselines agree to within 1-2% for factor
  time and about 5% for analyze.

Results from the earlier, uncontrolled batches (`pardisoTuning/`,
`pardisoTuning/quick`) are only trusted where the effect is large.

### Findings

Ratios vs adjacent baselines (analyze / factor / solve / analyze+factor+solve):

| setting | `slab` SPD | `slab` mtype -2 | `slabC` |
|---|---|---|---|
| `iparm[1]=13` (L=1), `iparm[23]=0` | 1.0/1.0/1.0/1.0 | 1.0/1.0/1.0/1.0 | 1.0/**0.55-0.63**/1.0/**0.67-0.86** |
| `iparm[1]=13`, `iparm[23]=1` (keep two-level) | — | — | 1.1/**0.59-0.64**/1.0/0.71-0.85 |
| `iparm[1]=103` (L=10) | same as L=1 | same as L=1 | same as L=1 |
| `iparm[23]=0` alone (classic, L=0) | — | 1.0/1.0/1.0/1.0 | 0.95/1.0/1.05/1.0 |
| `iparm[36]=-80` (VBSR) | 0.5/1.2-1.4/1.1/0.97 | 0.3/1.2-1.4/1.1/0.76 | 0.26/0.64-0.81/**2.0-2.2**/0.58 |
| VBSR + L=1 | 0.5/1.2-1.4/1.0/0.95 | 0.24/1.1-1.3/1.1/0.72 | 0.25/0.57-0.79/**1.9-2.3**/0.53-0.61 |
| `iparm[36]=-95` | ≈ -80 | — | ≈ -80 |
| `iparm[36]=-50` | factor 4-6x | — | factor 2-4x |
| `iparm[1]=2` (METIS vs parallel ND) | analyze 1.4-1.6x | — | analyze 1.5x |
| `iparm[24]=2`, 2026 libiomp5, `OMP_PROC_BIND=close` | ±3% | — | ±3% |

Other findings:
- **L at 1 thread:** L=1 also gives `slabC` factor about 0.74x. METIS
  vs parallel nested dissection makes no difference at 1 thread; an apparent
  7-9% gain in earlier runs was throttling noise.
- **L memory cost:** factor-phase memory is +4-6% with `iparm[23]=0`, and
  +1% with `iparm[23]=1`. L=10 costs no more than L=1 at these sizes.
- **`iparm[23]=10` is broken for symmetric indefinite matrices.** Residuals
  are about 5e3; the manual documents it for nonsymmetric matrices only.
- **`iparm[36]=3` (fixed 3x3 BSR) crashes the JVM inside MKL.** It needs
  BSR3-format input arrays, not CSR, and ArtiSynth's mixed 3x3/6x6/1xn
  blocks couldn't use it anyway.
- **`iparm[1]=0` (minimum degree)** has 1.5-2.3x the fill and factors 2.4-4x
  slower.
- **mtype -2 costs relative to SPD:** the same unconstrained matrix analyzes
  about 2x slower and factors about 25% slower when declared symmetric
  indefinite instead of SPD.

### Interpretation

- **Optimization level L.** The 2026 gain is real but only applies to
  constrained (KKT) systems. There, `iparm[1] = 10 + method` cuts factor time
  by 35-45% for a few percent more memory, and it has no measurable effect on
  unconstrained systems. Contrary to the manual ("applicable only when
  iparm[23] is 0 or 10"), it also works with two-level factorization, so the
  minimal change is to add L=1 and leave the `iparm[23]` gating alone. Higher
  L bought nothing here. The manual says it helps "large matrices"; the
  largest `slabC` in the full-size run (145k dofs, 150M nnzL) showed the same
  ~0.7x as L=1-6.
- **VBSR** makes analyze 2.5-4x faster, but factor gets slower for
  unconstrained systems and solve gets 2x slower with constraints. It only
  wins if the matrix is re-analyzed at nearly every factorization and each
  factorization is followed by few solves. Contact can cause re-analysis every
  step, but LCP solves also mean many solves per factor. Not recommended as a
  default; at most an opt-in setting.
- **Nothing else tested helps:** ordering choice, solve parallelism,
  OpenMP runtime or thread pinning.

### L=1 made the default

`pardisoMkl.cc` `setMatrix()` now sets `iparm[1] = 10 + reorderMethod`.
- **Version guard:** `#if defined(INTEL_MKL_VERSION) && INTEL_MKL_VERSION >=
  20260000`, so builds against older MKL are unchanged. The format of
  `INTEL_MKL_VERSION` changed between releases: 2021.1 is `202101`, 2026.1
  is `20260100`.
- **Pitfall:** on Linux `pardisoMkl.h` does not include `mkl.h`, so the macro
  was undefined and the first guard silently compiled to the `#else` branch.
  `pardisoMkl.cc` now includes `mkl_version.h` directly.
- **Verification:** `ThreadCountSweep` now prints the `iparm[1]` and
  `iparm[23]` actually used (via `getIParam()`; -1 for libraries without
  it).
- **Status:** only the Linux 2026 library has been rebuilt so far.

## MKL 2026.1 (L=1) vs 2021.1.1 — final comparison (Linux)

Setup:
- **Libraries:** the shipped `libPardisoJNI.so.2021.1.1` against
  `libPardisoJNI.so.2026.1.1` with the L=1 default and the per-type
  `iparm[23]` gating.
- **Cases:** `slab` (SPD), `slab -symmetric` (mtype -2, how ArtiSynth passes
  FEM systems) and `slabC`.
- **Sizes and threads:** resolutions 16, 32 and 48 (~3k, 25k and 85k dofs),
  1 and 8 threads, 3 reps.
- **Controls:** two rounds in ABBA order, cooldown to ≤60°C before every run.
- **Data:** `pardisoTuning/final2021vs2026` (under `~/projects/artisynth/claude`).
- **Checks:** every 2026 run reported `iparm[1]=13`; no failures, and no
  residual above 1e-6.

The table gives 2026/2021 time ratios (geometric mean over the two rounds;
< 1 means 2026 is faster). Round-to-round spread of factor+solve was 0-10% at
8 threads and up to 14% at 1 thread; the 1-thread runs throttle more.

| variant | nt | size | analyze | factor | solve | factor+solve | total | memory |
|---|---|---|---|---|---|---|---|---|
| slab SPD | 1 | 3072 | 0.97 | 0.97 | 0.67 | 0.93 | 0.96 | 1.04 |
| slab SPD | 1 | 24576 | 1.01 | 0.91 | 0.56 | 0.87 | 0.92 | 1.00 |
| slab SPD | 1 | 82944 | 0.99 | 0.92 | 0.54 | 0.90 | 0.91 | 1.00 |
| slab SPD | 8 | 3072 | 1.07 | 1.07 | 1.23 | 1.09 | 1.07 | 1.02 |
| slab SPD | 8 | 24576 | 0.63 | 0.88 | 0.64 | 0.83 | 0.71 | 0.99 |
| slab SPD | 8 | 82944 | 0.63 | 0.88 | 0.63 | 0.85 | 0.77 | 0.99 |
| slab mtype -2 | 1 | 3072 | 0.95 | 1.09 | 0.65 | 1.04 | 0.98 | 1.05 |
| slab mtype -2 | 1 | 24576 | 0.99 | 0.97 | 0.58 | 0.94 | 0.95 | 1.00 |
| slab mtype -2 | 1 | 82944 | 0.97 | 0.97 | 0.57 | 0.95 | 0.96 | 1.00 |
| slab mtype -2 | 8 | 3072 | 1.06 | 1.01 | 0.89 | 0.99 | 1.04 | 0.98 |
| slab mtype -2 | 8 | 24576 | 0.79 | 0.96 | 0.64 | 0.89 | 0.83 | 0.95 |
| slab mtype -2 | 8 | 82944 | 0.82 | 0.98 | 0.62 | 0.94 | 0.88 | 0.97 |
| slabC | 1 | 3379 | 0.96 | 0.94 | 0.58 | 0.90 | 0.93 | 1.02 |
| slabC | 1 | 27034 | 0.96 | 0.66 | 0.53 | 0.65 | 0.73 | 0.99 |
| slabC | 1 | 91238 | 1.01 | 0.66 | 0.51 | 0.66 | 0.70 | 1.02 |
| slabC | 8 | 3379 | 1.10 | 0.97 | 0.93 | 0.96 | 1.07 | 0.92 |
| slabC | 8 | 27034 | 0.80 | 0.52 | 0.61 | 0.53 | 0.66 | 0.92 |
| slabC | 8 | 91238 | 0.78 | 0.62 | 0.60 | 0.62 | 0.67 | 1.00 |

Summary:
- **Constrained (contact) systems, ≥25k dofs:** 2026 is about **1.5x
  faster** for analyze+factor+solve at both 1 and 8 threads (total 0.66-0.73).
  Factor+solve is 0.53-0.66.
- **Unconstrained FEM as ArtiSynth declares it (mtype -2):**
  - 8 threads: 12-17% faster overall, mainly from faster analyze (0.8) and
    solve (0.6); factor is about the same.
  - 1 thread: 4-5% faster.
- **SPD:** 23-29% faster at 8 threads, 8-9% at 1 thread.
- **Solve:** 2026 is consistently 35-50% faster, apart from the smallest
  problems at 8 threads.
- **Small problems (~3k dofs):** no meaningful difference (0.93-1.09, within
  noise).
- **Memory:** within ±8%.
- **Earlier Windows run:** this is a much better outcome than the Windows
  result above, where 2026 was 10-45% slower. That run predates both the
  `iparm[23]` gating and L=1, and the machine showed a lot of noise.
  Re-measuring on Windows with a rebuilt DLL is the obvious next step.

## Caveats on the synthetic matrices

`FemMatrixGenerator` reproduces FE *sparsity* (hex 27-node stencil, tet 15,
quad 9, tri 7) and assembles SPD values from element matrices
`k(I - 1/m 11^T) ⊗ A`, plus a diagonal mass term. Constraints are contiguous
runs of nodes, which are spatially local in the grid ordering — this
approximates contact. It is not a real ArtiSynth stiffness matrix: values are
synthetic, so numeric pivoting behaviour may differ from a real model, though
fill and therefore factor cost are governed by the pattern, which is exact.
