import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;

public class TopicModelingApp {

    List<List<String>> uniqueNGramDict = new ArrayList();
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
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
        String filePath = "/Users/bigdata/workspace/malletPreProcessing/resources/15Sep.txt";
//        String filePath = "/Users/bigdata/workspace/malletPreProcessing/resources/ajay_termite.txt";
        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequenceRemoveStopwords(new File("/Users/bigdata/workspace/malletPreProcessing/resources/stopWords.txt"), "UTF-8", false, false, false));
        //pipeList.add( new TokenSequence2FeatureSequence() );

//        pipeList.add(new TokenSequenceNGrams(new int[]{2, 3}));
        pipeList.add(new TokenSequenceNGrams(new int[]{ 2, 3}));
        pipeList.add(new TokenSequence2FeatureSequenceWithBigrams());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));

        Reader fileReader = new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8");
        instances.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
                3, 2, 1)); // data, label, name fields
        //instances.addThruPipe(new FileIterator (new File(filePath))); // data, label, name fields
        // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
        //  Note that the first parameter is passed as the sum over topics, while
        //  the second is the parameter for a single dimension of the Dirichlet prior.
        int numTopics = 5;
        ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);
        //HierarchicalLDA model = new HierarchicalLDA();
        model.addInstances(instances);

        // Use two parallel samplers, which each look at one half the corpus and combine
        //  statistics after every iteration.
        model.setNumThreads(2);

        // Run the model for 50 iterations and stop (this is for testing only, 
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(200);
        model.estimate();
//        model.
        // Show the words and topics in the first instance

        // The data alphabet maps word IDs to strings
        Alphabet dataAlphabet = instances.getDataAlphabet();

        FeatureSequenceWithBigrams tokens = (FeatureSequenceWithBigrams) model.getData().get(0).instance.getData();
        LabelSequence topics = model.getData().get(0).topicSequence;

        Formatter out = new Formatter(new StringBuilder(), Locale.US);
        Formatter forFile = new Formatter(new StringBuilder(), Locale.US);
        for (int position = 0; position < tokens.getLength(); position++) {
            out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
        }
        System.out.println(out);

        // Estimate the topic distribution of the first instance, 
        //  given the current Gibbs state.
        double[] topicDistribution = model.getTopicProbabilities(topics);

        // Get an array of sorted sets of word ID/count pairs
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
        //model.printTopicWordWeights(new File("out_word.txt"));

        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("output/15Sep_bi_tri_out.txt")));
        // Show top 5 words in topics with proportions for the first document
        for (int topic = 0; topic < numTopics; topic++) {
            Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

            out = new Formatter(new StringBuilder(), Locale.US);
            out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
            //System.out.println(out + " Arun");//@Arun
            int rank = 0;
            String s="";
            while (iterator.hasNext() && rank < 30) {
                IDSorter idCountPair = iterator.next();
                //@Arun - refining the ngram search
//                if(isUnique(idCountPair.getID(),dataAlphabet)) {
                    out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
                    rank++;
//                }
//                else
//                    continue;
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


        // Create a new instance with high probability of topic 0
        StringBuilder topicZeroText = new StringBuilder();
        Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

        int rank = 0;
        while (iterator.hasNext() && rank < 50) {
            IDSorter idCountPair = iterator.next();
            topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
            rank++;
        }

        // Create a new instance named "test instance" with empty target and source fields.
        InstanceList testing = new InstanceList(instances.getPipe());
        testing.addThruPipe(new Instance(topicZeroText.toString(), null, "test instance", null));

        TopicInferencer inferencer = model.getInferencer();
        double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
        System.out.println("0\t" + testProbabilities[0]);
    }

    private static boolean isUnique(int id,Alphabet dictionary) {
        int countUnderscore;
        String word = dictionary.lookupObject(id).toString();
        countUnderscore=word.split("_").length-1;
        String[] words = word.split("_");

        return false;
    }

}