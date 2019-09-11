# 校园搜索引擎实验报告
2016011388 潘庆霖 计61
2016011398 ⾼鸿鹏 计61
## 所使⽤的⼯具和代码框架
1. ⽹⻚抓取⼯具Heritrix1.14.4
2. Lucene 8.1.1
3. 分词⼯具： Lucene内置StandardAnalyzer
4. Html解析：Jsoup 1.21.1
5. PDF 解析：pdfbox 2.0.15
6. Doc 解析：poi 4.1.0
7. 前端服务：apache+tomcat (http://tomcat.apache.org/)
## 实验要求
+ 抓取清华校内绝⼤部分⽹⻚资源以及⼤部分在线万维⽹⽂本资源（含M.S. office⽂档、pdf⽂档等，约20-30万
个⽂件）
+ 实现基于概率模型的内容排序算法；
+ ⽂本检索实验已经让⼤家实现基于查询分词的VSM或BM25模型
+ 建议改写提供的图⽚搜索框架或查找开源资源在其之上进⾏加⼯。
+ 实现基于HTML结构的分域权重计算，并应⽤到搜索结果排序中；
+ 实现基于PageRank的链接结构分析功能，并应⽤到搜索结果排序中；
+ 采⽤便于⽤户信息交互的Web界⾯。
## 基本功能实现流程
### 1.使⽤Heritrix来抓取校园的⽹⻚
##### 教程参考：
https://www.ibm.com/developerworks/cn/opensource/os-cn-heritrix/
##### 在完成基础配置之后，将Heritrix改造成多线程抓取(50个线程)，参考教程如下
https://blog.csdn.net/yangding_/article/details/41122977
##### 然后在⽹⻚端开启Heritrix服务器开始抓取，相关选项的具体设置如下：
```
Heritrix组件配置参考
Select Crawl Scope
org.archive.crawler.scope.BroadScope
Select URI Frontier
org.archive.crawler.frontier.BdbFrontier
Select Pre Processors
org.archive.crawler.prefetch.Preselector
```
设置抓取的种⼦列表为
```
http://news.tsinghua.edu.cn
```
设置过滤器不抓取图书馆资源：
```
[\S]*lib.tsinghua.edu.cn[\S]*；
[\S]*166.111.120.[\S]*
```
设置正则表达式的过滤项
```
.*(?i)\.(mso|tar|txt|asx|asf|bz2|mpe?g|MPE?G| tiff?
|gif|GIF|png|PNG|ico|ICO|css|sit|eps|wmf|zip|pptx?|xlsx?|gz|rpm|tgz|mov|MOV|exe|jpe?g|JPE?
G|bmp|BMP|rar|RAR|jar|JAR|ZIP|zip|gz|GZ|wma|WMA|rm|RM|rmvb|RMVB|avi|AVI|swf|SWF|mp3|MP3|wmv
|WMV|ps|PS)$
```
### 2. ⽂本⽹⻚数据，数据处理与PageRank的计算
所有⽂件⻅⽂件夹Search\searcher\src\url_measure，各⽂件的⽤途如下：
+ clean_url.py
输⼊⽂件为crawl.log（Heretrix⾃带）,输出⽂件为cleaned_url.log,筛选需要的⽂件格式类型，过滤错误形式的
⽹⻚
+ get_all_information.py
主要完成对htm/html结尾的⽂件的内容提取，将提取出的信息保存在anchor.log与title.log中，然后⽣成计算
PageRank的⽹⻚关联信息，保存在tsinghua.graph中
+ get_page_rank.py
完成对PageRank的计算，主要依靠tsinghua.graph⽂件，⾸先计算出⼊度，再根据算法计算每个结点的
PageRank信息，再读⼊get_all_information⽣成的题⽬和锚⽂本信息(仅html⽂件处理)，输出为pagerank.py
⽂件
排名前⼗的⻚⾯信息如下：
```
/publish/thunews/index.html 0.054811795153016936 ⾸⻚ 清华⼤学新闻⽹
/publish/thunews/9652/index.html 0.0443036459792194 更多 &#8250; 清华⼤学新闻⽹ - 图
说清华
/publish/thunewsen/index.html 0.03675168257909178 ENGLISH Tsinghua University News
/publish/thunews/9650/index.html 0.026648308726768863 媒体清华 清华⼤学新闻⽹ - 媒
体清华
/publish/thunews/10303/index.html 0.02524876605085597 综合新闻 清华⼤学新闻⽹ - 综合新
闻
/publish/thunews/9649/index.html 0.02453537834353733 要闻聚焦 清华⼤学新闻⽹ - 要闻聚
焦
/publish/thunews/9657/index.html 0.024534809277958085 新闻合集 清华⼤学新闻⽹ - 新
闻合集
/publish/thunews/9656/index.html 0.024527182247498405 清华⼈物 清华⼤学新闻⽹ - 清
华⼈物
/publish/thunews/10304/index.html 0.024485341293384307 新闻排⾏ 清华⼤学新闻⽹ - 新
闻排⾏
/publish/thunews/10237/index.html 0.023906930952428785 rss 清华⼤学新闻⽹ - rss
```
### 3. 构建检索及倒排索引
#### 3.1 ⽂档解析
##### 3.1.1 HTML⽂件解析
实验中使⽤Jsoup⼯具包解析⽹⻚，抽取title标签的⽂本内容作为⽂档的的标题域；抽取p、span、td、div、li、a标
签的⽂本内容作为⽂档的内容域；a标签的内容表⽰⻚⾯链出的内容，也作为⼀个anchorOut域单独索引；h1-h6标
签的⽂本内容表⽰⻚⾯内的⼩标题，拿出来作为⼀个域；此外，进⼊⻚⾯的链接有着和⻚⾯标题相似的作⽤，单独成
为⼀个anchorIn域。
##### 3.1.2 PDF⽂件解析
实验中使⽤pdfbox解析⽂件获得内容域，直接以⽂件名作为标题域。(这也导致了搜索出来的pdf⽂件的标题有时候表
现为⼀串没有规律的数字)
##### 3.1.3 DOC⽂件解析
实验中使⽤POI包解析⽂件获得内容域，也是直接以⽂件名作为标题域。⽐较⿇烦的是，POI⼯具解析.doc⽂件.docx
⽂件的⽅法并不⼀样，因此需要在解析之前进⾏更细致的分类。
### 4. 检索
+ 借鉴学⻓的经验，使⽤MultiFieldQueryParser将多个Field组合在⼀起进⾏查询。同时，由于Lucene4.0的版本
有点⽼旧，我们采⽤了⽐较新的Lucene8.1.1,在API的使⽤上和图⽚搜索框架出现了⼀些不⼀致的地⽅。IK
Analyzer由于太久没有更新，也⽆法使⽤了，我们将分词器改为了Lucene提供的StandardAnalyzer。
+ 评分⽅法采⽤了Lucene中实现好的BM25评分。
+ 通过⼈为观察效果，来确定各个域在搜索过程中的权重。最终将分域权重调整为100、25、35、1、0.1。（依
次为标题域、h域、链⼊域、内容域、链出域）
# 运⾏⽅式
+ 运⾏环境 : Ubuntu16.04
+ IDE : IDEA
+ jdk : 12.0.1
+ 需要进⾏如下设置。
  + ⽤idea打开maven⼯程之后，由于不同的pc上tomcat的位置有可能不同，所以可能需要在idea中进⾏编
辑。
  + 将pagerank⽂件放置在项⽬顶层⽬录下，将源代码中的MyIndex.java中的page_root改为爬⾍爬取的⽂件
所在的⽬录，MyServlet.java中的indexDir改为建⽴的索引的全局⽬录（与MyIndexer中main函数的给⼊
参数⼀致）。
  + 运⾏配置好的项⽬，IDEA将会⾃动打开浏览器测试⽹⻚。
