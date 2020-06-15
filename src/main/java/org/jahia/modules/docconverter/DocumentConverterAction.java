/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.docconverter;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FilenameUtils;
import org.artofsolving.jodconverter.office.OfficeException;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.*;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.transform.DocumentConverterService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.tools.files.FileUpload;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Document conversion action.
 * User: fabrice
 * Date: Apr 20, 2010
 * Time: 11:14:20 AM
 */
public class DocumentConverterAction extends Action {

    private DocumentConverterService converterService;

    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        if (converterService.isEnabled()) {
            // Get parameters + file
            final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
            DiskFileItem inputFile = fu.getFileItems().get("fileField");
            List<String> mimeTypeList = parameters.get("mimeType");

            String returnedMimeType = mimeTypeList != null ? mimeTypeList.get(0) : null;

            // Convert
            boolean conversionSucceeded = true;
            String failureMessage = null;
            File convertedFile = null;
            try {
                convertedFile = converterService.convert(inputFile.getStoreLocation(), inputFile.getContentType(),
                        returnedMimeType);
            } catch (IOException ioe) {
                conversionSucceeded = false;
                failureMessage = ioe.getMessage();
            } catch (OfficeException ioe) {
                conversionSucceeded = false;
                failureMessage = ioe.getMessage();
            }

            if (convertedFile == null) {
                conversionSucceeded = false;
            }


            // Create a conversion node and the file node if all succeeded
            String originFileName = inputFile.getName();
            String originMimeType = inputFile.getContentType();
            String convertedFileName = FilenameUtils.getBaseName(inputFile.getName()) + "." + converterService.getExtension(
                    returnedMimeType);


            // todo: upgrade to use JCRContentUtils and appropriate version of doExecute when possible
            // check that the user has a files folder and create it if needed
            final JahiaUser user = renderContext.getUser();
            final JCRNodeWrapper userNode = session.getNode(user.getLocalPath());
            final String filesRelPath = "files";
            if (!userNode.hasNode(filesRelPath)) {
                JCRTemplate.getInstance().doExecuteWithSystemSession(user.getUsername(), Constants.EDIT_WORKSPACE, new JCRCallback<Object>() {
                    @Override
                    public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                        // create files directory for user in edit if it doesn't already exists
                        final String userPath = userNode.getPath();
                        final JCRNodeWrapper editUserNode = session.getNode(userPath);
                        final JCRNodeWrapper files;
                        if (!editUserNode.hasNode(filesRelPath)) {
                            files = editUserNode.addNode(filesRelPath, "jnt:folder");
                            session.save();
                        } else {
                            files = editUserNode.getNode(filesRelPath);
                        }

                        // publish it
                        JCRPublicationService.getInstance().publish(Collections.singletonList(files.getIdentifier()), Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, false,
                                null);
                        return null;
                    }
                });
            }

            JCRNodeWrapper convertedFilesNode = session.getNode(renderContext.getUser().getLocalPath() + "/files");
            JCRNodeWrapper convertedFileNode;
            if (conversionSucceeded) {
                FileInputStream iStream = new FileInputStream(convertedFile);
                try {
                    convertedFileNode = convertedFilesNode.uploadFile(convertedFileName, iStream, returnedMimeType);
                    convertedFileNode.addMixin("jmix:convertedFile");
                } finally {
                    iStream.close();
                }
            } else {
                convertedFileNode = convertedFilesNode.uploadFile(convertedFileName, inputFile.getInputStream(), inputFile.getContentType());
                convertedFileNode.addMixin("jmix:convertedFile");
                convertedFileNode.setProperty("conversionFailedMessage", failureMessage);
            }

            convertedFileNode.setProperty("originDocName", originFileName);
            convertedFileNode.setProperty("originDocFormat", originMimeType);
            convertedFileNode.setProperty("convertedDocName", convertedFileName);
            convertedFileNode.setProperty("convertedDocFormat", returnedMimeType);
            convertedFileNode.setProperty("conversionSucceeded", conversionSucceeded);

            session.save();
        }
        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject());
    }


    /**
     * @param converterService the converterService to set
     */
    public void setConverterService(DocumentConverterService converterService) {
        this.converterService = converterService;
    }

}
