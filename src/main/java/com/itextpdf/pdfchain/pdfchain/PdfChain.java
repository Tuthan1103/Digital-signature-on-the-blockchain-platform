package com.itextpdf.pdfchain.pdfchain;

import com.itextpdf.kernel.xmp.impl.Base64;
import com.itextpdf.pdfchain.blockchain.IBlockChain;
import com.itextpdf.pdfchain.blockchain.Record;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.pdfchain.sign.AbstractExternalSignature;
import com.itextpdf.pdfchain.sign.NoOpSignature;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides blockchain functionality for pdf files
 */
public class PdfChain {

    private final AbstractExternalSignature externalSignature;
    private final IBlockChain blockChain;

    /**
     * Construct a new pdfchain.PdfChain object with a given IBlockchain implempentation and AbstractExternalSignature implementation
     *
     * @param blockChain        the underlying blockchain to be used
     * @param externalSignature the signing and hashing methods to be used
     */
    public PdfChain(IBlockChain blockChain, AbstractExternalSignature externalSignature) {
        this.blockChain = blockChain;
        this.externalSignature = externalSignature;
    }

    /**
     * Construct a new pdfchain.PdfChain object with a given IBlockchain implementation
     *
     * @param blockChain the underlying blockchain to be used
     */
    public PdfChain(IBlockChain blockChain) {
        this.blockChain = blockChain;
        this.externalSignature = new NoOpSignature();
    }

    /**
     * Puts a pdfFile on the blockchain
     *
     * @param pdfFile the pdf file to be put on the blockchain
     * @return true iff the data was successfully put on the blockchain
     * @throws IOException              if the file could not be read, or is not a valid PDF document
     * @throws GeneralSecurityException if the signature could not be generated
     */
    public boolean put(File pdfFile) throws IOException, GeneralSecurityException {
        return put(new FileInputStream(pdfFile), new HashMap<String, String>());
    }

    /**
     * Puts a pdfFile on the blockchain
     *
     * @param pdfFile the pdf file to be put on the blockchain
     * @return true iff the data was successfully put on the blockchain
     * @throws IOException              if the file could not be read, or is not a valid PDF document
     * @throws GeneralSecurityException if the signature could not be generated
     */
    public boolean put(InputStream pdfFile) throws IOException, GeneralSecurityException {
        return put(pdfFile, new HashMap<String, String>());
    }

    /**
     * Get all information related to a specific PDF File from the blockchain
     *
     * @param pdfFile the input file
     * @return a List of records related to the given File
     * @throws IOException if the file could not be read or is not a valid PDF document
     */
    public List<Record> get(File pdfFile) throws IOException {
        return get(new FileInputStream(pdfFile));
    }

    /**
     * Get all information related to a specific PDF File from the blockchain
     *
     * @param pdfFile the file being queried
     * @return a List of records related to the given File
     * @throws IOException if the file could not be read or is not a valid PDF document     *
     */
    public List<Record> get(InputStream pdfFile) throws IOException {

        // open document
        PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdfFile));

        // get document properties
        PdfArray idArr = pdfDocument.getTrailer().getAsArray(PdfName.ID);
        String id1 = idArr.getAsString(0).toString();

        // close document
        pdfDocument.close();

        // return
        return blockChain.get(id1);
    }

    /**
     * Get all information related to a specific PDF document from the blockchain
     *
     * @param id1 the first ID of the PDF document
     * @return a List of records related to the given ID
     */
    public List<Record> get(String id1) {
        return blockChain.get(id1);
    }

    /**
     * Puts a pdfFile on the blockchain
     *
     * @param pdfFile   the pdf file being put on the blockchain
     * @param extraData extra attributes being added on the blockchain
     * @return true iff the data was successfully added to the blockchain
     * @throws IOException              if the file could not be read, or is not a valid PDF document
     * @throws GeneralSecurityException if the signature could not be generated
     */
    public boolean put(InputStream pdfFile, Map<String, String> extraData) throws IOException, GeneralSecurityException {

        // open document
        PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdfFile));

        // get document properties
        PdfArray idArr = pdfDocument.getTrailer().getAsArray(PdfName.ID);
        String id1 = idArr.getAsString(0).toString();
        String id2 = idArr.getAsString(1).toString();
        String hash = new String(externalSignature.hash(pdfFile));
        String signedHash = new String(externalSignature.encryptHash(pdfFile));

        // close document
        pdfDocument.close();

        // build data to put on blockchain
        Record dataOnChain = new Record();
        for (Map.Entry<String, String> en : extraData.entrySet()) {
            dataOnChain.put(en.getKey(), en.getValue());
        }
        dataOnChain.put("id1", id1);
        dataOnChain.put("id2", id2);
        dataOnChain.put("hsh", hash);
        dataOnChain.put("key", externalSignature.getPublicKey() == null ? "" : new String(externalSignature.getPublicKey().getEncoded()));
        dataOnChain.put("hshalgo", externalSignature.getHashAlgorithm());
        dataOnChain.put("sgnalgo", externalSignature.getEncryptionAlgorithm());
        dataOnChain.put("shsh", signedHash);

        // call blockchain implementation
        return blockChain.put(id1, dataOnChain);
    }

    /**
     * Check whether a given record is signed by a given public key
     * @param r the record to check
     * @param key the public key to check against
     * @return true iff the record was signed by the public key, false otherwise
     */
    public boolean isSigned(Record r, Key key){
        if(r == null || key == null)
            return false;
        if(!r.containsKey("shsh"))
            return false;
        if(!r.containsKey("sgnalgo"))
            return false;
        if(!r.containsKey("hsh"))
            return false;
        try {
            String alg = r.get("sgnalgo").toString();
            byte[] enc = r.get("shsh").toString().getBytes();
            byte[] dec = decryptHash(enc, key, alg);
            return r.get("hsh").toString().equals(new String(dec));
        }catch (Exception ex){}
        return false;
    }

    /**
     * decrypt the signed hash for a given pdf file
     * @param encrypted the encrypted hash
     * @param pubKey the public key to verify against
     * @param encryptionAlgorithm  the encryption algorithm to undo
     * @return
     * @throws Exception
     */
    private byte[] decryptHash(byte[] encrypted, Key pubKey, String encryptionAlgorithm) throws Exception {
        Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, pubKey);
        // return
        return cipher.doFinal(Base64.decode(encrypted));
    }
}
