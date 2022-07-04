package entity;

import entites.Insert;
import org.junit.Test;

import java.sql.SQLException;

public class InsertTest {

    private Integer count;

    @Test
    public void insertTest() throws SQLException {
        System.out.println("begin test insert......");
        Insert.insertTest();
        System.out.println("finish test insert!!!!!!");
    }


    @Test
    public void createTest() throws Exception {
        System.out.println("begin test insert......");
        Insert.createTest();
        System.out.println("finish test insert!!!!!!");
    }
}
