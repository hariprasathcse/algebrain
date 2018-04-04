package common.system;

import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import common.parser.ExprLexer;
import common.parser.ExprParser;
import common.system.domain.AbstractNode;
import common.system.domain.DollarNode;
import common.system.domain.NumNode;
import common.system.domain.OperatorNode;
import common.system.domain.Rule;
import common.system.domain.VarNode;

public class NodeFunctions {

	public static String expression(AbstractNode node){
		String output = null;

		if (isNum(node)) { 
			NumNode numNode = (NumNode) node;
			output = Integer.toString(numNode.getKey());

		}
		else if (isVar(node)){
			VarNode varNode = (VarNode) node;
			output = varNode.getKey();
		}
		else if (isDollar(node)){
			DollarNode dollarNode = (DollarNode) node;
			output="$"+dollarNode.getKey();
		}
		else if (isOperator(node)){
			boolean parens=false;
			OperatorNode opeNode = (OperatorNode) node;
			ArrayList<AbstractNode> children = opeNode.getChildren();
			int childCount = children.size();
			String[] arrayOutput = new String[childCount];
			switch (opeNode.getKey()){
			case "+":
				for (int i=0; i<childCount; i++) {
					if (isOperator(children.get(i))){

						OperatorNode opeChildNode = (OperatorNode) children.get(i);
						if (i!=0 && opeChildNode.getKey().matches("[\\-]") && opeChildNode.getChildren().size()==1){
							if (opeChildNode.getKey().matches("[\\-\\+\\/]")) parens=true;
						}
					}
					if (parens) {arrayOutput[i] = "(" + expression(children.get(i)) + ")"; parens=false;}
					else arrayOutput[i] = expression(children.get(i));
				}
				break;
			case "-":
				arrayOutput[0] = expression(children.get(0));


				if (childCount==2){
					if (isOperator(children.get(1))){
						OperatorNode opeChild1 = (OperatorNode) children.get(1);
						if (opeChild1.getKey().matches("[\\-\\+]")) parens=true;
					}
					if (parens) {arrayOutput[1] = "(" + expression(children.get(1)) + ")"; parens=false;}
					else arrayOutput[1] = expression(children.get(1));
				}
				else return "-" + arrayOutput[0];

				break;
			case "*":
				for (int i=0; i<childCount; i++) {
					if (isOperator(children.get(i))){
						OperatorNode opeChildNode = (OperatorNode) children.get(i);
						if (opeChildNode.getKey().matches("[\\-\\+\\/]")) parens=true;
					}
					if (parens) {arrayOutput[i] = "(" + expression(children.get(i)) + ")"; parens=false;}
					else arrayOutput[i] = expression(children.get(i));
				}
				break;
			case "/":
				for (int i=0; i<childCount; i++) {
					if (isOperator(children.get(i))){
						OperatorNode opeChildNode = (OperatorNode) children.get(i);
						if (opeChildNode.getKey().matches("[\\-\\+\\/\\*]")) parens=true;
					}
					if (parens) {arrayOutput[i] = "(" + expression(children.get(i)) + ")"; parens=false;}
					else arrayOutput[i] = expression(children.get(i));
				}
				break;
			case "^":
				for (int i=0; i<childCount; i++) {
					if (isOperator(children.get(i))){
						OperatorNode opeChildNode = (OperatorNode) children.get(i);
						if (opeChildNode.getKey().matches("[\\-\\+\\/\\*\\^]")) parens=true;
					}
					if (parens) {arrayOutput[i] = "(" + expression(children.get(i)) + ")"; parens=false;}
					else arrayOutput[i] = expression(children.get(i));
				}
				break;
			case "=":
				arrayOutput[0]="(" + expression(children.get(0));
				arrayOutput[1]=expression(children.get(1))+")";
				break;
			default:
				for (int i=0; i<childCount; i++) {
					arrayOutput[i] = expression(children.get(i));
				}
				arrayOutput[0] = "(" + arrayOutput[0];
				arrayOutput[childCount-1] = arrayOutput[childCount-1] + ")";
				output =  opeNode.getKey();

				for (int i=0; i<childCount; i++){
					if (i==0) output = output + arrayOutput[i];
					else output = output + "," + arrayOutput[i];
				}
				return output;
			}

			if (opeNode.getKey().matches("=")) output = arrayOutput[0] + "==" + arrayOutput[childCount-1];
			else output = arrayOutput[0] + opeNode.getKey() + arrayOutput[childCount-1];

		}
		else if(node instanceof Rule){
			Rule ruleNode = (Rule) node;
			String conditionString;
			if (ruleNode.getCondition()!=null) {
				conditionString = " if ";
				if (ruleNode.getCondition().getKey().matches("OR|AND")){
					conditionString = conditionString + expression(ruleNode.getCondition().getChildren().get(0)) + " " + ruleNode.getCondition().getKey() + " " + expression(ruleNode.getCondition().getChildren().get(1)); 
				}
				else if(ruleNode.getCondition().getKey().matches("=")){
					conditionString = conditionString + "("+ expression(ruleNode.getCondition().getChildren().get(0)) + "==" + expression(ruleNode.getCondition().getChildren().get(1)) +")"; 

				}
				else conditionString = conditionString + ruleNode.getCondition().getKey() +"(" +expression(ruleNode.getCondition().getChildren().get(0)) + ")";
			}
			else conditionString = "";

			output = expression(ruleNode.getLhs()) + "=" + expression(ruleNode.getRhs()) + conditionString;
			return output;
		}
		else return "AbstractNode[]";
		return output;
	}


