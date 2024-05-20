package com.itextpdf.pdfchain.bql.relational;

import com.itextpdf.pdfchain.blockchain.Record;
import com.itextpdf.pdfchain.bql.AbstractBQLOperator;

import java.util.ArrayList;
import java.util.Collection;

/**
 * BQL Relational == operator
 */
public class Equal extends AbstractBQLOperator {

    private String fieldName;
    private Object fieldValue;

    public Equal(String fieldName, Object fieldValue) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    @Override
    public Collection<Record> apply(Collection<Record> in) {
        Collection<Record> out = new ArrayList<>();
        for (Record r : in) {
            if (r.containsKey(fieldName) && r.get(fieldName).equals(fieldValue))
                out.add(r);
        }
        return out;
    }
}
