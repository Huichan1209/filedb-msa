package com.example.product.db.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Component("fileInitializer")
public class FileInitializer
{
    public FileInitializer(@Value("${config.db.path}/dat/product.dat") String DAT_PATH,
                           @Value("${config.db.path}/idx/product.idx") String IDX_PATH,
                           @Value("${config.db.path}/txn/product.txn") String TXN_PATH) throws Exception
    {
        File datFile = new File(DAT_PATH);
        File idxFile = new File(IDX_PATH);
        File txnFile = new File(TXN_PATH);

        if(!datFile.getParentFile().exists()) { datFile.getParentFile().mkdirs(); }
        if(!idxFile.getParentFile().exists()) { idxFile.getParentFile().mkdirs(); }
        if(!txnFile.getParentFile().exists()) { txnFile.getParentFile().mkdirs(); }

        if(!datFile.exists()) { datFile.createNewFile(); }
        if(!idxFile.exists()) { idxFile.createNewFile(); }
        if(!txnFile.exists()) { txnFile.createNewFile(); }
    }
}
