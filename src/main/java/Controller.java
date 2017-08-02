import MemoryTable.Table;
import MemoryTable.Record;
import weka.classifiers.Classifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

/**
 * Created by Szymon on 17/07/2017.
 */
public class Controller {

    private static Matchmaker matchmaker = new Matchmaker();


    public static void main(String[] args) {

    //   (new SimilarityMatrix("logic programming artificial intelligence reasoning", "constraint logic programming")).print();
    //    (new SimilarityMatrix("logic programming artificial intelligence reasoning", "automated reasoning")).print();
    //    (new SimilarityMatrix("inductive logic programming", "statistical relational learning")).print();

        String str;

        String menu = "" +
                "\n\n" +
                "(1) Train word matchmaker.\n" +
                "(2) Reset word matchmaker.\n" +
                "(3) Generate top word mappings.\n" +
                "(4) Train label matchmaker.\n" +
                "(5) Reset label matchmaker.\n" +
                "(6) Generate mappings.\n" +
                "(x) Exit. \n\n";

        do {
            System.out.println(menu);

            Scanner s = new Scanner(System.in);
            str = s.nextLine();

            if (str.equals("1")) {
                System.out.println("\nTraining word matchmaker...\n");
                trainWordMatcher();
            }

            if (str.equals("2")) {
                System.out.println("\nNot yet buddy...\n");
            }

            if (str.equals("3")) {
                System.out.println("\n\nRecording best word mappings and such...\n");
                mappingTrain();
            }

            if (str.equals("4")) {
                System.out.println("\n\nTraining full matchmaker...\n");
                trainFullMatcher();
            }

            if (str.equals("5")) {
                System.out.println("\nNot yet buddy...\n");
            }

            if (str.equals("6")) {
                System.out.println("\n\nGenerating mappings...\n");
                generateMappings();
            }

            if (str.equals("x")) {
                System.out.println("\nDoei!\n");
            }

        } while (!str.equals("x"));

    }

