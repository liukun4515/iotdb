package cn.edu.tsinghua.tsfile.timeseries.readV2.reader.impl;

import cn.edu.tsinghua.tsfile.file.metadata.enums.CompressionTypeName;
import cn.edu.tsinghua.tsfile.file.metadata.enums.TSDataType;
import cn.edu.tsinghua.tsfile.format.PageHeader;
import cn.edu.tsinghua.tsfile.timeseries.readV2.datatype.TimeValuePair;
import cn.edu.tsinghua.tsfile.timeseries.readV2.datatype.TsPrimitiveType;
import cn.edu.tsinghua.tsfile.timeseries.readV2.reader.SeriesReaderByTimeStamp;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zhangjinrui on 2017/12/26.
 */
public class SeriesChunkReaderByTimestampImpl extends SeriesChunkReader implements SeriesReaderByTimeStamp{

    private long currentTimestamp;
    private boolean hasCachedTimeValuePair;
    private TimeValuePair cachedTimeValuePair;

    public SeriesChunkReaderByTimestampImpl(InputStream seriesChunkInputStream, TSDataType dataType, CompressionTypeName compressionTypeName) {
        super(seriesChunkInputStream, dataType, compressionTypeName);
    }

    @Override
    public boolean pageSatisfied(PageHeader pageHeader) {
        long maxTimestamp = pageHeader.data_page_header.max_timestamp;
        //If minTimestamp > currentTimestamp, this page should NOT be skipped
        if (maxTimestamp < currentTimestamp) {
            return false;
        }
        return true;
    }

    @Override
    public boolean timeValuePairSatisfied(TimeValuePair timeValuePair) {
        return true;
    }
    
    public void setCurrentTimestamp(long currentTimestamp) {
        this.currentTimestamp = currentTimestamp;
    }

    @Override
    public TsPrimitiveType getValueInTimestamp(long timestamp) throws IOException {
        setCurrentTimestamp(timestamp);
        if(hasCachedTimeValuePair && cachedTimeValuePair.getTimestamp() == timestamp){
            hasCachedTimeValuePair = false;
            return cachedTimeValuePair.getValue();
        }
        while (hasNext()){
            cachedTimeValuePair = next();
            if(cachedTimeValuePair.getTimestamp() == timestamp){
                return cachedTimeValuePair.getValue();
            }
            else if(cachedTimeValuePair.getTimestamp() > timestamp){
                hasCachedTimeValuePair = true;
                return null;
            }
        }
        return null;
    }
}
