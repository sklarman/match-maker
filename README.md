<b>Matchmaker</b>
------

A tool for semi-automated label matching. 

The prototypical use-case scenario is matching labels extracted from text (noun phrases represented as bags of words) against the labels of SKOS concept from SKOS taxonomy in order to annotate the text with SKOS concepts and/or extend the SKOS-based knowledge graph with new concepts. 

Key components involved:
* Wordnet ontology (http://wordnet-rdf.princeton.edu/) and wordnet-based semantic similarity service (https://github.com/sklarman/wordnet-distance)
* WEKA machine learning library (http://www.cs.waikato.ac.nz/ml/weka/)

Example data includes: 
* noun phrases extracted using NLP GATE library (https://gate.ac.uk/) extracted from a sample of DBLP data (http://dblp.l3s.de/dblp++.php)
* an ACM SKOS taxonomy (https://www.acm.org/publications/class-2012)

This work is HEAVILY UNDER CONSTRUCTION...
