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
package lupos.gui.anotherSyntaxHighlighting.javacc;

import lupos.gui.anotherSyntaxHighlighting.ILuposToken;
import lupos.gui.anotherSyntaxHighlighting.LANGUAGE.TYPE_ENUM;

public abstract class JavaCCToken implements ILuposToken {
	protected final int ID;
	protected final String contents;

	protected final int beginChar;
	protected final int endChar;
	
	/**
	 * Create a new token. The constructor is typically called by the parser.
	 * 
	 * @param ID
	 *            the id number of the token
	 * @param contents
	 *            A string representing the text of the token
	 * @param beginColumn
	 *            the line number of the input on which this token started
	 * @param beginColumn
	 *            the offset into the input in characters at which this token
	 *            started
	 * @param endColumn
	 *            the offset into the input in characters at which this token
	 *            ended
	 */
	public JavaCCToken(int ID, String contents, int beginChar, int endChar) {
		this.ID = ID;
		this.contents = new String(contents);
		this.beginChar = beginChar;
		this.endChar = endChar;
	}


	public JavaCCToken(TYPE_ENUM description, String contents, int beginChar) {
		this(description.ordinal(), contents, beginChar, beginChar + contents.length());
	}


	/**
	 * get a String that explains the error, if this token is an error.
	 * 
	 * @return a String that explains the error, if this token is an error, null
	 *         otherwise.
	 */
	@Override
	public String errorString() {
		String s;
		if (this.getDescription().isError()) {
			s = "Error from " + this.beginChar + " to " + this.endChar + ": ";
		} else {
			s = null;
		}
		return (s);
	}

	/**
	 * get a representation of this token as a human readable string. The format
	 * of this string is subject to change and should only be used for debugging
	 * purposes.
	 * 
	 * @return a string representation of this token
	 */
	@Override
	public String toString() {
		return ("Token #" + Integer.toHexString(this.ID) + ": " + this.getDescription() + " Position from " + this.beginChar + " to " + this.endChar + " : " + this.contents);
	}

	/**
	 * @return the iD
	 */
	public int getID() {
		return this.ID;
	}

	/**
	 * @return the contents
	 */
	@Override
	public String getContents() {
		return this.contents;
	}

	/**
	 * @return the beginChar
	 */
	@Override
	public int getBeginChar() {
		return this.beginChar;
	}

	/**
	 * @return the endChar
	 */
	public int getEndChar() {
		return this.endChar;
	}
}
