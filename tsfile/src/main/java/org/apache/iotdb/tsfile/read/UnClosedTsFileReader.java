/**
 * Copyright © 2019 Apache IoTDB(incubating) (dev@iotdb.apache.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.iotdb.tsfile.read;

import org.apache.iotdb.tsfile.file.metadata.TsDeviceMetadata;
import org.apache.iotdb.tsfile.file.metadata.TsDeviceMetadataIndex;
import org.apache.iotdb.tsfile.file.metadata.TsFileMetaData;
import org.apache.iotdb.tsfile.file.metadata.TsDeviceMetadata;
import org.apache.iotdb.tsfile.file.metadata.TsDeviceMetadataIndex;
import org.apache.iotdb.tsfile.file.metadata.TsFileMetaData;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;

/** A class for reading unclosed tsfile. */
public class UnClosedTsFileReader extends TsFileSequenceReader {

    public UnClosedTsFileReader(String file) throws IOException {
        super(file, false);
    }

    /** unclosed file has no tail magic data */
    @Override
    public String readTailMagic() throws IOException {
        throw new NotImplementedException();
    }

    /** unclosed file has no file metadata */
    @Override
    public TsFileMetaData readFileMetadata() throws IOException {
        throw new NotImplementedException();
    }

    /** unclosed file has no metadata */
    @Override
    public TsDeviceMetadata readTsDeviceMetaData(TsDeviceMetadataIndex index) throws IOException {
        throw new NotImplementedException();
    }
}
