/*
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

package org.wso2.carbon.integration.common.utils;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Date;


public class ThreadAnalyser {

    /**
     * Get ThreadMXBean object of the remote VM using Objectname object and server connection.
     *
     * @param nameOfObject          registered name of MBean object in MBeanServer.
     * @param mBeanServerConnection MBeanServerConnection object.
     * @return List of ThreadMXBean object which has only one value.
     * @throws java.io.IOException if MXBean proxy failure
     *                     can be results in an <tt>IOException</tt>.
     */
    //filter ThreadMXBean objects
    public ThreadMXBean getThreadMXBeanObjects(ObjectName nameOfObject,
                                               MBeanServerConnection mBeanServerConnection)
            throws IOException {
        return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, nameOfObject.toString(), ThreadMXBean.class);
    }

    //##############################################################################################

    /**
     * Return all thread ids using ThreadMXBean object of remote VM
     *
     * @param threadMXBean ThreadMXBean objects.
     * @return All thread ids.
     */
    public long[] getAllThreadIds(ThreadMXBean threadMXBean) {
        return threadMXBean.getAllThreadIds();
    }

    /**
     * Using ThreadMXBean object and thread Id, it'll return thread info of a thread.
     *
     * @param threadMXBean ArrayList of ThreadMXBean objects.
     * @param id           Thread Id.
     * @return ThreadInfo object.
     * @throws IndexOutOfBoundsException if empty array list of ThreadMXBean
     *                                   can be results in an <tt>IndexOutOfBoundsException</tt>.
     */
    //
    public ThreadInfo getThreadInfo(ThreadMXBean threadMXBean, long id)
            throws IndexOutOfBoundsException {
        return threadMXBean.getThreadInfo(id);
    }

    /**
     * Get all the thread names which provide ThreadMXBean object and match with a given name and.
     * if match was found, it will increase the value of <code>count</code> variable and finally
     * return that.
     *
     * @param threadMXBean ThreadMXBean object of remote VM.
     * @param allThreadIds Array of thread Ids which contains all the Thread Ids.
     * @param threadName   Thread name.
     * @return Integer value which is the number of threads.
     * @throws IndexOutOfBoundsException if empty array list of ThreadMXBean
     *                                   can be results in an <tt>IndexOutOfBoundsException</tt>.
     */
    public int getThreadCount(ThreadMXBean threadMXBean, long[] allThreadIds,
                              String threadName) throws IndexOutOfBoundsException {
        int count = 0;

        for (long allThreadId : allThreadIds) {
            if (threadMXBean.getThreadInfo(allThreadId).getThreadName().equals(threadName)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get all the thread names which provide ThreadMXBean object and get given name as a sub string
     * and. find that sub string inside the thread name if match was found, it will increase the
     * value of <code>count</code> variable and finally return that.
     *
     * @param threadMXBean ThreadMXBean object of remote VM.
     * @param allThreadIds Array of thread Ids which contains all the Thread Ids.
     * @param regex        sub string of thread name that wants to search.
     * @return Integer value which is the number of threads.
     * @throws IndexOutOfBoundsException if empty array list of ThreadMXBean
     *                                   can be results in an <tt>IndexOutOfBoundsException</tt>.
     */
    public int getThreadCountUsingRegex(ThreadMXBean threadMXBean, long[] allThreadIds,
                                        String regex) throws IndexOutOfBoundsException {
        int count = 0;

        for (long threadId : allThreadIds) {
            String threadName = threadMXBean.getThreadInfo(threadId).getThreadName();

            if (threadName.contains(regex)) {
                count++;
            }
        }
        return count;
    }

    //##############################################################################################

    /**
     * return ThreadInfo of all threads
     *
     * @param threadMXBean ThreadMXBean object of remote VM.
     * @return Array of ThreadInfo objects
     */
    public ThreadInfo[] getAllThreadInfo(ThreadMXBean threadMXBean) {
        ThreadInfo[] allThreadInfo = threadMXBean.dumpAllThreads(true, true);
        return allThreadInfo;
    }

    /**
     * find deadlocked threads and return ThreadInfo
     *
     * @param threadMXBean ThreadMXBean object of remote VM.
     * @return array
     */
    public ThreadInfo[] getDeadlockedThreads(ThreadMXBean threadMXBean) {
        ThreadInfo[] deadlockedThreadInfo = null;
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreads != null) {
            deadlockedThreadInfo = threadMXBean.getThreadInfo(deadlockedThreads);
        }
        return deadlockedThreadInfo;
    }

    /**
     * write thread dump into a file
     *
     * @param allThreadInfo ArrayList of ThreadMXBean objects.
     * @param path          path, that wants to create new file.
     * @throws java.io.IOException if create new file failure
     *                     can be results in an <tt>IOException</tt>.
     */
    public void createThreadDumpFile(ThreadInfo[] allThreadInfo, String path)
            throws IOException {

        Timestamp timeStamp = new Timestamp(new Date().getTime());

        StringBuilder absoluteFilePathBuilder = new StringBuilder();

        absoluteFilePathBuilder.append(path);
        absoluteFilePathBuilder.append(File.separator);
        absoluteFilePathBuilder.append(timeStamp);
        absoluteFilePathBuilder.append(".txt");

        File threadDumpFile = new File(absoluteFilePathBuilder.toString());

        if (!threadDumpFile.exists()) {
            if (!threadDumpFile.createNewFile()) {
                return;
            }
        }

        BufferedWriter bufferedWriter =
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(threadDumpFile),
                                                          Charset.defaultCharset()));
        try {
            if (allThreadInfo != null) {
                for (ThreadInfo threadInfo : allThreadInfo) {
                    StringBuilder dump = new StringBuilder();
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
                    bufferedWriter.write(dump.toString());
                    bufferedWriter.newLine();
                }
            } else {
                if(!threadDumpFile.delete()){

                }
            }
        }finally{
            bufferedWriter.close();
        }


    }

}
