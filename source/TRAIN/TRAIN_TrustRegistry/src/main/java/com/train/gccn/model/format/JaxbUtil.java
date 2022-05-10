package com.train.gccn.model.format;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.StringReader;

public class JaxbUtil {
    
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> T unmarshal(String xml, Class<T> clss) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(clss);
        Object o = JaxbUtil.unmarshal(jaxbContext, xml, clss);
        return clss.cast(o);
    }
    
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> T unmarshal(JAXBContext jaxbContext, String xml, Class<T> clss) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Object obj = unmarshaller.unmarshal(new StringReader(xml));
        return clss.cast(obj);
    }
    
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> T unmarshal(File xmlf, Class<T> clss) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(clss);
        return JaxbUtil.unmarshal(ctx, xmlf, clss);
    }
    
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> T unmarshal(JAXBContext ctx, File xmlf, Class<T> clss) throws JAXBException {
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        Object obj = unmarshaller.unmarshal(xmlf);
        return clss.cast(obj);
    }
    
}
