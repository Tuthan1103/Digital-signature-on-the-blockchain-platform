package com.itextpdf.pdfchain.bql.relational;

import com.itextpdf.pdfchain.blockchain.Record;
import com.itextpdf.pdfchain.bql.AbstractBQLOperator;

import java.util.Collection;

public class Star extends AbstractBQLOperator {

    @Override
    public Collection<Record> apply(Collection<Record> in) {
        return in;
    }

}
