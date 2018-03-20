package cn.edu.tsinghua.iotdb.MonitorV2;

import cn.edu.tsinghua.tsfile.timeseries.write.record.DataPoint;
import cn.edu.tsinghua.tsfile.timeseries.write.record.TSRecord;
import cn.edu.tsinghua.tsfile.timeseries.write.record.datapoint.LongDataPoint;

import java.util.ArrayList;
import java.util.List;

public class StatisticTSRecord extends TSRecord {

    public StatisticTSRecord(long timestamp, String path) {
        super(timestamp, path);
        for(StatisticConstants type : StatisticConstants.values()){
            addTuple(new LongDataPoint(type.name(), 0));
        }
    }

    public StatisticTSRecord(StatisticTSRecord record, String path){
        super(record.time, path);
        for(DataPoint dataPoint : record.dataPointList){
            addTuple(new LongDataPoint(dataPoint.getMeasurementId(), (long)dataPoint.getValue()));
        }
    }

    public void addOneStatistic(StatisticConstants type, StatisticTSRecord record){
        for(int i = 0;i < dataPointList.size();i++){
            if(dataPointList.get(i).getMeasurementId().equals(type.name())){
                long old_value = (long)dataPointList.get(i).getValue();
                dataPointList.get(i).setLong(old_value + (long)record.getConstantValue(type));
            }
        }
    }

    public void addOneStatistic(StatisticConstants type, long value){
        for(int i = 0;i < dataPointList.size();i++){
            if(dataPointList.get(i).getMeasurementId().equals(type.name())){
                long old_value = (long)dataPointList.get(i).getValue();
                dataPointList.get(i).setLong(old_value + value);
            }
        }
    }

    public static List<String> getAllPaths(String prefix){
        List<String> paths = new ArrayList<>();
        for(StatisticConstants statistic : StatisticConstants.values()){
            paths.add(prefix + MonitorConstants.MONITOR_PATH_SEPERATOR + statistic.name());
        }
        return paths;
    }

    public enum StatisticConstants {
        TOTAL_REQ_SUCCESS, TOTAL_REQ_FAIL,
        TOTAL_POINTS_SUCCESS, TOTAL_POINTS_FAIL,
    }

    public int getConstantIndex(StatisticConstants constants){
        for(int i = 0;i < dataPointList.size();i++){
            if(dataPointList.get(i).getMeasurementId().equals(constants.name()))return i;
        }
        return -1;
    }

    public Object getConstantValue(StatisticConstants constants){
        int index = getConstantIndex(constants);
        if(index == -1)return null;
        else return dataPointList.get(index).getValue();
    }
}