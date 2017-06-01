package cdac.storm.elastic;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

import twitter4j.TwitterStream;

import twitter4j.TwitterStreamFactory;
//import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import org.apache.storm.Config;
import org.apache.storm.spout.SpoutOutputCollector;

import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import org.apache.storm.utils.Utils;

@SuppressWarnings("serial")
public class TwitterSpout extends BaseRichSpout
{
	SpoutOutputCollector _collector;
	LinkedBlockingQueue<Status> queue = null;
	TwitterStream  _twitterStream;

	String consumerKey;
	String consumerSecret;
	String accessToken;
	String accessTokenSecret;
	String[] keyWords;

	//constructor
	public TwitterSpout(String consumerKey, String consumerSecret,
			String accessToken, String accessTokenSecret, String[] keyWords)
	{
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
		this.accessTokenSecret = accessTokenSecret;
		this.keyWords = keyWords;
	}

	public TwitterSpout() 
	{

	}

	//called by nimbus 
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) 
	{

		System.out.println("oooooooooooooooooooopppppppppppppppppppeeeeeeeeeeeeeennnnnnnnnnnnnn");
		queue = new LinkedBlockingQueue<Status>(1000);
		_collector = collector;
//status:it is a object received from twitter4j api when some one post's  onstatus() method called
		StatusListener listener = new StatusListener()
		{
			
			public void onStatus(Status status) 
			{
				queue.offer(status);
			}

			public void onDeletionNotice(StatusDeletionNotice sdn) {}

			public void onTrackLimitationNotice(int i) {}

			public void onScrubGeo(long l, long l1)
			{
				System.out.println("geolocation:"+l+l1);
				
			}
			public void onException(Exception ex) {}

			public void onStallWarning(StallWarning arg0) {}
		};

		//connecting to twitter api
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey(consumerKey)
		.setOAuthConsumerSecret(consumerSecret)
		.setOAuthAccessToken(accessToken)
		.setOAuthAccessTokenSecret(accessTokenSecret);

		_twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
		_twitterStream.addListener(listener);

		//filtering output based on language ,keyword,location
		
		
	//	if (keyWords.length == 0) 
	//{
	//FilterQuery query = new FilterQuery();
		//query.language("en");

		//_twitterStream.filter(query);
		_twitterStream.sample("en");

		//}else 
		//{
	//FilterQuery query = new FilterQuery();
	//	query.track(keyWords);
		// query.language("en");
		///_twitterStream.filter(query);
	//	/}
	}

	//called by nimbus 
	public void nextTuple() 
	{
		Status ret = queue.poll();

		if (ret == null) 
		{
			Utils.sleep(50);
		}
		else 
		{
			//passing tuble consisting status object to next bolt specified in topology
			
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			_collector.emit(new Values(ret));
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			
		}
	}

	public void close() 
	{
		_twitterStream.shutdown();
	}

	@Override
	public Map<String, Object> getComponentConfiguration() 
	{
		Config ret = new Config();
		ret.setMaxTaskParallelism(1);
		return ret;
	}

	public void ack(Object id) {}

	public void fail(Object id) {}
//to tell next bolt what are the fields to extract 
	public void declareOutputFields(OutputFieldsDeclarer declarer)
	{
		declarer.declare(new Fields("tweet"));
	}
}