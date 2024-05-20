package com.itextpdf.pdfchain;
import com.itextpdf.pdfchain.blockchain.IBlockChain;
import com.itextpdf.pdfchain.blockchain.MultiChain;
import com.itextpdf.pdfchain.blockchain.Record;
import com.itextpdf.pdfchain.bql.AbstractBQLOperator;
import com.itextpdf.pdfchain.bql.executor.BQLCompiler;
import com.itextpdf.pdfchain.bql.executor.BQLExecutor;
import com.itextpdf.pdfchain.bql.logical.And;
import com.itextpdf.pdfchain.bql.logical.Or;
import com.itextpdf.pdfchain.bql.relational.EqualID;
import com.itextpdf.pdfchain.bql.relational.Greater;
import com.itextpdf.pdfchain.bql.relational.Smaller;
import com.itextpdf.pdfchain.bql.sort.SortBy;
import com.itextpdf.pdfchain.bql.transform.Select;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

@Category(IntegrationTest.class)
public class BQLFunctionalityTest {

    @BeforeClass
    public static void beforeClass() {
    }

    @Test
    public void queryBlockChainTestA() throws IOException, GeneralSecurityException {
        IBlockChain mc = new MultiChain(
                "http://127.0.0.1",
                4352,
                "chain1",
                "stream1",
                "multichainrpc",
                "BHcXLKwR218R883P6pjiWdBffdMx398im4R8BEwfAxMm");

        // build query
        AbstractBQLOperator op = new SortBy(new Select(
                new And(
                        new Or(
                                new Greater("confirmations", 10),
                                new Smaller("confirmations", 5)
                        ),
                        new EqualID("z�L{�Wd=��\u007F\u0010��G�")
                ),
                new String[]{"id1", "id2", "blocktime", "confirmations"}
        ),
                "confirmations");

        // build executor
        BQLExecutor exe = new BQLExecutor(mc);

        // execute query
        boolean isEmpty = exe.execute(op).isEmpty();
        assertFalse(isEmpty);
    }

    @Test
    public void queryBlockChainTestB() throws IOException, GeneralSecurityException {
        IBlockChain mc = new MultiChain(
                "http://127.0.0.1",
                4352,
                "chain1",
                "stream1",
                "multichainrpc",
                "BHcXLKwR218R883P6pjiWdBffdMx398im4R8BEwfAxMm");

        // build query
        AbstractBQLOperator op = BQLCompiler.compile("SELECT [id1, id2, confirmations,hsh]( confirmations > 10 AND confirmations < 50 ) SORT confirmations");

        // build executor
        BQLExecutor exe = new BQLExecutor(mc);

        // execute query
        Collection<Record> resultSet = exe.execute(op);
        assertFalse(resultSet.isEmpty());
    }
}
