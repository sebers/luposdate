/* Generated By:JavaCC: Do not edit this line. StreamSPARQL1_1ParserVisitor.java Version 5.0 */
package lupos.sparql1_1;

public interface StreamSPARQL1_1ParserVisitor
{
  public Object visit(SimpleNode node, Object data);
  public Object visit(ASTGroupConstraint node, Object data);
  public Object visit(ASTTripleSet node, Object data);
  public Object visit(ASTAVerbType node, Object data);
  public Object visit(ASTQuery node, Object data);
  public Object visit(ASTBaseDecl node, Object data);
  public Object visit(ASTPrefixDecl node, Object data);
  public Object visit(ASTSelectQuery node, Object data);
  public Object visit(ASTAs node, Object data);
  public Object visit(ASTConstructQuery node, Object data);
  public Object visit(ASTDescribeQuery node, Object data);
  public Object visit(ASTAskQuery node, Object data);
  public Object visit(ASTStream node, Object data);
  public Object visit(ASTWindow node, Object data);
  public Object visit(ASTStart node, Object data);
  public Object visit(ASTEnd node, Object data);
  public Object visit(ASTType node, Object data);
  public Object visit(ASTDefaultGraph node, Object data);
  public Object visit(ASTNamedGraph node, Object data);
  public Object visit(ASTGroup node, Object data);
  public Object visit(ASTHaving node, Object data);
  public Object visit(ASTOrderConditions node, Object data);
  public Object visit(ASTAscOrder node, Object data);
  public Object visit(ASTDescOrder node, Object data);
  public Object visit(ASTLimit node, Object data);
  public Object visit(ASTOffset node, Object data);
  public Object visit(ASTBindings node, Object data);
  public Object visit(ASTPlusNode node, Object data);
  public Object visit(ASTUndef node, Object data);
  public Object visit(ASTLoad node, Object data);
  public Object visit(ASTClear node, Object data);
  public Object visit(ASTDrop node, Object data);
  public Object visit(ASTCreate node, Object data);
  public Object visit(ASTAdd node, Object data);
  public Object visit(ASTMove node, Object data);
  public Object visit(ASTCopy node, Object data);
  public Object visit(ASTInsert node, Object data);
  public Object visit(ASTDelete node, Object data);
  public Object visit(ASTModify node, Object data);
  public Object visit(ASTDefault node, Object data);
  public Object visit(ASTNamed node, Object data);
  public Object visit(ASTAll node, Object data);
  public Object visit(ASTConstructTemplate node, Object data);
  public Object visit(ASTGraphConstraint node, Object data);
  public Object visit(ASTOptionalConstraint node, Object data);
  public Object visit(ASTService node, Object data);
  public Object visit(ASTBind node, Object data);
  public Object visit(ASTMinus node, Object data);
  public Object visit(ASTUnionConstraint node, Object data);
  public Object visit(ASTFilterConstraint node, Object data);
  public Object visit(ASTFunctionCall node, Object data);
  public Object visit(ASTArguments node, Object data);
  public Object visit(ASTExpressionList node, Object data);
  public Object visit(ASTNodeSet node, Object data);
  public Object visit(ASTObjectList node, Object data);
  public Object visit(ASTPathAlternative node, Object data);
  public Object visit(ASTPathSequence node, Object data);
  public Object visit(ASTInvers node, Object data);
  public Object visit(ASTArbitraryOccurences node, Object data);
  public Object visit(ASTOptionalOccurence node, Object data);
  public Object visit(ASTArbitraryOccurencesNotZero node, Object data);
  public Object visit(ASTNegatedPath node, Object data);
  public Object visit(ASTInteger node, Object data);
  public Object visit(ASTBlankNodePropertyList node, Object data);
  public Object visit(ASTCollection node, Object data);
  public Object visit(ASTVar node, Object data);
  public Object visit(ASTNIL node, Object data);
  public Object visit(ASTOrNode node, Object data);
  public Object visit(ASTAndNode node, Object data);
  public Object visit(ASTEqualsNode node, Object data);
  public Object visit(ASTNotEqualsNode node, Object data);
  public Object visit(ASTLessThanNode node, Object data);
  public Object visit(ASTGreaterThanNode node, Object data);
  public Object visit(ASTLessThanEqualsNode node, Object data);
  public Object visit(ASTGreaterThanEqualsNode node, Object data);
  public Object visit(ASTInNode node, Object data);
  public Object visit(ASTNotInNode node, Object data);
  public Object visit(ASTAdditionNode node, Object data);
  public Object visit(ASTSubtractionNode node, Object data);
  public Object visit(ASTMultiplicationNode node, Object data);
  public Object visit(ASTDivisionNode node, Object data);
  public Object visit(ASTNotNode node, Object data);
  public Object visit(ASTMinusNode node, Object data);
  public Object visit(ASTStrFuncNode node, Object data);
  public Object visit(ASTLangFuncNode node, Object data);
  public Object visit(ASTLangMatchesFuncNode node, Object data);
  public Object visit(ASTDataTypeFuncNode node, Object data);
  public Object visit(ASTBoundFuncNode node, Object data);
  public Object visit(ASTIriFuncNode node, Object data);
  public Object visit(ASTUriFuncNode node, Object data);
  public Object visit(ASTBnodeFuncNode node, Object data);
  public Object visit(ASTRandFuncNode node, Object data);
  public Object visit(ASTABSFuncNode node, Object data);
  public Object visit(ASTCeilFuncNode node, Object data);
  public Object visit(ASTFloorFuncNode node, Object data);
  public Object visit(ASTRoundFuncNode node, Object data);
  public Object visit(ASTConcatFuncNode node, Object data);
  public Object visit(ASTStrlenFuncNode node, Object data);
  public Object visit(ASTUcaseFuncNode node, Object data);
  public Object visit(ASTLcaseFuncNode node, Object data);
  public Object visit(ASTEncodeForUriFuncNode node, Object data);
  public Object visit(ASTContainsFuncNode node, Object data);
  public Object visit(ASTStrstartsFuncNode node, Object data);
  public Object visit(ASTStrEndsFuncNode node, Object data);
  public Object visit(ASTStrBeforeFuncNode node, Object data);
  public Object visit(ASTStrAfterFuncNode node, Object data);
  public Object visit(ASTYearFuncNode node, Object data);
  public Object visit(ASTMonthFuncNode node, Object data);
  public Object visit(ASTDayFuncNode node, Object data);
  public Object visit(ASTHoursFuncNode node, Object data);
  public Object visit(ASTMinutesFuncNode node, Object data);
  public Object visit(ASTSecondsFuncNode node, Object data);
  public Object visit(ASTTimeZoneFuncNode node, Object data);
  public Object visit(ASTTzFuncNode node, Object data);
  public Object visit(ASTNowFuncNode node, Object data);
  public Object visit(ASTUUIDFuncNode node, Object data);
  public Object visit(ASTSTRUUIDFuncNode node, Object data);
  public Object visit(ASTMD5FuncNode node, Object data);
  public Object visit(ASTSHA1FuncNode node, Object data);
  public Object visit(ASTSHA256FuncNode node, Object data);
  public Object visit(ASTSHA384FuncNode node, Object data);
  public Object visit(ASTSHA512FuncNode node, Object data);
  public Object visit(ASTCoalesceFuncNode node, Object data);
  public Object visit(ASTIfFuncNode node, Object data);
  public Object visit(ASTStrLangFuncNode node, Object data);
  public Object visit(ASTStrdtFuncNode node, Object data);
  public Object visit(ASTSameTermFuncNode node, Object data);
  public Object visit(ASTisIRIFuncNode node, Object data);
  public Object visit(ASTisURIFuncNode node, Object data);
  public Object visit(ASTisBlankFuncNode node, Object data);
  public Object visit(ASTisLiteralFuncNode node, Object data);
  public Object visit(ASTisNumericFuncNode node, Object data);
  public Object visit(ASTRegexFuncNode node, Object data);
  public Object visit(ASTSubstringFuncNode node, Object data);
  public Object visit(ASTStrReplaceFuncNode node, Object data);
  public Object visit(ASTExists node, Object data);
  public Object visit(ASTNotExists node, Object data);
  public Object visit(ASTAggregation node, Object data);
  public Object visit(ASTRDFLiteral node, Object data);
  public Object visit(ASTDoubleCircumflex node, Object data);
  public Object visit(ASTLangTag node, Object data);
  public Object visit(ASTFloatingPoint node, Object data);
  public Object visit(ASTBooleanLiteral node, Object data);
  public Object visit(ASTStringLiteral node, Object data);
  public Object visit(ASTQuotedURIRef node, Object data);
  public Object visit(ASTQName node, Object data);
  public Object visit(ASTBlankNode node, Object data);
  public Object visit(ASTEmptyNode node, Object data);
}
/* JavaCC - OriginalChecksum=61a3b3640f687edd94fb554ed95cdf50 (do not edit this line) */
