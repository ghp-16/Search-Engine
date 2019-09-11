package com.panpan.indxer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.NumericUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyIndexer {
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private static String page_root = "/home/pan/Code/second-20190609144420843/mirror/news.tsinghua.edu.cn";

    public MyIndexer(String indexDir) {
        analyzer = new StandardAnalyzer();
        try {
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer).setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            indexWriter = new IndexWriter(dir, iwc);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MyIndexer indexer = new MyIndexer("forIndex/index");
        indexer.buildIndex("pagerank.log", page_root);
    }

    float pagerank;

    public void buildIndex(String pageRankFile, String pageRootDir) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(pageRankFile));
            String line;
            while((line = reader.readLine()) != null) {
                String[] items = line.split("\t", 4);
                String loc = items[0];
                String anchorIn;
                pagerank = Float.parseFloat(items[1]);
                pagerank = (float)Math.sqrt(pagerank);
                if (items[2].equals("null")) {
                    if(items[3].equals("null")){
                        anchorIn = "";
                    }else{
                        anchorIn = items[3];
                    }
                }else{
                    anchorIn = items[2];
                }

                File result;
                try{
                    result = new File(pageRootDir + loc);
                }catch (Exception e){
                    continue;
                }

                Document document = new Document();
                Field pathField = new StringField("path", loc, Field.Store.YES);
                document.add(pathField);
                Field anchorInField = new TextField("anchorIn", anchorIn, Field.Store.YES);
                document.add(anchorInField);

                boolean success = false;
                if(loc.endsWith("docx") || loc.endsWith("doc")){
                    success = parseDoc(result, document);
                } else if(loc.endsWith("pdf")){
                    success = parsePDF(result, document);
                }else if(loc.endsWith("html") || loc.endsWith("htm")) {
                    success = parseHtml(result, document);
                } else {
                    continue;
                }

                if (success) {
                    document.add(new SortedNumericDocValuesField("boost", NumericUtils.floatToSortableInt(pagerank)));
                    indexWriter.addDocument(document);
                }
            }
            indexWriter.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean parseDoc(File in, Document doc) {
        try{
            String name = in.getName().toLowerCase();
            String content = "";
            if(name.endsWith("docx")){
                content = getDocXContent(in);
            } else if(name.endsWith("doc")){
                content = getDocContent(in);
            } else {
                return false;
            }

            addContentField(content, doc);

            String title = in.getName();
//            System.out.println(title);
            addTitleField(title, doc);

            addTypeField("doc", doc);

            return true;
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private String getDocXContent(File in){
        try{
            InputStream is = new FileInputStream(in);
            XWPFWordExtractor extractor = new XWPFWordExtractor(new XWPFDocument(is));
            String content = extractor.getText();
            return content;
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private String getDocContent(File in){
        try{
            FileInputStream fis = new FileInputStream(in.getAbsolutePath());
            HWPFDocument doc = new HWPFDocument(fis);
            WordExtractor we = new WordExtractor(doc);
            return we.getText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean parsePDF(File in, Document doc){
        try {
            PDDocument pd = PDDocument.load(in);
            PDFTextStripper ts = new PDFTextStripper();
            String content = ts.getText(pd);
            pd.close();

            addContentField(content, doc);

            String title = in.getName();
            addTitleField(title, doc);
//            System.out.println(title);

            addTypeField("pdf", doc);

            return true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void addTitleField(String title, Document doc) {
        Field titleField = new TextField("title", title, Field.Store.YES);
        doc.add(titleField);
    }

    private void addContentField(String content, Document doc) {
        Field contentField = new TextField("content", content, Field.Store.YES);
        doc.add(contentField);
    }

    private void addTypeField(String type, Document doc){
        Field typeField = new TextField("type", type, Field.Store.YES);
        doc.add(typeField);
    }

    private boolean parseHtml(File in, Document doc) {
        try{
            org.jsoup.nodes.Document html = Jsoup.parse(in, "utf-8");

            String title = "";
            Elements titleEles = null;
            Elements headEles = html.getElementsByTag("head");
            if(headEles.size() > 0){
                Elements tempEles = headEles.get(0).getElementsByTag("title");
                if(tempEles.size() > 0){
                    titleEles = tempEles;
                } else {
                    titleEles = html.getElementsByTag("h1");
                }
                if(titleEles.size() > 0){
                    title = titleEles.get(0).text();
                    System.out.println(title);
                }
            }
            addTitleField(title, doc);

            String content = "";
            String anchorOut = "";
            String hStr = "";
            Elements elements = html.getElementsByClass("article");
            if(elements.size() > 0){
                for (org.jsoup.nodes.Element e : elements.get(0).select(
                        "p,span,td,div,li,a")) {
                    content += ' ' + e.ownText();
                }
                for (org.jsoup.nodes.Element e : elements.get(0)
                        .getElementsByTag("a")) {
                    anchorOut += ' ' + e.text();
                }
                for (org.jsoup.nodes.Element e : elements.get(0)
                        .getElementsByTag("h1,h2,h3,h4,h5,h6")) {
                    hStr += e.ownText();
                }
            }else{
                for (org.jsoup.nodes.Element e : html
                        .select("p,span,td,div,li,a")) {
                    content += ' ' + e.ownText();
                }
                for (org.jsoup.nodes.Element e : html.getElementsByTag("a")) {
                    anchorOut += ' ' + e.text();
                }
                for (org.jsoup.nodes.Element e : html
                        .getElementsByTag("h1,h2,h3,h4,h5,h6")) {
                    hStr += e.ownText();
                }
            }
            addContentField(content, doc);

            Field anchorOutField = new TextField("anchorOut", anchorOut, Field.Store.YES);
            doc.add(anchorOutField);
            Field hField = new TextField("h", hStr, Field.Store.YES);
            doc.add(hField);

            addTypeField("html", doc);

//            System.out.println(title);

            return true;
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public static List<String> tokenize(String queryString){
        Analyzer analyzer = new StandardAnalyzer();
        List<String> tokens = new ArrayList<String>();
        for(String part : queryString.split(" ")){
            if(part.length() == 0){
                continue;
            }
            int colonIndex = part.indexOf(':');
            if(colonIndex > 0){
                tokens.add(part);
            }else{
                TokenStream ts;
                try{
                    ts = analyzer.tokenStream("content", new StringReader(part));
                    ts.reset();
                }catch (Exception e){
                    e.printStackTrace();
                    return tokens;
                }
                try{
                    while (ts.incrementToken()){
                        tokens.add(ts.getAttribute(CharTermAttribute.class).toString());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        analyzer.close();
        return tokens;
    }

    @SuppressWarnings("serial")
    public static final Set<Character> stopChar = new HashSet<Character>() {
        {
            add('.');
            add(',');
            add('!');
            add('?');
            add('。');
            add('，');
            add('！');
            add('？');
        }
    };

    // 生成简介
    public static String genAbstract(List<String> tokens, String content){
        int maxLen = 300;
        int range = 30;
        String result = "";
        content = content.trim();
        List<Integer> startPositions = new ArrayList<Integer>();
        List<Integer> endPositions = new ArrayList<Integer>();
        for(String t : tokens){
            String token = t;
            int colonIndex = token.indexOf(':');
            if(colonIndex > 0){
                token = token.split(":")[0];
            }
            int pos = 0;
            // 创建一个不区分大小写的匹配器
            Pattern pattern = Pattern.compile(token, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);

            int num = 0;
            while(matcher.find(pos) && ++num < maxLen / range){ // 记录所有出现关键词的位置
                pos = matcher.start();
                startPositions.add(pos);
                endPositions.add(pos + token.length());
                ++pos;
            }
        }
        Collections.sort(startPositions);   // 保证记录的位置按出现的先后顺序
        Collections.sort(endPositions);
        int i = 0;
        int size = startPositions.size();   // 关键词的数量
        while (i < size) {
            int pos = startPositions.get(i);
            int end = endPositions.get(i);
            int ptr;
            // 截取向左最多range个字符的范围
            for(ptr = pos; ptr >= pos - range; --ptr) {
                if (ptr < 0 || stopChar.contains(content.charAt(ptr))) {
                    ++ptr;
                    break;
                }
            }
            result += content.subSequence(ptr, pos);
            // 标红关键词
            result += "<em>";
            result += content.subSequence(pos, end);
            result += "</em>";
            ++i;
            while (i < size) {
                pos = startPositions.get(i);
                if (end > pos) {    // 下一个关键词和当前关键词存在相互嵌套
                    result += "<em>";
                    result += content.subSequence(end, endPositions.get(i));
                    result += "</em>";
                    end = endPositions.get(i);
                    ++i;
                } else {
                    if (pos == end) {   // 下一个关键词正好和当前关键词相邻
                        result += content.subSequence(end, pos);
                        end = endPositions.get(i);
                        result += "<em>";
                        result += content.subSequence(pos, end);
                        result += "</em>";
                        ++i;
                    } else if (pos - end < range) { // 下一个关键词有一部分在当前关键词范围内
                        result += content.subSequence(end, pos);
                        end = endPositions.get(i);
                        result += "<em>";
                        result += content.subSequence(pos, end);
                        result += "</em>";
                        ++i;
                        if (result.length() > maxLen - range) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            // 截取向右最多range个字符的范围（或者多出的部分为关键词）
            for (ptr = end; ptr < end + range; ++ptr) {
                if (ptr >= content.length()
                        || stopChar.contains(content.charAt(ptr))) {
                    break;
                }
            }
            result += content.subSequence(end, ptr) + "... ";
        }
        return result;
    }
}
