/**
  * Copyright © 2019 Apache IoTDB(incubating) (dev@iotdb.apache.org)
  *
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package org.apache.iotdb.tsfile

import java.io.{ObjectInputStream, ObjectOutputStream, _}
import java.net.URI
import java.util

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileStatus, Path}
import org.apache.hadoop.mapreduce.Job
import org.apache.iotdb.tsfile.DefaultSource.SerializableConfiguration
import org.apache.iotdb.tsfile.common.constant.QueryConstant
import org.apache.iotdb.tsfile.io.HDFSInput
import org.apache.iotdb.tsfile.qp.SQLConstant
import org.apache.iotdb.tsfile.read.common.Field
import org.apache.iotdb.tsfile.read.{ReadOnlyTsFile, TsFileSequenceReader}
import org.apache.spark.TaskContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.apache.spark.sql.execution.datasources.{FileFormat, OutputWriterFactory, PartitionedFile}
import org.apache.spark.sql.sources.{DataSourceRegister, Filter}
import org.apache.spark.sql.types._
import org.slf4j.LoggerFactory

private[tsfile] class DefaultSource extends FileFormat with DataSourceRegister {

  override def equals(other: Any): Boolean = other match {
    case _: DefaultSource => true
    case _ => false
  }

  override def inferSchema(
                            spark: SparkSession,
                            options: Map[String, String],
                            files: Seq[FileStatus]): Option[StructType] = {
    val conf = spark.sparkContext.hadoopConfiguration

    //check if the path is given
    options.getOrElse(DefaultSource.path, throw new TSFileDataSourceException(s"${DefaultSource.path} must be specified for org.apache.iotdb.tsfile DataSource"))

    //get union series in TsFile
    val tsfileSchema = Converter.getUnionSeries(files, conf)

    Some(Converter.toSqlSchema(tsfileSchema))
  }

  override def isSplitable(
                            sparkSession: SparkSession,
                            options: Map[String, String],
                            path: org.apache.hadoop.fs.Path): Boolean = {
    true
  }

  override def buildReader(
                            sparkSession: SparkSession,
                            dataSchema: StructType,
                            partitionSchema: StructType,
                            requiredSchema: StructType,
                            filters: Seq[Filter],
                            options: Map[String, String],
                            hadoopConf: Configuration): (PartitionedFile) => Iterator[InternalRow] = {
    val broadcastedConf =
      sparkSession.sparkContext.broadcast(new SerializableConfiguration(hadoopConf))

    (file: PartitionedFile) => {
      // TODO 倒是files之间可以用一个priority queue
      val log = LoggerFactory.getLogger(classOf[DefaultSource])
      log.info(file.toString())

      val conf = broadcastedConf.value.value
      val in = new HDFSInput(new Path(new URI(file.filePath)), conf)

      val reader: TsFileSequenceReader = new TsFileSequenceReader(in)
      val readTsFile: ReadOnlyTsFile = new ReadOnlyTsFile(reader)
      //      val reader: TsFileSequenceReader = new TsFileSequenceReader("D:\\github\\debt\\iotdb\\test.tsfile") //TODO 这里没用hdfsinputstream因为没接口
      //      val readTsFile: ReadOnlyTsFile = new ReadOnlyTsFile(reader)

      Option(TaskContext.get()).foreach { taskContext => {
        taskContext.addTaskCompletionListener { _ => in.close() }
        log.info("task Id: " + taskContext.taskAttemptId() + " partition Id: " + taskContext.partitionId())
      }
      }

      val parameters = new util.HashMap[java.lang.String, java.lang.Long]()
      parameters.put(QueryConstant.PARTITION_START_OFFSET, file.start.asInstanceOf[java.lang.Long])
      parameters.put(QueryConstant.PARTITION_END_OFFSET, (file.start + file.length).asInstanceOf[java.lang.Long])

      // get extendedSchema from requiredSchema
      var extendedSchema : StructType = requiredSchema
      if (requiredSchema.isEmpty ||
        (requiredSchema.size == 1 && requiredSchema.iterator.next().name == SQLConstant.RESERVED_TIME)) {
        // eg1: select count(*) from table
        // eg2: select time from table; select count(*) from table where time > 10
        val fileSchema = Converter.getUnionSeries(reader, conf)
        extendedSchema = Converter.toSqlSchema(fileSchema)
      }

      //convert filters to queryExpression
      val queryExpression = Converter.toQueryExpression(extendedSchema, filters)

      val queryDataSet = readTsFile.query(queryExpression, parameters) //TODO PARTITION

      new Iterator[InternalRow] {
        private val rowBuffer = Array.fill[Any](requiredSchema.length)(null)

        private val safeDataRow = new GenericRow(rowBuffer)

        // Used to convert `Row`s containing data columns into `InternalRow`s.
        private val encoderForDataColumns = RowEncoder(requiredSchema)

        override def hasNext: Boolean = {
          val hasNext = queryDataSet.hasNext
          hasNext
        }

        override def next(): InternalRow = {

          val curRecord = queryDataSet.next()
          val fields = new scala.collection.mutable.HashMap[String, Field]()
          for (i <- 0 until curRecord.getFields.size()) {
            val field = curRecord.getFields.get(i)
            fields.put(queryDataSet.getPaths.get(i).getFullPath, field)
          }

          //index in one required row
          var index = 0
          requiredSchema.foreach((field: StructField) => {
            if (field.name == SQLConstant.RESERVED_TIME) {
              rowBuffer(index) = curRecord.getTimestamp
            } else {
              rowBuffer(index) = Converter.toSqlValue(fields.getOrElse(field.name, null))
            }
            index += 1
          })

          encoderForDataColumns.toRow(safeDataRow)
        }
      }
    }
  }

  override def shortName(): String = "tsfile"

  override def prepareWrite(sparkSession: SparkSession,
                            job: Job,
                            options: Map[String, String],
                            dataSchema: StructType): OutputWriterFactory = {

    new TsFileWriterFactory(options)
  }

  class TSFileDataSourceException(message: String, cause: Throwable)
    extends Exception(message, cause) {
    def this(message: String) = this(message, null)
  }

}


private[tsfile] object DefaultSource {
  val path = "path"

  class SerializableConfiguration(@transient var value: Configuration) extends Serializable {
    private def writeObject(out: ObjectOutputStream): Unit = {
      out.defaultWriteObject()
      value.write(out)
    }

    private def readObject(in: ObjectInputStream): Unit = {
      value = new Configuration(false)
      value.readFields(in)
    }
  }

}
