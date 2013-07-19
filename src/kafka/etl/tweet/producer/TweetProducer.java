/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.etl.tweet.producer;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import kafka.etl.KafkaETLKey;
import kafka.etl.KafkaETLRequest;
import kafka.etl.Props;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.javaapi.producer.SyncProducer;
import kafka.message.Message;
import kafka.producer.SyncProducerConfig;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapred.JobConf;

import twitter4j.*;

/**
 * Use this class to produce test events to Kafka server. Each event contains a
 * random timestamp in text format.
 */
@SuppressWarnings("deprecation")
public class TweetProducer extends Thread {

	protected final static Random RANDOM = new Random(
			System.currentTimeMillis());

	protected Props _props;
	protected SyncProducer _producer = null;
	protected URI _uri = null;
	protected String _topic;
	protected int _count;
	protected String _offsetsDir;
	protected final int TCP_BUFFER_SIZE = 300 * 1000;
	protected final int CONNECT_TIMEOUT = 20000; // ms
	protected final int RECONNECT_INTERVAL = Integer.MAX_VALUE; // ms
	protected String _twitterContent = "";
	protected String _twitterUser = "";

	public TweetProducer(String id, Props props) throws Exception {
		_props = props;
		_topic = props.getProperty("kafka.etl.topic");
		System.out.println("topics=" + _topic);
		_count = props.getInt("event.count");

		_offsetsDir = _props.getProperty("input");

		// initialize kafka producer to generate count events
		String serverUri = _props.getProperty("kafka.server.uri");
		_uri = new URI(serverUri);

		System.out.println("server uri:" + _uri.toString());
		Properties producerProps = new Properties();
		producerProps.put("host", _uri.getHost());
		producerProps.put("port", String.valueOf(_uri.getPort()));
		producerProps.put("buffer.size", String.valueOf(TCP_BUFFER_SIZE));
		producerProps
				.put("connect.timeout.ms", String.valueOf(CONNECT_TIMEOUT));
		producerProps.put("reconnect.interval",
				String.valueOf(RECONNECT_INTERVAL));
		_producer = new SyncProducer(new SyncProducerConfig(producerProps));

	}


	TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
	UserStreamListener listener = new UserStreamListener() {
		@Override
		public void onStatus(Status status) {
			System.out.println("onStatus @" + status.getUser().getScreenName()
					+ " - " + status.getText());

			_twitterUser = status.getUser().getScreenName();
			_twitterContent = status.getText();
			
			System.out.println(_twitterUser);
			System.out.println(_twitterUser.isEmpty());
			
			List<Message> list = new ArrayList<Message>(); 
			byte[] bytes;
			try {
				bytes = (_twitterUser + " - " +_twitterContent).getBytes("UTF8");
				  list.add(new Message(bytes));
				  
				  System.out.println(_topic);
				  System.out.println(list);
				  
				  _producer.send(_topic, new ByteBufferMessageSet(kafka.message.NoCompressionCodec$.MODULE$, list));
					
				  // generate offset files
					generateOffsets();
			} catch ( Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}

		@Override
		public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
			System.out.println("Got a status deletion notice id:"
					+ statusDeletionNotice.getStatusId());
		}

		@Override
		public void onDeletionNotice(long directMessageId, long userId) {
			System.out.println("Got a direct message deletion notice id:"
					+ directMessageId);
		}

		@Override
		public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
			System.out.println("Got a track limitation notice:"
					+ numberOfLimitedStatuses);
		}

		@Override
		public void onScrubGeo(long userId, long upToStatusId) {
			System.out.println("Got scrub_geo event userId:" + userId
					+ " upToStatusId:" + upToStatusId);
		}

		@Override
		public void onStallWarning(StallWarning warning) {
			System.out.println("Got stall warning:" + warning);
		}

		@Override
		public void onFriendList(long[] friendIds) {
			System.out.print("onFriendList");
			for (long friendId : friendIds) {
				System.out.print(" " + friendId);
			}
			System.out.println();
		}

		@Override
		public void onFavorite(User source, User target, Status favoritedStatus) {
			System.out.println("onFavorite source:@" + source.getScreenName()
					+ " target:@" + target.getScreenName() + " @"
					+ favoritedStatus.getUser().getScreenName() + " - "
					+ favoritedStatus.getText());
		}

		@Override
		public void onUnfavorite(User source, User target,
				Status unfavoritedStatus) {
			System.out.println("onUnFavorite source:@" + source.getScreenName()
					+ " target:@" + target.getScreenName() + " @"
					+ unfavoritedStatus.getUser().getScreenName() + " - "
					+ unfavoritedStatus.getText());
		}

		@Override
		public void onFollow(User source, User followedUser) {
			System.out.println("onFollow source:@" + source.getScreenName()
					+ " target:@" + followedUser.getScreenName());
		}

