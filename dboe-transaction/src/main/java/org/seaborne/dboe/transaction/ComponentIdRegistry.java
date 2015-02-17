/**
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

package org.seaborne.dboe.transaction;

import java.util.Arrays ;
import java.util.Map ;
import java.util.UUID ;
import java.util.concurrent.ConcurrentHashMap ;

import org.apache.jena.atlas.lib.Bytes ;
import org.seaborne.dboe.sys.SystemLz ;
import org.seaborne.dboe.transaction.txn.ComponentId ;
import org.seaborne.dboe.transaction.txn.L ;
//package org.apache.jena.fuseki.migrate;


public class ComponentIdRegistry {
    // Not stable across 
    private ComponentId localCId = new ComponentId("Local", L.uuidAsBytes(UUID.randomUUID())) ;
    
    // No! byte[].equals is Object.equals
    
    // ComponentId equality is by bytes (not display label)
    // so this maps bytes to a better form. 
    private Map<Holder, ComponentId> registry = new ConcurrentHashMap<>() ;
    
    public ComponentIdRegistry() { }
    
    public void registerLocal(String label, int index) {
        register(localCId, label, index) ;
    }
    
    public void register(ComponentId base, String label, int index) {
        byte[] bytes = base.bytes() ;
        int x = Bytes.getInt(bytes, bytes.length - SystemLz.SizeOfInt) ;
        x = x ^ index ;
        Bytes.setInt(x, bytes, bytes.length-SystemLz.SizeOfInt) ;
        ComponentId cid = new ComponentId(label+"-"+index, bytes) ;
        Holder h = new Holder(bytes) ;
        registry.put(h, cid) ;
    }
    
    public ComponentId lookup(byte[] bytes) {
        Holder h = new Holder(bytes) ;
        return registry.get(h) ;
    }
    
    public void reset() {
        registry.clear() ;
    }
    
    // Makes equality the value of the bytes. 
    static class Holder {
        private final byte[] bytes ;

        Holder(byte[] bytes) { this.bytes = bytes ; }

        @Override
        public int hashCode() {
            final int prime = 31 ;
            int result = 1 ;
            result = prime * result + Arrays.hashCode(bytes) ;
            return result ;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj )
                return true ;
            if ( obj == null )
                return false ;
            if ( getClass() != obj.getClass() )
                return false ;
            Holder other = (Holder)obj ;
            if ( !Arrays.equals(bytes, other.bytes) )
                return false ;
            return true ;
        }
        
    }
}