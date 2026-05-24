# example

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
                               .noWhereClause();

// delete from accounts;

```

# batch delete

## entities

```java

SqlDelete sqlDelete = SqlDelete.batch("accounts", List.of(account1, account2))
                               .where("id", Op.eq(RefValue.ref("id"))); // $.id = account.id


```

## maps

```java

SqlDelete sqlDelete = SqlDelete.batch("accounts", List.of(map1, map2))
                               .where("id", Op.eq(RefValue.ref("id"))); // $.id = map.get("id")

```