<b>Matchmaker</b>
------

A prototype of a tool for semi-automated (interactive) label matching. 

The intended use-case scenario is focused on the task of matching labels extracted from text (noun phrases represented as bags of words) against the most relevant and semantically related labels of SKOS concepts from a given SKOS taxonomy, in order to annotate the text with SKOS concepts and/or extend the SKOS-based knowledge graph with new concepts originating in unstructured data. 

Key components involved:
* WordNet vocabulary (http://wordnet-rdf.princeton.edu/) and WordNet-based semantic similarity service (https://github.com/sklarman/wordnet-distance)
* WEKA machine learning library (http://www.cs.waikato.ac.nz/ml/weka/)

Example data includes: 
* noun phrases extracted from a sample of DBLP data (http://dblp.l3s.de/dblp++.php) using NLP GATE library (https://gate.ac.uk/)
* an ACM SKOS taxonomy (https://www.acm.org/publications/class-2012)

This work is <b>HEAVILY UNDER CONSTRUCTION!</b>



A sample workflow:
-----
1) (supervised) training of a word matching classifier to account for inflectional variants of concepts and minor typos occurring in the source and target labels, e.g.:

<p align="center">
<b>logic - logics (true) </b> 
</p>
<p align="center">
<b>logic - logica (true)</b> 
</p>
<p align="center">
<b>logic - logically (true)</b> 
</p>
<p align="center">
<b>logic - login (false) </b> 
</p>

2) applying the classifier for generating mappings from the words in the source and target labels to WordNet vocabulary;

<p align="center"><b> intelligence -intelligentsia|intelligently|intelligent|intelligence</b></p>

3) generating bags of words out of noun phrases extracted from labels (here conference names and SKOS labels) e.g.:

<p align="center"><b>logic programming artificial intelligence reasoning</b></p>

<i>Logic for Programming, Artificial Intelligence, and Reasoning - 19th International Conference, LPAR-19, Stellenbosch, South Africa, December 14-19, 2013. Proceedings</i>, http://dblp.l3s.de/d2r/page/publications/conf/lpar/2013

<p align="center"><b>automated reasoning</b></p>

<i>Automated reasoning (ACM:10003794)</i>

4) generating similarity matrices between source and target bags of words, e.g.:

```
+------------+--------------------+--------------------+--------------------+
|(NULL)      |automated           |reasoning           |(NULL)              |
+------------+--------------------+--------------------+--------------------+
|logic       |0.013245033112582781|0.043010752688172046|0.043010752688172046|
+------------+--------------------+--------------------+--------------------+
|programming |0.19444444444444445 |0.04395604395604396 |0.19444444444444445 |
+------------+--------------------+--------------------+--------------------+
|artificial  |0.013513513513513514|0.015625            |0.015625            |
+------------+--------------------+--------------------+--------------------+
|intelligence|0.053763440860215055|0.5                 |0.5                 |
+------------+--------------------+--------------------+--------------------+
|reasoning   |0.013333333333333334|1.0                 |1.0                 |
+------------+--------------------+--------------------+--------------------+
|(NULL)      |0.19444444444444445 |1.0                 |null                |
+------------+--------------------+--------------------+--------------------+

```

5) Propagating the matching score information to the neighborhood SKOS concepts.

6) Training a label matching classifier using users accept-reject responses to subsequently proposed matches.

7) Generating mappings by means of the classifier 
