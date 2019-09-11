
#in order to get title anchor and build graph

import head
from head import *
import datetime
import sys
import re

'''
re.I: ignore W/w
re.L: special code \w, \W, \b, \B, \s, \S depend on enviroment
re.M: multiply rows
re.S: ' . '+ \n (' . 'excluding \n)
re.U: special code \w, \W, \b, \B, \d, \D, \s, \S depend on Unicode Character Attribute Database
'''

#pattern
href_pattern = re.compile(r'<a href=[\"\']([^\"\']*?\.(html|pdf|doc|docx))[\"\'].+?>(.+?)</a>', re.S)
html_pattern = re.compile(r'<a href=[\"\']([^\"\']*?\.html)[\"\']', re.S)
title_pattern = re.compile(r'<title>(.+?)</title>', re.I | re.M | re.S)


#url_information
title_dict = {}
anchor_dict = {}

#connection information
graph_dict = {}
num_dict = {}
log_list = []

dir_list = []	#store url except http://news.tsinghua.edu.cn

url_info = open(url_file)

url_list = url_info.readlines()
url_num = 0

for entry in url_list:
	url_num += 1

	if url_num > MAX_COUNT:
		print("Too Many Url!\n")
		break;
	entry = entry.replace('http://news.tsinghua.edu.cn', '')
	entry = entry.replace('\n','')
	dir_list.append(entry)

print("Collection url num = "+str(url_num))

for i,myentry in enumerate(dir_list):
	title_dict[i] = 'null'
	anchor_dict[i] = 'null'
	graph_dict.setdefault(i,[])
	num_dict[myentry] = i

print("Max in graph: "+ str(max(graph_dict.keys())))

print(len(title_dict),len(num_dict))

start_time = datetime.datetime.now()

for i in range(0,len(dir_list)):
	if(i % 500 == 0):
		print('No.'+str(i)+'time: '+str(datetime.datetime.now()-start_time))

	temp = dir_list[i]

	# temp = temp[0:-1]		#remove '\r'


	if not temp.endswith(('.html','.htm')):
		continue

	try:
		input = open(base_dir+temp,encoding='utf-8')
		my_html = input.read()
			# my_html = head.measure_html(my_html)
			# my_html = my_html.encoding('utf-8')
		# print(my_html)

	except:
		print("can't open :" + base_dir+temp)
	
	mea_list = re.findall(href_pattern,my_html)
	# print(mea_list)
	for mea_entry in mea_list:
		href = mea_entry[0]
		# print(href)
		href.replace('http://news.tsinghua.edu.cn', '')
		# print("\n")
		if href.find('http') == 0:	#ignore tv.tsinghua.edu.cn and so on
			continue
		try:
			index = num_dict[href]	
		except:
			log_list.append('Index Error')
			log_list.append(sys.exc_info()[0])

		try:
			anchor = mea_entry[2]
			#clean message
			r = re.compile(r'''<.*?>''', re.I | re.M | re.S)
			anchor = r.sub('', anchor)
			r = re.compile(r'''\s+''', re.I | re.M | re.S)
			anchor = r.sub(' ', anchor)
			anchor = anchor.replace('\n','')
			anchor = anchor.replace('\r','')
			anchor = anchor.replace('\t','')
			# print(anchor)

			if anchor == '' or anchor == ' ':
				anchor = 'null'

			if title_dict[index] == 'null':
				title_dict[index] = anchor

			anchor_dict[index] = anchor
			graph_dict[i].append(index)
		except:
			log_list.append('Ignore')
			log_list.append(sys.exc_info()[0])

		title_list = re.findall(title_pattern,my_html)
		if title_list:
			title = title_list[0]
			title = title.replace('\n', '')
			title = title.replace('\t', '')
		title_dict[i] = title
	print(title_dict[i])

	store measured data
graph_store = open(graph_file,'w',encoding='utf-8')
for key in graph_dict.keys():
	graph_store.write(str(key)+':')
	for entry in graph_dict[key]:
		graph_store.write(str(entry)+',')
	graph_store.write('\n')

anchor_store = open(anchor_file,'w',encoding='utf-8')
title_store = open(title_file,'w',encoding='utf-8')
for i in range(0,len(title_dict)):
	title_store.writelines("%s\n" % (title_dict[i]))
	anchor_store.writelines("%s\n" % (anchor_dict[i]))


log_store = open(error_file,'w',encoding='utf-8')
for error in log_list:
	log_store.writelines(error)
	log_store.writelines('\n')

