
package cdac.storm.elastic;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.logging.log4j.core.util.Charsets;
import org.apache.storm.shade.com.google.common.base.Splitter;
import org.apache.storm.shade.com.google.common.collect.Lists;
import org.apache.storm.shade.com.google.common.collect.Maps;
import org.apache.storm.shade.com.google.common.io.Files;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
//import com.google.common.base.Charsets;
//import com.google.common.base.Splitter;
import org.apache.storm.tuple.Values;

//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import com.google.common.io.Files;

import twitter4j.Status;
import twitter4j.URLEntity;


public class ExtractorBolt extends BaseRichBolt
{
	OutputCollector collector;
	
	
	//private static final Logger LOGGER = LoggerFactory.getLogger(SentimentCalculatorBolt.class);
	//private static final long serialVersionUID = -5094673458112825122L;


	//private final long logIntervalInSeconds;

	private long runCounter;
	
	private SortedMap<String,Integer> afinnSentimentMap = null;
	

	//public SentimentCalculatorBolt(final long logIntervalInSeconds)
	//{
	//	this.logIntervalInSeconds = logIntervalInSeconds;
	//}

	public void execute(Tuple tuple)
	{
		Status status= (Status)tuple.getValueByField("tweet");
		
		final int intsentimentCurrentTweet = getSentimentOfTweet(status);
		String stringsentiment="Neutral";
		
		if(intsentimentCurrentTweet>0)
		{
			stringsentiment="Positive";
		}
		else if(intsentimentCurrentTweet<0)
		{
			stringsentiment="Negative";
		}
		
		
		//if(status.getGeoLocation()!=null)
	
		
		Long id = status.getId();
		String username = status.getUser().getName();

		String location = "-74.759473,61.687973";
		if(status.getGeoLocation()!=null)
			location=status.getGeoLocation().getLatitude()+"," +status.getGeoLocation().getLongitude();

		String area_name ="area_name not found";
		String area_type="area_type not found";
		String country ="country not found";

				if(status.getPlace()!=null)
				{
					area_name=status.getPlace().getName();

					area_type = status.getPlace().getPlaceType();

					country = status.getPlace().getCountry();
				}
		String tweet=filterOutURLFromTweet(status);
		String hashtag=" ";
				if(status.getHashtagEntities().length!=0)
					hashtag=status.getHashtagEntities()[0].getText();

		int followers = status.getUser().getFollowersCount();


		int friends = status.getUser().getFriendsCount();
		int retweets=status.getRetweetCount();
		String profile_location="not found profile";
		if(status.getUser().getLocation()!=null)
			profile_location=status.getUser().getLocation();

		//type_of_device=status.getMedia


		collector.emit(new Values(id,username,location,area_name,area_type,country,tweet,hashtag,followers,friends,retweets,profile_location,stringsentiment));
		collector.ack(tuple);
		}
	

	public void prepare(Map map, TopologyContext topologyContext, OutputCollector collector) 
	{
         afinnSentimentMap = Maps.newTreeMap();
		
		this.collector=collector;
		String text;
		try {
			text = Files.toString(new File("/home/tapac/apache-storm-1.0.2/swap/AFINN-111.txt"), Charsets.UTF_8);
			final Iterable<String> lineSplit = Splitter.on("\n").trimResults().omitEmptyStrings().split(text);
			List<String> tabSplit;
			for (final String str: lineSplit)
			{
				tabSplit = Lists.newArrayList(Splitter.on("\t").trimResults().omitEmptyStrings().split(str));
				afinnSentimentMap.put(tabSplit.get(0), Integer.parseInt(tabSplit.get(1)));
			}
		} catch (IOException e)
		{
		
			e.printStackTrace();
		
		}

			

		runCounter = 0;
		
		
	}

	public void declareOutputFields(OutputFieldsDeclarer Declarer) 
	{
		Declarer.declare(new Fields("id","username","location","Area_name","Area_type","country","tweet","hashtag","followers","friends","retweets","profile_location","sentiment"));

	}
	
	private final int getSentimentOfTweet(final Status status)
	{
		
		final String tweet = status.getText().replaceAll("\\p{Punct}|\\n", " ").toLowerCase();
		
		final Iterable<String> words = Splitter.on(' ')
				                               .trimResults()
				                               .omitEmptyStrings()
				                               .split(tweet);
		
		int sentimentOfCurrentTweet = 0;
		
		
		for (final String word : words)
		{
			if(afinnSentimentMap.containsKey(word))
			{
				sentimentOfCurrentTweet += afinnSentimentMap.get(word);
			}
		}
		
		return sentimentOfCurrentTweet;
	}

	
	private String filterOutURLFromTweet(final Status status) 
	{
final String tweet = status.getText();
		
		final URLEntity[] urlEntities = status.getURLEntities();
		int startOfURL;
		int endOfURL;
		String truncatedTweet = "";
		
		for(final URLEntity urlEntity: urlEntities)
		{
			startOfURL = urlEntity.getStart();
			endOfURL = urlEntity.getEnd();
			truncatedTweet += tweet.substring(0, startOfURL) + tweet.substring(endOfURL);
		}
		return truncatedTweet;
	}

}
