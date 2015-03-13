/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.docconverter;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FilenameUtils;
import org.artofsolving.jodconverter.office.OfficeException;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Action;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.transform.DocumentConverterService;
import org.jahia.tools.files.FileUpload;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
