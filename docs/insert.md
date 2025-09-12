# example

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

## maps

```java

SqlInsert sqlInsert = SqlInsert.batch("accounts", List.of(map1,map2,map3));

```

## override field of entities value

```java

List<Account> accounts = List.of(account1, account2, account3); 
SqlInsert sqlInsert = SqlInsert.batch("accounts", accounts)
                               // all account.create_time will set same value
                               .values("create_time", LocalDateTime.now()); 
// INSERT INTO accounts (name,email,code,create_time) VALUES (?,?,?,?);
// INSERT INTO accounts (name,email,code,create_time) VALUES (?,?,?,?);
// INSERT INTO accounts (name,email,code,create_time) VALUES (?,?,?,?);

```