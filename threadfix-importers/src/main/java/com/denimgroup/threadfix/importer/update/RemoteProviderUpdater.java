////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2014 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.importer.update;

import com.denimgroup.threadfix.annotations.MappingsUpdater;
import com.denimgroup.threadfix.data.entities.RemoteProviderType;
import com.denimgroup.threadfix.logging.SanitizedLogger;
import com.denimgroup.threadfix.service.RemoteProviderTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.io.BufferedReader;
import java.io.IOException;

import static com.denimgroup.threadfix.importer.update.UpdaterConstants.REMOTE_PROVIDERS_FOLDER;

/**
 * Created by mac on 11/18/14.
 */
@Service
@MappingsUpdater
public class RemoteProviderUpdater extends SpringBeanAutowiringSupport implements Updater {

    private static final SanitizedLogger LOG = new SanitizedLogger(RemoteProviderUpdater.class);

    @Autowired
    private RemoteProviderTypeService remoteProviderTypeService;

    enum State {
        START, NAME, CREDENTIALS
    }

    State currentState = State.START;

    @Override
    public void doUpdate(String fileName, BufferedReader bufferedReader) throws IOException {
        LOG.debug("Performing mapping update for file " + fileName);

        assert remoteProviderTypeService != null :
                "remoteProviderTypeService was null. This indicates an error in Spring autowiring.";

        String name = null;
        boolean usernamePassword = false;

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.equals("type.name")) {
                currentState = State.NAME;
            } else if (line.equals("type.credentials")) {
                currentState = State.CREDENTIALS;
            } else if (currentState == State.NAME) {
                name = line;
            } else if (currentState == State.CREDENTIALS) {
                usernamePassword = line.equalsIgnoreCase("usernamepassword");
            }
        }

        if (name == null) {
            throw new IllegalStateException("Failed to get a remote provider name from " + fileName);
        }

        RemoteProviderType databaseType = remoteProviderTypeService.load(name);

        if (databaseType == null) {
            LOG.info("Creating new RemoteProviderType with name " + name);

            RemoteProviderType type = new RemoteProviderType();
            type.setName(name);
            type.setHasUserNamePassword(usernamePassword);
            remoteProviderTypeService.store(type);

        } else {
            LOG.debug(name + " was already present in the database.");
        }
    }

    @Override
    public String getFolder() {
        return REMOTE_PROVIDERS_FOLDER;
    }

}
