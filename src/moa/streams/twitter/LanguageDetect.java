/*
 *    LanguageDetect.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Kenneth Gibson (kjjg1@waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package moa.streams.twitter;

import java.util.ArrayList;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

public class LanguageDetect {

    /**
     * Initialise the files used to predict the language.
     * @param profileDirectory - Where the profiles are stored.
     * @throws LangDetectException
     */
    public void init(String profileDirectory) throws LangDetectException {
        DetectorFactory.loadProfile(profileDirectory);
    }

    /**
     * Detect the language of the text input.
     * @param text
     * @return	- the detected language.
     * @throws LangDetectException
     */
    public String detect(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.detect();
    }

    /**
     * Detect the language of the text input, along with the probability of the prediction.
     * @param text
     * @return	- the detected language along with its probability.
     * @throws LangDetectException
     */
    public ArrayList<Language> detectLangs(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.getProbabilities();
    }
}
