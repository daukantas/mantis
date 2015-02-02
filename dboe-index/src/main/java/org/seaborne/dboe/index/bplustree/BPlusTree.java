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

package org.seaborne.dboe.index.bplustree;

import static org.seaborne.dboe.index.bplustree.BPlusTreeParams.CheckingTree ;

import java.util.Iterator ;

import org.apache.jena.atlas.io.IndentedWriter ;
import org.apache.jena.atlas.iterator.Iter ;
import org.seaborne.dboe.base.record.Record ;
import org.seaborne.dboe.base.record.RecordFactory ;
import org.seaborne.dboe.base.record.RecordMapper ;
import org.seaborne.dboe.base.recordbuffer.RecordBufferPageMgr ;
import org.seaborne.dboe.base.recordbuffer.RecordRangeIterator ;
import org.seaborne.dboe.index.RangeIndex ;
import org.seaborne.dboe.transaction.txn.TransactionalMRSW ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

/** B-Tree converted to B+Tree
 * 
 * B-Tree taken from:
 * <pre>
 * Introduction to Algorithms, Second Edition
 * Chapter 18: B-Trees
 * by Thomas H. Cormen, Charles E. Leiserson, 
 *    Ronald L. Rivest and Clifford Stein 
 * </pre> 
 * Includes implementation of removal.
 * <p>
 * Notes:
 * <ul>
 * <li>
 * Stores "records", which are a key and value (the value may be null).
 * <li>
 * In this B+Tree implementation, the (key,value) pairs are held in
 * RecordBuffer, which wrap a ByteBuffer that only has records in it.  
 * BPTreeRecords provides the B+Tree view of a RecordBuffer. All records
 * are in RecordBuffer - the "tree" part is an index for finding the right
 * page. The tree only holds keys, copies from the (key, value) pairs in
 * the RecordBuffers. 
 *<li>
 * The version above splits nodes on the way down when full,
 * not when needed where a split can bubble up from below.
 * It means it only ever walks down the tree on insert.
 * Similarly, the delete code ensures a node is suitable
 * before decending. 
 * </ul>
 */

public class BPlusTree extends TransactionalMRSW implements Iterable<Record>, RangeIndex
{
    /*
     * Insertion:
     * There are two styles for handling node splitting.
     * 
     * Classically, when a leaf is split, the separating key is inserted into
     * the parent, which may itself be full and so that is split, etc propagting
     * up to the root (splitting the root is the only time the depth of the
     * BTree increases). This involves walking back up the tree.
     * 
     * It is more convenient to have a spare slot in a tree node, so that the
     * new key can be inserted, then the keys and child pointers split.
     * 
     * Modification: during insertion, splitting is applied to any full node
     * traversed on the way down, resulting in any node passed through having
     * some space for a new key. When splitting starts at a leaf, only the
     * immediate parent is changed because it must have space for the new key.
     * There is no cascade back to the top of the tree (it would have happened on
     * the way down); in other words, splitting is done early. This is insertion
     * in a single downward pass.
     * 
     * When compared to the classic approach including the extra slot for
     * convenient inserting, the space useage is approximately the same.
     * 
     * Deletion:    
     * Deletion always occurs at a leaf; if it's an internal node, swap the key
     * with the right-most left key (predecessor) or left-most right key (successor),
     * and delete in the leaf.
     * 
     * The classic way is to propagate node merging back up from the leaf.  The
     * book outlines a way that checks that a nod eis delte-suitable (min+1 in size)
     * on the way down.  This is implemented here; this is one-pass(ish).  
     * 
     * Variants:
     * http://en.wikipedia.org/wiki/Btree
     * 
     * B+Tree: Tree contains keys, and only the leaves have the values. Used for
     * secondary indexes (external pointers) but also for general on-disk usage
     * because more keys are packed into a level. Can chain the leaves for a
     * sorted-order traversal.
     * 
     * B*Tree: Nodes are always 2/3 full. When a node is full, keys are shared adjacent
     * nodes and if all they are all full do 2 nodes get split into 3 nodes.
     * Implementation wise, it is more complicated; can cause more I/O.
     * 
     * B#Tree: A B+Tree where the operations try to swap nodes between immediate
     * sibling nodes instead of immediately splitting (like delete, only on insert).
     */ 
    
    private static Logger log = LoggerFactory.getLogger(BPlusTree.class) ;
    
