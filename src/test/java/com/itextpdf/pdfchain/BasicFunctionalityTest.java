package com.itextpdf.pdfchain;
import com.itextpdf.pdfchain.blockchain.IBlockChain;
import com.itextpdf.pdfchain.blockchain.MultiChain;
import com.itextpdf.pdfchain.blockchain.Record;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.itextpdf.pdfchain.pdfchain.PdfChain;
import com.itextpdf.pdfchain.sign.AbstractExternalSignature;
import com.itextpdf.pdfchain.sign.DefaultExternalSignature;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class BasicFunctionalityTest {

    @BeforeClass
    public static void beforeClass() {
    }

    @Test
    public void putOnChainTest() throws Exception {
        IBlockChain mc = new MultiChain(
                "http://127.0.0.1",
                9740,
                "chain-dev",
                "stream-dev",
                "multichainrpc",
                "TVnqseBcHsYjeTU1ACVmF75nCviRJ9UmLdubGApjtsD");

        InputStream keystoreInputStream = BasicFunctionalityTest.class.getClassLoader().getResourceAsStream("ks");
        InputStream inputFileStream = BasicFunctionalityTest.class.getClassLoader().getResourceAsStream("input.pdf");

        AbstractExternalSignature sgn = new DefaultExternalSignature(keystoreInputStream, "demo", "password");

        PdfChain chain = new PdfChain(mc, sgn);

        // put a document on the chain
        boolean wasAdded = chain.put(inputFileStream);
        assertTrue(wasAdded);

        // check whether the chain now contains this value
        boolean isEmpty = chain.get("z�L{�Wd=��\u007F\u0010��G�").isEmpty();
        assertFalse(isEmpty);

        // check signature
        for(Record r : chain.get("z�L{�Wd=��\u007F\u0010��G�")){
            if(chain.isSigned(r, sgn.getPublicKey()))
                System.out.println("This record is signed");
        }

    }
}
