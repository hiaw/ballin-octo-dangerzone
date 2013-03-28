package com.stillpig.babyonthecloud.customcode;


import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.SDKServiceProvider;
import java.util.Arrays;
import java.util.List;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author danielchong
 */
public class ReceivePaymentFromiTunes implements CustomCodeMethod {

    public String getMethodName() {
        return "receive_payment_from_itunes";
    }

    public List<String> getParams() {
        return Arrays.asList("receipt", "type", "date", "valid_until");
    }

    public ResponseToProcess execute(ProcessedAPIRequest papir, SDKServiceProvider sdksp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
