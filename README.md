# sjsql

simple java sql

## requirements

- jdk21+

## maven

```xml

<dependency>
    <groupId>io.github.pjcdarker</groupId>
    <artifactId>sjsql</artifactId>
    <version>0.2.0</version>
</dependency>

```

## select

### column

```java

SqlSelect sqlSelect = SqlSelect.from("accounts") 
                               .select("id", "name", "email");

// SELECT id,name,email FROM accounts;

```

### dynamic columns

```java

Account account = new Account();
boolean isAdmin = account.isAdmin(); // false

SqlSelect sqlSelect = SqlSelect.from("accounts")
                               .addColumn("id")
                               .addColumn("name", "account_name", true)
                               .addColumn("tenant_id", "tenantId", isAdmin);

// SELECT id,name AS account_name FROM accounts;

```

### where condition
```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .where
    .and_ex("enabled", Op.eq(true))
    .and_ex("code", Op.eq(null))
    .and("create_time", Op.gt(LocalDateTime.now().minusDays(30)))
    //.or("name", Op.neq(""))
    //.and("name", Op.neq(""))
    .end();

// SELECT * FROM accounts WHERE enabled = ? AND create_time > ?;

```

## dynamic where

```java

Account account = new Account();

boolean isAdmin = account.isAdmin();

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .where
    .and("tenant_id", Op.eq(account.getTenantId()), !isAdmin)
    .and("create_time", Op.gt(LocalDateTime.now().minusDays(30)))
    //.or("name", Op.neq(""))
    .and_ex("phone", Op.neq("aaa"), isAdmin)
    .end();

// SELECT * FROM accounts WHERE enabled = ? AND create_time > ?;

```

## offset and limit

```java

SqlSelect sqlSelect = SqlSelect.from("accounts") 
                               .orderBy("id") 
                               .limit(20, 10); // offset=20, limit=10

// SELECT * FROM accounts ORDER BY id LIMIT 20, 10;


```

## join, self join, left join, right join, full outer join

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts", "a") 
    .select("a.id", "a.name", "t.name as tenant_name") 
    .join("tenant t", "a.tenant_id", "t.id");
// SELECT a.id, a.name, t.name as tenant_name 
// FROM accounts a 
// JOIN tenant t ON a.tenant_id = t.id;

```

## group by and having

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .select("department", "COUNT(1) as count") 
    .groupBy("department") 
    .having("count", Op.gt(1));

// SELECT department, COUNT() as count 
// FROM accounts 
// GROUP BY department 
// HAVING count > ?;

```

## resultType

```java

class Account {
    Long id;
    String name;
    Tenant tenant;
}

class Tenant {
    Long id;
    String name;
}


```
## Object Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .select("id", "name", "email") 
    .where("id", Op.eq(1));

Account account = jdbcClient.queryForObject(sqlSelect, Account.class);

// OR

Account account = jdbcClient.queryForObject(sqlSelect, ResultType.of(Account.class));

```

## List Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .select("id", "name", "email") 
    .orderBy("id");

List<Account> accounts = jdbcClient.queryForList(sqlSelect, ResultType.forList(Account.class));

```

## Object Field Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts", "a") 
    .select("a.id", "a.name") 
    .addColumn("b.id", "tenant.id") // tenant.id mapping account.tenant.id 
    .addColumn("b.name", "tenant.name") // tenant.name mapping account.tenant.name 
    .leftJoin("tenant b", "a.id", "b.account_id");

Account account = jdbcClient.executeQuery(sqlSelect, ResultType.of(Account.class));
// Result: account.getTenant().getId() and account.getTenant().getName() will be correctly set

```

## Configuring Alias to Object Field Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts", "a") 
    .select("a.id", "a.name") 
    .addColumn("b.id", "b.id") 
    .addColumn("b.name", "b.name") 
    .leftJoin("tenant b", "a.id", "b.account_id");
ResultType<Account> resultType = ResultType.of(Account.class).aliasObjectField("b", "tenant"); // Map alias "b" to "tenant" field
Account account = jdbcClient.executeQuery(sqlSelect, resultType);


```

## Multi-level Object Field Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts", "a") 
    .select("a.id", "a.name") 
    .addColumn("b.id", "tenant.id") 
    .addColumn("b.name", "tenant.name") 
    .addColumn("c.id", "tenant.paymentOrder.id") 
    .addColumn("c.trade_no", "tenant.paymentOrder.tradeNo") 
    .leftJoin("tenant b", "a.id", "b.account_id") 
    .leftJoin("payment_order c", "b.id", "c.tenant_id");

