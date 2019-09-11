package com.panpan;

import com.panpan.indxer.MyIndexer;
import com.panpan.server.MySearcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyServlet extends HttpServlet {
    private MySearcher searcher = null;
    private Analyzer analyzer;
    private Highlighter highlighter = null;
    private SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter("<font color='red'>", "</font>");

    public static final int PAGE_RESULT = 10;
    public static final String indexDir = "forIndex";   // TODO 确定一个索引存放的目录

    public MyServlet() {
        super();
        analyzer = new StandardAnalyzer();
        searcher = new MySearcher(indexDir + "/index");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println(System.getProperty("user.dir"));
        resp.setContentType("text/html;charset=utf-8");
        req.setCharacterEncoding("utf-8");
        String queryString = req.getParameter("query"); // 获取jsp中的query变量
        String pageString = req.getParameter("page");   // 获取jsp中的page变量
        int page = 1;
        if(pageString != null){
            page = Integer.parseInt(pageString);
        }
        if(queryString == null){
            System.out.println("query is null");
        }else{
            List<String> tokens = MyIndexer.tokenize(queryString);
            System.out.println(tokens.toString());

            String[] titles = null;
            String[] absts = null;
            String[] paths = null;
            String[] types = null;

            Query query = null;

            String[] fields = {"title", "content", "path", "type"};
            Map<String, Float> boosts = new HashMap<String, Float>();
            boosts.put("title", 100.0f);
            boosts.put("h", 25.0f);
            boosts.put("anchorIn", 35.0f);
            boosts.put("content", 1.0f);
            boosts.put("anchorOut", 0.01f);

            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, boosts);
            parser.setDefaultOperator(QueryParser.AND_OPERATOR);
            try{
                query = parser.parse(queryString);
            }catch (Exception e){
                e.printStackTrace();
            }
            highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
            TopDocs results = searcher.searchQuery(query, 100);

            TokenStream ts = null;

            if(results != null) {
                ScoreDoc[] hits = locateByPageID(results.scoreDocs, page);
                if(hits != null){
                    titles = new String[hits.length];
                    absts = new String[hits.length];
                    paths = new String[hits.length];
                    types = new String[hits.length];
                    for(int i = 0; i < hits.length && i < PAGE_RESULT; i++ ){
                        Document doc = searcher.getDoc(hits[i].doc);
                        try{
                            ts = analyzer.tokenStream("content", new StringReader(doc.get("content")));
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        titles[i] = doc.get("title");
                        paths[i] = doc.get("path");
                        types[i] = doc.get("type");

                        try{
                            absts[i] = highlighter.getBestFragments(ts, doc.get("content"), 3, "...");
                        }catch (Exception e){
                            e.printStackTrace();
                            try{
                                absts[i] = MyIndexer.genAbstract(tokens, doc.get("content")) + " " + doc.get("anchorIn");
                            }catch (Exception ee){
                                ee.printStackTrace();
                                absts[i] = "null";
                            }
                        }
                        if(absts[i].length() == 0){
                            absts[i] = MyIndexer.genAbstract(tokens, doc.get("content")) + " " + doc.get("anchorIn");
                        }
                    }
                }else{
                    System.out.println("page is null");
                }
            }else{
                System.out.println("result is null");
            }
            req.setAttribute("currentQuery", queryString);
            req.setAttribute("currentPage", page);
            req.setAttribute("titles", titles);
            req.setAttribute("paths", paths);
            req.setAttribute("types", types);
            req.setAttribute("absts", absts);
            req.getRequestDispatcher("/show.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    // 根据当前处于第几页在结果中定位至多十个项
    public ScoreDoc[] locateByPageID(ScoreDoc[] results, int page) {
        if (results == null || results.length < (page - 1) * PAGE_RESULT) {
            return null;
        }
        int start = Math.max((page - 1) * PAGE_RESULT, 0);
        int documentNum = Math.min(results.length - start, PAGE_RESULT);
        ScoreDoc[] ret = new ScoreDoc[documentNum];
        for (int i = 0; i < documentNum; i++) {
            ret[i] = results[start + i];
        }
        return ret;
    }
}
