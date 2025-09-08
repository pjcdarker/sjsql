package com.reader.sjsql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reader.sjsql.model.Account;
import com.reader.sjsql.wrapper.EntityWrapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

class EntityWrapperTest {

    @Test
    void should_create_wrapper_with_ref() {
        Account account = new Account();
        EntityWrapper<Account> wrapper = EntityWrapper.wrapper(account);

        assertNotNull(wrapper);
        assertSame(account, wrapper.ref());
    }

    @Test
    void should_capture_updated_fields() {
        Account account = new Account();
        EntityWrapper<Account> wrapper = EntityWrapper.wrapper(account);
        wrapper.set(a -> a.setName("Test Name"))
               .set(a -> a.setEmail("test@example.com"))
               .set(a -> a.setEnabled(true));

        Map<String, Object> updatedFields = wrapper.updatedFields();
        assertEquals(3, updatedFields.size());
        assertEquals("Test Name", updatedFields.get("name"));
        assertEquals("test@example.com", updatedFields.get("email"));
        assertEquals(true, updatedFields.get("enabled"));
    }

    @Test
    void should_capture_updated_fields_when_call_method_to_update_multi_field() {
        Account account = new Account();
        EntityWrapper<Account> wrapper = EntityWrapper.wrapper(account);
        wrapper.set(a -> a.setId(1L))
               .set(a -> a.setTestInfo("entity@test.com"));

        Map<String, Object> updatedFields = wrapper.updatedFields();
        assertEquals(4, updatedFields.size());
        assertTrue(updatedFields.containsKey("id"));
    }

    @Test
    void should_capture_null_field_updates() {
        Account account = new Account();
        // account.setName("test");
        EntityWrapper<Account> wrapper = EntityWrapper.wrapper(account);

        wrapper.set(a -> a.setName(null))
               .set(a -> a.setEmail("test@example.com"));

        Map<String, Object> updatedFields = wrapper.updatedFields();
        assertEquals(1, updatedFields.size());
        assertNull(updatedFields.get("name"));
        assertEquals("test@example.com", updatedFields.get("email"));
    }

    @Test
    void should_not_capture_unset_fields() {
        Account account = new Account();
        account.setName("Existing Name");
        account.setEmail("existing@example.com");

        EntityWrapper<Account> wrapper = EntityWrapper.wrapper(account);

        wrapper.set(a -> a.setCode("NEWCODE"));

        Map<String, Object> updatedFields = wrapper.updatedFields();
        assertEquals(1, updatedFields.size());
        assertEquals("NEWCODE", updatedFields.get("code"));

        assertFalse(updatedFields.containsKey("name"));
        assertFalse(updatedFields.containsKey("email"));
    }


}
