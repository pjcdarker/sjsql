package io.github.reader.sjsql.bean;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassUtils {

    private static final Map<Class<?>, Map<String, BeanProperty>> bean_properties = new ConcurrentHashMap<>(
        64);

    private static final Map<Class<?>, MethodHandle> class_constructors = new ConcurrentHashMap<>(
        64);

    private static final Map<Class<?>, Class<?>> primitiveTypes = Map.of(
        boolean.class, Boolean.class,
        byte.class, Byte.class,
        char.class, Character.class,
        short.class, Short.class,
        int.class, Integer.class,
        long.class, Long.class,
        float.class, Float.class,
        double.class, Double.class
    );


    private static final Lookup lookup = MethodHandles.lookup();

    private ClassUtils() {
    }

    public static <T> T newInstance(Class<T> tClass) throws Throwable {
        return (T) class_constructors.computeIfAbsent(tClass, key -> {
            Lookup inLookup = lookup.in(tClass);
            try {
                return inLookup.unreflectConstructor(tClass.getDeclaredConstructor());
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }).invoke();
    }

    public static List<BeanProperty> persistentBeanProperties(Class<?> clazz) {
        Map<String, BeanProperty> descriptors = getBeanProperties(clazz);
        return descriptors.values()
                          .stream()
                          .filter(BeanProperty::hasWriteMethod)
                          .filter(BeanProperty::hasReadMethod)
                          .filter(e -> isSimpleType(e.getPropertyType()))
                          .toList();
    }

    public static BeanProperty getBeanProperty(Class<?> clazz, String propertyName) {
        final Map<String, BeanProperty> descriptors = getBeanProperties(clazz);
        String newFieldName = toCamelCase(propertyName);
        return descriptors.get(newFieldName);
    }

    private static Map<String, BeanProperty> getBeanProperties(Class<?> clazz) {
        return bean_properties.computeIfAbsent(clazz, key -> {
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(clazz, Object.class);
                PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                Map<String, BeanProperty> map = new ConcurrentHashMap<>(propertyDescriptors.length * 2);
                Lookup inLookup = lookup.in(clazz);
                for (PropertyDescriptor pd : propertyDescriptors) {
                    map.put(pd.getName(), new BeanProperty(pd, inLookup));
                }
                return map;
            } catch (IllegalAccessException | IntrospectionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static String toCamelCase(String fieldName) {
        if (fieldName == null || !fieldName.contains("_")) {
            return fieldName;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = fieldName.split("_");
        result.append(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)))
                      .append(parts[i].substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    public static String toSnakeCase(String fieldName) {
        StringBuilder snakeCase = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    snakeCase.append("_");
                }
                snakeCase.append(Character.toLowerCase(c));
            } else {
                snakeCase.append(c);
            }
        }
        return snakeCase.toString();
    }

    public static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
            || clazz == String.class
            || clazz == Byte.class
            || clazz == Short.class
            || clazz == Integer.class
            || clazz == Long.class
            || clazz == Boolean.class
            || clazz == Double.class
            || clazz == Float.class
            || clazz == BigDecimal.class
            || clazz == BigInteger.class
            || clazz == LocalDateTime.class
            || clazz == LocalDate.class
            || clazz == Date.class;
    }

    public static boolean isMapType(Class<?> clazz) {
        return clazz != null && Map.class.isAssignableFrom(clazz);
    }

    public static boolean isAssignable(Class<?> targetType, Class<?> valueType) {
        if (targetType.isPrimitive()) {
            Class<?> aClass = primitiveTypes.get(targetType);
            if (aClass != null && aClass == valueType) {
                return true;
            }
        }
        return targetType.isAssignableFrom(valueType);
    }
}
