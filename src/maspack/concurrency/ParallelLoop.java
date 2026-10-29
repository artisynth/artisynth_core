/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.concurrency;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.ArrayList;

/**
 * Simple utility for executing index-range loops in parallel, using a shared
 * {@link ForkJoinPool}. The range {@code [0, n)} is divided into contiguous
 * chunks, each of which is passed to a {@link RangeBody}. Since each chunk is
 * executed by a single thread, bodies can allocate per-chunk scratch storage
 * without needing thread-local variables.
 */
public class ParallelLoop {

   /**
    * Body of a parallel loop, called for the index sub-range {@code [lo, hi)}.
    */
   public interface RangeBody {
      void run (int lo, int hi);
   }

   private static int myNumThreads =
      Runtime.getRuntime().availableProcessors();
   private static ForkJoinPool myPool = null;

   // number of chunks per thread, used to help balance the load
   private static int myChunksPerThread = 4;

   /**
    * Sets the number of threads used for parallel loops. A value {@code <= 1}
    * causes loops to be executed serially in the calling thread.
    *
    * @param num number of threads
    */
   public static synchronized void setNumThreads (int num) {
      if (num != myNumThreads) {
         myNumThreads = num;
         if (myPool != null) {
            myPool.shutdown();
            myPool = null;
         }
      }
   }

   /**
    * Queries the number of threads used for parallel loops.
    *
    * @return number of threads
    */
   public static int getNumThreads() {
      return myNumThreads;
   }

   private static synchronized ForkJoinPool getPool() {
      if (myPool == null) {
         myPool = new ForkJoinPool (myNumThreads);
      }
      return myPool;
   }

   private static class ChunkAction extends RecursiveAction {
      RangeBody myBody;
      int myLo;
      int myHi;

      ChunkAction (RangeBody body, int lo, int hi) {
         myBody = body;
         myLo = lo;
         myHi = hi;
      }

      protected void compute() {
         myBody.run (myLo, myHi);
      }
   }

   private static class RootAction extends RecursiveAction {
      ArrayList<ChunkAction> myChunks;

      RootAction (ArrayList<ChunkAction> chunks) {
         myChunks = chunks;
      }

      protected void compute() {
         ForkJoinTask.invokeAll (myChunks);
      }
   }

   /**
    * Returns the number of chunks that {@link #forRange} will use for a
    * given range size and minimum chunk size.
    *
    * @param n range size
    * @param minChunk minimum chunk size
    * @return number of chunks
    */
   public static int numChunks (int n, int minChunk) {
      if (myNumThreads <= 1 || n <= minChunk) {
         return 1;
      }
      int nchunks = myNumThreads*myChunksPerThread;
      minChunk = Math.max (1, minChunk);
      return Math.max (1, Math.min (nchunks, n/minChunk));
   }

   /**
    * Executes {@code body} over the range {@code [0, n)}, dividing the range
    * into contiguous chunks that are processed in parallel. If only one
    * chunk is needed, {@code body} is called directly in the calling thread.
    * The method returns after all chunks have completed. Any exception thrown
    * by a chunk is rethrown in the calling thread.
    *
    * @param n range size
    * @param minChunk minimum size of each chunk
    * @param body loop body
    */
   public static void forRange (int n, int minChunk, RangeBody body) {
      int nchunks = numChunks (n, minChunk);
      if (nchunks <= 1) {
         if (n > 0) {
            body.run (0, n);
         }
         return;
      }
      ArrayList<ChunkAction> chunks = new ArrayList<>(nchunks);
      for (int k=0; k<nchunks; k++) {
         int lo = (int)((long)n*k/nchunks);
         int hi = (int)((long)n*(k+1)/nchunks);
         chunks.add (new ChunkAction (body, lo, hi));
      }
      getPool().invoke (new RootAction (chunks));
   }
}
