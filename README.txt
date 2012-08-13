README
=======================

MOA-TweetReader is a extension to MOA that allows to read instances from the Twitter Stream API directly.

INSTALL
=========
To install MOA-TweetReader, you will need the following libraries in a lib directory in your java classpath:
lib/jsonic-1.1.3.jar
lib/langdetect.jar:moa.jar
lib/weka.jar
lib/twitter4j-core-2.2.5-SNAPSHOT.jar
lib/twitter4j-stream-2.2.5-SNAPSHOT.jar

You can download them from:

http://code.google.com/p/language-detection/
http://sourceforge.jp/projects/jsonic/devel/
http://twitter4j.org/

The jsonic library is needed for the langdetect library.

Also you need to have the following jar files:

TweetReader.jar
moa.jar
sizeofag.jar

The first time you run MOA-TweetReader you will be asked for a PIN to get an authorization access token. You will need to open an URL with your account and get a PIN Number.  

>>Open the following URL and grant access to your account:
>>Enter the PIN(if available) or just hit enter.[PIN]:

USAGE
==========

You may use the following parameters with TweetReader

-l : Filter by language. Default:en

-s : Sketch algorithm to use. Default:SpaceSaving

-q : Query string to use for obtaining tweets. Default:obama

-f : Destination TWEET file.

-i : Input TWEET file.


EXAMPLES
===========
1/ Write a file of tweets that contain "iPhone" into an arff file called "test.arff":

java -cp TweetReader.jar:lib/jsonic-1.1.3.jar:lib/json-lib-2.3-jdk15.jar:lib/langdetect.jar:moa.jar:lib/weka.jar:lib/twitter4j-core-2.2.5-SNAPSHOT.jar:lib/twitter4j-stream-2.2.5-SNAPSHOT.jar -javaagent:sizeofag.jar moa.DoTask "WriteStreamToARFFFile -s (twitter.TweetReader -q iPhone -f test.tweet) -f test.arff -m 20"

2/ EvaluateSentimentOnTwitter is a simple code example that you can modify to create your sentiment analysis task.

Example: Obtains percentage of positive tweets in the Twitter stream that contains word "iphone". The learner is trained with tweets with emoticons.  At the beginning the classifier is trained with 1000 instances (you can change this number of instances with the parameter -b). After that training and testing is done at the same time.

java -cp TweetReader.jar:lib/jsonic-1.1.3.jar:lib/json-lib-2.3-jdk15.jar:lib/langdetect.jar:moa.jar:lib/weka.jar:lib/twitter4j-core-2.2.5-SNAPSHOT.jar:lib/twitter4j-stream-2.2.5-SNAPSHOT.jar -javaagent:sizeofag.jar moa.DoTask "EvaluateSentimentOnTwitter -w iPhone -i 100 -f 10"

Note that the number of tweets with positive emoticons is much higher than the number of negative tweets with emoticons.





