package com.example.product.repository;

import com.example.product.db.IdGenerator;
import com.example.product.db.TransactionManager;
import com.example.product.domain.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * .dat 저장 구조: [필드 수][데이터길이][값][데이터길이][값]...
 * .idx 저장 구조: [PK 값][offset]...
 */

@Repository
public class ProductRepository // 여기는 CRUD만 구현하고 Transaction과 GC는 분리해서 구현함.
{
    @Value("${config.path.db}/dat/product.dat")
    private String DAT_PATH;

    @Value("${config.path.db}/idx/product.idx")
    private String IDX_PATH;

    private final TransactionManager tm;

    private final IdGenerator idGenerator;

    private final ReentrantReadWriteLock datIdxLock;

    @Autowired
    public ProductRepository(TransactionManager tm,
                             IdGenerator idGenerator,
                             @Qualifier("datIdxLock")ReentrantReadWriteLock datIdxLock)
    {
        this.tm = tm;
        this.idGenerator = idGenerator;
        this.datIdxLock = datIdxLock;
    }

    public Product save(Product product) throws IOException
    {
        /*
            Jpa save 메소드 동작 과정
            1. 엔티티 id 확인
                1-1. id가 null이면 새로운 엔티티로 간주하여 persist 호출
                1-2. id가 존재하는 경우
                    1-2-1. 영속성 컨텍스트 혹은 DB에 해당 데이터가 존재하는지 확인
                    1-2-1-1. 존재하면 merge
                    1-2-1-2. 존재하지 않으면 persist
        */

        if(product.getId() == null)
        {
            return this.persist(product, true);
        }

        return this.merge(product, true);
    }

    public Product persist(Product product) throws IOException
    {
        return persist(product, true);
    }

    public Product persist(Product product, boolean logTxn) throws IOException
    {
        product.setId(idGenerator.getNextId());

        // 트랜잭션 로그 기록
        if(logTxn) { tm.writeTxnLog('I', product); }

        // 이 어플리케이션 내에서 쓰레드간 충돌을 방지
        datIdxLock.writeLock().lock();

        try (RandomAccessFile datFile = new RandomAccessFile(DAT_PATH, "rw");
             RandomAccessFile idxFile = new RandomAccessFile(IDX_PATH, "rw"))
        {
            // 외부 프로그램에서 수정을 방지 (RandomAccessFile이 close되면 channel도 close되고 모든 lock은 해제됨)
            datFile.getChannel().lock();
            idxFile.getChannel().lock();

            long position = datFile.length();
            datFile.seek(position);

            datFile.writeInt(3); // [필드 수]

            datFile.writeInt(8); // [id값 길이] Long 타입이라 8 고정
            datFile.writeLong(product.getId()); // [id 값]

            byte[] nameBytes = product.getName().getBytes();
            datFile.writeInt(nameBytes.length); // [name값 길이] (가변)
            datFile.write(nameBytes); // [name 값]

            datFile.writeInt(4); // price 값 길이. int 타입이라 4 고정
            datFile.writeInt(product.getPrice()); // price 값 설정

            // 저장한 id와 위치를 idx 파일에 기록한다.
            idxFile.seek(idxFile.length());
            idxFile.writeLong(product.getId());
            idxFile.writeLong(position);
        }
        catch (IOException e)
        {
            if(logTxn) { tm.rollbackTxn(); }
            throw e;
        }
        finally
        {
            datIdxLock.writeLock().unlock();
        }

        if(logTxn) { tm.commitTxnLog(); } // 트랜잭션 커밋
        return product; // Jpa에서 persist는 영속성 컨텍스트에 등록된 동일한 객체가 리턴된다.
    }

    public Product merge(Product product) throws IOException
    {
        return merge(product, true);
    }

    public Product merge(Product product, boolean logTxn) throws IOException
    {
        /*
             jpa merge 메소드 동작 과정 정리
             1. 병합할 엔티티의 id 확인
                1-1. id가 null인 경우 insert 준비
                1-2. id가 있으면 영속성 컨텍스트에서 찾아옴
                    1-2-1. 영속성 컨텍스트에 있으면 가져와서 병합
                    1-2-2. 영속성 컨텍스트에 없으면 DB 조회
                        1-2-2-1. DB에 있으면 가져와서 병합
                        1-2-2-2. DB에 없으면 새로운 엔티티로 간주하여 insert
             2. 영속성 컨텍스트에 있는 엔티티에 새로 받은 비영속 엔티티의 데이터를 복사(병합)
             3. 새로운 영속 엔티티 return
        */

        return null;
    }

    public Optional<Product> findById(long id) throws IOException
    {
        datIdxLock.readLock().lock();

        try (RandomAccessFile datFile = new RandomAccessFile(DAT_PATH, "r");
             RandomAccessFile idxFile = new RandomAccessFile(IDX_PATH, "r"))
        {
            datFile.getChannel().lock();
            idxFile.getChannel().lock();

            // idx 파일을 돌면서 데이터의 위치를 구한다.
            long position = -1; // .dat 파일에서 찾으려는 데이터의 위치값
            long idxFileLength = idxFile.length();
            while (idxFile.getFilePointer() < idxFileLength)
            {
                long storedId = idxFile.readLong();
                if (storedId == id)
                {
                    position = idxFile.readLong();
                    break;
                }
                else
                {
                    idxFile.readLong();
                }
            }

            // idx파일에 data의 위치가 있다면 dat 파일에서 조회한다.
            if(position != -1)
            {
                datFile.seek(position);

                int fieldCount = datFile.readInt(); // [필드 수]

                int idLength = datFile.readInt(); // [id 값 길이]
                long findId = datFile.readLong(); // [id 값]

                int nameLength = datFile.readInt(); // [name 값 길이]
                byte[] nameBytes = new byte[nameLength];
                datFile.readFully(nameBytes);
                String findName = new String(nameBytes); // [name 값]

                int priceLength = datFile.readInt(); // [price 값 길이]
                int findPrice = datFile.readInt(); // [price 값]

                return Optional.of(new Product(findId, findName, findPrice));
            }
        }
        finally
        {
            datIdxLock.readLock().unlock();
        }

        return Optional.empty();
    }

    public void delete(long id) throws IOException
    {
        delete(id, true);
    }

    public void delete(long id, boolean logTxn) throws  IOException
    {
        Product findProduct = findById(id).orElseGet(null);
        if (findProduct == null)
        {
            System.out.println("[delete] data not found. id: " + id);
            return;
        }

        if(logTxn) { tm.writeTxnLog('D', findProduct); }
        datIdxLock.writeLock().lock();

        try (RandomAccessFile idxFile = new RandomAccessFile(IDX_PATH, "rw"))
        {
            idxFile.getChannel().lock();

            long length = idxFile.length();
            while (idxFile.getFilePointer() < length)
            {
                long storedId = idxFile.readLong();
                long position = idxFile.readLong();
                if (storedId == id)
                {
                    idxFile.seek(idxFile.getFilePointer() - 16); // long 2칸 앞으로
                    idxFile.writeLong(-1); // id를 -1로 설정해서 참조되지 않는 데이터로 바꿈 (연결이 끊긴 데이터는 가비지 컬렉터에 의해 한번에 정리됨)
                    return;
                }
            }
        }
        catch (IOException e)
        {
            if(logTxn) { tm.rollbackTxn(); }
            throw e;
        }
        finally
        {
            datIdxLock.writeLock().unlock();
        }

        tm.commitTxnLog();
    }
}
