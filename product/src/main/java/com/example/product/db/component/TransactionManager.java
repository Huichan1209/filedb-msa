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

    private TransactionStatus status = TransactionStatus.IDLE;

    @Autowired
    public TransactionManager(@Lazy ProductRepository repository, // 순환참조 이슈 방지를 위한 @Lazy 설정
                              @Qualifier("txnLock")ReentrantReadWriteLock txnLock)
    {
        this.repository = repository;
        this.txnLock = txnLock;
    }

    public void begin() throws IOException
    {
        if(status != TransactionStatus.IDLE)
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
        status = TransactionStatus.IDLE;
    }

    private void clearLog() throws IOException
    {
        // 로그 파일 비우기
        Files.newBufferedWriter(Paths.get(TXN_PATH), StandardOpenOption.TRUNCATE_EXISTING).close();
    }

    // TODO 시간이 남으면 Product 도메인에 의존적이지 않은 형태로 Entity 인터페이스를 만든다던가 해서 라이브러리로 분리할 수 있도록 개선..
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

            logFile.writeInt(8); // ID값 길이
            logFile.writeLong(product.getId()); // ID값

            byte[] nameBytes = product.getName().getBytes();
            logFile.writeInt(nameBytes.length); // name값 길이
            logFile.write(nameBytes); // name값

            logFile.writeInt(4); // price값 길이
            logFile.writeInt(product.getPrice()); // price 값

            logFile.writeInt(4); // stock값 길이
            logFile.writeInt(product.getStock()); // stock 값
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

                int idLength = logReader.readInt(); // id값 길이
                long id = logReader.readLong(); // id값

                int nameLength = logReader.readInt(); // name값 길이
                byte[] nameBytes = new byte[nameLength];
                logReader.readFully(nameBytes); // name값
                String name = new String(nameBytes);

                int priceLength = logReader.readInt(); // price값 길이
                int price = logReader.readInt(); // price값

                int stockLength = logReader.readInt(); // stock값 길이
                int stock = logReader.readInt(); // stock값

                switch (type)
                {
                    case 'I': rollbackInsert(id); break;
                    case 'U': rollbackUpdate(new Product(id, name, price, stock)); break;
                    case 'D': rollbackDelete(new Product(id, name, price, stock)); break;
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