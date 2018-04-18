#!/usr/bin/python
# coding: utf8
#Copyright Â© 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#This program and the accompanying materials are made available under the
#terms of the Eclipse Public License v1.0 which accompanies this distribution,
#and is available at http://www.eclipse.org/legal/epl-v10.html

import sys
import zmq
import struct
import socket as socketlib
import datetime
import time
import signal
import argparse
import thread
import os

#from beautifultable import BeautifulTable
from prettytable import PrettyTable

parser = argparse.ArgumentParser(
                    description="Subscribes and counts ZMQ messages on the SB. Displays message types on exit",
                    formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument('--quiet', action='store_true', default=False,
                    dest='quiet_boolean_switch',
                    help='Don\'t print every message, but still maintain a count')
results = parser.parse_args()

totalcount = 0
totalCreateCount = 0
totalUpdateCount = 0
lstCreateTime = []
lstUpdateTime = []
lstCreateNBHandlerTime = []
lstCreateAQHandlerTime = []
lstUpdateNBHandlerTime = []
lstUpdateAQHandlerTime = []

lstCreateAQTranfTime = []
lstCreateNBTranfTime = []
lstUpdateAQTranfTime = []
lstUpdateNBTranfTime = []


totalDeleteCount = 0
lstDeleteTime = []
lstDeleteNBHandlerTime = []
lstDeleteAQHandlerTime = []
lstDeleteAQTranfTime = []
lstDeleteNBTranfTime = []

port = "5560"
# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)
print "Collecting updates from server... "
socket.connect ("tcp://localhost:%s" % port)
topicfilter = "1001"
socket.setsockopt(zmq.SUBSCRIBE, topicfilter)
print "Listening to port ", port

def print_csv():
    print "MsgType,  TotalMsgs , NBHndlr min(ms) , NBHndlr max(ms) , ActvHndlr min(ms) , ActvHndlr max(ms) , TotalSvctime min(ms)  TotalSvctime max(ms)"

    print "CREATE ,", totalCreateCount," , ", min(lstCreateNBHandlerTime), " , " , max(lstCreateNBHandlerTime)," , ",  min(lstCreateAQHandlerTime)," , " , max(lstCreateAQHandlerTime)," , ",  min(lstCreateTime), " , " ,max(lstCreateTime)
    print "UPDATE ,", totalUpdateCount," , " , min(lstUpdateNBHandlerTime)," , " , max(lstUpdateNBHandlerTime)," , " , min(lstUpdateAQHandlerTime)," , " , max(lstUpdateAQHandlerTime)," , ",  min(lstUpdateTime)," , ",  max(lstUpdateTime)
    print "DELETE ,", totalDeleteCount," , " , min(lstDeleteNBHandlerTime)," , " , max(lstDeleteNBHandlerTime)," , " , min(lstDeleteAQHandlerTime)," , " , max(lstDeleteAQHandlerTime)," , ",  min(lstDeleteTime)," , ",  max(lstDeleteTime)


def catch_signal(signal, frame):
    socket.close()
    print_csv()
    quit()


signal.signal(signal.SIGINT, catch_signal)

print "Operation , OpId , NBHandlr Time(ms), ActHanlr Time (ms), NB Queue Delay (ms), ActHandlr Queue Delay (ms) , Total Srv Time (ms)"


for update_nbr in range(90000000):

    string = socket.recv()

    millis = int(round(time.time() * 1000))
    topic, timestamp, operation, opId, contextid = string.split()

    print topic, timestamp, operation , opId , contextid
    totalcount += 1

    nbHanlderTime=int(timestamp.split(",")[2].split(":")[1])-int(timestamp.split(",")[1].split(":")[1])
    aqHandlerTime=int(timestamp.split(",")[4].split(":")[1])-int(timestamp.split(",")[3].split(":")[1])
    psToZmqTime=int(timestamp.split(",")[4].split(":")[1])-int(timestamp.split(",")[0].split(":")[1])

    actQueueDelay=int(timestamp.split(",")[3].split(":")[1])-int(timestamp.split(",")[2].split(":")[1])
    nbQueueDelay=int(timestamp.split(",")[1].split(":")[1])-int(timestamp.split(",")[0].split(":")[1])

    print operation," , " ,opId, " , ",nbHanlderTime," , ",aqHandlerTime," , ",nbQueueDelay," , ",actQueueDelay," , ",psToZmqTime

    if operation == "1" :
       totalCreateCount += 1
       lstCreateNBHandlerTime.append(nbHanlderTime)
       lstCreateAQHandlerTime.append(aqHandlerTime)
       lstCreateTime.append(psToZmqTime)
       lstCreateAQTranfTime.append(actQueueDelay)
       lstCreateNBTranfTime.append(nbQueueDelay)

    elif operation == "2" :
       totalUpdateCount += 1
       lstUpdateNBHandlerTime.append(nbHanlderTime)
       lstUpdateAQHandlerTime.append(aqHandlerTime)
       lstUpdateTime.append(psToZmqTime)
       lstUpdateAQTranfTime.append(actQueueDelay)
       lstUpdateNBTranfTime.append(nbQueueDelay)

    elif operation == "3" :
       totalDeleteCount += 1
       lstDeleteNBHandlerTime.append(nbHanlderTime)
       lstDeleteAQHandlerTime.append(aqHandlerTime)
       lstDeleteTime.append(psToZmqTime)
       lstDeleteAQTranfTime.append(actQueueDelay)
       lstDeleteNBTranfTime.append(nbQueueDelay)

socket.close()
