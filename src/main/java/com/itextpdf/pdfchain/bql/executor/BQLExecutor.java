package com.itextpdf.pdfchain.bql.executor;

import com.itextpdf.pdfchain.blockchain.IBlockChain;
import com.itextpdf.pdfchain.blockchain.Record;
import com.itextpdf.pdfchain.bql.AbstractBQLOperator;
import com.itextpdf.pdfchain.bql.logical.And;
import com.itextpdf.pdfchain.bql.relational.EqualID;
import com.itextpdf.pdfchain.bql.sort.SortBy;
import com.itextpdf.pdfchain.bql.transform.Select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

/**
 * BQLExecutor for BQL
 * This class executes statements in the form of abstract syntax trees.
 * Either build the tree yourself, or use the BQLCompiler to build it for you.
 */
public class BQLExecutor {

    private IBlockChain blockchain;

    public BQLExecutor(IBlockChain blockchain) {
        this.blockchain = blockchain;
    }

    public Collection<Record> execute(AbstractBQLOperator op) {
        Object id = useID(op);
        Collection<Record> db = (id == null) ? blockchain.all() : blockchain.get(id.toString());
        return op.apply(db);
    }

    private List<AbstractBQLOperator> leaves(AbstractBQLOperator root) {
        List<AbstractBQLOperator> out = new ArrayList<>();
        Stack<AbstractBQLOperator> operatorStack = new Stack<>();
        operatorStack.push(root);
        while (!operatorStack.isEmpty()) {
            AbstractBQLOperator op = operatorStack.pop();
            if (op.getChildren().isEmpty())
                out.add(op);
            else {
                for (AbstractBQLOperator c : op.getChildren())
                    operatorStack.push(c);
            }
        }
        return out;
    }

    private Object useID(AbstractBQLOperator root) {
        for (AbstractBQLOperator leaf : leaves(root)) {
            AbstractBQLOperator tmp = leaf;
            if (!(tmp instanceof EqualID))
                continue;
            boolean pathUp = true;
            while (tmp.getParent() != null) {
                AbstractBQLOperator parent = tmp.getParent();
                if (!enforcesID(parent)) {
                    pathUp = false;
                    break;
                }
                tmp = parent;
            }
            if (pathUp)
                return ((EqualID) leaf).getSelectedValue();
        }
        return null;
    }

    private boolean enforcesID(AbstractBQLOperator operator) {
        return (operator instanceof And) ||
                (operator instanceof Select) ||
                (operator instanceof SortBy);
    }
}
