package moa.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import moa.core.ObjectRepository;
import moa.options.FileOption;
import moa.options.IntOption;
import moa.options.StringOption;
import moa.streams.twitter.TokenTwitterStreamingAPI;

import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.StatusAdapter;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterException;
import twitter4j.TwitterStreamFactory;

public class PrintTwitterStream extends MainTask {

    public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
            "Maximum number of instances to print   (-1 = no limit).",
            10, -1, Integer.MAX_VALUE);

   // public IntOption timeLimitOption = new IntOption("timeLimit", 't',
   //         "Maximum number of seconds to print for (-1 = no limit).", -1,
   //         -1, Integer.MAX_VALUE);

    public StringOption queryOption = new StringOption("query", 'q',
            "Query string to use for obtaining tweets",
            "=( :-( :( :{ :[ ={ =[ =) =D :) =P :P =] ;)");

    public FileOption tweetFileOption = new FileOption("tweetFile", 'f',
            "Destination  file.", null, "tweet", true);

    public class PrintStream extends StatusAdapter {

        protected int firstTrainingInstances = 0;

        protected boolean finished = false;

        protected Writer w = null;

        public void start(File destFile) throws TwitterException, IOException {


            if (destFile != null) {
                w = new BufferedWriter(new FileWriter(destFile));
            }
            StatusListener listener = new StatusListener() {

                public void onStatus(Status status) {
                    String str = "@" + status.getUser().getScreenName() + " - " + status.getText();
                    System.out.println(str);
                    firstTrainingInstances++;
                    if (firstTrainingInstances == instanceLimitOption.getValue()) {
                        finished = true;
                        System.out.println("Finished!");
                        //w.close();
                    }
                    if (w != null) {
                        try {
                            w.write(str);
                            if (finished == true) {
                                w.close();
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(
                                    "Failed writing to file ", ex);
                        }
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

            twitter4j.TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
            twitterStream.addListener(listener);
            TokenTwitterStreamingAPI tokens = new TokenTwitterStreamingAPI();
            twitterStream.setOAuthConsumer(tokens.getMyKey(), tokens.getMyKeySecret());
            twitterStream.setOAuthAccessToken(tokens.getAccessToken());
            FilterQuery filterQuery = new FilterQuery();
            String[] queryTrain = queryOption.getValue().split(" ");
            // {"=(", ":-(", ":(", ":{", ":[", "={", "=[", "=)", "=D", ":)", "=P", ":P", "=]", ";)"};
            //String[] queryTrain = { ":(", ":)", };
            //for ( int i=0;i<queryTrain.length;i++){
            //	queryTrain[i] = "apple "+ queryTrain[i];
            //}
            finished = false;
            filterQuery.track(queryTrain);
            twitterStream.filter(filterQuery);
            /* FilterQuery filterQuery2 = new FilterQuery();
            String[] queryTest2= {"apple"};
            filterQuery2.track( queryTest2);
            twitterStream.filter(filterQuery2); */
            while (!finished) {
            }
            twitterStream.cleanUp();
            twitterStream.shutdown();
        }
    }

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        PrintStream ps = new PrintStream();

        File destFile = this.tweetFileOption.getFile();
        try {

            ps.start(destFile);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed writing to file " + destFile, ex);
        }
        return null;
    }
}
