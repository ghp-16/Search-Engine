import re

#direction
base_dir = '/media/ghp/Fun/Java/program/MyHeritrix/jobs/second-20190609144420843/mirror/news.tsinghua.edu.cn'
base_url = 'http://news.tsinghua.edu.cn'

error_file = 'error.log'
graph_file = 'tsinghua.graph'
anchor_file = 'anchor.log'
title_file = 'title.log'
url_file = 'cleaned_url.log'
pagerank_file = 'pagerank.txt'

MAX_COUNT = 5000000

'''
re.I: ignore W/w
re.L: special code \w, \W, \b, \B, \s, \S depend on enviroment
re.M: multiply rows
re.S: ' . '+ \n (' . 'excluding \n)
re.U: special code \w, \W, \b, \B, \d, \D, \s, \S depend on Unicode Character Attribute Database
'''

def measure_html(content):
	#remove css and js style in html
	r = re.compile(r'''<script.*?</script>''', re.I | re.M | re.S)
	s = r.sub('', content)
	r = re.compile(r'''<style.*?</style>''', re.I | re.M | re.S)
	s = r.sub('', s)
	r = re.compile(r'''<!--.*?-->''', re.I | re.M | re.S)
	s = r.sub('', s)
	r = re.compile(r'''<meta.*?>''', re.I | re.M | re.S)
	s = r.sub('', s)
	r = re.compile(r'''<ins.*?</ins>''', re.I | re.M | re.S)
	s = r.sub('', s)
	r = re.compile(r'''<embed.*?>''', re.I | re.M | re.S)
	s = r.sub('', s)

	r = re.compile(r'''<img.*?>''', re.I | re.M | re.S)
	s = r.sub('', s)


	return s