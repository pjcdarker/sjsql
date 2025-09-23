package io.github.reader.sjsql;

import io.github.reader.sjsql.bean.BeanProperty;
import io.github.reader.sjsql.bean.ClassUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RefValue {

    private static final String PREFIX = "$.";

    public static String ref(String fieldName) {
        return PREFIX + fieldName;
    }

    public static Object replace(Object instance, Object value) {
        if (value instanceof String valueString && valueString.startsWith(PREFIX)) {
            String fieldName = valueString.substring(PREFIX.length());
            if (instance instanceof Map<?, ?> map) {
                return map.get(fieldName);
            }

            BeanProperty beanProperty = ClassUtils.getBeanProperty(instance.getClass(),
                ClassUtils.toCamelCase(fieldName));
            if (beanProperty != null && beanProperty.hasReadMethod()) {
                return beanProperty.read(instance);
            }
            throw new IllegalArgumentException(
                "The property[" + fieldName + "] getter method is not exist in "
                    + instance.getClass().getCanonicalName());
        }

        return value;
    }

    public static void replaceForList(Object entity, List<Object> values) {
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            Object newValue = RefValue.replace(entity, value);
            if (newValue != null && !Objects.equals(value, newValue)) {
                // replace oldValue with newValue
                values.set(i, newValue);
            }
        }
    }
}
