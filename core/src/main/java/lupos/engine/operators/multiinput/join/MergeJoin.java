package lupos.engine.operators.multiinput.join;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import lupos.datastructures.bindings.Bindings;
import lupos.datastructures.dbmergesortedds.DBMergeSortedBag;
import lupos.datastructures.items.Variable;
import lupos.datastructures.items.literal.Literal;
import lupos.datastructures.queryresult.ParallelIterator;
import lupos.datastructures.queryresult.QueryResult;
import lupos.datastructures.queryresult.QueryResultDebug;
import lupos.datastructures.queryresult.SIPParallelIterator;
import lupos.datastructures.sorteddata.SortedBag;
import lupos.engine.operators.Operator;
import lupos.engine.operators.OperatorIDTuple;
import lupos.engine.operators.messages.EndOfEvaluationMessage;
import lupos.engine.operators.messages.Message;
import lupos.engine.operators.multiinput.optional.OptionalResult;
import lupos.misc.debug.DebugStep;

public class MergeJoin extends Join {

	protected SortedBag<Bindings> left = null;
	protected SortedBag<Bindings> right = null;
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

	public void init(final SortedBag<Bindings> left,
			final SortedBag<Bindings> right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public QueryResult process(final QueryResult bindings, final int operandID) {
		if (operandID == 0) {
			final Iterator<Bindings> itb = bindings.oneTimeIterator();
			while (itb.hasNext())
				left.add(itb.next());
		} else if (operandID == 1) {
			final Iterator<Bindings> itb = bindings.oneTimeIterator();
			while (itb.hasNext())
				right.add(itb.next());
		} else
			System.err.println("MergeJoin is a binary operator, but received the operand number "
							+ operandID);
		return null;
	}

	@Override
	public OptionalResult processJoin(final QueryResult bindings,
			final int operandID) {
		if (operandID == 0) {
			left.addAll(bindings.getCollection());
		} else if (operandID == 1) {
			right.addAll(bindings.getCollection());
		} else
			System.err.println("Embedded MergeJoin operator in Optional is a binary operator, but received the operand number "
							+ operandID);
		return null;
	}

	@Override
	public OptionalResult joinBeforeEndOfStream() {
		if (left != null && right != null) {
			final OptionalResult or = mergeJoinOptionalResult(left, right, comp);
			left.clear();
			right.clear();
			return or;
		} else
			return null;
	}

	@Override
	public Message preProcessMessage(final EndOfEvaluationMessage msg) {
		if (left != null && right != null && left.size() > 0
				&& right.size() > 0) {
			final ParallelIterator<Bindings> currentResult = (intersectionVariables
					.size() == 0) ? MergeJoin.cartesianProductIterator(
					QueryResult.createInstance(left.iterator()), QueryResult
							.createInstance(right.iterator())) : MergeJoin
					.mergeJoinIterator(left.iterator(), right.iterator(), comp,
							intersectionVariables);
			if (currentResult != null && currentResult.hasNext()) {
				final QueryResult result = QueryResult
						.createInstance(new ParallelIterator<Bindings>() {

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
								if (!currentResult.hasNext()) {
									realCardinality = number;
									close();
								}
								if (b != null)
									number++;
								return b;
							}

							public void remove() {
								currentResult.remove();
							}

							@Override
							public void finalize() {
								close();
							}
						});

				if (succeedingOperators.size() > 1)
					result.materialize();
				for (final OperatorIDTuple opId : succeedingOperators) {
					opId.processAll(result);
				}
			} else {
				if (left instanceof DBMergeSortedBag)
					((DBMergeSortedBag) left).release();
				if (right instanceof DBMergeSortedBag)
					((DBMergeSortedBag) right).release();
			}

		}
		// left.clear();
		// right.clear();
		return msg;
	}

