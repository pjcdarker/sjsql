package com.reader.sjsql.wrapper;

import com.reader.sjsql.result.ClassUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *
 * Example:
 * <pre>
 * Account account = new Account();
 * EntityWrapper<Account> wrapper = EntityWrapper.wrapper(account);
 * wrapper.set(a -> {
 *     a.setName("New Name");
 *     a.setEmail("new@example.com");
 * });
 * OR
 * wrapper.ref().setName("New Name");
 * wrapper.ref().setEmail("new@example.com");
 * Map<String, Object> updatedFields = wrapper.updatedFields();
 * </pre>
 */
public class EntityWrapper<T> {

    private final T entity;
    private final Map<String, Object> originalFieldValues;

    private EntityWrapper(T entity) {
        this.entity = entity;
        this.originalFieldValues = ClassUtils.persistentFieldValues(this.entity);
    }

    public static <T> EntityWrapper<T> wrapper(T entity) {
        return new EntityWrapper<>(entity);
    }

    public EntityWrapper<T> set(Consumer<T> fieldSetter) {
        fieldSetter.accept(this.entity);
        return this;
    }

    public T ref() {
        return this.entity;
    }

    public Map<String, Object> updatedFields() {
        Map<String, Object> updatedFields = new LinkedHashMap<>();

        Map<String, Object> currentFieldValues = ClassUtils.persistentFieldValues(entity);
        currentFieldValues.forEach((fieldName, currentValue) -> {
            Object originalValue = originalFieldValues.get(fieldName);
            if (!Objects.equals(currentValue, originalValue)) {
                updatedFields.put(fieldName, currentValue);
            }
        });

        return updatedFields;
    }

}