import MemoryTable.Table;
import MemoryTable.Record;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;
import weka.core.*;
import weka.core.converters.ConverterUtils;

import java.io.*;
import java.util.*;



/**
 * Created by szymon on 11/07/2017.
 */
public class Matchmaker {

    public static Table wordMatchTable = new Table(
            "data/wordMatch.csv",
            new String[] {"source", "target", "length", "full", "prefix", "stem", "suffix", "match"},
            new String[] {"source", "target"});
    public static Table fullMatchTable = new Table(
            "data/fullMatch.csv",
                    new String[] {"source", "targetURI", "sourceLength", "targetLength",
                            "sourceMatch", "sourceAvrg", "sourceAvrgTop", "targetMatch", "targetAvrg", "targetAvrgTop",
                            "targetDepth", "globalAvrg", "neighborhood", "match"},
                    new String[] {"source", "targetURI"});
    public static Table w2wntTable = new Table(
            "data/w2wnt.csv",
            new String[] {"id", "labels"},
            new String[] {"id"});
    public static Table w2wMaxTable = new Table(
            "data/w2wmax.csv",
            new String[]{"source", "target", "score"},
            new String[]{"source", "target"});
    public static Table acmDepthTable = new Table(
            "data/target-depth.csv",
            new String[] {"id", "n"},
            new String[] {"id"});
    public static Table fullMatchSingleTable = new Table(
            "data/fullMatchSingleTable.csv",
            new String[] {"source", "sourceURI", "target", "targetURI", "sourceLength", "targetLength",
                    "sourceMatch", "sourceAvrg", "sourceAvrgTop", "targetMatch", "targetAvrg", "targetAvrgTop",
                    "targetDepth", "neighborhood", "globalAvrg", "match"},
            new String[] {"sourceURI", "targetURI"});
    public static Table forbiddenTable = new Table(
            "data/forbidden.csv",
            new String[] {"word"},
            new String[] {"word"});
    public static Table wordnetTable = new Table(
            "data/wordnet.csv",
            new String[] {"id", "labels"},
            new String[] {"id"});
    public static Set<String> sourceWords = new HashSet<>();
    public static Set<String> targetWords = new HashSet<>();
    public static Map<String, Integer> wordStats = new HashMap<>();
    public static Map<String, List<String>> sourceLabels = new HashMap<>();
    public static Map<String, List<String>> targetLabels = new HashMap<>();
    private static Set<String> forbiddenWords = new HashSet<>();
    public static Set<String> wordnetWords = new HashSet<>();
    private static String[] labelsAtts = {"id", "labels"};
    private static String[] labelsKeys = {"id"};
    private static String labelSplit = "\\|";
    private static String matchAttribute = "match";
    public static String wordMatchTrainingDataFileName = "data/wordMatch.arff";
    public static String fullMatchTrainingDataFileName = "data/fullMatch.arff";
    public static String sourceFileName = "data/source.csv";
    public static String targetFileName = "data/target.rdf";
    public static String outputMappingFileName = "data/mappings.n3";
    public static Classifier wordMatchClassifier;
    public static Classifier fullMatchClassifier;
    public static Model targetModel;


    public Matchmaker() {

        System.out.println("Initiating instance matcher. Please wait... ");

        Table sourceTable = new Table(sourceFileName, labelsAtts, labelsKeys);
        targetModel = RDFDataMgr.loadModel(targetFileName);
        Table targetTable = new Table(targetModel, labelsAtts, labelsKeys);

        for (Record record : forbiddenTable.getRecords()) forbiddenWords.add((String) record.get(forbiddenTable.attributes[0]));

        wordMatchClassifier = getMatchClassifier(wordMatchTable, wordMatchTrainingDataFileName);
        fullMatchClassifier = getMatchClassifier(fullMatchTable, fullMatchTrainingDataFileName);
        wordStats.put("_stat_all", 0);
        initWordLabelSets(wordnetTable, new HashMap<>(), wordnetWords);
        initWordLabelSets(sourceTable, sourceLabels, sourceWords);
        initWordLabelSets(targetTable, targetLabels, targetWords);
    }

