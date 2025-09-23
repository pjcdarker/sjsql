package io.github.reader.sjsql.bean;

import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;

public class BeanProperty {

    private final String name;
    private final Class<?> propertyType;
    private MethodHandle readMH;
    private MethodHandle writeMH;

    public BeanProperty(PropertyDescriptor pd, Lookup lookup) throws IllegalAccessException {
        this.name = pd.getName();
        this.propertyType = pd.getPropertyType();

        Method readMethod = pd.getReadMethod();
        if (readMethod != null) {
            this.readMH = lookup.unreflect(readMethod);
        }

        Method writeMethod = pd.getWriteMethod();
        if (writeMethod != null) {
            this.writeMH = lookup.unreflect(writeMethod);
        }
    }

    public String getName() {
        return name;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }

    public Object read(Object bean) {
        if (this.readMH == null) {
            return null;
        }

        try {
            return this.readMH.invoke(bean);
        } catch (Throwable e) {
            throw new BeanPropertyInvocationException("The property[" + name + "] getter invoke exception", e);
        }
    }

    public void write(Object bean, Object value) {
        if (this.writeMH == null) {
            return;
        }

        if (value != null && !ClassUtils.isAssignable(propertyType, value.getClass())) {
            throw new IllegalArgumentException("Value type mismatch. "
                + "Expected: " + propertyType
                + ", Actual: " + value.getClass());
        }

        try {
            this.writeMH.invoke(bean, value);
        } catch (Throwable e) {
            throw new BeanPropertyInvocationException("The property[" + name + "] setter invoke exception", e);
        }
    }

    public boolean hasReadMethod() {
        return this.readMH != null;
    }

    public boolean hasWriteMethod() {
        return this.writeMH != null;
    }


    static class BeanPropertyInvocationException extends RuntimeException {

        public BeanPropertyInvocationException(String msg, Throwable e) {
            super(msg, e);
        }
    }
}

