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

package org.seaborne.dboe.transaction.txn;

import java.nio.ByteBuffer ;



/** Single component aspect of a transaction */  
final 
class SysTrans { // implements X_SystemTransaction {
    private final TransactionalComponent elt ;
    private final Transaction transaction ;
    private final TxnId txnId ;
//    // Current mode - when promotable transaction are implemented, this will be resettable.
//    private final ReadWrite mode ;

    public SysTrans(TransactionalComponent elt, Transaction transaction, TxnId txnId) { 
        this.elt = elt ;
        this.transaction = transaction ;
        this.txnId = txnId ;
    }

    //@Override
    public void begin() { }

    //@Override
    public ByteBuffer commitPrepare() { return elt.commitPrepare(transaction) ; }

    //@Override
    public void commit() { elt.commit(transaction); }

    //@Override
    public void commitEnd() { elt.commitEnd(transaction); }

    //@Override
    public void abort() { elt.abort(transaction); }

    //@Override
    public void complete() { elt.complete(transaction); }
    
    public Transaction getTransaction()     { return transaction ; } 
    public TxnId getTxnId()                 { return txnId ; } 
    public ComponentId getComponentId()     { return elt.getComponentId() ; }
}