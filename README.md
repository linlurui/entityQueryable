[![使用IntelliJ IDEA开发维护](https://img.shields.io/badge/IntelliJ%20IDEA-提供支持-blue.svg)](https://www.jetbrains.com/idea/)

 **EntityQueryable is a Micro ORM library** 

<p align="center">
    <p align="left">
EntityQueryable是Java开如平台下轻量级ORM, 支持多数据库映射操作的开源ORM库, 兼容mybatis配置,支持联表查询、异步批量插入、更新、删除,内置阿里的Druid数据库连接池支持数据库mysql/mariadb/sqlserver/sqlite/oracle/postgresql,以及集成了rxjava2-jdbc(默认不开启),可以支持响应式的异步非阻塞IO操作数据; 
</p>
    <img align="right" src="https://github.com/linlurui/entityQueryable/blob/main/pay5.jpg" alt="捐赠给作者"  width="200">
    <p align="right">
        <em>捐赠给作者</em>
    </p>
</p>


### **How to start EntityQueryable?**


 ### **1.Download database driver first, And build a "db-config.xml". eg:** 
```xml
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
  <typeAliases>
    <typeAlias alias="UserEntity" type="ccait.entity.UserEntity" />
  </typeAliases>
  <mappers>
    <mapper resource="ccait/mybatis/map/user-mapper.xml" />
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

 ### 2.Add the jar package by maven
```xml
     <repositories>
        <repository>
            <id>entityQueryable</id>
            <url>https://raw.github.com/linlurui/entityQueryable/snapshots</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>entity.queryable</groupId>
            <artifactId>entity.queryable</artifactId>
            <version>3.0-SNAPSHOT</version>
        </dependency>
    <dependencies>
```
 ### **3.Inherit base class "entity.query.Queryable" in your entity. eg:** 
```java
package ccait.entity;

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
 
 
 
 
### **How to use EntityQueryable to access database? eg:**
 
###  **1.How to insert data** 
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

###  **2.How to update data?** 
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
###  **3.How to delete data** 
```java
@RequestMapping(value="/eqdelete", method=RequestMethod.GET)
public ModelAndView remove() {

	User user = new User();
	user.setAccount("linlurui");
	user.where("account=#{account}").delete();
	
	return new ModelAndView("hello", "message", "Delete data by entity queryable");
}
```
###  **4.How to query data** 
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
### **5.How to join table**
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


### **6.How to union data**
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

### **7.How to return custom result**
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
### **8.How to query first**
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
### **9.How to query top data**
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
### **10.How to check exist**
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
### **11.How to count data**
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
### **12.How to query by page**
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
### **13.select from entity**
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