	public static String tree(AbstractNode node) {
		return tree("", node, true, "");
	}

	private static String tree(String prefix, AbstractNode node, boolean isTail, String yetOutput) {
		String key=null;
		if (isNum(node)) { 
			NumNode numNode = (NumNode) node;
			key = Integer.toString(numNode.getKey());
		}
		else if (isVar(node)){
			VarNode varNode = (VarNode) node;
			key = varNode.getKey();
		}
		else if (isDollar(node)){
			DollarNode dollarNode = (DollarNode) node;
			key = dollarNode.getKey();
		}
		else if (isOperator(node)){
			OperatorNode opeNode = (OperatorNode) node;
			key = opeNode.getKey();
		}
		String output = yetOutput + prefix + (isTail ? "└── " : "├── ") + key + "\n";


		if (isOperator(node)) {

			OperatorNode opeNode = (OperatorNode) node;
			ArrayList<AbstractNode> children = opeNode.getChildren();
			int childCount = children.size();

			for (int i=0;i<childCount;i++){
				if (i==childCount-1) output = NodeFunctions.tree(prefix + (isTail ? "    " : "│   "), children.get(i), true, output);
				else output = NodeFunctions.tree(prefix + (isTail ? "    " : "│   "), children.get(i), false, output);
			}
		}


		return output; 
	}


	public static HashMap<DollarNode, AbstractNode> match(AbstractNode lhs, AbstractNode node) throws CloneNotSupportedException{



		HashMap<DollarNode,AbstractNode> map = new HashMap<DollarNode, AbstractNode>();

		if (isNum(lhs)&&isNum(node)){
			NumNode numLhs = (NumNode) lhs;
			NumNode num = (NumNode) node;

			if (numLhs.getKey()==num.getKey()) return map;
			else return null;
		}
		else if (isVar(lhs)&&isVar(node)){
			VarNode varLhs = (VarNode) lhs;
			VarNode var = (VarNode) node;
			if (varLhs.getKey().matches(var.getKey())) return map;
			else return null;
		}
		else if (isDollar(lhs)){
			DollarNode dolLhs = (DollarNode) lhs;
			if (map.get(lhs)!=null){
				if (NodeFunctions.expression(map.get(lhs)).equals(NodeFunctions.expression(node))) return map;
				else return null;
			}
			else {
				map.put((DollarNode) lhs, node);
				return map;
			}
		}
		else if (isOperator(lhs)&&isOperator(node)){
			OperatorNode opeLhs = (OperatorNode) lhs;
			OperatorNode opeNode = (OperatorNode) node;

			int sizeLhs = opeLhs.getChildren().size();
			int sizeOpeNode = opeNode.getChildren().size();

			if (opeLhs.getKey().equals(opeNode.getKey())&&sizeLhs==sizeOpeNode){    			

				HashMap<DollarNode,AbstractNode> mapTemp = (HashMap<DollarNode, AbstractNode>) map;

				for (int i=0; i<sizeLhs; i++){
					HashMap<DollarNode,AbstractNode> childMap = match(opeLhs.getChildren().get(i), opeNode.getChildren().get(i));
					HashMap<DollarNode,AbstractNode> childMapTemp = match(opeLhs.getChildren().get(i), opeNode.getChildren().get(i));


					if (childMap==null) return null;
					else{
						Set<DollarNode> sameSet = childMapTemp.keySet();
						Set<DollarNode> mapTempSet = mapTemp.keySet();
						sameSet.retainAll(mapTempSet);
						if(!sameSet.isEmpty()){
							for (DollarNode sameKey : childMapTemp.keySet()){
								if (match(childMap.get(sameKey), map.get(sameKey))==null){
									return null;
								}
							}
							map.putAll(match(opeLhs.getChildren().get(i), opeNode.getChildren().get(i)));
						}
						else map.putAll(match(opeLhs.getChildren().get(i), opeNode.getChildren().get(i)));
					}
				}
				return map;
			}
			else return null;
		}



		return null;

	}

