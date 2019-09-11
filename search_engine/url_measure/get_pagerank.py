import numpy as np
import sys
import networkx as nx
import operator
from head import *
import datetime
from functools import cmp_to_key

#Damping coefficient
delta = 0.15

#page_rank_dict
pr_dict = {}
entry_dict = {}

#Record the entries and exits of web page nodes
in_degree = {}
out_degree = {}

#loop num
LOOP = 30

#Record the sum of PageRank values for no OUT nodes
no_out_sum = 0.0

#calculate in/out_degree of each node
graph = open(graph_file)
graph_list = graph.readlines()
for node_list in graph_list:
    node_list = node_list.replace('\n','')
    node_list = node_list.replace('\r','')
    temp_list = node_list.split(':')
    # set default value
    src = temp_list[0]
    in_degree.setdefault(src,0)
    out_degree.setdefault(src,0)
    # print(temp_list[0],temp_list[1])
    temp = temp_list[1]
    if len(temp_list[1]) != 0:
        in_nodes = temp.split(',')
        for i in in_nodes:
            if i != '':
                in_degree.setdefault(i,0)
                out_degree.setdefault(i,0)

                out_degree[src] += 1
                in_degree[i] += 1

print('in/out_degree calculate over')

N = len(out_degree)

for key in out_degree.keys():
    pr_dict[key] = float(1)/float(N)
    entry_dict[key] = delta/float(N)

    if out_degree[key] == 0:
        no_out_sum += pr_dict[key]

page_num = sum(pr_dict.values())

start = datetime.datetime.now()
for i in range(LOOP):
    print('loop = '+str(i)+' time is: '+ str(datetime.datetime.now()-start))
    temp_sum = 0.0

    for entry in graph_list:
        entry = entry.replace('\n','')
        entry = entry.replace('\r','')
        temp_entry = entry.split(':')
        src_node = temp_entry[0]
        dst_node = temp_entry[1].split(',')

        for dst in dst_node:
            if dst == '':
                continue
            entry_dict[dst] += (1.0 - delta) * pr_dict[src_node]/out_degree[src_node]

    for out in out_degree.keys():
        pr_dict[out] = entry_dict[out] + (1-delta)*no_out_sum/float(N)
        entry_dict[out] = delta/float(N)
        temp_sum+=pr_dict[out]

    no_out_sum = 0
    for out in out_degree.keys():
        if out_degree[out] == 0:
            no_out_sum += pr_dict[out]

sort_pgdict = sorted(pr_dict.items(), key=lambda x:x[1], reverse=True)
# sort_pgdict = sorted(pr_dict.items(), lambda x, y: mycmp(x[1], y[1]), reverse=True)
for i in sort_pgdict:
    print(i)
print("sort over")

title = open(title_file)
title_lines = title.readlines()

anchor = open(anchor_file)
anchor_lines = anchor.readlines()

url = open(url_file)
url_lines = url.readlines()

#begin to store page_rank
pf_file = open(pagerank_file,'w')

top = 0
for i in sort_pgdict:
    top += 1
    now_node = int(i[0])        #get now node
    my_anchor = anchor_lines[now_node].replace('\n', '')
    my_title = title_lines[now_node].replace('\n', '')
    my_url = url_lines[now_node].replace('\n', '')

    pwd = my_url.replace('http://news.tsinghua.edu.cn', '')

    pf_file.writelines(pwd+'\t'+str(i[1])+'\t'+my_anchor+'\t'+my_title+ '\n')

    if top < 11:
        print('No.' + str(top) + ' node: '+str(now_node) 
            + ' url: ' + my_url + ' anchor: ' + my_anchor + ' title: ' + my_title)