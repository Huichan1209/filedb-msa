package com.example.product.db.component;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class GarbageCollector
{
    @Value("${config.db.path}/dat/product.dat")
    private String DAT_PATH;

    @Value("${config.db.path}/dat/product_tmp.dat")
    private String TMP_DAT_PATH;

    @Value("${config.db.path}/idx/product.idx")
    private String IDX_PATH;

    @Value("${config.db.path}/idx/product_tmp.idx")
    private String TMP_IDX_PATH;

    private final ReentrantReadWriteLock datIdxLock;

    public GarbageCollector(@Qualifier("datIdxLock")ReentrantReadWriteLock datIdxLock)
    {
        this.datIdxLock = datIdxLock;
    }

    // 매일 새벽 3시에 clean
    @Scheduled(cron = "0 0 3 * * *")
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

                int idLength = oldDatFile.readInt();
                long productId = oldDatFile.readLong();

                int nameLength = oldDatFile.readInt();
                byte[] nameBytes = new byte[nameLength];
                oldDatFile.readFully(nameBytes);

                int priceLength = oldDatFile.readInt();
                int price = oldDatFile.readInt();

                newDatFile.seek(newPosition);
                newDatFile.writeInt(fieldCount);
                newDatFile.writeInt(idLength);
                newDatFile.writeLong(productId);
                newDatFile.writeInt(nameLength);
                newDatFile.write(nameBytes);
                newDatFile.writeInt(priceLength);
                newDatFile.writeInt(price);
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
