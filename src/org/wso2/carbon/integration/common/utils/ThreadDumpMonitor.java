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
import com.sun.tools.attach.VirtualMachine;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class ThreadDumpMonitor {

    private final String LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
    private final String JAVA_HOME = "java.home";
    private final String LIBRARY = "lib";
    private final String MANAGEMENT_AGENT_JAR = "management-agent.jar";
    private final String DEADLOCK = "DEADLOCK";

    //get connector address using java process Id
    public String getConnectorAddress(String pid)
            throws IOException, AttachNotSupportedException, AgentLoadException,
                   AgentInitializationException {
        VirtualMachine remoteVirtualMachine = VirtualMachine.attach(pid);
        String connectorAddress = remoteVirtualMachine.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);

        if (connectorAddress == null) {
            String agent = remoteVirtualMachine.getSystemProperties().getProperty(JAVA_HOME) + File.separator + LIBRARY + File.separator + MANAGEMENT_AGENT_JAR;
            remoteVirtualMachine.loadAgent(agent);
            connectorAddress = remoteVirtualMachine.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
        }

        return connectorAddress;
    }

    //create connection to the MBean server
    public MBeanServerConnection getMBeanServerConnection(String connectorAddress)
            throws IOException {
        JMXServiceURL serviceUrl = new JMXServiceURL(connectorAddress);
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);
        return connector.getMBeanServerConnection();
    }

    //get all MBean objects in remote VM
    public Set<ObjectName> getMbeanObjects(String connectorAddress)
            throws IOException, MalformedObjectNameException {
        ObjectName objectName = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
        MBeanServerConnection mBeanServerConnection = getMBeanServerConnection(connectorAddress);
        return mBeanServerConnection.queryNames(objectName, null);
    }


    //filter ThreadMXBean objects
    public ArrayList<ThreadMXBean> getThreadMXBeanObjects(Set<ObjectName> mBeans,
                                                          MBeanServerConnection mBeanServerConnection)
            throws IOException {
        ArrayList<ThreadMXBean> threadMXBeans = new ArrayList<ThreadMXBean>();

        for (ObjectName name : mBeans) {
            threadMXBeans.add(ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, name.toString(), ThreadMXBean.class));
        }

        return threadMXBeans;
    }

    //##############################################################################################

    //return all thread Ids
    public long[] getAllThreadIds(ArrayList<ThreadMXBean> threadMXBeans){

        long[] allThreadIds = threadMXBeans.get(0).getAllThreadIds();

        return allThreadIds;
    }

    //return ThreadInfo using Thread Id
    public ThreadInfo getThreadInfo(ArrayList<ThreadMXBean> threadMXBeans, long Id)throws NullPointerException{

        ThreadInfo requestedThreadList = threadMXBeans.get(0).getThreadInfo(Id);

        return requestedThreadList;
    }

    public int getThreadCount(ArrayList<ThreadMXBean> threadMXBeans,long[] allThreadIds, String name)throws NullPointerException{
        int count = 0;

        for (int i = 0; i < allThreadIds.length; i++) {
            if(threadMXBeans.get(0).getThreadInfo(allThreadIds[i]).getThreadName().equals(name)){
                count++;
            }
        }
        return count;
    }

    public int getThreadCountUsingRegex(ArrayList<ThreadMXBean> threadMXBeans,long[] allThreadIds, String subString)throws NullPointerException{
        int count = 0;


        for (int i = 0; i < allThreadIds.length; i++) {
            String threadName = threadMXBeans.get(0).getThreadInfo(allThreadIds[i]).getThreadName();

            int indicator = threadName.split(subString).length;

            if(indicator>1){
                count++;
            }
        }
        return count;
    }

    //##############################################################################################

    //to get default thread dump, set threadInfoType to null
    private String getThreadDump(ThreadMXBean threadMXBean, String threadInfoType) throws NullPointerException {

        ThreadInfo[] threadInfos = null;

        if(threadInfoType == null){
            threadInfos = threadMXBean.dumpAllThreads(true, true);
        }else if(threadInfoType.equals(DEADLOCK)){
            threadInfos = getDeadlockedThreads(threadMXBean);
        }

          final StringBuilder dump = new StringBuilder();

        if(threadInfos != null) {
            for (ThreadInfo threadInfo : threadInfos) {
                dump.append("Thread name:");
                dump.append('"');
                dump.append(threadInfo.getThreadName());
                dump.append("\" ");

                final Thread.State state = threadInfo.getThreadState();

                dump.append("\n\tjava.lang.Thread.State: ");
                dump.append(state);

                final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();

                for (final StackTraceElement stackTraceElement : stackTraceElements) {
                    dump.append("\n\t\tat ");
                    dump.append(stackTraceElement);
                }

                dump.append("\n\n");
            }
        }

        return dump.toString();
    }

    private ThreadInfo[] getDeadlockedThreads(ThreadMXBean threadMXBean) {
        ThreadInfo[] deadlockedThreadInfos = null;
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        if(deadlockedThreads != null) {
            deadlockedThreadInfos = threadMXBean.getThreadInfo(deadlockedThreads);
        }
        return deadlockedThreadInfos;
    }

    public void createThreadDumpFile(ArrayList<ThreadMXBean> threadMXBeans,String path)
            throws IOException {

        String absolutePath = path+File.separator+"threadDumpFile.txt";

        File threadDumpFile = new File(absolutePath);
        //FileOutputStream fileOutputStream = new FileOutputStream(threadDumpFile);

        if(!threadDumpFile.exists()) {
            if(!threadDumpFile.createNewFile()){
                return;
            }
        }

        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(threadDumpFile), Charset.defaultCharset()));

        for (ThreadMXBean threadMXBean : threadMXBeans) {
            String threadDump = getThreadDump(threadMXBean,null);
            String deadlockThreadDump = getThreadDump(threadMXBean, DEADLOCK);

            String fullThreadDump = threadDump;

            if(null!=deadlockThreadDump){
                fullThreadDump = fullThreadDump+"\n\n#Deadlocked Threads\n\n"+deadlockThreadDump;
            }

            bufferedWriter.write(fullThreadDump);
            bufferedWriter.newLine();

        }

        bufferedWriter.close();
    }

}
