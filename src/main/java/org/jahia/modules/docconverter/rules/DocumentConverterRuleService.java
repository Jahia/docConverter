/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.docconverter.rules;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artofsolving.jodconverter.document.DocumentFormat;
import org.drools.core.spi.KnowledgeHelper;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.transform.DocumentConverterService;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Service class for converting documents from the right-hand-side
 * (consequences) of rules.
 *
 * @author Sergiy Shyrkov
 */
public class DocumentConverterRuleService {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger(DocumentConverterRuleService.class);

    private DocumentConverterService converterService;

    /**
     * Converts the specified node using target MIME type.
     *
     * @param nodeFact          the node to be converted
     * @param targetMimeType    the target MIME type
     * @param overwriteIfExists is set to true, the existing file should be
     *                          overwritten if exists; otherwise the new file name will be
     *                          generated automatically.
     * @param drools            the rule engine helper class
     * @throws RepositoryException in case of an error
     */
    public void convert(final AddedNodeFact nodeFact, final String targetMimeType, boolean overwriteIfExists,
                        KnowledgeHelper drools) throws RepositoryException {
        if (!converterService.isEnabled()) {
            logger.info("Conversion service is not enabled." + " Skip converting file " + nodeFact.getPath());
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Converting node " + nodeFact.getPath() + " into target MIME type '" + targetMimeType + "'");
        }
        DocumentFormat format = converterService.getFormatByMimeType(targetMimeType);
        if (format == null) {
            logger.warn("Unsupported target MIME type '" + targetMimeType + "'. Skip converting file "
                    + nodeFact.getPath());
            return;
        }

        InputStream is = null;
        try {
            JCRNodeWrapper node = nodeFact.getNode();
            if (node.isNodeType("nt:file")) {
                is = node.getFileContent().downloadFile();
                File temp = File.createTempFile("doc-converter-rule", null);
                FileOutputStream os = new FileOutputStream(temp);
                try {
                    converterService.convert(is, nodeFact.getMimeType(), os, targetMimeType);
                    os.close();

                    JCRNodeWrapper folder = node.getParent();
                    String newName = StringUtils.substringBeforeLast(node.getName(), ".") + "." + format.getExtension();

                    if (!overwriteIfExists) {
                        newName = JCRContentUtils.findAvailableNodeName(folder, newName);
                    }

                    FileInputStream convertedStream = new FileInputStream(temp);
                    try {
                        folder.uploadFile(newName, convertedStream, format.getMediaType());
                        logger.info("Converted node " + nodeFact.getPath() + " to type " + format.getMediaType());
                    } finally {
                        IOUtils.closeQuietly(convertedStream);
                    }
                } finally {
                    IOUtils.closeQuietly(os);
                    FileUtils.deleteQuietly(temp);
                }
            } else {
                logger.warn("Path should correspond to a file node. Skipping node " + nodeFact.getPath());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * Injects an instance of the {@link DocumentConverterService}.
     *
     * @param converterService an instance of the
     *                         {@link DocumentConverterService}
     */
    public void setConverterService(DocumentConverterService converterService) {
        this.converterService = converterService;
    }

}