    public static Classifier getMatchClassifier(Table matchTable, String matchTrainingDataFileName) {
        try {
            createMatchTrainingData(matchTable, matchTrainingDataFileName);

            ConverterUtils.DataSource trainDataSource = new ConverterUtils.DataSource(matchTrainingDataFileName);
            Instances trainDataSet = trainDataSource.getDataSet();
            trainDataSet.setClassIndex(trainDataSet.numAttributes() - 1);

           // RandomForest matchClassifier = new RandomForest();

            LMT matchClassifier = new LMT();

           // MultilayerPerceptron matchClassifier = new MultilayerPerceptron();

            matchClassifier.buildClassifier(trainDataSet);

            return matchClassifier;

        } catch (Exception e) {
            System.out.println("Failed to create match classifier.");
            return null;
        }
    }

    private static void createMatchTrainingData(Table matchTable, String labelledDataFileName) throws IOException {

        File labelledDataFile = new File(labelledDataFileName);

        Writer outputDataWriter = new OutputStreamWriter(new FileOutputStream(labelledDataFile), "UTF-8");

        outputDataWriter.write("@relation matcher\n\n");
        for (String attribute : matchTable.attributes) {
            if (!(new ArrayList(Arrays.asList(matchTable.keyAttributes)).contains(attribute)) & !attribute.equals(matchAttribute)) {
                outputDataWriter.write("@attribute " + attribute + " numeric\n");
            }
        }
        outputDataWriter.write("@attribute " + matchAttribute + " {true,false}\n\n@data\n");

        for (Record record : matchTable.getRecords()) {
            for (String attribute : matchTable.attributes) {
                if (!(new ArrayList(Arrays.asList(matchTable.keyAttributes)).contains(attribute)) & !attribute.equals(matchAttribute)) {
                    outputDataWriter.write(record.get(attribute) + ",");
                }
            }
            outputDataWriter.write((String) record.get(matchAttribute));
            outputDataWriter.write("\n");
        }

        outputDataWriter.flush();
        outputDataWriter.close();
    }

    private void initWordLabelSets(Table labelTable, Map<String, List<String>> labelMap, Set<String> wordSet) {
        for (Record record : labelTable.getRecords()) {
            String[] labels = ((String) record.get(labelsAtts[1])).split(labelSplit);
            String id = (String) record.get(labelsAtts[0]);
            Set<String> labelSet = new HashSet<>();
            for (String label : labels) {
                label = cleanLabel(label);
                labelSet.add(label);
                wordSet.addAll(new ArrayList(Arrays.asList(label.split(" "))));
                if (wordSet == sourceWords || wordSet == targetWords) includeStats(wordStats, label.split(" "));
            }
            List<String> labelList = new ArrayList(labelSet);
            Collections.sort(labelList);
            labelMap.put(id, labelList);
        }
    }

    private String cleanLabel(String label) {
        label = label.toLowerCase();
        label = label.replaceAll("[\"\\\\<>\n\t.,;':()\\[\\]/-]", " ");
        label = label.replace("\b", " ");

        String[] labelSplit = label.split(" ");

        //remove all the words that appear in the forbidden table...

        for (int i = 0; i < labelSplit.length; i++) {
            if (forbiddenWords.contains(labelSplit[i])) labelSplit[i] = "";
            if (labelSplit[i].length()<=1) labelSplit[i] = "";
        }
        label = String.join(" ", labelSplit);
        label = label.trim().replaceAll(" +", " ");

        return label;


    }

    private void includeStats(Map<String, Integer> wordStats, String[] wordArray) {

        wordStats.put("_stat_all", wordStats.get("_stat_all")+wordArray.length);

        for (int i=0; i<wordArray.length; i++) {

            Integer no = wordStats.get(wordArray[i]);
            if (no==null) wordStats.put(wordArray[i], 0); else wordStats.put(wordArray[i], no+1);
        }
    }

