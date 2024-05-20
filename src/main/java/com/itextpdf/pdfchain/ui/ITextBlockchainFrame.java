package com.itextpdf.pdfchain.ui;

import com.itextpdf.pdfchain.blockchain.IBlockChain;
import com.itextpdf.pdfchain.blockchain.MultiChain;
import com.itextpdf.pdfchain.blockchain.Record;
import com.itextpdf.pdfchain.bql.AbstractBQLOperator;
import com.itextpdf.pdfchain.bql.executor.BQLCompiler;
import com.itextpdf.pdfchain.bql.executor.BQLExecutor;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import com.itextpdf.pdfchain.pdfchain.PdfChain;
import com.itextpdf.pdfchain.sign.DefaultExternalSignature;
import com.itextpdf.pdfchain.ui.dialog.BQLQueryDialog;
import com.itextpdf.pdfchain.ui.dialog.KeystoreDialog;
import com.itextpdf.pdfchain.ui.filechooser.JKeystoreFileChooser;
import com.itextpdf.pdfchain.ui.filechooser.JPdfFileChooser;
import com.itextpdf.pdfchain.ui.model.JBlockchainTableModel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

class ITextBlockchainFrame extends JFrame {

    private JTable resultsTable;

    private IBlockChain blockchainImpl = new MultiChain(
            "http://127.0.0.1",
            4352,
            "chain1",
            "stream1",
            "multichainrpc",
            "BHcXLKwR218R883P6pjiWdBffdMx398im4R8BEwfAxMm");

    ITextBlockchainFrame() {
        initComponents();
    }

    private void initComponents() {
        // a container to put all JXTaskPane together
        JXTaskPaneContainer taskPaneContainer = new JXTaskPaneContainer();

        // create file taskpane
        JXTaskPane actionPane = new JXTaskPane();
        actionPane.setTitle("File");
        actionPane.add(putFileAction());
        actionPane.add(getFileAction());
        //actionPane.add(getIDAction());
        taskPaneContainer.add(actionPane);

        // create signature taskpane
        JXTaskPane signaturePane = new JXTaskPane();
        signaturePane.setTitle("Signature");
        signaturePane.add(signAction());
        signaturePane.add(verifyAction());
        taskPaneContainer.add(signaturePane);

        // create workflow taskpane
        JXTaskPane workflowPane = new JXTaskPane();
        workflowPane.setTitle("BQL");
        workflowPane.add(queryAction());
        taskPaneContainer.add(workflowPane);

        // put the action list on the left
        add(taskPaneContainer, BorderLayout.EAST);

        // and contentpane
        resultsTable = new JTable(new JBlockchainTableModel());
        add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * PUT
     */
    private Action putFileAction() {
        Icon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(this.getClass().getClassLoader().getResourceAsStream("img/cloud_put.png")));
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return new AbstractAction("put", icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                putFile();
            }
        };
    }

    private void putFile() {

        PdfChain pdfChain = new PdfChain(blockchainImpl);
        JFileChooser jFileChooser = new JPdfFileChooser();
        int retVal = jFileChooser.showOpenDialog(this);
        if (retVal != JFileChooser.APPROVE_OPTION)
            return;

        File pdfFile = jFileChooser.getSelectedFile();
        try {
            pdfChain.put(pdfFile);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        try {
            java.util.List<Record> records = pdfChain.get(pdfFile);
            resultsTable.setModel(new JBlockchainTableModel().setData(records));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * GET
     */
    private Action getFileAction() {
        Icon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(this.getClass().getClassLoader().getResourceAsStream("img/cloud_get.png")));
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return new AbstractAction("get", icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                getFile();
            }
        };
    }

    private void getFile() {
        PdfChain pdfChain = new PdfChain(blockchainImpl);
        JFileChooser jFileChooser = new JPdfFileChooser();
        int retVal = jFileChooser.showOpenDialog(this);
        if (retVal != JFileChooser.APPROVE_OPTION)
            return;

        File pdfFile = jFileChooser.getSelectedFile();
        try {
            java.util.List<Record> records = pdfChain.get(pdfFile);
            resultsTable.setModel(new JBlockchainTableModel().setData(records));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * SIGN
     */
    private Action signAction() {
        Icon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(this.getClass().getClassLoader().getResourceAsStream("img/document_sign.png")));
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return new AbstractAction("sign", icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                sign();
            }
        };
    }

    private void sign() {

        // open dialog for selecting a keystore
        JFileChooser jFileChooser = new JKeystoreFileChooser();
        int retVal = jFileChooser.showOpenDialog(this);
        if (retVal != JFileChooser.APPROVE_OPTION)
            return;

        // open dialog for selecting user from keystore
        KeystoreDialog keystoreDialog = new KeystoreDialog(this, true);
        keystoreDialog.setTitle("Enter your credentials");
        keystoreDialog.setVisible(true);
        if (keystoreDialog.getUsername().isEmpty() || keystoreDialog.getPassword().isEmpty())
            return;

        // set up a signature provider
        DefaultExternalSignature signature;
        try {
            signature = new DefaultExternalSignature(new FileInputStream(jFileChooser.getSelectedFile()), keystoreDialog.getUsername(), keystoreDialog.getPassword());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Keystore was tampered with, or password was incorrect.",
                    "IOException",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // set up a pdf chain
        PdfChain pdfChain = new PdfChain(blockchainImpl, signature);

        jFileChooser = new JPdfFileChooser();
        retVal = jFileChooser.showOpenDialog(this);
        if (retVal != JFileChooser.APPROVE_OPTION)
            return;

        File pdfFile = jFileChooser.getSelectedFile();
        try {
            pdfChain.put(pdfFile);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        try {
            java.util.List<Record> records = pdfChain.get(pdfFile);
            resultsTable.setModel(new JBlockchainTableModel().setData(records));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * VERIFY
     */
    private Action verifyAction() {
        Icon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(this.getClass().getClassLoader().getResourceAsStream("img/document_sign.png")));
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return new AbstractAction("verify", icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                verify();
            }
        };
    }

    private void verify() {
        sign();
    }

    /**
     * QUERY
     */
    private Action queryAction() {
        Icon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(this.getClass().getClassLoader().getResourceAsStream("img/cloud_find.png")));
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return new AbstractAction("query", icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                query();
            }
        };
    }

    private void query() {
        BQLQueryDialog dialog = new BQLQueryDialog(this, true);
        dialog.setTitle("Enter a BQL statement");
        dialog.setVisible(true);
        String q = dialog.getQuery();
        if (q.isEmpty())
            return;

        AbstractBQLOperator operator = null;
        try {
            operator = BQLCompiler.compile(q);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        BQLExecutor exe = new BQLExecutor(blockchainImpl);
        Collection<Record> records = exe.execute(operator);
        resultsTable.setModel(new JBlockchainTableModel().setData(records));
    }
}
