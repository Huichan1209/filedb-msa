package com.example.product.db;

import com.example.product.domain.Product;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * .tx 저장 구조: [유형][데이터 길이][값][데이터 길이][값]…
 */

@Component
public class TransactionManager
{
    @Value("${config.path.db}/txn/product.txn")
    private String TXN_PATH;

    private final ProductRepository repository;

    private final ReentrantReadWriteLock txnLock;

    @Autowired
    public TransactionManager(@Lazy ProductRepository repository, @Qualifier("txnLock")ReentrantReadWriteLock txnLock) // 순환참조 이슈 방지를 위한 @Lazy 설정
    {
        this.repository = repository;
        this.txnLock = txnLock;
    }

    public void writeTxnLog(char type, Product product) throws IOException
    {
        txnLock.writeLock().lock();
        try (RandomAccessFile logFile = new RandomAccessFile(TXN_PATH, "rw"))
        {
            logFile.seek(logFile.length());
            logFile.writeByte(type);
            logFile.writeInt(8);
            logFile.writeLong(product.getId());
            byte[] nameBytes = product.getName().getBytes();
            logFile.writeInt(nameBytes.length);
            logFile.write(nameBytes);
            logFile.writeInt(4);
            logFile.writeInt(product.getPrice());
        }
        finally
        {
            txnLock.writeLock().unlock();
        }
    }

    public void commitTxnLog() throws IOException
    {
        // 로그 파일 비우기
        Files.newBufferedWriter(Paths.get(TXN_PATH), StandardOpenOption.TRUNCATE_EXISTING).close();
    }

    public void rollbackTxn() throws IOException
    {
        File logFile = new File(TXN_PATH);
        if (!logFile.exists()) return;

        try (RandomAccessFile logReader = new RandomAccessFile(logFile, "r"))
        {
            while (logReader.getFilePointer() < logReader.length())
            {
                char type = (char) logReader.readByte();

                int idLength = logReader.readInt();
                long id = logReader.readLong();

                int nameLength = logReader.readInt();
                byte[] nameBytes = new byte[nameLength];
                logReader.readFully(nameBytes);
                String name = new String(nameBytes);

                int priceLength = logReader.readInt();
                int price = logReader.readInt();

                if (type == 'I')
                {
                    repository.delete(id);
                }
                else if (type == 'U')
                {
                    // TODO
                }
                else if (type == 'D')
                {
                    repository.persist(new Product(id, name, price), false);
                }
            }
        }

        commitTxnLog();
    }
}
