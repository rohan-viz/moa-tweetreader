/*
 *    TokenTwitterStreamingAPI.java
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * This class deals with getting a request token with the Key and Key Secret and returning the URL
 * needed to get the Access Token and it's Secret for connecting with Twitter.
 * @author Kenneth Gibson (kjjg1@waikato.ac.nz)
 *
 */
public class TokenTwitterStreamingAPI {

    protected String myKey = "3ghHxwhYZaaa3iIYIeXLxA";

    protected String myKeySecret = "5asfT2bbwiznijdYkuiPJolEz2XdysJzB33X8rx3cN8";

    private RequestToken requestToken = null;

    AccessToken accessToken = null;

    protected int pin = 0;

    private static int numberInstances = 0;

    public TokenTwitterStreamingAPI() {
        //askPin();
        this.numberInstances++;
        getCredentials();
    }

    public AccessToken getAccessToken() {
        return this.accessToken;
    }
    /*
    public String getAccessTokenSecret()
    {
    return accessTokenSecret;
    }*/

    public String getMyKey() {
        return myKey;
    }

    public String getMyKeySecret() {
        return myKeySecret;
    }
    private String tokenFile;

    public void getCredentials() {

        //if the access key and secret already exist, use them,
        //otherwise get new ones with oAuth
        File file;
        if (numberInstances == 1) {
            tokenFile = "tokens1.txt";
        } else {
            tokenFile = "tokens2.txt";
        }

        file = new File(tokenFile);

        //AccessToken at;
        if (!file.exists()) {
            this.accessToken = newAccessToken(myKey, myKeySecret);
        } else {
            this.accessToken = loadAccessToken();
        }
        //accessToken = at.getToken();
        //accessTokenSecret = at.getTokenSecret();
    }

    /*
     * gets a new access token and secret from the Twitter api with oAuth
     */
    public AccessToken newAccessToken(String k, String s) {

        Twitter twitter = new TwitterFactory().getInstance();
        AccessToken theAccessToken = null;
        twitter.setOAuthConsumer(k, s);

        try {
            RequestToken requestToken = twitter.getOAuthRequestToken();
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            while (null == theAccessToken) {
                System.out.println("Open the following URL and grant access to your account:");
                System.out.println(requestToken.getAuthorizationURL());
                System.out.print("Enter the PIN(if available) or just hit enter.[PIN]:");
                String pin = br.readLine();
                try {

                    if (pin.length() > 0) {
                        theAccessToken = twitter.getOAuthAccessToken(requestToken, pin);
                    } else {
                        theAccessToken = twitter.getOAuthAccessToken();
                    }

                } catch (TwitterException te) {

                    if (401 == te.getStatusCode()) {
                        System.out.println("Unable to get the access token.");
                    } else {
                        te.printStackTrace();
                    }
                }
            }
            //write the access tokens to file for future use
            storeAccessToken(twitter.verifyCredentials().getId(), theAccessToken);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TwitterException e) {
            e.printStackTrace();
        }

        return theAccessToken;
    }

    /*
     * Writes the access token and access token secret to a file names "tokens.txt" in the
     * current directory.
     */
    private void storeAccessToken(long useId, AccessToken at) {

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(tokenFile));
            out.write(at.getToken());
            out.newLine();
            out.write(at.getTokenSecret());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * loads in the access token and secret from a file names "tokens.txt"
     * in the current directory and stores them in the global variables
     */
    public AccessToken loadAccessToken() {

        File file = new File(tokenFile);
        String token = null;
        String tokenSecret = null;
        AccessToken at = null;

        if (file.exists()) {
            try {

                BufferedReader reader = new BufferedReader(new FileReader(file));

                token = reader.readLine();
                tokenSecret = reader.readLine();

                at = new AccessToken(token, tokenSecret);

                reader.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //System.out.println("Error: Missing File");
            //System.out.println("File with Access Tokens does not exist");
        }
        return at;
    }
}
