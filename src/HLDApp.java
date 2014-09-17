
import au.com.bytecode.opencsv.CSVWriter;
import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.UnknownHostException;

//import com.mongodb.BasicDBObject;
//import com.mongodb.DB;
//import com.mongodb.DBCollection;
//import com.mongodb.DBCursor;
//import com.mongodb.DBObject;
//import com.mongodb.MongoClient;

public class HLDApp {
    /*public InstanceList readDirectory(File directory) {
        return readDirectories(new File[] {directory});
    }

    public InstanceList readDirectories(File[] directories) {
        
        // Construct a file iterator, starting with the 
        //  specified directories, and recursing through subdirectories.
        // The second argument specifies a FileFilter to use to select
        //  files within a directory.
        // The third argument is a Pattern that is applied to the 
        //   filename to produce a class label. In this case, I've 
        //   asked it to use the last directory name in the path.
        FileIterator iterator =
            new FileIterator(directories,
                             new TxtFilter(),
                             FileIterator.LAST_DIRECTORY);

        // Construct a new instance list, passing it the pipe
        //  we want to use to process instances.
        InstanceList instances = new InstanceList(pipe);

        // Now process each instance provided by the iterator.
        instances.addThruPipe(iterator);

        return instances;
    }*/


    //@Arun: Code edited for sending output to http://felix-kling.de/lda_model_viewer/ , sample data can be found there (sample.txt)

    public static void main(String[] args) throws Exception {

        //@Arun: Output file
//        FileWriter output = new FileWriter("/resources/output.txt");

        // Begin by importing documents from text to feature sequences
//    	generateInput();
    	CSVWriter outCsv = new CSVWriter(new FileWriter("out1.csv"));
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
        //String filePath = "/Users/bigdata2/Downloads/input.csv";
        String filePath = "/Users/bigdata/workspace/malletPreProcessing/resources/abstract/";
        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add(new Input2CharSequence("UTF-8"));
        Pattern ABVPattern = Pattern.compile("\\p{Lu}{2,4}");
       // Pattern tokenPattern =
         //       Pattern.compile("[\\p{L}\\p{N}_]+");
       pipeList.add(new CharSequenceReplace(ABVPattern,""));
        HashMap<Integer,ArrayList<String>> topicHashMap = new HashMap<Integer,ArrayList<String>>();
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        //pipeList.add(new CharSequence2TokenSequence());

        //pipeList.add(new CharSequence2TokenSequence(tokenPattern));
        //pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequenceRemoveNonAlpha());
        //pipeList.add(new TokenSequenceLowercase());
        pipeList.add(new TokenSequenceRemoveStopwords(new File("/Users/bigdata/workspace/malletPreProcessing/resources/stopWords.txt"), "UTF-8", true, false, true));
        //pipeList.add( new TokenSequence2FeatureSequence() );
        pipeList.add(new TokenSequenceNGrams(new int[]{ 2}));
        pipeList.add(new TokenSequence2FeatureSequenceWithBigrams());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));
 
        //Reader fileReader = new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8");
        FileIterator fIterator =
                new FileIterator(new File[]{new File(filePath)},
                                 new TxtFilter(),
                                 FileIterator.LAST_DIRECTORY);
        
        //instances.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
         //       3, 2, 1)); // data, label, name fields
        instances.addThruPipe(fIterator);
        //instances.addThruPipe(new FileIterator (new File(filePath))); // data, label, name fields
        // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
        //  Note that the first parameter is passed as the sum over topics, while
        //  the second is the parameter for a single dimension of the Dirichlet prior.
        //int numTopics = 20;
        HierarchicalLDA model = new HierarchicalLDA();
        model.initialize(instances, instances, 4, new Randoms());
        //model.setAlpha(10.0);
		//model.setGamma(1.0);
		//model.setEta(0.1);
//        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("heira.txt")));
//        bw.write(model.estimate(200));
//        System.out.println("printing here");
//        PrintStream ps = new PrintStream(new FileOutputStream(new File("heira.txt")));
//        System.out.println(model.toString());
//        System.out.println("stop printing");
//        bw.close();
        System.setOut(outputFile("heira.txt"));
        model.estimate(200);
        model.setProgressDisplay(true);
        model.setTopicDisplay(50, 2);
        model.setStateFile("out.txt");
        model.printState();
        
        
        // Run the model for 50 iterations and stop (this is for testing only, 
        //  for real applications, use 1000 to 2000 iterations)
       
        // Show the words and topics in the first instance

        // The data alphabet maps word IDs to strings
       
        
    }
    protected static PrintStream outputFile(String name) {
        try {
			return new PrintStream(new BufferedOutputStream(new FileOutputStream(name)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
 
   /*public static void generateInput()
   {
	   try {
		MongoClient mongo = new MongoClient("localhost", 27017);
		DB db = mongo.getDB("quickscrape");
		DBCollection collection = db.getCollection("test");
		BasicDBObject searchQuery = new BasicDBObject();
		//searchQuery.put("source", "communication");
		DBCursor cursor = collection.find(searchQuery);
		System.out.println("list");
		int count =0;
		while(cursor.hasNext())
		{   try{
			DBObject dbcontent = cursor.next();
			String articleAbstract = (String)dbcontent.get("abstract");
			if(articleAbstract!=null)
			{
			File abstractFile = new File("/Users/bigdata2/Downloads/my_test2/abstract/"+String.valueOf(count)+".txt");
			if (!abstractFile.exists()) {
				abstractFile.createNewFile();
			}
           
			FileWriter fw = new FileWriter(abstractFile.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(articleAbstract);
			count++;
			bw.close();
			fw.close();
			}
		}
		catch(Exception e1)
		{
			System.out.println("Exception occured");
			e1.printStackTrace();
		}
			//System.out.println(articleAbstract);
		}
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }*/
   


}