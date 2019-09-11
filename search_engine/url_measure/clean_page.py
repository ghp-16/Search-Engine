import json

graph_dict = {}
map_dict = {}
page_dict = {}

def clean_url():
	file = open("crawl.log","r")
	entry = file.readlines();

	output = open("cleaned_url.log","w")
	num = 0;

	for i in range(0, len(entry)):
		line = ' '.join(filter(lambda x:x,entry[i].split(' ')))
		mylist = line.split(" ")		#all information about html
		# print(mylist)
		http_code = mylist[1]
		net_addr = mylist[3]
		net_addr2 = mylist[5]

		if http_code != '200':
			continue
		if mylist[2] == '0' or mylist[2] == '-':
			continue

		if net_addr.endswith(('.html','.pdf','.doc','.png','.PNG','.HTML','.jpg','JPG')):
			page_dict[net_addr] = 1;
			graph_dict.setdefault(net_addr2,[])
			graph_dict[net_addr2].append(net_addr)
			num += 1

			output.writelines("%s\n" % (net_addr))
	print("clean finished, page_num is "+str(num))
