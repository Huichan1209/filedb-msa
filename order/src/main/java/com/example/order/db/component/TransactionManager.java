package com.example.order.db.component;

import com.example.order.db.constant.TransactionStatus;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.repository.OrderRepository;
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

    private final OrderRepository repository;

    private final ReentrantReadWriteLock txnLock;

    private TransactionStatus status = TransactionStatus.IDLE;

    @Autowired
    public TransactionManager(@Lazy OrderRepository repository, // 순환참조 이슈 방지를 위한 @Lazy 설정
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

    // TODO 시간이 남으면 도메인에 의존적이지 않은 형태로 Entity 인터페이스를 만든다던가 해서 독립적인 라이브러리로 분리할 수 있도록 개선..
    public void writeTxnLog(char type, Order order) throws IOException
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
            logFile.writeLong(order.getId()); // ID값

            logFile.writeInt(8); // product id값 길이
            logFile.writeLong(order.getProductId()); // product id값

            logFile.writeInt(4); // count값 길이
            logFile.writeInt(order.getCount()); // count값

            byte[] statusBytes = order.getStatus().name().getBytes();
            logFile.writeInt(statusBytes.length); // status값 길이 (가변)
            logFile.write(statusBytes); // status 값]=
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

                int productIdLength = logReader.readInt(); // product id값 길이
                long productId = logReader.readLong(); // product id값

                int countLength = logReader.readInt(); // count값 길이
                int count = logReader.readInt(); // count값

                int statusLength = logReader.readInt(); // status값 길이
                byte[] statusByte = new byte[statusLength];
                logReader.readFully(statusByte);
                OrderStatus status = OrderStatus.fromString(new String(statusByte));

                switch (type)
                {
                    case 'I': rollbackInsert(id); break;
                    case 'U': rollbackUpdate(new Order(id, productId, count, status)); break;
                    case 'D': rollbackDelete(new Order(id, productId, count, status)); break;
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

    private void rollbackUpdate(Order order) throws Exception
    {
        repository.merge(order, false);
    }

    private void rollbackDelete(Order order) throws Exception
    {
        repository.persist(order, false);
    }
}