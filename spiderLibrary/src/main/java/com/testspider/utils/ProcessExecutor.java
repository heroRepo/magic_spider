package com.testspider.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProcessExecutor {
    private List<String> result;
    private List<String> error;
    public List<String> execute(String cmd, String... args) {
        List<String> command = new ArrayList<>();
        command.add(cmd);
        if(null != args) {
            for (String arg : args) {
                command.add(arg);
            }
        }
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        try {
            Process process = processBuilder.start();
            WatchThread info = new WatchThread(process.getInputStream());
            info.start();
            WatchThread error = new WatchThread(process.getErrorStream());
            error.start();
            process.waitFor();
            this.result = info.getStream();
            info.setOver(true);
            this.error = error.getStream();
            error.setOver(true);
        } catch (Throwable e) {
            error = new ArrayList<>();
            error.add(e.getMessage());
        }
        return this.result;
    }

    class WatchThread extends Thread   {
        boolean   over;
        ArrayList<String> stream;
        InputStream inputStream;
        public WatchThread(InputStream inputStream) {
            over = false;
            stream = new ArrayList<String>();
            this.inputStream = inputStream;
        }
        public void run() {
            try {
                Scanner br = new Scanner(inputStream, "UTF-8");
                while (true) {
                    if (over) break;
                    while(br.hasNextLine()){
                        String tempStream = br.nextLine();
                        stream.add(tempStream);
                    }
                }
            } catch(Exception   e){e.printStackTrace();}
        }

        public void setOver(boolean   over)   {
            this.over   =   over;
        }
        public ArrayList<String> getStream() {
            return stream;
        }
    }
}