		@Override
		public void onDirectMessage(DirectMessage directMessage) {
			System.out.println("onDirectMessage text:"
					+ directMessage.getText());
		}

		@Override
		public void onUserListMemberAddition(User addedMember, User listOwner,
				UserList list) {
			System.out.println("onUserListMemberAddition added member:@"
					+ addedMember.getScreenName() + " listOwner:@"
					+ listOwner.getScreenName() + " list:" + list.getName());
		}

		@Override
		public void onUserListMemberDeletion(User deletedMember,
				User listOwner, UserList list) {
			System.out.println("onUserListMemberDeleted deleted member:@"
					+ deletedMember.getScreenName() + " listOwner:@"
					+ listOwner.getScreenName() + " list:" + list.getName());
		}

		@Override
		public void onUserListSubscription(User subscriber, User listOwner,
				UserList list) {
			System.out.println("onUserListSubscribed subscriber:@"
					+ subscriber.getScreenName() + " listOwner:@"
					+ listOwner.getScreenName() + " list:" + list.getName());
		}

		@Override
		public void onUserListUnsubscription(User subscriber, User listOwner,
				UserList list) {
			System.out.println("onUserListUnsubscribed subscriber:@"
					+ subscriber.getScreenName() + " listOwner:@"
					+ listOwner.getScreenName() + " list:" + list.getName());
		}

		@Override
		public void onUserListCreation(User listOwner, UserList list) {
			System.out.println("onUserListCreated  listOwner:@"
					+ listOwner.getScreenName() + " list:" + list.getName());
		}

		@Override
		public void onUserListUpdate(User listOwner, UserList list) {
			System.out.println("onUserListUpdated  listOwner:@"
					+ listOwner.getScreenName() + " list:" + list.getName());
		}

		@Override
		public void onUserListDeletion(User listOwner, UserList list) {
			System.out.println("onUserListDestroyed  listOwner:@"
					+ listOwner.getScreenName() + " list:" + list.getName());
		}

		@Override
		public void onUserProfileUpdate(User updatedUser) {
			System.out.println("onUserProfileUpdated user:@"
					+ updatedUser.getScreenName());
		}

		@Override
		public void onBlock(User source, User blockedUser) {
			System.out.println("onBlock source:@" + source.getScreenName()
					+ " target:@" + blockedUser.getScreenName());
		}

		@Override
		public void onUnblock(User source, User unblockedUser) {
			System.out.println("onUnblock source:@" + source.getScreenName()
					+ " target:@" + unblockedUser.getScreenName());
		}

		@Override
		public void onException(Exception ex) {
			ex.printStackTrace();
			System.out.println("onException:" + ex.getMessage());
		}
	};

	
	public void run() {

		twitterStream.addListener(listener);
		twitterStream.user();

		
		//System.out.println("*  " + _twitterUser);
		//System.out.println("*  " + _twitterUser.isEmpty());
		

		/*
		  List<Message> list = new ArrayList<Message>();
		  
		  while(true) {
			  if(!_twitterUser.isEmpty()){
				  byte[] bytes;
				try {
					bytes = (_twitterUser + " - " +_twitterContent).getBytes("UTF8");
					list.add(new Message(bytes));
					  _producer.send(_topic, new
								 ByteBufferMessageSet( kafka.message.NoCompressionCodec$.MODULE$,
										  list));
					_twitterUser = "";
					_twitterContent = "";
					
					// generate offset files
					generateOffsets();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				  
			  }
		  }
	  */
			// close the producer
			//_producer.close();

	}

	protected void generateOffsets() throws Exception {
		JobConf conf = new JobConf();
		java.util.Date date = new java.util.Date();
		conf.set("hadoop.job.ugi", _props.getProperty("hadoop.job.ugi"));
		conf.setCompressMapOutput(false);
		Calendar cal = Calendar.getInstance();
		Path outPath = new Path(_offsetsDir + Path.SEPARATOR + "1.dat");
		FileSystem fs = outPath.getFileSystem(conf);
		if (fs.exists(outPath))	fs.delete(outPath);

		KafkaETLRequest request = new KafkaETLRequest(_topic, "tcp://"
				+ _uri.getHost() + ":" + _uri.getPort(), 0);

		System.out.println("Dump " + request.toString() + " to "
				+ outPath.toUri().toString());
		
		byte[] bytes = request.toString().getBytes("UTF-8");
		KafkaETLKey dummyKey = new KafkaETLKey();
		SequenceFile.setDefaultCompressionType(conf,
				SequenceFile.CompressionType.NONE);
		SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf,
				outPath, KafkaETLKey.class, BytesWritable.class);
		writer.append(dummyKey, new BytesWritable(bytes));
		writer.close();
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 1)
			throw new Exception("Usage: - config_file");

		Props props = new Props(args[0]);
		TweetProducer job = new TweetProducer("DataGenerator", props);
		job.start();
	}

}
