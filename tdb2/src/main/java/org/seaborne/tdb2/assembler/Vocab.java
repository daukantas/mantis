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

package org.seaborne.tdb2.assembler ;

import org.apache.jena.rdf.model.Property ;
import org.apache.jena.rdf.model.Resource ;
import org.apache.jena.rdf.model.ResourceFactory ;

public class Vocab {
    public static Resource type(String namespace, String localName) {
        return ResourceFactory.createResource(namespace + localName) ;
    }

    public static Resource resource(String namespace, String localName) {
        return ResourceFactory.createResource(namespace + localName) ;
    }

    public static Property property(String namespace, String localName) {
        return ResourceFactory.createProperty(namespace + localName) ;
    }
}
