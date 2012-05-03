package lupos.engine.operators.multiinput.join;

import java.util.Comparator;
import java.util.Iterator;

import lupos.datastructures.bindings.Bindings;
import lupos.datastructures.items.Variable;
import lupos.datastructures.items.literal.Literal;
import lupos.datastructures.queryresult.ParallelIterator;
import lupos.datastructures.queryresult.QueryResult;
import lupos.datastructures.queryresult.SIPParallelIterator;
import lupos.engine.operators.messages.Message;
import lupos.engine.operators.messages.StartOfEvaluationMessage;

public class NAryMergeJoinWithoutSorting extends Join {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5051512203278340771L;

	protected QueryResult[] operandResults;
	protected final Bindings minimum;
	protected final Bindings maximum;

	public NAryMergeJoinWithoutSorting(final int numberOfOperands,
			final Bindings minimum, final Bindings maximum) {
		super();
		operandResults = new QueryResult[this.getNumberOfOperands()];
		this.minimum = minimum;
		this.maximum = maximum;
	}

	protected Comparator<Bindings> comp = new Comparator<Bindings>() {

		public int compare(final Bindings o1, final Bindings o2) {
			for (final Variable var : intersectionVariables) {
				final Literal l1 = o1.get(var);
				final Literal l2 = o2.get(var);
				if (l1 != null && l2 != null) {
					final int compare = l1
							.compareToNotNecessarilySPARQLSpecificationConform(l2);
					if (compare != 0)
						return compare;
				} else if (l1 != null)
					return -1;
				else if (l2 != null)
					return 1;
			}
			return 0;
		}

	};

	/**
	 * This method pre-processes the StartOfStreamMessage
	 * 
	 * @param msg
	 *            the message to be pre-processed
	 * @return the pre-processed message
	 */
	@Override
	public Message preProcessMessage(final StartOfEvaluationMessage msg) {
		for (int i = 0; i < operandResults.length; i++) {
			if (operandResults[i] != null) {
				operandResults[i].release();
				operandResults[i] = null;
			}
		}
		return super.preProcessMessage(msg);
	}

	@Override
	public QueryResult process(final QueryResult bindings, final int operandID) {
		if (operandID < operandResults.length) {
			operandResults[operandID] = bindings;
		} else
			System.err.println("NAryMergeJoin is a " + operandResults.length
					+ "-ary operator, but received the operand number "
					+ operandID);
		boolean go = true;
		for (int i = 0; i < operandResults.length; i++) {
			if (operandResults[i] == null) {
				go = false;
				break;
			}
		}
		if (go) {

			if (minimum == null)
				return null;

			final Iterator<Bindings>[] itb = new Iterator[operandResults.length];
			for (int i = 0; i < operandResults.length; i++) {
				itb[i] = operandResults[i].oneTimeIterator();
			}

			final ParallelIterator<Bindings> currentResult = (intersectionVariables
					.size() == 0) ? MergeJoin
					.cartesianProductIterator(operandResults) : MergeJoin
					.mergeJoinIterator(itb, comp, intersectionVariables,
							minimum, maximum);
			if (currentResult != null && currentResult.hasNext()) {
				final QueryResult result = QueryResult
						.createInstance(new SIPParallelIterator<Bindings, Bindings>() {

							int number = 0;

							public void close() {
								currentResult.close();
							}

							public boolean hasNext() {
								if (!currentResult.hasNext()) {
									realCardinality = number;
									close();
								}
								return currentResult.hasNext();
							}

							public Bindings next() {
								final Bindings b = currentResult.next();
								if (b != null)
									number++;
								if (!currentResult.hasNext()) {
									realCardinality = number;
									close();
								}
								return b;
							}

							public Bindings getNext(final Bindings k) {
								final Bindings b = ((SIPParallelIterator<Bindings, Bindings>) currentResult)
										.next(k);
								if (b != null)
									number++;
								if (!currentResult.hasNext()) {
									realCardinality = number;
									close();
								}
								return b;
							}

							public void remove() {
								currentResult.remove();
							}

							@Override
							public void finalize() {
								close();
							}

							public Bindings next(final Bindings k) {
								if (currentResult instanceof SIPParallelIterator)
									return getNext(k);
								else
									return next();
							}
						});
				// System.out.println(this.toString());
				// System.out.println("!!!!!!!!!!Preceding operators:"
				// + this.getPrecedingOperators());
				// System.out.println("!!!!!!!!!!Results: Left:"+left.size()+
				// "\n!!!!!!!!!! Right:"
				// +right.size()+"\n!!!!!!!!!! Result:"+((result
				// ==null)?"null":result.size()));*/
				// System.out.println("!!!!!!!!!!Results: Left:" + left
				// + "\n!!!!!!!!!! Right:" + right
				// + "\n!!!!!!!!!! Result:"
				// + ((result == null) ? "null" : result));
				// System.out.println("Result:" + result);
				// System.out.println("Result size:" + result.size());
				return result;
			} else {
				for (int i = 0; i < operandResults.length; i++) {
					if (operandResults[i] != null) {
						operandResults[i].release();
						operandResults[i] = null;
					}
				}
				return null;
			}
		} else {
			return null;
		}
	}
}
