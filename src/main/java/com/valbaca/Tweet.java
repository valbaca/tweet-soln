package com.valbaca;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Reads in text files from src/main/resources/ and splits them intelligently into multiple tweets if necessary, including prefixes
 */
public class Tweet {
    private static final int MAX_TWEET_SIZE = 140;


    public static void main(String[] args) {
        List<String> filenames = List.of("short.txt", "two_tweets.txt", "alice_short.txt", "alice.txt");
        for (String filename : filenames) {
            System.out.println("Sourcing tweet text from file: " + filename);
            String text = readResourceFileToString(filename);
            List<String> tweets = splitTweets(text);
            for (String tweet : tweets) {
                System.out.println(">" + tweet);
                System.out.println("" + tweet.length() + " chars");
                assert (tweet.length() <= 140);
            }

            System.out.println("=====");
        }

    }

    // Not meant for files that don't fit into memory
    private static String readResourceFileToString(String filename) {
        try {
            URI uri = Tweet.class.getClassLoader().getResource(filename).toURI();
            Path path = new File(uri).toPath();
            return Files.readString(path, StandardCharsets.UTF_8).replace("\n", "");
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    private static List<String> splitTweets(String text) {
        if (text.length() <= MAX_TWEET_SIZE) {
            return List.of(text);
        }

        // denom is how many chars to reserve for the denominator of the prefix, 1 = up to 9 tweets; 2 = up to 99 tweets, etc.
        return splitTweets(text, 1);
    }


    // because Java doesn't have tuples...and I need to return (string, int) pairs
    record StringAndPosition(String text, int pos) {
    }

    private static List<String> splitTweets(String text, int denom) {
        List<String> tweets = new ArrayList<>();
        int pos = 0; // pos is the character position within the entire text
        while (pos < text.length()) {
            int nthTweet = tweets.size() + 1;
            // check if the denom estimation is being exceeded
            if (("" + nthTweet).length() > denom) {
                // if the denom needs another character, hard-reset with one more denom character
                return splitTweets(text, 1 + denom);
            }

            int tweetPrefixSize = 1 + (("" + nthTweet).length()) + 1 + denom + 1; // (n/d)

            if (tweetPrefixSize >= MAX_TWEET_SIZE) {
                throw new RuntimeException("The tweet prefix doesn't even fit in a tweet anymore...use Dropbox???");
            }

            StringAndPosition tweetPos = pullTweet(text, pos, MAX_TWEET_SIZE - tweetPrefixSize);
            tweets.add(tweetPos.text);
            pos = tweetPos.pos;
        }

        List<String> tweetsWithPrefix = new ArrayList<>();
        int d = tweets.size();
        for (int i = 0; i < d; i++) {
            int n = i + 1;
            String tweetWithPrefix = String.format("(%s/%s)%s", n, d, tweets.get(i));
            tweetsWithPrefix.add(tweetWithPrefix);
        }
        return tweetsWithPrefix;
    }

    private static StringAndPosition pullTweet(String text, int pos, int tweetSize) {
        // starting at pos, pull from text until you get a text within tweetSize
        String tweet = "";
        while (tweet.length() < tweetSize && pos < text.length()) {
            StringAndPosition nextWordAndPos = pullWord(text, pos);
            if (nextWordAndPos == null) {
                // no more words to pull, exit loop
                pos = Integer.MAX_VALUE;
                break;
            }

            String nextWord = nextWordAndPos.text;
            String tweetWithWord = tweet + (tweet.isEmpty() ? "" : " ") + nextWord;

            if (tweetWithWord.length() <= tweetSize) {
                // base case: simply appending a small word into the tweet
                tweet = tweetWithWord;
                pos = nextWordAndPos.pos;
            } else if (nextWord.length() >= tweetSize) {
                // huge word case: the next could never fit in a tweet, so we have to break it mid-word
                boolean noSpacer = tweet.isEmpty();
                String tweetWithSpacer = (tweet + (noSpacer ? "" : " "));
                int nextWordBreak = tweetSize - tweetWithSpacer.length();
                tweet = tweetWithSpacer + nextWord.substring(0, nextWordBreak);
                pos = pos + (noSpacer ? 0 : 1) + nextWordBreak;
            } else {
                // else: next word isn't "too" huge, so we nicely break the tweet on a space...and force the loop to end
                break;
            }

        }
        return new StringAndPosition(tweet, pos);
    }

    private static StringAndPosition pullWord(String text, int pos) {
        // Using a Scanner is a bit "cheating" but it's really the best and easiest way to parse Strings
        Scanner scanner = new Scanner(text.substring(pos)).useDelimiter(" ");
        if (scanner.hasNext()) {
            String word = scanner.next();
            return new StringAndPosition(word, pos + word.length() + 1);
        } else {
            return null;
        }
    }
}
