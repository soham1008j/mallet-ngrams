import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
//import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Arun Balarkishnan on 15/09/14.
 */
public class GetWeightedTopics {
    private List<Pipe> pipeList;
    private String filePath;
    private String stopWordPath;
    private int numTopics;
    private int wordsPerTopic;
    private int numIterations;
    Alphabet dataAlphabet;



    public GetWeightedTopics(String filePath, String stopWordPath){
        this(filePath,stopWordPath,5,20);

    }
    public GetWeightedTopics(String filePath, String stopWordPath,int wordsPerTopic,int numIterations){
        this(filePath,stopWordPath,5,20,500);

    }
    public GetWeightedTopics(String filePath, String stopWordPath,int numTopics,int wordsPerTopic,int numIterations){
        this.filePath=filePath;
        this.stopWordPath=stopWordPath;
        this.numTopics=numTopics;
        this.wordsPerTopic=wordsPerTopic;
        this.numIterations=numIterations;
        try{
            init();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Adds the following preprocessing before topic modeling
     * 1. Converts all the characters to lower case
     * 2. Converts character sequence to token sequence
     * 3. Remove stop words (list to be provided as a text file, each word in a line)
     * 4. Generate N-Gram sequences (as provided by the user)- currently 1, 2 and 3 produced by default
     */
    private void init() throws Exception{
        pipeList = new ArrayList<Pipe>();

        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequenceRemoveStopwords(new File(stopWordPath), "UTF-8", false, false, false));
        pipeList.add(new TokenSequenceNGrams(new int[]{2, 3}));
        pipeList.add(new TokenSequence2FeatureSequenceWithBigrams());
        InstanceList instances = new InstanceList(new SerialPipes(pipeList));

        Reader fileReader = new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8");
        instances.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
                3, 2, 1)); // data, label, name fields
        ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);
        model.addInstances(instances);
        model.setNumThreads(2);
        model.setNumIterations(numIterations);
        model.estimate();

        this.dataAlphabet = instances.getDataAlphabet();

        FeatureSequenceWithBigrams tokens = (FeatureSequenceWithBigrams) model.getData().get(0).instance.getData();
        LabelSequence topics = model.getData().get(0).topicSequence;

        Formatter out = new Formatter(new StringBuilder(), Locale.US);
        Formatter forFile = new Formatter(new StringBuilder(), Locale.US);
        for (int position = 0; position < tokens.getLength(); position++) {
            out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
        }
        System.out.println(out);

        double[] topicDistribution = model.getTopicProbabilities(topics);
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("output/15Sep_bi_tri_out.txt")));
        // Show top 5 words in topics with proportions for the first document
        for (int topic = 0; topic < numTopics; topic++) {
            Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

            out = new Formatter(new StringBuilder(), Locale.US);
            out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
            //System.out.println(out + " Arun");//@Arun
            int rank = 0;
            String s="";
            Set<String> topicWords = new HashSet<String>();
            while (iterator.hasNext() && rank < 30) {
                IDSorter idCountPair = iterator.next();
//                @Arun - refining the ngram search
                if(isUnique(idCountPair.getID(),topicWords)) {
                out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
                rank++;
                }
                else
                    continue;
                s=dataAlphabet.lookupObject(idCountPair.getID()).toString()+"\t";

                for(int i=0;i<numTopics;i++){
                    if(topic==i)
                        s+=idCountPair.getWeight()+" ";
                    else
                        s+="0.01 ";
                }
                bw.write(s.substring(0,s.length()-1)+"\n");

            }

            System.out.println(out);
        }
        bw.close();

    }

    private  boolean isUnique(int id,Set<String> uniqueWords) {
        int countUnderscore;
        String infWord = dataAlphabet.lookupObject(id).toString();
        countUnderscore=infWord.split("_").length-1;
        String[] words = infWord.split("_");
        int matchCount=0;
        for(int i=0;i< words.length;i++){
            if(uniqueWords.contains(words[i])){
                matchCount++;
            }
            else
                uniqueWords.add(words[i]);
        }
        return (matchCount==0);
    }

    public static void main(String[] args) {
        String word ="oneadsg";
        System.out.println(word.split("_").length);
        String path = "/Users/bigdata/workspace/malletPreProcessing/resources/15Sep.txt";
        String stopOutPath ="/Users/bigdata/workspace/malletPreProcessing/resources/stopWords.txt";
        GetWeightedTopics gWT = new GetWeightedTopics(path,stopOutPath,3,20,2000);


    }

}
