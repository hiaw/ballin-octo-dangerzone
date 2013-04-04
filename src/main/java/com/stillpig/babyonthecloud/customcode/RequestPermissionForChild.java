/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stillpig.babyonthecloud.customcode;

import com.stackmob.core.PushServiceException;
import com.stackmob.core.ServiceNotActivatedException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.*;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author danielchong
 */
public class RequestPermissionForChild implements CustomCodeMethod {

    public String getMethodName() {
        return "request_permission_for_child";
    }

    public List<String> getParams() {
        return Arrays.asList("child_code", "from_user");
    }

    private boolean sentPushNotificationToUser(SDKServiceProvider serviceProvider, String to_user, String from_user, String child_code) {
        try {
            PushService pushService = serviceProvider.getPushService();

            String message = from_user + "is requesting permission to sync with " + child_code + ".";
            //get all tokens for John Doe
            List<String> users = new ArrayList<String>();
            users.add(to_user);

            //send a push notification just to user's iOS device
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("badge", "1");
            payload.put("sound", "customsound.wav");
            payload.put("alert", message);

            //send a push notification to all of user's devices
            pushService.sendPushToUsers(users, payload);

            return true;

        } catch (PushServiceException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ServiceNotActivatedException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
        String child_code = request.getParams().get("child_code");
        String from_user = request.getParams().get("from_user");

        if (from_user == null || from_user.isEmpty() || child_code == null || child_code.isEmpty()) {
            HashMap<String, String> errParams = new HashMap<String, String>();
            errParams.put("error", "one or both the child code or from user was empty or null");
            return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
        }

        // get the datastore service and assemble the query
        DataService dataService = serviceProvider.getDataService();

        // build a query
        List<SMCondition> query = new ArrayList<SMCondition>();
        query.add(new SMEquals("child_code", new SMString(child_code)));

        // execute the query
        List<SMObject> result = null;
        try {
            boolean childFound = false;

            result = dataService.readObjects("child", query);

            SMObject childObject;
            String childName = null;
            String to_user_name = null;

            if (result != null && result.size() == 1) {
                childFound = true;

                childObject = result.get(0);
                childName = childObject.getValue().get("first_name").toString() + " "
                        + childObject.getValue().get("last_name").toString();
                SMList<SMString> main_users = (SMList<SMString>) childObject.getValue().get("main_users");
                SMString to_user = main_users.getValue().get(0);

                // Find out the name of main user.
                List<SMCondition> query2 = new ArrayList<SMCondition>();
                query2.add(new SMEquals("username", to_user));
                List<SMObject> result2 = dataService.readObjects("user", query2);

                SMObject foundUser = result2.get(0);
                to_user_name = foundUser.getValue().get("first_name").toString() + " "
                        + foundUser.getValue().get("last_name").toString();
            }

            Map<String, Object> returnMap = new HashMap<String, Object>();

            if (childFound) {

//                boolean sentPushNotification = sentPushNotificationToUser(serviceProvider, to_user, from_user, child_code);

//                if (!sentPushNotification) {
                // Do something to keep trying to sent push notification to user.
//                }

                returnMap.put("status", "Your request has been sent to the person responsible, " + to_user_name + ", for " + childName
                        + ". You will receive a notification when this person approves your request.");
            } else {
                returnMap.put("status", "Sorry, child was not found in system.");
            }

            return new ResponseToProcess(HttpURLConnection.HTTP_OK, returnMap);

        } catch (Exception e) {
            HashMap<String, String> errMap = new HashMap<String, String>();
            errMap.put("error", "unknown");
            errMap.put("detail", e.toString());
            errMap.put("result", result.get(0).toString());
            return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
        }
    }
}
