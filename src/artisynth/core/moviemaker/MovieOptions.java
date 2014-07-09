/**
 * Copyright (c) 2014, by the Authors: Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.moviemaker;
import java.awt.Rectangle;


/**
 Used to manage various movie rendering options. Instance is passed around to various
 entities that need this info. 
 */

public class MovieOptions
{
    public double srate = 44100; 		// sampling rate
    public boolean audioToFile = false; 	// true if currently rendering audio to file
    public boolean normalizeAudio = false;	// true if audio has to be normalized
    public boolean audioToText = false;		// true if rendering audio to text file
    public String lastSavedAudioFile = null; 	// for wav file
    public String audioFn = null; 		// for saving raw audio data
    public double speed = 1.0; 			// 2 will run 2X faster than real-time
    public boolean framesToFile = false; 	// true if currently rendering frames to file
    public String lastSavedMovieFile = null; 	// for mov file, temp stuff in /temp. 
    public MovieMaker movieMaker = null;	// default instance of movie maker
    public Rectangle rectangle = null; 		// area to be movied
}
