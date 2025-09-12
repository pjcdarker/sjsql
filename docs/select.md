# example

## basic

```java

SqlSelect sqlSelect = SqlSelect.from("accounts");

// SELECT * FROM accounts;
```

## column

```java

SqlSelect sqlSelect = SqlSelect.from("accounts") 
                               .select("id", "name", "email");

// SELECT id,name,email FROM accounts;

```

## dynamic columns

```java

Account account = new Account();
boolean isAdmin = account.isAdmin(); // false

SqlSelect sqlSelect = SqlSelect.from("accounts")
                               .addColumn("id")
                               .addColumn("name", "account_name", true)
                               .addColumn("tenant_id", "tenantId", isAdmin);

// SELECT id,name AS account_name FROM accounts;

```

## where

```java

SqlSelect sqlSelect = SqlSelect.from("accounts") 
                               .where("id", Op.eq(1));

// SELECT * FROM accounts WHERE id = ?;

```

## multi where

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .where
    .and("enabled", Op.eq(true))
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

## multi where [and_ex, or_ex] declare if the param is null or blank, the condition will ignore

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

## where like('xxx'), _like('%xxx'), like_('xxx%'), like&_('%xxx%')

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .where("name", Op.like_("Tom")); // Tom%

// SELECT * FROM accounts WHERE name LIKE ?;


```

## where [IN, EXISTS] sub query

```java

SqlSelect subQuery = SqlSelect
    .from("orders") 
    .select("account_id") 
    .where("status", Op.eq("completed"));

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .where("id", Op.in(subQuery));

// SELECT * FROM accounts 
// WHERE id IN (SELECT account_id FROM orders WHERE status = ?);

```


## order by and limit

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .orderBy("create_time", false) // false: asc
    .limit(10);

// SELECT * FROM accounts ORDER BY create_time ASC LIMIT 10;
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

## union, union all

```java

SqlSelect select1 = SqlSelect.from("accounts") .where("enabled", Op.eq(true));
SqlSelect select2 = SqlSelect.from("accounts") .where("create_time", Op.gt(LocalDateTime.now().minusDays(7)));
SqlSelect sqlSelect = select1.union(select2);

// SELECT * FROM accounts WHERE enabled = ? 
// UNION 
// SELECT * FROM accounts WHERE create_time > ?;

```

## sub table

```java

SqlSelect subQuery = SqlSelect
    .from("accounts") 
    .select("department", "COUNT(*) as emp_count") 
    .groupBy("department");
SqlSelect sqlSelect = SqlSelect
    .from(subQuery, "a") 
    .where("a.emp_count", Op.gt(10));
// SELECT * FROM 
// (SELECT department, COUNT(*) as emp_count FROM accounts GROUP BY department) a 
// WHERE a.emp_count > ?;

```