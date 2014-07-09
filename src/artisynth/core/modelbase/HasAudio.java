/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * 
 * This interface provides a means for the MovieMaker to interact with the
 * current sound-producing model to be recorded. All models that produce sound
 * should implement this interface if the audio is to be automatically merged
 * with the movie. *
 * 
 * 
 * Usage:
 * 
 * In the current implementation, the audio model is expected to produce a raw
 * audio file. The file path will be saved to a MovieOptions data structure
 * stored in myMain.getMovieOptions() The raw file will be converted by the
 * MovieMakerDialog and then merged with the movie.
 * 
 * The setRenderAudioToFile is called right before movie recording starts. This
 * method should initialize the audio model to produce a raw audio file
 * according to the path set in the MovieOptions. Then, if necessary, during the
 * onStop method it should do any post processing of the newly created audio
 * file (e.g. normalization). The MovieMaker will then convert the raw file into
 * a .wav, and merge it with the movie.
 * 
 * After the movie is made, setRenderAudioToFile can be called again with
 * 'false', so that the model goes back to producing audio the normal way (i.e.
 * through the Soundcard, etc.), and no new output files are produced when the
 * model is run normally.
 * 
 * As a reference, see the implementation of HasAudio by the VTNTDemo in
 * artisynth.models.tubesounds.
 * 
 * If future models will produce Wave (or other) files directly, the
 * MovieOptions class should be expanded to allow for these, and no file
 * conversion will be necessary during the movie making process
 * 
 * Additionally, an option to write out the audio as a text file could also be
 * set by the MovieMakerDialog. This option expects the audio model to output an
 * ASCII data file of the audio samples.
 * 
 * TODO: implement adherence to specifications such as sample/bit rates, etc, if
 * necessary. It all depends on how acoustic models produce the audio output.
 * 
 * @author johnty
 * 
 */
public interface HasAudio {
   /**
    * called when switching between soundcard and file output for the model
    */
   public void setRenderAudioToFile (boolean toFile);

   /**
    * called after recording. this handles any processing of the audio file
    * before MovieMaker attempts to merge it with the video.
    */
   public void onStop();
}
