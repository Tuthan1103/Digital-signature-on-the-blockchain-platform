package com.itextpdf.pdfchain.bql.executor;

import com.itextpdf.pdfchain.bql.AbstractBQLOperator;
import com.itextpdf.pdfchain.bql.logical.And;
import com.itextpdf.pdfchain.bql.logical.Or;
import com.itextpdf.pdfchain.bql.relational.Equal;
import com.itextpdf.pdfchain.bql.relational.Greater;
import com.itextpdf.pdfchain.bql.relational.GreaterOrEqual;
import com.itextpdf.pdfchain.bql.relational.NotEqual;
import com.itextpdf.pdfchain.bql.relational.Smaller;
import com.itextpdf.pdfchain.bql.relational.SmallerOrEqual;
import com.itextpdf.pdfchain.bql.relational.Star;
import com.itextpdf.pdfchain.bql.sort.SortBy;
import com.itextpdf.pdfchain.bql.string.EndsWith;
import com.itextpdf.pdfchain.bql.string.StartsWith;
import com.itextpdf.pdfchain.bql.transform.Select;

import java.util.List;
import java.util.Stack;

/**
 * Compiler for BQL
 */
public class BQLCompiler {

    /**
     * Compiles an expression written in BQL into an AbstractBQLOperator which can then be executed with BQLExecutor
     *
     * @param expression expression to be compiled
     * @return the root of a tree of BQL operators
     */
    public static AbstractBQLOperator compile(String expression) {
        List<BQLTokenizer.Token> tokens = ShuntingYard.postfix(BQLTokenizer.tokenize(expression));

        Stack<Object> tmp = new Stack<>();
        for (BQLTokenizer.Token t : tokens) {
            if (t.getType() == BQLTokenizer.Type.OPERATOR) {
                String operator = t.getText().toUpperCase();
                switch (operator) {
                    case "*":
                        tmp.push(buildStarOperator());
                        break;
                    case "AND":
                        tmp.push(buildAnd(tmp));
                        break;
                    case "OR":
                        tmp.push(buildOr(tmp));
                        break;
                    case ">":
                        tmp.push(buildGreater(tmp));
                        break;
                    case ">=":
                        tmp.push(buildGreaterOrEqual(tmp));
                        break;
                    case "<":
                        tmp.push(buildSmaller(tmp));
                        break;
                    case "<=":
                        tmp.push(buildSmallerOrEqual(tmp));
                        break;
                    case "==":
                        tmp.push(buildEquals(tmp));
                        break;
                    case "!=":
                        tmp.push(buildNotEquals(tmp));
                        break;
                    case "SORT":
                        tmp.push(buildSort(tmp));
                        break;
                    case "SELECT":
                        tmp.push(buildSelect(tmp));
                        break;
                    case "STARTS_WITH":
                        tmp.push(buildStartsWith(tmp));
                        break;
                    case "ENDS_WITH":
                        tmp.push(buildEndsWith(tmp));
                        break;
                }
            } else {
                tmp.push(t);
            }
        }
        if (tmp.size() != 1)
            throw new IllegalArgumentException("Invalid input '" + expression + "'");
        return (AbstractBQLOperator) tmp.pop();
    }

    private static AbstractBQLOperator buildStarOperator() {
        return new Star();
    }

    private static AbstractBQLOperator buildSelect(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator SELECT");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isOperator(arg0) || !isArray(arg1))
            throw new IllegalArgumentException("Invalid argument(s) for operator SELECT");
        return new Select((AbstractBQLOperator) arg0, ((BQLTokenizer.Token) arg1).getTexts());
    }

    private static AbstractBQLOperator buildStartsWith(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator STARTS_WITH");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isVariable(arg1) || !isString(arg0))
            throw new IllegalArgumentException("Invalid argument(s) for operator STARTS_WITH");
        String val = ((BQLTokenizer.Token) arg0).getText();
        val = val.substring(1, val.length() - 1);
        return new StartsWith(((BQLTokenizer.Token) arg1).getText(), val);
    }

    private static AbstractBQLOperator buildEndsWith(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator ENDS_WITH");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isVariable(arg1) || !isString(arg0))
            throw new IllegalArgumentException("Invalid argument(s) for operator ENDS_WITH");
        String val = ((BQLTokenizer.Token) arg0).getText();
        val = val.substring(1, val.length() - 1);
        return new EndsWith(((BQLTokenizer.Token) arg1).getText(), val);
    }

    private static AbstractBQLOperator buildSort(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator SORT");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isVariable(arg0) || !isOperator(arg1))
            throw new IllegalArgumentException("Invalid argument(s) for operator SORT");
        return new SortBy((AbstractBQLOperator) arg1, ((BQLTokenizer.Token) arg0).getText());
    }

