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

            String successMessage = "Congrates! " + from_user + "has given you permission to sync with " + child_code + ".";
            String failMessage = "Sorry, " + from_user + "has denied you permission to sync with " + child_code + ".";
            String message = permission ? successMessage : failMessage;
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

    private long getSecondsSince1970() {
        return System.currentTimeMillis() / 1000L;
    }

    private SMObject getChildObject(DataService dataService, String child_code) {
        try {
            List<SMCondition> query = new ArrayList<SMCondition>();
            query.add(new SMEquals("child_code", new SMString(child_code)));

            List<SMObject> result = dataService.readObjects("child", query);

            SMObject childObject = result.get(0);

            return childObject;
        } catch (InvalidSchemaException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatastoreException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    private void createPermissionInFromUser(DataService dataService, String child_id, String permission_id, String to_user, String from_user) {

        try {
            List<SMCondition> query = new ArrayList<SMCondition>();
            query.add(new SMEquals("username", new SMString(from_user)));
            List<SMObject> result = dataService.readObjects("user", query);
            SMObject userObject = result.get(0);
            SMList<SMString> permissionToList = (SMList<SMString>) userObject.getValue().get("permissions_to");

            List<SMUpdate> toUserMap = new ArrayList<SMUpdate>();

            List<SMString> newPermissions = permissionToList.getValue();
            newPermissions.add(new SMString(permission_id));
            toUserMap.add(new SMSet("permissions_from", new SMList<SMString>(newPermissions)));

            dataService.updateObject("user", from_user, toUserMap);

        } catch (InvalidSchemaException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatastoreException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void createPermissionInToUser(DataService dataService, String child_id, String permission_id, String to_user, String from_user) {

        try {
            List<SMCondition> query = new ArrayList<SMCondition>();
            query.add(new SMEquals("username", new SMString(to_user)));
            List<SMObject> result = dataService.readObjects("user", query);
            SMObject userObject = result.get(0);
            SMList<SMString> permissionFromList = (SMList<SMString>) userObject.getValue().get("permissions_from");
            SMList<SMString> childReadPermissionList = (SMList<SMString>) userObject.getValue().get("child_read_permission");
            SMList<SMString> childWritePermissionList = (SMList<SMString>) userObject.getValue().get("child_write_permission");

            List<SMUpdate> toUserMap = new ArrayList<SMUpdate>();

            List<SMString> newWritePermissions = permissionFromList.getValue();
            newWritePermissions.add(new SMString(permission_id));
            toUserMap.add(new SMSet("permissions_from", new SMList<SMString>(newWritePermissions)));

            List<SMString> newChildReadPermissions = childReadPermissionList.getValue();
            newChildReadPermissions.add(new SMString(child_id));
            toUserMap.add(new SMSet("child_read_permission", new SMList<SMString>(newChildReadPermissions)));

            List<SMString> newChildWritePermissions = childWritePermissionList.getValue();
            newChildWritePermissions.add(new SMString(child_id));
            toUserMap.add(new SMSet("child_read_permission", new SMList<SMString>(newChildWritePermissions)));

            dataService.updateObject("user", to_user, toUserMap);

        } catch (InvalidSchemaException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatastoreException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createPermissionInChild(DataService dataService, SMObject childObject, String permission_id, String to_user) {
        try {
            String child_id = childObject.getValue().get("child_id").toString();
            SMList<SMString> readPermissionList = (SMList<SMString>) childObject.getValue().get("user_read_permission");
            SMList<SMString> writePermissionList = (SMList<SMString>) childObject.getValue().get("user_write_permission");
            SMList<SMString> permissionList = (SMList<SMString>) childObject.getValue().get("permissions");

            // update permission list on child
            List<SMUpdate> childMap = new ArrayList<SMUpdate>();
            List<SMString> newReadPermissions = readPermissionList.getValue();
            newReadPermissions.add(new SMString(to_user));
            childMap.add(new SMSet("user_read_permission", new SMList<SMString>(newReadPermissions)));

            List<SMString> newWritePermissions = writePermissionList.getValue();
            newWritePermissions.add(new SMString(to_user));
            childMap.add(new SMSet("user_write_permission", new SMList<SMString>(newWritePermissions)));

            List<SMString> newPermissionsList = writePermissionList.getValue();
            newPermissionsList.add(new SMString(to_user));
            childMap.add(new SMSet("user_write_permission", new SMList<SMString>(newPermissionsList)));
            dataService.updateObject("child", child_id, childMap);
        } catch (InvalidSchemaException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatastoreException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
        String child_code = request.getParams().get("child_code");
        String from_user = request.getParams().get("from_user");
        String to_user = request.getParams().get("to_user");
        String permissionString = request.getParams().get("permission");

        if (from_user == null || from_user.isEmpty() || child_code == null || child_code.isEmpty() || to_user == null || to_user.isEmpty() || permissionString == null || permissionString.isEmpty()) {
            HashMap<String, String> errParams = new HashMap<String, String>();
            errParams.put("error", "one or more parameters were empty or null");
            return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
        }

        DataService dataService = serviceProvider.getDataService();

        SMObject childObject = getChildObject(dataService, child_code);
        String child_id = childObject.getValue().get("child_id").toString();

        try {

            boolean permission = false;

            // create new permission object
            Map<String, SMValue> permissionMap = new HashMap<String, SMValue>();
            permissionMap.put("child", new SMString(child_id));
            permissionMap.put("from_user", new SMString(from_user));
            permissionMap.put("to_user", new SMString(to_user));
            permissionMap.put("approved_date", new SMInt(getSecondsSince1970()));
            permissionMap.put("permission_type", new SMInt(Long.parseLong(permissionString)));
            SMObject permissionObject = new SMObject(permissionMap);
            SMObject createdPermissionObject = dataService.createObject("permission", permissionObject);


            String permission_id = createdPermissionObject.getValue().get("permission_id").toString();

            createPermissionInChild(dataService, childObject, permission_id, to_user);

            createPermissionInToUser(dataService, child_id, permission_id, to_user, from_user);
            
            createPermissionInFromUser(dataService, child_id, permission_id, to_user, from_user);

            boolean sentPushNotification = sentPushNotificationToUser(serviceProvider, to_user, from_user, child_code, permission);


            Map<String, Object> returnMap = new HashMap<String, Object>();
            returnMap.put("permission", permission);
            returnMap.put("from_user", from_user);
            returnMap.put("child_code", child_code);
            return new ResponseToProcess(HttpURLConnection.HTTP_OK, returnMap);

        } catch (Exception e) {
            HashMap<String, String> errMap = new HashMap<String, String>();
            errMap.put("error", "unknown");
            errMap.put("detail", e.toString());
            return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
        }
    }
}
