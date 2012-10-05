/**
 * Copyright (c) 2012, Institute of Information Systems (Sven Groppe), University of Luebeck
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 	- Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * 	  disclaimer.
 * 	- Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * 	  following disclaimer in the documentation and/or other materials provided with the distribution.
 * 	- Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 * 	  products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package lupos.gui.operatorgraph.arrange;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import lupos.gui.operatorgraph.GraphBox;
import lupos.gui.operatorgraph.GraphWrapperIDTuple;
import lupos.gui.operatorgraph.OperatorGraph;
import lupos.gui.operatorgraph.graphwrapper.GraphWrapper;

/**
 * The class EdgeBased implements a simple edge based Layout algorithm
 * which is used in the LUPOSDATE- Project 
 * 
 * @author Tobias Bielfeld
 *
 */

public class EdgeBased {
	
	private static final int EDGE_LENGTH =  250;
	private static final double AGAINST_OVERLAPPING = 10.0;
		
	/**
	 * Method gets the amount of different edge-types in 
	 * the graph.
	 * @param op	the graph	
	 * @return		edge-type-amount
	 */
	private static int getTypeCount(OperatorGraph op) {
		int count = 0;		
		HashMap<GraphWrapper, GraphBox> boxes = op.getBoxes();
		
		for(GraphWrapper gw : boxes.keySet()) {
			if (gw.getSucceedingElements().size() > count)
				count = gw.getSucceedingElements().size();
		}
		
		return count;
	}
	
	/**
	 * Method sets the edge-directions for the different
	 * edge-types in the graph.
	 * @param op	the graph
	 * @param directions	already set directions
	 */
	private static void setDirections(final OperatorGraph op, final HashMap<Integer, Double> directions) {
		int count = getTypeCount(op);
		if(count>0){
			double directionDistance = 180/count;		
			double direction = 0.0;
			for (int type = 0; type < count; type++) {
				direction = ((type * directionDistance)*(Math.PI/180));
				directions.put(type, direction);
			}
		}
	}

	/**
	 * Method gets all edges which are connected with the node
	 * @param node	
	 * @return	List with all connected edges
	 */
	private static LinkedList<Edge> getEdges(GraphWrapper node){
		LinkedList<Edge> edges = new LinkedList<Edge>();
		LinkedList<GraphWrapperIDTuple> successors = node.getSucceedingElements();		
		int type = 0; 
		
		if (successors.size() == 0) return edges;
			
		for (GraphWrapperIDTuple suc : successors) {
			Edge edge = new Edge(node, suc.getOperator(), type);
			edges.add(edge);
			type++;
		}		
		return edges;
	}
	
