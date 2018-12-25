package cn.edu.tsinghua.iotdb.query.reader;

import cn.edu.tsinghua.iotdb.exception.FileNodeManagerException;
import cn.edu.tsinghua.iotdb.jdbc.TsfileJDBCConfig;
import cn.edu.tsinghua.iotdb.query.executor.EngineQueryRouter;
import cn.edu.tsinghua.iotdb.service.IoTDB;
import cn.edu.tsinghua.iotdb.integration.Constant;
import cn.edu.tsinghua.iotdb.utils.EnvironmentUtils;
import cn.edu.tsinghua.tsfile.common.conf.TSFileConfig;
import cn.edu.tsinghua.tsfile.common.conf.TSFileDescriptor;
import cn.edu.tsinghua.tsfile.read.common.Path;
import cn.edu.tsinghua.tsfile.read.common.RowRecord;
import cn.edu.tsinghua.tsfile.read.expression.QueryExpression;
import cn.edu.tsinghua.tsfile.read.expression.impl.SingleSeriesExpression;
import cn.edu.tsinghua.tsfile.read.filter.TimeFilter;
import cn.edu.tsinghua.tsfile.read.filter.ValueFilter;
import cn.edu.tsinghua.tsfile.read.query.dataset.QueryDataSet;
import org.junit.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static cn.edu.tsinghua.iotdb.integration.Constant.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Real data insert and query test.
 */
public class IoTDBSeriesReaderTest {

  private static IoTDB deamon;

  private static boolean testFlag = Constant.testFlag;
  private static TSFileConfig tsFileConfig = TSFileDescriptor.getInstance().getConfig();
  private static int maxNumberOfPointsInPage;
  private static int pageSizeInByte;
  private static int groupSizeInByte;

  private static Connection connection;

  @BeforeClass
  public static void setUp() throws Exception {
    if (testFlag) {
      EnvironmentUtils.closeStatMonitor();
      EnvironmentUtils.closeMemControl();

      // use small page setting
      // origin value
      maxNumberOfPointsInPage = tsFileConfig.maxNumberOfPointsInPage;
      pageSizeInByte = tsFileConfig.pageSizeInByte;
      groupSizeInByte = tsFileConfig.groupSizeInByte;
      // new value
      tsFileConfig.maxNumberOfPointsInPage = 1000;
      tsFileConfig.pageSizeInByte = 1024 * 1024 * 150;
      tsFileConfig.groupSizeInByte = 1024 * 1024 * 1000;

      deamon = IoTDB.getInstance();
      deamon.active();
      EnvironmentUtils.envSetUp();

      if (testFlag) {
        Thread.sleep(1000);
        insertData();
        connection = DriverManager.getConnection("jdbc:tsfile://127.0.0.1:6667/", "root", "root");
      }
    }
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (testFlag) {
      connection.close();
      deamon.stop();
      Thread.sleep(1000);

      //recovery value
      tsFileConfig.maxNumberOfPointsInPage = maxNumberOfPointsInPage;
      tsFileConfig.pageSizeInByte = pageSizeInByte;
      tsFileConfig.groupSizeInByte = groupSizeInByte;
      EnvironmentUtils.cleanEnv();
    }
  }

  @Test
  public void selectAllTest() throws IOException, FileNodeManagerException {
    String selectSql = "select * from root";
    System.out.println("Test >>> " + selectSql);

    EngineQueryRouter engineExecutor = new EngineQueryRouter();
    QueryExpression queryExpression = QueryExpression.create();
    queryExpression.addSelectedPath(new Path(d0s0));
    queryExpression.addSelectedPath(new Path(d0s1));
    queryExpression.addSelectedPath(new Path(d0s2));
    queryExpression.addSelectedPath(new Path(d0s3));
    queryExpression.addSelectedPath(new Path(d0s4));
    queryExpression.addSelectedPath(new Path(d0s5));
    queryExpression.addSelectedPath(new Path(d1s0));
    queryExpression.addSelectedPath(new Path(d1s1));
    queryExpression.setExpression(null);

    QueryDataSet queryDataSet = engineExecutor.query(queryExpression);

    int cnt = 0;
    while (queryDataSet.hasNext()) {
      RowRecord rowRecord = queryDataSet.next();
      cnt++;
    }
    assertEquals(23400, cnt);
  }

  @Test
  public void selectOneSeriesWithValueFilterTest() throws IOException, FileNodeManagerException {

    String selectSql = "select s0 from root.vehicle.d0 where s0 >= 20";
    System.out.println("Test >>> " + selectSql);

    EngineQueryRouter engineExecutor = new EngineQueryRouter();
    QueryExpression queryExpression = QueryExpression.create();
    Path p = new Path(d0s0);
    queryExpression.addSelectedPath(p);
    SingleSeriesExpression singleSeriesExpression = new SingleSeriesExpression(p, ValueFilter.gtEq(20));
    queryExpression.setExpression(singleSeriesExpression);

    QueryDataSet queryDataSet = engineExecutor.query(queryExpression);

    int cnt = 0;
    while (queryDataSet.hasNext()) {
      RowRecord rowRecord = queryDataSet.next();
      String result = rowRecord.toString();
      //System.out.println(result);
      cnt++;
    }
    assertEquals(16440, cnt);

  }

