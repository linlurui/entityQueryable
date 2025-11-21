[![使用IntelliJ IDEA开发维护](https://img.shields.io/badge/IntelliJ%20IDEA-提供支持-blue.svg)](https://www.jetbrains.com/idea/)

**EntityQueryable 是一个轻量级的 ORM 库**

（EntityQueryable is a Micro ORM library）


<p align="right">
  <img align="right" src="https://gitee.com/linlurui/entityqueryable/raw/main/pay5.jpg" alt="捐赠给作者" width="200">
  <em>请作者喝饮料</em>
</p>


<p style="font-size:16px;line-height:40px;"><strong>EntityQueryable 是面向 Java 平台的轻量级 ORM：以 fluent API 构建 SQL，利用方法返回类型在编译期约束链式调用顺序（如 `form → where → and/or → group → select`），无需 AST 解析即可确保语义合法；同时兼容 MyBatis 风格 XML 作为复杂查询的补充，两种方式共用同一 `QueryableAction` 执行管线。内置多数据源映射、联表查询、异步批量写、Druid 连接池与可选 RxJava2-JDBC（默认关闭），覆盖 mysql/mariadb/sqlserver/sqlite/oracle/postgresql 等主流数据库，并提供 SQL/PreparedSql 预览、可插拔诊断追踪、元数据探查、动态数据源与热切换支持，既可走链式 DSL，也能按需回退到原生 SQL 与 XML Mapper，交付体验从调试到运维都保持轻盈可控。<br/></strong><p>


### **如何开始**
<div style="font-size:12px;">How to start?</div>


#### **1.先下载数据库驱动并创建 `db-config.xml`**
<div style="font-size:12px;">Download database driver first, then build a "db-config.xml"</div>

```xml
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
  <typeAliases>
    <typeAlias alias="UserEntity" type="entity.UserEntity" />
  </typeAliases>
  <mappers>
    <mapper resource="mybatis/map/user-mapper.xml" />
  </mappers>
  <environments default="mysql">
    <environment id="mysql" classScope="public" >
      <transactionManager type="JDBC"/>
      <dataSource type="POOLED">
        <property name="driver" value="com.mysql.jdbc.Driver" />
        <property name="url" value="jdbc:mysql://127.0.0.1:3306/your_db_name?serverTimezone=Asia/Shanghai&amp;useSSL=false&amp;connectTimeout=60000&amp;socketTimeout=60000&amp;autoReconnect=true" />
        <property name="username" value="root" />
        <property name="password" value="your password" />
      </dataSource>
    </environment>
  </environments>
</configuration>
```

#### **或者在 `/conf/application.yml` 中添加配置**
<div style="font-size:12px;">Or add config to /conf/application.yml</div>

```yaml
# entityQueryable config
entity:
  datasource:
    activated: test #activated datasource
    #configFile: db-config.xml #db-config path
    environments:
      test:
        default: true
        driver: com.mysql.jdbc.Driver
        url: jdbc:mysql://localhost:3306/test?useUnicode=true&amp;characterEncoding=UTF-8&amp;useSSL=false&amp;autoReconnect=true&amp;failOverReadOnly=false&amp;serverTimezone=CTT
        username: root
        password: 123456
```

**诊断日志开关 / Diagnostics toggle**

```yaml
entity:
  query:
    diagnostics:
      enabled: true   # 设置为 false 可完全关闭 QueryDiagnostics 采集
```
若不配置该项，默认开启；当设置为 `false` 时，`QueryDiagnostics.start(...)` 将返回 no-op trace，不再记录 SQL、耗时或缓存命中统计。

> Default is `true`; set to `false` to fully disable query diagnostics so no UUID/timing/counter updates are produced.
#### **2.通过 Maven 引入依赖**
<div style="font-size:12px;">Add the jar package by Maven</div>

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>entity-orm</groupId>
    <artifactId>entity.queryable</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <repositories>
        <repository>
            <id>gitee</id>
            <url>https://gitee.com/linlurui/entityqueryable/raw/v1.0</url>
            <releases>
                <updatePolicy>always</updatePolicy>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <updatePolicy>always</updatePolicy>
                <enabled>true</enabled>
                <checksumPolicy>ignore</checksumPolicy>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>entity-orm</groupId>
            <artifactId>entity.queryable</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

#### **3.让实体继承 `entity.query.Queryable` 基类**

<div style="font-size:12px;">Inherit base class "entity.query.Queryable" in your entity</div>

```java
package cn.entity;

import java.util.Date;

import entity.query.Queryable;
import entity.query.annotation.AutoIncrement;
import entity.query.annotation.DBConfig;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.annotation.Tablename;

@DBConfig(id="mysql")
@Tablename(value="user")
public class User extends Queryable<User> {

	public User() {
		super();
	}

	@PrimaryKey
	@AutoIncrement
	private int id;
	public int getId()
	{
		return id;
	}
	public void setId(int value)
	{
		id = value;
	}

	private String name;
	public String getName()
	{
		return name;
	}
        ...

	@Fieldname(value="create_on")
	private Date createOn;
	public Date getCreateOn()
	{
		return createOn;
	}
	public void setCreateOn(Date value)
	{
		createOn = value;
	}
}
```


### 核心 API 速查
| 使用场景 | 关键方法 | 说明 |
| --- | --- | --- |
| 基础 CRUD | `insert` / `update` / `update(String... exp)` / `delete` | 实体方法自动按注解映射字段，`update(String...)` 允许手写 set 语句 |
| 条件查询 | `where` / `and` / `or` / `select` / `orderby` / `groupby` | 与 MyBatis 语法一致，支持 `#{field}` 占位符 |
| 执行控制 | `query` / `query(Class)` / `query(skip, top)` / `first` / `top` / `exist` / `count` | 同步查询、分页、单条与布尔判断 |
| 组合查询 | `join` / `on` / `union` / `from(select, alias)` / `as` | 支持联表、子查询、UNION 与表别名 |
| 静态链 | `Queryable.from(User.class, "u")` | 直接通过实体 class 构建查询，无需 new |
| SQL预览 | `toString(CommandMode)` / `toString(mode, DBType)` / `getPreparedSql` / `toPreparedSql` | 输出最终 SQL 或 PreparedSql（含参数） |
| 响应式 | `asyncQuery` / `asyncFirst` / `asyncCount` / `asyncInsert` 等 | 基于 RxJava2，返回 `Flowable` / `Maybe` / `Single` |
| 批量操作 | `batchInsert` / `batchUpdate` / `batchDelete` | 复用同一 parser 批量执行 |
| 原生 SQL | `QueryableAction.executeSql(...)` | 支持默认数据源、指定数据源 ID 或自定义 `DataSource` |
| XML SQL | `QueryableAction.executeSqlFromXml(...)` | 读取配置目录下的 XML Mapper，兼容常见 MyBatis 标签但无需引入 MyBatis |
| 元数据 | `getTables` / `getColumns` / `getPrimaryKey` / `selectNow` | 查询库表、字段及数据库时间 |

###### 下面章节会逐一对应每个方法族的典型写法，可直接复制调试。

### **如何使用 EntityQueryable 访问数据库**
<div style="font-size:12px;">How to use EntityQueryable to access database</div>

##### **1.如何插入数据**

<div style="font-size:12px;">How to insert data</div>

```java
@RequestMapping(value="/eqinsert", method=RequestMethod.GET)
public ModelAndView add() {
	User user = new User();
	user.setName("linlurui");
	user.setAccount("linlurui");
	user.setPassword("******");
	user.setMobile("13000000007");
	user.setCreateOn(new Date());
	user.insert();

	return new ModelAndView("hello", "message", "Insert data by entity queryable");
}

```

####  **2.如何更新数据**
<div style="font-size:12px;">How to update data?</div>

```java
@RequestMapping(value="/equpdate", method=RequestMethod.GET)
public ModelAndView edit() {
	User user = new User();
	user.setAccount("linlurui");
	user.setName("linlurui");
	user.setMobile("13300000000");
	user.where("account=#{account}").update("name=#{name}, mobile=#{mobile}");

	return new ModelAndView("hello", "message", "Update data by entity queryable");
}

```

#### **3.如何删除数据**
<div style="font-size:12px;">How to delete data</div>

```java
@RequestMapping(value="/eqdelete", method=RequestMethod.GET)
public ModelAndView remove() {

	User user = new User();
	user.setAccount("linlurui");
	user.where("account=#{account}").delete();

	return new ModelAndView("hello", "message", "Delete data by entity queryable");
}

```
#### **4.如何查询数据**
<div style="font-size:12px;">How to query data</div>

```java
@RequestMapping(value="/eqselect", method=RequestMethod.GET)
public ModelAndView query() {

	User user = new User();
	user.setName("linlurui");
	List<User> list = user
		.where("name=#{name}")
		.where(Condition.OR, "account=#{name}")
		.orderby("id")
		.query();

	return new ModelAndView("data", "list", list);
}

```

#### **5.如何进行联表查询**

<div style="font-size:12px;">How to join table</div>

```java
@RequestMapping(value="/eqjoin", method=RequestMethod.GET)
public ModelAndView joinQuery() {

	User user = new User();
	Auth auth = new Auth();
	user.setName("linlurui");

	List<User> list = user
		.where("account=#{name}")
		.join(JoinMode.Inner, auth, "a")
		.on("[user].[id]=[a].[user_id]")
		.query();

	return new ModelAndView("data", "list", list);
}

```

#### **6.如何进行 UNION 合并**

<div style="font-size:12px;">How to union data</div>

```java
@RequestMapping(value="/equnion", method=RequestMethod.GET)
public ModelAndView unionQuery() {

	User user = new User();
	Auth auth = new Auth();
	user.setName("linlurui");

	List<Auth> list = user
		.where("name=#{name}")
		.select("[id] AS [user_id]")
		.union(auth.select("[user_id]"))
		.query();

	return new ModelAndView("data2", "list", list);
}
```

#### **7.如何返回自定义结果**

<div style="font-size:12px;">How to return custom result</div>

```java
@RequestMapping(value="/eqmyresult", method=RequestMethod.GET)
public ModelAndView myresult() {

	User user = new User();
	user.setName("linlurui");
	List<MyResult> list = user
		.where("1=1")
		.select("COUNT(Id) AS count, SUM(Id) AS sum, MAX(Id) AS max")
		.query(MyResult.class);

	return new ModelAndView("data3", "list", list);
}
```


#### **8.如何查询首条数据**
<div style="font-size:12px;">How to query first</div>

```java
@RequestMapping(value="/eqfirst", method=RequestMethod.GET)
public ModelAndView first() {

	User user = new User();
	user.setName("linlurui");
	List<User> list = new ArrayList<User>();
	User entity = user
		.where("name=#{name}")
		.or("account=#{name}")
		.orderby("id")
		.first();

	list.add(entity);

	return new ModelAndView("data", "list", list);
}
```


#### **9.如何查询前 N 条数据**
<div style="font-size:12px;">How to query top data</div>

```java
@RequestMapping(value="/eqtop", method=RequestMethod.GET)
public ModelAndView top() {

	User user = new User();
	user.setName("linlurui");
	List<User> list = user
		.where("name=#{name}")
		.or("account=#{name}")
		.orderby("id")
		.top(5);

	return new ModelAndView("data", "list", list);
}
```


#### **10.如何判断记录是否存在**

<div style="font-size:12px;">How to check exist</div>

```java
@RequestMapping(value="/eqexist", method=RequestMethod.GET)
public ModelAndView exist() {

	User user = new User();
	user.setName("linlurui");
	List<Boolean> list = new ArrayList<Boolean>();
	Boolean exist = user
		.where("name=#{name}")
		.where(Condition.OR, "account=#{name}")
		.exist();

	list.add(exist);

	return new ModelAndView("dataexist", "list", list);
}

```

#### **11.如何统计数量**

<div style="font-size:12px;">How to count data</div>

```java
@RequestMapping(value="/eqcount", method=RequestMethod.GET)
public ModelAndView count() {

	User user = new User();
	user.setName("linlurui");
	List<Number> list = new ArrayList<Number>();
	Number count = user
		.where("name=#{name}")
		.where(Condition.OR, "account=#{name}")
		.count();

	list.add(count);

	return new ModelAndView("datacount", "list", list);
}
```
#### **12.如何分页查询**
<div style="font-size:12px;">How to query by page</div>

```java
@RequestMapping(value="/eqpage", method=RequestMethod.GET)
public ModelAndView page() {

	int pageIndex = 1;
	int pageSize = 20;
	User user = new User();
	user.setName("linlurui");
	List<User> list = user
		.where("name=#{name}")
		.where(Condition.OR, "account=#{name}")
		.query(pageIndex * pageSize - pageSize, pageSize);

	return new ModelAndView("data", "list", list);
}
```
#### **13.如何直接从实体查询**
<div style="font-size:12px;">select from entity</div>

```java
@RequestMapping(value="/eqfrom", method=RequestMethod.GET)
public ModelAndView from() {

	User user = new User();
	user.setName("linlurui");
	Select<User> s = user
		.where("name=#{name}")
		.where(Condition.OR, "account=#{name}")
		.select("account,name");

	List<User> list = user.from(s, "a").where("name='linlurui'").query();

	return new ModelAndView("data", "list", list);
}
```
#### **14.如何使用静态查询链**
<div style="font-size:12px;">Static query chain (new)</div>

```java
// 1. 普通查询
List<User> list = Queryable.from(User.class, "user")
    .where("id=#{id}", 2)
    .and("parent_id>0")
    .or("parent_id=#{parentId}", -1)
    .orderby("id DESC")
    .query();

// 2. 查询首条
User first = Queryable.from(User.class, "user")
    .where("id=#{id}", 2)
    .and("parent_id>0")
    .first();

// 3. 只生成 SQL
String mysqlSql = Queryable.from(User.class, "user")
    .where("id=#{id}", 2)
    .select("id", "name")
    .toString(CommandMode.Select);
```

> 静态链式调用内部会自动实例化实体并为 `#{field}` 占位符注入值, 可直接复用 `query/first/exist/top/count` 等所有查询能力。
#### **15.如何执行原生 SQL**
<div style="font-size:12px;">How to run raw SQL (executeSql)</div>

```java
// 1. 使用默认/实体推断数据源
List<User> users = QueryableAction.executeSql(
    "SELECT * FROM user WHERE id > ?",
    User.class,
    100
);

// 2. 指定数据源 ID
List<User> shUsers = QueryableAction.executeSql(
    "shard_mysql",
    "SELECT * FROM user WHERE region = ?",
    User.class,
    "sh"
);

// 3. 自定义 DataSource（例如多租户动态构建）
DataSource tenantDs = buildTenantDataSource();
List<Map> summary = QueryableAction.executeSql(
    tenantDs,
    "SELECT status, COUNT(1) as total FROM `order` GROUP BY status",
    Map.class
);
```
#### **20.如何执行 XML 中定义的 SQL**
<div style="font-size:12px;">How to execute SQL defined in MyBatis XML</div>

在 `application.yml` 中声明 XML Mapper 根目录，可使用逗号或分号分隔多个路径，也支持 `classpath:` 前缀。

> Configure XML mapper roots in `application.yml`; multiple paths separated by comma/semicolon and `classpath:` is supported.

```yaml
entity:
  mybatis:
    mapperRoot: config/mybatis-mappers   # 可填绝对路径，也可相对 ${user.dir}
```

轻量解析器在不依赖 MyBatis 的前提下兼容 `<if/>`、`<choose/>`、`<when/>`、`<otherwise/>`、`<foreach/>`、`<trim/>`、`<where/>`、`<set/>`、`<include/>` 等常用标签，并将 `#{}` 替换为安全的预编译参数。

> The lightweight parser (no MyBatis dependency) understands `<if/>`, `<choose/>`, `<when/>`, `<otherwise/>`, `<foreach/>`, `<trim/>`, `<where/>`, `<set/>`, `<include/>`, etc., and keeps `#{}` as prepared-statement placeholders.

```java
// 假设 mapper xml 中存在 namespace="userMapper"，id="selectActive"
Map<String, Object> params = new HashMap<>();
params.put("status", "ACTIVE");

// 默认数据源
List<User> users = QueryableAction.executeSqlFromXml(
    "userMapper.selectActive",
    User.class,
    params
);

// 指定数据源 ID（可切换租户/分库）
List<Map> stats = QueryableAction.executeSqlFromXml(
    "analytics",
    "orderMapper.summary",
    Map.class,
    Collections.singletonMap("region", "sh")
);
```

`mapperRoot` 支持相对路径、绝对路径与 `classpath:` 前缀；如需热加载可重启应用即可重新读取。

> <span style="font-size:13px;">`mapperRoot` accepts relative/absolute paths and `classpath:`; restart the app to reload mappers if needed.</span>
#### **16.如何预览 SQL / PreparedSql**
<div style="font-size:12px;">How to preview SQL / PreparedSql</div>

```java
User user = new User();
user.setName("linlurui");

// 1. 输出不同 CommandMode（Insert/Update/Delete/SelectCount）
String selectSql = user.where("name=#{name}").toString(CommandMode.Select);
String countSql  = user.where("name=#{name}").toString(CommandMode.SelectCount);

// 2. 根据数据库方言动态生成（可强制切换 DBType）
String oracleSql = user.where("name=#{name}")
    .toString(CommandMode.Select, DBType.Oracle);

// 3. 获取 PreparedSql（SQL + 参数）
PreparedSql prepared = user.where("name=#{name}")
    .getPreparedSql(CommandMode.Select);
prepared.getSql();        // 带问号的 SQL
prepared.getParameters(); // Map<Integer,Object> 参数列表
```
#### **17.如何开启 RxJava2-JDBC**
<div style="font-size:12px;">How to enable RxJava2-JDBC</div>
RxJava2-JDBC 默认关闭，仅当你需要 `Flowable` / `Maybe` / `Single` 等异步 API 时再打开即可，避免在纯同步场景下引入额外依赖与线程调度成本。

1. **引入依赖（可选）**
项目已自带如下依赖，如你在业务工程里单独使用 EntityQueryable，请确保 `rxjava2-jdbc` + `rxjava` 位于 classpath：
```xml
<dependency>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>rxjava2-jdbc</artifactId>
    <version>0.2.0</version>
</dependency>
<dependency>
    <groupId>io.reactivex.rxjava2</groupId>
    <artifactId>rxjava</artifactId>
    <version>2.2.19</version>
</dependency>
```

0.2.0 之后的 `rxjava2-jdbc` 在连接池上有兼容性问题，暂不建议升级。

2. **在 XML 数据源中开启**
```xml
<environments default="mysql">
    <environment id="mysql">
        <dataSource type="druid">
            <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
            <property name="url" value="jdbc:mysql://..."/>
            <property name="username" value="root"/>
            <property name="password" value="***"/>
            <property name="rxjava2" value="true"/> <!-- 开启 RxJava2-JDBC -->
        </dataSource>
    </environment>
</environments>
```

3. **或在注解式配置中开启**
```java
@DBConfig(
    id = "mysql",
    driver = "com.mysql.cj.jdbc.Driver",
    driverType = "jdbc",
    dbType = "mysql",
    server = "127.0.0.1",
    port = 3306,
    db = "app",
    uid = "root",
    pwd = "123456",
    rxjava2 = true           // 打开 RxJava2-JDBC
)
public class AppConfig {}
```

4. **编程式开启（非常用）**
```java
DataSource ds = DataSourceFactory.getInstance().getDataSource("mysql");
ds.setRxjava2(true);
```

完成以上任意方式后，`asyncQuery` / `asyncFirst` / `asyncCount` / `asyncInsert` 等 API 会自动切换到响应式实现；若未开启，将退回普通阻塞实现。
#### **18.如何使用异步/响应式 API**
<div style="font-size:12px;">How to use async / reactive APIs</div>

```java
User user = new User();
user.where("status=#{status}").setStatus("ACTIVE");

// Flowable 列表
Flowable<User> flowable = user.asyncQuery();
flowable.subscribe(System.out::println);

// Maybe 首条
Maybe<User> maybe = user.orderby("id DESC").asyncFirst();

// Single 计数
Single<Long> total = user.where("status=#{status}").asyncCount();

// 异步写操作
Flowable<Integer> insertResult = user.asyncInsert();
insertResult.subscribe(rows -> log.info("Inserted {}", rows));
```

#### **19.如何批量写操作**
<div style="font-size:12px;">How to batch insert / update / delete</div>

```java
List<User> newUsers = buildUsers();

// 批量插入
new User().batchInsert(newUsers);

// 批量更新（exp 用法与 update(String... exp) 一致）
new User().batchUpdate(newUsers, "name=#{name}", "mobile=#{mobile}");

// 批量删除
new User().batchDelete(newUsers);

// 可传入回调，方便分批处理
new User().batchInsert(newUsers, chunk -> log.info("saved {}", chunk.size()));
```

#### **20.如何手动管理事务**
<div style="font-size:12px;">How to handle transactions manually</div>

```java
@RestController
public class PayController {

    @PostMapping("/pay")
    public String pay(@RequestBody PayRequest request) throws Exception {
        DataSource dataSource = DataSourceFactory.getInstance().getDataSource("mysql");
        DBTransaction tran = null;
        try {
            tran = dataSource.beginTransaction();      // 将 Connection 绑定到当前线程

            User user = new User();
            user.setTransaction(tran);                 // 可选：显式注入，便于在异步/新线程中复用
            user.setId(request.getUserId());
            user.where("id=#{id}")
                .update("balance=balance-#{amount}", request.getAmount());

            new Order().setTransaction(tran);
            new Order()
                .where("order_no=#{orderNo}", request.getOrderNo())
                .update("status='PAID'");

            // 任意 executeSql / Xml SQL 也会走当前线程事务
            QueryableAction.executeSql(
                "mysql",
                "INSERT INTO pay_log(order_no, amount) VALUES(?,?)",
                Map.class,
                request.getOrderNo(),
                request.getAmount()
            );

            dataSource.commit(tran.getConnection());
            return "ok";
        } catch (Exception ex) {
            dataSource.rollback();                     // 自动取线程内 Connection
            throw ex;
        }
    }
}
```

`DataSource.beginTransaction()` 会关闭 auto-commit 并把 `Connection` 放入 ThreadLocal，之后同一线程内的实体/SQL 都复用该连接；`commit/rollback` 会自动清理 ThreadLocal，并可多次调用 `setTransaction(tran)` 来跨实体共享事务上下文。

> `beginTransaction()` disables auto-commit and pins the connection to the current thread so every entity or raw SQL call participates; `commit/rollback` clear the ThreadLocal, and `setTransaction(tran)` lets you explicitly reuse it when jumping across components.
#### **21.如何读取元数据/工具信息**
<div style="font-size:12px;">How to read metadata / utility info</div>

```java
// 查询数据源所有表
List<TableInfo> tables = Queryable.getTables("mysql");

// 查询表字段
List<ColumnInfo> columns = Queryable.getColumns("mysql", "user");

// 获取主键字段名
String pk = Queryable.getPrimaryKey("mysql", "user");

// 获取数据库当前时间（由 parser 生成对应 SQL）
Date dbNow = new User().selectNow();
```
