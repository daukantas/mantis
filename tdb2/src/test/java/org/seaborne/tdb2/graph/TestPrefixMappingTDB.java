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

package org.seaborne.tdb2.graph;

import org.apache.jena.atlas.lib.FileOps ;
import org.apache.jena.shared.PrefixMapping ;
import org.apache.jena.sparql.core.DatasetPrefixStorage ;
import org.apache.jena.sparql.graph.AbstractTestPrefixMapping2 ;
import org.junit.* ;
import org.seaborne.dboe.base.file.Location ;
import org.seaborne.tdb2.ConfigTest ;
import org.seaborne.tdb2.junit.BuildTestLib ;
import org.seaborne.tdb2.sys.StoreConnection ;

public class TestPrefixMappingTDB extends AbstractTestPrefixMapping2
{
    static DatasetPrefixStorage last = null ;
    
    @BeforeClass public static void beforeClass() {}
    @AfterClass public static void afterClass()   { StoreConnection.reset() ; ConfigTest.deleteTestingDir() ; }

    @Before public void before() { StoreConnection.reset() ; }
    @After public  void after()  { }

    
    @Override
    protected PrefixMapping create() {
        last = createTestingMem() ;
        return view() ;
    }

    static DatasetPrefixStorage createTestingMem() { 
        return createTesting(Location.mem()) ;
    }
    
    static DatasetPrefixStorage createTesting(Location location) {
        return BuildTestLib.makePrefixes(location) ;
    }

    @Override
    protected PrefixMapping view() {
        return last.getPrefixMapping() ; 
    }

    @Test public void multiple1() {
        DatasetPrefixStorage prefixes = createTestingMem() ;
        PrefixMapping pmap1 = prefixes.getPrefixMapping() ;
        PrefixMapping pmap2 = prefixes.getPrefixMapping("http://graph/") ;
        pmap1.setNsPrefix("x", "http://foo/") ;
        assertNull(pmap2.getNsPrefixURI("x")) ;
        assertNotNull(pmap1.getNsPrefixURI("x")) ;
    }
    
    @Test public void multiple2() {
        DatasetPrefixStorage prefixes = createTestingMem() ;
        PrefixMapping pmap1 = prefixes.getPrefixMapping("http://graph/") ;  // Same
        PrefixMapping pmap2 = prefixes.getPrefixMapping("http://graph/") ;
        pmap1.setNsPrefix("x", "http://foo/") ;
        assertNotNull(pmap2.getNsPrefixURI("x")) ;
        assertNotNull(pmap1.getNsPrefixURI("x")) ;
    }
    
    // Persistent.
    @Test
    public void persistent1() {
        StoreConnection.reset();
        String dir = ConfigTest.getTestingDir() ;
        FileOps.clearDirectory(dir) ;

        DatasetPrefixStorage prefixes = createTesting(Location.create(dir)) ;
        PrefixMapping pmap1 = prefixes.getPrefixMapping() ;

        String x = pmap1.getNsPrefixURI("x") ;
        assertNull(x) ;
        prefixes.close() ;
    }
    
    // Persistent.
    @Test
    public void persistent2() {
        StoreConnection.reset();
        String dir = ConfigTest.getTestingDir() ;
        FileOps.clearDirectory(dir) ;

        DatasetPrefixStorage prefixes = createTesting(Location.create(dir)) ;
        PrefixMapping pmap1 = prefixes.getPrefixMapping() ;

        pmap1.setNsPrefix("x", "http://foo/") ;
        assertEquals("http://foo/", pmap1.getNsPrefixURI("x")) ;
        prefixes.close() ;
    }
    
}
