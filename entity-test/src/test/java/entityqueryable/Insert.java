package entityqueryable;

import entity.query.ColumnInfo;
import entity.query.Datetime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Insert {
    private static int count;
    private static int total = 1000000;

    public static void createTest() throws Exception {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        for(int i=0; i<20; i++) {
            final int finalI = i;
            columns.add(new ColumnInfo(){{
                setType(String.class);
                setColumnName(String.format("field%s", finalI +1));
            }});
        }
        TestEntity.createTable("test", "test", columns);
    }

    public static void insertTest() throws SQLException {
        TestEntity entity = new TestEntity() {{
            setField1(")12345678(");
            setField2(")12345678(");
            setField3(")12345678(");
            setField4(")12345678(");
            setField5(")12345678(");
            setField6(")12345678(");
            setField7(")12345678(");
            setField8(")12345678(");
            setField9(")12345678(");
            setField10(")12345678(");
            setField11(")12345678(");
            setField12(")12345678(");
            setField13(")12345678(");
            setField14(")12345678(");
            setField15(")12345678(");
            setField16(")12345678(");
            setField17(")12345678(");
            setField18(")12345678(");
            setField19(")12345678(");
            setField20(")12345678(");
        }};
        entity.insert();
    }

    public static void main(String[] args) {
        System.out.println("begin test insert......");
        System.out.println(String.format("time=%s\r", Datetime.format(new Date(), "HH:mm:ss.SSS")));
        for(int i = 0; i< total; i++) {
            int finalI = i;
            try {
                Insert.insertTest();
                count++;
                System.out.print(String.format("count=%s, index=%s, time=%s\r", count, finalI, Datetime.format(new Date(), "HH:mm:ss.SSS")));
                if(count == total) {
                    System.out.println(String.format("finish test insert %s times %s!!!!!!", count, Datetime.format(new Date(), "HH:mm:ss.SSS")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
