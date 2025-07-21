package com.avpuser.mongo;

import com.avpuser.mongo.exception.EntityNotFoundException;
import com.avpuser.mongo.exception.VersionConflictException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongojack.JacksonMongoCollection;
import org.mongojack.MongoCollection;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CommonDaoTest {

    @Mock
    private MongoDatabase database;

    @Mock
    private JacksonMongoCollection<TestEntity> mongoCollection;

    private Clock fixedClock;
    private CommonDao<TestEntity> dao;
    private Instant testTime;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private com.mongodb.client.MongoCollection<TestEntity> nativeCollection;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        testTime = Instant.parse("2025-01-15T10:00:00Z");
        fixedClock = Clock.fixed(testTime, ZoneId.systemDefault());

        // Мокируем вызов database.getCollection(...) → возвращаем nativeCollection
        when(database.getCollection(anyString(), eq(TestEntity.class))).thenReturn(nativeCollection);

        // Строим dao — теперь getCollection(...) не вернёт null
        dao = new CommonDao<>(database, TestEntity.class, fixedClock);

        // Внедряем мок mongoCollection вручную
        Field mongoCollectionField = CommonDao.class.getDeclaredField("mongoCollection");
        mongoCollectionField.setAccessible(true);
        mongoCollectionField.set(dao, mongoCollection);
    }

    @Test
    void testGetType() {
        // Act & Assert
        assertEquals(TestEntity.class, dao.getType());
    }

    @Test
    void testInsert_NewEntity() {
        // Arrange
        TestEntity entity = new TestEntity("test-id", "Test Name");

        // Act
        String result = dao.insert(entity);

        // Assert
        assertEquals("test-id", result);
        assertEquals(testTime, entity.getCreatedAt());
        assertEquals(testTime, entity.getUpdatedAt());
        verify(mongoCollection).insert(entity);
    }

    @Test
    void testInsert_EntityWithExistingTimestamps() {
        // Arrange
        Instant existingTime = testTime.minusSeconds(3600);
        TestEntity entity = new TestEntity("test-id", "Test Name");
        entity.setCreatedAt(existingTime);
        entity.setUpdatedAt(existingTime);

        // Act
        String result = dao.insert(entity);

        // Assert
        assertEquals("test-id", result);
        assertEquals(existingTime, entity.getCreatedAt()); // Не должно измениться
        assertEquals(existingTime, entity.getUpdatedAt()); // Не должно измениться
        verify(mongoCollection).insert(entity);
    }

    @Test
    void testUpdate_Success() {
        // Arrange
        TestEntity entity = new TestEntity("test-id", "Updated Name");
        entity.setVersion(5);
        entity.setCreatedAt(testTime.minusSeconds(3600));

        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoCollection.findOneById("test-id")).thenReturn(entity);
        when(mongoCollection.replaceOne(any(), eq(entity))).thenReturn(updateResult);

        // Act
        dao.update(entity);

        // Assert
        assertEquals(6, entity.getVersion()); // Версия увеличилась
        assertEquals(testTime, entity.getUpdatedAt()); // Время обновления установлено
        verify(mongoCollection).replaceOne(any(), eq(entity));
    }

    @Test
    void testUpdate_EntityNotFound() {
        // Arrange
        TestEntity entity = new TestEntity("non-existent-id", "Test Name");
        when(mongoCollection.findOneById("non-existent-id")).thenReturn(null);

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> dao.update(entity));
        assertTrue(exception.getMessage().contains("non-existent-id"));
    }

    @Test
    void testUpdate_VersionConflict() {
        // Arrange
        TestEntity entity = new TestEntity("test-id", "Updated Name");
        entity.setVersion(5);

        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(0L); // Не обновлено
        when(mongoCollection.findOneById("test-id")).thenReturn(entity);
        when(mongoCollection.replaceOne(any(), eq(entity))).thenReturn(updateResult);

        // Act & Assert
        VersionConflictException exception = assertThrows(VersionConflictException.class,
                () -> dao.update(entity));
        assertTrue(exception.getMessage().contains("Version conflict"));
        assertTrue(exception.getMessage().contains("test-id"));
    }

    @Test
    void testFindById_Found() {
        // Arrange
        TestEntity entity = new TestEntity("test-id", "Test Name");
        when(mongoCollection.findOneById("test-id")).thenReturn(entity);

        // Act
        Optional<TestEntity> result = dao.findById("test-id");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        when(mongoCollection.findOneById("non-existent-id")).thenReturn(null);

        // Act
        Optional<TestEntity> result = dao.findById("non-existent-id");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindById_NullId() {
        // Act
        Optional<TestEntity> result = dao.findById(null);

        // Assert
        assertFalse(result.isPresent());
        verify(mongoCollection, never()).findOneById(any());
    }

    @Test
    void testFindByIdOrThrow_Found() {
        // Arrange
        TestEntity entity = new TestEntity("test-id", "Test Name");
        when(mongoCollection.findOneById("test-id")).thenReturn(entity);

        // Act
        TestEntity result = dao.findByIdOrThrow("test-id");

        // Assert
        assertEquals(entity, result);
    }

    @Test
    void testFindByIdOrThrow_NotFound() {
        // Arrange
        when(mongoCollection.findOneById("non-existent-id")).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dao.findByIdOrThrow("non-existent-id"));
        assertTrue(exception.getMessage().contains("non-existent-id"));
    }

    @Test
    void testExistsById_True() {
        // Arrange
        TestEntity entity = new TestEntity("test-id", "Test Name");
        when(mongoCollection.findOneById("test-id")).thenReturn(entity);

        // Act
        boolean result = dao.existsById("test-id");

        // Assert
        assertTrue(result);
    }

    @Test
    void testExistsById_False() {
        // Arrange
        when(mongoCollection.findOneById("non-existent-id")).thenReturn(null);

        // Act
        boolean result = dao.existsById("non-existent-id");

        // Assert
        assertFalse(result);
    }

    @Test
    void testDeleteById_Success() {
        // Arrange
        DeleteResult deleteResult = mock(DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(1L);
        when(mongoCollection.removeById("test-id")).thenReturn(deleteResult);

        // Act
        boolean result = dao.deleteById("test-id");

        // Assert
        assertTrue(result);
        verify(mongoCollection).removeById("test-id");
    }

    @Test
    void testDeleteById_NotFound() {
        // Arrange
        DeleteResult deleteResult = mock(DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(0L);
        when(mongoCollection.removeById("non-existent-id")).thenReturn(deleteResult);

        // Act
        boolean result = dao.deleteById("non-existent-id");

        // Assert
        assertFalse(result);
    }

    @Test
    void testCount() {
        // Arrange
        when(mongoCollection.countDocuments()).thenReturn(42L);

        // Act
        long result = dao.count();

        // Assert
        assertEquals(42L, result);
        verify(mongoCollection).countDocuments();
    }

    @Test
    void testFindByIds() {
        // Arrange
        List<String> ids = Arrays.asList("id1", "id2", "id3");
        TestEntity entity1 = new TestEntity("id1", "Name1");
        TestEntity entity2 = new TestEntity("id2", "Name2");
        List<TestEntity> expectedEntities = Arrays.asList(entity1, entity2);

        FindIterable<TestEntity> findIterable = mock(FindIterable.class);
        when(mongoCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.into(any(List.class))).thenReturn(expectedEntities);

        // Act
        List<TestEntity> result = dao.findByIds(ids);

        // Assert
        assertEquals(expectedEntities, result);
        verify(mongoCollection).find(any(Bson.class));
    }

    @Test
    void testFindAll() {
        // Arrange
        TestEntity entity1 = new TestEntity("id1", "Name1");
        TestEntity entity2 = new TestEntity("id2", "Name2");

        FindIterable<TestEntity> findIterable = mock(FindIterable.class);
        MongoCursor<TestEntity> cursor = mockCursorFor(entity1, entity2);

        when(mongoCollection.find()).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(cursor);

        // Act
        List<TestEntity> result = dao.findAll();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(entity1));
        assertTrue(result.contains(entity2));
        verify(cursor).close(); // проверяем, что курсор закрыт
    }

    private MongoCursor<TestEntity> mockCursorFor(TestEntity... entities) {
        Iterator<TestEntity> iterator = Arrays.asList(entities).iterator();
        MongoCursor<TestEntity> cursor = mock(MongoCursor.class);

        when(cursor.hasNext()).thenAnswer(inv -> iterator.hasNext());
        when(cursor.next()).thenAnswer(inv -> iterator.next());
        doNothing().when(cursor).close();

        return cursor;
    }

    @Test
    void testDeleteAll() {
        // Arrange
        DeleteResult deleteResult = mock(DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(5L);
        when(mongoCollection.deleteMany(any(Bson.class))).thenReturn(deleteResult);

        // Act
        dao.deleteAll();

        // Assert
        verify(mongoCollection).deleteMany(any(Bson.class));
    }

    @Test
    void testFindBySpecification() {
        // Arrange
        LimitSpecification specification = mock(LimitSpecification.class);
        Bson filter = mock(Bson.class);
        Bson sort = mock(Bson.class);

        when(specification.filter()).thenReturn(filter);
        when(specification.sort()).thenReturn(sort);
        when(specification.getLimit()).thenReturn(10);
        when(specification.collation()).thenReturn(Optional.empty());

        TestEntity entity1 = new TestEntity("id1", "Name1");

        FindIterable<TestEntity> findIterable = mock(FindIterable.class);
        MongoCursor<TestEntity> cursor = mockCursorFor(entity1);

        when(mongoCollection.find(filter)).thenReturn(findIterable);
        when(findIterable.sort(sort)).thenReturn(findIterable);
        when(findIterable.limit(10)).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(cursor); // <== важно!

        // Act
        List<TestEntity> result = dao.findBySpecification(specification);

        // Assert
        assertEquals(1, result.size());
        assertEquals(entity1, result.get(0));
        verify(mongoCollection).find(filter);
        verify(findIterable).sort(sort);
        verify(findIterable).limit(10);
        verify(cursor).close();
    }

    @Test
    void testFindSingleBySpecification_Found() {
        // Arrange
        LimitSpecification specification = mock(LimitSpecification.class);
        Bson filter = mock(Bson.class);
        Bson sort = mock(Bson.class);

        when(specification.filter()).thenReturn(filter);
        when(specification.sort()).thenReturn(sort);
        when(specification.getLimit()).thenReturn(1);
        when(specification.collation()).thenReturn(Optional.empty());

        TestEntity entity = new TestEntity("id1", "Name1");

        FindIterable<TestEntity> findIterable = mock(FindIterable.class);
        MongoCursor<TestEntity> cursor = mock(MongoCursor.class);

        when(mongoCollection.find(filter)).thenReturn(findIterable);
        when(findIterable.sort(sort)).thenReturn(findIterable);
        when(findIterable.limit(1)).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(cursor);

        when(cursor.hasNext()).thenReturn(true, false); // сначала true, потом false
        when(cursor.next()).thenReturn(entity);
        doNothing().when(cursor).close();

        // Act
        Optional<TestEntity> result = dao.findSingleBySpecification(specification);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    void testFindSingleBySpecification_NotFound() {
        // Arrange
        LimitSpecification specification = mock(LimitSpecification.class);
        Bson filter = mock(Bson.class);
        Bson sort = mock(Bson.class);

        when(specification.filter()).thenReturn(filter);
        when(specification.sort()).thenReturn(sort);
        when(specification.getLimit()).thenReturn(1);
        when(specification.collation()).thenReturn(Optional.empty());

        FindIterable<TestEntity> findIterable = mock(FindIterable.class);
        MongoCursor<TestEntity> cursor = mock(MongoCursor.class);

        when(mongoCollection.find(filter)).thenReturn(findIterable);
        when(findIterable.sort(sort)).thenReturn(findIterable);
        when(findIterable.limit(1)).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(cursor);

        when(cursor.hasNext()).thenReturn(false);
        doNothing().when(cursor).close();

        // Act
        Optional<TestEntity> result = dao.findSingleBySpecification(specification);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindSingleBySpecification_MultipleFound() {
        // Arrange
        LimitSpecification specification = mock(LimitSpecification.class);
        Bson filter = mock(Bson.class);
        Bson sort = mock(Bson.class);

        when(specification.filter()).thenReturn(filter);
        when(specification.sort()).thenReturn(sort);
        when(specification.getLimit()).thenReturn(10);
        when(specification.collation()).thenReturn(Optional.empty());

        TestEntity entity1 = new TestEntity("id1", "Name1");
        TestEntity entity2 = new TestEntity("id2", "Name2");

        FindIterable<TestEntity> findIterable = mock(FindIterable.class);
        MongoCursor<TestEntity> cursor = mock(MongoCursor.class);

        when(mongoCollection.find(filter)).thenReturn(findIterable);
        when(findIterable.sort(sort)).thenReturn(findIterable);
        when(findIterable.limit(10)).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(cursor);

        // Первый hasNext() — true, вернётся entity1
        // Второй hasNext() — true, вернётся entity2
        // Далее упадёт, так как более одного найдено
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(entity1, entity2);
        doNothing().when(cursor).close();

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> dao.findSingleBySpecification(specification));
        assertTrue(exception.getMessage().contains("Expected at most one element"));
    }

    @Test
    void testDeleteBySpecification() {
        // Arrange
        LimitSpecification specification = mock(LimitSpecification.class);
        Bson filter = mock(Bson.class);
        when(specification.filter()).thenReturn(filter);

        DeleteResult deleteResult = mock(DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(3L);
        when(mongoCollection.deleteMany(filter)).thenReturn(deleteResult);

        // Act
        DeleteResult result = dao.deleteBySpecification(specification);

        // Assert
        assertEquals(deleteResult, result);
        verify(mongoCollection).deleteMany(filter);
    }

    @Test
    void testForEachEntity() {
        // Arrange
        TestEntity entity1 = new TestEntity("id1", "Name1");
        TestEntity entity2 = new TestEntity("id2", "Name2");

        MongoCursor<TestEntity> cursor = mock(MongoCursor.class);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(entity1, entity2);

        FindIterable<TestEntity> findIterable = mock(FindIterable.class);
        when(mongoCollection.find()).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(cursor);

        AtomicInteger counter = new AtomicInteger(0);
        Consumer<TestEntity> consumer = entity -> counter.incrementAndGet();

        // Act
        dao.forEachEntity(consumer);

        // Assert
        assertEquals(2, counter.get());
        verify(cursor).close();
    }

    @Test
    void testUpdate_WithNullCreatedAt() {
        // Arrange
        TestEntity entity = new TestEntity("test-id", "Updated Name");
        entity.setVersion(3);
        entity.setCreatedAt(null); // Null createdAt

        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoCollection.findOneById("test-id")).thenReturn(entity);
        when(mongoCollection.replaceOne(any(), eq(entity))).thenReturn(updateResult);

        // Act
        dao.update(entity);

        // Assert
        assertEquals(4, entity.getVersion());
        assertEquals(testTime, entity.getCreatedAt()); // Должно быть установлено
        assertEquals(testTime, entity.getUpdatedAt());
        verify(mongoCollection).replaceOne(any(), eq(entity));
    }

    @Test
    void testUpdate_Fallback_Success() {
        // Arrange
        TestEntity entity = new TestEntity("test-id", "Fallback Name");
        entity.setVersion(0); // версия 0 — старый объект
        entity.setCreatedAt(testTime.minusSeconds(3600));

        // Первый вызов — по версии (не сработает)
        UpdateResult versionedResult = mock(UpdateResult.class);
        when(versionedResult.getModifiedCount()).thenReturn(0L);

        // Второй вызов — fallback (по _id)
        UpdateResult fallbackResult = mock(UpdateResult.class);
        when(fallbackResult.getModifiedCount()).thenReturn(1L); // fallback успешен

        when(mongoCollection.findOneById("test-id")).thenReturn(entity);

        // Первый replaceOne — должен содержать "version" в фильтре
        when(mongoCollection.replaceOne(
                argThat(bson -> bson != null && bson.toBsonDocument().toJson().contains("version")),
                eq(entity))
        ).thenReturn(versionedResult);

        // Fallback — без условия по version
        when(mongoCollection.replaceOne(
                argThat(bson -> bson != null && !bson.toBsonDocument().toJson().contains("version")),
                eq(entity))
        ).thenReturn(fallbackResult);

        // Act
        dao.update(entity);

        // Assert
        assertEquals(1, entity.getVersion()); // version должен увеличиться
        assertEquals(testTime, entity.getUpdatedAt());
        assertEquals(testTime.minusSeconds(3600), entity.getCreatedAt()); // createdAt не трогается

        // Проверка, что оба вызова replaceOne произошли
        verify(mongoCollection, times(2)).replaceOne(any(Bson.class), eq(entity));
    }

    @Test
    void testUpdate_Fallback_Failure() {
        // Arrange
        TestEntity entity = new TestEntity("test-id", "Fallback Failure");
        entity.setVersion(0);
        entity.setCreatedAt(testTime.minusSeconds(3600));

        UpdateResult versionedResult = mock(UpdateResult.class);
        when(versionedResult.getModifiedCount()).thenReturn(0L);

        UpdateResult fallbackResult = mock(UpdateResult.class);
        when(fallbackResult.getModifiedCount()).thenReturn(0L); // fallback тоже не сработал

        when(mongoCollection.findOneById("test-id")).thenReturn(entity);

        when(mongoCollection.replaceOne(
                argThat(bson -> bson != null && bson.toBsonDocument().toJson().contains("version")),
                eq(entity))
        ).thenReturn(versionedResult);

        when(mongoCollection.replaceOne(
                argThat(bson -> bson != null && !bson.toBsonDocument().toJson().contains("version")),
                eq(entity))
        ).thenReturn(fallbackResult);

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> dao.update(entity));
        assertTrue(exception.getMessage().contains("test-id"));

        verify(mongoCollection, times(2)).replaceOne(any(Bson.class), eq(entity));
    }

    @Test
    void testCountBySpecification() {
        // Arrange
        LimitSpecification specification = mock(LimitSpecification.class);
        Bson filter = mock(Bson.class);
        when(specification.filter()).thenReturn(filter);
        when(mongoCollection.countDocuments(filter)).thenReturn(123L);

        // Act
        long result = dao.countBySpecification(specification);

        // Assert
        assertEquals(123L, result);
        verify(mongoCollection).countDocuments(filter);
    }

    // Тестовая сущность для тестирования
    @MongoCollection(name = "test_entity")
    static class TestEntity extends DbEntity {
        private String id;
        private String name;

        public TestEntity() {
        }

        public TestEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
