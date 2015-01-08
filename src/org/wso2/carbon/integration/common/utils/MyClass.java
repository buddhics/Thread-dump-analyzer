package org.wso2.carbon.integration.common.utils;/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Set;

public class MyClass {
    public static void main(String[] args)
            throws AgentInitializationException, AgentLoadException, AttachNotSupportedException,
                   IOException, MalformedObjectNameException, InterruptedException {

//        ThreadDumpMonitor threadDumpMonitor = new ThreadDumpMonitor();
//        String connectorAddress = threadDumpMonitor.getConnectorAddress("15547");
//        MBeanServerConnection mBeanServerConnection = threadDumpMonitor.getMBeanServerConnection(connectorAddress);
//        Set<ObjectName> mbeanObjects = threadDumpMonitor.getMbeanObjects(connectorAddress);
//        ArrayList<ThreadMXBean> threadMXBeans = threadDumpMonitor.getThreadMXBeanObjects(mbeanObjects, mBeanServerConnection);
//        threadDumpMonitor.createThreadDumpFile(threadMXBeans, "/home/buddhi/Desktop");
//        long[] ids = threadDumpMonitor.getAllThreadIds(threadMXBeans);
//
//        for (long l:ids){
//            System.out.println(l);
//        }
//
//        System.out.println("Thread count:  "+threadDumpMonitor.getThreadCount(threadMXBeans,ids,"PassThroughHTTPSSender"));
//
//        System.out.println("Thread count using regex:  "+threadDumpMonitor.getThreadCountUsingRegex(threadMXBeans,ids,"PassThrough"));

        MBeanHandler myMBeanHandler = new MBeanHandler();

        String connectorAddress = myMBeanHandler.getConnectorAddress(5048);

        MBeanServerConnection mBeanServerConnection = myMBeanHandler.getMBeanServerConnection(connectorAddress);

        ObjectName objectName = myMBeanHandler.getRegisteredMBeanObject(mBeanServerConnection, ManagementFactory.THREAD_MXBEAN_NAME, null);

        ThreadAnalyser myThreadAnalyser = new ThreadAnalyser();

        ThreadMXBean myThreadMXBean = myThreadAnalyser.getThreadMXBeanObjects(objectName, mBeanServerConnection);

        ThreadInfo[] myThreadInfo = myThreadAnalyser.getAllThreadInfo(myThreadMXBean);

        myThreadAnalyser.createThreadDumpFile(myThreadInfo, "/home/buddhi/Desktop");

        System.out.println("done....");

    }
}
