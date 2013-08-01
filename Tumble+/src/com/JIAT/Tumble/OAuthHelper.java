package com.JIAT.Tumble;

/**
 * Created with IntelliJ IDEA.
 * User: Arsen
 * Date: 7/30/13
 * Time: 9:44 AM
 * To change this template use File | Settings | File Templates.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;


import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


//ASync thread class to download data outside the main thread
class DataDownloader extends AsyncTask<String, Void, String>
{
    private String data = "";

    public String getData()
    {
        return data;
    }

    protected String doInBackground(String... requestUrl)
    {
        //We have the URL to perform our request, time to actually go perform the request
        URL url = null;
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        String line = "";

        try
        {
            url = new URL(requestUrl[0]);
            inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            //Use the buffered reader to store the info line by line into the data string
            while((line = bufferedReader.readLine()) != null)
            {
                data = data + line;
            }
        }
        catch (MalformedURLException e)
        {
            Log.e("Error", "Malformed Exception", e);
        }
        catch (IOException e)
        {
            Log.e("Error", "IO Exception", e);
        }
        catch (Exception e)
        {
            Log.e("Error", "Exception", e);
        }
        finally
        {
            try
            {
                if(inputStream != null)
                    inputStream.close();
            }
            catch (IOException e)
            {
                Log.e("Error", "IO Exception - Closing Stream", e);
            }
        }

        //By this point, our data string has been filled and we can return it
        return data;
    }
}



public class OAuthHelper {

    //Nessecarily evil public static variables
    public static String oauthConsumerKey;
    public static String oauthSecretKey;
    public static String oauthAccessToken;
    public static String oauthAccessSecret;

    public static String oauthRequestToken;
    public static String oauthRequestSecret;
    public static String oauthVerifier;

    //Gets Request Tokens and Stores Them Statically
    public static int GetRequestTokens()
    {
        String baseUrl = "http://www.tumblr.com/oauth/request_token";

        String tokenData = OAuthRequest(baseUrl, "GET", "", "", "");

        if(tokenData.equals(null) || tokenData.equals(""))
        {
            return -1;
        }

        //Parse the request token and the secret from the raw token data
        int requestSplitIndex = tokenData.indexOf ("&");
        String token = tokenData.substring (12, requestSplitIndex);
        String tokenSecret = tokenData.substring (token.length() + 32);
        tokenSecret = tokenSecret.substring (0, tokenSecret.indexOf("&"));

        oauthRequestToken = token;
        oauthRequestSecret = tokenSecret;

        return 1;
    }

    //Gets Access Tokens and Stores Them Statically
    public static int GetAccessTokens()
    {
        String baseUrl = "http://www.tumblr.com/oauth/access_token";

        String tokenData = OAuthRequest (baseUrl, "GET", oauthRequestToken, oauthRequestSecret, oauthVerifier);

        if(tokenData.equals(null) || tokenData.equals(""))
        {
            return -1;
        }

        //Parse the access token and secret from the raw token data
        int accessSplitIndex = tokenData.indexOf ("&");
        String accessToken = tokenData.substring (12, accessSplitIndex);
        String accessSecret = tokenData.substring (accessToken.length() + 32);

        oauthAccessToken = accessToken;
        oauthAccessSecret = accessSecret;

        return 1;
    }

    //Performs a basic OAuth Request and returns the callback
    public static String OAuthRequest(String baseUrl, String requestType, String token, String tokenSecret, String verifier)
    {
        //Gather all required data to generate the signature
        String oauthKey = oauthConsumerKey;
        String oauthSecret = oauthSecretKey;

        String timestamp = getTimestamp();
        String nonce = generateNonce();

        String oauthSignature;
        try{
            oauthSignature = generateSignature(baseUrl, requestType, nonce, timestamp, token, tokenSecret, verifier);
        }
        catch (Exception e)
        {
            String message = "";
            message = e.getMessage();
            Log.v("SIG ERROR: ", message);
            return null;
        }

        StringBuilder builder = new StringBuilder();

        builder.append(baseUrl);
        builder.append("?oauth_consumer_key=");
        builder.append(oauthConsumerKey);
        builder.append("&oauth_nonce=");
        builder.append(nonce);
        builder.append("&oauth_signature_method=HMAC-SHA1");
        builder.append("&oauth_timestamp=");
        builder.append(timestamp);
        builder.append("&oauth_version=1.0");
        if(token != "" && token != null)
        {
            builder.append("&oauth_token=");
            builder.append(token);
        }
        if(verifier != "" && verifier != null)
        {
            builder.append("&oauth_verifier=");
            builder.append(verifier);
        }
        builder.append("&oauth_signature=");
        builder.append(oauthSignature);

        String requestUrl = builder.toString();

        //Now that we've built the request URL, call a thread to download the data
        //We're going to have to wait for the thread to finish before we return it
        String data = "";

        Log.v("Request URL: ", requestUrl);

        DataDownloader dataDownloader = new DataDownloader();
        dataDownloader.execute(requestUrl);

        //Try to wait for the thread to finish
        try
        {
            dataDownloader.get();
        }
        catch (Exception e)
        {
            //If there is an exception, we return null
            Log.e("Error", "Exception", e);
            return null;
        }

        //If we get this far and no exceptions, get the data out of the thread
        data = dataDownloader.getData();

        Log.v("Data: ", data);

        return data;
    }

    //Return a base64 encoded signature for the OAuth request
    private static String generateSignature(String base, String type, String nonce, String timestamp, String token, String tokenSecret, String verifier)
        throws NoSuchAlgorithmException, InvalidKeyException
    {
        String encodedBase = Uri.encode(base);

        StringBuilder builder = new StringBuilder();

        //These are in alphabetical order
        builder.append("oauth_consumer_key=");
        builder.append(oauthConsumerKey);
        builder.append("&oauth_nonce=");
        builder.append(nonce);
        builder.append("&oauth_signature_method=HMAC-SHA1");
        builder.append("&oauth_timestamp=");
        builder.append(timestamp);
        if(token != "" && token != null)
        {
            builder.append("&oauth_token=");
            builder.append(token);
        }
        if(verifier != "" && verifier != null)
        {
            builder.append("&oauth_verifier=");
            builder.append(verifier);
        }
        builder.append("&oauth_version=1.0");


        String params = builder.toString();
        //Percent encoded the whole url
        String encodedParams = Uri.encode(params);

        String completeUrl = type + "&" + encodedBase + "&" + encodedParams;

        String completeSecret = oauthSecretKey;

        if(tokenSecret != null && tokenSecret !="")
        {
            completeSecret = completeSecret + "&" +  tokenSecret;
        }else
        {
            completeSecret = completeSecret + "&";
        }

        Log.v("Complete URL: ", completeUrl);
        Log.v("Complete Key: ", completeSecret);

        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secret = new SecretKeySpec(completeSecret.getBytes(), mac.getAlgorithm());
        mac.init(secret);
        byte[] sig = mac.doFinal(completeUrl.getBytes());

        String signature = Base64.encodeToString(sig,0);
        signature = signature.replace("+", "%2b"); //Specifically encode all +s to %2b

        return signature;
    }

    //Return the seconds since Jan 1, 1970
    private static String getTimestamp()
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        long secondsSince = calendar.getTimeInMillis() / 1000L;

        return "" + secondsSince;
    }

    //Returns a simple nonce string
    private static String generateNonce()
    {
        int max = 9999999;
        int min = 123400;

        double rand = Math.random();

        //Return a string representation of a random value between the max and min
        return ""  + (int)(min + (rand * ((max - min) + 1)));
    }
}