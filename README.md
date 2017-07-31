<b>Matchmaker</b>
------

A tool for semi-automated label matching

The prototypical use-case scenario is matching labels extracted from text (bags of words from noun phrases) against the labels of SKOS concept from SKOS taxonomy in order to annotate the text with SKOS concepts and/or extend the SKOS-based knowledge graph with new concepts. 

Some components involved:
* Wordnet ontology (http://wordnet-rdf.princeton.edu/) and wordnet-based semantic similarity service (https://github.com/sklarman/wordnet-distance)
* WEKA machine learning library (http://www.cs.waikato.ac.nz/ml/weka/)
* Example data: sample of DBLP data (http://dblp.l3s.de/dblp++.php) with some noun phrases extracted using NLP GATE library (https://gate.ac.uk/) 

This work is HEAVILY UNDER CONSTRUCTION...