    public static String getClassifierPrediction(Classifier classifier, Table matchTable, Record record) {

        ArrayList<Attribute> atts = new ArrayList<>();

        for (String attribute : matchTable.attributes) {
            if (!(new ArrayList(Arrays.asList(matchTable.keyAttributes)).contains(attribute)) & !attribute.equals(matchAttribute)) {
                Attribute att = new Attribute(attribute);
                atts.add(att);
            }
        }

        List vals = new ArrayList(Arrays.asList(new String[]{"true", "false"}));
        Attribute match = new Attribute(matchAttribute, vals);
        atts.add(match);

        Instances testSet = new Instances("Rel", atts, 2);
        testSet.setClassIndex(atts.size() - 1);
        Instance inst = new DenseInstance(atts.size());

        for (Attribute attribute : atts)
            if (!attribute.name().equals(matchAttribute)) {
                inst.setValue(attribute, Double.valueOf(String.valueOf(record.get(attribute.name()))));
            }

        testSet.add(inst);

        try {
            double clsLabel = classifier.classifyInstance(testSet.instance(0));
            testSet.instance(0).setClassValue(clsLabel);

            return testSet.instance(0).stringValue(testSet.numAttributes() - 1);
        } catch (Exception e) {
            System.out.println("Exception in getClassifierPrediction for: ");
            matchTable.printRecord(record);
            return null;
        }
    }

    public static String getTablePrediction(Table matchTable, Record record) {

        Record output = matchTable.getRecord(record);
        try {
            if (output != null) return (String) output.get(matchAttribute);
            else return null;
        } catch (Exception e) {
            System.out.println("Exception in getTablePrediction for: ");
            matchTable.printRecord(record);
            matchTable.printRecord(output);
            return null;
        }
    }

    public static String getTotalPrediction(Classifier classifier, Table matchTable, Record record) {
        String tabPred = getTablePrediction(matchTable, record);
        if (tabPred!=null) return tabPred;

        if (classifier != null)
            return getClassifierPrediction(classifier, matchTable, record);

        return "false";
        }

    public static Record bestWordScorePrediction(String sourceWord, String targetWord) {

        Record outRecord = new Record();
        outRecord.put("source", sourceWord);
        outRecord.put("target", targetWord);

     //   if (w2wMaxTable.hasRecord(outRecord)) return w2wMaxTable.getRecord(outRecord);

        Record record = getWordMatchRecord(sourceWord, targetWord);

        String tabPred = getTablePrediction(wordMatchTable, record);
        if (tabPred != null) {
            if (tabPred.equals("true")) {
                outRecord.put("score", 1.5);
                w2wMaxTable.post(outRecord);
                return outRecord;
            } else {
                outRecord.put("score", 0.0);
                w2wMaxTable.post(outRecord);
                return outRecord;
            }
        }

        if (wordMatchClassifier != null) {
            String classPred = getClassifierPrediction(wordMatchClassifier, wordMatchTable, record);

            if (classPred.equals("true")) {
                outRecord.put("score", 1.2);
                w2wMaxTable.post(outRecord);
                return outRecord;
            }
        }

        Record srcCheckRec = new Record();
        srcCheckRec.put("id", sourceWord);
        Record trgCheckRec = new Record();
        trgCheckRec.put("id", targetWord);

        Record sourceRecord = w2wntTable.getRecord(srcCheckRec);
        Record targetRecord = w2wntTable.getRecord(trgCheckRec);

        if (sourceRecord!=null&targetRecord!=null) {
            String srcLabs = String.join(",", new HashSet<>(Arrays.asList(((String) sourceRecord.get("labels")).split("\\|"))));
            String targLabs = String.join(",", new HashSet<>(Arrays.asList(((String) targetRecord.get("labels")).split("\\|"))));

        //    if (wordnetWords.contains(sourceWord)) srcLabs = sourceWord;
          //  if (wordnetWords.contains(targetWord)) targLabs = targetWord;

            HttpResponse<JsonNode> jsonResponse = null;
            try {
                jsonResponse = Unirest.get("http://localhost:4567")
                        .header("accept", "application/json")
                        .queryString("word1", srcLabs)
                        .queryString("word2", targLabs)
                        .queryString("details", false)
                        .asJson();
            } catch (UnirestException e) {
            }

            if (jsonResponse != null) {
                Double score = Double.valueOf(jsonResponse.getBody().getObject().get("score").toString());
                outRecord.put("score", score / 2);
                w2wMaxTable.post(outRecord);
                return outRecord;
            }

        }

        outRecord.put("score", 0.0);
        w2wMaxTable.post(outRecord);
        return outRecord;

    }