    private static AbstractBQLOperator buildAnd(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator AND");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isOperator(arg0) || !isOperator(arg1))
            throw new IllegalArgumentException("Invalid argument(s) for operator AND");
        return new And((AbstractBQLOperator) arg0, (AbstractBQLOperator) arg1);
    }

    private static AbstractBQLOperator buildOr(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator OR");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isOperator(arg0) || !isOperator(arg1))
            throw new IllegalArgumentException("Invalid argument(s) for operator OR");
        return new Or((AbstractBQLOperator) arg0, (AbstractBQLOperator) arg1);
    }

    private static AbstractBQLOperator buildGreater(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator >");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isNumber(arg0) || !isVariable(arg1))
            throw new IllegalArgumentException("Invalid argument(s) for operator >");
        return new Greater(((BQLTokenizer.Token) arg1).getText(),
                Double.parseDouble(((BQLTokenizer.Token) arg0).getText()));
    }

    private static AbstractBQLOperator buildSmaller(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator <");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isNumber(arg0) || !isVariable(arg1))
            throw new IllegalArgumentException("Invalid argument for operator <");
        return new Smaller(((BQLTokenizer.Token) arg1).getText(),
                Double.parseDouble(((BQLTokenizer.Token) arg0).getText()));
    }

    private static AbstractBQLOperator buildGreaterOrEqual(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator >=");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isNumber(arg0) || !isVariable(arg1))
            throw new IllegalArgumentException("Invalid argument for operator >=");
        return new GreaterOrEqual(((BQLTokenizer.Token) arg1).getText(),
                Double.parseDouble(((BQLTokenizer.Token) arg0).getText()));
    }

    private static AbstractBQLOperator buildSmallerOrEqual(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator <=");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isNumber(arg0) || !isVariable(arg1))
            throw new IllegalArgumentException("Invalid argument for operator <=");
        return new SmallerOrEqual(((BQLTokenizer.Token) arg1).getText(),
                Double.parseDouble(((BQLTokenizer.Token) arg0).getText()));
    }

    private static AbstractBQLOperator buildNotEquals(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator !=");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isVariable(arg1))
            throw new IllegalArgumentException("Invalid argument for operator !=");
        if (!isString(arg0) && !isNumber(arg0))
            throw new IllegalArgumentException("Invalid argument for operator !=");

        Object val = null;

        // text
        if (isString(arg0)) {
            val = ((BQLTokenizer.Token) arg0).getText();
            val = ((String) val).substring(1, ((String) val).length() - 1);

        }

        // number
        if (isNumber(arg0))
            val = Double.parseDouble(((BQLTokenizer.Token) arg0).getText());

        return new NotEqual(((BQLTokenizer.Token) arg1).getText(), val);
    }

    private static AbstractBQLOperator buildEquals(Stack<Object> stk) {
        if (stk.size() < 2)
            throw new IllegalArgumentException("Not enough arguments for operator ==");
        Object arg0 = stk.pop();
        Object arg1 = stk.pop();
        if (!isVariable(arg1))
            throw new IllegalArgumentException("Invalid argument for operator ==");
        if (!isString(arg0) && !isNumber(arg0))
            throw new IllegalArgumentException("Invalid argument for operator ==");

        Object val = null;

        // text
        if (isString(arg0)) {
            val = ((BQLTokenizer.Token) arg0).getText();
            val = ((String) val).substring(1, ((String) val).length() - 1);
        }

        // number
        if (isNumber(arg0))
            val = Double.parseDouble(((BQLTokenizer.Token) arg0).getText());

        return new Equal(((BQLTokenizer.Token) arg1).getText(), val);
    }


    private static boolean isVariable(Object o) {
        return (o instanceof BQLTokenizer.Token) && ((BQLTokenizer.Token) o).getType() == BQLTokenizer.Type.VARIABLE;
    }

    private static boolean isNumber(Object o) {
        return (o instanceof BQLTokenizer.Token) && ((BQLTokenizer.Token) o).getType() == BQLTokenizer.Type.NUMBER;
    }

    private static boolean isString(Object o) {
        return (o instanceof BQLTokenizer.Token) && ((BQLTokenizer.Token) o).getType() == BQLTokenizer.Type.STRING;
    }

    private static boolean isArray(Object o) {
        return (o instanceof BQLTokenizer.Token) && ((BQLTokenizer.Token) o).getType() == BQLTokenizer.Type.ARRAY;
    }

    private static boolean isOperator(Object o) {
        return o instanceof AbstractBQLOperator;
    }

}
