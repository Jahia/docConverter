# Document Converter

Jahia Document Converter Service delegates conversion tasks to an OpenOffice/LibreOffice instance, either to a local one or a remote service.
To use the converter service, you need OpenOffice/LibreOffice v3 or higher installed (the latest stable 4.x version is recommended).
Further in this document, we refer to OpenOffice or LibreOffice as “OpenOffice” for the sake of simplicity.

## Open-Source community module

This is an Open-Source, community-supported module, you can find more details about Open-Source @ Jahia [in this repository](https://github.com/Jahia/open-source) and more about Jahia Community on this [Academy page](https://academy.jahia.com/community).

## Documentation

### Document Converter

Jahia Document Converter Service delegates conversion tasks to an OpenOffice/LibreOffice instance, either to a local one or a remote service. To use the converter service, you need OpenOffice/LibreOffice v3 or higher installed (the latest stable 4.x version is recommended). Further in this document, we refer to OpenOffice or LibreOffice as “OpenOffice” for the sake of simplicity.

In order to enable the service the following setting should be set to true in the jahia.properties file:

```
######################################################################
### Document Converter Service #######################################
######################################################################
# Set this to true to enable the document conversion service
documentConverter.enabled = true
```

#### LocalOpenOffice instance
The converter service is capable of creating an OpenOffice process and using it, in case Jahia and OpenOffice are located on the same machine. In such case, the converter service starts a local instance of the OpenOffice service for processing conversion tasks. The configuration in this case is pretty simple: a service needs to be enabled (see above) and a path to the OpenOffice folder has to be provided in the jahia.properties file:


```
######################################################################
### Document Converter Service #######################################
######################################################################
# Set this to true to enable the document conversion service
documentConverter.enabled = false
# The filesystem path to the OpenOffice
# Usually for Linux it is: /usr/lib/openoffice
# for Windows: c:/Program Files (x86)/OpenOffice 4
# and for Mac OS X: /Applications/OpenOffice.org.app/Contents
documentConverter.officeHome = /usr/lib/openoffice
```

#### RemoteOpenOffice service
The converter service is capable of using an OpenOffice process started as a service on a local or remote machine. This connection is configured as given below in the snapshot of the applicationcontext-doc-converter.xml file:

```xml
<bean id="DocumentConverterService"
      class="org.jahia.services.transform.DocumentConverterService"
      init-method="start" destroy-method="stop">
    <property name="enabled" value="true"/>
    <property name="officeManagerBeanName" value="remoteOfficeManagerFactory"/>
</bean>
<bean name="remoteOfficeManagerFactory"
      class="org.jahia.services.transform.RemoteOfficeManagerFactory"
      lazy-init="true">
    <property name="host" value="192.168.1.101"/>
    <property name="portNumber" value="19001"/>
</bean>
```

OpenOffice in this case should be started as a service on the 192.168.1.101 machine. A sample command for starting OpenOffice as a service looks like:

```bash
soffice -headless -accept="socket,host=192.168.1.101,port=19001;urp;" -nofirststartwizard
```
More details can be found on the JODConverter Web Site (https://github.com/sbraconnier/jodconverter/wiki/), including the HowTo for:
* Creating an OpenOffice.org Service on Windows (https://github.com/sbraconnier/jodconverter/wiki/Command-Line-Tool)
* Creating an OpenOffice.org Service on Unix-like systems (https://github.com/sbraconnier/jodconverter/wiki/Command-Line-Tool).

### Document Viewer

Jahia offers a built-in support for previewing various types of documents (PDF, Office, etc.) as a SWF flash using a player in a Web page. The direct conversion to flash is available for PDF documents only. To have a preview for non-PDF files (Microsoft Office, OpenOffice etc.) the document converter service should be enabled to perform an intermediate conversion of documents to PDF files.

The viewer service requires the pdf2swf utility (from SWFTools: http://www.swftools.org/) to be installed. The installation guidelines are available on the corresponding Wiki pages: http://wiki.swftools.org/wiki/Installation.

The following two configuration parameters in digital-factory-config/jahia/jahia.properties file are responsible for enabling and configuring the document viewer service:

```
######################################################################
### Document Viewer Service ##########################################
######################################################################
# Viewer service enables previewing of documents of various formats
# (PDF, Office, etc.) as a SWF flash.
# The direct conversion to flash is available for PDF files only.
# In order for this service to work with non-PDF files a document
# converter service (see section above) should be enabled to perform
# an intermediate conversion of documents to PDF files.
# Set this to true to enable the document viewer service
jahia.dm.viewer.enabled = false
# Viewer service requires the pdf2swf utility (from SWFTools) to be installed
# The following specifies the path to the pdf2swf executable file
# Usually for Linux it is: /usr/bin/pdf2swf
# for Windows: C:/Program Files (x86)/SWFTools/pdf2swf.exe
# If the SWFTools installation folder is present in your PATH, you can
# specify only the executable name here
jahia.dm.viewer.pdf2swf = pdf2swf
```

The jahia.dm.viewer.pdf2swf parameter should contain an absolute path to the pdf2swf executable file or, in case the corresponding folder is included into the PATH environment variable, just the executable name, i.e. pdf2swf.

### Document thumbnails
In Jahia we are pleased to offer an out-of-the-box support for automatic creation of image thumbnails for uploaded documents that significantly improves the usability and user experience when working with Jahia Document Manager or document-related components.

The service is enabled by default for all PDF documents. A thumbnail is automatically created for the first page of an uploaded document.

To have thumbnails for non-PDF files (Microsoft Office, OpenOffice etc.) the document converter service should be enabled to perform an intermediate conversion of documents to PDF files.

The following entry in the `digital-factory-config/jahia/jahia.properties` file is responsible for enabling/disabling the document thumbnails service:

```
######################################################################
### Document Thumbnails Service ######################################
######################################################################
# Document thumbnails service enables automatic creation of thumbnail
# images for uploaded documents.
# The direct creation of a thumbnail is available for PDF files only.
# In order for this service to work with non-PDF files a document
# converter service (see section above) should be enabled to perform
# an intermediate conversion of documents to PDF files.
# The following enables/disables the document thumbnails service
jahia.dm.thumbnails.enabled = true
```