    public static double editSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
/* // If you have StringUtils, you can use it to calculate the edit distance:
  return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
  (double) longerLength; */
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;

    }

    // Example implementation of the Levenshtein Edit Distance
// See http://r...content-available-to-author-only...e.org/wiki/Levenshtein_distance#Java
    public static int editDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    public static Record getWordMatchRecord(String sourceWord, String targetWord) {

        Record newRecord = new Record();

        String sourcePrefix;
        String sourceStem;
        String sourceSuffix;
        if (sourceWord.length()>4) {
            sourcePrefix = sourceWord.substring(0, 3);
            if (sourceWord.length()>6) sourceStem = sourceWord.substring(0, sourceWord.length()-3);
            else sourceStem = sourceWord.substring(0, sourceWord.length()-2);
            sourceSuffix = sourceWord.substring(sourceWord.length()-3);
        } else {
            sourcePrefix = sourceWord;
            sourceStem = sourceWord;
            sourceSuffix = sourceWord;
        }

        String targetPrefix;
        String targetStem;
        String targetSuffix;
        if (targetWord.length() > 4) {
            targetPrefix = targetWord.substring(0, 3);
            if (targetWord.length()>=sourceStem.length()) targetStem = targetWord.substring(0, sourceStem.length());
            else targetStem = targetWord;
            targetSuffix = targetWord.substring(targetWord.length() - 3);
        } else {
            targetPrefix = targetWord;
            targetStem = targetWord;
            targetSuffix = targetWord;
        }

        Double sourceLength = Double.valueOf(sourceWord.length());
        Double fullScore = editSimilarity(sourceWord, targetWord);
        Double prefixScore = editSimilarity(sourcePrefix, targetPrefix);
        Double stemScore = editSimilarity(sourceStem, targetStem);
        Double suffixScore = editSimilarity(sourceSuffix, targetSuffix);


        newRecord.put("source", sourceWord);
        newRecord.put("target", targetWord);
        newRecord.put("length", sourceLength);
        newRecord.put("full", fullScore);
        newRecord.put("prefix", prefixScore);
        newRecord.put("stem", stemScore);
        newRecord.put("suffix", suffixScore);

        return newRecord;
    }

    public static List<Record> getFullMatchRecord(String sourceId) {

        for (String targetId : targetLabels.keySet()) {
            Record record = getFullMatchRecordSingle(sourceId, targetId);
            fullMatchSingleTable.post(record);
        }

        for (int i = 0; i < 4; i++) neighborPropagation(sourceId);




        System.out.println();

        Map<Double, Set<String>> matches = new HashMap<>();

        for (String targetId : targetLabels.keySet()) {
            Record record = new Record();
            record.put("sourceURI", sourceId);
            record.put("targetURI", targetId);
            record = fullMatchSingleTable.getRecord(record);

            String matchValue = getTotalPrediction(fullMatchClassifier, fullMatchTable, record);
            record.put("match", matchValue);

            Double score = Double.valueOf(String.valueOf(record.get("neighborhood")));
            Set<String> targets = matches.get(score);

            if (targets == null) {
                targets = new HashSet<>();
                matches.put(score, targets);
            }

            targets.add(String.valueOf(record.get("targetURI")));
        }

        List<Double> finds = new ArrayList(Arrays.asList(matches.keySet().toArray()));
        Collections.sort(finds, Collections.reverseOrder());

        List<Record> output = new ArrayList<Record>();

        for (Double hit : finds) {
            Record record = new Record();
            record.put("sourceURI", sourceId);
            Set<String> targetIds = matches.get(hit);
            for (String targetId : targetIds) {
                record.put("targetURI", targetId);
                record = fullMatchSingleTable.getRecord(record);
                output.add(record);
            }
        }

        return output;
    }

    public static Record getFullMatchRecordSingle(String sourceId, String targetId) {


        List<String> sourceLabSet = sourceLabels.get(sourceId);
        List<String> targetLabSet = targetLabels.get(targetId);

        String[] sourceLabs = sourceLabSet.toArray(new String[sourceLabSet.size()]);
        String[] targetLabs = targetLabSet.toArray(new String[targetLabSet.size()]);

        SimilarityMatrix[][] fullMatrix = new SimilarityMatrix[sourceLabs.length][targetLabs.length];

        Double maxColAvrg = 0.0;
        Double maxRowAvrg = 0.0;

        Double rowAvrgGlob = 0.0;
        Double colAvrgGlob = 0.0;

        List<Double> topRowAvrgGlobal = new ArrayList<>();
        List<Double> topColAvrgGlobal = new ArrayList<>();

        for (int x=0; x < sourceLabs.length; x++)
            for (int y=0; y < targetLabs.length; y++) {

                fullMatrix[x][y] = new SimilarityMatrix(sourceLabs[x], targetLabs[y]);
                if (fullMatrix[x][y].rowAvrg>maxRowAvrg) maxRowAvrg = fullMatrix[x][y].rowAvrg;
                if (fullMatrix[x][y].colAvrg>maxColAvrg) maxColAvrg = fullMatrix[x][y].colAvrg;
                rowAvrgGlob = rowAvrgGlob + fullMatrix[x][y].rowAvrg;
                topRowAvrgGlobal.add(fullMatrix[x][y].rowAvrg);
                colAvrgGlob = colAvrgGlob + fullMatrix[x][y].colAvrg;
                topColAvrgGlobal.add(fullMatrix[x][y].colAvrg);
            }

        rowAvrgGlob = rowAvrgGlob / (sourceLabs.length * targetLabs.length);
        colAvrgGlob = colAvrgGlob / (sourceLabs.length * targetLabs.length);
        Double globAvrg = rowAvrgGlob * colAvrgGlob;

        Collections.sort(topRowAvrgGlobal,Collections.reverseOrder());
        int topRows = (int) Math.ceil(Double.valueOf(topRowAvrgGlobal.size()) / 2);
        Double topRowSum = 0.0;
        for (int i=0; i<topRows; i++) {
            topRowSum = topRowSum + topRowAvrgGlobal.get(i);
        }
        topRowSum = topRowSum / topRows;


        Collections.sort(topColAvrgGlobal,Collections.reverseOrder());
        int topCols = (int) Math.ceil(Double.valueOf(topColAvrgGlobal.size()) / 2);
        Double topColSum = 0.0;
        for (int i=0; i<topCols; i++) {
            topColSum = topColSum + topColAvrgGlobal.get(i);
        }
        topColSum = topColSum / topCols;

        Double sourceLengthSum = 0.0;
        for (int x=0; x < sourceLabs.length; x++) {
            sourceLengthSum = sourceLengthSum + fullMatrix[x][0].sourceLength;
        }

        Double targetLengthSum = 0.0;
        for (int y=0; y < targetLabs.length; y++) {
            targetLengthSum = targetLengthSum + fullMatrix[0][y].targetLength;
        }

        Record depthRec = new Record();
        depthRec.put("id", targetId);
        Double targetDepth = Double.valueOf(String.valueOf(acmDepthTable.getRecord(depthRec).get("n")));

        Double sourceLengthAvrg = sourceLengthSum / sourceLabs.length;
        Double targetLengthAvrg = targetLengthSum / targetLabs.length;

        Record record = new Record();

        Double scoreSum =  (topColSum + topRowSum + globAvrg)/3;

        record.put("source", sourceLabels.get(sourceId).toString());
        record.put("sourceURI", sourceId);
        record.put("target", targetLabels.get(targetId).toString());
        record.put("targetURI", targetId);
        record.put("sourceLength", sourceLengthAvrg);
        record.put("targetLength", targetLengthAvrg);
        record.put("sourceMatch", maxColAvrg);
        record.put("sourceAvrg", colAvrgGlob);
        record.put("sourceAvrgTop", topColSum);
        record.put("targetMatch", maxRowAvrg);
        record.put("targetAvrg", rowAvrgGlob);
        record.put("targetDepth", targetDepth);
        record.put("targetAvrgTop", topRowSum);
        record.put("globalAvrg", globAvrg);
        record.put("neighborhood", scoreSum);

        String matchValue = getTotalPrediction(fullMatchClassifier, fullMatchTable, record);
        record.put("match", matchValue);

        return record;
    }

    public static List<String> getNeighbors(String nodeId) {

        String queryString =
                "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>" +
                        "SELECT distinct ?neighbor " +
                        "WHERE {" +
                        "<%s> skos:broader|skos:narrower ?neighbor . }";

        Query uriQuery = QueryFactory.create(String.format(queryString, nodeId));
        QueryExecution qexec = QueryExecutionFactory.create(uriQuery, targetModel);
        ResultSet results = qexec.execSelect();
        List<String> output = new ArrayList<>();
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            if (soln.get("?neighbor")!=null)
                output.add(soln.get("?neighbor").toString());
        }
        return output;
    }

    public static int countNeighbors(String nodeId) {

        String queryString =
                "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>" +
                        "SELECT (count (distinct ?neighbor) as ?count) " +
                        "WHERE {" +
                        "<%s> skos:broader|skos:narrower ?neighbor . }";

        Query uriQuery = QueryFactory.create(String.format(queryString, nodeId));
        QueryExecution qexec = QueryExecutionFactory.create(uriQuery, targetModel);
        ResultSet results = qexec.execSelect();
        if (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            if (soln.get("?count")!=null)
                return soln.get("?count").asLiteral().getInt();
        }
        return 0;
    }

    private static void neighborPropagation(String sourceId) {

        Map<Record, Double> newValues = new HashMap<Record, Double>();
        for (String targetId : targetLabels.keySet()) {
            Record record = new Record();
            record.put("sourceURI", sourceId);
            record.put("targetURI", targetId);
            record = fullMatchSingleTable.getRecord(record);
            Double orgScore = Double.valueOf(String.valueOf(record.get("neighborhood")));
            int count = countNeighbors(targetId);
            List<String> neighbors = getNeighbors(targetId);

            Double finalScore = orgScore;
            Double factorSelf = 0.05;
            if (String.valueOf(record.get("match")).equals("true")) factorSelf = 0.2;
            finalScore = finalScore + (factorSelf * orgScore / (count + 1 ));
            for (String neighborId : neighbors) {
                Record neighbor = new Record();
                neighbor.put("sourceURI", sourceId);
                neighbor.put("targetURI", neighborId);
                neighbor = fullMatchSingleTable.getRecord(neighbor);
                Double factor = 0.05;
                if (String.valueOf(neighbor.get("match")).equals("true")) factor = 0.2;
                finalScore = finalScore + (factor * Double.valueOf(String.valueOf(neighbor.get("neighborhood"))) / (count + 1 ));
            }
            newValues.put(record, finalScore);
        }
        newValues.keySet().forEach(record -> record.put("neighborhood", newValues.get(record)));
    }

}