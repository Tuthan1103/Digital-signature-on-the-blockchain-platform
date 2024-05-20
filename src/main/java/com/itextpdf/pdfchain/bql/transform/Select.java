package com.itextpdf.pdfchain.bql.transform;

import com.itextpdf.pdfchain.blockchain.Record;
import com.itextpdf.pdfchain.bql.AbstractBQLOperator;
import com.itextpdf.pdfchain.bql.IBQLOperator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Select extends AbstractBQLOperator {

    private String[] fieldNames;

    public Select(AbstractBQLOperator from, String[] fieldNames) {
        this.addChild(from);
        this.fieldNames = fieldNames;
    }

    @Override
    public Collection<Record> apply(Collection<Record> in) {
        IBQLOperator from = getChild(0);
        Set<Record> out = new HashSet<>();
        for (Record r : from.apply(in)) {
            Record rCopy = retainAll(r, fieldNames);
            out.add(rCopy);
        }
        return out;
    }

    private Record retainAll(Record in, String[] keys) {
        Record out = new Record();
        for (String k : keys) {
            if (in.containsKey(k))
                out.put(k, in.get(k));
        }
        return out;
    }

}