  @Test
  public void seriesTimeDigestReadTest() throws IOException, FileNodeManagerException {
    String selectSql = "select s0 from root.vehicle.d0 where time >= 22987";
    System.out.println("Test >>> " + selectSql);

    EngineQueryRouter engineExecutor = new EngineQueryRouter();
    QueryExpression queryExpression = QueryExpression.create();
    Path path = new Path(d0s0);
    queryExpression.addSelectedPath(path);
    SingleSeriesExpression expression = new SingleSeriesExpression(path, TimeFilter.gt(22987L));
    queryExpression.setExpression(expression);

    QueryDataSet queryDataSet = engineExecutor.query(queryExpression);

    int cnt = 0;
    while (queryDataSet.hasNext()) {
      RowRecord rowRecord = queryDataSet.next();
      String result = rowRecord.toString();
      //System.out.println(result);
      cnt++;
    }
    assertEquals(3012, cnt);

  }

  @Test
  public void crossSeriesReadUpdateTest() throws IOException, FileNodeManagerException {
    System.out.println("Test >>> select s1 from root.vehicle.d0 where s0 < 111");
    EngineQueryRouter engineExecutor = new EngineQueryRouter();
    QueryExpression queryExpression = QueryExpression.create();
    Path path1 = new Path(d0s0);
    Path path2 = new Path(d0s1);
    queryExpression.addSelectedPath(path1);
    queryExpression.addSelectedPath(path2);
    SingleSeriesExpression singleSeriesExpression = new SingleSeriesExpression(path1, ValueFilter.lt(111));
    queryExpression.setExpression(singleSeriesExpression);

    QueryDataSet queryDataSet = engineExecutor.query(queryExpression);

    int cnt = 0;
    while (queryDataSet.hasNext()) {
      RowRecord rowRecord = queryDataSet.next();
      //System.out.println("~~~~" + rowRecord);
      //long time = rowRecord.getTimestamp();
      //String value = rowRecord.getFields().get(1).getStringValue();
      //System.out.println("===" + rowRecord.toString());
      cnt++;
    }
    assertEquals(22800, cnt);
  }

  private static void insertData() throws ClassNotFoundException, SQLException {
    Class.forName(TsfileJDBCConfig.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.getConnection("jdbc:tsfile://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();

      for (String sql : create_sql) {
        statement.execute(sql);
      }

      // insert large amount of data    time range : 3000 ~ 13600
      for (int time = 3000; time < 13600; time++) {
        //System.out.println("===" + time);
        String sql = String.format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, time % 100);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, time % 17);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, time % 22);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s3) values(%s,'%s')", time, stringValue[time % 5]);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s4) values(%s, %s)", time, booleanValue[time % 2]);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s5) values(%s, %s)", time, time);
        statement.execute(sql);
      }

      // statement.executeWithGlobalTimeFilter("flush");

      // insert large amount of data time range : 13700 ~ 24000
      for (int time = 13700; time < 24000; time++) {

        String sql = String.format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, time % 70);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, time % 40);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, time % 123);
        statement.execute(sql);
      }

      statement.execute("merge");

      Thread.sleep(5000);

      // buffwrite data, unsealed file
      for (int time = 100000; time < 101000; time++) {

        String sql = String.format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, time % 20);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, time % 30);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, time % 77);
        statement.execute(sql);
      }

      statement.execute("flush");

      // bufferwrite data, memory data
      for (int time = 200000; time < 201000; time++) {

        String sql = String.format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, -time % 20);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, -time % 30);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, -time % 77);
        statement.execute(sql);
      }

      // overflow insert, time < 3000
      for (int time = 2000; time < 2500; time++) {

        String sql = String.format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, time);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, time + 1);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, time + 2);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s3) values(%s,'%s')", time, stringValue[time % 5]);
        statement.execute(sql);
      }

      // overflow insert, time > 200000
      for (int time = 200900; time < 201000; time++) {

        String sql = String.format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, 6666);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, 7777);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, 8888);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s3) values(%s,'%s')", time, "goodman");
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s4) values(%s, %s)", time, booleanValue[time % 2]);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s5) values(%s, %s)", time, 9999);
        statement.execute(sql);
      }

      // overflow update
      //statement.execute("UPDATE root.vehicle SET d0.s1 = 11111111 WHERE time > 23000 and time < 100100");

      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }
}