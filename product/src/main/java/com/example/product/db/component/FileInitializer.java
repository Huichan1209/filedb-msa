package com.example.product.db.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class FileInitializer
{
    @Value("${config.path.db}/dat/product.dat")
    private String DAT_PATH;

    @Value("${config.path.db}/idx/product.idx")
    private String IDX_PATH;

    @Value("${config.path.db}/txn/product.txn")
    private String TXN_PATH;

    public FileInitializer() throws IOException
    {
        File datFile = new File(DAT_PATH);
        File idxFile = new File(IDX_PATH);
        File txnFile = new File(TXN_PATH);

        if(!datFile.exists()) { datFile.createNewFile(); }
        if(!idxFile.exists()) { datFile.createNewFile(); }
        if(!txnFile.exists()) { datFile.createNewFile(); }
    }
}
