package test.java.entity;

import lombok.extern.slf4j.Slf4j;
import main.java.entites.Insert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.SQLException;

import static org.junit.Assert.*;

@Slf4j
public class InsertTest {

    @Test
    public void insertTesttt() throws SQLException {
        log.info("begin test insert......");
        for(int i=0; i<1000000; i++) {
            Insert.insertTesttt();
        }
        log.info("finish test insert!!!!!!");
    }
}
