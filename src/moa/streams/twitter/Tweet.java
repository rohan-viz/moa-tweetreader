/*
 *    Tweet.java
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

/**
 * Class used to store and preprocess a tweet
 *
 * @author Kenneth Gibson (kjjg1@waikato.ac.nz)
 *
 */
package moa.streams.twitter;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

public class Tweet {

    protected String message;			//The message of the Tweet

    protected String type = "N";		        //Happy or Sad Tweet (H or S)

    private String language = "";			//Detected language

    private Double prob;				//Probability of the correct language being detected

    private ArrayList<Language> detect;

    protected Double minimumProb = 0.95;	//The min probability required for predicted language

    private static LanguageDetect languageDetect;

    public Tweet() {
    }

    /**
     * Take out unneeded symbols (eg. $%^&*_+= etc.) and
     * detects the language if asked.
     * @param s - The information about the Tweet (message, user, timeline etc).
     * @param l - The setting to check for language, true will filter out by language.
     */
    public void cleanTweets(String m, String languageFilter) {
        m = m.replaceAll("[\\n\\r]", "");		//removes new lines.
        m = m.replaceAll("[^a-z\\sA-Z!0-9#@()\\[\\]\\{\\}:-=;]", "");
        m = m.replaceAll("\\s{2,}", "");		//removes white space of 2 or more.

        //Filters the emoticon out and classifies it
        m = getFilteredTweet(m);
        m = m.replaceAll("[^a-z\\sA-Z#@]", "");

        //take out all Twitter specific info, ie. @users and #tags
        Pattern p = Pattern.compile("\\s@.+?\\s|^@.+?\\s|\\s#.+?\\s|^#.+?\\s");

        StringBuffer sb = new StringBuffer();

        Matcher match = p.matcher(m);
        boolean result = match.find();

        while (result) {
            match.appendReplacement(sb, " ");
            result = match.find();
        }

        match.appendTail(sb);
        m = sb.toString();

        if (languageFilter.equals("")) {
            this.message = m;
        } else {
            detectLanguage(m);
            String temp = m.replaceAll("\\s+", "");	//If the message is nothing but whitespace
            this.message = "";
            if (!m.equals("") && !temp.equals("")) { //&& !type.equals("N")){
                if ((this.language.equals(languageFilter))) {// || (language.equals("es")) || (language.equals("pt")))
                    if (prob > minimumProb) {
                        this.message = m;
                    } 

                }
            }
        }
    }

    private void detectLanguage(String m) {
        //System.out.println("DETECT "+m);
        if (m.length() > 0) {
            try {
                if (languageDetect == null) {
                    languageDetect = new LanguageDetect();
                    languageDetect.init("lib/profiles");
                }

                detect = languageDetect.detectLangs(m);
                language = detect.get(0).lang;
                //System.out.println("Language "+language);
                prob = detect.get(0).prob;

            } catch (LangDetectException e) {
                //e.printStackTrace();
            }
        }
    }

    public Double getMinProb() {
        return minimumProb;
    }

    public void setMinProb(Double p) {
        this.minimumProb = p;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    /**
     * Returns the message passed to the class after it has had the emoticon removed.
     * @param s - The message to search through.
     * @return - The message with removed emoticon.
     */
    public String getFilteredTweet(String s) {
        boolean result;

        Pattern pHappy = Pattern.compile("=\\)|=D|:\\)|=P|:P|:\\]|=\\]|;\\)|:\\}|:p|=p|: P|:D|=d");
        Pattern pSad = Pattern.compile("=\\(|:-\\(|:\\(|:\\{|:\\[|=\\[|D:|=\\{");

        StringBuffer sb = new StringBuffer();

        Matcher m = pHappy.matcher(s);
        result = m.find();
        this.type = "N";

        while (result) {
            m.appendReplacement(sb, "");
            this.type = "H";
            result = m.find();
        }

        if (this.type.equals("H")) {
            m.appendTail(sb);
        } else {
            m = pSad.matcher(s);
            result = m.find();

            while (result) {
                m.appendReplacement(sb, "");
                this.type = "S";
                result = m.find();
            }
            m.appendTail(sb);
        }
        return sb.toString();
    }
}
