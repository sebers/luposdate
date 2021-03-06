/**
 * Copyright (c) 2013, Institute of Information Systems (Sven Groppe and contributors of LUPOSDATE), University of Luebeck
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
package lupos.engine.operators.multiinput.minus;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import lupos.datastructures.bindings.Bindings;
import lupos.datastructures.items.Variable;
import lupos.datastructures.queryresult.ParallelIteratorMultipleQueryResults;
import lupos.datastructures.queryresult.QueryResult;
import lupos.datastructures.queryresult.QueryResultDebug;
import lupos.engine.operators.BasicOperator;
import lupos.engine.operators.OperatorIDTuple;
import lupos.engine.operators.messages.BoundVariablesMessage;
import lupos.engine.operators.messages.EndOfEvaluationMessage;
import lupos.engine.operators.messages.Message;
import lupos.engine.operators.multiinput.MultiInputOperator;
import lupos.misc.debug.DebugStep;

public class Minus extends MultiInputOperator {

	protected ParallelIteratorMultipleQueryResults[] operands = {	new ParallelIteratorMultipleQueryResults(),
																	new ParallelIteratorMultipleQueryResults()};

	protected final boolean considerEmptyIntersection;
	
	public Minus(){
		this(true);
	}
	
	public Minus(final boolean considerEmptyIntersection){
		this.considerEmptyIntersection = considerEmptyIntersection;
	}
	
	@Override
	public synchronized QueryResult process(final QueryResult queryResult, final int operandID) {
		// wait for all query results and process them when
		// EndOfEvaluationMessage arrives
		this.operands[operandID].addQueryResult(queryResult);
		return null;
	}

	@Override
	public Message preProcessMessage(final EndOfEvaluationMessage msg) {
		if (!this.operands[0].isEmpty() && !this.operands[1].isEmpty()) {
			QueryResult result = QueryResult.createInstance();
			Iterator<Bindings> iteratorLeftChild = this.operands[0].getQueryResult().oneTimeIterator();
			while (iteratorLeftChild.hasNext()) {
				Bindings leftItem = iteratorLeftChild.next();
				boolean found = false;
				for(Bindings rightItem : this.operands[1].getQueryResult()) {
					// compute intersection of the variable sets
					Set<Variable> vars = rightItem.getVariableSet();
					vars.retainAll(leftItem.getVariableSet());
					
					// if intersection is empty then isEqual should always be false in the typical case (except for not in RIF rules),
					// workaround: check whether vars is empty
					if(vars.isEmpty() && this.considerEmptyIntersection){
						continue;
					}

					boolean isEqual = true;
					for (Variable v : vars) {
						if ((v.getLiteral(leftItem).compareToNotNecessarilySPARQLSpecificationConform(v.getLiteral(rightItem))) != 0) {
							isEqual = false;
						}
					}

					if (isEqual){
						found = true;
						break;
					}
				}
				if (!found){
					result.add(leftItem);
				}
			}

			for (final OperatorIDTuple opId : this.succeedingOperators) {
				opId.processAll(result);
			}
		} else if (!this.operands[0].isEmpty()) { // happens when the group constraint which follows the minus is empty
			for (final OperatorIDTuple opId : this.succeedingOperators) {
				opId.processAll(this.operands[0].getQueryResult());
			}
		}
		return msg;
	}
	
	@Override
	public Message preProcessMessageDebug(final EndOfEvaluationMessage msg, final DebugStep debugstep) {
		if (!this.operands[0].isEmpty() && !this.operands[1].isEmpty()) {
			QueryResult result = QueryResult.createInstance();
			Iterator<Bindings> iteratorLeftChild = this.operands[0].getQueryResult().oneTimeIterator();
			while (iteratorLeftChild.hasNext()) {
				Bindings leftItem = iteratorLeftChild.next();
				boolean found = false;
				for (Bindings rightItem : this.operands[1].getQueryResult()) {
					// compute intersection of the variable sets
					Set<Variable> vars = rightItem.getVariableSet();
					vars.retainAll(leftItem.getVariableSet());

					// if intersection is empty then isEqual should always be false in the typical case (except for not in RIF rules),
					// workaround: check whether vars is empty
					if(vars.isEmpty() && this.considerEmptyIntersection){
						continue;
					}
					
					boolean isEqual = true;
					for (Variable v : vars) {
						if ((v.getLiteral(leftItem).compareToNotNecessarilySPARQLSpecificationConform(v.getLiteral(rightItem))) != 0) {
							isEqual = false;
						}
					}

					if (isEqual){
						found = true;
						break;
					}
				}
				if (!found){
					result.add(leftItem);
				}
			}

			for (final OperatorIDTuple opId : this.succeedingOperators) {
				opId.processAllDebug(new QueryResultDebug(result, debugstep, this, opId.getOperator(), true), debugstep);
			}
		} else if (!this.operands[0].isEmpty()) { // happens when the group constraint which follows the minus is empty
			for (final OperatorIDTuple opId : this.succeedingOperators) {
				opId.processAllDebug(this.operands[0].getQueryResult(), debugstep);
			}
		}
		return msg;
	}

	@Override
	public Message preProcessMessage(BoundVariablesMessage msg) {
		BoundVariablesMessage msg_result = new BoundVariablesMessage(msg);
		Set<Variable> variableSet = null;
		// the variables, which are always bound are the intersection
		// variables of the left hand side
		for (BasicOperator parent : this.getPrecedingOperators()) {
			OperatorIDTuple opID = parent.getOperatorIDTuple(this);
			if (opID.getId() == 0) {
				if (variableSet == null) {
					variableSet = new HashSet<Variable>();
					variableSet.addAll(parent.getIntersectionVariables());
				} else {
					variableSet.retainAll(parent.getIntersectionVariables());
				}
			}
		}

		this.intersectionVariables = variableSet;

		msg_result.setVariables(this.intersectionVariables);
		return msg_result;
	}
	
	@Override
	public String toString(){
		return super.toString()+" "+this.intersectionVariables;
	}
}
