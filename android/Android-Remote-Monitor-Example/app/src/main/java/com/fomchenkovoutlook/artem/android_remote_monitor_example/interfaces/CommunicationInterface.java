package com.fomchenkovoutlook.artem.android_remote_monitor_example.interfaces;

import java.io.IOException;

public interface CommunicationInterface {

    void read(byte[] buffer, int bytes)
            throws IOException;
    void write(String data)
            throws IOException;
}
