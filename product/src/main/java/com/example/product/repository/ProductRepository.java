package com.example.product.repository;

import com.example.product.db.component.IdGenerator;
import com.example.product.db.component.TransactionManager;
import com.example.product.db.paging.Page;
import com.example.product.db.paging.Pageable;
import com.example.product.db.paging.Sort;
import com.example.product.domain.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * .dat 저장 구조: [필드 수][데이터길이][값][데이터길이][값]...
 * .idx 저장 구조: [PK 값][offset]...
 */

@Repository
public class ProductRepository // 여기는 CRUD만 구현하고 Transaction과 GC는 분리해서 구현함.
{
    @Value("${config.db.path}/dat/${config.db.domain}.dat")
    private String DAT_PATH;

    @Value("${config.db.path}/idx/${config.db.domain}.idx")
    private String IDX_PATH;

    private final TransactionManager tm;

    private final IdGenerator idGenerator;

    private final ReentrantReadWriteLock datIdxLock;

    @Autowired
    public ProductRepository(TransactionManager tm,
                             IdGenerator idGenerator,
                             @Qualifier("datIdxLock") ReentrantReadWriteLock datIdxLock)
    {
        this.tm = tm;
        this.idGenerator = idGenerator;
        this.datIdxLock = datIdxLock;
    }

    public Product save(Product product) throws Exception
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

        if (product.getId() == null)
        {
            return this.persist(product, true);
        }

