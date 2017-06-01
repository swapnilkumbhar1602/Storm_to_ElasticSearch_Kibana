package cdac.storm.elastic;


import java.util.*;
import org.apache.storm.StormSubmitter;
//import org.apache.storm.hbase.bolt.HBaseBolt;
//import org.apache.storm.hbase.bolt.mapper.SimpleHBaseMapper;
import org.apache.storm.tuple.Fields;
//import in.cdac.twitter.storm.spout.TwitterSpout;
//import in.cdac.twitter.storm.bolt.ExtractorBolt;
import org.apache.storm.Config;
import org.apache.storm.topology.TopologyBuilder;
import org.elasticsearch.storm.EsBolt;


public class Topology
{
   public static void main(String[] args) throws Exception
   {
	   
	   
	   //Twitter Access tokens 
      String consumerKey = "kkGk1Dt9iGmgNe8NkGo6jdpot";
      String consumerSecret = "3RqbRjA6rbbGU9gFs8CfugzMDViJ5uwjKuxGAByeZXKkik0NhR";
	  String accessToken ="2912971610-MLornEgX31KXHZjL0BuuOxNVIBb0V9HUH2sLOCr";
      String accessTokenSecret = "rWiKWZScId7JkH38NhMxYX6hy8r7WyyqqYwGcJhKNsGnp";
	 String[] keyWords={ };
    
	 
	 //Storm configuration
      Config config = new Config();
      config.put(Config.NIMBUS_HOST, "storm");
      config.put(Config.NIMBUS_THRIFT_PORT,6627);
      config.put(Config.STORM_ZOOKEEPER_PORT,2181);
      config.setNumWorkers(4);
      config.setDebug(true);
      config.put(Config.TOPOLOGY_MAX_SPOUT_PENDING, 1);
   
      //Elasticsearch Configuration
      Map<String, String> conf = new HashMap<String, String>();
      conf.put("es.nodes", "10.210.0.180");
      conf.put("es.port","9200");
      conf.put("es.index.auto.create", "true");
      conf.put("es.nodes.wan.only","false");
      conf.put("es.net.http.auth.user","elastic");
      conf.put("es.net.http.auth.pass","changeme");
      conf.put("es.mapping.names","id:id,username:username,location:location,Area_name:Area_name,Area_type:Area_type,country:country,tweet:tweet,hashtag:hashtag,followers:followers,friends:friends,retweets:retweets,profile_location:profile_location,sentiment:sentiment");
     
      //Building topology
      TopologyBuilder builder = new TopologyBuilder();
      builder.setSpout("twitter-spout", new TwitterSpout(consumerKey,consumerSecret, accessToken, accessTokenSecret, keyWords));
      builder.setBolt("tweet-Extractor-bolt", new ExtractorBolt()).shuffleGrouping("twitter-spout");
      builder.setBolt("es-bolt", new EsBolt("storm/docs",conf), 5).fieldsGrouping("tweet-Extractor-bolt",new Fields("id","username","location","Area_name","Area_type","country","tweet","hashtag","followers","friends","retweets","profile_location","sentiment"));
      
   //Submitting topology
      StormSubmitter.submitTopology(args[0], config, builder.createTopology());
    
   }
}