    public static void generateMappings() {

        File outputFile = new File(matchmaker.outputMappingFileName);
        try {
            Writer outputWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");

            Set<String> from = matchmaker.sourceLabels.keySet();

            for (String sourceId : from) {
                System.out.println();
                System.out.println(sourceId);
                System.out.println(matchmaker.sourceLabels.get(sourceId).toString());
                System.out.println();

                List<Record> outputList = matchmaker.getFullMatchRecord(sourceId);

                for (Record record : outputList) if (record.get("match").equals("true")) {
                    System.out.println("\t\t" + record.get("targetURI"));
                    System.out.println("\t\t" + record.get("target"));
                    outputWriter.write("<" + sourceId + "> <http://klarman.me/labelmatcher/relatedMatch> <" + record.get("targetURI") + "> .\n");
                }
            }

            outputWriter.flush();
            outputWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
}


    public static void trainWordMatcher() {

        Map<String, Integer> counter = new HashMap<>();
        counter.put("all", 0);
        counter.put("pos", 0);
        counter.put("neg", 0);

        int skip = 7000;

        Set<String> from = new HashSet<>();
        Set<String> to = new HashSet<>();

        from.addAll(matchmaker.sourceWords);
        from.addAll(matchmaker.targetWords);
        to.addAll(matchmaker.targetWords);
        to.addAll(matchmaker.wordnetWords);


        outerloop:
        for (String sourceWord : from) {
            for (String targetWord : to) {
                Record newRecord = matchmaker.getWordMatchRecord(sourceWord, targetWord);

                if (!matchmaker.wordMatchTable.hasRecord(newRecord)) {

                    Double test = (Double) newRecord.get("full");
                    Boolean next = Controller.trainControlLoop(newRecord, (test < 0.65), (test == 1),
                            counter, skip, matchmaker.wordMatchTable, matchmaker.wordMatchClassifier);

                    if (!next) break outerloop;
                }
            }
        }

        matchmaker.wordMatchClassifier = matchmaker.getMatchClassifier(matchmaker.wordMatchTable, matchmaker.wordMatchTrainingDataFileName);

    }

    public static void trainFullMatcher() {

        Map<String, Integer> counter = new HashMap<>();
        counter.put("all", 0);
        counter.put("pos", 0);
        counter.put("neg", 0);

        int skip = 10000;

        Set<String> from = matchmaker.sourceLabels.keySet();

        for (String sourceId : from) {

            System.out.println();
            System.out.println(sourceId);
            System.out.println(matchmaker.sourceLabels.get(sourceId).toString());
            System.out.println();

            List<Record> outputList = matchmaker.getFullMatchRecord(sourceId);

            outerloop1:

            for (Record rec : outputList) {


                if (matchmaker.getTablePrediction(matchmaker.fullMatchTable, rec)==null) {

                   Boolean next = Controller.trainControlLoop(rec, (false), (false),
                           counter, skip, matchmaker.fullMatchTable, matchmaker.fullMatchClassifier);

                   if (next == null) return;
                   if (next == false) break outerloop1;

               }
            }

            matchmaker.w2wMaxTable.save();
            matchmaker.fullMatchClassifier = matchmaker.getMatchClassifier(matchmaker.fullMatchTable, matchmaker.fullMatchTrainingDataFileName);

            outputList = matchmaker.getFullMatchRecord(sourceId);

            outerloop2:

            for (Record rec : outputList) {


                if (rec.get("match").equals("true")& matchmaker.getTablePrediction(matchmaker.fullMatchTable, rec)==null) {

                    Boolean next = Controller.trainControlLoop(rec, (false), (false),
                            counter, skip, matchmaker.fullMatchTable, matchmaker.fullMatchClassifier);

                    if (next == null) return;
                    if (next == false) break outerloop2;

                }
            }

        }

    }

    public static Boolean trainControlLoop(Record newRecord, Boolean falseCondition, Boolean trueCondition,
                                           Map<String, Integer> counter, int skip, Table matchTable,
                                           Classifier matchClassifier) {


        counter.put("all", counter.get("all")+1);

        String tabPred = matchmaker.getTablePrediction(matchTable, newRecord);

        if (!falseCondition & !trueCondition) {

                System.out.println(newRecord.get("source"));
                System.out.println(newRecord.get("target"));

                matchTable.printRecord(newRecord);

            String classPred = "classifier not trained yet";
            if (matchClassifier != null) classPred = matchmaker.getClassifierPrediction(matchClassifier, matchTable, newRecord);

            System.out.println("\nClassifier: " + classPred + ";\tTable: " + tabPred);

            Scanner s = new Scanner(System.in);
            String str = s.nextLine();

            if (str.equals("y")) {
                newRecord.put("match", true);
                matchTable.post(newRecord);
                counter.put("pos", counter.get("pos")+1);
            }
            if (str.equals("n")) {
                newRecord.put("match", false);
                matchTable.post(newRecord);
                counter.put("neg", counter.get("neg")+1);
            }
            if (str.equals("q")) {
                matchTable.save();
                return false;
            }
            if (str.equals("a")) {
                return false;
            }
            if (str.equals("z")) {
                matchmaker.w2wMaxTable.save();
                return null;
            }
            if (str.equals("s")) {
                matchTable.save();
            }
            System.out.println("(all=" + counter.get("all") + "; table=" + matchTable.keySet().size() + "; pos=" + counter.get("pos") + "; neg=" + counter.get("neg") + ")\n");
            if (!str.equals("y") & !str.equals("n")) System.out.println("ignore");
        } else {
            if (trueCondition) {
                newRecord.put("match", true);
                matchTable.post(newRecord);
            }
            if (falseCondition & counter.get("all") % skip == 0) {
                newRecord.put("match", false);
                matchTable.post(newRecord);
            }

        }

        return true;
    }

    public static void mappingTrain() {


        int i=0;

        Set<String> words = new HashSet<>();

        words.addAll(matchmaker.sourceWords);
        words.addAll(matchmaker.targetWords);


        System.out.println(words.size());


        for (String word: words) {
            System.out.println(i++);
            Set<String> bag = new HashSet<>();
            for (String wntword : matchmaker.wordnetWords) {
                Record record = matchmaker.getWordMatchRecord(word, wntword);
                if (Double.valueOf(String.valueOf(record.get("stem")))==1.0) {
                    String tabPred = matchmaker.getTablePrediction(matchmaker.wordMatchTable, record);
                    if (tabPred != null) if (tabPred.equals("true")) bag.add(wntword);

                    if (matchmaker.wordMatchClassifier != null) {
                        String classPred = matchmaker.getClassifierPrediction(matchmaker.wordMatchClassifier, matchmaker.wordMatchTable, record);

                        if (classPred.equals("true")) bag.add(wntword);


                    }
                }
            }
            if (bag.size()>0) {
                Record record = new Record();
                record.put("id", word);
                record.put("labels", String.join("|", bag));
                matchmaker.w2wntTable.post(record);
            }
            System.out.println(word + " - " + bag.toString());
        }

        matchmaker.w2wntTable.save();
    }

}
