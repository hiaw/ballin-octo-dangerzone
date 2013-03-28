/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stillpig.babyonthecloud.customcode;

import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.core.PushServiceException;
import com.stackmob.core.ServiceNotActivatedException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.*;
import com.stackmob.sdkapi.PushService.TokenAndType;
import com.stackmob.sdkapi.PushService.TokenType;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author danielchong
 */
public class GrantPermissionForChild implements CustomCodeMethod {

    public String getMethodName() {
        return "grant_permission_for_child";
    }

    public List<String> getParams() {
        return Arrays.asList("permission", "from_user", "to_user", "child_code");
    }

    private boolean sentPushNotificationToUser(SDKServiceProvider serviceProvider, String to_user, String from_user, String child_code, boolean permission) {
        try {
            PushService pushService = serviceProvider.getPushService();

            String successMessage = "Congrates! " + from_user + "has given you permission to sync with " + child_code +".";
            String failMessage = "Sorry, " + from_user + "has denied you permission to sync with " + child_code + ".";
            String message = permission? successMessage:failMessage;
            //get all tokens for John Doe
            List<String> users = new ArrayList<String>();
            users.add(to_user);

            //send a push notification just to John Doe's iOS device
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("badge", "1");
            payload.put("sound", "customsound.wav");
            payload.put("alert", message);
            payload.put("other", "stuff");
            
            //send a push notification to all of John Doe's devices
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
        String to_user = request.getParams().get("to_user");
        String permissionString = request.getParams().get("permission");

        if (from_user == null || from_user.isEmpty() || child_code == null || child_code.isEmpty() || to_user == null || to_user.isEmpty() || permissionString == null || permissionString.isEmpty()) {
            HashMap<String, String> errParams = new HashMap<String, String>();
            errParams.put("error", "one or more of the parameters was empty or null");
            return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
        }

        // get the datastore service and assemble the query
        DataService dataService = serviceProvider.getDataService();

        // build a query
        List<SMCondition> query = new ArrayList<SMCondition>();

//        dataService.
        List<SMObject> result;
        try {

            boolean permission = false;
            
            boolean sentPushNotification = sentPushNotificationToUser(serviceProvider, to_user, from_user, child_code, permission);






            
//
//            result = dataService.readObjects("users", query);
//            result = dataService.readObjects("users", query, 1); // Expanded relationship
//
//            SMObject userObject;
//
//            // user was in the datastore, so check the score and update if necessary
//            if (result != null && result.size() == 1) {
//                userObject = result.get(0);
//            } else {
//                Map<String, SMValue> userMap = new HashMap<String, SMValue>();
//                userMap.put("username", new SMString(username));
//                userMap.put("score", new SMInt(0L));
//                newUser = true;
//                userObject = new SMObject(userMap);
//            }
//
//            SMValue oldScore = userObject.getValue().get("score");
//
//            // if it was a high score, update the datastore
//            List<SMUpdate> update = new ArrayList<SMUpdate>();
//            if (oldScore == null || ((SMInt) oldScore).getValue() < score) {
//                update.add(new SMSet("score", new SMInt(score)));
//                permission = true;
//            }


            Map<String, Object> returnMap = new HashMap<String, Object>();
            returnMap.put("permission", permission);
            returnMap.put("from_user", from_user);
            returnMap.put("child_code", child_code);
            return new ResponseToProcess(HttpURLConnection.HTTP_OK, returnMap);

//        } catch (InvalidSchemaException e) {
//            HashMap<String, String> errMap = new HashMap<String, String>();
//            errMap.put("error", "invalid_schema");
//            errMap.put("detail", e.toString());
//            return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
//        } catch (DatastoreException e) {
//            HashMap<String, String> errMap = new HashMap<String, String>();
//            errMap.put("error", "datastore_exception");
//            errMap.put("detail", e.toString());
//            return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
        } catch (Exception e) {
            HashMap<String, String> errMap = new HashMap<String, String>();
            errMap.put("error", "unknown");
            errMap.put("detail", e.toString());
            return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
        }
    }
}
