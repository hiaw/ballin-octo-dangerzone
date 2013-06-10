/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stillpig.babyonthecloud.customcode_dev;

import com.stackmob.core.jar.JarEntryObject;
import com.stackmob.customcode.dev.server.CustomCodeServer;
import com.stillpig.babyonthecloud.customcode.EntryPointExtender;

public class LocalDevServer {
    public static void main(String[] args) {
        final JarEntryObject entryObject = new EntryPointExtender();
        CustomCodeServer.serve(entryObject, "8ac98918-c048-4f60-881e-2b76a26a7e38", "170d6f42-2887-40d6-85fb-6a63506697a4", 8080);
    }
}