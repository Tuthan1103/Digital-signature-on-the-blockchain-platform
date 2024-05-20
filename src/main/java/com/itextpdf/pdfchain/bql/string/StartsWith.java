package com.itextpdf.pdfchain.bql.string;

import com.itextpdf.pdfchain.blockchain.Record;
import com.itextpdf.pdfchain.bql.AbstractBQLOperator;

import java.util.ArrayList;
import java.util.Collection;

public class StartsWith extends AbstractBQLOperator {

    private String fieldName;
    private String suffix;

    public StartsWith(String fieldName, String suffix) {
        this.fieldName = fieldName;
        this.suffix = suffix;
    }

    @Override
    public Collection<Record> apply(Collection<Record> in) {
        Collection<Record> out = new ArrayList<>();
        for (Record r : in) {
            if (r.containsKey(fieldName) && r.get(fieldName).toString().startsWith(suffix))
                out.add(r);
        }
        return out;
    }
}
