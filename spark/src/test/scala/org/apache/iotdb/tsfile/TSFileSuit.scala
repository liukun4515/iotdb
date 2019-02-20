package org.apache.iotdb.tsfile

import java.io.File

import org.apache.iotdb.tsfile.qp.SQLConstant
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.junit.Assert
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.collection.mutable


class TSFileSuit extends FunSuite with BeforeAndAfterAll {

  private val resourcesFolder = "src/test/resources"
  private val tsfileFolder = "src/test/resources/tsfile"
  private val tsfile1 = "src/test/resources/tsfile/test1.tsfile"
  private val tsfile2 = "src/test/resources/tsfile/test2.tsfile"
  private val outputPath = "src/test/resources/output"
  private val outputPathFile = outputPath + "/part-m-00000"
  private val outputPath2 = "src/test/resources/output2"
  private val outputPathFile2 = outputPath2 + "/part-m-00000"
  private var spark: SparkSession = _

  override protected def beforeAll(): Unit = {
    System.setProperty("hadoop.home.dir", "D:\\winutils")
    super.beforeAll()
    //    val resources = new File(resourcesFolder)
    //    if (!resources.exists())
    //      resources.mkdirs()
    //    val tsfile_folder = new File(tsfileFolder)
    //    if (!tsfile_folder.exists())
    //      tsfile_folder.mkdirs()
    //    val output = new File(outputPath)
    //    if (output.exists())
    //      deleteDir(output)
    //    val output2 = new File(outputPath2)
    //    if (output2.exists())
    //      deleteDir(output2)
    //    new CreateTSFile().createTSFile1(tsfile1)
    //    new CreateTSFile().createTSFile2(tsfile2)
    spark = SparkSession
      .builder()
      .config("spark.master", "local")
      .appName("TSFile test")
      .getOrCreate()
  }

  override protected def afterAll(): Unit = {
    val out = new File(outputPath)
    deleteDir(out)
    val out2 = new File(outputPath2)
    deleteDir(out2)
    try {
      spark.sparkContext.stop()
    } finally {
      super.afterAll()
    }
  }

  def deleteDir(dir: File): Unit = {
    if (dir.isDirectory) {
      dir.list().foreach(f => {
        deleteDir(new File(dir, f))
      })
    }
    dir.delete()

  }

  test("mytest") {
    val df = spark.read.tsfile("D:/github/debt/iotdb/spark/src/test/resources/test2.tsfile") // inferSchema
//    df.show()
    df.createOrReplaceTempView("tsfile_table")

//    val newdf = spark.sql("select * from tsfile_table where `device_1.sensor_1` >0")
    val newdf = spark.sql("select * from tsfile_table where `device_1.sensor_1` < 100")
//    newdf.show()
    Assert.assertEquals(99, newdf.count())

//    spark.sql("select * from tsfile_table where time > 3").show()

//    spark.sql("select * from tsfile_table where `device_1.sensor_2`>2").show()

//    spark.sql("select * from tsfile_table where `device_1.sensor_2`>2 or Time<9").show()


//    spark.sql("select * from tsfile_table where 'device_1.sensor_3' > 2").show()

//    val path = "D:\\github\\debt\\iotdb\\spark\\src\\test\\resources\\output"
//    df.write.tsfile (path)
//    val newDf = spark.read.tsfile(path)
//    newDf.show()

  }


  test("writer") {
    val df = spark.read.tsfile(tsfile1)
    df.show()
    df.write.tsfile(outputPath)
    val newDf = spark.read.tsfile(outputPathFile)
    newDf.show()
    Assert.assertEquals(newDf.collectAsList(), df.collectAsList())
  }

  test("test write options") {
    val df = spark.read.option("delta_object_name", "root.carId.deviceId").tsfile(tsfile1)
    df.write.option("delta_object_name", "root.carId.deviceId") tsfile (outputPath2)
    val newDf = spark.read.option("delta_object_name", "root.carId.deviceId").tsfile(outputPathFile2)
    newDf.show()
    Assert.assertEquals(newDf.collectAsList(), df.collectAsList())
  }