	/**
	 * Method test if the node is overlapping or hiding another node
	 * @param node	the node
	 * @param op	the graph
	 * @return
	 */
	private static boolean isHidingANode(GraphWrapper node, OperatorGraph op) {
		HashMap<GraphWrapper, GraphBox> boxes = op.getBoxes();
		GraphBox nodeBox = boxes.get(node);
		int nodeX = nodeBox.getX();
		int nodeY = nodeBox.getY();
		int nodeXArea = nodeX+nodeBox.width;
		int nodeYArea = nodeY+nodeBox.height;
		for (GraphBox hideBox : boxes.values()) {
			int hideX = hideBox.getX();
			int hideY = hideBox.getY();
			int hideXArea = hideX+hideBox.width;
			int hideYArea = hideY+hideBox.height;
			
			if((nodeX > hideXArea) || (nodeY > hideYArea)) {
				return false;
			}
			
			if ((hideX > nodeXArea) || (hideY > nodeYArea)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Method give nodes an ID to identify to which hierarchy
	 * they belong
	 * @param op 	the graph
	 * @param idHierarchy 	hierarchies
	 */
	private static void fillIDs(final OperatorGraph op, final HashMap<GraphWrapper, Integer> idHierarchy){
		HashMap <GraphWrapper, GraphBox> boxes = op.getBoxes();
		LinkedList <GraphWrapper> roots = op.getRootList(false);
		
		for (GraphWrapper root : roots) {
			idHierarchy.put(root, -1);
		}
		
		int i = 0;
		for(GraphWrapper gw : boxes.keySet()){
			
			LinkedList<GraphWrapperIDTuple> successors = gw.getSucceedingElements();
			for (GraphWrapperIDTuple suc : successors) {
				if (!idHierarchy.containsKey(suc.getOperator()))
					idHierarchy.put(suc.getOperator(), i);
			}
			i++;
		}
	}
	
	/**
	 * Method recursively computes and sets the new coordinates for all nodes
	 * in the graph with a depth-search.  
	 * @param node		the node
	 * @param op		the graph
	 * @param isSet		HashMap with already visited nodes
	 * @param directions		HashMap with already set directions
	 * @param idHierarchies		hierarchies
	 */
	public static void setCoordinates(final GraphWrapper node, final OperatorGraph op, final HashSet<GraphWrapper> isSet, final HashMap<Integer, Double> directions, final HashMap<GraphWrapper, Integer> idHierarchy) {
		HashMap <GraphWrapper, GraphBox> boxes = op.getBoxes();
		
		GraphBox box = boxes.get(node);
		
		LinkedList<Edge>suc = getEdges(node);		
		
		for (Edge e : suc) {
			if (isSet.contains(e.getTarget())) {
				return;
			}
			int type = e.getEdgeType();
			double direction = directions.get(type);
			GraphBox sucBox = boxes.get(e.getTarget());
			
			int x = (int)Math.ceil(box.getX()+((op.getZoomFactor()*EDGE_LENGTH)*Math.cos(direction)));
			int y = (int)Math.ceil(box.getY()+((op.getZoomFactor()*EDGE_LENGTH)*Math.sin(direction)));
			
			if (!isSet.contains(e.getTarget()) && (idHierarchy.get(e.getTarget()) > idHierarchy.get(node))) {
				sucBox.setXWithoutUpdatingParentsSize(x);
				sucBox.setYWithoutUpdatingParentsSize(y);
				isSet.add(e.getTarget());
			}
			
			while (isHidingANode(e.getTarget(), op)){
				direction += ((Math.PI/180)*AGAINST_OVERLAPPING);
				x = (int)Math.ceil(box.getX()+((op.getZoomFactor()*EDGE_LENGTH)*Math.cos(direction)));
				y = (int)Math.ceil(box.getY()+((op.getZoomFactor()*EDGE_LENGTH)*Math.sin(direction)));
				sucBox.setXWithoutUpdatingParentsSize(x);
				sucBox.setYWithoutUpdatingParentsSize(y);
			}
			setCoordinates(e.getTarget(),op, isSet, directions, idHierarchy);
		}
		
	}
	
	public static void arrange(final OperatorGraph operatorgraph) {
		//long time1 = System.currentTimeMillis();
		final HashMap <GraphWrapper, GraphBox> boxes = operatorgraph.getBoxes();
		final HashSet<GraphWrapper> isSet = new HashSet<GraphWrapper>();
		final LinkedList <GraphWrapper> roots = operatorgraph.getRootList(false);
		final HashMap<Integer, Double> directions = new HashMap<Integer, Double>();
		final HashMap<GraphWrapper, Integer> idHierarchy = new HashMap <GraphWrapper, Integer>();
		setDirections(operatorgraph, directions);
		fillIDs(operatorgraph, idHierarchy);
		
		GraphWrapper root1 = roots.getFirst();
		
		GraphBox box1 = boxes.get(root1);
		box1.setXWithoutUpdatingParentsSize((int)Math.ceil(operatorgraph.getWidth()/roots.size()+1));
		box1.setYWithoutUpdatingParentsSize((int)Math.ceil(operatorgraph.PADDING));

		isSet.add(root1);
		setCoordinates(root1, operatorgraph, isSet, directions, idHierarchy);
		int x = 0; int width = 0;
		for (int i = 1; i < roots.size(); i++) {
			for (GraphBox box : boxes.values()) {
				if (box.getY() == box1.getY()) {
					if (box.getX() > x) {
						x = box.getX(); width = box.width;
					}
				}
			}
			GraphWrapper root = roots.get(i);
			GraphBox rootBox = boxes.get(root);
			int x_Coord = (int)Math.ceil(x + width + operatorgraph.SPACING_X);
			rootBox.setXWithoutUpdatingParentsSize(x_Coord);
			rootBox.setYWithoutUpdatingParentsSize(box1.getY());
			setCoordinates(root, operatorgraph, isSet, directions, idHierarchy);
		}		
		//time1 = System.currentTimeMillis() - time1;
		//System.out.println("Laufzeit = " + time1 + " ms");
	}
}
