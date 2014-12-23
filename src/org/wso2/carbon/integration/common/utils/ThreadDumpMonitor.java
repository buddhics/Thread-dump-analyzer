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
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Set;


public class ThreadDumpMonitor {

    private final String Local_Connector_Address = "com.sun.management.jmxremote.localConnectorAddress";
    private final String Java_Home = "java.home";
    private final String Library = "lib";
    private final String Management_Agent_Jar = "management-agent.jar";

    public String getConnectorAddress(String pid)
            throws IOException, AttachNotSupportedException, AgentLoadException,
                   AgentInitializationException {
        VirtualMachine remoteVirtualMachine = VirtualMachine.attach(pid);
        String connectorAddress = remoteVirtualMachine.getAgentProperties().getProperty(Local_Connector_Address);

        if (connectorAddress == null) {
            String agent = remoteVirtualMachine.getSystemProperties().getProperty(Java_Home) + File.separator + Library + File.separator + Management_Agent_Jar;
            remoteVirtualMachine.loadAgent(agent);
            connectorAddress = remoteVirtualMachine.getAgentProperties().getProperty(Local_Connector_Address);
        }

        return connectorAddress;
    }

    private MBeanServerConnection getMBeanServerConnection(String connectorAddress)
            throws IOException {
        JMXServiceURL serviceUrl = new JMXServiceURL(connectorAddress);
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);
        return connector.getMBeanServerConnection();
    }

    public Set<ObjectName> getMbeanObjects(String connectorAddress)
            throws IOException, MalformedObjectNameException {
        ObjectName objectName = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
        MBeanServerConnection mBeanServerConnection = getMBeanServerConnection(connectorAddress);
        return mBeanServerConnection.queryNames(objectName, null);
    }

    public ArrayList<ThreadMXBean> getThreadMXBeanObjects(Set<ObjectName> mBeans,
                                                          String connectorAddress)
            throws IOException {
        ArrayList<ThreadMXBean> threadMXBeans = new ArrayList<ThreadMXBean>();
        MBeanServerConnection mBeanServerConnection = getMBeanServerConnection(connectorAddress);

        for (ObjectName name : mBeans) {
            threadMXBeans.add(ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, name.toString(), ThreadMXBean.class));
        }

        return threadMXBeans;
    }

    private String getThreadDump(ThreadMXBean threadMXBean) throws NullPointerException {
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        final StringBuilder dump = new StringBuilder();

        for (ThreadInfo threadInfo : threadInfos) {
            dump.append("Thread name:");
            dump.append('"');
            dump.append(threadInfo.getThreadName());
            dump.append("\" ");

            final Thread.State state = threadInfo.getThreadState();

            dump.append("\n   java.lang.Thread.State: ");
            dump.append(state);

            final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();

            for (final StackTraceElement stackTraceElement : stackTraceElements) {
                dump.append("\nat ");
                dump.append(stackTraceElement);
            }

            dump.append("\n\n");
        }

        if(getDeadlockedThreadsInfo(threadMXBean) != null) {
            return dump.toString() + getDeadlockedThreadsInfo(threadMXBean);
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

    private String getDeadlockedThreadsInfo(ThreadMXBean threadMXBean) {
            ThreadInfo[] deadlockedThreads = getDeadlockedThreads(threadMXBean);

        if(deadlockedThreads != null) {
            final StringBuilder dump = new StringBuilder();

            String deadlockTag = "# Deadlocked Threads\n";

            for (ThreadInfo threadInfo : deadlockedThreads) {
                dump.append('"');
                dump.append(threadInfo.getThreadName());
                dump.append("\" ");

                final Thread.State state = threadInfo.getThreadState();

                dump.append("\n   java.lang.Thread.State: ");
                dump.append(state);

                final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();

                for (final StackTraceElement stackTraceElement : stackTraceElements) {
                    dump.append("\nat ");
                    dump.append(stackTraceElement);
                }

                dump.append("\n\n");
            }
            return deadlockTag + dump.toString();
        }
        return null;
    }

    public void createThreadDumpFile(ArrayList<ThreadMXBean> threadMXBeans) {
        for (ThreadMXBean threadMXBean : threadMXBeans) {
            String threadDump = getThreadDump(threadMXBean);
            System.out.println(threadDump);
        }
    }

}
