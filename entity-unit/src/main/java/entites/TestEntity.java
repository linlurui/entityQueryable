package main.java.entites;

import entity.query.Queryable;
import entity.query.annotation.*;
import lombok.Data;

@Data
@Tablename(value = "test")
@DataSource(value = "test")
public class TestEntity extends Queryable<TestEntity> {
    private String field1;
    private String field2;
    private String field3;
    private String field4;
    private String field5;
    private String field6;
    private String field7;
    private String field8;
    private String field9;
    private String field10;
    private String field11;
    private String field12;
    private String field13;
    private String field14;
    private String field15;
    private String field16;
    private String field17;
    private String field18;
    private String field19;
    private String field20;
}
