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
package lupos.datastructures.dbmergesortedds.heap;

import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lupos.datastructures.parallel.BoundedBuffer;

class InnerParallelHeap<E extends Comparable<E>> extends Thread {

	protected Lock lockFinish = new ReentrantLock();
	protected Condition finishCondition = lockFinish.newCondition();

	protected Lock lockEmpty = new ReentrantLock();
	protected Condition emptyCondition = lockEmpty.newCondition();
	protected Condition nonemptyCondition = lockEmpty.newCondition();

	protected int waitForEmpty = 0;

	protected static final int maxInstructionBuffer = 10;
	protected final SequentialHeap<E> sh;
	protected BoundedBuffer<InstructionContainer> bi = new BoundedBuffer<InstructionContainer>(
			maxInstructionBuffer);
	protected volatile boolean running = true;

	protected volatile int runningInstructionId = 0;
	protected volatile int finishedInstructionId = -1;

	public enum INSTRUCTION {
		BUBBLEDOWN, UPDATE, BUILDHEAP
	};

	/**
	 * @param sh
	 */
	public InnerParallelHeap(final SequentialHeap<E> sh) {
		super("InnerParallelHeap");
		this.sh = sh;
		String s = "";
		final Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
		Thread main = null;
		for (final Thread t : map.keySet()) {
			if (t.getName().compareTo("main") == 0) {
				main = t;
			}
		}
		if (main != null)
			for (final StackTraceElement str : map.get(main))
				s += str.toString() + "\n";
		this.setName("InnerParallelHeap, StackTrace: " + s);
	}

	public Object getLastElement() {
		lockEmpty.lock();
		try {
			waitForEmpty++;
			lockFinish.lock();
			try {
				try {
					while (finishedInstructionId + 1 < runningInstructionId) {
						finishCondition.await();
					}
				} catch (final InterruptedException e) {
					System.err.println(e);
					e.printStackTrace();
				}
				final E e = (E) sh.arr[--sh.length];
				sh.arr[sh.length] = null;
				return e;
			} finally {
				lockFinish.unlock();
			}
		} finally {
			waitForEmpty--;
			if (waitForEmpty == 0)
				nonemptyCondition.signalAll();
			lockEmpty.unlock();
		}
	}

	public E peek() {
		lockEmpty.lock();
		try {
			waitForEmpty++;
			lockFinish.lock();
			try {
				try {
					while (finishedInstructionId + 1 < runningInstructionId) {
						finishCondition.await();
					}
				} catch (final InterruptedException e) {
					System.err.println(e);
					e.printStackTrace();
				}
				return sh.peek();
			} finally {
				lockFinish.unlock();
			}
		} finally {
			waitForEmpty--;
			if (waitForEmpty == 0)
				nonemptyCondition.signalAll();
			lockEmpty.unlock();
		}
	}

	public void waitForEmptyInstructionQueue() {
		lockEmpty.lock();
		try {
			waitForEmpty++;
			lockFinish.lock();
			try {
				try {
					while (finishedInstructionId + 1 < runningInstructionId) {
						finishCondition.await();
					}
				} catch (final InterruptedException e) {
					System.err.println(e);
					e.printStackTrace();
				}
				return;
			} finally {
				lockFinish.unlock();
			}
		} finally {
			waitForEmpty--;
			if (waitForEmpty == 0)
				nonemptyCondition.signalAll();
			lockEmpty.unlock();
		}
	}

	public void bubbledown(final E data) {
		lockEmpty.lock();
		try {
			while (waitForEmpty > 0)
				nonemptyCondition.await();
			bi.put(new InstructionContainer(INSTRUCTION.BUBBLEDOWN, data));
		} catch (final InterruptedException e) {
			System.err.println(e);
			e.printStackTrace();
		} finally {
			lockEmpty.unlock();
		}
	}

	public void buildHeap() {
		lockEmpty.lock();
		try {
			while (waitForEmpty > 0)
				nonemptyCondition.await();
			bi.put(new InstructionContainer(INSTRUCTION.BUILDHEAP));
		} catch (final InterruptedException e) {
			System.err.println(e);
			e.printStackTrace();
		} finally {
			lockEmpty.unlock();
		}
	}

	public void addByUpdating(final E data) {
		lockEmpty.lock();
		try {
			while (waitForEmpty > 0)
				nonemptyCondition.await();
			bi.put(new InstructionContainer(INSTRUCTION.UPDATE, data));
		} catch (final InterruptedException e) {
			System.err.println(e);
			e.printStackTrace();
		} finally {
			lockEmpty.unlock();
		}
	}

	@Override
	public void run() {
		try {
			while (running) {
				InstructionContainer ic;
				ic = bi.get();
				if (ic != null) {
					switch (ic.getInstruction()) {
					case BUBBLEDOWN:
						sh.arr[0] = ic.getData();
						if (sh.arr[0] == null) {
							sh.bubbleDown(0);
							sh.length--;
						} else {
							sh.bubbleDown(0);
						}
						break;
					case UPDATE:
						sh.addByUpdating(ic.getData());
						break;
					case BUILDHEAP:
						sh.buildHeap();
						break;
					}
					finishedInstructionId = ic.getInstructionId();
					lockFinish.lock();
					try {
						if (finishedInstructionId + 1 == runningInstructionId) {
							finishCondition.signalAll();
						}
					} finally {
						lockFinish.unlock();
					}
				}
			}
		} catch (final InterruptedException e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	@Override
	public void finalize() {
		stopIt();
	}

	public void stopIt() {
		running = false;
		bi.stopIt();
	}

	private class InstructionContainer {
		private E data;
		private INSTRUCTION instruction;
		private int instructionId = runningInstructionId++;

		/**
		 * @param data
		 * @param instruction
		 */
		public InstructionContainer(final INSTRUCTION instruction, final E data) {
			this(instruction, data, null);
		}

		public InstructionContainer(final INSTRUCTION instruction) {
			this(instruction, null, null);
		}

		public InstructionContainer(final INSTRUCTION instruction,
				final E data, final Object dummy) {
			this.data = data;
			this.instruction = instruction;
		}

		public E getData() {
			return data;
		}

		public void setData(final E data) {
			this.data = data;
		}

		public INSTRUCTION getInstruction() {
			return instruction;
		}

		public void setInstruction(final INSTRUCTION instruction) {
			this.instruction = instruction;
		}

		public int getInstructionId() {
			return instructionId;
		}

		public void setInstructionId(final int instructionId) {
			this.instructionId = instructionId;
		}
	}
}
