/*
 *    TwitterStreamAPIReader.java
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

import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

/**
 * This class sets up the connection with Twitter and creates the listener to listen for incoming Tweets.
 *
 */
public class TwitterStreamAPIReader implements TwitterStreamReader{

    protected static String language;

    private StatusListener listener;

    private Tweet tweet = new Tweet();

    private TokenTwitterStreamingAPI tokens;

    //The ArrayList to hold all the incoming Tweets.
    protected ArrayList<String> tweetList = new ArrayList<String>();

    protected int sizeTweetList = 0;

    protected twitter4j.TwitterStream twitterStream;

    /**
     * Makes the tokens, creates the listener and returns the Twitter Stream.
     * @return
     */
    @SuppressWarnings("deprecation")
    public void initStream() {

        makeListener();

        this.twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(listener);

        //set the consumer and access keys and tokens to authorise access
        //tokens = TokenTwitterStreamingAPI.getInstance();
        tokens = new TokenTwitterStreamingAPI();
        this.twitterStream.setOAuthConsumer(tokens.getMyKey(), tokens.getMyKeySecret());
        this.twitterStream.setOAuthAccessToken(tokens.getAccessToken());
        return;
    }

    public TwitterStream getStream() {
        return this.twitterStream;
    }

    /**
     * Create the FilterQuery to filter the Tweets by the trackAll list of strings.
     * @return A FilterQuery object to be used with the Twitter stream.
     */
    public FilterQuery getFilterQuery(String[] trackAll) {
        FilterQuery filterQuery = new FilterQuery();
        filterQuery.track(trackAll);
        return filterQuery;
    }

    /**
     * Creates the listener to deal with incomming Tweets.
     * Cleans the new Tweet, filters it and adds it to the list of ready Tweets.
     */
    private void makeListener() {
        listener = new StatusListener() {

            public void onStatus(Status status) {
                if (sizeTweetList < 500) {
                    
                    //synchronized(tweet) {
                    tweet.cleanTweets(status.getText(), language);
                    String s = tweet.getMessage();

                    //System.out.println("AFTER CLEANING: "+s);

                    if (s != null && !s.equals("") && !s.equals(" ")) {
                        //System.out.println("RECEIVED: "+ status.getText());
                        String m = tweet.getMessage() + "," + tweet.getType();
                        //System.out.println(sizeTweetList+" AFTER CLEANING: "+m);
                        synchronized (tweetList) {
                            tweetList.add(m);
                            sizeTweetList++;
                            //System.out.println("AFTER CLEANING: "+m);
                        }
                    }
                    //}
                } else {
                    tweetList.remove(0);
                    sizeTweetList--;
                }

            }

            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Start consuming public statuses that match one or more filter predicates.
     * At least one predicate parameter, follow, locations, or track must be specified.
     * Multiple parameters may be specified which allows most clients to use a single connection to the Streaming API. 
     * Placing long parameters in the URL may cause the request to be rejected for excessive URL length.<br>
     * 
     * The default access level allows up to 200 track keywords, 400 follow userids and 10 1-degree location boxes. 
     * Increased access levels allow 80,000 follow userids ("shadow" role), 400,000 follow userids ("birddog" role), 
     * 10,000 track keywords ("restricted track" role),  200,000 track keywords ("partner track" role), and 200 10-degree 
     * location boxes ("locRestricted" role). Increased track access levels also pass a higher proportion of statuses before
     * limiting the stream.
     *
     * @param query Filter query
     * @see twitter4j.StatusStream
     * @see <a href="http://dev.twitter.com/pages/streaming_api_methods#statuses-filter">Streaming API: Methods statuses/filter</a>
     * @since Twitter4J 2.1.2
     */
    public void filter(String[] query) {
        //try {
        this.twitterStream.filter(getFilterQuery(query));
        //} catch (TwitterException e) {
        //e.printStackTrace();
        //	}

    }

    public int size() {
        synchronized (tweetList) {
            //if (this.tweetList.size()>0) System.out.println("SIZE "+this.tweetList.size());
            //return sizeTweetList; //this.tweetList.size();
            return this.tweetList.size();
        }
    }

    public String getAndRemove(int position) {
        //synchronized(tweetList) {
        String ret = this.tweetList.get(position);
        synchronized (tweetList) {
            this.tweetList.remove(position);
            sizeTweetList--;
        }
        //System.out.println( "GET AND REMOVE "+ret);
        return ret;
        //}

    }

    public void shutdown() {
        this.twitterStream.shutdown();
    }
}
