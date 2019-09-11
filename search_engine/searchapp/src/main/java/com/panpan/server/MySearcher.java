package com.panpan.server;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;

public class MySearcher {
    private IndexReader reader;
    private IndexSearcher searcher;

    public MySearcher(String indexdir){
        try{
            reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir)));
            searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public TopDocs searchQuery(Query query, int maxnum){
        try{
            TopDocs results = searcher.search(query, maxnum);
            System.out.println(results.scoreDocs);
            return results;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Document getDoc(int docID){
        try{
            return searcher.doc(docID);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        MySearcher search = new MySearcher("WebRoot/forIndex/index");
    }
}