	public static boolean evalCondition(OperatorNode condition) throws CloneNotSupportedException{

		switch (condition.getKey()){
		case "is_const":
			if (isNum(eval(condition.getChildren().get(0)))) return true;
			else return false;
		case "depends":
			return evalDepends(condition.getChildren().get(0), condition.getChildren().get(1));
		case "AND":
			if (evalCondition((OperatorNode)condition.getChildren().get(0))){
				if (evalCondition((OperatorNode)condition.getChildren().get(1))) return true;
				else return false;}
			else return false;

		case "OR":
			if (evalCondition((OperatorNode)condition.getChildren().get(0)) || evalCondition((OperatorNode)condition.getChildren().get(1))) return true;
			else return false;
		case "TRUE":
			return true;
		case "FALSE":
			return false;
		case "NOT":
			if (evalCondition((OperatorNode) condition.getChildren().get(0))) return false;
			else return true;
		case "=":
			if(isNum(eval(condition.getChildren().get(0))) && isNum(eval(condition.getChildren().get(1)))){
				if (((NumNode) eval(condition.getChildren().get(0))).getKey() == ((NumNode) eval(condition.getChildren().get(1))).getKey() ) return true;
			}
			else return false;
		default:
			return false;
		}


	}

	public static AbstractNode eval(AbstractNode node) throws CloneNotSupportedException{

		if (isNum(node)){
			NumNode num = (NumNode) node;
			if (num.getKey()<0){
				OperatorNode minusOpe = new OperatorNode("-");
				minusOpe.getChildren().add(new NumNode(num.getKey()*(-1)));
				return minusOpe;
			}

		}

		if (isOperator(node)){

			OperatorNode opeNode = (OperatorNode) node;
			for(AbstractNode i : opeNode.getChildren()){
				boolean canBeEvaluated=false;
				if (isNum(i)) canBeEvaluated = true;
				else if (isOperator(i)){
					OperatorNode iOpe = (OperatorNode) i;
					if ((iOpe.getKey().matches("\\-") && iOpe.getChildren().size()==1 && isNum(iOpe.getChildren().get(0)))){
						canBeEvaluated = true;
						NumNode iOpeChild = (NumNode) iOpe.getChildren().get(0);
						int idx=opeNode.getChildren().indexOf(i);
						opeNode.getChildren().set(idx, new NumNode(iOpeChild.getKey() * (-1)));	
					}
				}
				if (!canBeEvaluated) return node;
			}

			if(opeNode.getChildren().size()==2){

				NumNode left = (NumNode) opeNode.getChildren().get(0);
				NumNode right = (NumNode) opeNode.getChildren().get(1);
				int result=0;

				switch (opeNode.getKey()){
				case "+":
					result = left.getKey()+right.getKey();
					break;
				case "-":
					result = left.getKey()-right.getKey();    	
					break;
				case "*":
					result = left.getKey()*right.getKey();
					break;
				case "/":
					if (left.getKey() % right.getKey() == 0){
						result = left.getKey()/right.getKey();
					}
					else return node;
					break;
				case "^":
					result = (int) Math.pow(left.getKey(),right.getKey());
					break;
				default:
					return node;
				}

				NumNode newNode = new NumNode(result);
				return newNode;
			}

			for (int i=0; i<opeNode.getChildren().size();i++){
				opeNode.getChildren().set(i, eval(opeNode.getChildren().get(i)));
			}
			return opeNode;
		}

		else return node;
	}

	public static boolean evalDepends(AbstractNode expr, AbstractNode onWhat) throws CloneNotSupportedException{

		if (isNum(expr) || isNum(onWhat)) return false;
		else {
			if (match(expr, onWhat)!=null) return true;
			else {
				if (isOperator(expr)){
					for (AbstractNode i : ((OperatorNode) expr).getChildren()){
						if (match(i,onWhat)!=null) return true;
						else if (isOperator(i))  return evalDepends((OperatorNode) i, onWhat);
					}
				}
				return false;
			}
		}

	}

