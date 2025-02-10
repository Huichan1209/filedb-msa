package com.example.order.db.component;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class GarbageCollector
{
    @Value("${config.db.path}/dat/${config.db.domain}.dat")
    private String DAT_PATH;

    @Value("${config.db.path}/dat/${config.db.domain}_tmp.dat")
    private String TMP_DAT_PATH;

    @Value("${config.db.path}/idx/${config.db.domain}.idx")
    private String IDX_PATH;

    @Value("${config.db.path}/idx/${config.db.domain}_tmp.idx")
    private String TMP_IDX_PATH;

    private final ReentrantReadWriteLock datIdxLock;

    public GarbageCollector(@Qualifier("datIdxLock")ReentrantReadWriteLock datIdxLock)
    {
        this.datIdxLock = datIdxLock;
    }

    // 매일 새벽 3시에 clean
    @Scheduled(cron = "0 0 3 * * *")
    @PostConstruct // 기동할때 최초 한번은 그냥 실행
    public void clean() throws IOException
    {
        datIdxLock.writeLock().lock();

        try (RandomAccessFile oldDatFile = new RandomAccessFile(DAT_PATH, "r");
             RandomAccessFile newDatFile = new RandomAccessFile(TMP_DAT_PATH, "rw");
             RandomAccessFile oldIdxFile = new RandomAccessFile(IDX_PATH, "r"))
        {

            Map<Long, Long> newOffsets = new HashMap<>();

            while (oldIdxFile.getFilePointer() < oldIdxFile.length())
            {
                long id = oldIdxFile.readLong();
                long oldPosition = oldIdxFile.readLong();

                if (id == -1) continue;

                oldDatFile.seek(oldPosition);

                long newPosition = newDatFile.length();
                newOffsets.put(id, newPosition);

                int fieldCount = oldDatFile.readInt();

                int orderIdLength = oldDatFile.readInt();
                long orderId = oldDatFile.readLong();

                int productIdLength = oldDatFile.readInt();
                long productId = oldDatFile.readLong();

                int countLength = oldDatFile.readInt();
                long count = oldDatFile.readInt();

                newDatFile.seek(newPosition);
                newDatFile.writeInt(fieldCount);
                newDatFile.writeInt(orderIdLength);
                newDatFile.writeLong(orderId);
                newDatFile.writeInt(productIdLength);
                newDatFile.writeLong(productId);
                newDatFile.writeInt(countLength);
                newDatFile.writeLong(count);
            }

            try (RandomAccessFile newIdxFile = new RandomAccessFile(TMP_IDX_PATH, "rw"))
            {
                for (Map.Entry<Long, Long> entry : newOffsets.entrySet())
                {
                    newIdxFile.writeLong(entry.getKey());
                    newIdxFile.writeLong(entry.getValue());
                }
            }

            new File(DAT_PATH).delete();
            new File(TMP_DAT_PATH).renameTo(new File(DAT_PATH));

            new File(IDX_PATH).delete();
            new File(TMP_IDX_PATH).renameTo(new File(IDX_PATH));
        }
        finally
        {
            datIdxLock.writeLock().unlock();
        }
    }
}
