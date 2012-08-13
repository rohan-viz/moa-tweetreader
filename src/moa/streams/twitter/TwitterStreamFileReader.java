/*
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

/*
 * SetUpStream.java
 * Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */

package moa.streams.twitter;

import java.util.ArrayList;
import java.util.Random;


/**
 * This class sets up the connection with Twitter and creates the listener to listen for incoming Tweets.
 * @author KenAdmin
 *
 */
public class TwitterStreamFileReader implements TwitterStreamReader{

	protected static String language;

	private Tweet tweet = new Tweet();
	//The ArrayList to hold all the incoming Tweets.
	protected ArrayList<String> tweetList = new ArrayList<String>();
	
	protected int[]    wordTwitterGenerator;
	protected double[] freqTwitterGenerator;
	protected double[] sumFreqTwitterGenerator;
	protected int[] classTwitterGenerator;
	
	protected int sizeTable = 10000;
	protected double probPositive = 0.05;
	protected double probNegative = 0.05;
	protected double zipfExponent = 1.5;
	protected double lengthTweet = 15;
	
	protected int changeTime = 50000;
	
	protected int countTweets = 0;
	
	protected Random random;

	public int getSizeTable() {
		return sizeTable;
	}

	public double getWord(int i) {
		return wordTwitterGenerator[i];
	}

	public double getFreqWord(int i) {
		return freqTwitterGenerator[i];
	}

	/**
	 * Makes the tokens, creates the listener and returns the Twitter Stream.
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public void initStream() {

		//Prepare table of words to generate tweets
		this.wordTwitterGenerator = new int[sizeTable];
		this.freqTwitterGenerator = new double[sizeTable];
		this.sumFreqTwitterGenerator = new double[sizeTable];
		this.classTwitterGenerator = new int[sizeTable];
		
		this.countTweets = 0;
		
		double sum = 0;
		this.random = new Random();
		for (int i=0; i<this.sizeTable ; i++) {
			this.wordTwitterGenerator[i] = i+1;
			this.freqTwitterGenerator[i] = 1.0/Math.pow(i+1, zipfExponent);
			sum += this.freqTwitterGenerator[i];
			this.sumFreqTwitterGenerator[i] = sum;
			double rand = random.nextDouble();
			this.classTwitterGenerator[i] = ( rand < probPositive ? 1 : (rand < probNegative+probPositive ? 2 : 0) );
		}
		for (int i=0; i<this.sizeTable ; i++) {
			this.freqTwitterGenerator[i] /= sum;
			this.sumFreqTwitterGenerator[i] /= sum;	
		}

	}
	
	public void changePolarity(int numberWords) {
		for (int i=0; i<numberWords ; ) {
			int randWord = random.nextInt(this.sizeTable);
			int polarity = this.classTwitterGenerator[randWord];
			/*while (polarity == this.classTwitterGenerator[randWord]){
				double rand = random.nextDouble();
				this.classTwitterGenerator[i] = ( rand < probPositive ? (rand < probNegative+probPositive ? 2 : 0) : 1);
			}*/
			if (polarity == 1) {
				this.classTwitterGenerator[i] = 2;
				i++;
			}
			if (polarity == 2) {
				this.classTwitterGenerator[i] = 1;
				i++;
			}
		}
	}
	
	public void changeFreqWords(int numberWords) {
		for (int i=0; i<numberWords ; i++) {
			int randWordTo = random.nextInt(this.sizeTable);
			int randWordFrom = random.nextInt(this.sizeTable);
			this.wordTwitterGenerator[randWordTo] = randWordFrom;
			this.wordTwitterGenerator[randWordFrom] = randWordTo;
		}
	}
	
	private void generateTweets(int numberTweets) {
		
		for (int tweetCount = 0; tweetCount < numberTweets;tweetCount++){
			int[] votes;
			String tweet;
			do {
				int length = (int) (lengthTweet * (1.0+ this.random.nextGaussian()));
				if (length<1) length =1;
				votes = new int[3];
				tweet ="";
				for(int j=0; j<length; j++) {
					double rand = random.nextDouble();
					//binary search
					int i = 0;
					int min = 0;
					int max = sizeTable-1 ; 
					int mid;
					do {
						mid = (min+max) / 2;
						if (rand > this.sumFreqTwitterGenerator[mid]) {
							min = mid + 1;
						} else {
							max = mid - 1;
						}
					} while ((this.sumFreqTwitterGenerator[mid] != rand) && (min <= max));
					if (tweet.length() < 131) {
						tweet += " word"+this.wordTwitterGenerator[mid];
						votes[this.classTwitterGenerator[mid]]++;
					} else break;
				}
			} while (votes[1] == votes[2]);
			String type = (votes[1]> votes[2]) ? "H" : "S";
			String m = tweet + "," + type;
			//System.out.println(votes[1]+" "+votes[2]+" "+m);
			tweetList.add(m);
		}
	}
	

	
	private void generateTweets0() {
	
		String[] tweets = {"This is a tweet test :)", "This is another test:(", "another test :)", "quite interesting is this @fv #td :)"};	
		
		for (String tw:tweets) {
			tweet.cleanTweets(tw, language);
				String s = tweet.getMessage();

					if(s != null && !s.equals("") && !s.equals(" ") && tweet.getType() != "N")
					{
						String m = tweet.getMessage() + "," + tweet.getType();
						tweetList.add(m);
						
					}
			}
	}

	/**
	 * Creates the listener to deal with incomming Tweets.
	 * Cleans the new Tweet, filters it and adds it to the list of ready Tweets.
	 */
	private static void makeListener()
	{
	
	}

	public void setLanguage(String language)
	{
		this.language = language;
	}

	public void filter(String[] query) {
		
	}
	
	public int size() {
		return 1; //this.tweetList.size();
	}
	public String getAndRemove(int position) {
		if (this.countTweets == this.changeTime) {
			this.countTweets = 0;
			//changePolarity(10);
			changeFreqWords(2000);
		}
		generateTweets(1);
		countTweets++;
		String ret = this.tweetList.get(position);
		this.tweetList.remove(position);
		return ret;
	}
	public void shutdown(){}
}
