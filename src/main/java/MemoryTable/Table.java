package MemoryTable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.io.*;
import java.util.*;

/**
 * Created by szymon on 07/07/2017.
 */


public class Table extends HashMap<String, Record> {
    public String file;
    public String[] attributes;
    public String[] keyAttributes;



    public Table(String[] attributes, String[] keys) {
        this.attributes = attributes;
        this.keyAttributes = keys;
    }

    public Table(String absFile, String[] attributes, String[] keys) {
        this.file = absFile;
        this.attributes = attributes;
        this.keyAttributes = keys;

        List<CSVRecord> records = csvReader(this.file);

        for (CSVRecord record :records) {
            Record myRecord = new Record();
            myRecord.putAll(record.toMap());
            this.post(myRecord);
        }
    }

    public Table(Model model, String[] attributes, String[] keys) {
        this.attributes = attributes;
        this.keyAttributes = keys;

        String sparqlQuery =
                "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>" +
                "SELECT ?id (GROUP_CONCAT(distinct str(?lab);separator='|') as ?labels) " +
                "WHERE {?id skos:prefLabel|skos:altLabel ?lab} GROUP BY ?id";

        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();

        while (results.hasNext()) {
            QuerySolution soln = results.next();
            Record myRecord = new Record();
            myRecord.put("id", soln.get("?id").asResource().getURI());
            myRecord.put("labels", soln.get("?labels").asLiteral().getString());
            this.post(myRecord);
        }
    }

    public List<Record> getRecords() {
        return new ArrayList(Arrays.asList(this.values().toArray()));
    }

    public String getId(Record record) {

        List<String> keyString = new ArrayList<>();
        for(int i = 0; i < keyAttributes.length; i++) keyString.add((String) record.get(keyAttributes[i]));
        return StringUtils.join(keyString, ";");
    }

    public Record getRecord(Record record) {

        String id = getId(record);

        if (this.containsKey(id)) return this.get(id);
            else return null;
    }

    public Boolean hasRecord(Record record) {

        String id = getId(record);

        if (this.containsKey(id)) return true;
        else return false;
    }

    public void post(Record record) {

        String id = getId(record);

        Record targetRecord = new Record();

        for(int i = 0; i < attributes.length; i++)
            targetRecord.put(attributes[i], String.valueOf(record.get(attributes[i])));

        this.put(id, targetRecord);
    }

    public void printRecord(Record record) {
        for(int i = 0; i < attributes.length; i++)
                System.out.println(attributes[i] + ": " + record.get(attributes[i]));
    }

    public void save() {

        File outputFile = new File(this.file);
        try {
            Writer outputWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");

            for (int i = 0; i < attributes.length; i++) {
                outputWriter.write(attributes[i]);
                if (i < attributes.length - 1) outputWriter.write(",");
                else outputWriter.write("\n");
            }

            for (String id : this.keySet()) {
                Record record = this.get(id);

                for (int i = 0; i < attributes.length; i++) {
                    outputWriter.write(String.valueOf(record.get(attributes[i])));
                    if (i < attributes.length - 1) outputWriter.write(",");
                    else outputWriter.write("\n");
                }
            }
            outputWriter.flush();
            outputWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        private static List<CSVRecord> csvReader(String fileName) {
        Reader in = null;
        try {
            in = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        }
        List<CSVRecord> records = null;
        try {
            records = CSVFormat.EXCEL.withHeader().parse(in).getRecords();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("There's a problem with parsing file: " + fileName);
        }
        return records;
    }
}
