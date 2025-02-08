package com.example.product.db.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class IdGenerator
{
    private AtomicLong atomicId = new AtomicLong(0);

    @Value("${config.path.db}/idx/product.idx")
    private String IDX_PATH;

    public IdGenerator()
    {
        initValue();
    }

    private void initValue()
    {
        try (RandomAccessFile indexFile = new RandomAccessFile(IDX_PATH, "r"))
        {
            long fileLength = indexFile.length();
            while (indexFile.getFilePointer() < fileLength)
            {
                // .idx 저장 구조: [PK 값][Pointer]...
                atomicId.set(Math.max(atomicId.get(), indexFile.readLong())); // PK 값 get
                indexFile.readLong(); // Pointer 값 skip
            }
        }
        catch (IOException ignore)
        {
            ignore.printStackTrace();
        }
    }

    public long getNextId()
    {
        return atomicId.incrementAndGet();
    }

    public long getLastId()
    {
        return atomicId.get();
    }
}