package com.example.product.db.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;

@Component
@DependsOn("fileInitializer")
public class IdGenerator
{
    private AtomicLong atomicId = new AtomicLong(0);

    @Value("${config.db.path}/idx/product.idx")
    private String IDX_PATH;

    @PostConstruct
    private void initializeValue() throws Exception
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
    }

    public long getNextId()
    {
        return atomicId.incrementAndGet();
    }
}