	public static QueryResult mergeJoin(final Iterator<Bindings> ssb1it,
			final Iterator<Bindings> ssb2it, final Comparator<Bindings> comp) {

		if ((ssb1it == null) || (ssb2it == null) || !ssb1it.hasNext()
				|| !ssb2it.hasNext()) {
			return null;
		}

		final QueryResult result = QueryResult.createInstance();
		Bindings b1 = ssb1it.next();
		Bindings b2 = ssb2it.next();
		boolean processFurther = true;
		do {
			final int compare = comp.compare(b1, b2);
			// System.out.println("compare:"+compare+" b1:"+b1+" b2:"+b2);
			if (compare == 0) {

				final Collection<Bindings> bindings1 = new LinkedList<Bindings>();
				final Collection<Bindings> bindings2 = new LinkedList<Bindings>();

				final Bindings bindings = b1;
				int left = 0;
				do {
					bindings1.add(b1);
					left++;
					if (!ssb1it.hasNext()) {
						processFurther = false;
						break;
					}
					b1 = ssb1it.next();
				} while (comp.compare(b1, bindings) == 0);
				int right = 0;
				do {
					bindings2.add(b2);
					right++;
					if (!ssb2it.hasNext()) {
						processFurther = false;
						break;
					}
					b2 = ssb2it.next();
				} while (comp.compare(b2, bindings) == 0);
				for (final Bindings zb1 : bindings1)
					for (final Bindings zb2 : bindings2) {
						final Bindings bnew = zb1.clone();
						bnew.addAll(zb2);
						bnew.addAllTriples(zb2);
						bnew.addAllPresortingNumbers(zb2);
						result.add(bnew);
					}
			} else if (compare < 0) {
				if (ssb1it.hasNext()) {
					b1 = ssb1it.next();
				} else
					processFurther = false;
			} else if (compare > 0) {
				if (ssb2it.hasNext()) {
					b2 = ssb2it.next();
				} else
					processFurther = false;
			}
		} while (processFurther == true);
		if (result.size() > 0)
			return result;
		else
			return null;
	}

