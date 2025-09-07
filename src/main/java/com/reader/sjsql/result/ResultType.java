package com.reader.sjsql.result;


import java.lang.reflect.Field;
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

        return toResultType(rows);
    }

    private List<T> toResultType(List<Map<String, Object>> rows) throws Throwable {
        List<T> results = new ArrayList<>();

        // Map, List<Map>
        if (isMapType(this.resultType) || isMapType(this.elementType)) {
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
                    mappingNestedField(instance, fieldObjects, columnName, value);
                    continue;
                }

                Field field = getField(instance.getClass(), columnName);
                if (field != null) {
                    Object convertValue = TypeConverter.convert(value, field.getType());
                    ClassUtils.setFieldValue(instance, field, convertValue);
                }
            }

            results.add(instance);
        }

        return results;
    }


    private void mappingNestedField(T instance, Map<String, Object> fieldObjectCache, String columnName, Object value)
        throws Throwable {
        String[] columnNames = columnName.split("\\.");
        Object lastFieldObjectInstance = instance;
        Class<?> fieldType = instance.getClass();

        final StringJoiner columnAliasJoiner = new StringJoiner(".");
        for (int i = 0; i < columnNames.length; i++) {
            String column = aliasObjectFieldMap.getOrDefault(columnNames[i], columnNames[i]);
            Field field = getField(fieldType, column);
            if (field == null) {
                continue;
            }

            if (i == columnNames.length - 1) {
                Object convertValue = TypeConverter.convert(value, field.getType());
                ClassUtils.setFieldValue(lastFieldObjectInstance, field, convertValue);
                continue;
            }

            columnAliasJoiner.add(column);
            String columnAlias = columnAliasJoiner.toString();
            fieldType = field.getType();
            Object fieldObjectInstance = fieldObjectCache.get(columnAlias);
            if (fieldObjectInstance == null) {
                fieldObjectInstance = fieldType.getDeclaredConstructor().newInstance();
                fieldObjectCache.put(columnAlias, fieldObjectInstance);

                ClassUtils.setFieldValue(lastFieldObjectInstance, field, fieldObjectInstance);
            }

            lastFieldObjectInstance = fieldObjectInstance;
        }
    }

    private Field getField(Class<?> clazz, String columnName) throws Exception {
        Field field = ClassUtils.getFieldByName(clazz, columnName);
        if (field == null) {
            if (ignoreUnknownField) {
                return null;
            }

            throw new NoSuchFieldException(clazz + " cannot found field: " + columnName);
        }
        return field;
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

    private T newInstance() throws Exception {
        Class<?> targetClass = (elementType != null) ? elementType : resultType;
        return (T) targetClass.getDeclaredConstructor().newInstance();
    }


    private boolean isMapType(Class<?> clazz) {
        return clazz != null && Map.class.isAssignableFrom(clazz);
    }


}
