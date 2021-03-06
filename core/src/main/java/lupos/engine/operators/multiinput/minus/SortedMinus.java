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
import lupos.datastructures.queryresult.QueryResult;
import lupos.engine.operators.BasicOperator;
import lupos.engine.operators.OperatorIDTuple;
import lupos.engine.operators.messages.BoundVariablesMessage;
import lupos.engine.operators.messages.EndOfEvaluationMessage;
import lupos.engine.operators.messages.Message;
import lupos.engine.operators.singleinput.sort.Sort;
import lupos.engine.operators.singleinput.sort.comparator.ComparatorBindings;
import lupos.engine.operators.singleinput.sort.comparator.ComparatorVariables;
import lupos.misc.debug.DebugStep;

public class SortedMinus extends Minus {

	protected ComparatorBindings comp;
	protected Sort predecessorLeft, predecessorRight; // sorting nodes which
	// preprocess the input
	protected boolean isSortable;

	public SortedMinus(Sort predecessorLeft, Sort predecessorRight) {
		this.predecessorLeft = predecessorLeft;
		this.predecessorRight = predecessorRight;
	}

	@Override
	public synchronized QueryResult process(final QueryResult bindings,
			final int operandID) {

		if (!this.isSortable){
			return super.process(bindings, operandID);
		}

		// wait for all query results and process them when
		// EndOfEvaluationMessage arrives
		if(this.operands[operandID].isEmpty()){
			return super.process(bindings, operandID);
		} else {
			throw new UnsupportedOperationException("More than one query result, but result should be sorted.");
		}
	}

	@Override
	public Message preProcessMessage(EndOfEvaluationMessage msg) {

		if (!this.isSortable || this.operands[1].isEmpty()) {
			return super.preProcessMessage(msg);
		} else if (!this.operands[0].isEmpty() && !this.operands[1].isEmpty()) {
			QueryResult result = QueryResult.createInstance();

			Iterator<Bindings> leftIt = this.operands[0].getQueryResult().iterator();
			Iterator<Bindings> rightIt = this.operands[1].getQueryResult().iterator();
			Bindings leftBindings = null, rightBindings = null;
			if (leftIt.hasNext()) {
				leftBindings = leftIt.next();
			}
			if (rightIt.hasNext()) {
				rightBindings = rightIt.next();
			}

			while (rightBindings != null && leftBindings != null) {
				int difference = this.comp.compare(leftBindings, rightBindings);
				if (difference > 0) {
					rightBindings = rightIt.hasNext() ? rightIt.next() : null;
				} else if (difference == 0) {
					leftBindings = leftIt.hasNext() ? leftIt.next() : null;
				} else if (difference < 0) {
					result.add(leftBindings);
					leftBindings = leftIt.hasNext() ? leftIt.next() : null;
				}

			}

			while (leftBindings != null) {
				result.add(leftBindings);
				leftBindings = leftIt.hasNext() ? leftIt.next() : null;
			}

			for (final OperatorIDTuple opId : this.succeedingOperators) {
				opId.processAll(result);
			}
		}

		return msg;
	}
	
	@Override
	public Message preProcessMessageDebug(EndOfEvaluationMessage msg, final DebugStep debugstep) {

		if (!this.isSortable || this.operands[1].isEmpty()) {
			return super.preProcessMessage(msg);
		} else if (!this.operands[0].isEmpty() && !this.operands[1].isEmpty()) {
			QueryResult result = QueryResult.createInstance();

			Iterator<Bindings> leftIt = this.operands[0].getQueryResult().iterator();
			Iterator<Bindings> rightIt = this.operands[1].getQueryResult().iterator();
			Bindings leftBindings = null, rightBindings = null;
			if (leftIt.hasNext()) {
				leftBindings = leftIt.next();
			}
			if (rightIt.hasNext()) {
				rightBindings = rightIt.next();
			}

			while (rightBindings != null && leftBindings != null) {
				int difference = this.comp.compare(leftBindings, rightBindings);
				if (difference > 0) {
					rightBindings = rightIt.hasNext() ? rightIt.next() : null;
				} else if (difference == 0) {
					leftBindings = leftIt.hasNext() ? leftIt.next() : null;
				} else if (difference < 0) {
					result.add(leftBindings);
					leftBindings = leftIt.hasNext() ? leftIt.next() : null;
				}

			}

			while (leftBindings != null) {
				result.add(leftBindings);
				leftBindings = leftIt.hasNext() ? leftIt.next() : null;
			}

			for (final OperatorIDTuple opId : this.succeedingOperators) {
				opId.processAllDebug(result, debugstep);
			}
		}

		return msg;
	}

	@Override
	public Message preProcessMessage(BoundVariablesMessage msg) {
		Message msgResult = super.preProcessMessage(msg);
		Set<Variable> leftUnion = null; 
		Set<Variable> rightUnion = null;

		// if all variables that must be compared are always bound in every
		// binding, then they can easily be presorted,
		// we check this
		for (BasicOperator parent : this.getPrecedingOperators()) {
			OperatorIDTuple opID = parent.getOperatorIDTuple(this);
			if (opID.getId() == 0) {
				if (leftUnion == null) {
					leftUnion = new HashSet<Variable>();
					leftUnion.addAll(parent.getIntersectionVariables());
				} else {
					leftUnion.retainAll(parent.getIntersectionVariables());
				}
			} else if (opID.getId() == 1) {
				if (rightUnion == null) {
					rightUnion = new HashSet<Variable>();
					rightUnion.addAll(parent.getIntersectionVariables());
				} else {
					rightUnion.retainAll(parent.getIntersectionVariables());
				}
			}
		}

		Set<Variable> intersection = leftUnion;
		intersection.retainAll(rightUnion);

		this.isSortable = !this.intersectionVariables.isEmpty() && intersection.equals(this.intersectionVariables);

		this.comp = new ComparatorVariables(this.intersectionVariables);
		this.predecessorLeft.setComparator(this.comp);
		this.predecessorRight.setComparator(this.comp);

		return msgResult;
	}

}
