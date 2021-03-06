# LUPOSDATE Semantic Web Database Management System

## Module distributedendpoints

This module contains the code for the distributed data storage and querying based on SPARQL endpoints.

The module contains two different main types of distributed storage and distributed querying based on SPARQL endpoints.

1) All registered endpoints are asked for the evaluation of the triple patterns within a SPARQL query.
It is assumed that the data is not distributed in an intelligent way and that any registered endpoint
can have data for any triple pattern.
Also non-luposdate SPARQL endpoints are supported.
It uses the super and helper classes of the distributed module for a first and simple example of a distributed scenario.

2) The data is distributed according to a specific distribution strategy (one key, two keys or one to three keys).
The distributed querying is optimized concerning the data distribution strategy.

First run lupos.distributedendpoints.endpoint.ClearIndex to create (empty) indices for initialization purposes.
Start the endpoints on the different nodes by running lupos.distributedendpoints.endpoint.StartEndpoint.

Edit distributedendpoints/src/main/resources/endpoints.config: 
For registering a new endpoint, type in its url in a new line of the file.

Run lupos.gui.Start_Demo_Applet for starting the Demo_Applet 
in which evaluators with different distribution strategies can be
chosen and queries can be processed. When evaluating a query, you are asked
whether histograms for query optimization are asked for at the endpoints or
a static query optimization is used.