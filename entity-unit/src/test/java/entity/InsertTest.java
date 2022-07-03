package test.java.entity;

import entity.tool.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import main.java.entites.Insert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class InsertTest {

    private Integer count;

    @Test
    public void insertTesttt() throws SQLException {
        System.out.println("begin test insert......");
        Insert.insertTesttt();
        System.out.println("finish test insert!!!!!!");
    }
}