  test("test read options") {
    val options = new mutable.HashMap[String, String]()
    options.put(SQLConstant.DELTA_OBJECT_NAME, "root.carId.deviceId")
    val df = spark.read.options(options).tsfile(tsfile1) // inferSchema
    //    df.show()
    df.createOrReplaceTempView("tsfile_table")
    //    spark.sql("select time from tsfile_table where deviceId = 'd1' and carId = 'car' and time < 10").show()
    spark.sql("select device_1+sensor_1 from tsfile_table").show()

    //    val newDf = spark.sql("select * from tsfile_table where deviceId = 'd1'")
    //    Assert.assertEquals(4, newDf.count())
  }

  //  test("mytest") {
  //    val sc = spark.sparkContext
  //    val configuration = sc.hadoopConfiguration
  //    val fs = FileSystem.get(configuration)
  //    val fsDataInputStream = fs.open(new Path("D:/github/debt/iotdb/test.tsfile"))
  //  }

  test("tsfile_qp") {
    val df = spark.read.tsfile(tsfileFolder)
    df.show()
    //    df.createOrReplaceTempView("tsfile_table")
    ////    val newDf = spark.sql("select s1,s2 from tsfile_table where delta_object = 'root.car.d1' and time <= 10 and (time > 5 or s1 > 10)")
    ////    val rs=spark.sql("select s1,s2 from tsfile_table where time>10").show()
    //    val rs=spark.sql("select * from tsfile_table").show()
    //    rs.show()
    //    Assert.assertEquals(16,rs)
    //    newDf.show()
    //    Assert.assertEquals(0, newDf.count())
    //    Assert.assertEquals(16, newDf.count())
  }

  test("testMultiFilesNoneExistDelta_object") {
    val df = spark.read.tsfile(tsfileFolder)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table where delta_object = 'd4'")
    Assert.assertEquals(0, newDf.count())
  }

  test("testMultiFilesWithFilterOr") {
    val df = spark.read.tsfile(tsfileFolder)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table where s1 < 2 or s2 > 60")
    Assert.assertEquals(4, newDf.count())
  }

  test("testMultiFilesWithFilterAnd") {
    val df = spark.read.tsfile(tsfileFolder)
    df.show()
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table where s2 > 20 and s1 < 5")
    Assert.assertEquals(2, newDf.count())
    newDf.show()
  }

  test("testMultiFilesSelect*") {
    val df = spark.read.tsfile(tsfileFolder)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table")
    Assert.assertEquals(16, newDf.count())
  }

  test("testCount") {
    val df = spark.read.tsfile(tsfile1)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select count(*) from tsfile_table")
    Assert.assertEquals(8, newDf.head().apply(0).asInstanceOf[Long])
  }

  test("testSelect *") {
    val df = spark.read.tsfile(tsfile1)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table")
    val count = newDf.count()
    Assert.assertEquals(8, count)
  }

  test("testQueryData1") {
    val df = spark.read.tsfile(tsfile1)
    df.createOrReplaceTempView("tsfile_table")

    val newDf = spark.sql("select s1, s3 from tsfile_table where s1 > 4 and delta_object = 'root.car.d2'").cache()
    val count = newDf.count()
    Assert.assertEquals(4, count)
  }

  test("testQueryDataComplex2") {
    val df = spark.read.tsfile(tsfile1)
    df.createOrReplaceTempView("tsfile_table")

    val newDf = spark.sql("select * from tsfile_table where s1 <4 and delta_object = 'root.car.d1' or s1 > 5 and delta_object = 'root.car.d2'").cache()
    val count = newDf.count()
    Assert.assertEquals(6, count)
  }

  test("testQuerySchema") {
    val df = spark.read.format("cn.edu.tsinghua.tsfile").load(tsfile1)

    val expected = StructType(Seq(
      StructField(SQLConstant.RESERVED_TIME, LongType, nullable = true),
      StructField(SQLConstant.RESERVED_DELTA_OBJECT, StringType, nullable = true),
      StructField("s3", FloatType, nullable = true),
      StructField("s4", DoubleType, nullable = true),
      StructField("s5", StringType, nullable = true),
      StructField("s1", IntegerType, nullable = true),
      StructField("s2", LongType, nullable = true)
    ))
    Assert.assertEquals(expected, df.schema)
  }

}