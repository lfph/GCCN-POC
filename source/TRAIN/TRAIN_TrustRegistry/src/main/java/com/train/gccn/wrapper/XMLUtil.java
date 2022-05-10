package com.train.gccn.wrapper;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class XMLUtil {
    
    private static Logger logger = Logger.getLogger(XMLUtil.class);
    private final Element root;
    private final String rootTagName;
    
    public XMLUtil(InputStream xmlFilestream) throws IOException, ParserConfigurationException, SAXException {
        this(IOUtils.toString(xmlFilestream));
    }
    
    
    public XMLUtil(File xmlFile) throws IOException, ParserConfigurationException, SAXException {
        this(new String(Files.readAllBytes(Paths.get(xmlFile.getPath()))));
    }
    
    public XMLUtil(String xmlData) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        XMLUtil.secureFactory(dbFactory);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        
        InputSource is = new InputSource(new StringReader(xmlData));
        Document doc = dBuilder.parse(is);
        
        doc.getDocumentElement().normalize();
        this.root = doc.getDocumentElement();
        
        this.rootTagName = this.root.getTagName();
    }
    
    public static void secureFactory(DocumentBuilderFactory factory) throws ParserConfigurationException {
        // via https://gist.github.com/AlainODea/1779a7c6a26a5c135280bc9b3b71868f
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
    }
    
    public String getAttribute(String attributeName) {
        
        return this.root.getAttributes().getNamedItem(attributeName).getNodeValue();
    }
    
    public String getElementAttribute(List<String> path, String attributeName) {
        if(!isSafeString(attributeName)) {
            XMLUtil.logger.warn("Invalid attributeName: " + attributeName);
            return null;
        }
    
        String xpath = convertPathToXPath(path, false, true);
    
        xpath = "string(" + xpath + "/@" + attributeName + ")";
    
        return getElementByXPath(xpath);
    }
    
    public boolean isElement(String path) {
        XMLUtil.logger.info("path: " + path);
        String xpath = convertPathToXPath(path, false, true);
        return getElementByXPath(xpath) != null;
    }
    
    public String getElement(String path) {
        String xpath = convertPathToXPath(path);
        return getElementByXPath(xpath);
    }
    
    public String getElement(String path, Node node) {
        String xpath = convertPathToXPath(path, true, false);
        return getElementByXPath(xpath, node);
    }
    
    public String getElement(List<String> path) {
        String xpath = convertPathToXPath(path);
        return getElementByXPath(xpath);
    }
    
    public NodeList getNodesAsMap(String path) {
        String xpath = convertPathToXPath(path, false, true);
        return getNodeListByXPath(xpath);
    }
    
    public Map<String, String> getNodesAsMap(String path1, String path2key, String path2value, Node agreementNode) {
        String xpath1 = convertPathToXPath(path1, false, false);
        String xpath2key = convertPathToXPath(path2key, true, false);
        String xpath2value = convertPathToXPath(path2value, true, false);
        
        NodeList nodeList = getNodeListByXPath(xpath1, agreementNode);
        if(nodeList == null) {
            return null;
        }
    
        Map<String, String> results = new HashMap<>();
        
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node paramNode = nodeList.item(i);
            String key = getElementByXPath(xpath2key, paramNode);
            String value = getElementByXPath(xpath2value, paramNode);
            results.put(key, value);
        }
        
        return results;
    }
    
    private NodeList getNodeListByXPath(String xpath) {
        return getNodeListByXPath(xpath, this.root);
    }
    
    private NodeList getNodeListByXPath(String xpath, Object rootElement) {
        if(xpath == null) {
            return null;
        }
        
        XMLUtil.logger.info("Executing xpath " + xpath);
        
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        try {
            return (NodeList) xp.evaluate(xpath, rootElement, XPathConstants.NODESET);
            
        } catch(XPathExpressionException e) {
            XMLUtil.logger.error("XPath Error", e);
            return null;
        }
    }
    
    public String getElementByXPath(String xpath) {
        return getElementByXPath(xpath, this.root);
    }

    public NodeList getElementsByXpath(String xpath)
    {
        if(xpath == null) {
            return null;
        }

        XMLUtil.logger.info("Executing xpath " + xpath);

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        try {
            return (NodeList) xp.evaluate(xpath, this.root, XPathConstants.NODESET);

        } catch(XPathExpressionException e) {
            XMLUtil.logger.error("XPath Error", e);
            return null;
        }
    }
    
    public String getElementByXPath(String xpath, Object rootElement) {
        
        if(xpath == null) {
            return null;
        }
        
        XMLUtil.logger.info("Executing xpath " + xpath);
        
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        try {
            String res = xp.evaluate(xpath, rootElement);
            return res != null && res.trim().length() > 0 ? res : null;
            
        } catch(XPathExpressionException e) {
            XMLUtil.logger.error("XPath Error", e);
            return null;
        }
    }
    
    private String convertPathToXPath(List<String> path) {
        return convertPathToXPath(path, true, true);
    }
    
    private String convertPathToXPath(List<String> path, boolean pathToText, boolean prefixRootname) {
        String xpath = "";
        if(prefixRootname) {
            xpath += "//" + this.rootTagName + "/";
        }
        
        for(String s : path) {
            s = s.trim();
            if(isSafeString(s)) {
                xpath += s;
                xpath += "/";
            } else {
                XMLUtil.logger.warn("Invalid element in path: " + s);
                return null;
            }
        }
        if(pathToText == true) {
            xpath += "text()";
        } else {
            xpath = xpath.substring(0, xpath.length() - 1);
        }
        
        return xpath;
    }
    
    private String convertPathToXPath(String path) {
        return convertPathToXPath(path, true, true);
    }
    
    private String convertPathToXPath(String path, boolean pathToText, boolean prefixRootname) {
        path = path.trim();
        return convertPathToXPath(Arrays.asList(path.split("\\.")), pathToText, prefixRootname);
    }
    
    private boolean isSafeString(String s) {
        return s.matches("[a-zA-Z0-9_\\-]+");
    }
    
}