	public static ParallelIterator<Bindings> mergeJoinIterator(
			final Iterator<Bindings> ssb1it, final Iterator<Bindings> ssb2it,
			final Comparator<Bindings> comp, final Collection<Variable> vars) {

		if ((ssb1it == null) || (ssb2it == null) || !ssb1it.hasNext()
				|| !ssb2it.hasNext()) {
			return new ParallelIterator<Bindings>() {
				public boolean hasNext() {
					return false;
				}

				public Bindings next() {
					return null;
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}

				public void close() {
				}
			};
		}

		return new SIPParallelIterator<Bindings, Bindings>() {
			Bindings b1 = ssb1it.next();
			Bindings b2 = ssb2it.next();
			boolean processFurther = true;
			Iterator<Bindings> currentBinding = null;
			Collection<Bindings> bindings1 = null;
			Collection<Bindings> bindings2 = null;

			public boolean hasNext() {
				if (currentBinding != null && currentBinding.hasNext())
					return true;
				if (processFurther) {
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return true;
				}
				return false;
			}

			public void close() {
				if (ssb1it instanceof ParallelIterator)
					((ParallelIterator) ssb1it).close();
				if (ssb2it instanceof ParallelIterator)
					((ParallelIterator) ssb2it).close();
			}

			public Bindings next() {
				if (currentBinding != null && currentBinding.hasNext())
					return currentBinding.next();
				if (processFurther) {
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return currentBinding.next();
				}
				return null;
			}

			public void computeNextResults() {
				while (processFurther == true) {
					final int compare = comp.compare(b1, b2);
					//System.out.println("compare:"+compare+" b1:"+b1+" b2:"+b2)
					// ;
					if (compare == 0) {

						bindings1 = new LinkedList<Bindings>();
						bindings2 = new LinkedList<Bindings>();

						final Bindings bindings = b1;
						do {
							bindings1.add(b1);
							if (!ssb1it.hasNext()) {
								processFurther = false;
								break;
							}
							b1 = ssb1it.next();
						} while (comp.compare(b1, bindings) == 0);
						do {
							bindings2.add(b2);
							if (!ssb2it.hasNext()) {
								processFurther = false;
								break;
							}
							b2 = ssb2it.next();
						} while (comp.compare(b2, bindings) == 0);
						currentBinding = new Iterator<Bindings>() {
							final Iterator<Bindings> itb1 = bindings1
									.iterator();
							Iterator<Bindings> itb2 = bindings2.iterator();
							Bindings zb1 = itb1.next();

							public boolean hasNext() {
								return itb1.hasNext() || itb2.hasNext();
							}

							public Bindings next() {
								if (!hasNext())
									return null;
								if (!itb2.hasNext()) {
									zb1 = itb1.next();
									itb2 = bindings2.iterator();
								}
								final Bindings bnew = zb1.clone();
								final Bindings zb2 = itb2.next();
								bnew.addAll(zb2);
								bnew.addAllTriples(zb2);
								bnew.addAllPresortingNumbers(zb2);
								return bnew;
							}

							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
						return;
					} else if (compare < 0) {
						if (ssb1it.hasNext()) {
							if (ssb1it instanceof SIPParallelIterator) {
								final Bindings projected = Bindings
										.createNewInstance();
								for (final Variable v : vars) {
									projected.add(v, b2.get(v));
								}
								do {
									b1 = ((SIPParallelIterator<Bindings, Bindings>) ssb1it)
											.next(projected);
								} while (b1 != null
										&& comp.compare(b1, projected) < 0);
								if (b1 == null)
									processFurther = false;
							} else
								b1 = ssb1it.next();
						} else
							processFurther = false;
					} else if (compare > 0) {
						if (ssb2it.hasNext()) {
							if (ssb2it instanceof SIPParallelIterator) {
								final Bindings projected = Bindings
										.createNewInstance();
								for (final Variable v : vars) {
									projected.add(v, b1.get(v));
								}
								do {
									b2 = ((SIPParallelIterator<Bindings, Bindings>) ssb2it)
											.next(projected);
								} while (b2 != null
										&& comp.compare(b2, projected) < 0);
								if (b2 == null)
									processFurther = false;
							} else
								b2 = ssb2it.next();
						} else
							processFurther = false;
					}
				}
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public Bindings next(final Bindings k) {
				if (currentBinding != null && currentBinding.hasNext())
					return currentBinding.next();
				if (processFurther) {
					if (ssb1it instanceof SIPParallelIterator) {
						while (comp.compare(b1, k) < 0)
							b1 = ((SIPParallelIterator<Bindings, Bindings>) ssb1it)
									.next(k);
					} else
						while (comp.compare(b1, k) < 0)
							b1 = ssb1it.next();
					if (ssb2it instanceof SIPParallelIterator) {
						while (comp.compare(b2, k) < 0)
							b2 = ((SIPParallelIterator<Bindings, Bindings>) ssb1it)
									.next(k);
					} else
						while (comp.compare(b2, k) < 0)
							b2 = ssb1it.next();
					if (b1 == null || b2 == null) {
						processFurther = false;
						return null;
					}
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return currentBinding.next();
				}
				return null;
			}
		};
	}

	public static ParallelIterator<Bindings> mergeJoinIterator(
			final Iterator<Bindings>[] ssbit, final Comparator<Bindings> comp,
			final Collection<Variable> vars) {

		for (final Iterator<Bindings> it : ssbit) {
			if (it == null || !it.hasNext()) {
				return new ParallelIterator<Bindings>() {
					public boolean hasNext() {
						return false;
					}

					public Bindings next() {
						return null;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}

					public void close() {
					}
				};
			}
		}

		final Bindings[] b = new Bindings[ssbit.length];
		for (int i = 0; i < b.length; i++)
			b[i] = ssbit[i].next();
		final Collection<Bindings>[] bindings = new Collection[b.length];

		return new SIPParallelIterator<Bindings, Bindings>() {
			boolean processFurther = true;
			Iterator<Bindings> currentBinding = null;

			public boolean hasNext() {
				if (currentBinding != null && currentBinding.hasNext())
					return true;
				if (processFurther) {
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return true;
				}
				return false;
			}

			public void close() {
				for (final Iterator<Bindings> it : ssbit) {
					if (it instanceof ParallelIterator)
						((ParallelIterator) it).close();
				}
			}

			public Bindings next() {
				if (currentBinding != null && currentBinding.hasNext())
					return currentBinding.next();
				if (processFurther) {
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return currentBinding.next();
				}
				return null;
			}

			public void computeNextResults() {
				while (processFurther == true) {
					boolean equal = true;
					Bindings maximum = b[0];
					for (int i = 1; i < b.length; i++) {
						final int compare = comp.compare(maximum, b[i]);
						if (compare > 0) {
							equal = false;
						} else if (compare < 0) {
							equal = false;
							maximum = b[i];
						}
					}

					if (equal) {
						for (int i = 0; i < b.length; i++) {
							bindings[i] = new LinkedList<Bindings>();
							final Bindings bindingsCurrent = b[i];
							while (comp.compare(b[i], bindingsCurrent) == 0) {
								bindings[i].add(b[i]);
								if (!ssbit[i].hasNext()) {
									processFurther = false;
									break;
								}
								b[i] = ssbit[i].next();
							}
						}

						final Iterator<Bindings>[] itb = new Iterator[b.length];
						final Bindings zb[] = new Bindings[b.length - 1];
						for (int i = 0; i < b.length; i++) {
							itb[i] = bindings[i].iterator();
						}
						for (int i = 0; i < zb.length; i++) {
							zb[i] = itb[i].next();
						}

						currentBinding = new Iterator<Bindings>() {

							public boolean hasNext() {
								for (final Iterator<Bindings> it : itb)
									if (it.hasNext())
										return true;
								return false;
							}

							public Bindings next() {
								if (!hasNext())
									return null;
								int i = b.length - 1;
								for (; i >= 0 && !itb[i].hasNext(); i--) {
									itb[i] = bindings[i].iterator();
								}
								if (i < b.length - 1)
									zb[i] = itb[i].next();
								final Bindings bnew = zb[0].clone();
								for (int j = 1; j < zb.length; j++) {
									bnew.addAll(zb[j]);
									bnew.addAllTriples(zb[j]);
									bnew.addAllPresortingNumbers(zb[j]);
								}
								final Bindings zb2 = itb[b.length - 1].next();
								bnew.addAll(zb2);
								bnew.addAllTriples(zb2);
								bnew.addAllPresortingNumbers(zb2);
								return bnew;
							}

							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
						return;
					} else {
						for (int i = 0; i < b.length; i++) {
							if (comp.compare(maximum, b[i]) != 0) {
								if (ssbit[i].hasNext()) {
									if (ssbit[i] instanceof SIPParallelIterator) {
										final Bindings projected = Bindings
												.createNewInstance();
										for (final Variable v : vars) {
											projected.add(v, maximum.get(v));
										}
										do {
											b[i] = ((SIPParallelIterator<Bindings, Bindings>) ssbit[i])
													.next(projected);
										} while (b[i] != null
												&& comp
														.compare(b[i],
																projected) < 0);
										if (b[i] == null)
											processFurther = false;
									} else
										b[i] = ssbit[i].next();
								} else
									processFurther = false;
							}
						}
					}
				}
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public Bindings next(final Bindings k) {
				if (currentBinding != null && currentBinding.hasNext())
					return currentBinding.next();
				if (processFurther) {
					for (int i = 0; i < b.length; i++) {
						if (ssbit[i] instanceof SIPParallelIterator) {
							while (comp.compare(b[i], k) < 0)
								b[i] = ((SIPParallelIterator<Bindings, Bindings>) ssbit[i])
										.next(k);
						} else
							while (comp.compare(b[i], k) < 0)
								b[i] = ssbit[i].next();
						if (b[i] == null) {
							processFurther = false;
							return null;
						}
					}
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return currentBinding.next();
				}
				return null;
			}
		};
	}

	public static ParallelIterator<Bindings> mergeJoinIterator(
			final Iterator<Bindings>[] ssbit, final Comparator<Bindings> comp,
			final Collection<Variable> vars, final Bindings minimum,
			final Bindings maximum2) {

		for (final Iterator<Bindings> it : ssbit) {
			if (it == null || !it.hasNext()) {
				return new ParallelIterator<Bindings>() {
					public boolean hasNext() {
						return false;
					}

					public Bindings next() {
						return null;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}

					public void close() {
					}
				};
			}
		}

		final Bindings[] b = new Bindings[ssbit.length];
		for (int i = 0; i < b.length; i++) {
			if (ssbit[i] instanceof SIPParallelIterator)
				b[i] = ((SIPParallelIterator<Bindings, Bindings>) ssbit[i])
						.next(minimum);
			else
				b[i] = ssbit[i].next();
		}
		final Collection<Bindings>[] bindings = new Collection[b.length];

		return new SIPParallelIterator<Bindings, Bindings>() {
			boolean processFurther = true;
			Iterator<Bindings> currentBinding = null;

			public boolean hasNext() {
				if (currentBinding != null && currentBinding.hasNext())
					return true;
				if (processFurther) {
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return true;
				}
				return false;
			}

			public void close() {
				for (final Iterator<Bindings> it : ssbit) {
					if (it instanceof ParallelIterator)
						((ParallelIterator) it).close();
				}
			}

			public Bindings next() {
				if (currentBinding != null && currentBinding.hasNext())
					return currentBinding.next();
				if (processFurther) {
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return currentBinding.next();
				}
				return null;
			}

			public void computeNextResults() {
				while (processFurther == true) {
					boolean equal = true;
					Bindings maximum = b[0];
					for (int i = 1; i < b.length; i++) {
						final int compare = comp.compare(maximum, b[i]);
						if (compare > 0) {
							equal = false;
						} else if (compare < 0) {
							equal = false;
							maximum = b[i];
						}
					}
					if (comp.compare(maximum, maximum2) > 0) {
						processFurther = false;
						break;
					}

					if (equal) {
						for (int i = 0; i < b.length; i++) {
							bindings[i] = new LinkedList<Bindings>();
							final Bindings bindingsCurrent = b[i];
							do {
								bindings[i].add(b[i]);
								if (!ssbit[i].hasNext()) {
									processFurther = false;
									break;
								}
								b[i] = ssbit[i].next();
							} while (comp.compare(b[i], bindingsCurrent) == 0);
						}

						final Iterator<Bindings>[] itb = new Iterator[b.length];
						final Bindings zb[] = new Bindings[b.length - 1];
						for (int i = 0; i < b.length; i++) {
							itb[i] = bindings[i].iterator();
						}
						for (int i = 0; i < zb.length; i++) {
							zb[i] = itb[i].next();
						}

						currentBinding = new Iterator<Bindings>() {

							public boolean hasNext() {
								for (final Iterator<Bindings> it : itb)
									if (it.hasNext())
										return true;
								return false;
							}

							public Bindings next() {
								if (!hasNext())
									return null;
								int i = b.length - 1;
								for (; i >= 0 && !itb[i].hasNext(); i--) {
									itb[i] = bindings[i].iterator();
									if (i != b.length - 1)
										zb[i] = itb[i].next();
								}
								if (i < b.length - 1) {
									zb[i] = itb[i].next();
								}
								final Bindings bnew = zb[0].clone();
								for (int j = 1; j < zb.length; j++) {
									bnew.addAll(zb[j]);
									bnew.addAllTriples(zb[j]);
									bnew.addAllPresortingNumbers(zb[j]);
								}
								final Bindings zb2 = itb[b.length - 1].next();
								bnew.addAll(zb2);
								bnew.addAllTriples(zb2);
								bnew.addAllPresortingNumbers(zb2);
								return bnew;
							}

							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
						return;
					} else {
						final Bindings projected = Bindings.createNewInstance();
						for (final Variable v : vars) {
							projected.add(v, maximum.get(v));
						}
						for (int i = 0; i < b.length; i++) {
							if (comp.compare(maximum, b[i]) != 0) {
								if (ssbit[i].hasNext()) {
									if (ssbit[i] instanceof SIPParallelIterator) {
										do {
											b[i] = ((SIPParallelIterator<Bindings, Bindings>) ssbit[i])
													.next(projected);
										} while (b[i] != null
												&& comp
														.compare(b[i],
																projected) < 0);
										if (b[i] == null)
											processFurther = false;
									} else
										b[i] = ssbit[i].next();
								} else
									processFurther = false;
							}
						}
					}
				}
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public Bindings next(final Bindings k) {
				if (comp.compare(k, maximum2) > 0) {
					processFurther = false;
					return null;
				}
				if (currentBinding != null && currentBinding.hasNext())
					return currentBinding.next();
				if (processFurther) {
					for (int i = 0; i < b.length; i++) {
						if (ssbit[i] instanceof SIPParallelIterator) {
							while (comp.compare(b[i], k) < 0) {
								b[i] = ((SIPParallelIterator<Bindings, Bindings>) ssbit[i])
										.next(k);
								if (comp.compare(b[i], maximum2) > 0) {
									processFurther = false;
									return null;
								}
							}
						} else {
							while (comp.compare(b[i], k) < 0) {
								b[i] = ssbit[i].next();
							}
							if (comp.compare(b[i], maximum2) > 0) {
								processFurther = false;
								return null;
							}
						}
						if (b[i] == null) {
							processFurther = false;
							return null;
						}
					}
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return currentBinding.next();
				}
				return null;
			}
		};
	}

	public static Iterator<Bindings> mergeOptionalIterator(
			final Iterator<Bindings> ssb1it, final Iterator<Bindings> ssb2it,
			final Comparator<Bindings> comp) {

		if ((ssb1it == null) || (ssb2it == null) || !ssb1it.hasNext()
				|| !ssb2it.hasNext()) {
			return new Iterator<Bindings>() {
				public boolean hasNext() {
					return false;
				}

				public Bindings next() {
					return null;
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		return new Iterator<Bindings>() {
			Bindings b1 = ssb1it.next();
			Bindings b2 = ssb2it.next();
			boolean processFurther = true;
			Iterator<Bindings> currentBinding = null;
			Collection<Bindings> bindings1 = null;
			Collection<Bindings> bindings2 = null;

			public boolean hasNext() {
				if (currentBinding != null && currentBinding.hasNext())
					return true;
				if (processFurther) {
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return true;
				}
				return false;
			}

			public Bindings next() {
				if (currentBinding != null && currentBinding.hasNext())
					return currentBinding.next();
				if (processFurther) {
					computeNextResults();
					if (currentBinding != null && currentBinding.hasNext())
						return currentBinding.next();
				}
				return null;
			}

			public void computeNextResults() {
				while (processFurther == true) {

					final int compare = comp.compare(b1, b2);
					if (compare == 0) {

						bindings1 = new LinkedList<Bindings>();
						bindings2 = new LinkedList<Bindings>();
						Iterator<Bindings> currentBinding2 = null;

						final Bindings bindings = b1;
						do {
							bindings1.add(b1);
							if (!ssb1it.hasNext()) {
								b1 = null;
								processFurther = false;
								break;
							}
							b1 = ssb1it.next();
						} while (comp.compare(b1, bindings) == 0);
						do {
							bindings2.add(b2);
							if (!ssb2it.hasNext()) {
								processFurther = false;
								// rest from ssb1:
								currentBinding2 = new Iterator<Bindings>() {
									Bindings zb1 = b1;

									public boolean hasNext() {
										return (zb1 != null || ssb1it.hasNext());
									}

									public Bindings next() {
										if (zb1 != null) {
											final Bindings zzb1 = zb1;
											zb1 = null;
											return zzb1;
										}
										return ssb1it.next();
									}

									public void remove() {
										throw new UnsupportedOperationException();
									}
								};
								break;
							}
							b2 = ssb2it.next();
						} while (comp.compare(b2, bindings) == 0);
						final Iterator<Bindings> currentBinding3 = currentBinding2;
						currentBinding = new Iterator<Bindings>() {
							final Iterator<Bindings> itb1 = bindings1
									.iterator();
							Iterator<Bindings> itb2 = bindings2.iterator();
							Bindings zb1 = itb1.next();
							Iterator<Bindings> restFrom1 = currentBinding3;

							public boolean hasNext() {
								return itb1.hasNext()
										|| itb2.hasNext()
										|| (restFrom1 != null && restFrom1
												.hasNext());
							}

							public Bindings next() {
								if (!hasNext())
									return null;
								if (!(itb1.hasNext() || itb2.hasNext())) {
									if (restFrom1 != null
											&& restFrom1.hasNext()) {
										return restFrom1.next();
									}
								}
								if (!itb2.hasNext()) {
									zb1 = itb1.next();
									itb2 = bindings2.iterator();
								}
								final Bindings bnew = zb1.clone();
								final Bindings zb2 = itb2.next();
								bnew.addAll(zb2);
								bnew.addAllTriples(zb2);
								bnew.addAllPresortingNumbers(zb2);
								return bnew;
							}

							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
						return;
					} else if (compare < 0) {
						if (ssb1it.hasNext()) {
							currentBinding = new Iterator<Bindings>() {
								public boolean hasNext() {
									return (b1 != null && comp.compare(b1, b2) < 0);
								}

								public Bindings next() {
									if (comp.compare(b1, b2) >= 0)
										return null;
									final Bindings zb1 = b1;
									b1 = ssb1it.next();
									if (b1 == null)
										processFurther = false;
									return zb1;
								}

								public void remove() {
									throw new UnsupportedOperationException();
								}
							};
							return;
						} else
							processFurther = false;
					} else if (compare > 0) {
						if (ssb2it.hasNext()) {
							b2 = ssb2it.next();
						} else {
							currentBinding = new Iterator<Bindings>() {
								Bindings zb1 = b1;

								public boolean hasNext() {
									return (zb1 != null || ssb1it.hasNext());
								}

								public Bindings next() {
									if (zb1 != null) {
										final Bindings zzb1 = zb1;
										zb1 = null;
										return zzb1;
									}
									return ssb1it.next();
								}

								public void remove() {
									throw new UnsupportedOperationException();
								}
							};
							processFurther = false;
							return;
						}
					}
				}
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static QueryResult mergeJoin(final SortedBag<Bindings> ssb1,
			final SortedBag<Bindings> ssb2, final Comparator<Bindings> comp,
			final Collection<Variable> vars) {
		if (ssb1 == null || ssb2 == null || ssb1.size() == 0
				|| ssb2.size() == 0)
			return null;
		return QueryResult.createInstance(MergeJoin.mergeJoinIterator(ssb1
				.iterator(), ssb2.iterator(), comp, vars));
		// return mergeJoin(ssb1.iterator(), ssb2.iterator(), comp);
	}

	private OptionalResult mergeJoinOptionalResult(
			final SortedBag<Bindings> ssb1, final SortedBag<Bindings> ssb2,
			final Comparator<Bindings> comp) {
		if (ssb1 == null || ssb2 == null || ssb1.size() == 0
				|| ssb2.size() == 0)
			return null;

		// different from mergeJoin:
		final OptionalResult or = new OptionalResult();
		final QueryResult joinPartnerFromLeftOperand = QueryResult
				.createInstance();

		final QueryResult result = QueryResult.createInstance();
		final Iterator<Bindings> ssb1it = ssb1.iterator();
		final Iterator<Bindings> ssb2it = ssb2.iterator();
		Bindings b1 = ssb1it.next();
		Bindings b2 = ssb2it.next();
		boolean processFurther = true;
		do {
			final int compare = comp.compare(b1, b2);
			if (compare == 0) {

				final Collection<Bindings> bindings1 = new LinkedList<Bindings>();
				final Collection<Bindings> bindings2 = new LinkedList<Bindings>();

				final Bindings bindings = b1;
				int left = 0;
				while (comp.compare(b1, bindings) == 0) {
					bindings1.add(b1);

					// different from mergeJoin:
					joinPartnerFromLeftOperand.add(b1);

					left++;
					if (!ssb1it.hasNext()) {
						processFurther = false;
						break;
					}
					b1 = ssb1it.next();
				}
				int right = 0;
				while (comp.compare(b2, bindings) == 0) {
					bindings2.add(b2);
					right++;
					if (!ssb2it.hasNext()) {
						processFurther = false;
						break;
					}
					b2 = ssb2it.next();
				}
				for (final Bindings zb1 : bindings1)
					for (final Bindings zb2 : bindings2) {
						final Bindings bnew = zb1.clone();
						bnew.addAll(zb2);
						bnew.addAllTriples(zb2);
						bnew.addAllPresortingNumbers(zb2);
						result.add(bnew);
					}
			} else if (compare < 0) {
				if (ssb1it.hasNext()) {
					b1 = ssb1it.next();
				} else
					processFurther = false;
			} else if (compare > 0) {
				if (ssb2it.hasNext()) {
					b2 = ssb2it.next();
				} else
					processFurther = false;
			}
		} while (processFurther == true);

		// different from mergeJoin:
		or.setJoinPartnerFromLeftOperand(joinPartnerFromLeftOperand);
		or.setJoinResult(result);
		return or;
	}

	public static ParallelIterator<Bindings> cartesianProductIterator(
			final QueryResult left, final QueryResult right) {
		if (left == null || right == null)
			return null;
		final QueryResult smaller;
		final QueryResult larger;
		// if (left.size() < right.size()) {
		// smaller = left;
		// larger = right;
		// } else {
		// smaller = right;
		// larger = left;
		// }
		// assume left result to be bigger in order to avoid one unnecessary
		// materialization when calling left.size()
		smaller = right;
		larger = left;
		return cartesianProductIterator(smaller.oneTimeIterator(), larger);
	}

	public static ParallelIterator<Bindings> cartesianProductIterator(
			final Iterator<Bindings> left, final QueryResult right) {
		if (left == null || right == null)
			return null;
		return new ParallelIterator<Bindings>() {
			Iterator<Bindings> outer = left;
			Iterator<Bindings> inner = right.iterator();
			Bindings currentOuterElement = outer.next();

			public boolean hasNext() {
				if (currentOuterElement == null)
					return false;
				if (outer.hasNext()) {
					if (!inner.hasNext()) {
						currentOuterElement = outer.next();
						if (inner instanceof ParallelIterator)
							((ParallelIterator) inner).close();
						inner = right.iterator();
						if (!inner.hasNext())
							return false;
					}
					return true;
				}
				if (inner.hasNext())
					return true;
				return false;
			}

			public Bindings next() {
				if (!inner.hasNext()) {
					if (outer.hasNext()) {
						currentOuterElement = outer.next();
						if (inner instanceof ParallelIterator)
							((ParallelIterator) inner).close();
						inner = right.iterator();
						if (!inner.hasNext())
							return null;
					} else
						return null;
				}
				if (currentOuterElement == null)
					return null;
				final Bindings innerElement = inner.next();
				final Bindings bnew = currentOuterElement.clone();
				bnew.addAll(innerElement);
				bnew.addAllTriples(innerElement);
				bnew.addAllPresortingNumbers(innerElement);

				return bnew;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public void close() {
				if (inner instanceof ParallelIterator)
					((ParallelIterator) inner).close();
				if (outer instanceof ParallelIterator)
					((ParallelIterator) outer).close();
			}

			@Override
			public void finalize() {
				close();
			}
		};
	}

	public static ParallelIterator<Bindings> cartesianProductIterator(
			final QueryResult[] operands) {
		for (int i = 0; i < operands.length; i++)
			if (operands[i] == null)
				return null;
		final Iterator<Bindings>[] itb = new Iterator[operands.length];
		itb[0] = operands[0].oneTimeIterator();
		for (int i = 1; i < operands.length; i++) {
			itb[i] = operands[i].iterator();
		}
		final Bindings[] currentOuterElements = new Bindings[operands.length - 1];
		for (int i = 0; i < currentOuterElements.length; i++) {
			currentOuterElements[i] = itb[i].next();
		}
		return new ParallelIterator<Bindings>() {

			public boolean hasNext() {
				for (int i = 0; i < currentOuterElements.length; i++)
					if (currentOuterElements[i] == null)
						return false;
				for (int i = 0; i < itb.length; i++)
					if (itb[i].hasNext())
						return true;
				return false;
			}

			public Bindings next() {
				if (!hasNext())
					return null;
				int i = itb.length - 1;
				for (; i >= 0 && !itb[i].hasNext(); i--) {
					itb[i] = operands[i].iterator();
					if (i != itb.length - 1)
						currentOuterElements[i] = itb[i].next();
				}
				if (i < itb.length - 1) {
					currentOuterElements[i] = itb[i].next();
				}
				if (!itb[itb.length - 1].hasNext())
					itb[itb.length - 1] = operands[itb.length - 1].iterator();
				final Bindings bnew = itb[itb.length - 1].next().clone();
				for (int j = 0; j < currentOuterElements.length; j++) {
					bnew.addAll(currentOuterElements[j]);
					bnew.addAllTriples(currentOuterElements[j]);
					bnew.addAllPresortingNumbers(currentOuterElements[j]);
				}
				return bnew;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public void close() {
				for (int i = 0; i < itb.length; i++) {
					if (itb[i] instanceof ParallelIterator)
						((ParallelIterator) itb[i]).close();
				}
			}

			@Override
			public void finalize() {
				close();
			}
		};
	}

	@Override
	public QueryResult deleteQueryResult(final QueryResult queryResult,
			final int operandID) {
		final Iterator<Bindings> itb = queryResult.oneTimeIterator();
		if (operandID == 0)
			while (itb.hasNext())
				left.remove(itb.next());
		else
			while (itb.hasNext())
				right.remove(itb.next());
		return null;
	}

	@Override
	public void deleteAll(final int operandID) {
		if (operandID == 0) {
			left.clear();
		} else {
			right.clear();
		}
	}

	@Override
	protected boolean isPipelineBreaker() {
		return true;
	}
	
	@Override
	public Message preProcessMessageDebug(final EndOfEvaluationMessage msg,
			final DebugStep debugstep) {
		if (left != null && right != null && left.size() > 0
				&& right.size() > 0) {
			final ParallelIterator<Bindings> currentResult = (intersectionVariables
					.size() == 0) ? MergeJoin.cartesianProductIterator(
					QueryResult.createInstance(left.iterator()), QueryResult
							.createInstance(right.iterator())) : MergeJoin
					.mergeJoinIterator(left.iterator(), right.iterator(), comp,
							intersectionVariables);
			if (currentResult != null && currentResult.hasNext()) {
				final QueryResult result = QueryResult
						.createInstance(new ParallelIterator<Bindings>() {

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
								if (!currentResult.hasNext()) {
									realCardinality = number;
									close();
								}
								if (b != null)
									number++;
								return b;
							}

							public void remove() {
								currentResult.remove();
							}

							@Override
							public void finalize() {
								close();
							}
						});

				if (succeedingOperators.size() > 1)
					result.materialize();
				for (final OperatorIDTuple opId : succeedingOperators) {
					final QueryResultDebug qrDebug = new QueryResultDebug(
							result, debugstep, this, opId.getOperator(), true);
					((Operator) opId.getOperator()).processAllDebug(qrDebug,
							opId.getId(), debugstep);
				}
			} else {
				if (left instanceof DBMergeSortedBag)
					((DBMergeSortedBag) left).release();
				if (right instanceof DBMergeSortedBag)
					((DBMergeSortedBag) right).release();
			}

		}
		// left.clear();
		// right.clear();
		return msg;
	}
}
