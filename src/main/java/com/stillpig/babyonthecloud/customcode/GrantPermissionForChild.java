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

    public static final String CHILD = "child";
    public static final String CHILD_CODE = "child_code";
    public static final String CHILD_ID = "child_id";
    public static final String CHILD_PERMISSIONS = "permissions";
    public static final String CHILD_USER_READ_PERMISSIONS = "user_read_permissions";
    public static final String CHILD_USER_WRITE_PERMISSIONS = "user_write_permissions";
    public static final String PERMISSION = "permission";
    public static final String PERMISSION_APPROVED_DATE = "approved_date";
    public static final String PERMISSION_FROM_USER = "from_user";
    public static final String PERMISSION_ID = "permission_id";
    public static final String PERMISSION_TO_USER = "to_user";
    public static final String PERMISSION_TYPE = "permission_type";
    public static final String USER = "user";
    public static final String USERNAME = "username";
    public static final String USER_CHILD_READ_PERMISSIONS = "child_read_permissions";
    public static final String USER_CHILD_WRITE_PERMISSIONS = "child_write_permissions";
    public static final String USER_PERMISSIONS_FROM = "permissions_from";
    public static final String USER_PERMISSIONS_TO = "permissions_to";

    public String getMethodName() {
        return "grant_permission_for_child";
    }

    public List<String> getParams() {
        return Arrays.asList(PERMISSION, PERMISSION_FROM_USER, PERMISSION_TO_USER, CHILD_CODE);
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
            query.add(new SMEquals(CHILD_CODE, new SMString(child_code)));

            List<SMObject> result = dataService.readObjects(CHILD, query);

            SMObject childObject = result.get(0);

            return childObject;
        } catch (InvalidSchemaException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatastoreException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private void insertIntoList(SMList<SMString> originalList, String value, List<SMUpdate> updateMap, String parameter) {
        List<SMString> newList = null;
        if (originalList != null) {
            newList = originalList.getValue();
        } else {
            newList = new ArrayList<SMString>();
        }
        newList.add(new SMString(value));
        updateMap.add(new SMSet(parameter, new SMList<SMString>(newList)));
    }

    private SMObject getUser(String to_user, DataService dataService) throws DatastoreException, InvalidSchemaException {
        List<SMCondition> query = new ArrayList<SMCondition>();
        query.add(new SMEquals(USERNAME, new SMString(to_user)));
        List<SMObject> result = dataService.readObjects(USER, query);
        SMObject userObject = result.get(0);
        return userObject;
    }
    
    private void createPermissionInFromUser(DataService dataService, String permission_id, String from_user) {

        try {
            SMObject userObject = getUser(from_user, dataService);
            SMList<SMString> permissionToList = (SMList<SMString>) userObject.getValue().get(USER_PERMISSIONS_TO);

            List<SMUpdate> toUserMap = new ArrayList<SMUpdate>();

            insertIntoList(permissionToList, permission_id, toUserMap, USER_PERMISSIONS_TO);

            dataService.updateObject(USER, from_user, toUserMap);

        } catch (InvalidSchemaException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatastoreException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createPermissionInToUser(DataService dataService, String child_id, String permission_id, String to_user, String from_user, boolean permitted) {

        try {
            SMObject userObject = getUser(to_user, dataService);
            SMList<SMString> permissionFromList = (SMList<SMString>) userObject.getValue().get(USER_PERMISSIONS_FROM);

            List<SMUpdate> toUserMap = new ArrayList<SMUpdate>();

            insertIntoList(permissionFromList, permission_id, toUserMap, USER_PERMISSIONS_FROM);

            if (permitted) {
                SMList<SMString> childReadPermissionList = (SMList<SMString>) userObject.getValue().get(USER_CHILD_READ_PERMISSIONS);
                SMList<SMString> childWritePermissionList = (SMList<SMString>) userObject.getValue().get(USER_CHILD_WRITE_PERMISSIONS);

                insertIntoList(childReadPermissionList, child_id, toUserMap, USER_CHILD_READ_PERMISSIONS);
                insertIntoList(childWritePermissionList, child_id, toUserMap, USER_CHILD_WRITE_PERMISSIONS);
            }

            dataService.updateObject(USER, to_user, toUserMap);

        } catch (InvalidSchemaException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatastoreException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createPermissionInChild(LoggerService logger, DataService dataService, SMObject childObject, String permission_id, String to_user) {
        try {
            String child_id = childObject.getValue().get(CHILD_ID).toString();
            SMList<SMString> readPermissionList = (SMList<SMString>) childObject.getValue().get(CHILD_USER_READ_PERMISSIONS);
            SMList<SMString> writePermissionList = (SMList<SMString>) childObject.getValue().get(CHILD_USER_WRITE_PERMISSIONS);
            SMList<SMString> permissionList = (SMList<SMString>) childObject.getValue().get(CHILD_PERMISSIONS);

//            logger.debug(readPermissionList.toString());
//            logger.debug(writePermissionList.toString());

            // update permission list on child
            List<SMUpdate> childMap = new ArrayList<SMUpdate>();

            insertIntoList(readPermissionList, to_user, childMap, CHILD_USER_READ_PERMISSIONS);
            insertIntoList(writePermissionList, to_user, childMap, CHILD_USER_WRITE_PERMISSIONS);
            insertIntoList(permissionList, permission_id, childMap, CHILD_PERMISSIONS);

            dataService.updateObject(CHILD, child_id, childMap);

        } catch (InvalidSchemaException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DatastoreException ex) {
            Logger.getLogger(GrantPermissionForChild.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String createPermissionObject(String child_id, String from_user, String to_user, String permissionString, DataService dataService) throws DatastoreException, InvalidSchemaException, NumberFormatException {
        // create new permission object
        Map<String, SMValue> permissionMap = new HashMap<String, SMValue>();
        permissionMap.put(CHILD, new SMString(child_id));
        permissionMap.put(PERMISSION_FROM_USER, new SMString(from_user));
        permissionMap.put(PERMISSION_TO_USER, new SMString(to_user));
        permissionMap.put(PERMISSION_APPROVED_DATE, new SMInt(getSecondsSince1970()));
        permissionMap.put(PERMISSION_TYPE, new SMInt(Long.parseLong(permissionString)));
        SMObject permissionObject = new SMObject(permissionMap);
        SMObject createdPermissionObject = dataService.createObject(PERMISSION, permissionObject);
        String permission_id = createdPermissionObject.getValue().get(PERMISSION_ID).toString();
        return permission_id;
    }

    public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {

        LoggerService logger = serviceProvider.getLoggerService(GrantPermissionForChild.class);

        String child_code = request.getParams().get(CHILD_CODE);
        String from_user = request.getParams().get(PERMISSION_FROM_USER);
        String to_user = request.getParams().get(PERMISSION_TO_USER);
        String permissionString = request.getParams().get(PERMISSION);

        if (from_user == null || from_user.isEmpty() || child_code == null || child_code.isEmpty() || to_user == null || to_user.isEmpty() || permissionString == null || permissionString.isEmpty()) {
            HashMap<String, String> errParams = new HashMap<String, String>();
            errParams.put("error", "one or more parameters were empty or null");
            return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
        }

        DataService dataService = serviceProvider.getDataService();

        SMObject childObject = getChildObject(dataService, child_code);
        String child_id = childObject.getValue().get(CHILD_ID).toString();

        try {

            boolean permitted = Integer.parseInt(permissionString) == 1;

            String permission_id = createPermissionObject(child_id, from_user, to_user, permissionString, dataService);
            logger.debug("Permission created: " + permission_id);

            if (permitted) {
                createPermissionInChild(logger, dataService, childObject, permission_id, to_user);
            }
            createPermissionInToUser(dataService, child_id, permission_id, to_user, from_user, true);
            createPermissionInFromUser(dataService, permission_id, from_user);

            boolean sentPushNotification = sentPushNotificationToUser(serviceProvider, to_user, from_user, child_code, permitted);

            Map<String, Object> returnMap = new HashMap<String, Object>();
            returnMap.put(PERMISSION, permitted);
            returnMap.put(PERMISSION_FROM_USER, from_user);
            returnMap.put(CHILD_CODE, child_code);
            return new ResponseToProcess(HttpURLConnection.HTTP_OK, returnMap);

        } catch (Exception e) {
            HashMap<String, String> errMap = new HashMap<String, String>();
            errMap.put("error", "unknown");
            errMap.put("detail", e.toString());
            return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
        }
    }
}
