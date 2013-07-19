package kafka.etl.tweet.producer;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import twitter4j.*;


public class TestTweetAPI {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("TEST");
		
		try {
            // gets Twitter instance with default credentials
            Twitter twitter = new TwitterFactory().getInstance();
            User user = twitter.verifyCredentials();
            
            BufferedReader br = null;
            String idFromFile;
            long i=1L;
            br = new BufferedReader(new FileReader("sinceIDstorage"));
            if ((idFromFile = br.readLine())!=null) {
            	System.out.println(idFromFile);
            	i = Long.parseLong(idFromFile);
            	br.close();
            };
            
            Paging paging = new Paging(1).sinceId(i);
            List<Status> statuses = twitter.getHomeTimeline(paging);
            
            FileWriter fw = new FileWriter("sinceIDstorage");
            BufferedWriter bw = new BufferedWriter(fw);
            if (statuses.size() > 1 ) {
            	bw.write(Long.toString(statuses.get(1).getId()));
            	bw.close();
            }

            System.out.println("yanfang0 @" + user.getScreenName() + "'s home timeline.");
            for (Status status : statuses) {
            	System.out.println(status.getId());
            
                System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
            }
            
         // The factory instance is re-useable and thread safe.
            /*
            Query query = new Query("test");
            QueryResult result = twitter.search(query);
            
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            
            for (Status status : result.getTweets()) {
                System.out.println("@" + status.getUser().getScreenName() + ":" + status.getText());
            }
            
            */
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
            System.exit(-1);
        }
		
		
		
    }
		

}
