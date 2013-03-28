/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stillpig.babyonthecloud.customcode;

import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//TODO: need to add friend to user, don't need to mass assign username to each objects

/**
 *
 * @author danielchong
 */
public class MassObjectEditor implements CustomCodeMethod {

    public String getMethodName() {
        return "mass_object_editor";
    }

    public List<String> getParams() {
        return new ArrayList<String>();
    }

    public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
        String username = request.getParams().get("username");
        Long score = Long.parseLong(request.getParams().get("score"));

        if (username == null || username.isEmpty() || score == null) {
            HashMap<String, String> errParams = new HashMap<String, String>();
            errParams.put("error", "one or both the username or score was empty or null");
            return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
        }

        // get the datastore service and assemble the query
        DataService dataService = serviceProvider.getDataService();

        // build a query
        List<SMCondition> query = new ArrayList<SMCondition>();
        query.add(new SMEquals("username", new SMString(username)));
        query.add(new SMNotEqual("friends", new SMString(username)));

//        dataService.
        // execute the query
        List<SMObject> result;
        try {
            boolean newUser = false;
            boolean updated = false;

            result = dataService.readObjects("users", query);
            result = dataService.readObjects("users", query, 1); // Expanded relationship

            SMObject userObject;

            // user was in the datastore, so check the score and update if necessary
            if (result != null && result.size() == 1) {
                userObject = result.get(0);
            } else {
                Map<String, SMValue> userMap = new HashMap<String, SMValue>();
                userMap.put("username", new SMString(username));
                userMap.put("score", new SMInt(0L));
                newUser = true;
                userObject = new SMObject(userMap);
            }

            SMValue oldScore = userObject.getValue().get("score");

            // if it was a high score, update the datastore
            List<SMUpdate> update = new ArrayList<SMUpdate>();
            if (oldScore == null || ((SMInt) oldScore).getValue() < score) {
                update.add(new SMSet("score", new SMInt(score)));
                updated = true;
            }

            if (newUser) {
                dataService.createObject("users", userObject);
            } else if (updated) {
                dataService.updateObject("users", username, update);
            }

            Map<String, Object> returnMap = new HashMap<String, Object>();
            returnMap.put("updated", updated);
            returnMap.put("newUser", newUser);
            returnMap.put("username", username);
            return new ResponseToProcess(HttpURLConnection.HTTP_OK, returnMap);
        } catch (InvalidSchemaException e) {
            HashMap<String, String> errMap = new HashMap<String, String>();
            errMap.put("error", "invalid_schema");
            errMap.put("detail", e.toString());
            return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
        } catch (DatastoreException e) {
            HashMap<String, String> errMap = new HashMap<String, String>();
            errMap.put("error", "datastore_exception");
            errMap.put("detail", e.toString());
            return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
        } catch (Exception e) {
            HashMap<String, String> errMap = new HashMap<String, String>();
            errMap.put("error", "unknown");
            errMap.put("detail", e.toString());
            return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
        }

    }
}