Account account = jdbcClient.executeQuery(sqlSelect, ResultType.of(Account.class));

// Results: 
// account.getTenant().getId() 
// account.getTenant().getName() 
// account.getTenant().getPaymentOrder().getId() 
// account.getTenant().getPaymentOrder().getTradeNo()

```

## ignore Unknown fields

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .select("id", "name", "code AS unknown_field");

ResultType<Account> resultType = ResultType.of(Account.class).ignoreUnknownField(true); // Ignore unknown fields
Account account = jdbcClient.executeQuery(sqlSelect, resultType);

// unknown_field will not map to account

```

## insert

## basics

```java

SqlInsert sqlInsert = SqlInsert.into("accounts")
                               .values("name", "Tom")
                               .values("email", "tom@example.com");
// INSERT INTO accounts (name,email) VALUES (?,?);
```

## entity

```java

Account account = new Account(); 
account.setName("Tom"); 
account.setEmail("tom@example.com"); 
account.setCode("USER001");
SqlInsert sqlInsert = SqlInsert.into("accounts", account);
// INSERT INTO accounts (name,email,code) VALUES (?,?,?);

```

## map

```java

Map<String, Object> map = new HashMap<>();
map.put("name", "Tom");
map.put("code", "USER001");
map.put("email", "tom@test.com");

SqlInsert sqlInsert = SqlInsert.into("accounts", map);
// INSERT INTO accounts (name,email,code) VALUES (?,?,?);

```

# batch

## entities

```java

List<Account> accounts = List.of(account1, account2, account3); 
SqlInsert sqlInsert = SqlInsert.batch("accounts", accounts);
// INSERT INTO accounts (name,email,code) VALUES (?,?,?);
// INSERT INTO accounts (name,email,code) VALUES (?,?,?);
// INSERT INTO accounts (name,email,code) VALUES (?,?,?);

```

## update

## basic

```java

SqlUpdate sqlUpdate = SqlUpdate.table("accounts")
    .set("name", "Tom")
    .where("id", Op.eq(1));

// update accounts set name = ? where id = ?;
// update accounts set name = 'Tom' where id = 1;

```

## entity

```java

Account account = new Account();
account.setName("Tom"); 
account.setEmail("tom@example.com"); 
account.setCode("USER001");

SqlUpdate sqlUpdate = SqlUpdate.table("accounts", account)
    .set$("name") // set name=account.name
    .set$("code") // set code=account.code
    .set$("email") // set email=account.email
    .where("id", eq(1));

// update accounts set name=?, code=?, email=? where id=?;

```

## map

```java

Map<String, Object> map = new HashMap<>();
map.put("name", "Tom");
map.put("code", "aaa");
map.put("email", "123@qq.com");

SqlUpdate sqlUpdate = SqlUpdate.table("accounts", map)
                               .set$("name") // set name=map.name
                               .set$("code") // set code=map.code
                               .set$("email")
                               .where("code", eq("111"));

// update accounts set name=?, code=?, email=? where code=?;

```


# batch example

## entities

```java

SqlUpdate sqlUpdate = SqlUpdate.batch("accounts", List.of(account1, account2))
    .set$("name")
    .set$("code")
    .set$("email")
    .where("create_time", gt(RefValue.ref("createTime")))
    .where("code", eq(RefValue.ref("code")))
    ;

// update accounts set name=?, code=?, email=? where create_time>? and code=?;
// update accounts set name=?, code=?, email=? where create_time>? and code=?;

``` 

## delete

## basic

```java

SqlDelete.from("accounts").where("id",1);

// delete from accounts where id = 1;

```

```java

SqlDelete.from("accounts").where.and("id",1).end();

// delete from accounts where id = 1;

```

## without where clause

```java

SqlDelete sqlDelete = SqlDelete.from("accounts")
                               .agree_without_where_clause(true);

// delete from accounts;

```

# batch delete

## entities

```java

SqlDelete sqlDelete = SqlDelete.batch("accounts", List.of(account1, account2))
                               .where("id", Op.eq(RefValue.ref("id"))); // $.id = account.id


```

## [select examples](./docs/select.md)

## [insert examples](./docs/insert.md)

## [update examples](./docs/update.md)

## [delete examples](./docs/delete.md)

## [resultType examples](./docs/result-type.md)

