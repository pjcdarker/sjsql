# example

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

## maps

```java

List<Map<String, Object>> maps = List.of(map1, map2, map3);
SqlUpdate sqlUpdate = SqlUpdate.batch("accounts", maps)
                               .set$("name") 
                               .set$("email") 
                               .where("id", Op.eq(RefValue.ref("id")));

// update accounts set name=?, email=? where id=?;
// update accounts set name=?, email=? where id=?;
// update accounts set name=?, email=? where id=?;


```