    private int rootIdx = BPlusTreeParams.RootId ;
    ///*package*/ BPTreeNode root ;
    private BPTreeNodeMgr nodeManager ; 
    private BPTreeRecordsMgr recordsMgr; 
    private final BPlusTreeParams bpTreeParams ;
    
    // Construction is a two stage process
    //    Create the object, uninitialized
    //    Setup data structures
    //    initialize
    /*package*/ BPlusTree(BPlusTreeParams bpTreeParams) { 
        this.rootIdx = -99 ;
        this.bpTreeParams = bpTreeParams ;
        this.nodeManager = null ;
        this.recordsMgr = null ;
    }

    /*package*/ void init(int rootId, BPTreeNodeMgr  nodeManager, BPTreeRecordsMgr recordsMgr) {
        this.rootIdx = rootId ;
        this.nodeManager = nodeManager ;
        this.recordsMgr = recordsMgr ;
    }

    private BPTreeNode getRootRead() {
        // No caching here.
        return nodeManager.getRead(rootIdx, BPlusTreeParams.RootParent) ;
    }

    private BPTreeNode getRootWrite() {
        // No caching here.
        return nodeManager.getRead(rootIdx, BPlusTreeParams.RootParent) ;
    }

    private void releaseRootRead(BPTreeNode rootNode) {
        rootNode.release() ;
    }

    private void releaseRootWrite(BPTreeNode rootNode) {
        rootNode.release() ;
    }

    private void setRoot(BPTreeNode node) {
        // root = node ;
    }

    /** Get the parameters describing this B+Tree */
    public BPlusTreeParams getParams()          { return bpTreeParams ; } 

    /** Only use for careful manipulation of structures */
    public BPTreeNodeMgr getNodeManager()       { return nodeManager ; }
    
    /** Only use for careful manipulation of structures */
    public BPTreeRecordsMgr getRecordsMgr()     { return recordsMgr ; }
    
    @Override
    public RecordFactory getRecordFactory() {
        return bpTreeParams.recordFactory ;
    }

    @Override
    public Record find(Record record) {
        startReadBlkMgr() ;
        BPTreeNode root = getRootRead() ;
        Record v = BPTreeNode.search(root, record) ;
        releaseRootRead(root) ;
        finishReadBlkMgr() ;
        return v ;
    }

    @Override
    public boolean contains(Record record) {
        Record r = find(record) ;
        return r != null ;
    }

    @Override
    public Record minKey() {
        startReadBlkMgr() ;
        BPTreeNode root = getRootRead() ;
        Record r = root.minRecord() ;
        releaseRootRead(root) ;
        finishReadBlkMgr() ;
        return r ;
    }

    @Override
    public Record maxKey() {
        startReadBlkMgr() ;
        BPTreeNode root = getRootRead() ;
        Record r = root.maxRecord() ;
        releaseRootRead(root) ;
        finishReadBlkMgr() ;
        return r ;
    }

    @Override
    public boolean add(Record record) {
        return addAndReturnOld(record) == null ;
    }

    /** Add a record into the B+Tree */
    public Record addAndReturnOld(Record record) {
        startUpdateBlkMgr() ;
        BPTreeNode root = getRootWrite() ;
        Record r = BPTreeNode.insert(root, record) ;
        if ( CheckingTree )
            root.checkNodeDeep() ;
        releaseRootWrite(root) ;
        finishUpdateBlkMgr() ;
        return r ;
    }
    
    @Override
    public boolean delete(Record record) {
        return deleteAndReturnOld(record) != null ;
    }

    public Record deleteAndReturnOld(Record record) {
        startUpdateBlkMgr() ;
        BPTreeNode root = getRootWrite() ;
        Record r = BPTreeNode.delete(root, record) ;
        if ( CheckingTree )
            root.checkNodeDeep() ;
        releaseRootWrite(root) ;
        finishUpdateBlkMgr() ;
        return r ;
    }

    private static Record noMin = null ; 
    private static Record noMax = null ; 
    
    @Override
    public Iterator<Record> iterator() {
        return iterator(noMin, noMax) ; 
    }

    @Override
    public Iterator<Record> iterator(Record fromRec, Record toRec) {
        return iterator(fromRec, toRec, RecordFactory.mapperRecord) ;
    }
    
