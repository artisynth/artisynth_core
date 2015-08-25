/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Code extracted from org.apache.commons.io.FileUtils
 */

package maspack.fileutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class SafeFileUtils {

   public static final long ONE_KB = 1024;
   public static final long ONE_MB = ONE_KB * ONE_KB;
   private static final long FILE_COPY_BUFFER_SIZE = ONE_MB * 30;

   private static final String LOCK_EXTENSION = ".lock";

   public static final int LOCK_FILE = 0x0001; // use file locks
   public static final int CLEAN_LOCK = 0x0002; // if lock exists, clean-up and
                                                // continue (otherwise, throw
                                                // error)
   public static final int PRESERVE_DATE = 0x0010; // preserve modified date on
                                                   // file

   public static int DEFAULT_OPTIONS = PRESERVE_DATE | LOCK_FILE;
   private static int defaultOptions = DEFAULT_OPTIONS;

   /**
    * Moves a file.
    * <p>
    * When the destination file is on another file system, do a
    * "copy and delete".
    * 
    * @param srcFile
    * the file to be moved
    * @param destFile
    * the destination file
    * @throws NullPointerException
    * if source or destination is {@code null}
    * @throws IOException
    * if source or destination is invalid
    * @throws IOException
    * if an IO error occurs moving the file
    * @since 1.4
    */
   public static void moveFile(File srcFile, File destFile, int options)
      throws IOException {

      if (srcFile == null) {
         throw new NullPointerException("Source must not be null");
      }
      if (destFile == null) {
         throw new NullPointerException("Destination must not be null");
      }
      if (!srcFile.exists()) {
         throw new FileNotFoundException("Source '" + srcFile
            + "' does not exist");
      }
      if (srcFile.isDirectory()) {
         throw new IOException("Source '" + srcFile + "' is a directory");
      }
      if (destFile.isDirectory()) {
         throw new IOException("Destination '" + destFile + "' is a directory");
      }

      if (destFile.exists()) {
         destFile.delete(); // try to delete destination before move
      }

      boolean rename = srcFile.renameTo(destFile);
      if (!rename) {
         copyFile(srcFile, destFile, options);
         if (!srcFile.delete()) {
            deleteQuietly(destFile);
            throw new IOException("Failed to delete original file '" + srcFile
               + "' after copy to '" + destFile + "'");
         }
      }
   }

   public static void moveFile(File srcFile, File destFile) throws IOException {
      moveFile(srcFile, destFile, defaultOptions);
   }

   /**
    * Moves a file.
    * <p>
    * When the destination file is on another file system, do a
    * "copy and delete".
    * 
    * @param srcDir
    * the file to be moved
    * @param destDir
    * the destination file
    * @throws NullPointerException
    * if source or destination is {@code null}
    * @throws IOException
    * if source or destination is invalid
    * @throws IOException
    * if an IO error occurs moving the file
    * @since 1.4
    */
   public static void moveDirectory(File srcDir, File destDir, int options)
      throws IOException {

      if (srcDir == null) {
         throw new NullPointerException("Source must not be null");
      }
      if (destDir == null) {
         throw new NullPointerException("Destination must not be null");
      }
      if (!srcDir.exists()) {
         throw new FileNotFoundException("Source '" + srcDir
            + "' does not exist");
      }

      boolean rename = srcDir.renameTo(destDir);
      if (!rename) {
         copyDirectory(srcDir, destDir, options);
         if (!srcDir.delete()) {
            throw new IOException("Failed to delete original file '" + srcDir
               + "' after copy to '" + destDir + "'");
         }
      }
   }

   public static void copyDirectory(File srcDir, File destDir, int options)
      throws IOException {
      Files.walkFileTree(
         srcDir.toPath(), new CopyFileVisitor(destDir.toPath()));
   }

   public static void moveDirectory(File srcDir, File destDir)
      throws IOException {
      moveDirectory(srcDir, destDir, defaultOptions);
   }

   /**
    * Deletes a file, never throwing an exception. If file is a directory,
    * delete it and all sub-directories.
    * <p>
    * The difference between File.delete() and this method are:
    * <ul>
    * <li>A directory to be deleted does not have to be empty.</li>
    * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
    * </ul>
    *
    * @param file
    * file or directory to delete, can be {@code null}
    * @return {@code true} if the file or directory was deleted, otherwise
    * {@code false}
    *
    * @since 1.4
    */
   public static boolean deleteQuietly(File file) {
      if (file == null) {
         return false;
      }
      try {
         if (file.isDirectory()) {
            return false;
         }
      } catch (Exception ignored) {}

      try {
         return file.delete();
      } catch (Exception ignored) {
         return false;
      }
   }

   /**
    * Copies a file to a new location preserving the file date.
    * <p>
    * This method copies the contents of the specified source file to the
    * specified destination file. The directory holding the destination file is
    * created if it does not exist. If the destination file exists, then this
    * method will overwrite it.
    * <p>
    * <strong>Note:</strong> This method tries to preserve the file's last
    * modified date/times using {@link File#setLastModified(long)}, however it
    * is not guaranteed that the operation will succeed. If the modification
    * operation fails, no indication is provided.
    * 
    * @param srcFile
    * an existing file to copy, must not be {@code null}
    * @param destFile
    * the new file, must not be {@code null}
    * 
    * @throws NullPointerException
    * if source or destination is {@code null}
    * @throws IOException
    * if source or destination is invalid
    * @throws IOException
    * if an IO error occurs during copying
    */
   public static void copyFile(File srcFile, File destFile) throws IOException {
      copyFile(srcFile, destFile, defaultOptions);
   }

   /**
    * Copies a file to a new location.
    * <p>
    * This method copies the contents of the specified source file to the
    * specified destination file. The directory holding the destination file is
    * created if it does not exist. If the destination file exists, then this
    * method will overwrite it.
    * <p>
    * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
    * {@code true} tries to preserve the file's last modified date/times using
    * {@link File#setLastModified(long)}, however it is not guaranteed that the
    * operation will succeed. If the modification operation fails, no indication
    * is provided.
    *
    * @param srcFile
    * an existing file to copy, must not be {@code null}
    * @param destFile
    * the new file, must not be {@code null}
    * @param options
    * determine whether to use file locks and preserve date information
    *
    * @throws NullPointerException
    * if source or destination is {@code null}
    * @throws IOException
    * if source or destination is invalid
    * @throws IOException
    * if an IO error occurs during copying
    */
   public static void copyFile(File srcFile, File destFile, int options)
      throws IOException {
      if (srcFile == null) {
         throw new NullPointerException("Source must not be null");
      }
      if (destFile == null) {
         throw new NullPointerException("Destination must not be null");
      }
      if (srcFile.exists() == false) {
         throw new FileNotFoundException("Source '" + srcFile
            + "' does not exist");
      }
      if (srcFile.isDirectory()) {
         throw new IOException("Source '" + srcFile
            + "' exists but is a directory");
      }
      if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
         throw new IOException("Source '" + srcFile + "' and destination '"
            + destFile + "' are the same");
      }
      File parentFile = destFile.getParentFile();
      if (parentFile != null) {
         if (!parentFile.mkdirs() && !parentFile.isDirectory()) {
            throw new IOException("Destination '" + parentFile
               + "' directory cannot be created");
         }
      }
      if (destFile.exists() && destFile.canWrite() == false) {
         throw new IOException("Destination '" + destFile
            + "' exists but is read-only");
      }
      doCopyFile(srcFile, destFile, options);
   }

   /**
    * Internal copy file method.
    * 
    * @param srcFile
    * the validated source file, must not be {@code null}
    * @param destFile
    * the validated destination file, must not be {@code null}
    * @param options
    * determine whether to use a file lock, and preserve date information
    * @throws IOException
    * if an error occurs
    */
   private static void doCopyFile(File srcFile, File destFile, int options)
      throws IOException {
      if (destFile.exists() && destFile.isDirectory()) {
         throw new IOException("Destination '" + destFile
            + "' exists but is a directory");
      }

      File lockFile = new File(destFile.getAbsolutePath() + LOCK_EXTENSION);

      FileInputStream fis = null;
      FileOutputStream fos = null;
      FileChannel input = null;
      FileChannel output = null;
      try {
         fis = new FileInputStream(srcFile);
         fos = new FileOutputStream(destFile);
         input = fis.getChannel();
         output = fos.getChannel();
         long size = input.size();
         long pos = 0;
         long count = 0;

         // Create lock before starting transfer
         // NOTE: we are purposely not using the Java NIO FileLock, because that
         // is automatically removed when the JVM exits. We want this file to
         // persist to inform the system the transfer was never completed.
         if ((options & LOCK_FILE) != 0) {
            if (lockFile.exists()) {

               // if we are not cleaning old locks, throw error
               if ((options & CLEAN_LOCK) == 0) {
                  throw new IOException(
                     "Lock file exists, preventing a write to "
                        + destFile.getAbsolutePath() + ".  Delete "
                        + lockFile.getName()
                        + " or set the CLEAN_LOCK option flag");
               }
            } else {
               lockFile.createNewFile(); // will always return true or throw
                                         // error
            }

         }

         while (pos < size) {
            count =
               size - pos > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE
                  : size - pos;
            pos += output.transferFrom(input, pos, count);
         }
      } finally {
         closeQuietly(output);
         closeQuietly(fos);
         closeQuietly(input);
         closeQuietly(fis);
      }

      if (srcFile.length() != destFile.length()) {
         throw new IOException("Failed to copy full contents from '" + srcFile
            + "' to '" + destFile + "'");
      }

      if ((options & PRESERVE_DATE) != 0) {
         destFile.setLastModified(srcFile.lastModified());
      }

      // successful copy, delete lock file
      deleteQuietly(lockFile);

   }

   // added to close a file quietly
   public static boolean closeQuietly(InputStream stream) {
      try {
         stream.close();
      } catch (Exception e) {
         return false;
      }
      return true;
   }

   // added to close a file quietly
   public static boolean closeQuietly(OutputStream stream) {
      try {
         stream.close();
      } catch (Exception e) {
         return false;
      }
      return true;
   }

   // added to close a file quietly
   public static boolean closeQuietly(FileChannel stream) {
      try {
         stream.close();
      } catch (Exception e) {
         return false;
      }
      return true;
   }

   public static void setDefaultOptions(int options) {
      defaultOptions = options;
   }

   private static class CopyFileVisitor extends SimpleFileVisitor<Path> {
      private final Path targetPath;
      private Path sourcePath = null;

      public CopyFileVisitor(Path targetPath) {
         this.targetPath = targetPath;
      }

      @Override
      public FileVisitResult preVisitDirectory(
         final Path dir, final BasicFileAttributes attrs) throws IOException {
         if (sourcePath == null) {
            sourcePath = dir;
         } else {
            Files.createDirectories(targetPath.resolve(sourcePath
               .relativize(dir)));
         }
         return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(
         final Path file, final BasicFileAttributes attrs) throws IOException {
         Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
         return FileVisitResult.CONTINUE;
      }
   }

}