	public static AbstractNode rewrite(AbstractNode node, HashMap<DollarNode,AbstractNode> Map) throws CloneNotSupportedException{
		AbstractNode output = node.clone();
		if (isNum(output)) return (NumNode) output;
		else if (isVar(output)) return (VarNode) output;
		else if (isDollar(output)){
			DollarNode dolNode = (DollarNode) output;
			return Map.get(dolNode);
		}
		else if (isOperator(output)){
			OperatorNode opeNode = (OperatorNode) output;
			int childrenCount = opeNode.getChildren().size();
			for (int i=0; i<childrenCount; i++){
				opeNode.getChildren().set(i,rewrite(opeNode.getChildren().get(i), Map));
			}
			return opeNode;
		}
		return output;
	}

	public static AbstractNode expressionToNode(String expression) throws IOException{
		InputStream stream = new ByteArrayInputStream(expression.getBytes(StandardCharsets.UTF_8));

		ANTLRInputStream input = new ANTLRInputStream(stream);
		ExprLexer lexer = new ExprLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ExprParser parser = new ExprParser(tokens);

		ParseTree tree = parser.expr();


		Visitor visitor = new Visitor();
		AbstractNode rootNode = visitor.visit(tree);

		return rootNode;
	}

	public static ArrayList<Rule> textToArray(String rules) throws IOException{
		ArrayList<Rule> ruleList = new ArrayList<Rule>();
		String lines[] = rules.split("\\r?\\n");


		for (String str : lines){
			InputStream stream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
			ANTLRInputStream input = new ANTLRInputStream(stream);
			ExprLexer lexer = new ExprLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			ExprParser parser = new ExprParser(tokens);

			ParseTree tree = parser.expr();

			stream.close();
			Visitor visitor = new Visitor();
			AbstractNode rootNode = visitor.visit(tree);

			ruleList.add((Rule) rootNode);
		}
		return ruleList;	
	}

	public static AbstractNode cannonical(AbstractNode node) throws CloneNotSupportedException{

		if (isOperator(node)){
			OperatorNode opeNode = (OperatorNode) node.clone();

			ArrayList<NumNode> numList = new ArrayList<NumNode>();
			ArrayList<VarNode> varList = new ArrayList<VarNode>();
			ArrayList<DollarNode> dolList = new ArrayList<DollarNode>();
			ArrayList<OperatorNode> opeList = new ArrayList<OperatorNode>();

			if (opeNode.getKey().matches("[\\+\\*]")){
				for (int i=0; i<opeNode.getChildren().size();i++){
					if (isOperator(opeNode.getChildren().get(i))) {
						opeNode.getChildren().set(i, cannonical(opeNode.getChildren().get(i)));
						opeList.add((OperatorNode) opeNode.getChildren().get(i));
					}
					else if(isNum(opeNode.getChildren().get(i))) numList.add((NumNode) opeNode.getChildren().get(i));
					else if(isVar(opeNode.getChildren().get(i))) varList.add((VarNode) opeNode.getChildren().get(i));
					else if(isDollar(opeNode.getChildren().get(i))) dolList.add((DollarNode) opeNode.getChildren().get(i));
				}

				Collections.sort(varList);
				Collections.sort(dolList);

				ArrayList<AbstractNode> sortedChildren = new ArrayList<AbstractNode>();
				if (!numList.isEmpty()) sortedChildren.addAll(numList);
				if (!varList.isEmpty()) sortedChildren.addAll(varList);
				if (!dolList.isEmpty()) sortedChildren.addAll(dolList);
				if (!opeList.isEmpty()) sortedChildren.addAll(opeList);

				if (!sortedChildren.isEmpty()) opeNode.setChildren(sortedChildren);

			}
			else {
				for (int i=0; i<opeNode.getChildren().size();i++){
					if (NodeFunctions.isOperator(opeNode.getChildren().get(i))) opeNode.getChildren().set(i, cannonical(opeNode.getChildren().get(i)));
				}
			}



			return opeNode;
		}
		else return node;

	}

	public static boolean isNum(AbstractNode node){
		if (node instanceof NumNode) return true;
		else return false;
	}

	public static boolean isVar(AbstractNode node){
		if (node instanceof VarNode) return true;
		else return false;
	}

	public static boolean isOperator(AbstractNode node){
		if (node instanceof OperatorNode) return true;
		else return false;
	}

	public static boolean isDollar(AbstractNode node){
		if (node instanceof DollarNode) return true;
		else return false;
	}
}