    @Override
    public <X> Iterator<X> iterator(Record minRec, Record maxRec, RecordMapper<X> mapper) {
        startReadBlkMgr() ;
        BPTreeNode root = getRootRead() ;
        Iterator<X> iter = iterator(root, minRec, maxRec, mapper) ;
        releaseRootRead(root) ;
        finishReadBlkMgr() ;
        // Note that this end the read-part (find the start), not the iteration.
        // Iterator read blocks still get handled.
        return iter ;
    }

    /** Iterate over a range of fromRec (inclusive) to toRec (exclusive) */ 
    private static <X> Iterator<X> iterator(BPTreeNode node, Record fromRec, Record toRec, RecordMapper<X> mapper) {
        // Look for starting RecordsBufferPage id.
        int id = BPTreeNode.recordsPageId(node, fromRec) ;
        if ( id < 0 )
            return Iter.nullIter() ;
        RecordBufferPageMgr pageMgr = node.bpTree.getRecordsMgr().getRecordBufferPageMgr() ;
        // No pages are active at this point.
        return RecordRangeIterator.iterator(id, fromRec, toRec, pageMgr, mapper) ;
    }
    
    private static <X> Iterator<X> iterator(BPTreeNode node, RecordMapper<X> mapper) { 
        return iterator(node, null, null, mapper) ; 
    }
    
    @Override
    protected void startWriteTxn() {
        startBatch() ;
    }
    
    @Override
    protected void finishWriteTxn() {
        finishBatch() ;
    }
    
    // Internal calls.
    private void startReadBlkMgr() {
        nodeManager.startRead() ;
        recordsMgr.startRead() ;
    }

    private void finishReadBlkMgr() {
        nodeManager.finishRead() ;
        recordsMgr.finishRead() ;
    }

    private void startUpdateBlkMgr() {
        nodeManager.startUpdate() ;
        recordsMgr.startUpdate() ;
    }

    private void finishUpdateBlkMgr() {
        nodeManager.finishUpdate() ;
        recordsMgr.finishUpdate() ;
    }

    // Or Txn interface?
    public void startBatch() {
        nodeManager.startBatch() ;
        recordsMgr.startBatch() ;
    }

    public void finishBatch() {
        nodeManager.finishBatch() ;
        recordsMgr.finishBatch() ;
    }

    @Override
    public boolean isEmpty() {
        startReadBlkMgr() ;
        BPTreeNode root = getRootRead() ;
        boolean b = !root.hasAnyKeys() ;
        releaseRootRead(root) ;
        finishReadBlkMgr() ;
        return b ;
    }
    
    private static int SLICE = 10000 ;
    @Override
    public void clear() {
        Record[] records = new Record[SLICE] ;
        while(true) {
            Iterator<Record> iter = iterator() ;
            int i = 0 ; 
            for ( i = 0 ; i < SLICE ; i++ ) {
                if ( ! iter.hasNext() )
                    break ;
                Record r = iter.next() ;
                records[i] = r ;
            }
            if ( i == 0 )
                break ;
            for ( int j = 0 ; j < i ; j++ ) {
                delete(records[j]) ;
                records[j] = null ;
            }
        }
    }
    
    @Override
    public void sync() {
        if ( nodeManager.getBlockMgr() != null )
            nodeManager.getBlockMgr().sync() ;
        if ( recordsMgr.getBlockMgr() != null )
            recordsMgr.getBlockMgr().sync() ;
    }

    @Override
    public void close() {
        if ( nodeManager.getBlockMgr() != null )
            nodeManager.getBlockMgr().close() ;
        if ( recordsMgr.getBlockMgr() != null )
            recordsMgr.getBlockMgr().close() ;
    }
    
//    public void closeIterator(Iterator<Record> iter)
//    {
//    }

    @Override
    public long size() {
        Iterator<Record> iter = iterator() ;
        return Iter.count(iter) ;
    }

    @Override
    public void check() {
        BPTreeNode root = getRootRead() ;
        try { root.checkNodeDeep() ; }
        finally { releaseRootRead(root) ; }
    }

    public void dump() {
        // Caution - nesting via startReadBlkMgr
        startReadBlkMgr() ;
        BPTreeNode root = getRootRead() ;
        try { root.dump() ; }
        finally { releaseRootRead(root) ; }
        finishReadBlkMgr() ;
    }

    public void dump(IndentedWriter out) {
        BPTreeNode root = getRootRead() ;
        try { root.dump(out) ; }
        finally { releaseRootRead(root) ; }
    }
}