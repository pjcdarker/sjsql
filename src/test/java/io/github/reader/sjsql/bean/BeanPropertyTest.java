package io.github.reader.sjsql.bean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandles;

class BeanPropertyTest {

    private PropertyDescriptor nameProperty;
    private PropertyDescriptor ageProperty;
    private PropertyDescriptor booleanProperty;

    @BeforeEach
    void setUp() throws Exception {
        nameProperty = new PropertyDescriptor("name", TestBean.class);
        ageProperty = new PropertyDescriptor("age", TestBean.class);
        booleanProperty = new PropertyDescriptor("active", TestBean.class);
    }

    @Test
    void testBeanPropertyConstructor() throws Exception {
        BeanProperty nameBeanProperty = new BeanProperty(nameProperty, MethodHandles.lookup());
        assertNotNull(nameBeanProperty);
        assertEquals("name", nameBeanProperty.getName());
        assertEquals(String.class, nameBeanProperty.getPropertyType());
        assertTrue(nameBeanProperty.hasReadMethod());
        assertTrue(nameBeanProperty.hasWriteMethod());

        BeanProperty ageBeanProperty = new BeanProperty(ageProperty, MethodHandles.lookup());
        assertNotNull(ageBeanProperty);
        assertEquals("age", ageBeanProperty.getName());
        assertEquals(int.class, ageBeanProperty.getPropertyType());
        assertTrue(ageBeanProperty.hasReadMethod());
        assertTrue(ageBeanProperty.hasWriteMethod());
    }


    @Test
    void testWriteProperty() throws Exception {
        TestBean bean = new TestBean();
        BeanProperty nameBeanProperty = new BeanProperty(nameProperty, MethodHandles.lookup());

        nameBeanProperty.write(bean, "Alice");
        assertEquals("Alice", bean.getName());
    }

    @Test
    void testWriteIntProperty() throws Exception {
        TestBean bean = new TestBean();
        BeanProperty ageBeanProperty = new BeanProperty(ageProperty, MethodHandles.lookup());

        ageBeanProperty.write(bean, 30);
        assertEquals(30, bean.getAge());
    }

    @Test
    void testWriteBooleanProperty() throws Exception {
        TestBean bean = new TestBean();
        BeanProperty booleanBeanProperty = new BeanProperty(booleanProperty, MethodHandles.lookup());

        booleanBeanProperty.write(bean, true);
        assertTrue(bean.isActive());
    }

    @Test
    void testWriteNullValue() throws Exception {
        TestBean bean = new TestBean("John", 25, true);
        BeanProperty nameBeanProperty = new BeanProperty(nameProperty, MethodHandles.lookup());

        nameBeanProperty.write(bean, null);
        assertNull(bean.getName());
    }

    @Test
    void testWriteTypeMismatch() throws Exception {
        TestBean bean = new TestBean();
        BeanProperty nameBeanProperty = new BeanProperty(nameProperty, MethodHandles.lookup());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            nameBeanProperty.write(bean, 123);
        });

        exception.printStackTrace();

        assertTrue(exception.getMessage().contains("Value type mismatch"));
        assertTrue(exception.getMessage().contains("Expected: class java.lang.String"));
        assertTrue(exception.getMessage().contains("Actual: class java.lang.Integer"));
    }

    @Test
    void testWriteCompatibleType() throws Exception {
        TestBean bean = new TestBean();
        BeanProperty ageBeanProperty = new BeanProperty(ageProperty, MethodHandles.lookup());

        assertDoesNotThrow(() -> ageBeanProperty.write(bean, Integer.valueOf(25)));
        assertEquals(25, bean.getAge());
    }

    @Test
    void testWriteNullValueNoTypeCheck() throws Exception {
        TestBean bean = new TestBean();
        BeanProperty nameBeanProperty = new BeanProperty(nameProperty, MethodHandles.lookup());

        assertDoesNotThrow(() -> nameBeanProperty.write(bean, null));
        assertNull(bean.getName());
    }

    public static class TestBean {

        private String name;
        private int age;
        private boolean active;

        public TestBean() {
        }

        public TestBean(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestBean testBean = (TestBean) o;
            return age == testBean.age &&
                active == testBean.active &&
                name.equals(testBean.name);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + age;
            result = 31 * result + (active ? 1 : 0);
            return result;
        }
    }
}
