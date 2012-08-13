/*
 *    TweetReader.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.io.IOException;
import java.util.ArrayList;

import moa.core.InstancesHeader;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.options.FileOption;
import moa.options.StringOption;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class TweetReader extends AbstractOptionHandler implements InstanceStream {

    private static final long serialVersionUID = 1L;

    public StringOption languageFilterOption = new StringOption("languageFilter", 'l',
            "Filter by language.", "en");

    public ClassOption sketchOption = new ClassOption("sketch", 's',
            "Sketch algorithm to use.", Sketch.class, "SpaceSaving");

    public StringOption queryStringOption = new StringOption("queryString", 'q',
            "Query string to use for obtaining tweets.", "obama");

    public FileOption tweetFileOption = new FileOption("tweetFile", 'f',
            "Destination TWEET file.", null, "tweet", true);

    public FileOption inputTweetFileOption = new FileOption("inputTweetFile", 'i',
            "Input TWEET file.", null, "tweet", true);

    protected Writer writer;

    protected BufferedReader reader;

    private TwitterStreamAPIReader twitterStreamReader = new TwitterStreamAPIReader();

    protected static InstancesHeader streamHeader; //Only one for all streams

    protected static FilterTfIdf filterTfIdf; //Only one for all streams

    protected boolean isTraining = false;

    protected boolean hasMoreInstances = true;

    protected String lastTweetRead;

    protected boolean isReadingFile;

    private Tweet tweet = new Tweet();

    public TweetReader() {
    }

    public TweetReader(String query, String language, boolean isTraining) {
        this.queryStringOption.setValue(query);
        //this.inputTweetFileOption.setValue(query);
        this.languageFilterOption.setValue(language);
        this.isTraining = isTraining;
    }

    public String getPurposeString() {
        return "Generates instances based on Tweets from Twitter.";
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor arg0, ObjectRepository arg1) {
        File destFile = this.tweetFileOption.getFile();
        File inputFile = this.inputTweetFileOption.getFile();
        try {
            if (destFile != null) {
                this.writer = new BufferedWriter(new FileWriter(destFile));
            }
            if (inputFile != null) {
                this.reader = new BufferedReader(new FileReader(inputFile));
                this.lastTweetRead = "";
                this.hasMoreInstances = readNextTweetFromFile();
                this.isReadingFile = true;
            } else {
                this.isReadingFile = false;
            }
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed writing to file " + destFile, ex);
        }
        //if the language option is set, filter out the Tweets by language.
        twitterStreamReader.setLanguage(this.languageFilterOption.getValue());
        twitterStreamReader.initStream();
        String[] queryTest = {this.queryStringOption.getValue()};
        String[] queryTrain = {"=(", ":-(", ":(", ":{", ":[", "={", "=[", "=)", "=D", ":)", "=P", ":P", "=]", ";)"};
        if (this.queryStringOption.getValue() != "") {
            for (int i = 0; i < queryTrain.length; i++) {
                queryTrain[i] = this.queryStringOption.getValue() + " " + queryTrain[i];
            }
        }
        if (this.isTraining) {
            twitterStreamReader.filter(queryTrain);
            //System.out.println("Ask for "+this.queryStringOption.getValue());
        } else {
            twitterStreamReader.filter(queryTest);
        }

        ArrayList<String> classVal = new ArrayList<String>();
        classVal.add("H");
        classVal.add("S");
        Attribute classAtt = new Attribute("class", classVal);

        ArrayList<Attribute> wekaAtt = new ArrayList<Attribute>();
        wekaAtt.add(classAtt);

        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), wekaAtt, 0));
        this.streamHeader.setClassIndex(0);

        if (this.filterTfIdf == null) {
            Sketch sketch = (Sketch) getPreparedClassOption(this.sketchOption);
            this.filterTfIdf = new FilterTfIdf(sketch);
        }

    }

    @Override
    public long estimatedRemainingInstances() {

        return -1;
    }

    @Override
    public InstancesHeader getHeader() {

        return this.streamHeader;
    }

    @Override
    public boolean hasMoreInstances() {

        return this.hasMoreInstances;
    }

    @Override
    public boolean isRestartable() {

        return false;
    }

    @Override
    public Instance nextInstance() {
        Instance inst = null;
        if (this.isReadingFile) {
            inst = checkIfThereIsAnyInstance();
        } else {
            boolean isTweetReady = false;
            while (isTweetReady == false) {
                //if there is a Tweet ready,
                inst = checkIfThereIsAnyInstance();
                if (inst == null) {
                    //if there is currently no Tweet, wait before checking again.
                    try {
                        Thread.sleep(500);
                        System.out.println("waiting...");
                    } catch (InterruptedException x) {
                    }
                } else {
                    isTweetReady = true;
                }
            }
        }
        return inst;
    }
    protected int numInstances = 0;

    public Instance checkIfThereIsAnyInstance() {
        //construct the instance and remove the Tweet from the waiting list.
        Instance inst = null;
        String m="";
        numInstances++;

        if (this.isReadingFile) {
            m = this.lastTweetRead;
            this.hasMoreInstances = readNextTweetFromFile();
            if (m != null && !m.equals("")) {
                inst = this.filterTfIdf.filter(m, this.getHeader());
            }

        } //System.out.println("CHECK "+this.twitterStreamReader.size());
        else if (this.twitterStreamReader.size() > 0) {
            //	System.out.println("CHECK "+this.twitterStreamReader.size());
            m = this.twitterStreamReader.getAndRemove(0);
            inst = this.filterTfIdf.filter(m, this.getHeader());
            if (this.writer != null) {
                try {
                    writer.write(m);
                    writer.write("\n");
                } catch (Exception ex) {
                    throw new RuntimeException(
                            "Failed writing to file ", ex);
                }
            }
            //	System.out.println("CHECK "+m +" "+inst.classValue() );
            //	System.out.println("CHECK "+m + (!inst.classIsMissing() ? " TRAINING" :" TESTING"));
        }
        	if (inst!= null) System.out.println("CHECK "+m +" " );
        return inst;
    }

    @Override
    public void getDescription(StringBuilder arg0, int arg1) {
    }

    protected boolean readNextTweetFromFile() {
        try {
            if (this.lastTweetRead != null) { //end of file
                this.lastTweetRead = reader.readLine();

                if (this.lastTweetRead != null) {
                    tweet.cleanTweets(this.lastTweetRead, this.languageFilterOption.getValue());
                    String s = tweet.getMessage();
                    if (s != null && !s.equals("") && !s.equals(" ")) {
                        this.lastTweetRead = tweet.getMessage() + "," + tweet.getType();
                    }
                    return true;
                } else {
                    //this.filterTfIdf.printSpaceSaving();
                }
            }
            if (this.lastTweetRead == null) {
                this.reader.close();
                this.reader = null;
            }
            System.out.println(this.lastTweetRead != null);
            return false;
        } catch (IOException ioe) {
            throw new RuntimeException(
                    "TwitterStream failed to read instance from stream.", ioe);
        }
    }

    @Override
    public void restart() {
    }

    public void shutdown() {
        this.twitterStreamReader.shutdown();
        try {
            if (this.writer != null) {
                this.writer.close();
            }
            if (this.reader != null) {
                this.reader.close();
            }
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed writing to file ", ex);
        }
    }
}
