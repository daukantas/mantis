/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package org.seaborne.dboe.base.file ;

import org.seaborne.dboe.base.objectfile.ObjectFile ;
import org.seaborne.dboe.base.objectfile.ObjectFileStorage ;
import org.seaborne.dboe.base.objectfile.StringFile ;

public class FileFactory {
    
    public static BinaryDataFile createBinaryDataFile(FileSet fileset, String ext) {
        String x = fileset.filename(ext) ;
        if ( fileset.isMem() ) {
            return new BinaryDataFileMem() ;
        } else {
            BinaryDataFile bdf = new BinaryDataFileRandomAccess(x) ;
            bdf = new BinaryDataFileWriteBuffered(bdf) ;
            return bdf ;
        }
    }

    public static BinaryDataFile createBinaryDataFile() {
        return new BinaryDataFileMem() ;
    }

    // TODO More use of fileset.
    // Prune use elsewhere e.g BlockMgrFactory.
    
    public static StringFile createStringFileDisk(String filename) {
        return new StringFile(createObjectFileDisk(filename)) ;
    }

    public static StringFile createStringFileMem(String filename) {
        return new StringFile(createObjectFileMem(filename)) ;
    }

    public static ObjectFile createObjectFileDisk(String filename) {
        BufferChannel file = BufferChannelFile.create(filename) ;
        return new ObjectFileStorage(file) ;
    }

    public static ObjectFile createObjectFileMem(String filename) {
        BufferChannel file = BufferChannelMem.create(filename) ;
        return new ObjectFileStorage(file) ;
    }

    public static BufferChannel createBufferChannel(FileSet fileset, String ext) {
        String x = fileset.filename(ext) ;
        if ( fileset.isMem() )
            return BufferChannelMem.create(x) ;
        else
            return BufferChannelFile.create(x) ;
    }

    public static BufferChannel createBufferChannelMem() {
        return createBufferChannel(FileSet.mem(), null) ;
    }
}
