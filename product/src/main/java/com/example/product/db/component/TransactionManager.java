package com.example.product.db.component;

import com.example.product.db.constant.TransactionStatus;
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
    @Value("${config.db.path}/txn/${config.db.domain}.txn")
    private String TXN_PATH;

    private final ProductRepository repository;

    private final ReentrantReadWriteLock txnLock;

    private TransactionStatus status = TransactionStatus.READY;

    @Autowired
    public TransactionManager(@Lazy ProductRepository repository, // 순환참조 이슈 방지를 위한 @Lazy 설정
                              @Qualifier("txnLock")ReentrantReadWriteLock txnLock)
    {
        this.repository = repository;
        this.txnLock = txnLock;
    }

    public void begin() throws IOException
    {
        if(status != TransactionStatus.READY)
        {
            throw new RuntimeException("[TransactionManager] 트랜잭션이 READY 상태가 아님");
        }

        txnLock.writeLock().lock();
        clearLog();
        status = TransactionStatus.ACTIVE;
    }

    public void end() throws IOException
    {
        if(status != TransactionStatus.ACTIVE)
        {
            throw new RuntimeException("[TransactionManager] 트랜잭션이 ACTIVE 상태가 아님");
        }

        clearLog();
        txnLock.writeLock().unlock();
        status = TransactionStatus.READY;
    }

    private void clearLog() throws IOException
    {
        // 로그 파일 비우기
        Files.newBufferedWriter(Paths.get(TXN_PATH), StandardOpenOption.TRUNCATE_EXISTING).close();
    }

    public void writeTxnLog(char type, Product product) throws IOException
    {
        if(status != TransactionStatus.ACTIVE)
        {
            throw new RuntimeException("[TransactionManager] 트랜잭션이 ACTIVE 상태가 아님");
        }

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
        catch (IOException e)
        {
            System.out.println("[TransactionManager] 로그 write 중 Exception 발생");
            throw e;
        }
    }

    public void commitTxnLog() throws IOException
    {
        end();
    }

    public void rollbackTxn() throws Exception
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

                switch (type)
                {
                    case 'I': rollbackInsert(id); break;
                    case 'U': rollbackUpdate(new Product(id, name, price)); break;
                    case 'D': rollbackDelete(new Product(id, name, price)); break;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("[TransactionManager] rollback 중 Exception 발생");
            throw e;
        }

        end();
    }

    private void rollbackInsert(Long id) throws Exception
    {
        repository.delete(id, false);
    }

    private void rollbackUpdate(Product product) throws Exception
    {
        repository.merge(product, false);
    }

    private void rollbackDelete(Product product) throws Exception
    {
        repository.persist(product, false);
    }
}