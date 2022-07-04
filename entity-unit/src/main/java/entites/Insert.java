package entites;

import entity.tool.util.ThreadUtils;

import java.sql.SQLException;

public class Insert {
    private static int count;
    private static int total = 1000000;

    public static void insertTesttt() throws SQLException {
        TestttEntity entity = new TestttEntity() {{
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
        for(int i = 0; i< total; i++) {
            int finalI = i;
            ThreadUtils.onec(new Runnable() {
                @Override
                public void run() {
                    try {
                        Insert.insertTesttt();
                        count++;
                        System.out.print(String.format("count=%s, index=%s\r", count, finalI));
                        if(count == total) {
                            System.out.println(String.format("finish test insert %s times!!!!!!", count));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