        return this.merge(product, true);
    }

    public Product persist(Product product) throws Exception
    {
        return persist(product, true);
    }

    public Product persist(Product product, boolean logTxn) throws Exception
    {
        product.setId(idGenerator.getNextId());

        if (logTxn)
        {
            tm.begin(); // 트랜잭션 시작
            tm.writeTxnLog('I', product); // 트랜잭션 로그 기록
        }

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
            if (logTxn)
            {
                tm.rollbackTxn();
            }
            throw e;
        }
        finally
        {
            datIdxLock.writeLock().unlock(); // try-finally로 lock 해제를 보장.
        }

        if (logTxn)
        {
            tm.commitTxnLog();
        } // 트랜잭션 커밋
        return product; // Jpa에서 persist는 영속성 컨텍스트에 등록된 동일한 객체가 리턴된다. 비슷하게 구현하기 위해 매개변수로 받은 객체를 리턴한다.
    }

    public Product merge(Product product) throws Exception
    {
        return merge(product, true);
    }

    public Product merge(Product product, boolean logTxn) throws Exception
    {
        /*
             jpa merge 메소드 동작 과정 정리
             1. 병합할 엔티티의 id 확인
                1-1. id가 null인 경우 신규 insert
                1-2. id가 있으면 영속성 컨텍스트에서 찾아옴
                    1-2-1. 영속성 컨텍스트에 있으면 가져와서 병합
                    1-2-2. 영속성 컨텍스트에 없으면 DB 조회
                        1-2-2-1. DB에 있으면 가져와서 병합
                        1-2-2-2. DB에 없으면 새로운 엔티티로 간주하여 insert
             2. 영속성 컨텍스트에 있는 엔티티에 새로 받은 비영속 엔티티의 데이터를 복사(병합)
             3. 새로운 영속 엔티티 return
        */

        if (product.getId() == null)
        {
            persist(product, true);
            return new Product(product.getId(), product.getName(), product.getPrice()); // 새로운 엔티티 return
        }

        Product findProduct = findById(product.getId()).orElseGet(null);
        if (findProduct == null)
        {
            persist(product, true);
            return new Product(product.getId(), product.getName(), product.getPrice()); // 새로운 엔티티 return
        }

        if (logTxn)
        {
            tm.begin();
            tm.writeTxnLog('U', findProduct); // 트랜잭션 로그에 업데이트 이전 데이터 기록
        }

        datIdxLock.writeLock().lock();
        long newPosition;
        try (RandomAccessFile datFile = new RandomAccessFile(DAT_PATH, "rw");
             RandomAccessFile idxFile = new RandomAccessFile(IDX_PATH, "rw"))
        {
            datFile.getChannel().lock();
            idxFile.getChannel().lock();

            newPosition = datFile.length();
            datFile.seek(newPosition); // 파일 맨 뒤에 새로 값을 저장하기 위해 마지막으로 이동

            datFile.writeInt(3); // [필드 수]

            datFile.writeInt(8); // [id값 길이]
            datFile.writeLong(product.getId()); // [id 값]

            byte[] nameBytes = product.getName().getBytes();
            datFile.writeInt(nameBytes.length); // [name값 길이]
            datFile.write(nameBytes); // [name값]

            datFile.writeInt(4); // [price값 길이]
            datFile.writeInt(product.getPrice());

            long idxFileLength = idxFile.length();
            while (idxFile.getFilePointer() < idxFileLength)
            {
                long storedId = idxFile.readLong();
                long position = idxFile.readLong();
                if (storedId == product.getId())
                {
                    idxFile.seek(idxFile.getFilePointer() - 8); // long 한칸 앞으로
                    idxFile.writeLong(newPosition); // 새로 저장한 position으로 덮어쓰기. 얘는 고정 크기 방식이기 때문에 dat 파일과 달리 덮어씌워도 됨.
                    // 참조가 끊긴 기존 데이터는 가비지 컬렉터에 의해 자동으로 청소됨
                    break;
                }
            }
        }
        catch (IOException e)
        {
            if (logTxn)
            {
                tm.rollbackTxn();
            }
            throw e;
        }
        finally
        {
            datIdxLock.writeLock().unlock();
        }

        if (logTxn)
        {
            tm.commitTxnLog();
        }

        return new Product(product.getId(), product.getName(), product.getPrice()); // 새로운 엔티티 return
    }

    public void delete(long id) throws Exception
    {
        delete(id, true);
    }

    public void delete(long id, boolean logTxn) throws Exception
    {
        Product findProduct = findById(id).orElseGet(null);
        if (findProduct == null)
        {
            System.out.println("[delete] data not found. id: " + id);
            return;
        }

        if (logTxn)
        {
            tm.begin();
            tm.writeTxnLog('D', findProduct);
        }

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
            if (logTxn)
            {
                tm.rollbackTxn();
            }
            throw e;
        }
        finally
        {
            datIdxLock.writeLock().unlock();
        }

        if (logTxn)
        {
            tm.commitTxnLog();
        }
    }

    public Optional<Product> findById(long id) throws IOException
    {
        datIdxLock.readLock().lock();

        try (RandomAccessFile datFile = new RandomAccessFile(DAT_PATH, "r");
             RandomAccessFile idxFile = new RandomAccessFile(IDX_PATH, "r"))
        {
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
            if (position != -1)
            {
                datFile.seek(position);

                int fieldCount = datFile.readInt(); // [필드 수]
                if (fieldCount != 3)
                {
                    throw new IOException("[Repository.findById] 저장된 필드의 수가 3이 아님. fieldCount: " + fieldCount);
                }

                int idLength = datFile.readInt(); // [id 값 길이]
                if (idLength != 8)
                {
                    throw new IOException("[Repository.findById] 저장된 id의 길이가 8이 아님. idLength: " + idLength);
                }

                long findId = datFile.readLong(); // [id 값]

                int nameLength = datFile.readInt(); // [name 값 길이] 가변길이라 validation 생략
                byte[] nameBytes = new byte[nameLength];
                datFile.readFully(nameBytes);
                String findName = new String(nameBytes); // [name 값]

                int priceLength = datFile.readInt(); // [price 값 길이]
                if (priceLength != 4)
                {
                    throw new IOException("[Repository.findById] 저장된 price의 길이가 4가 아님. priceLength: " + priceLength);
                }

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

    public Page<Product> findAll(Pageable pageable) throws IOException
    {
        datIdxLock.readLock().lock();

        List<Product> products = new ArrayList<>();
        List<Map.Entry<Long, Long>> indexList = new ArrayList<>();

        try (RandomAccessFile idxFile = new RandomAccessFile(IDX_PATH, "r");
             RandomAccessFile datFile = new RandomAccessFile(DAT_PATH, "r"))
        {
            while (idxFile.getFilePointer() < idxFile.length())
            {
                long id = idxFile.readLong();
                long position = idxFile.readLong();
                indexList.add(new AbstractMap.SimpleEntry<>(id, position));
            }

            // 정렬 처리
            Sort sort = pageable.getSort();
            Comparator<Map.Entry<Long, Long>> comparator = Comparator.comparing(Map.Entry::getKey);

            if (sort != null && !sort.isUnsorted())
            {
                String sortBy = sort.getProperty();
                boolean ascending = sort.isAscending();

                if ("name".equalsIgnoreCase(sortBy))  // name 기준 정렬
                {
                    comparator = Comparator.comparing(entry ->
                    {
                        try
                        {
                            datFile.seek(entry.getValue());
                            datFile.readInt();  // 필드 수
                            datFile.readInt();  // ID 길이
                            datFile.readLong(); // ID 값

                            int nameLength = datFile.readInt();
                            byte[] nameBytes = new byte[nameLength];
                            datFile.readFully(nameBytes);
                            return new String(nameBytes);  // name 값 String
                        }
                        catch (IOException e)
                        {
                            return "";
                        }
                    });
                }
                else if ("price".equalsIgnoreCase(sortBy)) // price 기준 정렬
                {
                    comparator = Comparator.comparingInt(entry ->
                    {
                        try
                        {
                            datFile.seek(entry.getValue());
                            datFile.readInt();  // 필드 수
                            datFile.readInt();  // ID 길이
                            datFile.readLong(); // ID 값

                            int nameLength = datFile.readInt();
                            datFile.skipBytes(nameLength);
                            datFile.readInt();  // 가격 길이
                            
                            return datFile.readInt(); // 가격 값
                        }
                        catch (IOException e)
                        {
                            return 0;
                        }
                    });
                }
                else
                {
                    comparator = Comparator.comparing(Map.Entry::getKey); // ID 기준 기본 정렬
                }

                if (!ascending)
                {
                    comparator = comparator.reversed();
                }
            }

            indexList.sort(comparator);

            // 페이징 처리
            int page = pageable.getPageNumber();
            if (page < 1) { page = 1; }
            int size = pageable.getPageSize();
            int startIndex = Math.min(0, ((page - 1) * size));
            if (startIndex >= indexList.size())
            {
                // 최대 페이지 초과시 빈페이지 반환
                return new Page<>(Collections.emptyList(), page, size, indexList.size(), pageable.getSort());
            }

            int endIndex = Math.min(startIndex + size, indexList.size());

            // 데이터 읽기
            for (int i = startIndex; i < endIndex; i++)
            {
                long position = indexList.get(i).getValue();
                datFile.seek(position);

                int fieldCount = datFile.readInt();
                if (fieldCount != 3)
                {
                    throw new IOException("[Repository.findAll] 저장된 필드의 수가 3이 아님. fieldCount: " + fieldCount);
                }

                int idLength = datFile.readInt();
                if (idLength != 8)
                {
                    throw new IOException("[Repository.findAll] 저장된 id의 길이가 8이 아님. idLength: " + idLength);
                }

                long productId = datFile.readLong();
                int nameLength = datFile.readInt();
                byte[] nameBytes = new byte[nameLength];
                datFile.readFully(nameBytes);
                String name = new String(nameBytes);

                int priceLength = datFile.readInt();
                if (priceLength != 4)
                {
                    throw new IOException("[Repository.findAll] 저장된 price의 길이가 4가 아님. priceLength: " + priceLength);
                }

                int price = datFile.readInt();

                products.add(new Product(productId, name, price));
            }
        }
        finally
        {
            datIdxLock.readLock().unlock();
        }

        return new Page<>(products, pageable.getPageNumber(), pageable.getPageSize(), indexList.size(), pageable.getSort());
    }
}