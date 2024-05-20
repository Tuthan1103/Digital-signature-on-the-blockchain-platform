package com.itextpdf.pdfchain.bql.relational;

import com.itextpdf.pdfchain.blockchain.Record;
import com.itextpdf.pdfchain.bql.AbstractBQLOperator;

import java.util.ArrayList;
import java.util.Collection;

/**
 * BQL Relational &gt;= operator
 */
public class GreaterOrEqual extends AbstractBQLOperator {

    private String fieldName;
    private Object fieldValue;

    public GreaterOrEqual(String fieldName, Object fieldValue) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    @Override
    public Collection<Record> apply(Collection<Record> in) {
        Collection<Record> out = new ArrayList<>();
        for (Record r : in) {
            if (r.containsKey(fieldName)) {
                Object val = r.get(fieldName);
                if (val instanceof Number) {
                    Number valN = (Number) val;
                    Number fldN = (Number) fieldValue;
                    if (cmpNumbers(valN, fldN) >= 0)
                        out.add(r);
                }
            }
        }
        return out;
    }

    private int cmpNumbers(Number n0, Number n1) {
        return Double.compare(n0.doubleValue(), n1.doubleValue());
    }
}
