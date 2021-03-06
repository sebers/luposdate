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
package lupos.rif.visitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import lupos.rif.IExpression;
import lupos.rif.IRuleVisitor;
import lupos.rif.RIFException;
import lupos.rif.model.Conjunction;
import lupos.rif.model.Constant;
import lupos.rif.model.Disjunction;
import lupos.rif.model.Document;
import lupos.rif.model.Equality;
import lupos.rif.model.ExistExpression;
import lupos.rif.model.External;
import lupos.rif.model.Rule;
import lupos.rif.model.RuleList;
import lupos.rif.model.RulePredicate;
import lupos.rif.model.RuleVariable;

public class ValidateRuleVisitor implements IRuleVisitor<Object, Object> {

	public enum VALIDATION {
		NOTHING, CHECK_FOR_SINGLE_CONSTANTS_VARIABLES, ALLOW_SINGLE_VARIABLES_TRIPLES, NO_TRIPLE_AND_EXISTS
	}

	final Set<RuleVariable> declaredVars = new HashSet<RuleVariable>();

	public Object visit(Document obj, Object arg) throws RIFException {
		// Fakten ueberpruefen
		// -nur Tripel erlaubt
		for (Rule rule : obj.getRules()) {
			if (!rule.isImplication()) {
				rule.getHead().accept(this, arg);
			}
		}
		// Jede Regel �berpr�fen
		for (Rule rule : obj.getRules()) {
			if (rule.isImplication())
				rule.accept(this, arg);
		}
		return true;
	}

	public Object visit(Rule obj, Object arg) throws RIFException {
		// Alle deklarierten Variablen muessen auch verwendet werden und keine
		// neuen verwendet werden
		Set<RuleVariable> declared = new HashSet<RuleVariable>(
				obj.getDeclaredVariables());
		if (!obj.getDeclaredVariables().isEmpty()) {
			Set<RuleVariable> used = obj.getHead().getVariables();
			if (obj.isImplication())
				used.addAll(obj.getBody().getVariables());
			if (declared.size() < used.size())
				for (RuleVariable var : used)
					if (!declared.contains(var))
						throw new RIFException("Variable " + var.toString()
								+ " not declared!");
		}
		// Jede Variable muss gebunden sein im Variablenscope
		if (obj.isImplication()) {
			final Collection<RuleVariable> boundVars = new HashSet<RuleVariable>();
			// Boundvars intialisieren
			int initSize = 0;
			do {
				initSize = boundVars.size();
				for (RuleVariable var : obj.getHead().getVariables())
					obj.getBody().isBound(var, boundVars);
			} while (initSize < boundVars.size());
			// zweite Iteration, Ungebundene Variablen koennen an gebundene
			// gebunden werden
			for (RuleVariable var : obj.getHead().getVariables())
				if (!boundVars.contains(var))
					throw new RIFException("Variable " + var.toString()
							+ " not bound!");
		}
		// VariablenScope setzen
		declaredVars.addAll(declared);
		// obj.head.accept(this, VALIDATION.ALLOW_SINGLE_VARIABLES_TRIPLES);
		// // Head darf NUR Variablen haben oder NUR Uniterme bzw. Tripel
		// // Set<RuleVariable> variables = obj.head.getVariables();
		// List<Uniterm> terms = obj.head.getPredicates();
		// if (obj.head.containsOnlyVariables() && !terms.isEmpty())
		// throw new RIFException(
		// "Only Variables or Only Terms are allowed in Rule-Head!");

		// Regelkoerper ueberpruefen
		if (obj.isImplication())
			obj.getBody().accept(this,
					VALIDATION.CHECK_FOR_SINGLE_CONSTANTS_VARIABLES);
		// VariablenScope entfernen
		declaredVars.removeAll(declared);
		return true;
	}

	public Object visit(RulePredicate obj, Object arg) throws RIFException {
		VALIDATION flag = (VALIDATION) arg;
		if (flag == VALIDATION.NO_TRIPLE_AND_EXISTS)
			throw new RIFException("Triple not allowed in Comparsion!");
		if (!(obj.termName instanceof RuleVariable || obj.termName instanceof Constant))
			throw new RIFException("Relation " + obj.termName.toString()
					+ " in Uniterm has to a Variable or Constant!");
		return true;
	}

	public Object visit(Conjunction obj, Object arg) throws RIFException {
		for (IExpression expr : obj.exprs)
			expr.accept(this, arg);
		return true;
	}

	public Object visit(Disjunction obj, Object arg) throws RIFException {
		for (IExpression expr : obj.exprs)
			expr.accept(this, arg);
		return true;
	}

	public Object visit(ExistExpression obj, Object arg) throws RIFException {
		VALIDATION flag = (VALIDATION) arg;
		if (flag == VALIDATION.NO_TRIPLE_AND_EXISTS)
			throw new RIFException("Exists not allowed in Comparsion!");
		// variablenscopes �berpr�fen
		Set<RuleVariable> declared = obj.getDeclaredVariables();
		Set<RuleVariable> innerVars = obj.expr.getVariables();
		// Deklarierte Variablen d�rfen nicht schon deklariert sein (Scope)
		// und m�ssen vorkommen im Term
		for (RuleVariable var : declared)
			if (!innerVars.contains(var))
				throw new RIFException("Variable " + var.toString()
						+ " is declared, but not used!");
			else if (declaredVars.contains(var))
				throw new RIFException("Variable " + var.toString()
						+ " in Exists is already declared!");

		// Jede Variable muss gebunden sein im Variablenscope
		final Collection<RuleVariable> boundVars = new HashSet<RuleVariable>();
		// Boundvars intialisieren
		int initSize = 0;
		do {
			initSize = boundVars.size();
			for (RuleVariable var : declared)
				obj.expr.isBound(var, boundVars);
		} while (initSize < boundVars.size());
		// zweite Iteration, Ungebundene Variablen k�nnen an gebundene
		// gebunden werden
		for (RuleVariable var : declared)
			if (!boundVars.contains(var))
				throw new RIFException("Variable " + var.toString()
						+ " not bound!");

		declaredVars.addAll(declared);
		obj.expr.accept(this, arg);
		declaredVars.removeAll(declared);
		return true;
	}

	public Object visit(Equality obj, Object arg) throws RIFException {
		obj.leftExpr.accept(this, null);
		obj.rightExpr.accept(this, null);
		return true;
	}

	public Object visit(External obj, Object arg) throws RIFException {
		return true;
	}

	public Object visit(Constant obj, Object arg) throws RIFException {
		VALIDATION flag = (VALIDATION) arg;
		if (flag == VALIDATION.CHECK_FOR_SINGLE_CONSTANTS_VARIABLES
				|| flag == VALIDATION.ALLOW_SINGLE_VARIABLES_TRIPLES)
			throw new RIFException("Single Constant " + obj.toString()
					+ " found!");
		return true;
	}

	public Object visit(RuleVariable obj, Object arg) throws RIFException {
		VALIDATION flag = (VALIDATION) arg;
		if (flag == VALIDATION.CHECK_FOR_SINGLE_CONSTANTS_VARIABLES)
			throw new RIFException("Single Variable " + obj.toString()
					+ " found!");
		return true;
	}

	public Object visit(RuleList obj, Object arg) throws RIFException {
		return true;
	}
}