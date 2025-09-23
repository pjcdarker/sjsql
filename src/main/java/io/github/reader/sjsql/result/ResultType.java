package io.github.reader.sjsql.result;


import io.github.reader.sjsql.bean.BeanProperty;
import io.github.reader.sjsql.bean.ClassUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

/**
 * SQL resultType mapping.
 */
public class ResultType<T> {

    private final Class<T> resultType;
    // list element type
    private Class<?> elementType;
    private final Map<String, String> aliasObjectFieldMap = new HashMap<>();
    private boolean ignoreUnknownField;

    private ResultType(Class<T> resultType) {
        this.resultType = resultType;
    }

    private ResultType(Class<T> resultType, Class<?> elementType) {
        this(resultType);
        this.elementType = elementType;
    }

    public static <T> ResultType<T> of(Class<T> tClass) {
        return new ResultType<>(tClass);
    }

    public static <E> ResultType<List<E>> forList(Class<E> elementType) {
        return new ResultType<>((Class) List.class, elementType);
    }

    public static ResultType<List<Map<String, Object>>> forMapList() {
        return (ResultType<List<Map<String, Object>>>) new ResultType<>((Class) List.class, Map.class);
    }

    public T mapping(ResultSet rs) throws Throwable {
        final List<T> results = mappingList(rs);
        return results.isEmpty() ? null : results.getFirst();
    }

    public List<T> mappingList(ResultSet rs) throws Throwable {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            rows.add(row);
        }

        return processRows(rows);
    }

    private List<T> processRows(List<Map<String, Object>> rows) throws Throwable {
        List<T> results = new ArrayList<>();

        // simple type
        if (ClassUtils.isSimpleType(this.resultType)) {
            for (Map<String, Object> row : rows) {
                row.forEach((columnName, value) -> {
                    Object convertedValue = TypeConverter.convert(value, this.resultType);
                    results.add((T) convertedValue);
                });
            }
            return results;
        }

        // Map, List<Map>
        if (ClassUtils.isMapType(this.resultType) || ClassUtils.isMapType(this.elementType)) {
            for (Map<String, Object> row : rows) {
                results.add((T) row);
            }
            return results;
        }

        for (Map<String, Object> row : rows) {
            T instance = newInstance();
            Map<String, Object> fieldObjects = new HashMap<>();
            for (Entry<String, Object> entry : row.entrySet()) {
                String columnName = entry.getKey();
                Object value = entry.getValue();
                if (columnName.contains(".")) {
                    mappingObjectField(instance, fieldObjects, columnName, value);
                    continue;
                }

                final BeanProperty bp = getBeanProperty(instance.getClass(), columnName);
                if (bp != null && bp.hasWriteMethod()) {
                    Object convertValue = TypeConverter.convert(value, bp.getPropertyType());
                    bp.write(instance, convertValue);
                }
            }

            results.add(instance);
        }

        return results;
    }


    private void mappingObjectField(T instance, Map<String, Object> fieldObjectCache, String columnName, Object value)
        throws Throwable {
        String[] columnNames = columnName.split("\\.");
        Object lastFieldObjectInstance = instance;
        Class<?> fieldType = instance.getClass();

        final StringJoiner columnAliasJoiner = new StringJoiner(".");
        for (int i = 0; i < columnNames.length; i++) {
            String column = aliasObjectFieldMap.getOrDefault(columnNames[i], columnNames[i]);
            BeanProperty bp = getBeanProperty(fieldType, column);
            if (bp == null) {
                continue;
            }

            if (i == columnNames.length - 1) {
                if (bp.hasWriteMethod()) {
                    Object convertValue = TypeConverter.convert(value, bp.getPropertyType());
                    bp.write(lastFieldObjectInstance, convertValue);
                }
                continue;
            }

            columnAliasJoiner.add(column);
            String columnAlias = columnAliasJoiner.toString();
            fieldType = bp.getPropertyType();
            Object fieldObjectInstance = fieldObjectCache.get(columnAlias);
            if (fieldObjectInstance == null) {
                fieldObjectInstance = fieldType.getDeclaredConstructor().newInstance();
                fieldObjectCache.put(columnAlias, fieldObjectInstance);

                if (bp.hasWriteMethod()) {
                    bp.write(lastFieldObjectInstance, fieldObjectInstance);
                }
            }

            lastFieldObjectInstance = fieldObjectInstance;
        }
    }

    private BeanProperty getBeanProperty(Class<?> clazz, String columnName) throws Exception {
        BeanProperty bp = ClassUtils.getBeanProperty(clazz, columnName);
        if (bp == null) {
            if (ignoreUnknownField) {
                return null;
            }

            throw new NoSuchFieldException(clazz + " cannot found field: " + columnName);
        }
        return bp;
    }

    public boolean isCollectionType() {
        return Collection.class.isAssignableFrom(resultType);
    }

    public ResultType<T> ignoreUnknownField(boolean enabled) {
        this.ignoreUnknownField = enabled;
        return this;
    }

    public ResultType<T> aliasObjectField(String aliasPrefix, String fieldName) {
        aliasObjectFieldMap.put(aliasPrefix, fieldName);
        return this;
    }

    private T newInstance() throws Throwable {
        Class<?> targetClass = (elementType != null) ? elementType : resultType;
        return (T) ClassUtils.newInstance(targetClass);
    }

}
